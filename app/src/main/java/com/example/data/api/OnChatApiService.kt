package com.example.data.api

import com.example.data.model.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

interface OnChatApiService {

    // 0. Countries List (Active countries with code, name, dialCode)
    @GET("api/v1/countries")
    suspend fun getCountries(): Response<CountriesResponse>

    // 1. Auth Login (Wallet ID or Phone)
    @POST("api/v1/auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<AuthResponse>

    // 2. Auth OTP Send (Explicit Channel)
    @POST("api/v1/auth/otp/send")
    suspend fun sendOtp(
        @Body request: OtpSendRequest
    ): Response<OtpSendResponse>

    // 3. Auth OTP Verify
    @POST("api/v1/auth/otp/verify")
    suspend fun verifyOtp(
        @Body request: OtpVerifyRequest
    ): Response<AuthResponse>

    // 4. Auth PIN Verify
    @POST("api/v1/auth/pin/verify")
    suspend fun verifyPin(
        @Body request: PinVerifyRequest
    ): Response<AuthResponse>

    // 4.1 Forgot PIN Engine (Public - No Token Required)
    @POST("api/v1/auth/forgot-pin/request")
    suspend fun requestForgotPin(
        @Body request: ForgotPinRequest
    ): Response<ForgotPinRequestResponse>

    @POST("api/v1/auth/forgot-pin/verify")
    suspend fun verifyForgotPinOtp(
        @Body request: ForgotPinVerifyRequest
    ): Response<ForgotPinVerifyResponse>

    @POST("api/v1/auth/forgot-pin/reset")
    suspend fun resetForgotPin(
        @Body request: ForgotPinResetRequest
    ): Response<ForgotPinResetResponse>

    // 5. Check Current Session (User profile)
    @GET("api/v1/profile/me")
    suspend fun getProfileMe(): Response<MeResponse>

    @GET("api/v1/auth/me")
    suspend fun getAuthMe(
        @Query("phone") phone: String? = null
    ): Response<MeResponse>

    @PATCH("api/v1/auth/me")
    suspend fun updateAuthMe(
        @Body request: UpdateProfileWithPinRequest
    ): Response<MeResponse>

    @PUT("api/v1/profile/me")
    suspend fun updateProfile(
        @Body request: UpdateProfileRequest
    ): Response<MeResponse>

    // 5. Public Profile lookup by Phone
    @GET("api/v1/profile/by-phone")
    suspend fun getProfileByPhone(
        @Query("phone") phone: String
    ): Response<PhoneProfileResponse>

    @POST("api/v1/social/action")
    suspend fun performSocialAction(
        @Body request: SocialActionRequest
    ): Response<SocialActionResponse>

    // 5.1 Private Profile Media (Albums, Photos, Avatars, Covers)
    @GET("api/v1/profile/media")
    suspend fun getProfileMedia(): Response<ProfileMediaResponse>

    @Multipart
    @POST("api/v1/profile/media")
    suspend fun uploadProfileMedia(
        @Part file: MultipartBody.Part,
        @Part("mediaType") mediaType: RequestBody,
        @Part("visibility") visibility: RequestBody
    ): Response<okhttp3.ResponseBody>

    @POST("api/v1/profile/media/set-profile")
    suspend fun setProfileMedia(
        @Body request: SetMediaTargetRequest
    ): Response<SetProfileMediaResponse>

    @POST("api/v1/profile/media/set-cover")
    suspend fun setCoverMedia(
        @Body request: SetMediaTargetRequest
    ): Response<SetCoverMediaResponse>

    // 2.5 Upload Media (Multipart: image, audio, video)
    @Multipart
    @POST("api/v1/media/upload")
    suspend fun uploadMedia(
        @Part file: MultipartBody.Part
    ): Response<MediaUploadResponse>

    // 3. Calls Engine (https://app.cashpay-all.com)
    @POST("api/v1/calls/initiate")
    suspend fun initiateCall(
        @Body request: InitiateCallRequest
    ): Response<CallActionResponse>

    @POST("api/v1/calls/{callId}/respond")
    suspend fun respondCall(
        @Path("callId") callId: String,
        @Body request: RespondCallRequest
    ): Response<CallActionResponse>

    @POST("api/v1/calls/{callId}/end")
    suspend fun endCall(
        @Path("callId") callId: String,
        @Body request: EndCallRequest
    ): Response<CallActionResponse>

    @GET("api/v1/calls/history")
    suspend fun getCallsHistory(
        @Query("walletId") walletId: String? = null
    ): Response<CallsHistoryResponse>

    // 4. Live Stage Engine
    @POST("api/v1/live/rooms/create")
    suspend fun createLiveRoom(
        @Body request: CreateLiveRoomRequest
    ): Response<CreateLiveRoomResponse>

    @POST("api/v1/live/rooms/{roomId}/join")
    suspend fun joinLiveRoom(
        @Path("roomId") roomId: String,
        @Body request: LiveRoomUserActionRequest
    ): Response<LiveRoomActionResponse>

    @POST("api/v1/live/rooms/{roomId}/invite-speaker")
    suspend fun inviteLiveSpeaker(
        @Path("roomId") roomId: String,
        @Body request: LiveSpeakerActionRequest
    ): Response<LiveRoomActionResponse>

    @POST("api/v1/live/rooms/{roomId}/promote-speaker")
    suspend fun promoteLiveSpeaker(
        @Path("roomId") roomId: String,
        @Body request: LiveSpeakerActionRequest
    ): Response<LiveRoomActionResponse>

    @POST("api/v1/live/rooms/{roomId}/demote-speaker")
    suspend fun demoteLiveSpeaker(
        @Path("roomId") roomId: String,
        @Body request: LiveSpeakerActionRequest
    ): Response<LiveRoomActionResponse>

    @POST("api/v1/live/rooms/{roomId}/end")
    suspend fun endLiveRoom(
        @Path("roomId") roomId: String,
        @Body request: EndLiveRoomRequest
    ): Response<LiveRoomActionResponse>

    // --- OnChat Conversations & Messaging (Official Spec) ---

    // 6. List Conversations (Direct Conversations)
    @GET("api/v1/chat/conversations")
    suspend fun getConversations(): Response<ConversationsListResponse>

    // 7. Create or Get Conversation by Wallet ID
    @POST("api/v1/chat/conversations")
    suspend fun createConversation(
        @Body request: CreateConversationRequest
    ): Response<CreateConversationResponse>

    // 8. Get Single Conversation with full message history
    @GET("api/v1/chat/conversations/{conversationId}")
    suspend fun getConversationDetails(
        @Path("conversationId") conversationId: String
    ): Response<ConversationDetailResponse>

    // 9. Delete Conversation
    @DELETE("api/v1/chat/conversations/{conversationId}")
    suspend fun deleteConversation(
        @Path("conversationId") conversationId: String
    ): Response<DeleteConversationResponse>

    // 10. Post Message in Conversation (Confirmed server route: POST /api/v1/chat/conversations/{conversationId})
    @POST("api/v1/chat/conversations/{conversationId}")
    suspend fun postMessage(
        @Path("conversationId") conversationId: String,
        @Body request: SendMessageRequest
    ): Response<SendMessageResponse>

    // 11. Delete Message in Conversation
    @DELETE("api/v1/chat/conversations/{conversationId}/messages/{messageId}")
    suspend fun deleteChatMessage(
        @Path("conversationId") conversationId: String,
        @Path("messageId") messageId: String
    ): Response<SimpleResponse>

    // 12. In-Chat Money Transfer Quote
    @POST("api/v1/chat/conversations/{conversationId}/transfer/quote")
    suspend fun quoteTransfer(
        @Path("conversationId") conversationId: String,
        @Body request: TransferQuoteRequest
    ): Response<TransferQuoteResponse>

    // 13. In-Chat Money Transfer Execution (CashPay TR Engine)
    @POST("api/v1/chat/conversations/{conversationId}/transfer")
    suspend fun executeTransfer(
        @Path("conversationId") conversationId: String,
        @Body request: TransferExecuteRequest
    ): Response<TransferExecuteResponse>

    // 14. Contacts Check (Batch)
    @POST("api/onchat/contacts/check")
    suspend fun checkContacts(
        @Body request: ContactsCheckRequest
    ): Response<List<ContactCheckResult>>

    // 6. List Groups
    @GET("api/onchat/groups")
    suspend fun getGroups(): Response<GroupsResponse>

    // 7. Create Group
    @POST("api/onchat/groups")
    suspend fun createGroup(
        @Body request: CreateGroupRequest
    ): Response<GroupDetailsResponse>

    // 8. Get Group Details
    @GET("api/onchat/groups/{groupId}")
    suspend fun getGroupDetails(
        @Path("groupId") groupId: Long
    ): Response<GroupDetailsResponse>

    // 9. Add Member to Group
    @POST("api/onchat/groups/{groupId}/members")
    suspend fun addGroupMember(
        @Path("groupId") groupId: Long,
        @Body request: AddMemberRequest
    ): Response<SimpleResponse>

    // 10. Remove Member from Group
    @DELETE("api/onchat/groups/{groupId}/members/{memberId}")
    suspend fun removeGroupMember(
        @Path("groupId") groupId: Long,
        @Path("memberId") memberId: Long
    ): Response<SimpleResponse>

    // 11. Edit Message
    @PUT("api/onchat/messages/{messageId}")
    suspend fun updateMessage(
        @Path("messageId") messageId: Long,
        @Body request: UpdateMessageRequest
    ): Response<SimpleResponse>

    // 12. Delete Message
    @DELETE("api/onchat/messages/{messageId}")
    suspend fun deleteMessage(
        @Path("messageId") messageId: Long
    ): Response<SimpleResponse>

    // 13. Statuses Management
    @GET("api/v1/statuses")
    suspend fun getStatuses(): Response<StatusesResponse>

    @POST("api/v1/statuses")
    suspend fun postStatus(
        @Body request: CreateStatusRequest
    ): Response<CreateStatusResponse>

    // Official Multipart Status Creation
    @Multipart
    @POST("api/onchat/statuses/create")
    suspend fun createStatusMultipart(
        @Part file: MultipartBody.Part,
        @Part("type") type: RequestBody,
        @Part("backgroundColor") backgroundColor: RequestBody? = null
    ): Response<CreateStatusMultipartResponse>

    // Official Status Retrieval
    @GET("api/onchat/statuses/create")
    suspend fun getStatusesMultipart(): Response<StatusesResponse>

    // Official CashPay Media Videos API Engine
    @Multipart
    @POST("api/v1/media/videos")
    suspend fun uploadVideo(
        @Part video: MultipartBody.Part,
        @Part("publisherType") publisherType: RequestBody? = null,
        @Part("publisherId") publisherId: RequestBody? = null,
        @Part("durationSeconds") durationSeconds: RequestBody? = null,
        @Part("thumbnailUrl") thumbnailUrl: RequestBody? = null
    ): Response<VideoUploadResponse>

    @GET("api/v1/media/videos")
    suspend fun getVideos(
        @Query("limit") limit: Int? = 20,
        @Query("offset") offset: Int? = 0
    ): Response<VideoListResponse>

    @GET("api/v1/media/videos/{id}")
    suspend fun getVideoDetail(
        @Path("id") id: Long
    ): Response<VideoDetailResponse>

    @DELETE("api/v1/media/videos/{id}")
    suspend fun deleteVideo(
        @Path("id") id: Long
    ): Response<SimpleResponse>

    @POST("api/v1/media/videos/{id}/like")
    suspend fun likeVideo(
        @Path("id") id: Long
    ): Response<VideoLikeResponse>

    @DELETE("api/v1/media/videos/{id}/like")
    suspend fun unlikeVideo(
        @Path("id") id: Long
    ): Response<VideoLikeResponse>

    @GET("api/v1/media/videos/{id}/comments")
    suspend fun getVideoComments(
        @Path("id") id: Long,
        @Query("limit") limit: Int? = 50,
        @Query("offset") offset: Int? = 0
    ): Response<VideoCommentsResponse>

    @POST("api/v1/media/videos/{id}/comments")
    suspend fun postVideoComment(
        @Path("id") id: Long,
        @Body request: PostVideoCommentRequest
    ): Response<PostVideoCommentResponse>

    @POST("api/onchat/statuses/check")
    suspend fun checkStatuses(
        @Body request: CheckStatusesRequest
    ): Response<CheckStatusesResponse>

    @POST("api/onchat/statuses/view")
    suspend fun viewStatus(
        @Body request: StatusViewRequest
    ): Response<StatusViewResponse>

    @POST("api/onchat/statuses/like")
    suspend fun likeStatus(
        @Body request: StatusLikeRequest
    ): Response<StatusLikeResponse>

    // 15. Wallet Transactions (History)
    @GET("api/v1/transactions")
    suspend fun getTransactions(
        @Query("limit") limit: Int? = 50,
        @Query("offset") offset: Int? = 0
    ): Response<TransactionsResponse>

    // 16. Full User Financial Wallet (GET /api/v1/wallet)
    @GET("api/v1/wallet")
    suspend fun getWallet(
    ): Response<WalletApiResponse>

    // 17. Execute Transfer (CashPay & Bitcoin)
    @POST("api/v1/transfers")
    suspend fun executeGlobalTransfer(
        @Body request: TransferRequest
    ): Response<TransferResponse>

    // 18. Jojo Shopping Feed & Follow
    @GET("api/v1/onchat/shopping/feed")
    suspend fun getShoppingFeed(
        @Query("limit") limit: Int? = 20
    ): Response<ShoppingFeedResponse>

    @POST("api/v1/onchat/shopping/feed")
    suspend fun followShop(
        @Body request: FollowShopRequest
    ): Response<FollowShopResponse>

    // 19. Events Feed
    @GET("api/v1/onchat/events/feed")
    suspend fun getEventsFeed(
        @Query("limit") limit: Int? = 10
    ): Response<EventsFeedResponse>

    // 20. MonPays Pages API
    @GET("api/onchat/monpays")
    suspend fun getMonPaysPages(
    ): Response<MonPaysListResponse>

    @GET("api/onchat/monpays/{companyId}")
    suspend fun getMonPaysPageDetail(
        @Path("companyId") companyId: Long
    ): Response<MonPaysDetailResponse>

    @POST("api/onchat/monpays/{companyId}/follow")
    suspend fun followMonPaysPage(
        @Path("companyId") companyId: Long,
        @Body request: MonPaysFollowRequest
    ): Response<MonPaysFollowResponse>

    @GET("api/onchat/monpays/feed")
    suspend fun getMonPaysFeed(
    ): Response<MonPaysFeedResponse>

    @GET("api/onchat/monpays/comments/{postId}")
    suspend fun getMonPaysComments(
        @Path("postId") postId: Long
    ): Response<MonPaysCommentsResponse>

    @POST("api/onchat/monpays/comments")
    suspend fun postMonPaysComment(
        @Body request: MonPaysCommentRequest
    ): Response<SimpleResponse>
}

