# @wishpool/api-client@1.0.0

A TypeScript SDK client for the localhost API.

## Usage

First, install the SDK from npm.

```bash
npm install @wishpool/api-client --save
```

Next, try it out.


```ts
import {
  Configuration,
  AdminApi,
} from '@wishpool/api-client';
import type { CreateAdminImageModelProviderRequest } from '@wishpool/api-client';

async function example() {
  console.log("🚀 Testing @wishpool/api-client SDK...");
  const config = new Configuration({ 
    // To configure API key authorization: internalToken
    apiKey: "YOUR API KEY",
  });
  const api = new AdminApi(config);

  const body = {
    // ImageModelProviderWriteRequest
    imageModelProviderWriteRequest: ...,
  } satisfies CreateAdminImageModelProviderRequest;

  try {
    const data = await api.createAdminImageModelProvider(body);
    console.log(data);
  } catch (error) {
    console.error(error);
  }
}

// Run the test
example().catch(console.error);
```


## Documentation

### API Endpoints

All URIs are relative to *http://localhost:8080*

| Class | Method | HTTP request | Description
| ----- | ------ | ------------ | -------------
*AdminApi* | [**createAdminImageModelProvider**](docs/AdminApi.md#createadminimagemodelprovider) | **POST** /internal/admin/image-model-providers | Create an image generation model provider.
*AdminApi* | [**deleteAdminImageModelProvider**](docs/AdminApi.md#deleteadminimagemodelprovider) | **DELETE** /internal/admin/image-model-providers/{id} | Delete an image generation model provider.
*AdminApi* | [**getAdminDashboard**](docs/AdminApi.md#getadmindashboard) | **GET** /internal/admin/dashboard | Get local administration dashboard counters.
*AdminApi* | [**grantAdminMediaAccess**](docs/AdminApi.md#grantadminmediaaccess) | **POST** /internal/admin/media-access-grants | Create an audited short-lived media access grant.
*AdminApi* | [**listAdminAuditLogs**](docs/AdminApi.md#listadminauditlogs) | **GET** /internal/admin/audit-logs | List audit logs for administration.
*AdminApi* | [**listAdminFamilies**](docs/AdminApi.md#listadminfamilies) | **GET** /internal/admin/families | List family metadata for administration.
*AdminApi* | [**listAdminImageGenUsages**](docs/AdminApi.md#listadminimagegenusages) | **GET** /internal/admin/image-gen-usages | List business usage to image provider mappings.
*AdminApi* | [**listAdminImageModelProviders**](docs/AdminApi.md#listadminimagemodelproviders) | **GET** /internal/admin/image-model-providers | List image generation model provider configurations with masked API keys.
*AdminApi* | [**listAdminPrivacyRequests**](docs/AdminApi.md#listadminprivacyrequests) | **GET** /internal/admin/privacy-requests | List privacy requests for administration.
*AdminApi* | [**setDefaultAdminImageModelProvider**](docs/AdminApi.md#setdefaultadminimagemodelprovider) | **POST** /internal/admin/image-model-providers/{id}/set-default | Set the global default image generation model provider.
*AdminApi* | [**toggleAdminImageModelProvider**](docs/AdminApi.md#toggleadminimagemodelprovider) | **POST** /internal/admin/image-model-providers/{id}/toggle | Enable or disable an image generation model provider.
*AdminApi* | [**updateAdminImageModelProvider**](docs/AdminApi.md#updateadminimagemodelprovider) | **PUT** /internal/admin/image-model-providers/{id} | Update an image generation model provider.
*AdminApi* | [**upsertAdminImageGenUsage**](docs/AdminApi.md#upsertadminimagegenusage) | **PUT** /internal/admin/image-gen-usages/{usageCode} | Create or update a business usage to image provider mapping.
*AuthApi* | [**getMe**](docs/AuthApi.md#getme) | **GET** /me | Get the current authenticated user context.
*AuthApi* | [**login**](docs/AuthApi.md#loginoperation) | **POST** /auth/login | Log in a parent user with phone or provider token.
*AuthApi* | [**refreshSession**](docs/AuthApi.md#refreshsession) | **POST** /auth/refresh | Refresh an access token.
*AuthApi* | [**requestPhoneCode**](docs/AuthApi.md#requestphonecode) | **POST** /auth/phone-codes | Request a phone verification code for parent login.
*ChildrenApi* | [**createChild**](docs/ChildrenApi.md#createchildoperation) | **POST** /families/{familyId}/children | Create a child profile.
*ChildrenApi* | [**getChildRewardSummary**](docs/ChildrenApi.md#getchildrewardsummary) | **GET** /children/{childId}/rewards/summary | Get read-only reward totals for a child.
*ChildrenApi* | [**listChildren**](docs/ChildrenApi.md#listchildren) | **GET** /families/{familyId}/children | List children in a family.
*ChildrenApi* | [**updateChild**](docs/ChildrenApi.md#updatechildoperation) | **PATCH** /children/{childId} | Update child profile.
*FamiliesApi* | [**createFamily**](docs/FamiliesApi.md#createfamilyoperation) | **POST** /families | Create a family.
*FamiliesApi* | [**getFamily**](docs/FamiliesApi.md#getfamily) | **GET** /families/{familyId} | Get family details.
*FamiliesApi* | [**inviteParent**](docs/FamiliesApi.md#inviteparentoperation) | **POST** /families/{familyId}/invites | Invite a parent to a family.
*FamiliesApi* | [**listFamilyMembers**](docs/FamiliesApi.md#listfamilymembers) | **GET** /families/{familyId}/members | List family members.
*HomeApi* | [**getChildHomeContext**](docs/HomeApi.md#getchildhomecontext) | **GET** /children/{childId}/home-context | Get the child-facing home context used by mobile and tablet clients.
*HomeApi* | [**getParentDashboardContext**](docs/HomeApi.md#getparentdashboardcontext) | **GET** /families/{familyId}/parent-dashboard | Get the parent dashboard context used by mobile and web clients.
*InternalApi* | [**acceptGenerateMemoryWorkflowActivity**](docs/InternalApi.md#acceptgeneratememoryworkflowactivity) | **POST** /internal/workflows/generate-memory | Execute weekly memory workflow activity.
*InternalApi* | [**acceptPrivacyDeletionWorkflowActivity**](docs/InternalApi.md#acceptprivacydeletionworkflowactivity) | **POST** /internal/workflows/privacy-deletion | Execute privacy deletion workflow activity.
*InternalApi* | [**claimNotificationEvents**](docs/InternalApi.md#claimnotificationeventsoperation) | **POST** /internal/notifications/claim | Claim pending notification events for dispatch.
*InternalApi* | [**claimOutboxEvents**](docs/InternalApi.md#claimoutboxeventsoperation) | **POST** /internal/outbox/events/claim | Claim unpublished outbox events for an internal publisher.
*InternalApi* | [**createNotification**](docs/InternalApi.md#createnotification) | **POST** /internal/notifications | Create or deduplicate an internal notification event.
*InternalApi* | [**markNotificationDispatchResult**](docs/InternalApi.md#marknotificationdispatchresult) | **POST** /internal/notifications/{notificationId}/dispatch-result | Mark notification dispatch result.
*InternalApi* | [**markOutboxEventPublished**](docs/InternalApi.md#markoutboxeventpublished) | **POST** /internal/outbox/events/{eventId}/published | Mark an outbox event as published.
*InternalApi* | [**retryOutboxEvent**](docs/InternalApi.md#retryoutboxeventoperation) | **POST** /internal/outbox/events/{eventId}/retry | Release an outbox event lease and schedule retry.
*InternalApi* | [**runAiPrecheckWorkflowActivity**](docs/InternalApi.md#runaiprecheckworkflowactivity) | **POST** /internal/workflows/run-ai-precheck | Execute submission AI precheck activity.
*InternalApi* | [**runMaterializeWeeklyPlanWorkflowActivity**](docs/InternalApi.md#runmaterializeweeklyplanworkflowactivity) | **POST** /internal/workflows/materialize-weekly-plan | Execute weekly plan materialization activity.
*InternalApi* | [**runRewardEvaluationWorkflowActivity**](docs/InternalApi.md#runrewardevaluationworkflowactivity) | **POST** /internal/workflows/evaluate-reward | Execute reward evaluation activity.
*MediaApi* | [**createUploadSession**](docs/MediaApi.md#createuploadsessionoperation) | **POST** /media/upload-sessions | Create a signed upload session.
*MediaApi* | [**finalizeMedia**](docs/MediaApi.md#finalizemediaoperation) | **POST** /media/{mediaId}/finalize | Finalize a media upload.
*MemoriesApi* | [**exportMemory**](docs/MemoriesApi.md#exportmemoryoperation) | **POST** /memories/{memoryId}/export | Request memory export.
*MemoriesApi* | [**featureMemory**](docs/MemoriesApi.md#featurememory) | **POST** /memories/{memoryId}/feature | Feature a memory into the child\&#39;s room (idempotent)
*MemoriesApi* | [**getMemory**](docs/MemoriesApi.md#getmemory) | **GET** /memories/{memoryId} | Get a memory detail.
*MemoriesApi* | [**listMemories**](docs/MemoriesApi.md#listmemories) | **GET** /memories | List memories for a child.
*NotificationsApi* | [**listNotificationPreferences**](docs/NotificationsApi.md#listnotificationpreferences) | **GET** /notification-preferences | List notification preferences for the current user.
*NotificationsApi* | [**listNotifications**](docs/NotificationsApi.md#listnotifications) | **GET** /notifications | List the current user\&#39;s inbox notifications.
*NotificationsApi* | [**markNotificationsRead**](docs/NotificationsApi.md#marknotificationsread) | **POST** /notifications/read | Mark inbox notifications as read.
*NotificationsApi* | [**registerPushToken**](docs/NotificationsApi.md#registerpushtokenoperation) | **POST** /devices/push-token | Register or update a push token for the current device.
*NotificationsApi* | [**updateNotificationPreference**](docs/NotificationsApi.md#updatenotificationpreferenceoperation) | **PUT** /notification-preferences | Upsert one notification preference for the current user.
*PairingApi* | [**consumePairingCode**](docs/PairingApi.md#consumepairingcodeoperation) | **POST** /pairing/consume | Pair a child device with a family.
*PairingApi* | [**createPairingSession**](docs/PairingApi.md#createpairingsessionoperation) | **POST** /families/{familyId}/pairing-sessions | Create a child device pairing session.
*PlansApi* | [**getWeeklyPlan**](docs/PlansApi.md#getweeklyplan) | **GET** /plans/{planId} | Get a weekly plan.
*PlansApi* | [**saveWeeklyPlan**](docs/PlansApi.md#saveweeklyplanoperation) | **POST** /plans | Create or update a weekly plan.
*PrivacyApi* | [**requestDataExport**](docs/PrivacyApi.md#requestdataexport) | **POST** /privacy/export | Request family data export.
*PrivacyApi* | [**requestFamilyDeletion**](docs/PrivacyApi.md#requestfamilydeletion) | **POST** /privacy/delete | Request family deletion.
*ReviewsApi* | [**getReviewDetail**](docs/ReviewsApi.md#getreviewdetail) | **GET** /reviews/{submissionId}/detail | Get a review detail view for a submission.
*ReviewsApi* | [**listPendingReviews**](docs/ReviewsApi.md#listpendingreviews) | **GET** /reviews/pending | List pending reviews.
*ReviewsApi* | [**reviewSubmission**](docs/ReviewsApi.md#reviewsubmissionoperation) | **POST** /reviews | Approve or reject a submission.
*ReviewsApi* | [**revokeReview**](docs/ReviewsApi.md#revokereviewoperation) | **POST** /reviews/{reviewId}/revoke | Revoke a review and create adjustment records when needed.
*RoomApi* | [**arrangeRoomItem**](docs/RoomApi.md#arrangeroomitemoperation) | **POST** /room/items/{itemId}/arrange | Arrange a room item.
*RoomApi* | [**getRoomState**](docs/RoomApi.md#getroomstate) | **GET** /room/state | Get room state for a child.
*RoomApi* | [**hideRoomItem**](docs/RoomApi.md#hideroomitem) | **POST** /room/items/{itemId}/hide | Hide a room item without deleting its history or position.
*RoomApi* | [**unhideRoomItem**](docs/RoomApi.md#unhideroomitem) | **POST** /room/items/{itemId}/unhide | Restore a hidden room item without changing its position.
*SubmissionsApi* | [**createSubmission**](docs/SubmissionsApi.md#createsubmissionoperation) | **POST** /submissions | Create a child submission.
*SubmissionsApi* | [**getSubmission**](docs/SubmissionsApi.md#getsubmission) | **GET** /submissions/{submissionId} | Get submission details.
*SyncApi* | [**pullSyncEvents**](docs/SyncApi.md#pullsyncevents) | **GET** /sync/pull | Pull family events after a sequence number.
*TaskTemplatesApi* | [**createTaskTemplate**](docs/TaskTemplatesApi.md#createtasktemplateoperation) | **POST** /task-templates | Create a task template.
*TaskTemplatesApi* | [**listTaskTemplates**](docs/TaskTemplatesApi.md#listtasktemplates) | **GET** /task-templates | List task templates for the current family.
*TasksApi* | [**getToday**](docs/TasksApi.md#gettoday) | **GET** /children/{childId}/today | Get child today dashboard data.
*TasksApi* | [**postponeTask**](docs/TasksApi.md#postponetaskoperation) | **POST** /tasks/{taskId}/postpone | Postpone a task to another date.
*TasksApi* | [**skipTask**](docs/TasksApi.md#skiptaskoperation) | **POST** /tasks/{taskId}/skip | Mark a task as skipped.
*WishesApi* | [**activateWish**](docs/WishesApi.md#activatewish) | **POST** /wishes/{wishId}/activate | Activate a wish.
*WishesApi* | [**attachWishImage**](docs/WishesApi.md#attachwishimageoperation) | **POST** /wishes/{wishId}/image | Attach a finalized wish image to a wish.
*WishesApi* | [**createWish**](docs/WishesApi.md#createwishoperation) | **POST** /wishes | Create a wish.
*WishesApi* | [**createWishImageGeneration**](docs/WishesApi.md#createwishimagegenerationoperation) | **POST** /wishes/image-generations | Create a wish image generation job.
*WishesApi* | [**findWishImageCandidates**](docs/WishesApi.md#findwishimagecandidates) | **POST** /wishes/image-candidates | Find reusable wish image candidates for a new wish.
*WishesApi* | [**getCurrentWish**](docs/WishesApi.md#getcurrentwish) | **GET** /children/{childId}/wishes/current | Get the current wish for a child.
*WishesApi* | [**getWish**](docs/WishesApi.md#getwish) | **GET** /wishes/{wishId} | Get a wish.
*WishesApi* | [**getWishImageGeneration**](docs/WishesApi.md#getwishimagegeneration) | **GET** /wishes/image-generations/{jobId} | Get a wish image generation job.
*WishesApi* | [**listChildWishHistory**](docs/WishesApi.md#listchildwishhistory) | **GET** /children/{childId}/wishes/history | List wish history items for a child.
*WishesApi* | [**listChildWishes**](docs/WishesApi.md#listchildwishes) | **GET** /children/{childId}/wishes | List wishes for a child.
*WishesApi* | [**listWishImageModelProviders**](docs/WishesApi.md#listwishimagemodelproviders) | **GET** /wishes/image-model-providers | List enabled image generation model providers for a business usage.
*WishesApi* | [**redeemWish**](docs/WishesApi.md#redeemwishoperation) | **POST** /wishes/{wishId}/redeem | Redeem an unlocked wish.


### Models

- [AdminAuditLog](docs/AdminAuditLog.md)
- [AdminDashboard](docs/AdminDashboard.md)
- [AdminFamilySummary](docs/AdminFamilySummary.md)
- [AdminMediaAccessGrant](docs/AdminMediaAccessGrant.md)
- [AdminMediaAccessGrantRequest](docs/AdminMediaAccessGrantRequest.md)
- [AdminPrivacyRequest](docs/AdminPrivacyRequest.md)
- [AiPrecheck](docs/AiPrecheck.md)
- [ArrangeRoomItemRequest](docs/ArrangeRoomItemRequest.md)
- [ArrangeRoomItemRequestPosition](docs/ArrangeRoomItemRequestPosition.md)
- [AttachWishImageRequest](docs/AttachWishImageRequest.md)
- [AuthTokenPair](docs/AuthTokenPair.md)
- [BusinessImageModelProviderList](docs/BusinessImageModelProviderList.md)
- [ChildFeedbackCard](docs/ChildFeedbackCard.md)
- [ChildHomeContext](docs/ChildHomeContext.md)
- [ChildProfile](docs/ChildProfile.md)
- [ClaimNotificationEventsRequest](docs/ClaimNotificationEventsRequest.md)
- [ClaimNotificationEventsResponse](docs/ClaimNotificationEventsResponse.md)
- [ClaimOutboxEventsRequest](docs/ClaimOutboxEventsRequest.md)
- [ConsumePairingCodeRequest](docs/ConsumePairingCodeRequest.md)
- [CreateChildRequest](docs/CreateChildRequest.md)
- [CreateFamilyRequest](docs/CreateFamilyRequest.md)
- [CreateNotificationEventRequest](docs/CreateNotificationEventRequest.md)
- [CreatePairingSessionRequest](docs/CreatePairingSessionRequest.md)
- [CreateSubmissionRequest](docs/CreateSubmissionRequest.md)
- [CreateTaskTemplateRequest](docs/CreateTaskTemplateRequest.md)
- [CreateUploadSessionRequest](docs/CreateUploadSessionRequest.md)
- [CreateWishImageGenerationRequest](docs/CreateWishImageGenerationRequest.md)
- [CreateWishRequest](docs/CreateWishRequest.md)
- [DailySummary](docs/DailySummary.md)
- [DeviceRegistration](docs/DeviceRegistration.md)
- [EvaluateRewardWorkflowRequest](docs/EvaluateRewardWorkflowRequest.md)
- [ExportMemoryRequest](docs/ExportMemoryRequest.md)
- [Family](docs/Family.md)
- [FamilyEvent](docs/FamilyEvent.md)
- [FamilyInvite](docs/FamilyInvite.md)
- [FamilyMember](docs/FamilyMember.md)
- [FamilyMemberContext](docs/FamilyMemberContext.md)
- [Feedback](docs/Feedback.md)
- [FeedbackInput](docs/FeedbackInput.md)
- [FinalizeMediaRequest](docs/FinalizeMediaRequest.md)
- [GenerateMemoryWorkflowRequest](docs/GenerateMemoryWorkflowRequest.md)
- [ImageGenUsage](docs/ImageGenUsage.md)
- [ImageGenUsageWriteRequest](docs/ImageGenUsageWriteRequest.md)
- [ImageModelProvider](docs/ImageModelProvider.md)
- [ImageModelProviderToggleRequest](docs/ImageModelProviderToggleRequest.md)
- [ImageModelProviderWriteRequest](docs/ImageModelProviderWriteRequest.md)
- [InviteParentRequest](docs/InviteParentRequest.md)
- [LoginRequest](docs/LoginRequest.md)
- [MarkNotificationReadRequest](docs/MarkNotificationReadRequest.md)
- [MaterializeWeeklyPlanWorkflowRequest](docs/MaterializeWeeklyPlanWorkflowRequest.md)
- [MeResponse](docs/MeResponse.md)
- [MediaAsset](docs/MediaAsset.md)
- [MemberRole](docs/MemberRole.md)
- [MemoryExport](docs/MemoryExport.md)
- [MemoryItem](docs/MemoryItem.md)
- [MemoryTimeline](docs/MemoryTimeline.md)
- [NotificationDevice](docs/NotificationDevice.md)
- [NotificationDispatchItem](docs/NotificationDispatchItem.md)
- [NotificationDispatchResultRequest](docs/NotificationDispatchResultRequest.md)
- [NotificationEvent](docs/NotificationEvent.md)
- [NotificationListResponse](docs/NotificationListResponse.md)
- [NotificationPreference](docs/NotificationPreference.md)
- [OutboxAckResponse](docs/OutboxAckResponse.md)
- [OutboxClaimResponse](docs/OutboxClaimResponse.md)
- [OutboxEvent](docs/OutboxEvent.md)
- [PairingSessionCreated](docs/PairingSessionCreated.md)
- [ParentDashboardContext](docs/ParentDashboardContext.md)
- [PendingReviewCard](docs/PendingReviewCard.md)
- [PhoneCodeCreated](docs/PhoneCodeCreated.md)
- [PhoneCodeRequest](docs/PhoneCodeRequest.md)
- [PostponeTaskRequest](docs/PostponeTaskRequest.md)
- [PrivacyDeletionWorkflowRequest](docs/PrivacyDeletionWorkflowRequest.md)
- [PrivacyRequest](docs/PrivacyRequest.md)
- [PrivacyRequestCreate](docs/PrivacyRequestCreate.md)
- [Problem](docs/Problem.md)
- [RedeemWishRequest](docs/RedeemWishRequest.md)
- [RefreshRequest](docs/RefreshRequest.md)
- [RegisterPushTokenRequest](docs/RegisterPushTokenRequest.md)
- [RelatedResource](docs/RelatedResource.md)
- [RetryOutboxEventRequest](docs/RetryOutboxEventRequest.md)
- [Review](docs/Review.md)
- [ReviewSubmissionRequest](docs/ReviewSubmissionRequest.md)
- [RevokeReviewRequest](docs/RevokeReviewRequest.md)
- [RewardSummary](docs/RewardSummary.md)
- [RoomItem](docs/RoomItem.md)
- [RoomState](docs/RoomState.md)
- [RunAiPrecheckWorkflowRequest](docs/RunAiPrecheckWorkflowRequest.md)
- [SaveWeeklyPlanRequest](docs/SaveWeeklyPlanRequest.md)
- [SkipTaskRequest](docs/SkipTaskRequest.md)
- [Submission](docs/Submission.md)
- [SubmissionDetail](docs/SubmissionDetail.md)
- [SubmissionType](docs/SubmissionType.md)
- [SyncPullResponse](docs/SyncPullResponse.md)
- [TaskCategory](docs/TaskCategory.md)
- [TaskInstance](docs/TaskInstance.md)
- [TaskTemplate](docs/TaskTemplate.md)
- [TodaySnapshot](docs/TodaySnapshot.md)
- [UpdateChildRequest](docs/UpdateChildRequest.md)
- [UpdateNotificationPreferenceRequest](docs/UpdateNotificationPreferenceRequest.md)
- [UploadSession](docs/UploadSession.md)
- [User](docs/User.md)
- [WeeklyMemory](docs/WeeklyMemory.md)
- [WeeklyPlan](docs/WeeklyPlan.md)
- [WeeklyPlanRule](docs/WeeklyPlanRule.md)
- [WeeklyPlanRuleInput](docs/WeeklyPlanRuleInput.md)
- [Wish](docs/Wish.md)
- [WishFragmentCell](docs/WishFragmentCell.md)
- [WishFragmentMask](docs/WishFragmentMask.md)
- [WishFragmentPoint](docs/WishFragmentPoint.md)
- [WishFragmentVisual](docs/WishFragmentVisual.md)
- [WishHistoryItem](docs/WishHistoryItem.md)
- [WishImageCandidate](docs/WishImageCandidate.md)
- [WishImageCandidateQuery](docs/WishImageCandidateQuery.md)
- [WishImageCandidateRequest](docs/WishImageCandidateRequest.md)
- [WishImageCandidateResponse](docs/WishImageCandidateResponse.md)
- [WishImageGenerationJob](docs/WishImageGenerationJob.md)
- [WishRedemption](docs/WishRedemption.md)
- [WorkflowAcceptedResponse](docs/WorkflowAcceptedResponse.md)

### Authorization


Authentication schemes defined for the API:
<a id="bearerAuth"></a>
#### bearerAuth


- **Type**: HTTP Bearer Token authentication (JWT)
<a id="internalToken"></a>
#### internalToken


- **Type**: API key
- **API key parameter name**: `X-Internal-Token`
- **Location**: HTTP header

## About

This TypeScript SDK client supports the [Fetch API](https://fetch.spec.whatwg.org/)
and is automatically generated by the
[OpenAPI Generator](https://openapi-generator.tech) project:

- API version: `1.0.0`
- Package version: `1.0.0`
- Generator version: `7.24.0`
- Build package: `org.openapitools.codegen.languages.TypeScriptFetchClientCodegen`

The generated npm module supports the following:

- Environments
  * Node.js
  * Webpack
  * Browserify
- Language levels
  * ES5 - you must have a Promises/A+ library installed
  * ES6
- Module systems
  * CommonJS
  * ES6 module system


## Development

### Building

To build the TypeScript source code, you need to have Node.js and npm installed.
After cloning the repository, navigate to the project directory and run:

```bash
npm install
npm run build
```

### Publishing

Once you've built the package, you can publish it to npm:

```bash
npm publish
```

## License

[Proprietary]()
