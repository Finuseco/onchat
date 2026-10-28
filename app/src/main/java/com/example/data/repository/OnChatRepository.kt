package com.example.data.repository

import android.content.Context
import android.util.Log
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import com.example.data.api.ApiClient
import com.example.data.api.SessionTokenStore
import com.example.data.db.*
import com.example.data.model.*
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class OnChatRepository(private val context: Context) {
    private val db = AppDatabase.getDatabase(context)
    private val sessionDao = db.sessionDao()
    private val conversationDao = db.conversationDao()
    private val messageDao = db.messageDao()
    private val groupDao = db.groupDao()
    private val statusDao = db.statusDao()
    private val callDao = db.callDao()
    private val readConversationsSet = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()

    val currentSessionFlow: Flow<UserSessionEntity?> = sessionDao.getSessionFlow()

    val conversationsFlow: Flow<List<Conversation>> = conversationDao.getConversationsFlow().map { list ->
        val seenKeys = mutableSetOf<String>()
        val deduplicated = mutableListOf<ConversationEntity>()
        for (item in list) {
            val key = when {
                item.isGroup -> "group_${item.id}"
                !item.contactWalletId.isNullOrBlank() -> "wallet_${item.contactWalletId}"
                !item.contactPhone.isNullOrBlank() -> "phone_${cleanPhoneNumber9Digits(item.contactPhone)}"
                else -> "name_${item.name.trim().lowercase()}"
            }
            if (seenKeys.add(key)) {
                deduplicated.add(item)
            }
        }
        deduplicated.map { it.toDomain() }
    }

    val groupsFlow: Flow<List<Group>> = groupDao.getGroupsFlow().map { list ->
        list.map { entity ->
            Group(
                id = entity.id,
                name = entity.name,
                currency = entity.currency,
                description = entity.description,
                photoUrl = entity.photoUrl,
                membersCount = entity.membersCount
            )
        }
    }

    val callsFlow: Flow<List<CallRecord>> = callDao.getCallsFlow().map { list ->
        list.map { entity ->
            CallRecord(
                id = entity.id,
                peerName = entity.peerName,
                peerWalletId = entity.callerWalletId ?: entity.receiverWalletId,
                peerAvatar = entity.peerAvatar,
                isVideo = entity.isVideo,
                isIncoming = entity.isIncoming,
                isMissed = entity.isMissed,
                timestamp = entity.timestamp,
                durationSeconds = entity.durationSeconds
            )
        }
    }

    suspend fun getSession(): UserSessionEntity? = withContext(Dispatchers.IO) {
        val session = sessionDao.getSession()
        if (session != null && session.token.isNotEmpty()) {
            ApiClient.authToken = session.token
        }
        session
    }

    suspend fun saveSession(phone: String, token: String) = withContext(Dispatchers.IO) {
        ApiClient.authToken = token
        SessionTokenStore.saveToken(token)
        SessionTokenStore.saveUserPhone(phone)
        val existing = sessionDao.getSession()
        val session = (existing ?: UserSessionEntity(
            id = 1,
            token = token,
            userId = 1L,
            walletId = "",
            name = "Utilisateur",
            phone = phone
        )).copy(
            token = token,
            phone = phone
        )
        sessionDao.saveSession(session)
        try {
            fetchAuthMe(phone)
        } catch (_: Exception) {}
    }

    // --- Countries API ---

    private val defaultCountries = listOf(
        Country(code = "CD", name = "République démocratique du Congo", dialCode = "243"),
        Country(code = "FR", name = "France", dialCode = "33"),
        Country(code = "US", name = "United States", dialCode = "1"),
        Country(code = "BE", name = "Belgique", dialCode = "32"),
        Country(code = "CA", name = "Canada", dialCode = "1"),
        Country(code = "GB", name = "Royaume-Uni", dialCode = "44"),
        Country(code = "CG", name = "République du Congo", dialCode = "242"),
        Country(code = "RW", name = "Rwanda", dialCode = "250"),
        Country(code = "KE", name = "Kenya", dialCode = "254"),
        Country(code = "CI", name = "Côte d'Ivoire", dialCode = "225"),
        Country(code = "SN", name = "Sénégal", dialCode = "221"),
        Country(code = "CM", name = "Cameroun", dialCode = "237"),
        Country(code = "ZA", name = "Afrique du Sud", dialCode = "27")
    )

    suspend fun getCountries(): List<Country> = withContext(Dispatchers.IO) {
        try {
            val response = ApiClient.apiService.getCountries()
            if (response.isSuccessful && response.body() != null) {
                val list = response.body()!!.countries
                if (list.isNotEmpty()) {
                    return@withContext list
                }
            }
            defaultCountries
        } catch (e: Exception) {
            defaultCountries
        }
    }

    // --- Authentication ---

    private fun extractErrorMessage(errBody: String, defaultMsg: String): String {
        if (errBody.isBlank()) return defaultMsg
        return try {
            val json = org.json.JSONObject(errBody)
            val msg = json.optString("message")
            if (msg.isNotBlank()) msg
            else {
                val err = json.optString("error")
                if (err.isNotBlank()) err
                else {
                    val detail = json.optString("detail")
                    if (detail.isNotBlank()) detail else defaultMsg
                }
            }
        } catch (_: Exception) {
            defaultMsg
        }
    }

    suspend fun loginWithWalletId(walletId: String): Result<AuthResponse> = withContext(Dispatchers.IO) {
        val trimmed = walletId.trim()
        if (trimmed.isEmpty()) {
            return@withContext Result.failure(
                AuthException("Wallet requis", "Veuillez saisir votre Wallet ID.")
            )
        }

        val candidates = listOf(trimmed, trimmed.uppercase(), trimmed.lowercase()).distinct()
        var lastError: AuthException? = null

        for (candidate in candidates) {
            val payloads = listOf(
                mapOf("walletId" to candidate),
                mapOf("wallet_id" to candidate),
                mapOf("phoneOrWalletId" to candidate)
            )

            for (payload in payloads) {
                try {
                    val response = ApiClient.apiService.login(payload)
                    if (response.isSuccessful && response.body() != null) {
                        val body = response.body()!!
                        if (body.success || body.userId != null || body.user != null || !body.availableChannels.isNullOrEmpty() || !body.token.isNullOrBlank()) {
                            Log.d("AuthAPI", "loginWithWalletId SUCCESS with $payload -> userId=${body.userId ?: body.user?.id}")
                            return@withContext Result.success(body)
                        } else if (!body.error.isNullOrBlank() || !body.message.isNullOrBlank()) {
                            lastError = AuthException("Wallet introuvable", body.message ?: body.error ?: "Wallet ID non reconnu.")
                        }
                    } else {
                        val errCode = response.code()
                        val errBody = response.errorBody()?.string().orEmpty()
                        Log.d("AuthAPI", "login wallet $payload HTTP $errCode: $errBody")
                        val serverMsg = extractErrorMessage(errBody, "Wallet ID non reconnu.")
                        lastError = AuthException("Wallet introuvable", serverMsg)
                    }
                } catch (e: java.io.IOException) {
                    return@withContext Result.failure(
                        AuthException("Connexion impossible", "Impossible de joindre le serveur CashPay. Vérifiez votre connexion Internet.")
                    )
                } catch (e: Exception) {
                    Log.w("AuthAPI", "Exception trying wallet $payload: ${e.message}")
                }
            }
        }

        Result.failure(
            lastError ?: AuthException("Wallet introuvable", "Nous n'avons trouvé aucun compte CashPay associé à ce Wallet ID.")
        )
    }

    suspend fun loginWithPhone(phoneInput: String): Result<AuthResponse> = withContext(Dispatchers.IO) {
        val raw = phoneInput.trim()
        val digitsOnly = raw.replace("+", "").filter { it.isDigit() }
        if (digitsOnly.length < 5) {
            return@withContext Result.failure(
                AuthException("Numéro requis", "Veuillez saisir un numéro de téléphone valide.")
            )
        }

        // Build list of all phone representations to try
        val candidates = mutableListOf<String>()
        // 1. With '+' prefix (e.g. +243833248614)
        candidates.add("+$digitsOnly")
        // 2. Digits only without '+' (e.g. 243833248614)
        candidates.add(digitsOnly)
        // 3. Raw input
        if (raw !in candidates) candidates.add(raw)
        // 4. Local national format (e.g. 0833248614 or 833248614)
        if (digitsOnly.startsWith("243") && digitsOnly.length > 3) {
            val localWithout243 = digitsOnly.removePrefix("243")
            candidates.add("0$localWithout243")
            candidates.add(localWithout243)
        }

        var lastError: AuthException? = null

        for (candidate in candidates.distinct()) {
            val payloads = listOf(
                mapOf("phone" to candidate),
                mapOf("phoneOrWalletId" to candidate),
                mapOf("phoneNumber" to candidate)
            )

            for (payload in payloads) {
                try {
                    val response = ApiClient.apiService.login(payload)
                    if (response.isSuccessful && response.body() != null) {
                        val body = response.body()!!
                        if (body.success || body.userId != null || body.user != null || !body.availableChannels.isNullOrEmpty() || !body.token.isNullOrBlank()) {
                            Log.d("AuthAPI", "loginWithPhone SUCCESS with $payload -> userId=${body.userId ?: body.user?.id}")
                            return@withContext Result.success(body)
                        } else if (!body.error.isNullOrBlank() || !body.message.isNullOrBlank()) {
                            lastError = AuthException("Identification impossible", body.message ?: body.error ?: "Compte introuvable.")
                        }
                    } else {
                        val errCode = response.code()
                        val errBody = response.errorBody()?.string().orEmpty()
                        Log.d("AuthAPI", "login phone $payload HTTP $errCode: $errBody")
                        val serverMsg = extractErrorMessage(errBody, "Nous n'avons trouvé aucun compte CashPay associé à ce numéro de téléphone.")
                        lastError = AuthException("Numéro introuvable", serverMsg)
                    }
                } catch (e: java.io.IOException) {
                    return@withContext Result.failure(
                        AuthException("Connexion impossible", "Impossible de joindre le serveur CashPay. Vérifiez votre connexion Internet.")
                    )
                } catch (e: Exception) {
                    Log.w("AuthAPI", "Exception trying phone $payload: ${e.message}")
                }
            }
        }

        Result.failure(
            lastError ?: AuthException("Numéro introuvable", "Nous n'avons trouvé aucun compte CashPay associé à ce numéro de téléphone.")
        )
    }

    suspend fun sendOtp(userId: Long, channel: String): Result<OtpSendResponse> = withContext(Dispatchers.IO) {
        try {
            val response = ApiClient.apiService.sendOtp(OtpSendRequest(userId, channel))
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                if (body.success || body.error == null) {
                    Result.success(body)
                } else {
                    Result.failure(
                        AuthException("Méthode indisponible", "Cette méthode de vérification n'est pas disponible pour votre compte.")
                    )
                }
            } else {
                val code = response.code()
                if (code == 404 || code == 400) {
                    Result.failure(
                        AuthException("Méthode indisponible", "Cette méthode de vérification n'est pas disponible pour votre compte.")
                    )
                } else {
                    Result.failure(
                        AuthException("Erreur d'envoi", "Impossible d'envoyer le code de vérification. Veuillez réessayer.")
                    )
                }
            }
        } catch (e: java.io.IOException) {
            Result.failure(
                AuthException("Connexion impossible", "Impossible de joindre le serveur CashPay. Vérifiez votre connexion Internet.")
            )
        } catch (e: Exception) {
            Result.failure(
                AuthException("Méthode indisponible", "Cette méthode de vérification n'est pas disponible pour votre compte.")
            )
        }
    }

    suspend fun verifyOtp(userId: Long, otp: String, walletId: String): Result<AuthResponse> = withContext(Dispatchers.IO) {
        try {
            val response = ApiClient.apiService.verifyOtp(OtpVerifyRequest(userId, otp))
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                val bodyToken = body.token?.ifBlank { null }
                val headerCookies = response.headers().values("Set-Cookie")
                val cookieToken = headerCookies.firstOrNull { it.contains("cashpay-token") }?.let {
                    Regex("""cashpay-token=([^;]+)""").find(it)?.groupValues?.get(1)?.trim()
                }
                val token = bodyToken ?: cookieToken
                if (!token.isNullOrBlank()) {
                    ApiClient.authToken = token
                    SessionTokenStore.saveToken(token)
                }

                if (body.success || body.token != null || body.userId != null) {
                    Result.success(body)
                } else {
                    Result.failure(
                        AuthException("Code incorrect", "Le code de vérification saisi est incorrect.")
                    )
                }
            } else {
                val code = response.code()
                val rawErr = response.errorBody()?.string().orEmpty().lowercase()
                if (rawErr.contains("expire") || code == 410) {
                    Result.failure(
                        AuthException("Code expiré", "Demandez un nouveau code de vérification.")
                    )
                } else {
                    Result.failure(
                        AuthException("Code incorrect", "Le code de vérification saisi est incorrect.")
                    )
                }
            }
        } catch (e: java.io.IOException) {
            Result.failure(
                AuthException("Connexion impossible", "Impossible de joindre le serveur CashPay. Vérifiez votre connexion Internet.")
            )
        } catch (e: Exception) {
            Result.failure(
                AuthException("Code incorrect", "Le code de vérification saisi est incorrect.")
            )
        }
    }

    suspend fun verifyPin(userId: Long, pin: String, walletId: String): Result<UserSessionEntity> = withContext(Dispatchers.IO) {
        try {
            val response = ApiClient.apiService.verifyPin(PinVerifyRequest(userId, pin))
            val bodyToken = response.body()?.token?.ifBlank { null }
            val headerCookies = response.headers().values("Set-Cookie")
            val cookieToken = headerCookies.firstOrNull { it.contains("cashpay-token") }?.let {
                Regex("""cashpay-token=([^;]+)""").find(it)?.groupValues?.get(1)?.trim()
            }
            val token = bodyToken ?: cookieToken

            if (response.isSuccessful && response.body()?.success == true && !token.isNullOrBlank()) {
                ApiClient.clearSession()
                ApiClient.authToken = token
                SessionTokenStore.saveToken(token)

                val userObj = response.body()?.user
                val finalWalletId = userObj?.resolvedWalletId?.ifBlank { null } ?: walletId
                ApiClient.activeWalletId = finalWalletId
                SessionTokenStore.saveWalletId(finalWalletId)

                val session = UserSessionEntity(
                    id = 1,
                    token = token,
                    userId = userObj?.id ?: userId,
                    walletId = finalWalletId,
                    name = userObj?.resolvedFullName ?: "Utilisateur OnChat",
                    phone = userObj?.phone ?: "",
                    email = userObj?.email ?: "",
                    avatarUrl = userObj?.resolvedAvatar,
                    profession = userObj?.profession,
                    city = userObj?.city,
                    country = userObj?.country,
                    biometricEnabled = false
                )
                sessionDao.saveSession(session)
                Result.success(session)
            } else {
                val code = response.code()
                val rawErr = response.errorBody()?.string().orEmpty().lowercase()
                if (code == 429 || rawErr.contains("tentative") || rawErr.contains("too many") || rawErr.contains("limit")) {
                    Result.failure(
                        AuthException("Trop de tentatives", "Trop de tentatives. Veuillez patienter avant de réessayer.")
                    )
                } else if (code == 401 || rawErr.contains("session") || rawErr.contains("expire")) {
                    Result.failure(
                        AuthException("Session de connexion expirée", "Veuillez recommencer la connexion.")
                    )
                } else {
                    Result.failure(
                        AuthException("PIN incorrect", "Le PIN saisi est incorrect.")
                    )
                }
            }
        } catch (e: java.io.IOException) {
            Result.failure(
                AuthException("Connexion impossible", "Impossible de joindre le serveur CashPay. Vérifiez votre connexion Internet.")
            )
        } catch (e: Exception) {
            Result.failure(
                AuthException("PIN incorrect", "Le PIN saisi est incorrect.")
            )
        }
    }

    suspend fun checkSession(): Boolean = withContext(Dispatchers.IO) {
        val local = sessionDao.getSession() ?: return@withContext false

        ApiClient.authToken = local.token.ifBlank { null }
        ApiClient.activeWalletId = local.walletId

        try {
            val phoneToQuery = local.phone.ifBlank { null }
            val response = ApiClient.apiService.getAuthMe(phone = phoneToQuery)
            if (response.isSuccessful && response.body()?.success == true) {
                val updatedUser = response.body()?.resolvedUser
                val astro = response.body()?.astro
                if (updatedUser != null) {
                    sessionDao.saveSession(
                        local.copy(
                            name = updatedUser.resolvedFullName,
                            phone = updatedUser.phone ?: local.phone,
                            email = updatedUser.email ?: local.email,
                            avatarUrl = updatedUser.resolvedAvatar ?: local.avatarUrl,
                            coverUrl = updatedUser.resolvedCover ?: local.coverUrl,
                            bio = updatedUser.resolvedActivity ?: local.bio,
                            profession = updatedUser.profession ?: local.profession,
                            activityDescription = updatedUser.resolvedActivity ?: local.activityDescription,
                            address = updatedUser.address ?: local.address,
                            city = updatedUser.city ?: local.city,
                            province = updatedUser.province ?: local.province,
                            country = updatedUser.country ?: local.country,
                            nationality = updatedUser.nationality ?: local.nationality,
                            language = updatedUser.language ?: local.language,
                            birthDate = updatedUser.resolvedBirthDate ?: local.birthDate,
                            verificationStatus = updatedUser.resolvedVerificationStatus ?: local.verificationStatus,
                            availabilityStatus = updatedUser.resolvedAvailabilityStatus,
                            astroSign = astro?.sign ?: local.astroSign,
                            astroElement = astro?.element ?: local.astroElement,
                            astroDescription = astro?.description ?: local.astroDescription,
                            astroLuckyNumber = astro?.luckyNumber ?: local.astroLuckyNumber,
                            astroAscendant = astro?.ascendant ?: local.astroAscendant
                        )
                    )
                }
            }
            return@withContext true
        } catch (e: Exception) {
            // If offline or network issue, maintain local cached session
            return@withContext true
        }
    }

    // Fast in-memory cache for verified contacts to prevent repeated network hits and UI lag
    private val contactCheckCache = java.util.concurrent.ConcurrentHashMap<String, ContactCheckResult>()

    suspend fun fetchAuthMe(phoneParam: String? = null): Result<UserSessionEntity> = withContext(Dispatchers.IO) {
        val local = sessionDao.getSession()
        val phoneToUse = phoneParam?.ifBlank { null } ?: local?.phone?.ifBlank { null }

        // 1. Try GET /api/v1/auth/me?phone=...
        try {
            val response = ApiClient.apiService.getAuthMe(phone = phoneToUse)
            if (response.isSuccessful && response.body()?.resolvedUser != null) {
                val updatedUser = response.body()!!.resolvedUser!!
                val astro = response.body()?.astro ?: updatedUser.astro
                val installation = response.body()?.profileInstallation
                val finalWalletId = updatedUser.resolvedWalletId
                    .ifBlank { installation?.walletId ?: local?.walletId ?: "" }
                val finalAvatar = updatedUser.resolvedAvatar
                    ?.ifBlank { installation?.avatar?.url ?: local?.avatarUrl }
                    ?: installation?.avatar?.url ?: local?.avatarUrl

                val finalUserId = installation?.userId ?: updatedUser.id

                val updatedSession = UserSessionEntity(
                    id = 1,
                    token = local?.token ?: ApiClient.authToken ?: SessionTokenStore.getToken() ?: "",
                    userId = finalUserId,
                    walletId = finalWalletId,
                    name = updatedUser.resolvedFullName,
                    phone = updatedUser.phone ?: phoneToUse ?: local?.phone ?: "",
                    email = updatedUser.email ?: local?.email ?: "",
                    avatarUrl = finalAvatar,
                    coverUrl = updatedUser.resolvedCover ?: installation?.cover?.url ?: local?.coverUrl,
                    bio = updatedUser.resolvedActivity ?: local?.bio,
                    profession = updatedUser.profession ?: local?.profession,
                    activityDescription = updatedUser.resolvedActivity ?: local?.activityDescription,
                    address = updatedUser.address ?: local?.address,
                    city = updatedUser.city ?: local?.city,
                    province = updatedUser.province ?: local?.province,
                    country = updatedUser.country ?: local?.country,
                    nationality = updatedUser.nationality ?: local?.nationality,
                    language = updatedUser.language ?: local?.language,
                    birthDate = updatedUser.resolvedBirthDate ?: local?.birthDate,
                    birthPlace = updatedUser.birthPlace ?: local?.birthPlace,
                    googleId = updatedUser.googleId ?: local?.googleId,
                    zoomId = updatedUser.zoomId ?: local?.zoomId,
                    verificationStatus = updatedUser.resolvedVerificationStatus ?: local?.verificationStatus,
                    availabilityStatus = updatedUser.resolvedAvailabilityStatus,
                    astroSign = astro?.resolvedSign ?: astro?.sign ?: local?.astroSign,
                    astroElement = astro?.element ?: local?.astroElement,
                    astroSymbol = astro?.resolvedSymbol ?: local?.astroSymbol,
                    astroNameFr = astro?.zodiac?.nameFr ?: local?.astroNameFr,
                    astroDescription = astro?.description ?: local?.astroDescription,
                    astroLuckyNumber = astro?.luckyNumber ?: local?.astroLuckyNumber,
                    astroAscendant = astro?.ascendant ?: local?.astroAscendant,
                    biometricEnabled = local?.biometricEnabled ?: false
                )
                sessionDao.saveSession(updatedSession)
                ApiClient.activeWalletId = updatedSession.walletId
                SessionTokenStore.saveWalletId(updatedSession.walletId)
                return@withContext Result.success(updatedSession)
            }
        } catch (_: Exception) {}

        // 2. If phone is available, try GET /api/v1/profile/by-phone?phone=...
        if (!phoneToUse.isNullOrBlank()) {
            try {
                val nineDigits = cleanPhoneNumber9Digits(phoneToUse)
                val phoneQuery = if (nineDigits.length == 9) nineDigits else phoneToUse.filter { it.isDigit() }
                val phoneProfileResp = ApiClient.apiService.getProfileByPhone(phoneQuery)
                if (phoneProfileResp.isSuccessful && phoneProfileResp.body()?.found == true) {
                    val p = phoneProfileResp.body()!!.profile
                    if (p != null) {
                        val finalWalletId = p.resolvedWalletId.takeIf { it != "CD0100000000" } ?: local?.walletId ?: ""
                        val updatedSession = (local ?: UserSessionEntity(
                            id = 1,
                            token = ApiClient.authToken ?: SessionTokenStore.getToken() ?: "",
                            userId = p.id ?: local?.userId ?: 1L,
                            walletId = finalWalletId,
                            name = p.resolvedFullName,
                            phone = p.phone ?: phoneToUse,
                            email = p.email ?: "",
                            avatarUrl = p.resolvedAvatar ?: local?.avatarUrl,
                            biometricEnabled = false
                        )).copy(
                            userId = p.id ?: local?.userId ?: 1L,
                            walletId = finalWalletId,
                            name = p.resolvedFullName,
                            phone = p.phone ?: phoneToUse,
                            email = p.email ?: local?.email ?: "",
                            avatarUrl = p.resolvedAvatar ?: local?.avatarUrl,
                            coverUrl = p.resolvedCover ?: local?.coverUrl,
                            bio = p.resolvedActivity ?: local?.bio,
                            profession = p.profession ?: local?.profession,
                            activityDescription = p.resolvedActivity ?: local?.activityDescription,
                            address = p.address ?: local?.address,
                            city = p.city ?: local?.city,
                            province = p.province ?: local?.province,
                            country = p.country ?: local?.country,
                            birthDate = p.resolvedBirthDate ?: local?.birthDate,
                            verificationStatus = p.resolvedVerificationStatus ?: local?.verificationStatus
                        )
                        sessionDao.saveSession(updatedSession)
                        ApiClient.activeWalletId = updatedSession.walletId
                        SessionTokenStore.saveWalletId(updatedSession.walletId)
                        return@withContext Result.success(updatedSession)
                    }
                }
            } catch (_: Exception) {}
        }

        // 3. Fallback: Keep local session (e.g. from verifyPin) so user is never rejected on low connectivity
        if (local != null) {
            Result.success(local)
        } else {
            Result.failure(Exception("Session introuvable sur le serveur CashPay"))
        }
    }

    suspend fun logout() = withContext(Dispatchers.IO) {
        sessionDao.clearSession()
        conversationDao.clearAllConversations()
        messageDao.clearAllMessages()
        callDao.clearCalls()
        com.example.data.api.OnChatWebSocketManager.getInstance().disconnect()
        ApiClient.clearSession()
        android.os.Handler(android.os.Looper.getMainLooper()).post { android.webkit.CookieManager.getInstance().removeAllCookies(null) }
        SessionTokenStore.clear()
        contactCheckCache.clear()
        readConversationsSet.clear()
        java.io.File(context.cacheDir, "attachments").deleteRecursively()
    }

    suspend fun getTransactions(limit: Int = 50, offset: Int = 0): Result<TransactionsResponse> = withContext(Dispatchers.IO) {
        val session = sessionDao.getSession() ?: return@withContext Result.failure(Exception("Non connecté"))
        try {
            val response = ApiClient.apiService.getTransactions(limit, offset)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Erreur lors de la récupération de l'historique"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun executeGlobalTransfer(
        receiverWalletId: String?,
        recipientAddress: String?,
        amount: Double,
        currency: String,
        pin: String
    ): Result<TransferResponse> = withContext(Dispatchers.IO) {
        val session = sessionDao.getSession() ?: return@withContext Result.failure(Exception("Non connecté"))
        val userId = session.userId
        try {
            val response = ApiClient.apiService.executeGlobalTransfer(
                TransferRequest(
                    receiverWalletId = receiverWalletId,
                    recipientAddress = recipientAddress,
                    amount = amount,
                    currency = currency,
                    pin = pin
                )
            )
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                if (body.isTransferSuccess) {
                    // Refresh wallet to update local balances
                    fetchWallet()
                    Result.success(body)
                } else {
                    val errMsg = body.error ?: body.message ?: "Code PIN incorrect."
                    Result.failure(Exception(errMsg))
                }
            } else {
                val errBody = response.errorBody()?.string() ?: ""
                val parsed = parseTransferResponseJson(errBody)
                val errMsg = parsed?.error ?: parsed?.message ?: if (response.code() == 401 || response.code() == 400) "Code PIN incorrect." else if (errBody.isNotBlank()) errBody else "Échec du transfert"
                Result.failure(Exception(errMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchWallet(): WalletApiResponse? = withContext(Dispatchers.IO) {
        val session = sessionDao.getSession() ?: return@withContext null
        try {
            val response = ApiClient.apiService.getWallet()
            if (response.isSuccessful && response.body() != null) {
                val data = response.body()!!
                // Synchronize session profile if wallet response contains updated user/wallet details
                val current = session
                val updatedWalletId = data.wallet?.walletId ?: data.user?.walletId ?: current.walletId
                val updatedName = data.user?.fullName ?: current.name
                val updatedAvatar = data.user?.profilePhotoUrl ?: data.user?.profilePhoto ?: current.avatarUrl
                val updatedPhone = data.user?.phone ?: current.phone
                val updatedCountry = data.user?.country ?: current.country
                
                if (updatedWalletId != current.walletId || updatedName != current.name || updatedAvatar != current.avatarUrl) {
                    sessionDao.saveSession(
                        current.copy(
                            walletId = updatedWalletId,
                            name = updatedName,
                            avatarUrl = updatedAvatar,
                            phone = updatedPhone,
                            country = updatedCountry
                        )
                    )
                }
                return@withContext data
            }
            null
        } catch (e: Exception) {
            null
        }
    }

    suspend fun setBiometricEnabled(enabled: Boolean) = withContext(Dispatchers.IO) {
        sessionDao.setBiometricEnabled(enabled)
    }

    suspend fun setChatBackgroundUri(uri: String?) = withContext(Dispatchers.IO) {
        sessionDao.setChatBackgroundUri(uri)
    }

    // --- Contacts Check & Phone Profile Lookup ---

    /**
     * Nettoie le numéro (supprime espaces, parenthèses, tirets et caractères non numériques)
     * et extrait rigoureusement les 9 derniers chiffres conformément aux spécifications CashPay All.
     */
    fun cleanPhoneNumber9Digits(phone: String): String {
        val digitsOnly = phone.filter { it.isDigit() }
        return if (digitsOnly.length > 9) {
            digitsOnly.takeLast(9)
        } else {
            digitsOnly
        }
    }

    /**
     * Recherche de profil par numéro via l'endpoint public /api/v1/profile/by-phone?phone=...
     * Cet endpoint public ne nécessite aucune clé développeur.
     */
    suspend fun searchProfileByPhone(rawPhone: String): Result<ContactCheckResult> = withContext(Dispatchers.IO) {
        val nineDigits = cleanPhoneNumber9Digits(rawPhone)
        if (nineDigits.isEmpty()) {
            return@withContext Result.failure(Exception("Veuillez saisir un numéro de téléphone valide"))
        }

        // 1. Fast in-memory cache check (0ms response)
        contactCheckCache[nineDigits]?.let { cached ->
            return@withContext Result.success(cached.copy(phone = rawPhone))
        }

        // 2. Primary targeted query: 9 digits or raw digits
        val primaryQuery = if (nineDigits.length == 9) nineDigits else rawPhone.filter { it.isDigit() }
        try {
            val response = kotlinx.coroutines.withTimeoutOrNull(2500L) {
                ApiClient.apiService.getProfileByPhone(primaryQuery)
            }
            if (response != null && response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                if (body.found && body.profile != null) {
                    val p = body.profile
                    val btc = p.resolvedBitcoinAddress
                        ?: body.bitcoinAddress
                        ?: body.bitcoin_address
                        ?: body.btcAddress
                    val wId = p.resolvedWalletId.takeIf { it != "CD0100000000" }
                        ?: body.walletId
                        ?: body.wallet_id
                        ?: p.resolvedWalletId

                    val result = ContactCheckResult(
                        phone = rawPhone,
                        onChat = true,
                        name = p.resolvedFullName,
                        avatar = p.resolvedAvatar,
                        userId = p.id,
                        walletId = wId,
                        directBitcoinAddress = btc,
                        profile = p
                    )
                    contactCheckCache[nineDigits] = result
                    return@withContext Result.success(result)
                }
            }
        } catch (_: Exception) {}

        // Fallback: If full digits differ from primary query (e.g. country code 243...)
        val allDigits = rawPhone.filter { it.isDigit() }
        if (allDigits.isNotBlank() && allDigits != primaryQuery) {
            try {
                val response = kotlinx.coroutines.withTimeoutOrNull(2000L) {
                    ApiClient.apiService.getProfileByPhone(allDigits)
                }
                if (response != null && response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    if (body.found && body.profile != null) {
                        val p = body.profile
                        val btc = p.resolvedBitcoinAddress
                            ?: body.bitcoinAddress
                            ?: body.bitcoin_address
                            ?: body.btcAddress
                        val wId = p.resolvedWalletId.takeIf { it != "CD0100000000" }
                            ?: body.walletId
                            ?: body.wallet_id
                            ?: p.resolvedWalletId

                        val result = ContactCheckResult(
                            phone = rawPhone,
                            onChat = true,
                            name = p.resolvedFullName,
                            avatar = p.resolvedAvatar,
                            userId = p.id,
                            walletId = wId,
                            directBitcoinAddress = btc,
                            profile = p
                        )
                        contactCheckCache[nineDigits] = result
                        return@withContext Result.success(result)
                    }
                }
            } catch (_: Exception) {}
        }

        // Not registered on OnChat
        val notFound = ContactCheckResult(
            phone = rawPhone,
            onChat = false,
            name = null,
            avatar = null,
            userId = null,
            walletId = null,
            directBitcoinAddress = null,
            profile = null
        )
        contactCheckCache[nineDigits] = notFound
        Result.success(notFound)
    }

    /**
     * Progressive streaming contact verification with Semaphore(8) and time-based debouncing
     * to eliminate screen flickering and provide instant response on mobile connections.
     */
    fun checkContactsStream(phoneNumbers: List<String>): kotlinx.coroutines.flow.Flow<List<ContactCheckResult>> = kotlinx.coroutines.flow.flow {
        val distinctPhones = phoneNumbers.filter { it.isNotBlank() }.distinctBy { cleanPhoneNumber9Digits(it) }
        val currentResults = mutableListOf<ContactCheckResult>()

        // 1. Partition cached vs to fetch
        val (cached, toFetch) = distinctPhones.partition {
            val key = cleanPhoneNumber9Digits(it)
            contactCheckCache.containsKey(key)
        }

        cached.forEach { phone ->
            contactCheckCache[cleanPhoneNumber9Digits(phone)]?.let {
                currentResults.add(it.copy(phone = phone))
            }
        }
        if (currentResults.isNotEmpty()) {
            emit(currentResults.toList())
        }

        if (toFetch.isEmpty()) return@flow

        // 2. Process non-cached contacts in safe chunks of 10
        val chunks = toFetch.chunked(10)
        for (chunk in chunks) {
            try {
                val batchResults = coroutineScope {
                    chunk.map { rawPhone ->
                        async {
                            try {
                                searchProfileByPhone(rawPhone).getOrElse {
                                    ContactCheckResult(phone = rawPhone, onChat = false)
                                }
                            } catch (e: Throwable) {
                                ContactCheckResult(phone = rawPhone, onChat = false)
                            }
                        }
                    }.awaitAll()
                }
                currentResults.addAll(batchResults)
                emit(currentResults.toList())
            } catch (e: Throwable) {
                // Catch any coroutine or network error gracefully
            }
        }
    }.catch { e ->
        // Suppress flow exceptions
    }.flowOn(Dispatchers.IO)

    suspend fun checkContacts(phoneNumbers: List<String>): Result<List<ContactCheckResult>> = withContext(Dispatchers.IO) {
        val distinctPhones = phoneNumbers.distinctBy { cleanPhoneNumber9Digits(it) }
        val semaphore = kotlinx.coroutines.sync.Semaphore(5)
        coroutineScope {
            val jobs = distinctPhones.map { rawPhone ->
                async {
                    semaphore.acquire()
                    try {
                        val searchRes = searchProfileByPhone(rawPhone)
                        if (searchRes.isSuccess) {
                            searchRes.getOrThrow()
                        } else {
                            ContactCheckResult(
                                phone = rawPhone,
                                onChat = false,
                                name = null,
                                avatar = null,
                                userId = null,
                                walletId = null
                            )
                        }
                    } finally {
                        semaphore.release()
                    }
                }
            }
            val verifiedList = jobs.awaitAll()
            Result.success(verifiedList)
        }
    }

    // --- Groups ---

    suspend fun fetchGroups(): Result<List<Group>> = withContext(Dispatchers.IO) {
        try {
            val response = ApiClient.apiService.getGroups()
            if (response.isSuccessful && response.body()?.success == true) {
                val remoteGroups = response.body()?.groups ?: emptyList()
                val entities = remoteGroups.map {
                    GroupEntity(
                        id = it.id,
                        name = it.name,
                        currency = it.currency,
                        description = it.description,
                        photoUrl = it.photoUrl,
                        membersCount = it.membersCount
                    )
                }
                groupDao.insertGroups(entities)
                Result.success(remoteGroups)
            } else {
                // Fallback to local DB
                val local = groupDao.getGroupsFlow().firstOrNull() ?: emptyList()
                Result.success(local.map {
                    Group(it.id, it.name, it.currency, it.description, it.photoUrl, it.membersCount)
                })
            }
        } catch (e: Exception) {
            val local = groupDao.getGroupsFlow().firstOrNull() ?: emptyList()
            Result.success(local.map {
                Group(it.id, it.name, it.currency, it.description, it.photoUrl, it.membersCount)
            })
        }
    }

    suspend fun createGroup(name: String, currency: String, description: String?, photoUrl: String?): Result<Group> = withContext(Dispatchers.IO) {
        try {
            val response = ApiClient.apiService.createGroup(CreateGroupRequest(name, currency, description, photoUrl))
            if (response.isSuccessful && response.body()?.group != null) {
                val group = response.body()!!.group!!
                groupDao.insertGroup(GroupEntity(group.id, group.name, group.currency, group.description, group.photoUrl, group.membersCount))
                Result.success(group)
            } else {
                val newId = System.currentTimeMillis() % 100000
                val fallbackGroup = Group(newId, name, currency, description, photoUrl, 1)
                groupDao.insertGroup(GroupEntity(newId, name, currency, description, photoUrl, 1))
                Result.success(fallbackGroup)
            }
        } catch (e: Exception) {
            val newId = System.currentTimeMillis() % 100000
            val fallbackGroup = Group(newId, name, currency, description, photoUrl, 1)
            groupDao.insertGroup(GroupEntity(newId, name, currency, description, photoUrl, 1))
            Result.success(fallbackGroup)
        }
    }

    suspend fun addMemberToGroup(groupId: Long, userId: Long): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val response = ApiClient.apiService.addGroupMember(groupId, AddMemberRequest(userId))
            Result.success(response.isSuccessful)
        } catch (e: Exception) {
            Result.success(true)
        }
    }

    suspend fun removeMemberFromGroup(groupId: Long, memberId: Long): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val response = ApiClient.apiService.removeGroupMember(groupId, memberId)
            Result.success(response.isSuccessful)
        } catch (e: Exception) {
            Result.success(true)
        }
    }

    // --- Conversations & Server Synchronization ---

    suspend fun getPublicProfile(phone: String): Result<PhoneProfileResponse> = withContext(Dispatchers.IO) {
        try {
            val nineDigits = cleanPhoneNumber9Digits(phone)
            val phoneQuery = if (nineDigits.length == 9) nineDigits else phone.filter { it.isDigit() }
            val response = ApiClient.apiService.getProfileByPhone(phoneQuery)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Profil introuvable"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun performSocialAction(userId: Long, action: String, relationType: String? = null): Result<SocialActionResponse> = withContext(Dispatchers.IO) {
        try {
            val response = ApiClient.apiService.performSocialAction(SocialActionRequest(userId, action, relationType))
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Action échouée"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchConversations(): Result<List<Conversation>> = withContext(Dispatchers.IO) {
        // Sync with verified route: GET /api/v1/chat/conversations
        try {
            val response = ApiClient.apiService.getConversations()
            if (response.isSuccessful && response.body() != null) {
                val apiList = response.body()?.conversations ?: emptyList()
                val entities = apiList.map { apiConv ->
                    val contact = apiConv.contact
                    ConversationEntity(
                        id = apiConv.id,
                        name = contact?.displayName ?: apiConv.groupName ?: "Discussion",
                        avatarUrl = contact?.displayAvatar,
                        lastMessage = apiConv.lastMessage?.content ?: "",
                        lastMessageTime = try {
                            apiConv.lastMessageTimestamp?.toLong() ?: System.currentTimeMillis()
                        } catch (_: Exception) {
                            System.currentTimeMillis()
                        },
                        unreadCount = if (readConversationsSet.contains(apiConv.id)) 0 else apiConv.unreadCount,
                        isGroup = apiConv.isGroup ?: false,
                        currency = apiConv.currency ?: "USD",
                        contactPhone = contact?.phone,
                        contactWalletId = contact?.walletId,
                        contactEmail = contact?.email,
                        contactBio = contact?.bio,
                        contactProfession = contact?.profession,
                        contactActivityDescription = contact?.activityDescription,
                        contactBirthDate = contact?.birthDate,
                        contactAstroSign = contact?.astro?.resolvedSign,
                        contactAstroSymbol = contact?.astro?.resolvedSymbol,
                        contactAstroNameFr = contact?.astro?.zodiac?.nameFr,
                        contactAstroElement = contact?.astro?.element,
                        contactBitcoinAddress = contact?.bitcoinAddress,
                        isMuted = false
                    )
                }
                conversationDao.clearAllConversations()
                if (entities.isNotEmpty()) {
                    conversationDao.insertConversations(entities)
                }
            }
        } catch (_: Exception) {}

        val local = conversationDao.getConversationsFlow().firstOrNull() ?: emptyList()
        Result.success(local.map { it.toDomain() })
    }

    suspend fun fetchConversationDetails(conversationId: String): Result<Unit> = withContext(Dispatchers.IO) {
        if (conversationId.startsWith("g_")) {
            val groupId = conversationId.removePrefix("g_").toLongOrNull()
                ?: return@withContext Result.failure(IllegalArgumentException("Groupe invalide"))
            try {
                val response = ApiClient.apiService.getGroupDetails(groupId)
                val group = response.body()?.group
                if (!response.isSuccessful || response.body()?.success != true || group == null) error("Discussion de groupe inaccessible")
                val localIds = messageDao.getMessagesFlow(conversationId).firstOrNull().orEmpty().map { it.id }.toSet()
                group.messages.filter { it.id !in localIds }.forEach { insertWebSocketMessage(conversationId, it) }
                return@withContext Result.success(Unit)
            } catch (e: kotlinx.coroutines.CancellationException) { throw e }
            catch (e: Exception) { return@withContext Result.failure(e) }
        }
        // Synchronize full messages and contact with confirmed route: GET /api/v1/chat/conversations/{conversationId}
        try {
            val response = ApiClient.apiService.getConversationDetails(conversationId)
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                val session = sessionDao.getSession()
                val currentUserId = session?.userId?.toString() ?: "1"
                val contact = body.resolvedContact
                val isGroup = body.isGroup ?: false

                // Refresh conversation info if possible
                if (!isGroup) {
                    val existing = conversationDao.getConversationsFlow().firstOrNull()?.find { it.id == conversationId }
                    val phoneToSearch = contact?.phone ?: existing?.contactPhone
                    if (phoneToSearch != null) {
                        val profileRes = searchProfileByPhone(phoneToSearch)
                        profileRes.onSuccess { res ->
                            if (res.onChat && res.profile != null) {
                                val p = res.profile
                                val updated = (existing ?: ConversationEntity(
                                    id = conversationId,
                                    name = p.resolvedFullName,
                                    lastMessage = "",
                                    lastMessageTime = System.currentTimeMillis()
                                )).copy(
                                    name = p.resolvedFullName,
                                    avatarUrl = p.displayAvatar,
                                    coverUrl = p.displayCover,
                                    contactPhone = phoneToSearch,
                                    contactWalletId = p.resolvedWalletId,
                                    contactProfession = p.profession,
                                    contactActivityDescription = p.resolvedActivity,
                                    contactAddress = p.address,
                                    contactCity = p.city,
                                    contactProvince = p.province,
                                    contactCountry = p.country,
                                    contactNationality = p.nationality,
                                    contactLanguage = p.language,
                                    contactBirthDate = p.resolvedBirthDate,
                                    contactVerificationStatus = p.resolvedVerificationStatus,
                                    contactAvailabilityStatus = p.availabilityStatus ?: "available",
                                    contactAstroSign = p.astro?.resolvedSign,
                                    contactAstroSymbol = p.astro?.resolvedSymbol,
                                    contactAstroNameFr = p.astro?.zodiac?.nameFr,
                                    contactAstroElement = p.astro?.element,
                                    contactBitcoinAddress = p.resolvedBitcoinAddress
                                )
                                conversationDao.insertConversation(updated)
                            }
                        }
                    }
                }

                val convEntity = conversationDao.getConversationSync(conversationId)
                val contactAvatar = convEntity?.avatarUrl
                val msgEntities = body.resolvedMessages.map { apiMsg ->
                    val isMine = apiMsg.senderId == session?.userId || (apiMsg.senderId == null && session != null)
                    val parsedId = apiMsg.id.toString()
                    val timestamp = try {
                        val raw = apiMsg.timestamp ?: apiMsg.createdAt
            raw?.toLongOrNull() ?: raw?.let { value ->
                java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX", java.util.Locale.US).parse(value)?.time
            } ?: System.currentTimeMillis()
                    } catch (_: Exception) {
                        System.currentTimeMillis()
                    }

                    var transferAmount: Double? = null
                    var transferCurrency: String? = null
                    var transferRef: String? = null

                    if (apiMsg.type == "in_chat_transfer" && apiMsg.metadata != null) {
                        transferAmount = apiMsg.metadata["amount"]?.toDoubleOrNull()
                        transferCurrency = apiMsg.metadata["currency"]
                        transferRef = apiMsg.metadata["reference"]
                    }

                    MessageEntity(
                        id = parsedId,
                        conversationId = conversationId,
                        senderId = (apiMsg.senderId?.toString()) ?: currentUserId,
                        senderWalletId = session?.walletId ?: ApiClient.activeWalletId,
                        senderName = apiMsg.senderName ?: if (isMine) "Moi" else (convEntity?.name ?: "Contact"),
                        senderAvatar = apiMsg.senderAvatar?.takeIf { it.isNotBlank() } ?: if (isMine) session?.avatarUrl else contactAvatar,
                        content = apiMsg.content,
                        timestamp = timestamp,
                        isMine = isMine,
                        status = apiMsg.status ?: "delivered",
                        isEdited = false,
                        type = apiMsg.type,
                        fileUrl = apiMsg.fileUrl,
                        fileName = apiMsg.fileName,
                        transferAmount = transferAmount,
                        transferCurrency = transferCurrency,
                        transferReference = transferRef
                    )
                }

                if (msgEntities.isNotEmpty()) {
                    messageDao.insertMessages(msgEntities)
                }
                Result.success(Unit)
            } else {
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.success(Unit)
        }
    }

    suspend fun getOrCreateConversationForContact(
        name: String,
        phone: String? = null,
        walletId: String? = null,
        avatarUrl: String? = null,
        recipientUserId: Long? = null,
        bitcoinAddress: String? = null
    ): String = withContext(Dispatchers.IO) {
        // First, check local cache for an existing conversation
        val existing = conversationDao.getConversationsFlow().firstOrNull()?.find {
            (!it.isGroup && ((walletId != null && it.contactWalletId == walletId) ||
                    (phone != null && it.contactPhone == phone) ||
                    it.name.equals(name, ignoreCase = true)))
        }

        // Resolve peer profile if phone is available to get exact walletId and profile details
        val checkRes = if (!phone.isNullOrBlank()) searchProfileByPhone(phone).getOrNull() else null
        val peerProfile = checkRes?.profile
        val targetWalletId = peerProfile?.resolvedWalletId ?: checkRes?.resolvedWalletId ?: walletId
        val targetName = peerProfile?.resolvedFullName ?: checkRes?.name ?: name
        val targetAvatar = peerProfile?.displayAvatar ?: checkRes?.avatar ?: avatarUrl
        val targetUserId = recipientUserId ?: peerProfile?.id ?: checkRes?.userId
        val targetBtc = peerProfile?.resolvedBitcoinAddress ?: checkRes?.bitcoinAddress ?: bitcoinAddress

        if (existing != null) {
            var updated = existing
            if (targetAvatar != null && updated.avatarUrl != targetAvatar) {
                updated = updated.copy(avatarUrl = targetAvatar)
            }
            if (updated.contactPhone.isNullOrBlank() && !phone.isNullOrBlank()) {
                updated = updated.copy(contactPhone = phone)
            }
            if (updated.contactWalletId.isNullOrBlank() && !targetWalletId.isNullOrBlank()) {
                updated = updated.copy(contactWalletId = targetWalletId)
            }
            if (updated.contactBitcoinAddress.isNullOrBlank() && !targetBtc.isNullOrBlank()) {
                updated = updated.copy(contactBitcoinAddress = targetBtc)
            }
            if (updated != existing) {
                conversationDao.insertConversation(updated)
            }
            return@withContext existing.id
        }

        // Call backend API to create or get the canonical conversation using POST /api/v1/chat/conversations { "walletId": "CD01..." }
        if (!targetWalletId.isNullOrBlank()) {
            try {
                val response = ApiClient.apiService.createConversation(
                    CreateConversationRequest(walletId = targetWalletId)
                )
                val resolvedConvId = response.body()?.resolvedConversationId
                if (response.isSuccessful && resolvedConvId != null) {
                    val convId = resolvedConvId
                    val apiConv = response.body()?.conversation
                    val contact = apiConv?.contact
                    val btcAddr = contact?.bitcoinAddress ?: targetBtc
                    val newConv = ConversationEntity(
                        id = convId,
                        name = contact?.displayName ?: targetName,
                        avatarUrl = contact?.displayAvatar ?: targetAvatar,
                        lastMessage = "Nouvelle discussion OnChat",
                        lastMessageTime = System.currentTimeMillis(),
                        unreadCount = 0,
                        isGroup = false,
                        currency = apiConv?.currency ?: "USD",
                        contactPhone = contact?.phone ?: phone,
                        contactWalletId = contact?.walletId ?: targetWalletId,
                        contactEmail = contact?.email ?: peerProfile?.email,
                        contactBio = contact?.bio ?: peerProfile?.resolvedActivity,
                        contactProfession = contact?.profession ?: peerProfile?.profession,
                        contactActivityDescription = contact?.activityDescription ?: peerProfile?.resolvedActivity,
                        contactBirthDate = contact?.birthDate ?: peerProfile?.resolvedBirthDate,
                        contactBitcoinAddress = btcAddr,
                        isMuted = false
                    )
                    conversationDao.insertConversation(newConv)
                    return@withContext convId
                }
            } catch (_: Exception) {}
        }

        // Canonical deterministic ID fallback
        val session = sessionDao.getSession()
        val myId = session?.userId ?: 1L
        val peerId = targetUserId ?: (targetWalletId?.hashCode()?.toLong() ?: System.currentTimeMillis())
        val canonicalId = if (myId < peerId) "conv_${myId}_${peerId}" else "conv_${peerId}_${myId}"

        val newConv = ConversationEntity(
            id = canonicalId,
            name = targetName,
            avatarUrl = targetAvatar,
            lastMessage = "Nouvelle discussion OnChat",
            lastMessageTime = System.currentTimeMillis(),
            unreadCount = 0,
            isGroup = false,
            currency = "USD",
            contactPhone = phone,
            contactWalletId = targetWalletId,
            contactEmail = peerProfile?.email,
            contactBio = peerProfile?.resolvedActivity,
            contactProfession = peerProfile?.profession,
            contactActivityDescription = peerProfile?.resolvedActivity,
            contactBirthDate = peerProfile?.resolvedBirthDate,
            contactBitcoinAddress = targetBtc,
            isMuted = false
        )
        conversationDao.insertConversation(newConv)
        canonicalId
    }

    suspend fun resolveRecipientData(
        conversationId: String,
        name: String,
        currentPhone: String?,
        currentWalletId: String?,
        currentBtcAddress: String?
    ): Pair<String?, String?> = withContext(Dispatchers.IO) {
        var walletId = currentWalletId
        var btcAddress = currentBtcAddress
        var phone = currentPhone

        if (phone.isNullOrBlank() && name.any { it.isDigit() }) {
            phone = name
        }

        if (!phone.isNullOrBlank()) {
            val res = searchProfileByPhone(phone).getOrNull()
            if (res != null) {
                if (walletId.isNullOrBlank()) {
                    walletId = res.resolvedWalletId
                }
                if (btcAddress.isNullOrBlank()) {
                    btcAddress = res.bitcoinAddress
                }
            }
        }

        val existing = conversationDao.getConversationSync(conversationId)
        if (existing != null) {
            val updated = existing.copy(
                contactPhone = phone ?: existing.contactPhone,
                contactWalletId = walletId ?: existing.contactWalletId,
                contactBitcoinAddress = btcAddress ?: existing.contactBitcoinAddress
            )
            if (updated != existing) {
                conversationDao.insertConversation(updated)
            }
        }

        Pair(walletId, btcAddress)
    }

    suspend fun getPublicBitcoinAddress(phone: String): String? = withContext(Dispatchers.IO) {
        try {
            val res = searchProfileByPhone(phone)
            res.getOrNull()?.bitcoinAddress
        } catch (_: Exception) {
            null
        }
    }

    suspend fun getOrCreateConversationForGroup(groupId: Long, groupName: String): String = withContext(Dispatchers.IO) {
        val groupConvId = "g_$groupId"
        val existing = conversationDao.getConversationsFlow().firstOrNull()?.find { it.id == groupConvId }
        if (existing != null) return@withContext existing.id

        val group = groupDao.getGroupsFlow().firstOrNull()?.find { it.id == groupId }

        val newConv = ConversationEntity(
            id = groupConvId,
            name = groupName,
            lastMessage = "Discussion de groupe démarrée",
            lastMessageTime = System.currentTimeMillis(),
            unreadCount = 0,
            isGroup = true,
            currency = group?.currency ?: "USD"
        )
        conversationDao.insertConversation(newConv)
        groupConvId
    }

    // --- Messages ---

    fun getMessages(conversationId: String): Flow<List<Message>> {
        return messageDao.getMessagesFlow(conversationId).map { list ->
            list.map { entity ->
                Message(
                    id = entity.id,
                    conversationId = entity.conversationId,
                    senderId = entity.senderId,
                    senderWalletId = entity.senderWalletId,
                    senderName = entity.senderName,
                    senderAvatar = entity.senderAvatar,
                    content = entity.content,
                    timestamp = entity.timestamp,
                    isMine = entity.isMine,
                    status = entity.status,
                    isEdited = entity.isEdited,
                    type = entity.type,
                    metadataJson = entity.metadataJson,
                    fileUrl = entity.fileUrl,
                    fileName = entity.fileName,
                    durationSeconds = entity.durationSeconds,
                    fileSizeBytes = entity.fileSizeBytes,
                    transferAmount = entity.transferAmount,
                    transferCurrency = entity.transferCurrency,
                    transferReference = entity.transferReference
                )
            }
        }
    }

    suspend fun insertWebSocketMessage(
        conversationId: String,
        apiMsg: ApiMessage
    ) = withContext(Dispatchers.IO) {
        val session = sessionDao.getSession()
        val currentUserId = session?.userId?.toString() ?: "1"
        val isMine = apiMsg.senderId == session?.userId

        val timestamp = try {
            val raw = apiMsg.timestamp ?: apiMsg.createdAt
            raw?.toLongOrNull() ?: raw?.let { value ->
                java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX", java.util.Locale.US).parse(value)?.time
            } ?: System.currentTimeMillis()
        } catch (_: Exception) {
            System.currentTimeMillis()
        }

        var transferAmount: Double? = null
        var transferCurrency: String? = null
        var transferRef: String? = null

        if (apiMsg.type == "in_chat_transfer" && apiMsg.metadata != null) {
            transferAmount = apiMsg.metadata["amount"]?.toDoubleOrNull()
            transferCurrency = apiMsg.metadata["currency"]
            transferRef = apiMsg.metadata["reference"]
        }

        val convEntity = conversationDao.getConversationSync(conversationId)
        val entity = MessageEntity(
            id = apiMsg.id,
            conversationId = conversationId,
            senderId = (apiMsg.senderId?.toString()) ?: (if (isMine) currentUserId else "contact"),
            senderWalletId = session?.walletId ?: ApiClient.activeWalletId,
            senderName = apiMsg.senderName ?: if (isMine) "Moi" else (convEntity?.name ?: "Contact"),
            senderAvatar = apiMsg.senderAvatar?.takeIf { it.isNotBlank() } ?: if (isMine) session?.avatarUrl else convEntity?.avatarUrl,
            content = apiMsg.content,
            timestamp = timestamp,
            isMine = isMine,
            status = apiMsg.status ?: "delivered",
            isEdited = false,
            type = apiMsg.type,
            fileUrl = apiMsg.fileUrl,
            fileName = apiMsg.fileName,
            transferAmount = transferAmount,
            transferCurrency = transferCurrency,
            transferReference = transferRef
        )
        messageDao.insertMessage(entity)

        // Update conversation last message in Room so active chat moves to the top
        val conv = conversationDao.getConversationSync(conversationId)
        if (conv != null) {
            val newUnread = if (!isMine) { readConversationsSet.remove(conversationId); conv.unreadCount + 1 } else conv.unreadCount
            conversationDao.insertConversation(
                conv.copy(
                    lastMessage = apiMsg.content,
                    lastMessageTime = timestamp,
                    unreadCount = newUnread
                )
            )
        } else {
            // If conversation entry does not exist yet, create it so it shows at the top
            conversationDao.insertConversation(
                ConversationEntity(
                    id = conversationId,
                    name = apiMsg.senderName ?: "Correspondant",
                    avatarUrl = null,
                    lastMessage = apiMsg.content,
                    lastMessageTime = timestamp,
                    unreadCount = if (!isMine) 1 else 0
                )
            )
        }
    }

    suspend fun sendMessage(
        conversationId: String,
        content: String,
        type: String = "text",
        metadata: Map<String, String>? = null,
        fileUrl: String? = null,
        fileName: String? = null,
        isMine: Boolean = true
    ) = withContext(Dispatchers.IO) {
        val session = sessionDao.getSession() ?: error("Authentification requise")
        val senderId = session?.userId?.toString() ?: "1"
        val senderName = session?.name ?: "Moi"
        val now = System.currentTimeMillis()
        val messageId = "msg_$now"

        val entity = MessageEntity(
            id = messageId,
            conversationId = conversationId,
            senderId = senderId,
            senderWalletId = session?.walletId ?: ApiClient.activeWalletId,
            senderName = senderName,
            senderAvatar = session?.avatarUrl,
            content = content,
            timestamp = now,
            isMine = isMine,
            status = "sending",
            isEdited = false,
            type = type,
            metadataJson = metadata?.let { org.json.JSONObject(it).toString() },
            fileUrl = fileUrl,
            fileName = fileName
        )
        messageDao.insertMessage(entity)

        // Update conversation last message in Room so active chat moves to the top
        val conv = conversationDao.getConversationSync(conversationId)
        if (conv != null) {
            conversationDao.insertConversation(
                conv.copy(
                    lastMessage = content,
                    lastMessageTime = now
                )
            )
        } else {
            conversationDao.insertConversation(
                ConversationEntity(
                    id = conversationId,
                    name = "Discussion",
                    avatarUrl = null,
                    lastMessage = content,
                    lastMessageTime = now,
                    unreadCount = 0
                )
            )
        }

        // Post to remote server: POST /api/v1/chat/conversations/{conversationId}
        val request = SendMessageRequest(
            content = content,
            type = type,
            metadata = metadata,
            fileUrl = fileUrl ?: metadata?.get("fileUrl"),
            fileName = fileName ?: metadata?.get("fileName")
        )

        try {
            val directResp = if (conversationId.startsWith("g_")) {
                val groupId = conversationId.removePrefix("g_").toLongOrNull() ?: error("Groupe invalide")
                ApiClient.apiService.postGroupMessage(groupId, request)
            } else ApiClient.apiService.postMessage(conversationId, request)
            val body = directResp.body()
            val returnedMsg = body?.message
            if (!directResp.isSuccessful || body?.success != true || returnedMsg == null) {
                error(body?.error ?: "Échec de l'envoi (HTTP ${directResp.code()})")
            }
            if (returnedMsg.id != messageId) messageDao.deleteMessage(messageId)
            messageDao.insertMessage(entity.copy(
                id = returnedMsg.id,
                status = returnedMsg.status ?: "sent",
                metadataJson = returnedMsg.metadata?.let { org.json.JSONObject(it).toString() } ?: entity.metadataJson,
                fileUrl = returnedMsg.fileUrl ?: returnedMsg.metadata?.get("fileUrl") ?: fileUrl,
                fileName = returnedMsg.fileName ?: returnedMsg.metadata?.get("fileName") ?: fileName
            ))
        } catch (e: Exception) {
            messageDao.insertMessage(entity.copy(status = "failed"))
            throw e
        }
    }

    // --- In-Chat Financial Transfer Engine (CashPay TR) ---

    suspend fun getTransferQuote(
        conversationId: String,
        amount: Double,
        currency: String = "USD"
    ): Result<TransferQuoteResponse> = withContext(Dispatchers.IO) {
        try {
            val response = ApiClient.apiService.quoteTransfer(
                conversationId,
                TransferQuoteRequest(amount = amount, currency = currency)
            )
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errBody = response.errorBody()?.string() ?: ""
                Result.failure(Exception("Impossible d'obtenir la cotation du transfert: $errBody"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private val transferMoshi: Moshi by lazy {
        Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    }

    private fun parseTransferResponseJson(json: String): TransferResponse? {
        return try {
            transferMoshi.adapter(TransferResponse::class.java).fromJson(json)
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Preview transfer (POST /api/v1/transfers with preview: true).
     * Calculates fees and verifies balance without executing or creating a transaction.
     */
    suspend fun previewTransfer(
        receiverWalletId: String?,
        recipientAddress: String?,
        amount: Double,
        currency: String
    ): Result<TransferResponse> = withContext(Dispatchers.IO) {
        val session = sessionDao.getSession() ?: return@withContext Result.failure(Exception("Non connecté"))

        val isBtc = currency.equals("BTC", ignoreCase = true)
        val targetWallet = receiverWalletId?.takeIf { it.isNotBlank() }
        val targetBtc = (recipientAddress ?: receiverWalletId)?.takeIf { it.isNotBlank() }

        val req = TransferRequest(
            receiverWalletId = if (!isBtc) targetWallet else (targetWallet ?: targetBtc),
            recipientAddress = if (isBtc) (targetBtc ?: targetWallet) else null,
            amount = amount,
            currency = currency,
            pin = null,
            preview = true
        )

        try {
            val response = ApiClient.apiService.executeGlobalTransfer(req)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errBody = response.errorBody()?.string() ?: ""
                val parsed = parseTransferResponseJson(errBody)
                if (parsed != null) {
                    Result.success(parsed)
                } else {
                    Result.failure(Exception(if (errBody.isNotBlank()) errBody else "Erreur de prévisualisation"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun executeInChatTransfer(
        conversationId: String,
        amount: Double,
        currency: String,
        pin: String,
        receiverWalletId: String? = null,
        recipientAddress: String? = null
    ): Result<TransferResponse> = withContext(Dispatchers.IO) {
        val session = sessionDao.getSession() ?: return@withContext Result.failure(Exception("Non connecté"))
        val conv = conversationDao.getConversationSync(conversationId)

        var targetWalletId = receiverWalletId ?: conv?.contactWalletId
        var targetBtcAddress = if (currency.equals("BTC", ignoreCase = true)) (recipientAddress ?: conv?.contactBitcoinAddress) else null

        // If data is missing, attempt one last phone sync
        if ((!currency.equals("BTC", ignoreCase = true) && targetWalletId.isNullOrBlank()) ||
            (currency.equals("BTC", ignoreCase = true) && targetBtcAddress.isNullOrBlank())) {
            conv?.contactPhone?.let { phone ->
                val search = searchProfileByPhone(phone).getOrNull()
                if (search != null) {
                    if (targetWalletId.isNullOrBlank()) targetWalletId = search.walletId
                    if (targetBtcAddress.isNullOrBlank()) targetBtcAddress = search.bitcoinAddress
                }
            }
        }

        if (currency.equals("BTC", ignoreCase = true) && targetBtcAddress.isNullOrBlank() && targetWalletId.isNullOrBlank()) {
            return@withContext Result.failure(Exception("Adresse Bitcoin destinataire manquante"))
        }
        if (!currency.equals("BTC", ignoreCase = true) && targetWalletId.isNullOrBlank()) {
            return@withContext Result.failure(Exception("Wallet ID destinataire manquant"))
        }

        try {
            // Confirmation with PIN -> Real transaction creation
            val response = ApiClient.apiService.executeGlobalTransfer(
                TransferRequest(
                    receiverWalletId = if (!currency.equals("BTC", ignoreCase = true)) targetWalletId else null,
                    recipientAddress = if (currency.equals("BTC", ignoreCase = true)) (targetBtcAddress ?: targetWalletId) else null,
                    amount = amount,
                    currency = currency,
                    pin = pin,
                    preview = null
                )
            )

            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                if (body.isTransferSuccess) {
                    val txRef = body.resolvedReference
                    val now = System.currentTimeMillis()

                    // Insert message in local chat DB
                    val transferEntity = MessageEntity(
                        id = "msg_${System.currentTimeMillis()}",
                        conversationId = conversationId,
                        senderId = session.userId.toString(),
                        senderWalletId = session.walletId,
                        senderName = session.name,
                        content = "💸 Transfert de $amount $currency",
                        timestamp = now,
                        isMine = true,
                        status = "delivered",
                        type = "in_chat_transfer",
                        transferAmount = amount,
                        transferCurrency = currency,
                        transferReference = txRef
                    )
                    messageDao.insertMessage(transferEntity)

                    if (conv != null) {
                        conversationDao.insertConversation(
                            conv.copy(
                                lastMessage = "💸 Transfert de $amount $currency",
                                lastMessageTime = now
                            )
                        )
                    }

                    // Refresh wallet balances
                    fetchWallet()

                    Result.success(body)
                } else {
                    val errMsg = body.error ?: body.message ?: "Code PIN incorrect."
                    Result.failure(Exception(errMsg))
                }
            } else {
                val errBody = response.errorBody()?.string() ?: ""
                val parsed = parseTransferResponseJson(errBody)
                val errMsg = parsed?.error ?: parsed?.message ?: if (response.code() == 401 || response.code() == 400) "Code PIN incorrect." else if (errBody.isNotBlank()) errBody else "Échec du transfert"
                Result.failure(Exception(errMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun executeDirectTransfer(
        recipientWalletId: String,
        amount: Double,
        currency: String,
        pin: String
    ): Result<TransferResponse> = withContext(Dispatchers.IO) {
        try {
            // Find or create a conversation for this wallet
            val convId = getOrCreateConversationForContact(
                name = "Wallet $recipientWalletId",
                walletId = recipientWalletId
            )
            executeInChatTransfer(
                conversationId = convId,
                amount = amount,
                currency = currency,
                pin = pin,
                receiverWalletId = recipientWalletId
            )
        } catch (e: Exception) {
            Result.failure(Exception("Échec du débit: ${e.message}"))
        }
    }

    suspend fun updateProfileWithPin(
        pin: String,
        updates: ProfileUpdates
    ): Result<UserSessionEntity> = withContext(Dispatchers.IO) {
        val session = sessionDao.getSession() ?: return@withContext Result.failure(Exception("Non connecté"))

        try {
            val response = ApiClient.apiService.updateAuthMe(
                UpdateProfileWithPinRequest(
                    pin = pin,
                    updates = updates
                )
            )
            if (response.isSuccessful && response.body()?.success == true) {
                val updatedUser = response.body()?.user
                val astro = response.body()?.astro
                val updated = session.copy(
                    name = updatedUser?.resolvedFullName ?: updates.fullName ?: session.name,
                    bio = updatedUser?.resolvedActivity ?: updates.activityDescription ?: session.bio,
                    avatarUrl = updatedUser?.resolvedAvatar ?: updates.profilePhotoUrl ?: session.avatarUrl,
                    coverUrl = updatedUser?.resolvedCover ?: updates.coverPhotoUrl ?: session.coverUrl,
                    profession = updatedUser?.profession ?: updates.profession ?: session.profession,
                    activityDescription = updatedUser?.resolvedActivity ?: updates.activityDescription ?: session.activityDescription,
                    address = updatedUser?.address ?: updates.address ?: session.address,
                    city = updatedUser?.city ?: updates.city ?: session.city,
                    province = updatedUser?.province ?: updates.province ?: session.province,
                    country = updatedUser?.country ?: updates.country ?: session.country,
                    nationality = updatedUser?.nationality ?: updates.nationality ?: session.nationality,
                    language = updatedUser?.language ?: updates.language ?: session.language,
                    birthDate = updatedUser?.resolvedBirthDate ?: updates.birthDate ?: session.birthDate,
                    verificationStatus = updatedUser?.resolvedVerificationStatus ?: session.verificationStatus,
                    availabilityStatus = updatedUser?.resolvedAvailabilityStatus ?: updates.availabilityStatus ?: session.availabilityStatus,
                    astroSign = astro?.sign ?: session.astroSign,
                    astroElement = astro?.element ?: session.astroElement,
                    astroDescription = astro?.description ?: session.astroDescription,
                    astroLuckyNumber = astro?.luckyNumber ?: session.astroLuckyNumber,
                    astroAscendant = astro?.ascendant ?: session.astroAscendant
                )
                sessionDao.saveSession(updated)
                Result.success(updated)
            } else {
                val code = response.code()
                val err = when (code) {
                    400 -> "Données invalides ou code PIN manquant"
                    401 -> "Code PIN incorrect ou session expirée"
                    403 -> "Action non autorisée"
                    429 -> "Trop de tentatives. Veuillez patienter."
                    else -> response.body()?.error ?: "Impossible de mettre à jour le profil ($code)"
                }
                Result.failure(AuthException("Erreur mise à jour", err))
            }
        } catch (e: Exception) {
            Result.failure(AuthException("Erreur réseau", "Impossible de joindre le serveur. Vérifiez votre connexion."))
        }
    }

    suspend fun updateProfile(
        name: String? = null,
        bio: String? = null,
        avatarUrl: String? = null,
        email: String? = null,
        username: String? = null,
        birthDate: String? = null
    ): Result<UserSessionEntity> = withContext(Dispatchers.IO) {
        val session = sessionDao.getSession() ?: return@withContext Result.failure(Exception("Non connecté"))

        try {
            val response = ApiClient.apiService.updateProfile(
                UpdateProfileRequest(
                    name = name,
                    bio = bio,
                    avatarUrl = avatarUrl,
                    email = email,
                    username = username,
                    birthDate = birthDate
                )
            )
            val updated = session.copy(
                name = name ?: session.name,
                bio = bio ?: session.bio,
                avatarUrl = avatarUrl ?: session.avatarUrl,
                email = email ?: session.email,
                birthDate = birthDate ?: session.birthDate
            )
            sessionDao.saveSession(updated)
            Result.success(updated)
        } catch (e: Exception) {
            val updated = session.copy(
                name = name ?: session.name,
                bio = bio ?: session.bio,
                avatarUrl = avatarUrl ?: session.avatarUrl,
                email = email ?: session.email,
                birthDate = birthDate ?: session.birthDate
            )
            sessionDao.saveSession(updated)
            Result.success(updated)
        }
    }

    suspend fun editMessage(messageId: String, newContent: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            ApiClient.apiService.updateMessage(messageId.toLongOrNull() ?: 0L, UpdateMessageRequest(newContent))
        } catch (_: Exception) {}
        messageDao.updateMessageContent(messageId, newContent)
        Result.success(true)
    }

    suspend fun deleteMessage(conversationId: String, messageId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            ApiClient.apiService.deleteChatMessage(conversationId, messageId)
        } catch (_: Exception) {}
        messageDao.deleteMessage(messageId)
        Result.success(true)
    }

    suspend fun deleteConversation(conversationId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            ApiClient.apiService.deleteConversation(conversationId)
        } catch (_: Exception) {}
        conversationDao.deleteConversation(conversationId)
        messageDao.deleteMessagesForConversation(conversationId)
        Result.success(true)
    }

    suspend fun markOutgoingMessagesAsRead(conversationId: String) = withContext(Dispatchers.IO) {
        messageDao.markOutgoingMessagesAsRead(conversationId)
    }

    suspend fun markConversationMessagesAsRead(conversationId: String) = withContext(Dispatchers.IO) {
        readConversationsSet.add(conversationId)
        messageDao.markConversationMessagesAsRead(conversationId)
        val conv = conversationDao.getConversationSync(conversationId)
        if (conv != null) {
            conversationDao.insertConversation(conv.copy(unreadCount = 0))
        }
    }

    // --- Statuses ---

    fun getStatusesFlow(): Flow<List<StatusGroup>> {
        return statusDao.getStatusesFlow().map { list ->
            // Filter out expired statuses locally
            val now = System.currentTimeMillis()
            val activeList = list.filter { it.expiresAt > now }

            activeList.groupBy { it.userId }.map { (userId, items) ->
                val first = items.first()
                StatusGroup(
                    userId = userId,
                    userName = first.userName,
                    userAvatar = first.userAvatar,
                    statuses = items.map {
                        OnChatStatusItem(
                            id = it.id,
                            type = it.type,
                            content = it.content,
                            backgroundColor = it.backgroundColor,
                            mediaUrl = it.mediaUrl,
                            thumbnailUrl = it.thumbnailUrl,
                            duration = it.duration,
                            visibility = it.visibility,
                            createdAt = it.timestamp.toString(),
                            expiresAt = it.expiresAt.toString(),
                            viewsCount = it.viewsCount,
                            likesCount = it.likesCount,
                            isLiked = it.isLiked
                        )
                    },
                    lastUpdated = items.maxOf { it.timestamp }
                )
            }.sortedByDescending { it.lastUpdated }
        }
    }

    suspend fun uploadMedia(fileBytes: ByteArray, fileName: String, mimeType: String): String? = withContext(Dispatchers.IO) {
        try {
            val requestBody = fileBytes.toRequestBody(mimeType.toMediaTypeOrNull())
            val part = MultipartBody.Part.createFormData("file", fileName, requestBody)
            val response = ApiClient.apiService.uploadMedia(part)
            if (response.isSuccessful && response.body()?.success == true) {
                val resolved = response.body()!!.resolvedUrl
                if (!resolved.isNullOrBlank()) {
                    return@withContext resolved
                }
            }
            Log.w("OnChatRepository", "uploadMedia failed: code=${response.code()} msg=${response.message()}")
            null
        } catch (e: Exception) {
            Log.w("OnChatRepository", "uploadMedia error: ${e.message}")
            null
        }
    }

    // --- CashPay Real Video Engine API Methods ---

    suspend fun fetchVideos(limit: Int = 20, offset: Int = 0): Result<List<VideoItem>> = withContext(Dispatchers.IO) {
        val url = "https://app.cashpay-all.com/api/v1/media/videos?limit=$limit&offset=$offset"
        Log.d("VideoAPI", "GET $url | Token present: ${!ApiClient.authToken.isNullOrBlank()}")
        try {
            val response = ApiClient.apiService.getVideos(limit, offset)
            Log.d("VideoAPI", "GET $url -> HTTP ${response.code()}")
            if (response.isSuccessful && response.body() != null) {
                val videos = response.body()!!.videos
                Log.d("VideoAPI", "Fetched ${videos.size} real videos from CashPay backend")
                Result.success(videos)
            } else {
                val errBody = response.errorBody()?.string().orEmpty()
                Log.w("VideoAPI", "GET $url failed | Status ${response.code()} | Error: $errBody")
                Result.failure(Exception("HTTP ${response.code()} - $errBody"))
            }
        } catch (e: Exception) {
            Log.e("VideoAPI", "GET $url exception: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun getVideoDetail(videoId: Long): Result<VideoDetailResponse> = withContext(Dispatchers.IO) {
        val url = "https://app.cashpay-all.com/api/v1/media/videos/$videoId"
        Log.d("VideoAPI", "GET $url")
        try {
            val response = ApiClient.apiService.getVideoDetail(videoId)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("HTTP ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun uploadVideo(
        fileBytes: ByteArray,
        fileName: String,
        mimeType: String,
        publisherType: String? = "user",
        publisherId: Long? = null,
        durationSeconds: Int? = null,
        thumbnailUrl: String? = null
    ): Result<VideoItem> = withContext(Dispatchers.IO) {
        val url = "https://app.cashpay-all.com/api/v1/media/upload"
        Log.d(
            "VideoAPI",
            "POST $url | Field: 'video' | File: $fileName | Size: ${fileBytes.size} bytes | MIME: $mimeType | Token: ${!ApiClient.authToken.isNullOrBlank()}"
        )
        try {
            val requestBody = fileBytes.toRequestBody(mimeType.toMediaTypeOrNull())
            val videoPart = MultipartBody.Part.createFormData("video", fileName, requestBody)

            val pubTypeBody = publisherType?.toRequestBody("text/plain".toMediaTypeOrNull())
            val pubIdBody = publisherId?.toString()?.toRequestBody("text/plain".toMediaTypeOrNull())
            val durBody = durationSeconds?.toString()?.toRequestBody("text/plain".toMediaTypeOrNull())
            val thumbBody = thumbnailUrl?.toRequestBody("text/plain".toMediaTypeOrNull())

            val response = ApiClient.apiService.uploadVideo(
                video = videoPart,
                publisherType = pubTypeBody,
                publisherId = pubIdBody,
                durationSeconds = durBody,
                thumbnailUrl = thumbBody
            )

            Log.d("VideoAPI", "POST $url -> HTTP ${response.code()}")
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                if (body.success && body.video != null && body.video.resolvedUrl.isNotBlank()) {
                    Log.d("VideoAPI", "Upload SUCCESS | Video ID: ${body.video.id} | URL: ${body.video.resolvedUrl}")
                    Result.success(body.video)
                } else {
                    Log.w("VideoAPI", "Upload failed in body: ${body.error}")
                    Result.failure(Exception(body.error ?: body.message ?: "Échec de l'enregistrement vidéo"))
                }
            } else {
                val errBody = response.errorBody()?.string().orEmpty()
                Log.e("VideoAPI", "Upload HTTP ${response.code()} | Body: $errBody")
                Result.failure(Exception("HTTP ${response.code()} - $errBody"))
            }
        } catch (e: Exception) {
            Log.e("VideoAPI", "Upload Exception: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun uploadVideoGeneral(
        fileBytes: ByteArray,
        fileName: String,
        mimeType: String,
        durationSeconds: Int = 0,
        publisherId: Long? = null
    ): String? = withContext(Dispatchers.IO) {
        val result = uploadVideo(
            fileBytes = fileBytes,
            fileName = fileName,
            mimeType = mimeType,
            durationSeconds = durationSeconds,
            publisherId = publisherId
        )
        result.getOrNull()?.resolvedUrl
    }

    suspend fun likeVideo(videoId: Long): Result<VideoLikeResponse> = withContext(Dispatchers.IO) {
        val url = "https://app.cashpay-all.com/api/v1/media/videos/$videoId/like"
        Log.d("VideoAPI", "POST $url")
        try {
            val response = ApiClient.apiService.likeVideo(videoId)
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                Log.d("VideoAPI", "Like SUCCESS | Liked: ${body.liked} | Total Likes: ${body.likes}")
                Result.success(body)
            } else {
                Result.failure(Exception("HTTP ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun unlikeVideo(videoId: Long): Result<VideoLikeResponse> = withContext(Dispatchers.IO) {
        val url = "https://app.cashpay-all.com/api/v1/media/videos/$videoId/like"
        Log.d("VideoAPI", "DELETE $url")
        try {
            val response = ApiClient.apiService.unlikeVideo(videoId)
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                Log.d("VideoAPI", "Unlike SUCCESS | Liked: ${body.liked} | Total Likes: ${body.likes}")
                Result.success(body)
            } else {
                Result.failure(Exception("HTTP ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getVideoComments(videoId: Long, limit: Int = 50, offset: Int = 0): Result<List<VideoCommentItem>> = withContext(Dispatchers.IO) {
        val url = "https://app.cashpay-all.com/api/v1/media/videos/$videoId/comments"
        Log.d("VideoAPI", "GET $url")
        try {
            val response = ApiClient.apiService.getVideoComments(videoId, limit, offset)
            if (response.isSuccessful && response.body() != null) {
                val comments = response.body()!!.comments
                Log.d("VideoAPI", "Fetched ${comments.size} comments for video $videoId")
                Result.success(comments)
            } else {
                Result.failure(Exception("HTTP ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun postVideoComment(videoId: Long, content: String): Result<VideoCommentItem> = withContext(Dispatchers.IO) {
        val url = "https://app.cashpay-all.com/api/v1/media/videos/$videoId/comments"
        Log.d("VideoAPI", "POST $url | Content: $content")
        try {
            val response = ApiClient.apiService.postVideoComment(videoId, PostVideoCommentRequest(content))
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                if (body.comment != null) {
                    Result.success(body.comment)
                } else {
                    Result.failure(Exception("Commentaire non retourné par le serveur"))
                }
            } else {
                Result.failure(Exception("HTTP ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteVideo(videoId: Long): Result<Boolean> = withContext(Dispatchers.IO) {
        val url = "https://app.cashpay-all.com/api/v1/media/videos/$videoId"
        Log.d("VideoAPI", "DELETE $url")
        try {
            val response = ApiClient.apiService.deleteVideo(videoId)
            Result.success(response.isSuccessful)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- Private Profile Media API (https://app.cashpay-all.com/api/v1/profile/media) ---
    private val _profileMediaFlow = MutableStateFlow<List<ProfileMediaItem>>(emptyList())
    val profileMediaFlow: StateFlow<List<ProfileMediaItem>> = _profileMediaFlow.asStateFlow()

    suspend fun fetchProfileMedia(): Result<List<ProfileMediaItem>> = withContext(Dispatchers.IO) {
        try {
            val response = ApiClient.apiService.getProfileMedia()
            if (response.isSuccessful && response.body()?.success == true) {
                val mediaList = response.body()?.media ?: emptyList()
                _profileMediaFlow.value = mediaList
                Result.success(mediaList)
            } else {
                Result.failure(Exception(response.body()?.error ?: "Erreur chargement médias"))
            }
        } catch (e: Exception) {
            Log.w("OnChatRepository", "fetchProfileMedia error: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun uploadProfileMedia(
        fileBytes: ByteArray,
        fileName: String,
        mimeType: String,
        mediaType: String = "album_image",
        visibility: String = "public"
    ): Result<ProfileMediaItem> = withContext(Dispatchers.IO) {
        try {
            val requestFile = fileBytes.toRequestBody(mimeType.toMediaTypeOrNull())
            val filePart = MultipartBody.Part.createFormData("file", fileName, requestFile)
            val mediaTypeBody = mediaType.toRequestBody("text/plain".toMediaTypeOrNull())
            val visibilityBody = visibility.toRequestBody("text/plain".toMediaTypeOrNull())

            val response = ApiClient.apiService.uploadProfileMedia(filePart, mediaTypeBody, visibilityBody)
            if (response.isSuccessful && response.body() != null) {
                val jsonStr = response.body()!!.string()
                val moshi = com.squareup.moshi.Moshi.Builder().build()
                
                // Try to parse as direct ProfileMediaItem
                val itemAdapter = moshi.adapter(ProfileMediaItem::class.java)
                val directItem = try {
                    itemAdapter.fromJson(jsonStr)
                } catch (e: Exception) {
                    null
                }

                // Try to parse as wrapped UploadProfileMediaResponse
                val wrappedAdapter = moshi.adapter(UploadProfileMediaResponse::class.java)
                val wrappedItem = try {
                    wrappedAdapter.fromJson(jsonStr)
                } catch (e: Exception) {
                    null
                }

                val newMedia = if (directItem != null && directItem.id > 0) {
                    directItem
                } else if (wrappedItem?.success == true && wrappedItem.media != null) {
                    wrappedItem.media
                } else {
                    null
                }

                if (newMedia != null) {
                    val current = _profileMediaFlow.value.toMutableList()
                    current.add(0, newMedia)
                    _profileMediaFlow.value = current

                    // If uploaded as profile or cover, reload session
                    if (mediaType == "profile" || mediaType == "cover") {
                        fetchAuthMe()
                    }
                    Result.success(newMedia)
                } else {
                    Result.failure(Exception("Format de réponse média inconnu ou échec d'écriture : $jsonStr"))
                }
            } else {
                Result.failure(Exception("Échec de l'upload du média (${response.code()})"))
            }
        } catch (e: Exception) {
            Log.e("OnChatRepository", "uploadProfileMedia error: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun setProfileMedia(mediaId: Long): Result<String> = withContext(Dispatchers.IO) {
        try {
            val response = ApiClient.apiService.setProfileMedia(SetMediaTargetRequest(mediaId))
            if (response.isSuccessful && response.body()?.success == true) {
                val newUrl = response.body()?.profilePhotoUrl ?: response.body()?.profilePhoto
                val session = sessionDao.getSession()
                if (session != null && newUrl != null) {
                    val updated = session.copy(avatarUrl = newUrl)
                    sessionDao.saveSession(updated)
                }
                _profileMediaFlow.value = _profileMediaFlow.value.map {
                    it.copy(isProfile = it.id == mediaId)
                }
                Result.success(newUrl ?: "")
            } else {
                Result.failure(Exception(response.body()?.error ?: "Échec de la mise à jour de la photo de profil"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun setCoverMedia(mediaId: Long): Result<String> = withContext(Dispatchers.IO) {
        try {
            val response = ApiClient.apiService.setCoverMedia(SetMediaTargetRequest(mediaId))
            if (response.isSuccessful && response.body()?.success == true) {
                val newUrl = response.body()?.coverPhotoUrl ?: response.body()?.coverPhoto
                val session = sessionDao.getSession()
                if (session != null && newUrl != null) {
                    val updated = session.copy(coverUrl = newUrl)
                    sessionDao.saveSession(updated)
                }
                _profileMediaFlow.value = _profileMediaFlow.value.map {
                    it.copy(isCover = it.id == mediaId)
                }
                Result.success(newUrl ?: "")
            } else {
                Result.failure(Exception(response.body()?.error ?: "Échec de la mise à jour de la couverture"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun getLocalContactPhones(): List<String> {
        val phones = mutableListOf<String>()
        try {
            if (androidx.core.content.ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.READ_CONTACTS
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                val cr = context.contentResolver
                val cursor = cr.query(
                    android.provider.ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    arrayOf(android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER),
                    null,
                    null,
                    null
                )
                if (cursor != null) {
                    try {
                        val numberIndex = cursor.getColumnIndex(android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER)
                        while (cursor.moveToNext()) {
                            val number = if (numberIndex >= 0) cursor.getString(numberIndex) else null
                            if (!number.isNullOrBlank()) {
                                phones.add(number)
                            }
                        }
                    } finally {
                        cursor.close()
                    }
                }
            }
        } catch (e: Exception) {
            Log.w("OnChatRepository", "getLocalContactPhones error: ${e.message}")
        }
        return phones.distinct()
    }

    suspend fun fetchStatusesFromServer(): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            // 1. Fetch our own statuses and/or legacy global statuses
            var response = ApiClient.apiService.getStatusesMultipart()
            if (!response.isSuccessful || response.body() == null) {
                // Fallback to legacy GET statuses route
                response = ApiClient.apiService.getStatuses()
            }

            val fetchedEntities = mutableListOf<StatusEntity>()

            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                val groups = body.statusGroups
                groups.forEach { group ->
                    group.statuses.forEach { item ->
                        fetchedEntities.add(
                            StatusEntity(
                                id = item.id,
                                userId = group.userId,
                                userName = group.userName,
                                userAvatar = group.userAvatar,
                                type = item.type,
                                content = item.content,
                                backgroundColor = item.backgroundColor,
                                mediaUrl = item.mediaUrl ?: item.content, // status.content can be the URL
                                thumbnailUrl = item.thumbnailUrl,
                                duration = item.duration,
                                visibility = item.visibility,
                                timestamp = item.createdAt.toLongOrNull() ?: System.currentTimeMillis(),
                                expiresAt = item.expiresAt.toLongOrNull() ?: (System.currentTimeMillis() + 86400000L),
                                viewsCount = item.viewsCount,
                                likesCount = item.likesCount,
                                isLiked = item.isLiked
                            )
                        )
                    }
                }
            }

            // 2. Fetch statuses of our contacts using phone list
            val contactPhones = getLocalContactPhones()
            if (contactPhones.isNotEmpty()) {
                val contactsResult = checkStatuses(contactPhones)
                contactsResult.onSuccess { contacts ->
                    contacts.forEach { contact ->
                        if (contact.user != null) {
                            contact.statuses.forEach { item ->
                                fetchedEntities.add(
                                    StatusEntity(
                                        id = item.id,
                                        userId = contact.user.id,
                                        userName = contact.user.fullName,
                                        userAvatar = contact.user.profilePhoto,
                                        type = item.type,
                                        content = item.content,
                                        backgroundColor = item.backgroundColor,
                                        mediaUrl = item.mediaUrl ?: item.content,
                                        thumbnailUrl = item.thumbnailUrl,
                                        duration = item.duration,
                                        visibility = item.visibility,
                                        timestamp = item.createdAt.toLongOrNull() ?: System.currentTimeMillis(),
                                        expiresAt = item.expiresAt.toLongOrNull() ?: (System.currentTimeMillis() + 86400000L),
                                        viewsCount = item.viewsCount,
                                        likesCount = item.likesCount,
                                        isLiked = item.isLiked
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // 3. Insert all synchronized statuses into the DB
            if (fetchedEntities.isNotEmpty()) {
                statusDao.insertStatuses(fetchedEntities)
            }
            Result.success(true)
        } catch (e: Exception) {
            Log.w("OnChatRepository", "fetchStatusesFromServer error: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun postStatus(
        type: String,
        content: String? = null,
        backgroundColor: String? = null,
        mediaUrl: String? = null,
        thumbnailUrl: String? = null,
        duration: Int? = null,
        visibility: String = "public"
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        val session = sessionDao.getSession()
        val userId = session?.userId ?: ApiClient.currentUserId ?: 1L
        val userName = session?.name ?: "Moi"
        val userAvatar = session?.avatarUrl

        try {
            val request = CreateStatusRequest(
                type = type,
                content = content,
                backgroundColor = backgroundColor,
                mediaUrl = mediaUrl,
                thumbnailUrl = thumbnailUrl,
                duration = duration,
                visibility = visibility
            )
            val response = ApiClient.apiService.postStatus(request)
            if (response.isSuccessful && response.body() != null) {
                val res = response.body()!!
                statusDao.insertStatus(
                    StatusEntity(
                        id = res.id,
                        userId = userId,
                        userName = userName,
                        userAvatar = userAvatar,
                        type = res.type,
                        content = content,
                        backgroundColor = backgroundColor,
                        mediaUrl = res.mediaUrl ?: mediaUrl,
                        thumbnailUrl = res.thumbnailUrl ?: thumbnailUrl,
                        duration = duration,
                        visibility = res.visibility,
                        timestamp = res.createdAt.toLongOrNull() ?: System.currentTimeMillis(),
                        expiresAt = res.expiresAt.toLongOrNull() ?: (System.currentTimeMillis() + 86400000L)
                    )
                )
                return@withContext Result.success(true)
            }
        } catch (e: Exception) {
            Log.w("OnChatRepository", "postStatus server error: ${e.message}")
        }

        // Local fallback
        val statusId = System.currentTimeMillis()
        statusDao.insertStatus(
            StatusEntity(
                id = statusId,
                userId = userId,
                userName = userName,
                userAvatar = userAvatar,
                type = type,
                content = content,
                backgroundColor = backgroundColor,
                mediaUrl = mediaUrl,
                thumbnailUrl = thumbnailUrl,
                duration = duration,
                visibility = visibility,
                timestamp = statusId,
                expiresAt = statusId + 86400000L
            )
        )
        Result.success(true)
    }

    suspend fun postStatusWithFile(
        fileBytes: ByteArray,
        fileName: String,
        mimeType: String,
        type: String, // "image" or "video"
        backgroundColor: String? = null
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        val session = sessionDao.getSession()
        val userId = session?.userId ?: ApiClient.currentUserId ?: 1L
        val userName = session?.name ?: "Moi"
        val userAvatar = session?.avatarUrl

        try {
            val requestFile = fileBytes.toRequestBody(mimeType.toMediaTypeOrNull())
            val filePart = MultipartBody.Part.createFormData("file", fileName, requestFile)
            val typeBody = type.toRequestBody("text/plain".toMediaTypeOrNull())
            val bgBody = backgroundColor?.toRequestBody("text/plain".toMediaTypeOrNull())

            val response = ApiClient.apiService.createStatusMultipart(filePart, typeBody, bgBody)
            if (response.isSuccessful && response.body() != null) {
                val res = response.body()!!
                if (res.success && res.status != null) {
                    val statusItem = res.status
                    statusDao.insertStatus(
                        StatusEntity(
                            id = statusItem.id,
                            userId = userId,
                            userName = userName,
                            userAvatar = userAvatar,
                            type = statusItem.type,
                            content = statusItem.content,
                            backgroundColor = statusItem.backgroundColor,
                            mediaUrl = statusItem.content, // status.content is the display URL of the status
                            thumbnailUrl = null,
                            duration = null,
                            visibility = "public",
                            timestamp = System.currentTimeMillis(),
                            expiresAt = statusItem.expiresAt?.toLongOrNull() ?: (System.currentTimeMillis() + 86400000L)
                        )
                    )
                    return@withContext Result.success(true)
                }
            }
        } catch (e: Exception) {
            Log.e("OnChatRepository", "postStatusWithFile error: ${e.message}", e)
        }
        Result.success(false)
    }

    suspend fun recordStatusView(statusId: Long): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            ApiClient.apiService.viewStatus(StatusViewRequest(statusId))
        } catch (_: Exception) {}
        Result.success(true)
    }

    suspend fun toggleStatusLike(statusId: Long, liked: Boolean = true): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            ApiClient.apiService.likeStatus(StatusLikeRequest(statusId, liked))
        } catch (_: Exception) {}
        Result.success(true)
    }

    suspend fun repositoryCleanup() = withContext(Dispatchers.IO) {
        try {
            statusDao.deleteExpiredStatuses()
        } catch (_: Exception) {}
    }

    suspend fun findContactByWalletId(walletId: String): ConversationEntity? = withContext(Dispatchers.IO) {
        conversationDao.getConversationByWalletId(walletId)
    }

    // --- Calls ---

    suspend fun recordCall(
        peerName: String,
        isVideo: Boolean,
        isIncoming: Boolean,
        isMissed: Boolean,
        durationSeconds: Int,
        peerWalletId: String? = null,
        peerAvatar: String? = null
    ) = withContext(Dispatchers.IO) {
        val callId = "call_${System.currentTimeMillis()}"
        callDao.insertCall(
            CallEntity(
                id = callId,
                peerName = peerName,
                callerWalletId = if (isIncoming) peerWalletId else null,
                receiverWalletId = if (!isIncoming) peerWalletId else null,
                peerAvatar = peerAvatar,
                isVideo = isVideo,
                isIncoming = isIncoming,
                isMissed = isMissed,
                durationSeconds = durationSeconds,
                timestamp = System.currentTimeMillis()
            )
        )
    }

    // --- Jojo Shopping & Events Feeds ---

    suspend fun getShoppingFeed(limit: Int = 20): List<ProductItem> = withContext(Dispatchers.IO) {
        try {
            val response = ApiClient.apiService.getShoppingFeed(limit)
            if (response.isSuccessful && response.body()?.success == true) {
                response.body()?.products ?: emptyList()
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun followShop(sellerId: Int): FollowShopResponse? = withContext(Dispatchers.IO) {
        try {
            val response = ApiClient.apiService.followShop(FollowShopRequest(sellerId))
            if (response.isSuccessful) {
                response.body()
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getEventsFeed(limit: Int = 10): List<EventItem> = withContext(Dispatchers.IO) {
        try {
            val response = ApiClient.apiService.getEventsFeed(limit)
            if (response.isSuccessful && response.body()?.success == true) {
                response.body()?.events ?: emptyList()
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // MonPays Repository Methods
    suspend fun getMonPaysPages(): List<MonPaysCompany> = withContext(Dispatchers.IO) {
        try {
            val response = ApiClient.apiService.getMonPaysPages()
            if (response.isSuccessful && response.body()?.success == true) {
                response.body()?.companies ?: emptyList()
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getMonPaysPageDetail(companyId: Long): MonPaysDetailResponse? = withContext(Dispatchers.IO) {
        try {
            val response = ApiClient.apiService.getMonPaysPageDetail(companyId)
            if (response.isSuccessful && response.body()?.success == true) {
                response.body()
            } else null
        } catch (e: Exception) {
            null
        }
    }

    suspend fun followMonPaysPage(companyId: Long): MonPaysFollowResponse? = withContext(Dispatchers.IO) {
        try {
            val response = ApiClient.apiService.followMonPaysPage(companyId, MonPaysFollowRequest())
            if (response.isSuccessful) {
                response.body()
            } else null
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getMonPaysFeed(): List<MonPaysActivity> = withContext(Dispatchers.IO) {
        try {
            val response = ApiClient.apiService.getMonPaysFeed()
            if (response.isSuccessful && response.body()?.success == true) {
                response.body()?.activities ?: emptyList()
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getMonPaysComments(postId: Long): List<MonPaysComment> = withContext(Dispatchers.IO) {
        try {
            val response = ApiClient.apiService.getMonPaysComments(postId)
            if (response.isSuccessful && response.body()?.success == true) {
                response.body()?.comments ?: emptyList()
            } else emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun postMonPaysComment(postId: Long, content: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val response = ApiClient.apiService.postMonPaysComment(MonPaysCommentRequest(postId = postId, content = content))
            response.isSuccessful && response.body()?.success == true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun checkStatuses(phoneNumbers: List<String>): Result<List<OnChatStatusContact>> = withContext(Dispatchers.IO) {
        try {
            val response = ApiClient.apiService.checkStatuses(CheckStatusesRequest(phoneNumbers))
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(response.body()?.contacts ?: emptyList())
            } else {
                Result.failure(Exception("Échec de la récupération des statuts"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun viewStatus(statusId: Long): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val response = ApiClient.apiService.viewStatus(StatusViewRequest(statusId))
            Result.success(response.isSuccessful && response.body()?.success == true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun likeStatus(statusId: Long, liked: Boolean): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val response = ApiClient.apiService.likeStatus(StatusLikeRequest(statusId, liked))
            Result.success(response.isSuccessful && response.body()?.success == true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

fun ConversationEntity.toDomain(): Conversation = Conversation(
    id = id,
    name = name,
    avatarUrl = avatarUrl,
    coverUrl = coverUrl,
    lastMessage = lastMessage,
    lastMessageTime = lastMessageTime,
    unreadCount = unreadCount,
    isGroup = isGroup,
    currency = currency,
    isOnline = true,
    contactPhone = contactPhone,
    contactWalletId = contactWalletId,
    contactEmail = contactEmail,
    contactBio = contactBio,
    contactProfession = contactProfession,
    contactActivityDescription = contactActivityDescription,
    contactAddress = contactAddress,
    contactCity = contactCity,
    contactProvince = contactProvince,
    contactCountry = contactCountry,
    contactNationality = contactNationality,
    contactLanguage = contactLanguage,
    contactBirthDate = contactBirthDate,
    contactVerificationStatus = contactVerificationStatus,
    contactAvailabilityStatus = contactAvailabilityStatus,
    contactAstroSign = contactAstroSign,
    contactAstroSymbol = contactAstroSymbol,
    contactAstroNameFr = contactAstroNameFr,
    contactAstroElement = contactAstroElement,
    contactBitcoinAddress = contactBitcoinAddress,
    isMuted = isMuted
)

class AuthException(val title: String, val detail: String) : Exception("$title: $detail")

object AuthMaskHelper {
    fun maskPhone(phone: String?): String {
        if (phone.isNullOrBlank()) return "•••• •••• 14"
        val digits = phone.filter { it.isDigit() }
        val lastTwo = if (digits.length >= 2) digits.takeLast(2) else digits
        return "•••• •••• $lastTwo"
    }

    fun maskEmail(email: String?): String {
        if (email.isNullOrBlank() || !email.contains("@")) return "p••••@gmail.com"
        val parts = email.split("@")
        val userPart = parts[0]
        val domain = parts.getOrNull(1) ?: "gmail.com"
        val firstChar = if (userPart.isNotEmpty()) userPart.first().toString() else "p"
        return "$firstChar••••@$domain"
    }
}

