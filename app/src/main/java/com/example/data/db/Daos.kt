package com.example.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
    @Query("SELECT * FROM user_session WHERE id = 1 LIMIT 1")
    fun getSessionFlow(): Flow<UserSessionEntity?>

    @Query("SELECT * FROM user_session WHERE id = 1 LIMIT 1")
    suspend fun getSession(): UserSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSession(session: UserSessionEntity)

    @Query("DELETE FROM user_session")
    suspend fun clearSession()

    @Query("UPDATE user_session SET biometricEnabled = :enabled WHERE id = 1")
    suspend fun setBiometricEnabled(enabled: Boolean)

    @Query("UPDATE user_session SET customChatBackgroundUri = :uri WHERE id = 1")
    suspend fun setChatBackgroundUri(uri: String?)
}

@Dao
interface ConversationDao {
    @Query("SELECT * FROM conversations ORDER BY lastMessageTime DESC")
    fun getConversationsFlow(): Flow<List<ConversationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversations(conversations: List<ConversationEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversation(conversation: ConversationEntity)

    @Query("SELECT * FROM conversations WHERE id = :id LIMIT 1")
    suspend fun getConversationSync(id: String): ConversationEntity?

    @Query("SELECT * FROM conversations WHERE contactWalletId = :walletId LIMIT 1")
    suspend fun getConversationByWalletId(walletId: String): ConversationEntity?

    @Query("DELETE FROM conversations WHERE id = :id")
    suspend fun deleteConversation(id: String)


    @Query("DELETE FROM conversations")
    suspend fun clearAllConversations()
}

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY timestamp ASC")
    fun getMessagesFlow(conversationId: String): Flow<List<MessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<MessageEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    @Query("UPDATE messages SET content = :content, isEdited = 1 WHERE id = :messageId")
    suspend fun updateMessageContent(messageId: String, content: String)

    @Query("UPDATE messages SET status = 'read' WHERE conversationId = :conversationId AND isMine = 1 AND status NOT IN ('failed', 'sending', 'receipt_pending')")
    suspend fun markOutgoingMessagesAsRead(conversationId: String)

    @Query("UPDATE messages SET status = 'READ' WHERE id = :messageId")
    suspend fun markMessageRead(messageId: String)

    @Query("UPDATE messages SET status = 'READ' WHERE conversationId = :conversationId AND isMine = 0")
    suspend fun markConversationMessagesAsRead(conversationId: String)

    @Query("DELETE FROM messages WHERE id = :messageId")
    suspend fun deleteMessage(messageId: String)

    @Query("DELETE FROM messages WHERE conversationId = :conversationId")
    suspend fun deleteMessagesForConversation(conversationId: String)


    @Query("DELETE FROM messages")
    suspend fun clearAllMessages()
}

@Dao
interface GroupDao {
    @Query("SELECT * FROM groups ORDER BY id DESC")
    fun getGroupsFlow(): Flow<List<GroupEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroups(groups: List<GroupEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroup(group: GroupEntity)

    @Query("DELETE FROM groups WHERE id = :groupId")
    suspend fun deleteGroup(groupId: Long)

}

@Dao
interface StatusDao {
    @Query("DELETE FROM statuses")
    suspend fun clearSyncedStatuses()
    @androidx.room.Transaction
    suspend fun replaceSyncedStatuses(statuses: List<StatusEntity>) {
        clearSyncedStatuses()
        if (statuses.isNotEmpty()) insertStatuses(statuses)
    }

    @Query("SELECT * FROM statuses WHERE expiresAt > :now ORDER BY timestamp DESC")
    fun getStatusesFlow(now: Long = System.currentTimeMillis()): Flow<List<StatusEntity>>

    @Query("DELETE FROM statuses WHERE expiresAt <= :now")
    suspend fun deleteExpiredStatuses(now: Long = System.currentTimeMillis())

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStatuses(statuses: List<StatusEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStatus(status: StatusEntity)

}

@Dao
interface CallDao {
    @Query("SELECT * FROM calls ORDER BY timestamp DESC")
    fun getCallsFlow(): Flow<List<CallEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCall(call: CallEntity)


    @Query("DELETE FROM calls")
    suspend fun clearCalls()
}
