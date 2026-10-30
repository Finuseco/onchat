package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.WhatsAppTopBar
import com.example.ui.components.OnChatTab
import com.example.ui.components.OnChatSplashScreen
import com.example.ui.screens.auth.LoginScreen
import com.example.ui.screens.profile.ProfileScreen
import com.example.ui.screens.profile.UserProfileScreen
import com.example.ui.screens.reels.LiveReelsScreen
import com.example.ui.screens.reels.VideoPublishScreen
import com.example.ui.screens.chats.ChatListScreen
import com.example.ui.screens.chats.ChatDetailScreen
import com.example.ui.screens.chats.JojoAiChatScreen
import com.example.ui.screens.groups.GroupListScreen
import com.example.ui.screens.status.StatusScreen
import com.example.ui.screens.calls.CallsScreen
import com.example.ui.screens.calls.ActiveCallScreen
import com.example.ui.screens.calls.IncomingCallOverlay
import com.example.ui.screens.calls.MeetingRoomScreen
import com.example.ui.screens.contacts.ContactsCheckScreen
import com.example.ui.screens.wallet.TransactionListScreen
import com.example.ui.screens.settings.SettingsScreen
import com.example.ui.theme.*
import com.example.ui.viewmodel.OnChatViewModel
import com.example.ui.viewmodel.LoginStep

class MainActivity : ComponentActivity() {

    private var pendingNotificationConversation by mutableStateOf<String?>(null)

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingNotificationConversation = intent.getStringExtra(com.example.util.OnChatNotificationHelper.EXTRA_CONVERSATION_ID)
    }

    private val viewModel: OnChatViewModel by viewModels()

    override fun onStart() { super.onStart(); com.example.util.OnChatNotificationHelper.appInForeground = true }
    override fun onStop() { com.example.util.OnChatNotificationHelper.appInForeground = false; super.onStop() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        pendingNotificationConversation = intent.getStringExtra(com.example.util.OnChatNotificationHelper.EXTRA_CONVERSATION_ID)

        setContent {
            OnChatTheme {
                val loginStep by viewModel.loginStep.collectAsState()
                val isSessionChecking by viewModel.isSessionChecking.collectAsState()
                val activeCall by viewModel.activeCall.collectAsState()
                val activeMeeting by viewModel.activeMeeting.collectAsState()
                val meetingUrl by viewModel.meetingWebUrl.collectAsState()
                val incomingCall by viewModel.incomingCall.collectAsState()
                val activeConversationId by viewModel.activeConversationId.collectAsState()

                var currentScreen by remember { mutableStateOf("main") }
                var currentTab by remember { mutableStateOf(OnChatTab.CHATS) }
                var profilePhoneTarget by remember { mutableStateOf("") }
                var searchQuery by remember { mutableStateOf("") }

                val notificationPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                    contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
                ) { }

                LaunchedEffect(Unit) {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                        notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                    }
                }

                LaunchedEffect(loginStep, isSessionChecking) {
                    if (!isSessionChecking && loginStep == LoginStep.AUTHENTICATED) {
                        try { com.example.service.OnChatBackgroundService.start(this@MainActivity) }
                        catch (error: Exception) { android.util.Log.w("OnChat", "Service de notifications indisponible", error) }
                    } else if (!isSessionChecking) {
                        stopService(android.content.Intent(this@MainActivity, com.example.service.OnChatBackgroundService::class.java))
                        com.example.util.OnChatNotificationHelper.signedInUserId = null
                    }
                }

                LaunchedEffect(loginStep, pendingNotificationConversation) {
                    if (loginStep == LoginStep.AUTHENTICATED) pendingNotificationConversation?.let {
                        viewModel.openConversation(it)
                        pendingNotificationConversation = null
                    }
                }

                // Automatically switch to chat_detail screen when activeConversationId is populated
                LaunchedEffect(activeConversationId) {
                    if (activeConversationId != null) {
                        currentScreen = "chat_detail"
                    } else if (currentScreen == "chat_detail") {
                        currentScreen = "main"
                    }
                }

                // Handle back gestures smoothly
                BackHandler(enabled = currentScreen != "main") {
                    when (currentScreen) {
                        "chat_detail" -> {
                            viewModel.closeConversation()
                            currentScreen = "main"
                        }
                        "reels" -> currentScreen = "main"
                        "publish" -> currentScreen = "reels"
                        "profile" -> currentScreen = "main"
                        "user_profile" -> currentScreen = "main"
                        "public_profile" -> currentScreen = "main"
                        "settings" -> currentScreen = "main"
                        "transactions" -> currentScreen = "settings"
                        "jojo_ai" -> currentScreen = "main"
                        else -> currentScreen = "main"
                    }
                }

                when {
                    isSessionChecking -> {
                        OnChatSplashScreen(onAnimationFinished = {
                            viewModel.finishSplash()
                        })
                    }
                    loginStep != LoginStep.AUTHENTICATED -> {
                        LoginScreen(
                            viewModel = viewModel
                        )
                    }
                    incomingCall != null -> {
                        IncomingCallOverlay(
                            callState = incomingCall!!,
                            onAccept = { viewModel.acceptIncomingCall() },
                            onDecline = { viewModel.declineIncomingCall() }
                        )
                    }
                    activeCall != null -> {
                        ActiveCallScreen(
                            callState = activeCall!!,
                            viewModel = viewModel
                        )
                    }
                    meetingUrl != null -> {
                        com.example.ui.screens.meetings.MeetingWebScreen(url = meetingUrl!!, onClose = { viewModel.closeMeetingSurface() })
                    }
                    activeMeeting != null -> {
                        MeetingRoomScreen(
                            session = activeMeeting!!,
                            viewModel = viewModel,
                            onClose = { viewModel.leaveMeeting() }
                        )
                    }
                    else -> {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = CashPayNavyDark
                        ) {
                            when (currentScreen) {
                                "main" -> {
                                    Column(modifier = Modifier.fillMaxSize()) {
                                        // Official WhatsApp top-bar matching Screenshot 1 & 2
                                        WhatsAppTopBar(
                                            searchQuery = searchQuery,
                                            onSearchQueryChange = { searchQuery = it },
                                            onOpenSettings = { currentScreen = "settings" },
                                            onOpenNewGroup = {
                                                currentTab = OnChatTab.GROUPS
                                                // GroupListScreen will handle the create group dialog
                                            },
                                            onOpenNewStatus = {
                                                currentTab = OnChatTab.STATUS
                                                // StatusScreen will handle the create status dialog
                                            },
                                            onOpenTransactions = {
                                                currentScreen = "transactions"
                                            },
                                            onOpenMyProfile = {
                                                profilePhoneTarget = viewModel.userPhone.value
                                                currentScreen = "profile"
                                            },
                                            onOpenLiveReels = {
                                                currentScreen = "reels"
                                            },
                                            onOpenAiChat = {
                                                currentScreen = "jojo_ai"
                                            }
                                        )

                                        // Render active tab content fully channelling original OnChat modules
                                        Box(modifier = Modifier.weight(1f)) {
                                            when (currentTab) {
                                                OnChatTab.CHATS -> {
                                                    ChatListScreen(
                                                        viewModel = viewModel,
                                                        searchQuery = searchQuery,
                                                        onStartNewChat = {
                                                            currentTab = OnChatTab.CONTACTS
                                                        },
                                                        onOpenProfile = { phone ->
                                                            profilePhoneTarget = phone
                                                            currentScreen = "profile"
                                                        },
                                                        onOpenLiveReels = {
                                                            currentScreen = "reels"
                                                        }
                                                    )
                                                }
                                                OnChatTab.GROUPS -> {
                                                    GroupListScreen(
                                                        viewModel = viewModel,
                                                        searchQuery = searchQuery,
                                                        onOpenGroupChat = { groupId, _ ->
                                                            viewModel.openGroupChat(groupId, "")
                                                            currentScreen = "chat_detail"
                                                        }
                                                    )
                                                }
                                                OnChatTab.STATUS -> {
                                                    StatusScreen(
                                                        viewModel = viewModel,
                                                        onOpenCreateStatus = {}
                                                    )
                                                }
                                                OnChatTab.CALLS -> {
                                                    CallsScreen(
                                                        viewModel = viewModel
                                                    )
                                                }
                                                OnChatTab.CONTACTS -> {
                                                    ContactsCheckScreen(
                                                        viewModel = viewModel,
                                                        onStartChatWithContact = { name, avatarUrl, phone, walletId, btcAddress ->
                                                            viewModel.openContactChat(
                                                                name = name,
                                                                avatarUrl = avatarUrl,
                                                                phone = phone,
                                                                walletId = walletId,
                                                                recipientUserId = null,
                                                                bitcoinAddress = btcAddress
                                                            )
                                                            currentScreen = "chat_detail"
                                                        },
                                                        onOpenPublicProfile = { phone ->
                                                            profilePhoneTarget = phone
                                                            currentScreen = "profile"
                                                        }
                                                    )
                                                }
                                            }
                                        }

                                        // Footer Navigation Bar (Menu de navigation en bas avec icônes)
                                        NavigationBar(
                                            containerColor = CashPayNavy,
                                            contentColor = Color.White,
                                            tonalElevation = 8.dp,
                                            modifier = Modifier.navigationBarsPadding()
                                        ) {
                                            OnChatTab.values().forEach { tab ->
                                                val selected = currentTab == tab
                                                NavigationBarItem(
                                                    selected = selected,
                                                    onClick = { currentTab = tab },
                                                    icon = {
                                                        Icon(
                                                            imageVector = tab.icon,
                                                            contentDescription = tab.title,
                                                            modifier = Modifier.size(24.dp)
                                                        )
                                                    },
                                                    label = {
                                                        Text(
                                                            text = tab.title,
                                                            fontSize = 11.sp,
                                                            fontWeight = if (selected) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Medium
                                                        )
                                                    },
                                                    colors = NavigationBarItemDefaults.colors(
                                                        selectedIconColor = OnChatCyan,
                                                        selectedTextColor = OnChatCyan,
                                                        indicatorColor = OnChatCyan.copy(alpha = 0.15f),
                                                        unselectedIconColor = Color.White.copy(alpha = 0.55f),
                                                        unselectedTextColor = Color.White.copy(alpha = 0.55f)
                                                    )
                                                )
                                            }
                                        }
                                    }
                                }
                                "chat_detail" -> {
                                    ChatDetailScreen(
                                        conversationId = activeConversationId ?: "",
                                        viewModel = viewModel,
                                        onBack = {
                                            viewModel.closeConversation()
                                            currentScreen = "main"
                                        },
                                        onOpenProfile = { phone ->
                                            profilePhoneTarget = phone
                                            currentScreen = "profile"
                                        }
                                    )
                                }
                                "reels" -> {
                                    LiveReelsScreen(
                                        viewModel = viewModel,
                                        onBack = { currentScreen = "main" },
                                        onOpenOwnProfile = {
                                            profilePhoneTarget = viewModel.userPhone.value
                                            currentScreen = "profile"
                                        },
                                        onOpenCreatorProfile = { phone ->
                                            profilePhoneTarget = phone
                                            currentScreen = "profile"
                                        }
                                    )
                                }
                                "publish" -> {
                                    VideoPublishScreen(
                                        viewModel = viewModel,
                                        onBack = { currentScreen = "reels" },
                                        onSuccess = { currentScreen = "reels" }
                                    )
                                }
                                "profile" -> {
                                    ProfileScreen(
                                        phone = profilePhoneTarget,
                                        viewModel = viewModel,
                                        onBack = { currentScreen = "main" }
                                    )
                                }
                                "settings" -> {
                                    SettingsScreen(
                                        viewModel = viewModel,
                                        onBack = { currentScreen = "main" },
                                        onOpenProfile = {
                                            profilePhoneTarget = viewModel.userPhone.value
                                            currentScreen = "profile"
                                        },
                                        onOpenTransactions = {
                                            currentScreen = "transactions"
                                        }
                                    )
                                }
                                "transactions" -> {
                                    TransactionListScreen(
                                        viewModel = viewModel,
                                        onBack = { currentScreen = "settings" },
                                        onOpenTransaction = { _ -> }
                                    )
                                }
                                "jojo_ai" -> {
                                    JojoAiChatScreen(
                                        onBack = { currentScreen = "main" }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
