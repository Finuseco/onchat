package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.RemoteInput
import com.example.data.api.ApiClient
import com.example.data.api.SessionTokenStore
import com.example.data.db.AppDatabase
import com.example.data.repository.OnChatRepository
import com.example.util.OnChatNotificationHelper
import kotlinx.coroutines.*

/** Explicit non-exported receiver: a reply stays in the account that owns the notification. */
class NotificationReplyReceiver : BroadcastReceiver() {
 override fun onReceive(context: Context, intent: Intent) {
  if (intent.action != OnChatNotificationHelper.ACTION_REPLY_MESSAGE) return
  val conversation = intent.getStringExtra(OnChatNotificationHelper.EXTRA_CONVERSATION_ID)?.takeIf { it.isNotBlank() } ?: return
  val owner = intent.getLongExtra("ownerUserId", -1L)
  val text = RemoteInput.getResultsFromIntent(intent)?.getCharSequence(OnChatNotificationHelper.KEY_TEXT_REPLY)?.toString()?.trim()?.takeIf { it.isNotBlank() && it.length <= 5000 } ?: return
  val pending = goAsync()
  CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
   try {
    withTimeout(8000) {
     val session = AppDatabase.getDatabase(context.applicationContext).sessionDao().getSession() ?: error("Session expirée")
     if (owner <= 0 || session.userId != owner) error("La session a changé")
     SessionTokenStore.init(context.applicationContext)
     ApiClient.authToken = session.token
     ApiClient.activeWalletId = session.walletId
     OnChatRepository(context.applicationContext).sendMessage(conversation, text)
     OnChatNotificationHelper.cancelMessageNotification(context, conversation)
    }
   } catch (_: Exception) {
    OnChatNotificationHelper.showReplyFailure(context, conversation)
   } finally { pending.finish() }
  }
 }
}
