package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.api.ApiClient
import com.example.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * Gestionnaire officiel des Réunions (Meetings), Participants, Facturation et Appels.
 * 100% Connecté à l'API de production CashPay All / OnChat (https://app.cashpay-all.com/api/v1).
 * Aucune fausse donnée ou mock data.
 */
class MeetingsRepository(private val context: Context) {

    private val tag = "MeetingsRepository"

    // ==========================================
    // 3.1 Créer une réunion
    // POST /api/v1/meetings/create
    // ==========================================
    suspend fun createMeeting(
        topic: String?,
        type: String = "video",
        scheduledAt: String? = null,
        durationMin: Int = 60,
        priceAmount: Double = 0.0,
        priceCurrency: String = "USD"
    ): Result<CreateMeetingResponse> = withContext(Dispatchers.IO) {
        val cleanTopic = (topic?.trim() ?: "Réunion OnChat").take(120)
        val cleanType = if (type.lowercase() == "audio") "audio" else "video"

        val request = CreateMeetingRequest(
            topic = cleanTopic,
            type = cleanType,
            scheduledAt = scheduledAt,
            durationMin = durationMin,
            priceAmount = priceAmount,
            priceCurrency = priceCurrency.uppercase()
        )

        try {
            val response = ApiClient.apiService.createMeeting(request)
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                if (body.success) {
                    Result.success(body)
                } else {
                    Result.failure(Exception(body.error ?: "Échec de création de la réunion"))
                }
            } else {
                val errorMsg = parseErrorMessage(response.code(), response.errorBody()?.string())
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e(tag, "createMeeting exception", e)
            Result.failure(Exception(formatNetworkError(e)))
        }
    }

    // ==========================================
    // 3.2 Lister les réunions récentes
    // GET /api/v1/meetings/list?limit=20
    // ==========================================
    suspend fun getMeetingsList(limit: Int = 20): Result<List<MeetingItemDto>> = withContext(Dispatchers.IO) {
        val safeLimit = limit.coerceIn(1, 50)
        try {
            val response = ApiClient.apiService.getMeetingsList(safeLimit)
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                if (body.success) {
                    Result.success(body.meetings)
                } else {
                    Result.failure(Exception(body.error ?: "Erreur de récupération des réunions"))
                }
            } else {
                val errorMsg = parseErrorMessage(response.code(), response.errorBody()?.string())
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e(tag, "getMeetingsList exception", e)
            Result.failure(Exception(formatNetworkError(e)))
        }
    }

    // ==========================================
    // 3.3 Obtenir le statut d'une réunion
    // GET /api/v1/meetings/{meetCode}/status
    // ==========================================
    suspend fun getMeetingStatus(meetCode: String): Result<MeetingStatusResponse> = withContext(Dispatchers.IO) {
        val cleanCode = meetCode.trim()
        if (cleanCode.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Code de réunion vide"))
        }

        try {
            val response = ApiClient.apiService.getMeetingStatus(cleanCode)
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                if (body.success) {
                    Result.success(body)
                } else {
                    Result.failure(Exception(body.error ?: "Erreur de statut de la réunion"))
                }
            } else {
                val errorMsg = parseErrorMessage(response.code(), response.errorBody()?.string())
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e(tag, "getMeetingStatus exception", e)
            Result.failure(Exception(formatNetworkError(e)))
        }
    }

    // ==========================================
    // 3.4 Rejoindre une réunion
    // POST /api/v1/meetings/{meetCode}/join
    // ==========================================
    suspend fun joinMeeting(meetCode: String): Result<JoinMeetingResponse> = withContext(Dispatchers.IO) {
        val cleanCode = meetCode.trim()
        if (cleanCode.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Code de réunion vide"))
        }

        try {
            val response = ApiClient.apiService.joinMeeting(cleanCode)
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                if (body.success) {
                    Result.success(body)
                } else {
                    Result.failure(Exception(body.error ?: "Impossible de rejoindre la réunion"))
                }
            } else {
                val errorMsg = parseErrorMessage(response.code(), response.errorBody()?.string())
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e(tag, "joinMeeting exception", e)
            Result.failure(Exception(formatNetworkError(e)))
        }
    }

    // ==========================================
    // 3.5 Consulter les participants
    // GET /api/v1/meetings/{meetCode}/participants
    // ==========================================
    suspend fun getMeetingParticipants(meetCode: String): Result<MeetingParticipantsResponse> = withContext(Dispatchers.IO) {
        val cleanCode = meetCode.trim()
        if (cleanCode.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Code de réunion vide"))
        }

        try {
            val response = ApiClient.apiService.getMeetingParticipants(cleanCode)
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                if (body.success) {
                    Result.success(body)
                } else {
                    Result.failure(Exception(body.error ?: "Erreur de liste des participants"))
                }
            } else {
                val errorMsg = parseErrorMessage(response.code(), response.errorBody()?.string())
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e(tag, "getMeetingParticipants exception", e)
            Result.failure(Exception(formatNetworkError(e)))
        }
    }

    // ==========================================
    // 3.6 Quitter une réunion
    // POST /api/v1/meetings/{meetCode}/leave
    // ==========================================
    suspend fun leaveMeeting(meetCode: String): Result<Boolean> = withContext(Dispatchers.IO) {
        val cleanCode = meetCode.trim()
        if (cleanCode.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Code de réunion vide"))
        }

        try {
            val response = ApiClient.apiService.leaveMeeting(cleanCode)
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                if (body.success) {
                    Result.success(true)
                } else {
                    Result.failure(Exception(body.error ?: "Erreur lors de la sortie de la réunion"))
                }
            } else {
                val errorMsg = parseErrorMessage(response.code(), response.errorBody()?.string())
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e(tag, "leaveMeeting exception", e)
            Result.failure(Exception(formatNetworkError(e)))
        }
    }

    // ==========================================
    // 3.7 Terminer une réunion (Hôte uniquement)
    // POST /api/v1/meetings/{meetCode}/end
    // ==========================================
    suspend fun endMeeting(meetCode: String): Result<Boolean> = withContext(Dispatchers.IO) {
        val cleanCode = meetCode.trim()
        if (cleanCode.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Code de réunion vide"))
        }

        try {
            val response = ApiClient.apiService.endMeeting(cleanCode)
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                if (body.success) {
                    Result.success(true)
                } else {
                    Result.failure(Exception(body.error ?: "Erreur lors de la fin de la réunion"))
                }
            } else {
                val errorMsg = parseErrorMessage(response.code(), response.errorBody()?.string())
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e(tag, "endMeeting exception", e)
            Result.failure(Exception(formatNetworkError(e)))
        }
    }

    // ==========================================
    // 3.8 Lever ou baisser la main
    // POST /api/v1/meetings/{meetCode}/raise-hand
    // ==========================================
    suspend fun raiseHand(meetCode: String, raised: Boolean): Result<Boolean> = withContext(Dispatchers.IO) {
        val cleanCode = meetCode.trim()
        if (cleanCode.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Code de réunion vide"))
        }

        try {
            val response = ApiClient.apiService.raiseHand(cleanCode, RaiseHandRequest(raised))
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                if (body.success) {
                    Result.success(body.raised)
                } else {
                    Result.failure(Exception(body.error ?: "Erreur lors du changement d'état de la main"))
                }
            } else {
                val errorMsg = parseErrorMessage(response.code(), response.errorBody()?.string())
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e(tag, "raiseHand exception", e)
            Result.failure(Exception(formatNetworkError(e)))
        }
    }

    // ==========================================
    // 3.9 Accorder ou retirer le droit de parole (Hôte)
    // POST /api/v1/meetings/{meetCode}/grant-speech
    // ==========================================
    suspend fun grantSpeech(meetCode: String, targetUserId: Long, canSpeak: Boolean): Result<GrantSpeechResponse> = withContext(Dispatchers.IO) {
        val cleanCode = meetCode.trim()
        if (cleanCode.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Code de réunion vide"))
        }

        val request = GrantSpeechRequest(targetUserId = targetUserId, canSpeak = canSpeak)
        try {
            val response = ApiClient.apiService.grantSpeech(cleanCode, request)
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                if (body.success) {
                    Result.success(body)
                } else {
                    Result.failure(Exception(body.error ?: "Erreur lors de la modification du droit de parole"))
                }
            } else {
                val errorMsg = parseErrorMessage(response.code(), response.errorBody()?.string())
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e(tag, "grantSpeech exception", e)
            Result.failure(Exception(formatNetworkError(e)))
        }
    }

    // ==========================================
    // 3.10 Consulter la facturation d'une réunion
    // GET /api/v1/meetings/{meetCode}/billing
    // ==========================================
    suspend fun getMeetingBilling(meetCode: String): Result<MeetingBillingResponse> = withContext(Dispatchers.IO) {
        val cleanCode = meetCode.trim()
        if (cleanCode.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Code de réunion vide"))
        }

        try {
            val response = ApiClient.apiService.getMeetingBilling(cleanCode)
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                if (body.success) {
                    Result.success(body)
                } else {
                    Result.failure(Exception(body.error ?: "Erreur de consultation de facturation"))
                }
            } else {
                val errorMsg = parseErrorMessage(response.code(), response.errorBody()?.string())
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e(tag, "getMeetingBilling exception", e)
            Result.failure(Exception(formatNetworkError(e)))
        }
    }

    // ==========================================
    // 3.11 Modifier la facturation d'une réunion (Hôte)
    // POST /api/v1/meetings/{meetCode}/billing
    // ==========================================
    suspend fun updateMeetingBilling(
        meetCode: String,
        priceAmount: Double,
        priceCurrency: String
    ): Result<UpdateMeetingBillingResponse> = withContext(Dispatchers.IO) {
        val cleanCode = meetCode.trim()
        if (cleanCode.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Code de réunion vide"))
        }

        val request = UpdateMeetingBillingRequest(
            priceAmount = priceAmount,
            priceCurrency = priceCurrency.uppercase()
        )
        try {
            val response = ApiClient.apiService.updateMeetingBilling(cleanCode, request)
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                if (body.success) {
                    Result.success(body)
                } else {
                    Result.failure(Exception(body.error ?: "Erreur de modification de la facturation"))
                }
            } else {
                val errorMsg = parseErrorMessage(response.code(), response.errorBody()?.string())
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e(tag, "updateMeetingBilling exception", e)
            Result.failure(Exception(formatNetworkError(e)))
        }
    }

    // ==========================================
    // 4.1 Initier un appel
    // POST /api/v1/calls/initiate
    // ==========================================
    suspend fun initiateCall(
        callerWalletId: String,
        receiverWalletId: String,
        isVideo: Boolean
    ): Result<ApiCallDto> = withContext(Dispatchers.IO) {
        if (callerWalletId.equals(receiverWalletId, ignoreCase = true)) {
            return@withContext Result.failure(IllegalArgumentException("Tentative de s'appeler soi-même refusée (400)"))
        }

        val request = InitiateCallRequest(
            callerWalletId = callerWalletId.trim(),
            receiverWalletId = receiverWalletId.trim(),
            isVideo = isVideo
        )

        try {
            val response = ApiClient.apiService.initiateCall(request)
            if (response.isSuccessful && response.body()?.success == true && response.body()?.call != null) {
                Result.success(response.body()!!.call!!)
            } else {
                val errorMsg = parseErrorMessage(response.code(), response.errorBody()?.string())
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e(tag, "initiateCall exception", e)
            Result.failure(Exception(formatNetworkError(e)))
        }
    }

    // ==========================================
    // 4.2 Répondre, refuser ou signaler un appel
    // POST /api/v1/calls/{callId}/respond
    // ==========================================
    suspend fun respondCall(
        callId: String,
        walletId: String,
        status: String
    ): Result<ApiCallDto> = withContext(Dispatchers.IO) {
        val validStatuses = listOf("COMPLETED", "MISSED", "REJECTED", "BUSY")
        val cleanStatus = status.uppercase()
        if (cleanStatus !in validStatuses) {
            return@withContext Result.failure(IllegalArgumentException("Statut d'appel non autorisé ($cleanStatus)"))
        }

        val request = RespondCallRequest(walletId = walletId.trim(), status = cleanStatus)
        try {
            val response = ApiClient.apiService.respondCall(callId, request)
            if (response.isSuccessful && response.body()?.success == true && response.body()?.call != null) {
                Result.success(response.body()!!.call!!)
            } else {
                val errorMsg = parseErrorMessage(response.code(), response.errorBody()?.string())
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e(tag, "respondCall exception", e)
            Result.failure(Exception(formatNetworkError(e)))
        }
    }

    // ==========================================
    // 4.3 Terminer un appel
    // POST /api/v1/calls/{callId}/end
    // ==========================================
    suspend fun endCall(callId: String, walletId: String, durationSec: Int = 0): Result<ApiCallDto> = withContext(Dispatchers.IO) {
        val request = EndCallRequest(walletId = walletId.trim())
        try {
            val response = ApiClient.apiService.endCall(callId, request)
            if (response.isSuccessful && response.body()?.success == true && response.body()?.call != null) {
                Result.success(response.body()!!.call!!)
            } else {
                val errorMsg = parseErrorMessage(response.code(), response.errorBody()?.string())
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e(tag, "endCall exception", e)
            Result.failure(Exception(formatNetworkError(e)))
        }
    }

    // ==========================================
    // 4.4 Historique des appels
    // GET /api/v1/calls/history?walletId=...
    // ==========================================
    suspend fun getCallsHistory(walletId: String? = null): Result<List<ApiCallDto>> = withContext(Dispatchers.IO) {
        try {
            val response = ApiClient.apiService.getCallsHistory(walletId)
            if (response.isSuccessful && response.body()?.success == true) {
                val calls = response.body()?.calls ?: emptyList()
                Result.success(calls)
            } else {
                val errorMsg = parseErrorMessage(response.code(), response.errorBody()?.string())
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e(tag, "getCallsHistory exception", e)
            Result.failure(Exception(formatNetworkError(e)))
        }
    }

    private fun parseErrorMessage(code: Int, body: String?): String {
        // Tenter d'extraire le message JSON d'erreur du backend
        var backendMsg: String? = null
        if (!body.isNullOrBlank()) {
            try {
                val json = JSONObject(body)
                if (json.has("error")) backendMsg = json.getString("error")
                else if (json.has("message")) backendMsg = json.getString("message")
            } catch (_: Exception) {}
        }

        if (!backendMsg.isNullOrBlank()) {
            return backendMsg
        }

        return when (code) {
            400 -> "Paramètres invalides ou requête incorrecte (400)"
            401 -> "Authentification requise. Session ou cookie 'cashpay-token' manquant (401)"
            403 -> "Action interdite : droit réservé à l'hôte (403)"
            404 -> "Réunion, utilisateur ou appel introuvable (404)"
            410 -> "La réunion est terminée (410)"
            500 -> "Erreur interne du serveur CashPay All (500)"
            else -> "Erreur HTTP $code"
        }
    }

    private fun formatNetworkError(e: Exception): String {
        return e.localizedMessage?.let { "Erreur réseau : $it" } ?: "Impossible de joindre le serveur CashPay All"
    }
}
