package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.api.ApiClient
import com.example.data.api.OnChatWebSocketManager
import com.example.data.api.WsConnectionStatus
import com.example.data.db.UserSessionEntity
import com.example.data.model.*
import com.example.data.repository.*
import com.example.util.OnChatNotificationHelper
import com.example.util.RealVoiceCallManager
import com.example.util.RingtoneHelper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive

enum class LoginStep {
    WALLET_ID,
    CHANNEL_SELECTION,
    OTP_VERIFY,
    PIN_VERIFY,
    PROFILE_INSTALLATING,
    PROFILE_CONFIRMATION,
    AUTHENTICATED
}

enum class AuthMethod {
    PHONE,
    WALLET_ID
}

enum class ForgotPinFlowStep {
    OFF,
    ID_INPUT,
    CHANNEL_SELECTION,
    OTP_VERIFY,
    NEW_PIN_INPUT,
    SUCCESS
}

data class UiAuthError(
    val title: String,
    val message: String
)

data class CallSessionState(
    val roomId: String,
    val peerName: String,
    val peerWalletId: String = "",
    val peerAvatar: String? = null,
    val isVideo: Boolean,
    val isConnected: Boolean = false,
    val isMuted: Boolean = false,
    val isSpeakerOn: Boolean = true,
    val isCameraFacingBack: Boolean = false,
    val durationSeconds: Int = 0
)

class OnChatViewModel(application: Application) : AndroidViewModel(application) {
    val repository = OnChatRepository(application)
    val meetingsRepository = MeetingsRepository(application)
    val wsManager = OnChatWebSocketManager.getInstance()
    private val voiceCallManager = RealVoiceCallManager(application)

    // --- FINUSECO SARL / CashPay All Meetings & Calls State ---
    private val _activeMeeting = MutableStateFlow<ActiveMeetingSession?>(null)
    val activeMeeting: StateFlow<ActiveMeetingSession?> = _activeMeeting.asStateFlow()

    private val _recentMeetings = MutableStateFlow<List<MeetingItemDto>>(emptyList())
    val recentMeetings: StateFlow<List<MeetingItemDto>> = _recentMeetings.asStateFlow()

    private val _isMeetingsLoading = MutableStateFlow(false)
    val isMeetingsLoading: StateFlow<Boolean> = _isMeetingsLoading.asStateFlow()

    private val _activeMeetingBilling = MutableStateFlow<MeetingBillingDto?>(null)
    val activeMeetingBilling: StateFlow<MeetingBillingDto?> = _activeMeetingBilling.asStateFlow()

    private val _apiCallsHistory = MutableStateFlow<List<ApiCallDto>>(emptyList())
    val apiCallsHistory: StateFlow<List<ApiCallDto>> = _apiCallsHistory.asStateFlow()


    private val _lastMeetingApiError = MutableStateFlow<String?>(null)
    val lastMeetingApiError: StateFlow<String?> = _lastMeetingApiError.asStateFlow()

    // Real-Time WebSocket State (wss://glob.cashpay-all.com/ws)
    val wsStatus: StateFlow<WsConnectionStatus> = wsManager.connectionStatus
    val typingStatus: StateFlow<Map<String, String>> = wsManager.typingStatus
    val onlineUsers: StateFlow<Set<String>> = wsManager.onlineUsers

    // Auth State
    private val _loginStep = MutableStateFlow(LoginStep.WALLET_ID)
    val loginStep: StateFlow<LoginStep> = _loginStep.asStateFlow()

    private val _authMethod = MutableStateFlow(AuthMethod.PHONE)
    val authMethod: StateFlow<AuthMethod> = _authMethod.asStateFlow()

    private val _countries = MutableStateFlow<List<Country>>(emptyList())
    val countries: StateFlow<List<Country>> = _countries.asStateFlow()

    private val _selectedCountry = MutableStateFlow(
        Country(code = "CD", name = "République démocratique du Congo", dialCode = "243")
    )
    val selectedCountry: StateFlow<Country> = _selectedCountry.asStateFlow()

    private val _phoneNumberInput = MutableStateFlow("")
    val phoneNumberInput: StateFlow<String> = _phoneNumberInput.asStateFlow()

    private val _walletIdInput = MutableStateFlow("")
    val walletIdInput: StateFlow<String> = _walletIdInput.asStateFlow()

    private val _availableChannels = MutableStateFlow<List<AvailableChannel>>(emptyList())
    val availableChannels: StateFlow<List<AvailableChannel>> = _availableChannels.asStateFlow()

    private val _isGoogleLinked = MutableStateFlow(false)
    val isGoogleLinked: StateFlow<Boolean> = _isGoogleLinked.asStateFlow()

    private val _selectedChannel = MutableStateFlow<AvailableChannel?>(null)
    val selectedChannel: StateFlow<AvailableChannel?> = _selectedChannel.asStateFlow()

    private val _otpInput = MutableStateFlow("")
    val otpInput: StateFlow<String> = _otpInput.asStateFlow()

    private val _pinInput = MutableStateFlow("")
    val pinInput: StateFlow<String> = _pinInput.asStateFlow()

    private val _pendingUserId = MutableStateFlow(1L)
    val pendingUserId: StateFlow<Long> = _pendingUserId.asStateFlow()

    private val _isUploading = MutableStateFlow(false)
    val isUploading: StateFlow<Boolean> = _isUploading.asStateFlow()

    private val _uploadProgress = MutableStateFlow(0f)
    val uploadProgress: StateFlow<Float> = _uploadProgress.asStateFlow()

    private val _activeUserProfile = MutableStateFlow<PhoneProfileResponse?>(null)
    val activeUserProfile: StateFlow<PhoneProfileResponse?> = _activeUserProfile.asStateFlow()

    val currentSession: StateFlow<UserSessionEntity?> = repository.currentSessionFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val userPhone: StateFlow<String> = currentSession
        .map { it?.phone ?: "" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val sessionToken: StateFlow<String?> = currentSession
        .map { it?.token }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _authLoading = MutableStateFlow(false)
    val authLoading: StateFlow<Boolean> = _authLoading.asStateFlow()

    private val _authError = MutableStateFlow<UiAuthError?>(null)
    val authError: StateFlow<UiAuthError?> = _authError.asStateFlow()

    private val _showPinForgotDialog = MutableStateFlow(false)
    val showPinForgotDialog: StateFlow<Boolean> = _showPinForgotDialog.asStateFlow()

    private val _forgotPinStep = MutableStateFlow(ForgotPinFlowStep.OFF)
    val forgotPinStep: StateFlow<ForgotPinFlowStep> = _forgotPinStep.asStateFlow()

    fun startForgotPin(phone: String? = null) {
        _forgotPinStep.value = ForgotPinFlowStep.ID_INPUT
    }

    fun closeForgotPin() {
        _forgotPinStep.value = ForgotPinFlowStep.OFF
    }

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _installationProgress = MutableStateFlow(0f)
    val installationProgress: StateFlow<Float> = _installationProgress.asStateFlow()

    private val _installationStatusText = MutableStateFlow("Initialisation de la session...")
    val installationStatusText: StateFlow<String> = _installationStatusText.asStateFlow()

    private val _isSessionChecking = MutableStateFlow(true)
    val isSessionChecking: StateFlow<Boolean> = _isSessionChecking.asStateFlow()

    // Skeleton Simulation Loading state (for snappy, Flutter/React Native-like skeleton UX)
    private val _isSkeletonLoading = MutableStateFlow(true)
    val isSkeletonLoading: StateFlow<Boolean> = _isSkeletonLoading.asStateFlow()

    // Data streams
    val conversations: StateFlow<List<Conversation>> = repository.conversationsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val groups: StateFlow<List<Group>> = repository.groupsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val statuses: StateFlow<List<StatusGroup>> = repository.getStatusesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Private Profile Media (Albums, Photos, Avatars, Covers)
    val profileMedia: StateFlow<List<ProfileMediaItem>> = repository.profileMediaFlow

    // --- CashPay Real Videos Feed State ---
    private val _realVideosFeed = MutableStateFlow<List<VideoItem>>(emptyList())
    val realVideosFeed: StateFlow<List<VideoItem>> = _realVideosFeed.asStateFlow()

    private val _isVideosLoading = MutableStateFlow(false)
    val isVideosLoading: StateFlow<Boolean> = _isVideosLoading.asStateFlow()

    val calls: StateFlow<List<CallRecord>> = repository.callsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Live Stage & Conference Streams ---
    private val _activeLiveRoom = MutableStateFlow<LiveStageRoom?>(null)
    val activeLiveRoom: StateFlow<LiveStageRoom?> = _activeLiveRoom.asStateFlow()

    private val _liveRoomsFeed = MutableStateFlow<List<LiveStageRoom>>(emptyList())
    val liveRoomsFeed: StateFlow<List<LiveStageRoom>> = _liveRoomsFeed.asStateFlow()

    // --- Incoming Call State ---
    private val _incomingCall = MutableStateFlow<IncomingCallNotificationState?>(null)
    val incomingCall: StateFlow<IncomingCallNotificationState?> = _incomingCall.asStateFlow()

    // --- Custom Ringtone Preference ---
    private val _selectedRingtoneTitle = MutableStateFlow("Sonnerie par défaut du système")
    val selectedRingtoneTitle: StateFlow<String> = _selectedRingtoneTitle.asStateFlow()

    private val _selectedRingtoneUri = MutableStateFlow<String?>("default")
    val selectedRingtoneUri: StateFlow<String?> = _selectedRingtoneUri.asStateFlow()

    // Active Chat
    private val _activeConversationId = MutableStateFlow<String?>(null)
    val activeConversationId: StateFlow<String?> = _activeConversationId.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val activeMessages: StateFlow<List<Message>> = _activeConversationId
        .flatMapLatest { convId ->
            if (convId != null) repository.getMessages(convId)
            else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Contacts verification
    private val _contactsResults = MutableStateFlow<List<ContactCheckResult>>(emptyList())
    val contactsResults: StateFlow<List<ContactCheckResult>> = _contactsResults.asStateFlow()

    private val _isContactsLoading = MutableStateFlow(false)
    val isContactsLoading: StateFlow<Boolean> = _isContactsLoading.asStateFlow()

    // Active call session
    private val _activeCall = MutableStateFlow<CallSessionState?>(null)
    val activeCall: StateFlow<CallSessionState?> = _activeCall.asStateFlow()

    // Real active contacts stream
    val activeContacts: StateFlow<List<ContactCheckResult>> = repository.conversationsFlow
        .map { convs: List<Conversation> ->
            convs.map { c: Conversation ->
                ContactCheckResult(
                    phone = c.contactPhone ?: "",
                    onChat = true,
                    name = c.name,
                    avatar = c.avatarUrl,
                    userId = null,
                    walletId = c.contactWalletId,
                    directBitcoinAddress = c.contactBitcoinAddress
                )
            }.distinctBy { it.walletId ?: it.phone }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        com.example.data.api.SessionTokenStore.init(application)
        loadCountries()
        checkExistingSession()
        observeWebSocketEvents()
        wsManager.onReconnected = {
            fetchServerConversations()
            refreshWallet()
        }

        // Active background sync loop to guarantee 100% zero missed messages or statuses (only when authenticated)
        viewModelScope.launch {
            while (isActive) {
                delay(10000) // Reduced frequency to 10s to prevent UI flicker
                try {
                    if (_loginStep.value == LoginStep.AUTHENTICATED && !ApiClient.authToken.isNullOrBlank()) {
                        repository.fetchConversations()
                        val activeId = _activeConversationId.value
                        if (!activeId.isNullOrBlank()) {
                            repository.fetchConversationDetails(activeId)
                        }
                        repository.fetchStatusesFromServer()
                        repository.fetchVideos()
                        // Cleanup expired statuses from local DB
                        repository.repositoryCleanup()
                    }
                } catch (_: Exception) {}
            }
        }
    }

    private fun observeWebSocketEvents() {
        // Direct duplex voice audio reception
        wsManager.onRemoteAudioReceived = { audioBytes ->
            voiceCallManager.onIncomingRemoteAudio(audioBytes)
        }

        viewModelScope.launch {
            wsManager.incomingMessages.collect { pair ->
                repository.insertWebSocketMessage(pair.first, pair.second)
                if (pair.second.senderId == currentSession.value?.userId) return@collect
                if (_activeConversationId.value == pair.first) wsManager.sendReadReceipt(pair.first)
                
                // Play immediate incoming notification sound
                RingtoneHelper.playNotificationSound(getApplication())

                // If user is not currently inside this conversation, trigger system push notification
                if (_activeConversationId.value != pair.first) {
                    OnChatNotificationHelper.showMessageNotification(
                        context = getApplication(),
                        conversationId = pair.first,
                        senderName = pair.second.senderName ?: "Nouveau message",
                        senderWalletId = null,
                        messageText = pair.second.content
                    )
                }
            }
        }
        viewModelScope.launch {
            wsManager.incomingCalls.collect { callState ->
                _incomingCall.value = callState
                RingtoneHelper.playCallRingtone(getApplication())
                OnChatNotificationHelper.showIncomingCallNotification(
                    context = getApplication(),
                    callId = callState.callId,
                    callerName = callState.callerName,
                    callerWalletId = callState.callerWalletId,
                    callerAvatar = callState.callerAvatar,
                    isVideo = callState.isVideo
                )

                // 25-second ring timeout -> converts to missed call notification
                viewModelScope.launch {
                    delay(25000)
                    if (_incomingCall.value?.callId == callState.callId) {
                        val missed = _incomingCall.value
                        _incomingCall.value = null
                        RingtoneHelper.stopCallRingtone(getApplication())
                        OnChatNotificationHelper.cancelIncomingCallNotification(getApplication())
                        if (missed != null) {
                            repository.recordCall(
                                peerName = missed.callerName,
                                isVideo = missed.isVideo,
                                isIncoming = true,
                                isMissed = true,
                                durationSeconds = 0,
                                peerWalletId = missed.callerWalletId,
                                peerAvatar = missed.callerAvatar
                            )
                            OnChatNotificationHelper.showMissedCallNotification(
                                context = getApplication(),
                                callerName = missed.callerName,
                                callerWalletId = missed.callerWalletId.ifBlank { null },
                                isVideo = missed.isVideo
                            )
                        }
                    }
                }
            }
        }
        viewModelScope.launch {
            wsManager.callSignalingEvents.collect { envelope ->
                when (envelope.event) {
                    "chat.message.read", "chat.read" -> {
                        val conversationId = envelope.resolvedConversationId
                        val readerId = envelope.resolvedUserId
                        if (conversationId != null && readerId != null && readerId != currentSession.value?.userId) {
                            repository.markOutgoingMessagesAsRead(conversationId)
                        }
                    }
                    "call.accepted", "call.answer" -> {
                        _activeCall.value?.let { current ->
                            _activeCall.value = current.copy(isConnected = true)
                        }
                    }
                    "call.rejected", "call.ended" -> {
                        endCall()
                    }
                    "conference.invite" -> {
                        val title: String = envelope.data?.get("title")?.toString() ?: "Conférence OnChat"
                        val host: String = envelope.data?.get("hostName")?.toString() ?: "Un contact"
                        val isAudio: Boolean = envelope.data?.get("isAudioOnly")?.toString() == "true"
                        val callId: String = envelope.resolvedCallId ?: "conf_${System.currentTimeMillis()}"
                        val callerWId: String = envelope.data?.get("peerWalletId")?.toString() ?: envelope.resolvedWalletId ?: ""
                        val notifTitle: String = "$host - $title"
                        _incomingCall.value = IncomingCallNotificationState(
                            callId = callId,
                            callerName = notifTitle,
                            callerWalletId = callerWId,
                            callerAvatar = null,
                            isVideo = !isAudio
                        )
                        OnChatNotificationHelper.showIncomingCallNotification(
                            context = getApplication(),
                            callId = callId,
                            callerName = "$host vous invite : $title",
                            callerWalletId = callerWId,
                            callerAvatar = null,
                            isVideo = !isAudio
                        )
                    }
                }
            }
        }
    }

    fun loadCountries() {
        viewModelScope.launch {
            val list = repository.getCountries()
            _countries.value = list
            val match = list.find { it.code.equals(_selectedCountry.value.code, ignoreCase = true) }
            if (match != null) {
                _selectedCountry.value = match
            } else if (list.isNotEmpty()) {
                _selectedCountry.value = list.first()
            }
        }
    }

    fun onAuthMethodChange(method: AuthMethod) {
        _authMethod.value = method
        _authError.value = null
    }

    fun onSelectCountry(country: Country) {
        _selectedCountry.value = country
        _authError.value = null
    }

    fun onPhoneNumberChange(phone: String) {
        _phoneNumberInput.value = phone
        _authError.value = null
    }

    fun clearSuccessMessage() {
        _successMessage.value = null
    }

    fun showSuccessMessage(msg: String) {
        _successMessage.value = msg
    }

    // Public Profile State
    private val _publicProfile = MutableStateFlow<PhoneProfileResponse?>(null)
    val publicProfile: StateFlow<PhoneProfileResponse?> = _publicProfile.asStateFlow()

    private val _isPublicProfileLoading = MutableStateFlow(false)
    val isPublicProfileLoading: StateFlow<Boolean> = _isPublicProfileLoading.asStateFlow()

    fun fetchPublicProfile(phone: String) {
        viewModelScope.launch {
            _isPublicProfileLoading.value = true
            val result = repository.getPublicProfile(phone)
            _isPublicProfileLoading.value = false
            result.onSuccess {
                _publicProfile.value = it
            }.onFailure {
                _errorMessage.value = "Impossible de charger le profil"
            }
        }
    }

    fun performSocialAction(userId: Long, action: String, relationType: String? = null) {
        viewModelScope.launch {
            val result = repository.performSocialAction(userId, action, relationType)
            result.onSuccess { res ->
                // Update local public profile state with new relation/stats in nested object
                val current = _publicProfile.value
                val p = current?.profile
                if (current != null && p != null) {
                    val newRelationship = res.relation?.let {
                        RelationshipDto(
                            isFollowing = it.following,
                            isFollowedBy = it.isFriend
                        )
                    } ?: p.relationship

                    val newFollowers = res.stats?.let {
                        FollowersDto(count = it.followers)
                    } ?: p.followers

                    val newFollowing = res.stats?.let {
                        FollowersDto(count = it.following)
                    } ?: p.following

                    _publicProfile.value = current.copy(
                        profile = p.copy(
                            relationship = newRelationship,
                            followers = newFollowers,
                            following = newFollowing
                        )
                    )
                }

                if (!res.message.isNullOrBlank()) {
                    _successMessage.value = res.message
                }
            }.onFailure {
                _errorMessage.value = it.message ?: "Action échouée"
            }
        }
    }

    private fun checkExistingSession() {
        viewModelScope.launch {
            _isSessionChecking.value = true

            // Check if valid session exists
            val session = repository.getSession()
            if (session != null && (session.token.isNotEmpty() || session.phone.isNotEmpty() || session.userId > 0L)) {
                ApiClient.authToken = session.token.ifBlank { null }
                ApiClient.activeWalletId = session.walletId

                val isValid = repository.checkSession()
                if (isValid) {
                    _loginStep.value = LoginStep.AUTHENTICATED
                    _authError.value = null
                    wsManager.connect()
                    fetchServerConversations()
                    fetchTransactions()
                    refreshWallet()
                } else {
                    _loginStep.value = LoginStep.WALLET_ID
                    _authError.value = null
                }
            } else {
                _loginStep.value = LoginStep.WALLET_ID
                _authError.value = null
            }

            _isSkeletonLoading.value = false
        }
    }

    fun finishSplash() {
        _isSessionChecking.value = false
    }

    fun triggerRefreshSkeleton() {
        viewModelScope.launch {
            repository.fetchGroups()
            fetchTransactions()
        }
    }

    // --- Auth Actions ---

    fun onWalletIdChange(id: String) {
        _walletIdInput.value = id
        _authError.value = null
    }

    fun onOtpChange(otp: String) {
        _otpInput.value = otp
        _authError.value = null
    }

    fun onPinChange(pin: String) {
        _pinInput.value = pin
        _authError.value = null
    }

    fun onPinDigit(digit: Char) {
        if (_pinInput.value.length < 4) {
            val newPin = _pinInput.value + digit
            _pinInput.value = newPin
            _authError.value = null
            if (newPin.length == 4) {
                submitPin(newPin)
            }
        }
    }

    fun onPinBackspace() {
        if (_pinInput.value.isNotEmpty()) {
            _pinInput.value = _pinInput.value.dropLast(1)
            _authError.value = null
        }
    }

    fun openPinForgotDialog() {
        _showPinForgotDialog.value = true
    }

    fun closePinForgotDialog() {
        _showPinForgotDialog.value = false
    }

    fun changeVerificationMethod() {
        _authError.value = null
        _otpInput.value = ""
        _loginStep.value = LoginStep.CHANNEL_SELECTION
    }

    fun restartLogin() {
        _authError.value = null
        _selectedChannel.value = null
        _availableChannels.value = emptyList()
        _otpInput.value = ""
        _pinInput.value = ""
        _loginStep.value = LoginStep.WALLET_ID
    }

    fun changeWalletId() {
        restartLogin()
    }

    private fun processAuthSuccess(auth: AuthResponse) {
        _pendingUserId.value = auth.userId ?: auth.user?.id ?: 1L
        _isGoogleLinked.value = (auth.isGoogleLinked == true || auth.googleLinked == true)

        val phoneToMask = auth.phone ?: auth.user?.phone
        val emailToMask = auth.email ?: auth.user?.email

        val channels = mutableListOf<AvailableChannel>()
        if (!auth.availableChannels.isNullOrEmpty()) {
            for (ch in auth.availableChannels) {
                if (!ch.available) continue
                val maskedDest = ch.maskedDestination ?: when (ch.type.lowercase()) {
                    "sms", "whatsapp" -> auth.maskedPhone ?: com.example.data.repository.AuthMaskHelper.maskPhone(phoneToMask)
                    "email" -> auth.maskedEmail ?: com.example.data.repository.AuthMaskHelper.maskEmail(emailToMask)
                    else -> null
                }
                channels.add(ch.copy(maskedDestination = maskedDest))
            }
        } else if (auth.user != null || auth.phone != null || auth.email != null) {
            if (!phoneToMask.isNullOrBlank()) {
                channels.add(
                    AvailableChannel(
                        type = "sms",
                        available = true,
                        maskedDestination = auth.maskedPhone ?: com.example.data.repository.AuthMaskHelper.maskPhone(phoneToMask)
                    )
                )
            }
            if (!emailToMask.isNullOrBlank()) {
                channels.add(
                    AvailableChannel(
                        type = "email",
                        available = true,
                        maskedDestination = auth.maskedEmail ?: com.example.data.repository.AuthMaskHelper.maskEmail(emailToMask)
                    )
                )
            }
        }

        _availableChannels.value = channels

        if (channels.isEmpty() && !_isGoogleLinked.value) {
            _authError.value = UiAuthError(
                title = "Aucune méthode de vérification disponible",
                message = "Aucun moyen de recevoir un code de vérification n'est actuellement disponible pour ce compte."
            )
        } else {
            // Do NOT send OTP automatically. Show channel selection screen.
            _loginStep.value = LoginStep.CHANNEL_SELECTION
        }
    }

    fun submitWalletId() {
        val wallet = _walletIdInput.value.trim()
        if (wallet.isEmpty()) {
            _authError.value = UiAuthError(
                title = "Wallet requis",
                message = "Veuillez saisir votre Wallet ID."
            )
            return
        }
        viewModelScope.launch {
            _authLoading.value = true
            _authError.value = null
            _successMessage.value = null
            val result = repository.loginWithWalletId(wallet)
            _authLoading.value = false

            result.onSuccess { auth ->
                processAuthSuccess(auth)
            }.onFailure { err ->
                if (err is com.example.data.repository.AuthException) {
                    _authError.value = UiAuthError(err.title, err.detail)
                } else {
                    _authError.value = UiAuthError(
                        title = "Wallet introuvable",
                        message = "Nous n'avons trouvé aucun compte CashPay associé à ce Wallet ID."
                    )
                }
            }
        }
    }

    fun getNormalizedPhoneNumber(): String {
        val raw = _phoneNumberInput.value.trim()
        if (raw.isEmpty()) return ""
        val dialCode = _selectedCountry.value.cleanDialCode
        val digitsOnly = raw.replace("+", "").filter { it.isDigit() }
        return if (raw.startsWith("+")) {
            digitsOnly
        } else if (digitsOnly.startsWith(dialCode)) {
            digitsOnly
        } else {
            val nationalWithoutZero = digitsOnly.trimStart('0')
            dialCode + nationalWithoutZero
        }
    }

    fun submitPhone() {
        val raw = _phoneNumberInput.value.trim()
        if (raw.isEmpty()) {
            _authError.value = UiAuthError(
                title = "Numéro requis",
                message = "Veuillez saisir votre numéro de téléphone."
            )
            return
        }

        val normalizedNumber = getNormalizedPhoneNumber()

        if (normalizedNumber.length < 6) {
            _authError.value = UiAuthError(
                title = "Numéro incomplet",
                message = "Veuillez vérifier le numéro de téléphone saisi."
            )
            return
        }

        viewModelScope.launch {
            _authLoading.value = true
            _authError.value = null
            _successMessage.value = null
            val result = repository.loginWithPhone(normalizedNumber)
            _authLoading.value = false

            result.onSuccess { auth ->
                processAuthSuccess(auth)
            }.onFailure { err ->
                if (err is com.example.data.repository.AuthException) {
                    _authError.value = UiAuthError(err.title, err.detail)
                } else {
                    _authError.value = UiAuthError(
                        title = "Numéro introuvable",
                        message = "Nous n'avons trouvé aucun compte CashPay associé à ce numéro de téléphone."
                    )
                }
            }
        }
    }

    fun selectChannelAndSendOtp(channel: AvailableChannel) {
        _selectedChannel.value = channel
        viewModelScope.launch {
            _authLoading.value = true
            _authError.value = null
            val result = repository.sendOtp(_pendingUserId.value, channel.type)
            _authLoading.value = false

            result.onSuccess { res ->
                _loginStep.value = LoginStep.OTP_VERIFY
                _otpInput.value = ""
                val dest = res.maskedDestination ?: channel.maskedDestination ?: ""
                val channelLabel = when (channel.type.lowercase()) {
                    "sms" -> "SMS"
                    "whatsapp" -> "WhatsApp"
                    "email" -> "Email"
                    "in-app" -> "l'application d'authentification"
                    else -> channel.type
                }
                _successMessage.value = if (dest.isNotEmpty()) {
                    "Un code à 6 chiffres a été envoyé par $channelLabel au $dest."
                } else {
                    "Un code à 6 chiffres a été envoyé par $channelLabel."
                }
            }.onFailure { err ->
                if (err is com.example.data.repository.AuthException) {
                    _authError.value = UiAuthError(err.title, err.detail)
                } else {
                    _authError.value = UiAuthError(
                        title = "Méthode indisponible",
                        message = "Cette méthode de vérification n'est pas disponible pour votre compte."
                    )
                }
            }
        }
    }

    fun continueWithGoogle() {
        if (!_isGoogleLinked.value) {
            _authError.value = UiAuthError(
                title = "Méthode indisponible",
                message = "Cette méthode de vérification n'est pas disponible pour votre compte."
            )
            return
        }
        viewModelScope.launch {
            _authLoading.value = true
            _authError.value = null
            // Google auth proceeds directly to PIN verification
            _loginStep.value = LoginStep.PIN_VERIFY
            _authLoading.value = false
            _successMessage.value = "Authentification Google validée. Veuillez saisir votre PIN."
        }
    }

    fun resendOtp() {
        val channel = _selectedChannel.value ?: _availableChannels.value.firstOrNull()
        if (channel == null) {
            _authError.value = UiAuthError(
                title = "Méthode indisponible",
                message = "Cette méthode de vérification n'est pas disponible pour votre compte."
            )
            return
        }
        selectChannelAndSendOtp(channel)
    }

    fun submitOtp() {
        val otp = _otpInput.value.trim()
        if (otp.length < 4) {
            _authError.value = UiAuthError(
                title = "Code requis",
                message = "Veuillez entrer le code de vérification reçu."
            )
            return
        }
        viewModelScope.launch {
            _authLoading.value = true
            _authError.value = null
            val result = repository.verifyOtp(_pendingUserId.value, otp, _walletIdInput.value)
            _authLoading.value = false

            result.onSuccess {
                _loginStep.value = LoginStep.PIN_VERIFY
                _pinInput.value = ""
                _successMessage.value = "Code de vérification validé !"
            }.onFailure { err ->
                if (err is com.example.data.repository.AuthException) {
                    _authError.value = UiAuthError(err.title, err.detail)
                } else {
                    _authError.value = UiAuthError(
                        title = "Code incorrect",
                        message = "Le code de vérification saisi est incorrect."
                    )
                }
            }
        }
    }

    fun submitPin(pinToVerify: String = _pinInput.value) {
        val pin = pinToVerify.trim()
        if (pin.length < 4) {
            _authError.value = UiAuthError(
                title = "PIN incomplet",
                message = "Veuillez saisir votre code PIN CashPay."
            )
            return
        }
        viewModelScope.launch {
            _authLoading.value = true
            _authError.value = null
            val result = repository.verifyPin(_pendingUserId.value, pin, _walletIdInput.value)
            _authLoading.value = false

            result.onSuccess {
                _loginStep.value = LoginStep.PROFILE_INSTALLATING
                _pinInput.value = ""
                _successMessage.value = "Authentification réussie ! Installation du profil en cours..."
                runProfileInstallation()
            }.onFailure { err ->
                _pinInput.value = ""
                if (err is com.example.data.repository.AuthException) {
                    _authError.value = UiAuthError(err.title, err.detail)
                } else {
                    _authError.value = UiAuthError(
                        title = "PIN incorrect",
                        message = "Le PIN saisi est incorrect."
                    )
                }
            }
        }
    }

    fun runProfileInstallation() {
        viewModelScope.launch {
            _installationProgress.value = 0f
            _installationStatusText.value = "Connexion sécurisée en cours..."
            delay(200)

            _installationProgress.value = 0.30f
            _installationStatusText.value = "Récupération de votre profil CashPay..."
            val phone = getNormalizedPhoneNumber().ifBlank { null }
            val authResult = repository.fetchAuthMe(phone)

            _installationProgress.value = 0.60f
            _installationStatusText.value = "Configuration de vos identifiants..."
            delay(150)

            _installationProgress.value = 0.90f
            _installationStatusText.value = "Synchronisation de vos préférences et du compte..."
            delay(150)

            _installationProgress.value = 1.0f
            _installationStatusText.value = "Configuration terminée avec succès !"
            delay(150)
            _loginStep.value = LoginStep.PROFILE_CONFIRMATION
        }
    }

    fun confirmProfile() {
        _loginStep.value = LoginStep.AUTHENTICATED
        _successMessage.value = "Profil installé et configuré. Bienvenue sur OnChat."
        wsManager.connect()
        fetchServerConversations()
        fetchTransactions()
        refreshWallet()
    }

    fun cancelProfileInstallation() {
        viewModelScope.launch {
            wsManager.disconnect()
            repository.logout()
            _loginStep.value = LoginStep.WALLET_ID
            _otpInput.value = ""
            _pinInput.value = ""
            _installationProgress.value = 0f
        }
    }

    fun logout() {
        viewModelScope.launch {
            wsManager.disconnect()
            repository.logout()
            _activeConversationId.value = null
            _activeCall.value = null
            _incomingCall.value = null
            _walletData.value = null
            _transactions.value = emptyList()
            _loginStep.value = LoginStep.WALLET_ID
            _otpInput.value = ""
            _pinInput.value = ""
            _installationProgress.value = 0f
        }
    }

    fun saveSession(phone: String, token: String) {
        viewModelScope.launch {
            repository.saveSession(phone, token)
        }
    }

    fun loadProfileByPhone(phone: String) {
        viewModelScope.launch {
            val result = repository.getPublicProfile(phone)
            result.onSuccess {
                _activeUserProfile.value = it
            }
        }
    }

    fun uploadVideo(uri: Uri, caption: String = "", onComplete: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            _isUploading.value = true
            _uploadProgress.value = 0.05f
            var exported: java.io.File? = null
            try {
                val context = getApplication<Application>()
                val uploadUri = if (caption.isBlank()) uri else {
                    val duration = com.example.media.VideoExporter.durationMs(context, uri)
                    val output = com.example.media.VideoExporter.export(context, uri, duration,
                        com.example.media.VideoExporter.Edits(0f, 1f, 1f, 1f, false, "Normal",
                            listOf(com.example.media.VideoExporter.Label(caption, android.graphics.Color.WHITE, 0x99000000.toInt(), 0f, -0.8f))))
                    exported = output
                    Uri.fromFile(output)
                }
                val bytes = withContext(Dispatchers.IO) { context.contentResolver.openInputStream(uploadUri)?.use { it.readBytes() } }
                if (bytes == null) {
                    onComplete(false, "Impossible de lire le fichier")
                    return@launch
                }
                
                val mimeType = context.contentResolver.getType(uploadUri) ?: "video/mp4"
                val extension = android.webkit.MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType) ?: "mp4"
                val fileName = "video_${System.currentTimeMillis()}.$extension"
                val currentUserId = currentSession.value?.userId
                val result = repository.uploadVideoGeneral(bytes, fileName, mimeType, publisherId = currentUserId)
                
                if (!result.isNullOrBlank()) {
                    _uploadProgress.value = 1.0f
                    onComplete(true, null)
                } else {
                    onComplete(false, "Échec de l'envoi au serveur")
                }
            } catch (e: Exception) {
                onComplete(false, e.message)
            } finally {
                exported?.delete()
                _isUploading.value = false
            }
        }
    }

    fun sendAudioMessage(file: java.io.File) {
        val convId = _activeConversationId.value ?: return
        viewModelScope.launch {
            try {
                val bytes = file.readBytes()
                val fileName = "voice_${System.currentTimeMillis()}.m4a"
                val url = repository.uploadMedia(bytes, fileName, "audio/mp4")
                if (url != null) {
                    val finalUrl = if (url.startsWith("http")) url else "https://app.cashpay-all.com$url"
                    repository.sendMessage(convId, "🎤 Note vocale", type = "audio", fileUrl = finalUrl, fileName = fileName)
                }
            } catch (e: Exception) {
                Log.e("OnChatViewModel", "sendAudioMessage error: ${e.message}")
            }
        }
    }

    fun forwardMessageToConversations(
        targetConversationIds: List<String>,
        message: Message,
        onComplete: (() -> Unit)? = null
    ) {
        viewModelScope.launch {
            try {
            targetConversationIds.forEach { targetConvId ->
                repository.sendMessage(
                    conversationId = targetConvId,
                    content = message.content,
                    type = message.type,
                    fileUrl = message.fileUrl,
                    fileName = message.fileName
                )
            }
            onComplete?.invoke()
            } catch (e: kotlinx.coroutines.CancellationException) { throw e }
            catch (e: Exception) { _errorMessage.value = e.message ?: "Échec du transfert du message" }
        }
    }

    fun setOnboardingCompleted(completed: Boolean) {
        // No-op for now or implement if repository supports it
    }

    fun toggleBiometric(enabled: Boolean) {
        viewModelScope.launch {
            repository.setBiometricEnabled(enabled)
        }
    }

    fun setChatBackgroundUri(uri: String?) {
        viewModelScope.launch {
            repository.setChatBackgroundUri(uri)
        }
    }

    fun fetchServerConversations() {
        if (ApiClient.authToken.isNullOrBlank()) return
        viewModelScope.launch {
            if (conversations.value.isEmpty()) {
                _isSkeletonLoading.value = true
            }
            try {
                repository.fetchConversations()
            } catch (_: Exception) {}
            _isSkeletonLoading.value = false
        }
    }

    fun fetchConversationDetails(conversationId: String) {
        if (ApiClient.authToken.isNullOrBlank()) return
        viewModelScope.launch {
            repository.fetchConversationDetails(conversationId)
        }
    }

    fun fetchAuthMe() {
        viewModelScope.launch {
            val phone = getNormalizedPhoneNumber().ifBlank { null }
            repository.fetchAuthMe(phone)
            repository.fetchProfileMedia()
        }
    }

    fun fetchProfileMedia() {
        viewModelScope.launch {
            repository.fetchProfileMedia()
        }
    }

    fun uploadProfileMedia(
        uri: Uri,
        mediaType: String = "album_image",
        visibility: String = "public",
        onProgress: ((Float) -> Unit)? = null,
        onComplete: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            try {
                onProgress?.invoke(0.1f)
                val context = getApplication<Application>()
                val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
                onProgress?.invoke(0.3f)
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                if (bytes == null) {
                    onComplete(false, "Impossible de lire le fichier")
                    return@launch
                }
                onProgress?.invoke(0.6f)
                val fileName = "media_${System.currentTimeMillis()}.${if (mimeType.contains("png")) "png" else if (mimeType.contains("video")) "mp4" else "jpg"}"
                val result = repository.uploadProfileMedia(bytes, fileName, mimeType, mediaType, visibility)
                onProgress?.invoke(0.9f)
                result.onSuccess {
                    onProgress?.invoke(1.0f)
                    onComplete(true, null)
                }.onFailure { err ->
                    onComplete(false, err.message)
                }
            } catch (e: Exception) {
                onComplete(false, e.message)
            }
        }
    }

    suspend fun uploadMediaBytes(bytes: ByteArray, fileName: String, mimeType: String): String? {
        val result = repository.uploadMedia(bytes, fileName, mimeType)
        if (!result.isNullOrBlank()) {
            return if (result.startsWith("http")) result else "https://app.cashpay-all.com$result"
        }
        return null
    }

    suspend fun uploadVideoGeneral(bytes: ByteArray, fileName: String, mimeType: String): String? {
        val result = repository.uploadVideoGeneral(bytes, fileName, mimeType)
        if (!result.isNullOrBlank()) {
            return if (result.startsWith("http")) result else "https://app.cashpay-all.com$result"
        }
        return null
    }

    // --- CashPay Real Videos Feed Actions ---

    fun fetchVideosFeed() {
        viewModelScope.launch {
            _isVideosLoading.value = true
            val result = repository.fetchVideos(limit = 50, offset = 0)
            _isVideosLoading.value = false
            result.onSuccess { list ->
                _realVideosFeed.value = list
            }.onFailure { err ->
                Log.w("OnChatViewModel", "fetchVideosFeed error: ${err.message}")
            }
        }
    }

    fun toggleVideoLike(videoId: Long, currentIsLiked: Boolean) {
        viewModelScope.launch {
            val result = if (currentIsLiked) {
                repository.unlikeVideo(videoId)
            } else {
                repository.likeVideo(videoId)
            }

            result.onSuccess { res ->
                // Update video item in realVideosFeed with server's actual likes count and isLiked status
                _realVideosFeed.value = _realVideosFeed.value.map { item ->
                    if (item.id == videoId) {
                        item.copy(
                            interactions = VideoInteractions(
                                likes = res.likes,
                                comments = item.interactions?.comments ?: 0,
                                isLiked = res.liked,
                                likedByMe = res.liked
                            )
                        )
                    } else {
                        item
                    }
                }
            }.onFailure { err ->
                _errorMessage.value = "Impossible d'enregistrer le j'aime: ${err.message}"
            }
        }
    }

    fun loadVideoComments(videoId: Long, onResult: (List<VideoCommentItem>) -> Unit) {
        viewModelScope.launch {
            val result = repository.getVideoComments(videoId)
            result.onSuccess { list ->
                onResult(list)
            }.onFailure {
                onResult(emptyList())
            }
        }
    }

    fun postVideoComment(videoId: Long, content: String, onResult: (VideoCommentItem?) -> Unit) {
        if (content.isBlank()) return
        viewModelScope.launch {
            val result = repository.postVideoComment(videoId, content.trim())
            result.onSuccess { comment ->
                // Update comment count on video in feed
                _realVideosFeed.value = _realVideosFeed.value.map { item ->
                    if (item.id == videoId) {
                        val currentComments = item.interactions?.comments ?: 0
                        item.copy(
                            interactions = (item.interactions ?: VideoInteractions()).copy(
                                comments = currentComments + 1
                            )
                        )
                    } else {
                        item
                    }
                }
                onResult(comment)
            }.onFailure { err ->
                _errorMessage.value = "Impossible de publier le commentaire: ${err.message}"
                onResult(null)
            }
        }
    }

    fun deleteVideo(videoId: Long) {
        viewModelScope.launch {
            val result = repository.deleteVideo(videoId)
            result.onSuccess {
                _realVideosFeed.value = _realVideosFeed.value.filter { it.id != videoId }
            }
        }
    }

    fun setProfileMedia(mediaId: Long, onComplete: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val result = repository.setProfileMedia(mediaId)
            result.onSuccess {
                onComplete(true, null)
            }.onFailure {
                onComplete(false, it.message)
            }
        }
    }

    fun setCoverMedia(mediaId: Long, onComplete: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val result = repository.setCoverMedia(mediaId)
            result.onSuccess {
                onComplete(true, null)
            }.onFailure {
                onComplete(false, it.message)
            }
        }
    }

    fun updateProfileWithPin(
        pin: String,
        fullName: String?,
        profession: String? = null,
        activityDescription: String? = null,
        address: String? = null,
        city: String? = null,
        province: String? = null,
        country: String? = null,
        nationality: String? = null,
        language: String? = null,
        birthDate: String? = null,
        availabilityStatus: String? = null,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            _authLoading.value = true
            val updates = ProfileUpdates(
                fullName = fullName,
                profession = profession,
                activityDescription = activityDescription,
                address = address,
                city = city,
                province = province,
                country = country,
                nationality = nationality,
                language = language,
                birthDate = birthDate,
                availabilityStatus = availabilityStatus
            )
            val result = repository.updateProfileWithPin(pin, updates)
            _authLoading.value = false
            result.onSuccess {
                onSuccess()
            }.onFailure { err ->
                val msg = if (err is com.example.data.repository.AuthException) err.detail else err.message ?: "Erreur de mise à jour"
                onError(msg)
            }
        }
    }

    fun updateProfile(
        name: String? = null,
        bio: String? = null,
        avatarUrl: String? = null,
        email: String? = null,
        username: String? = null,
        birthDate: String? = null
    ) {
        viewModelScope.launch {
            repository.updateProfile(name, bio, avatarUrl, email, username, birthDate)
        }
    }

    fun deleteConversation(conversationId: String) {
        viewModelScope.launch {
            repository.deleteConversation(conversationId)
            if (_activeConversationId.value == conversationId) {
                _activeConversationId.value = null
            }
        }
    }

    // --- In-Chat Financial Transfer Engine (CashPay TR) ---

    suspend fun getTransferQuote(amount: Double, currency: String = "USD"): TransferQuoteResponse? {
        val convId = _activeConversationId.value ?: return null
        val result = repository.getTransferQuote(convId, amount, currency)
        return result.getOrNull()
    }

    suspend fun previewTransfer(
        receiverWalletId: String?,
        recipientAddress: String?,
        amount: Double,
        currency: String
    ): Result<TransferResponse> {
        return repository.previewTransfer(receiverWalletId, recipientAddress, amount, currency)
    }

    suspend fun executeInChatTransfer(
        conversationId: String,
        amount: Double,
        currency: String,
        pin: String,
        receiverWalletId: String? = null,
        recipientAddress: String? = null,
        note: String = "Transfert OnChat"
    ): Result<TransferResponse> {
        val result = repository.executeInChatTransfer(
            conversationId = conversationId,
            amount = amount,
            currency = currency,
            pin = pin,
            receiverWalletId = receiverWalletId,
            recipientAddress = recipientAddress
        )
        if (result.isSuccess) {
            refreshWallet()
        }
        return result
    }

    // --- Chat & Messages ---

    fun openGroupChat(groupId: Long, name: String) {
        viewModelScope.launch {
            val convId = repository.getOrCreateConversationForGroup(groupId, name)
            _activeConversationId.value = convId
            repository.fetchConversationDetails(convId)
        }
    }

    fun openContactChat(
        name: String,
        avatarUrl: String? = null,
        phone: String? = null,
        walletId: String? = null,
        recipientUserId: Long? = null,
        bitcoinAddress: String? = null
    ) {
        viewModelScope.launch {
            val convId = repository.getOrCreateConversationForContact(
                name = name,
                phone = phone,
                walletId = walletId,
                avatarUrl = avatarUrl,
                recipientUserId = recipientUserId,
                bitcoinAddress = bitcoinAddress
            )
            _activeConversationId.value = convId
        }
    }

    suspend fun resolveConversationRecipient(conv: Conversation?): Pair<String?, String?> {
        if (conv == null) return Pair(null, null)
        return repository.resolveRecipientData(
            conversationId = conv.id,
            name = conv.name,
            currentPhone = conv.contactPhone,
            currentWalletId = conv.contactWalletId,
            currentBtcAddress = conv.contactBitcoinAddress
        )
    }

    fun openConversation(conversationId: String) {
        _activeConversationId.value = conversationId
        onEnterChat(conversationId)
    }

    fun onEnterChat(conversationId: String) {
        _activeConversationId.value = conversationId
        viewModelScope.launch {
            repository.markConversationMessagesAsRead(conversationId)
            wsManager.sendReadReceipt(conversationId)
            // Force re-fetch to ensure UI gets updated state
            repository.fetchConversationDetails(conversationId)
        }
    }

    fun sendTyping(conversationId: String) {
        wsManager.sendTyping(conversationId)
    }

    fun sendReadReceipt(conversationId: String) {
        wsManager.sendReadReceipt(conversationId)
    }

    fun closeConversation() {
        _activeConversationId.value = null
    }

    fun sendMessage(
        text: String,
        type: String = "text",
        metadata: Map<String, String>? = null,
        fileUrl: String? = null,
        fileName: String? = null
    ) {
        val convId = _activeConversationId.value ?: return
        if (text.isBlank()) return
        viewModelScope.launch {
            try { repository.sendMessage(
                conversationId = convId,
                content = text.trim(),
                type = type,
                metadata = metadata,
                fileUrl = fileUrl,
                fileName = fileName
            ) } catch (e: kotlinx.coroutines.CancellationException) { throw e }
            catch (e: Exception) { _errorMessage.value = e.message ?: "Échec de l'envoi" }
        }
    }

    fun editMessage(messageId: String, newText: String) {
        viewModelScope.launch {
            repository.editMessage(messageId, newText.trim())
        }
    }

    fun deleteMessage(messageId: String) {
        viewModelScope.launch {
            val convId = _activeConversationId.value ?: ""
            repository.deleteMessage(convId, messageId)
        }
    }

    // --- Groups ---

    fun createGroup(name: String, currency: String, description: String?) {
        viewModelScope.launch {
            repository.createGroup(name, currency, description, null)
        }
    }

    fun addGroupMember(groupId: Long, userId: Long) {
        viewModelScope.launch {
            repository.addMemberToGroup(groupId, userId)
        }
    }

    fun removeGroupMember(groupId: Long, memberId: Long) {
        viewModelScope.launch {
            repository.removeMemberFromGroup(groupId, memberId)
        }
    }

    // --- Statuses ---

    fun postStatus(
        type: String,
        content: String? = null,
        backgroundColor: String? = null,
        imageUri: Uri? = null,
        videoUri: Uri? = null,
        mediaUrl: String? = null,
        visibility: String = "public"
    ) {
        viewModelScope.launch {
            val context = getApplication<Application>()
            var success = false

            if (type == "image" && imageUri != null) {
                try {
                    val bytes = context.contentResolver.openInputStream(imageUri)?.use { it.readBytes() }
                    if (bytes != null) {
                        val mimeType = context.contentResolver.getType(imageUri) ?: "image/jpeg"
                        val fileName = "status_${System.currentTimeMillis()}.${if (mimeType.contains("png")) "png" else "jpg"}"
                        val result = repository.postStatusWithFile(
                            fileBytes = bytes,
                            fileName = fileName,
                            mimeType = mimeType,
                            type = "image",
                            backgroundColor = backgroundColor
                        )
                        success = result.getOrDefault(false)
                    }
                } catch (e: Exception) {
                    Log.w("OnChatViewModel", "Error posting multipart image status: ${e.message}")
                }
            } else if (type == "video" && videoUri != null) {
                try {
                    val bytes = context.contentResolver.openInputStream(videoUri)?.use { it.readBytes() }
                    if (bytes != null) {
                        val mimeType = context.contentResolver.getType(videoUri) ?: "video/mp4"
                        val fileName = "status_${System.currentTimeMillis()}.mp4"
                        val result = repository.postStatusWithFile(
                            fileBytes = bytes,
                            fileName = fileName,
                            mimeType = mimeType,
                            type = "video",
                            backgroundColor = backgroundColor
                        )
                        success = result.getOrDefault(false)
                    }
                } catch (e: Exception) {
                    Log.w("OnChatViewModel", "Error posting multipart video status: ${e.message}")
                }
            }

            if (!success) {
                // Fallback to json body create if multipart is skipped or failed
                repository.postStatus(
                    type = type,
                    content = content,
                    backgroundColor = backgroundColor,
                    mediaUrl = mediaUrl,
                    visibility = visibility
                )
            }
            repository.fetchStatusesFromServer()
        }
    }

    fun recordStatusView(statusId: Long) {
        viewModelScope.launch {
            repository.recordStatusView(statusId)
        }
    }

    fun toggleStatusLike(statusId: Long, liked: Boolean) {
        viewModelScope.launch {
            repository.toggleStatusLike(statusId, liked)
        }
    }

    fun checkStatusesForContacts(phoneNumbers: List<String>) {
        viewModelScope.launch {
            repository.checkStatuses(phoneNumbers).onSuccess { contacts ->
                // In a real app, we would merge these into the local DB or StateFlow
                // For now, fetchStatusesFromServer() handles the general sync
            }
        }
    }

    // --- Contacts Verification ---

    fun checkContacts(samplePhoneNumbers: List<String>) {
        viewModelScope.launch {
            _isContactsLoading.value = true
            try {
                repository.checkContactsStream(samplePhoneNumbers)
                    .catch { e -> android.util.Log.e("OnChatViewModel", "Contacts stream error", e) }
                    .collect { updatedList ->
                        _contactsResults.value = updatedList
                    }
            } catch (e: Exception) {
                android.util.Log.e("OnChatViewModel", "Contacts verification error", e)
            } finally {
                _isContactsLoading.value = false
            }
        }
    }

    // --- Audio & Video Calls ---

    fun startCall(
        peerName: String,
        peerWalletId: String = "",
        peerAvatar: String? = null,
        isVideo: Boolean = false,
        autoConnect: Boolean = false
    ) {
        val roomId = "call_${System.currentTimeMillis()}"
        _activeCall.value = CallSessionState(
            roomId = roomId,
            peerName = peerName,
            peerWalletId = peerWalletId,
            peerAvatar = peerAvatar,
            isVideo = isVideo,
            isConnected = autoConnect,
            isMuted = false,
            isSpeakerOn = true,
            isCameraFacingBack = false,
            durationSeconds = 0
        )

        OnChatNotificationHelper.cancelIncomingCallNotification(getApplication())

        // Send real-time call signaling through WebSocket
        if (!autoConnect) {
            wsManager.sendCallSignaling(
                event = "call.offer",
                callId = roomId,
                conversationId = null,
                data = mapOf(
                    "peerWalletId" to peerWalletId,
                    "peerName" to peerName,
                    "isVideo" to isVideo.toString()
                )
            )
        }

        viewModelScope.launch {
            // Forward recorded microphone audio chunks to remote peer via WebSocket
            voiceCallManager.onLocalAudioChunk = { chunk ->
                val current = _activeCall.value
                if (current != null && current.isConnected) {
                    val base64 = android.util.Base64.encodeToString(chunk, android.util.Base64.NO_WRAP)
                    wsManager.sendCallSignaling(
                        event = "call.audio",
                        callId = current.roomId,
                        conversationId = null,
                        data = mapOf(
                            "audio" to base64,
                            "peerWalletId" to current.peerWalletId
                        )
                    )
                }
            }

            // Start real duplex microphone & audio playback
            voiceCallManager.startVoiceSession()

            // Record initial call entry
            repository.recordCall(
                peerName = peerName,
                isVideo = isVideo,
                isIncoming = autoConnect,
                isMissed = false,
                durationSeconds = 0,
                peerWalletId = peerWalletId.ifBlank { null },
                peerAvatar = peerAvatar
            )

            // Duration counter starts counting ONLY when connected (after remote peer answers)
            while (_activeCall.value != null) {
                delay(1000)
                _activeCall.value?.let { current ->
                    if (current.isConnected) {
                        _activeCall.value = current.copy(durationSeconds = current.durationSeconds + 1)
                    }
                }
            }
        }
    }

    fun startCallByWalletId(walletId: String, isVideo: Boolean) {
        viewModelScope.launch {
            val contact = repository.findContactByWalletId(walletId)
            val name = contact?.name ?: "Contact ($walletId)"
            val avatar = contact?.avatarUrl
            startCall(
                peerName = name,
                peerWalletId = walletId,
                peerAvatar = avatar,
                isVideo = isVideo,
                autoConnect = false
            )
        }
    }

    fun toggleCallMute() {
        _activeCall.value = _activeCall.value?.let {
            val newMute = !it.isMuted
            voiceCallManager.setMute(newMute)
            it.copy(isMuted = newMute)
        }
    }

    fun toggleCallSpeaker() {
        _activeCall.value = _activeCall.value?.let {
            val newSpeaker = !it.isSpeakerOn
            voiceCallManager.setSpeakerOn(newSpeaker)
            it.copy(isSpeakerOn = newSpeaker)
        }
    }

    fun toggleCameraDirection() {
        _activeCall.value = _activeCall.value?.let { it.copy(isCameraFacingBack = !it.isCameraFacingBack) }
    }

    // --- Financial Wallet (GET /api/v1/wallet) ---
    private val _walletData = MutableStateFlow<WalletApiResponse?>(null)
    val walletData: StateFlow<WalletApiResponse?> = _walletData.asStateFlow()

    private val _isWalletLoading = MutableStateFlow(false)
    val isWalletLoading: StateFlow<Boolean> = _isWalletLoading.asStateFlow()

    fun refreshWallet() {
        viewModelScope.launch {
            _isWalletLoading.value = true
            try {
                val data = repository.fetchWallet()
                if (data != null) {
                    _walletData.value = data
                    data.transactions?.let { txs ->
                        _transactions.value = txs
                    }
                }
            } finally {
                _isWalletLoading.value = false
            }
        }
    }

    // --- Transactions ---
    private val _transactions = MutableStateFlow<List<TransactionDto>>(emptyList())
    val transactions: StateFlow<List<TransactionDto>> = _transactions.asStateFlow()

    private val _isTransactionsLoading = MutableStateFlow(false)
    val isTransactionsLoading = _isTransactionsLoading.asStateFlow()

    fun fetchTransactions(limit: Int = 50, offset: Int = 0) {
        viewModelScope.launch {
            _isTransactionsLoading.value = true
            repository.getTransactions(limit, offset).onSuccess {
                _transactions.value = it.transactions
            }.onFailure {
                _errorMessage.value = it.message ?: "Impossible de charger l'historique"
            }
            _isTransactionsLoading.value = false
        }
    }

    fun executeGlobalTransfer(
        receiverWalletId: String? = null,
        recipientAddress: String? = null,
        amount: Double,
        currency: String,
        pin: String,
        onSuccess: (TransferResponse) -> Unit = {}
    ) {
        viewModelScope.launch {
            _isWalletLoading.value = true
            repository.executeGlobalTransfer(
                receiverWalletId, recipientAddress, amount, currency, pin
            ).onSuccess {
                _successMessage.value = "Transfert réussi : ${it.resolvedReference}"
                refreshWallet()
                fetchTransactions()
                onSuccess(it)
            }.onFailure {
                _errorMessage.value = it.message ?: "Code PIN incorrect."
            }
            _isWalletLoading.value = false
        }
    }

    fun endCall() {
        val current = _activeCall.value
        voiceCallManager.stopVoiceSession()
        RingtoneHelper.stopCallRingtone(getApplication())
        OnChatNotificationHelper.cancelIncomingCallNotification(getApplication())

        if (current != null) {
            wsManager.sendCallSignaling(
                event = "call.ended",
                callId = current.roomId,
                conversationId = null,
                data = mapOf("peerWalletId" to current.peerWalletId)
            )
            if (current.durationSeconds > 0) {
                viewModelScope.launch {
                    repository.recordCall(
                        peerName = current.peerName,
                        isVideo = current.isVideo,
                        isIncoming = false,
                        isMissed = false,
                        durationSeconds = current.durationSeconds,
                        peerWalletId = current.peerWalletId.ifBlank { null },
                        peerAvatar = current.peerAvatar
                    )
                }
            }
        }
        _activeCall.value = null
    }

    // --- Ringtone Customization ---

    fun setRingtone(title: String, uri: String?) {
        _selectedRingtoneTitle.value = title
        _selectedRingtoneUri.value = uri
    }

    // --- Incoming Call & Notification Operations ---

    fun acceptIncomingCall() {
        val inc = _incomingCall.value ?: return
        _incomingCall.value = null
        RingtoneHelper.stopCallRingtone(getApplication())
        OnChatNotificationHelper.cancelIncomingCallNotification(getApplication())

        // Send call.accepted signaling back to caller via WebSocket
        wsManager.sendCallSignaling(
            event = "call.accepted",
            callId = inc.callId,
            conversationId = null,
            data = mapOf(
                "peerWalletId" to inc.callerWalletId,
                "callerWalletId" to inc.callerWalletId
            )
        )

        startCall(
            peerName = inc.callerName,
            peerWalletId = inc.callerWalletId,
            peerAvatar = inc.callerAvatar,
            isVideo = inc.isVideo,
            autoConnect = true
        )
    }

    fun declineIncomingCall() {
        val inc = _incomingCall.value
        _incomingCall.value = null
        RingtoneHelper.stopCallRingtone(getApplication())
        OnChatNotificationHelper.cancelIncomingCallNotification(getApplication())
        if (inc != null) {
            // Send call.rejected signaling back via WebSocket
            wsManager.sendCallSignaling(
                event = "call.rejected",
                callId = inc.callId,
                conversationId = null,
                data = mapOf("peerWalletId" to inc.callerWalletId)
            )
            viewModelScope.launch {
                repository.recordCall(
                    peerName = inc.callerName,
                    isVideo = inc.isVideo,
                    isIncoming = true,
                    isMissed = true,
                    durationSeconds = 0,
                    peerWalletId = inc.callerWalletId,
                    peerAvatar = inc.callerAvatar
                )
                OnChatNotificationHelper.showMissedCallNotification(
                    context = getApplication(),
                    callerName = inc.callerName,
                    callerWalletId = inc.callerWalletId.ifBlank { null },
                    isVideo = inc.isVideo
                )
            }
        }
    }

    // --- Live Stage & Conference Actions ---

    fun startLiveRoom(
        title: String,
        isPaid: Boolean,
        entryFee: Double,
        currency: String = "USD",
        isAudioOnly: Boolean = false
    ) {
        val session = currentSession.value
        val hostName = session?.name ?: "Moi"
        val hostWalletId = session?.walletId ?: ""
        val hostAvatar = session?.avatarUrl

        val newRoom = LiveStageRoom(
            id = "live_${System.currentTimeMillis()}",
            title = title,
            hostName = hostName,
            hostWalletId = hostWalletId,
            hostAvatar = hostAvatar,
            isPaid = isPaid,
            entryFee = entryFee,
            currency = currency,
            spectatorCount = 1,
            likesCount = 0,
            isHost = true,
            isJoined = true,
            isAudioOnly = isAudioOnly,
            raisedHandRequests = emptyList(),
            activeSpeakers = emptyList(),
            comments = emptyList()
        )

        _activeLiveRoom.value = newRoom
        _liveRoomsFeed.value = listOf(newRoom) + _liveRoomsFeed.value
        _successMessage.value = if (isAudioOnly) "Conférence audio démarrée !" else "Conférence vidéo démarrée !"
    }

    fun inviteContactToLiveRoom(roomId: String, contact: ContactCheckResult) {
        val current = _activeLiveRoom.value ?: return
        if (current.id != roomId) return

        val user = StageUser(
            walletId = contact.walletId ?: contact.phone,
            name = contact.name ?: "Contact (${contact.phone})",
            avatarUrl = contact.avatar,
            hasRequestedToSpeak = true,
            isSpeakerOnStage = current.isAudioOnly && current.activeSpeakers.size < 5
        )

        val updatedSpeakers = if (user.isSpeakerOnStage && !current.activeSpeakers.any { it.walletId == user.walletId }) {
            current.activeSpeakers + user
        } else {
            current.activeSpeakers
        }

        val updatedRequests = if (!user.isSpeakerOnStage && !current.raisedHandRequests.any { it.walletId == user.walletId }) {
            current.raisedHandRequests + user
        } else {
            current.raisedHandRequests
        }

        _activeLiveRoom.value = current.copy(
            activeSpeakers = updatedSpeakers,
            raisedHandRequests = updatedRequests,
            spectatorCount = current.spectatorCount + 1
        )

        // Send real-time conference invite over WebSocket
        wsManager.sendCallSignaling(
            event = "conference.invite",
            callId = roomId,
            conversationId = null,
            data = mapOf(
                "peerWalletId" to (contact.walletId ?: ""),
                "title" to current.title,
                "hostName" to current.hostName,
                "isAudioOnly" to current.isAudioOnly.toString()
            )
        )

        _successMessage.value = "Invitation envoyée à ${contact.name ?: contact.walletId}"
    }

    fun joinLiveRoom(room: LiveStageRoom) {
        _activeLiveRoom.value = room.copy(
            isHost = room.hostWalletId == currentSession.value?.walletId,
            isJoined = true,
            spectatorCount = room.spectatorCount + 1
        )
    }

    suspend fun payAndJoinLiveRoom(room: LiveStageRoom, pin: String): Boolean {
        // Execute debit to host wallet
        val session = currentSession.value
        val result = repository.executeDirectTransfer(
            recipientWalletId = room.hostWalletId,
            amount = room.entryFee,
            currency = room.currency,
            pin = pin
        )
        return if (result.isSuccess) {
            refreshWallet()
            _activeLiveRoom.value = room.copy(
                isHost = false,
                isJoined = true,
                spectatorCount = room.spectatorCount + 1
            )
            _successMessage.value = "Accès VIP validé (${room.entryFee} ${room.currency} débités)"
            true
        } else {
            false
        }
    }

    fun sendLiveComment(roomId: String, comment: String) {
        val session = currentSession.value
        val current = _activeLiveRoom.value ?: return
        if (current.id != roomId) return

        val newComment = StageComment(
            senderName = session?.name ?: "Moi",
            senderWalletId = session?.walletId ?: "",
            avatarUrl = session?.avatarUrl,
            comment = comment
        )

        _activeLiveRoom.value = current.copy(
            comments = current.comments + newComment
        )
    }

    fun sendLiveLike(roomId: String) {
        val current = _activeLiveRoom.value ?: return
        if (current.id != roomId) return
        _activeLiveRoom.value = current.copy(
            likesCount = current.likesCount + 1
        )
    }

    fun toggleRaiseHand(roomId: String) {
        val current = _activeLiveRoom.value ?: return
        if (current.id != roomId) return
        _activeLiveRoom.value = current.copy(
            hasRaisedHand = !current.hasRaisedHand
        )
    }

    fun inviteUserToStage(roomId: String, userWalletId: String, userName: String, userAvatar: String?) {
        val current = _activeLiveRoom.value ?: return
        if (current.id != roomId) return

        val user = StageUser(
            walletId = userWalletId,
            name = userName,
            avatarUrl = userAvatar,
            hasRequestedToSpeak = false,
            isSpeakerOnStage = true
        )

        val updatedSpeakers = if (current.activeSpeakers.any { it.walletId == userWalletId }) {
            current.activeSpeakers
        } else {
            (current.activeSpeakers + user).take(5)
        }

        _activeLiveRoom.value = current.copy(
            speakerOnStageName = userName,
            speakerOnStageWalletId = userWalletId,
            speakerOnStageAvatar = userAvatar,
            activeSpeakers = updatedSpeakers,
            raisedHandRequests = current.raisedHandRequests.filter { it.walletId != userWalletId }
        )
        _successMessage.value = "$userName est maintenant sur la scène !"
    }

    fun demoteSpeakerFromStage(roomId: String, userWalletId: String? = null) {
        val current = _activeLiveRoom.value ?: return
        if (current.id != roomId) return

        val targetWallet = userWalletId ?: current.speakerOnStageWalletId
        val prevSpeaker = current.speakerOnStageName ?: "L'intervenant"

        _activeLiveRoom.value = current.copy(
            speakerOnStageName = if (targetWallet == current.speakerOnStageWalletId) null else current.speakerOnStageName,
            speakerOnStageWalletId = if (targetWallet == current.speakerOnStageWalletId) null else current.speakerOnStageWalletId,
            speakerOnStageAvatar = if (targetWallet == current.speakerOnStageWalletId) null else current.speakerOnStageAvatar,
            activeSpeakers = current.activeSpeakers.filter { it.walletId != targetWallet }
        )
        _successMessage.value = "$prevSpeaker est retourné parmi les spectateurs"
    }

    fun toggleLiveOrientation(roomId: String) {
        val current = _activeLiveRoom.value ?: return
        if (current.id != roomId) return
        _activeLiveRoom.value = current.copy(
            isCameraLandscape = !current.isCameraLandscape
        )
    }

    fun toggleLiveCameraFlip(roomId: String) {
        val current = _activeLiveRoom.value ?: return
        if (current.id != roomId) return
        _activeLiveRoom.value = current.copy(
            isCameraFront = !current.isCameraFront
        )
    }

    fun leaveLiveRoom(roomId: String) {
        _activeLiveRoom.value = null
    }

    fun endLiveRoom(roomId: String) {
        _activeLiveRoom.value = null
        _liveRoomsFeed.value = _liveRoomsFeed.value.filter { it.id != roomId }
        _successMessage.value = "Conférence terminée"
    }

    private val _searchedProfile = MutableStateFlow<ContactCheckResult?>(null)
    val searchedProfile = _searchedProfile.asStateFlow()

    fun searchProfileByPhoneState(phone: String) {
        viewModelScope.launch {
            val result = repository.searchProfileByPhone(phone).getOrNull()
            _searchedProfile.value = result
        }
    }

    suspend fun searchProfileByPhone(phone: String): ContactCheckResult? {
        return repository.searchProfileByPhone(phone).getOrNull()
    }

    suspend fun getRecipientBitcoinAddress(phone: String): String? {
        return repository.getPublicBitcoinAddress(phone)
    }

    // --- Jojo Shopping, Events & MonPays Feeds ---
    private val _shoppingFeed = MutableStateFlow<List<ProductItem>>(emptyList())
    val shoppingFeed: StateFlow<List<ProductItem>> = _shoppingFeed.asStateFlow()

    private val _eventsFeed = MutableStateFlow<List<EventItem>>(emptyList())
    val eventsFeed: StateFlow<List<EventItem>> = _eventsFeed.asStateFlow()

    private val _monPaysPages = MutableStateFlow<List<MonPaysCompany>>(emptyList())
    val monPaysPages: StateFlow<List<MonPaysCompany>> = _monPaysPages.asStateFlow()

    private val _monPaysFeed = MutableStateFlow<List<MonPaysActivity>>(emptyList())
    val monPaysFeed: StateFlow<List<MonPaysActivity>> = _monPaysFeed.asStateFlow()

    init {
        fetchFeeds()
        fetchRecentMeetings()
        fetchCallsHistoryApi()
    }

    fun fetchFeeds() {
        viewModelScope.launch {
            val products = repository.getShoppingFeed(20)
            _shoppingFeed.value = products

            val events = repository.getEventsFeed(10)
            val now = System.currentTimeMillis()
            val validEvents = events.filter { event ->
                try {
                    val dateStr = event.date
                    if (dateStr.isBlank()) true
                    else {
                        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                        val parsedDate = sdf.parse(dateStr)
                        if (parsedDate != null) {
                            parsedDate.time + 86400000L >= now
                        } else true
                    }
                } catch (_: Exception) {
                    true
                }
            }
            _eventsFeed.value = validEvents

            fetchMonPaysData()
        }
    }

    fun fetchMonPaysData() {
        viewModelScope.launch {
            val pages = repository.getMonPaysPages()
            _monPaysPages.value = pages

            val feed = repository.getMonPaysFeed()
            _monPaysFeed.value = feed
        }
    }

    fun toggleFollowMonPaysPage(companyId: Long) {
        // Optimistic UI update
        _monPaysPages.value = _monPaysPages.value.map { company ->
            if (company.id == companyId) {
                val currentlyFollowing = company.isFollowing == true
                val newFollowing = !currentlyFollowing
                val newCount = if (newFollowing) (company.followerCount ?: 0) + 1 else maxOf(0, (company.followerCount ?: 1) - 1)
                company.copy(isFollowing = newFollowing, followerCount = newCount)
            } else company
        }

        viewModelScope.launch {
            val res = repository.followMonPaysPage(companyId)
            if (res != null && res.success) {
                _monPaysPages.value = _monPaysPages.value.map { company ->
                    if (company.id == companyId) {
                        company.copy(isFollowing = res.following, followerCount = res.followerCount)
                    } else company
                }
            }
        }
    }

    fun getMonPaysPageDetail(companyId: Long, onResult: (MonPaysDetailResponse?) -> Unit) {
        viewModelScope.launch {
            val detail = repository.getMonPaysPageDetail(companyId)
            onResult(detail)
        }
    }

    fun postMonPaysComment(postId: Long, content: String, onComplete: () -> Unit) {
        viewModelScope.launch {
            repository.postMonPaysComment(postId = postId, content = content)
            onComplete()
        }
    }

    fun toggleFollowShop(sellerId: Int) {
        // Optimistic UI update immediately
        _shoppingFeed.value = _shoppingFeed.value.map { product ->
            val matches = product.sellerId == sellerId.toString() ||
                    product.sellerId.toIntOrNull() == sellerId
            if (matches) {
                val currentlyFollowing = product.isFollowing == true
                val newFollowing = !currentlyFollowing
                val newCount = if (newFollowing) (product.followerCount ?: 0) + 1 else maxOf(0, (product.followerCount ?: 1) - 1)
                product.copy(isFollowing = newFollowing, followerCount = newCount)
            } else {
                product
            }
        }

        viewModelScope.launch {
            val res = repository.followShop(sellerId)
            if (res != null && res.success) {
                _shoppingFeed.value = _shoppingFeed.value.map { product ->
                    val matches = product.sellerId == sellerId.toString() ||
                            product.sellerId.toIntOrNull() == sellerId
                    if (matches) {
                        product.copy(isFollowing = res.isFollowing, followerCount = res.followerCount)
                    } else {
                        product
                    }
                }
            }
        }
    }

    // =========================================================================
    // FINUSECO SARL / CashPay All / OnChat Meetings API (Cahier des charges)
    // =========================================================================

    fun fetchRecentMeetings(limit: Int = 20) {
        viewModelScope.launch {
            _isMeetingsLoading.value = true
            meetingsRepository.getMeetingsList(limit).onSuccess { list ->
                _recentMeetings.value = list
            }.onFailure { err ->
                _lastMeetingApiError.value = err.message
            }
            _isMeetingsLoading.value = false
        }
    }

    fun createMeeting(
        topic: String,
        type: String = "video",
        scheduledAt: String? = null,
        durationMin: Int = 60,
        priceAmount: Double = 0.0,
        priceCurrency: String = "USD",
        autoJoin: Boolean = true,
        onSuccess: ((CreateMeetingResponse) -> Unit)? = null
    ) {
        viewModelScope.launch {
            _isMeetingsLoading.value = true
            meetingsRepository.createMeeting(
                topic = topic,
                type = type,
                scheduledAt = scheduledAt,
                durationMin = durationMin,
                priceAmount = priceAmount,
                priceCurrency = priceCurrency
            ).onSuccess { response ->
                _successMessage.value = "Réunion créée avec succès ! Code: ${response.meetCode}"
                fetchRecentMeetings()
                if (autoJoin && !response.meetCode.isNullOrBlank()) {
                    joinMeeting(response.meetCode)
                }
                onSuccess?.invoke(response)
            }.onFailure { err ->
                _errorMessage.value = err.message ?: "Impossible de créer la réunion"
                _lastMeetingApiError.value = err.message
            }
            _isMeetingsLoading.value = false
        }
    }

    private val _meetingWebUrl = MutableStateFlow<String?>(null)
    val meetingWebUrl: StateFlow<String?> = _meetingWebUrl.asStateFlow()

    fun joinMeeting(meetCode: String) {
        val url = com.example.data.api.MeetingLinks.canonical(meetCode)
        if (url == null) { _errorMessage.value = "Lien ou code OnChat invalide"; return }
        _meetingWebUrl.value = url
    }

    fun closeMeetingSurface() {
        val url = _meetingWebUrl.value
        _meetingWebUrl.value = null
        if (url != null) viewModelScope.launch {
            val code = java.net.URI(url).path.substringAfterLast('/')
            meetingsRepository.leaveMeeting(code)
        }
    }

    fun createGroupMeeting(groupId: Long, type: String) {
        viewModelScope.launch {
            try {
                val response = ApiClient.apiService.createGroupMeeting(groupId, mapOf("type" to type))
                val body = response.body()
                if (!response.isSuccessful || body?.success != true || body.meetCode.isNullOrBlank()) error(body?.error ?: "Création de réunion impossible")
                joinMeeting(body.meetCode)
            } catch (e: kotlinx.coroutines.CancellationException) { throw e }
            catch (e: Exception) { _errorMessage.value = e.message }
        }
    }

    fun leaveMeeting() {
        val current = _activeMeeting.value ?: return
        viewModelScope.launch {
            meetingsRepository.leaveMeeting(current.meetCode)
            _activeMeeting.value = null
            _successMessage.value = "Vous avez quitté la réunion"
            fetchRecentMeetings()
        }
    }

    fun endMeeting() {
        val current = _activeMeeting.value ?: return
        if (!current.isHost) {
            _errorMessage.value = "Seul l'hôte peut terminer la réunion (403)"
            return
        }
        viewModelScope.launch {
            meetingsRepository.endMeeting(current.meetCode).onSuccess {
                _activeMeeting.value = null
                _successMessage.value = "La réunion a été terminée avec succès"
                fetchRecentMeetings()
            }.onFailure { err ->
                _errorMessage.value = err.message ?: "Erreur lors de la fermeture de la réunion"
            }
        }
    }

    fun toggleMeetingRaiseHand() {
        val current = _activeMeeting.value ?: return
        val newRaised = !current.isHandRaised
        viewModelScope.launch {
            meetingsRepository.raiseHand(current.meetCode, newRaised).onSuccess {
                _activeMeeting.value = current.copy(isHandRaised = newRaised)
                refreshMeetingParticipants()
                _successMessage.value = if (newRaised) "Main levée pour demander la parole" else "Main baissée"
            }
        }
    }

    fun grantOrRevokeSpeech(targetUserId: Long, canSpeak: Boolean) {
        val current = _activeMeeting.value ?: return
        if (!current.isHost) {
            _errorMessage.value = "Seul l'hôte peut accorder la parole (403)"
            return
        }
        viewModelScope.launch {
            meetingsRepository.grantSpeech(current.meetCode, targetUserId, canSpeak).onSuccess { res ->
                refreshMeetingParticipants()
                _successMessage.value = if (canSpeak) "Droit de parole accordé (Rôle: ${res.role ?: "speaker"})" else "Droit de parole retiré"
            }.onFailure { err ->
                _errorMessage.value = err.message ?: "Erreur lors de la modification du droit de parole"
            }
        }
    }

    fun refreshMeetingParticipants() {
        val current = _activeMeeting.value ?: return
        viewModelScope.launch {
            meetingsRepository.getMeetingParticipants(current.meetCode).onSuccess { res ->
                val myUserId = ApiClient.currentUserId ?: 101L
                val me = res.participants.firstOrNull { it.userId == myUserId }
                _activeMeeting.value = current.copy(
                    participants = res.participants,
                    canSpeak = if (current.isHost) true else (me?.canSpeak ?: current.canSpeak),
                    isHandRaised = me?.raiseHand ?: current.isHandRaised
                )
            }
        }
    }

    fun fetchMeetingBilling(meetCode: String) {
        viewModelScope.launch {
            meetingsRepository.getMeetingBilling(meetCode).onSuccess { res ->
                _activeMeetingBilling.value = res.billing
                _activeMeeting.value = _activeMeeting.value?.copy(billing = res.billing)
            }
        }
    }

    fun updateMeetingBilling(priceAmount: Double, priceCurrency: String) {
        val current = _activeMeeting.value ?: return
        if (!current.isHost) {
            _errorMessage.value = "Seul l'hôte peut modifier la facturation (403)"
            return
        }
        viewModelScope.launch {
            meetingsRepository.updateMeetingBilling(current.meetCode, priceAmount, priceCurrency).onSuccess { res ->
                val updated = MeetingBillingDto(
                    priceAmount = res.priceAmount ?: priceAmount,
                    priceCurrency = res.priceCurrency ?: priceCurrency,
                    isPaid = priceAmount > 0,
                    hostUserId = ApiClient.currentUserId ?: 101L
                )
                _activeMeetingBilling.value = updated
                _activeMeeting.value = current.copy(billing = updated)
                _successMessage.value = "Facturation mise à jour : $priceAmount $priceCurrency"
            }.onFailure { err ->
                _errorMessage.value = err.message ?: "Erreur mise à jour facturation"
            }
        }
    }

    fun toggleMeetingMic() {
        val current = _activeMeeting.value ?: return
        if (!current.canSpeak && current.isMicMuted) {
            _errorMessage.value = "Droit de parole non accordé. Levez la main pour demander la parole."
            return
        }
        val newMute = !current.isMicMuted
        voiceCallManager.setMute(newMute)
        _activeMeeting.value = current.copy(isMicMuted = newMute)
    }

    fun toggleMeetingCamera() {
        val current = _activeMeeting.value ?: return
        _activeMeeting.value = current.copy(isCameraOn = !current.isCameraOn)
    }

    fun toggleMeetingCameraFront() {
        val current = _activeMeeting.value ?: return
        _activeMeeting.value = current.copy(isCameraFront = !current.isCameraFront)
    }

    // =========================================================================
    // Calls API (Cahier des charges section 4)
    // =========================================================================

    fun initiateCallApi(receiverWalletId: String, isVideo: Boolean) {
        val myWallet = currentSession.value?.walletId.orEmpty().ifBlank { "CD0100000001" }
        viewModelScope.launch {
            meetingsRepository.initiateCall(
                callerWalletId = myWallet,
                receiverWalletId = receiverWalletId,
                isVideo = isVideo
            ).onSuccess { callDto ->
                fetchCallsHistoryApi()
                startCall(
                    peerName = callDto.receiverName ?: "Contact ($receiverWalletId)",
                    peerWalletId = receiverWalletId,
                    isVideo = isVideo,
                    autoConnect = false
                )
            }.onFailure { err ->
                _errorMessage.value = err.message ?: "Impossible d'initier l'appel"
            }
        }
    }

    fun respondCallApi(callId: String, status: String) {
        val myWallet = currentSession.value?.walletId.orEmpty().ifBlank { "CD0100000001" }
        viewModelScope.launch {
            meetingsRepository.respondCall(callId, myWallet, status).onSuccess {
                fetchCallsHistoryApi()
            }
        }
    }

    fun endCallApi(callId: String, durationSec: Int = 0) {
        val myWallet = currentSession.value?.walletId.orEmpty().ifBlank { "CD0100000001" }
        viewModelScope.launch {
            meetingsRepository.endCall(callId, myWallet, durationSec).onSuccess {
                fetchCallsHistoryApi()
            }
        }
    }

    fun fetchCallsHistoryApi() {
        val myWallet = currentSession.value?.walletId.orEmpty().ifBlank { "CD0100000001" }
        viewModelScope.launch {
            meetingsRepository.getCallsHistory(myWallet).onSuccess { list ->
                _apiCallsHistory.value = list
            }
        }
    }
}

