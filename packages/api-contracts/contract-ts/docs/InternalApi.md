# InternalApi

All URIs are relative to *http://localhost:8080*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**acceptGenerateMemoryWorkflowActivity**](InternalApi.md#acceptgeneratememoryworkflowactivity) | **POST** /internal/workflows/generate-memory | Execute weekly memory workflow activity. |
| [**acceptPrivacyDeletionWorkflowActivity**](InternalApi.md#acceptprivacydeletionworkflowactivity) | **POST** /internal/workflows/privacy-deletion | Execute privacy deletion workflow activity. |
| [**claimNotificationEvents**](InternalApi.md#claimnotificationeventsoperation) | **POST** /internal/notifications/claim | Claim pending notification events for dispatch. |
| [**claimOutboxEvents**](InternalApi.md#claimoutboxeventsoperation) | **POST** /internal/outbox/events/claim | Claim unpublished outbox events for an internal publisher. |
| [**createNotification**](InternalApi.md#createnotification) | **POST** /internal/notifications | Create or deduplicate an internal notification event. |
| [**markNotificationDispatchResult**](InternalApi.md#marknotificationdispatchresult) | **POST** /internal/notifications/{notificationId}/dispatch-result | Mark notification dispatch result. |
| [**markOutboxEventPublished**](InternalApi.md#markoutboxeventpublished) | **POST** /internal/outbox/events/{eventId}/published | Mark an outbox event as published. |
| [**retryOutboxEvent**](InternalApi.md#retryoutboxeventoperation) | **POST** /internal/outbox/events/{eventId}/retry | Release an outbox event lease and schedule retry. |
| [**runAiPrecheckWorkflowActivity**](InternalApi.md#runaiprecheckworkflowactivity) | **POST** /internal/workflows/run-ai-precheck | Execute submission AI precheck activity. |
| [**runMaterializeWeeklyPlanWorkflowActivity**](InternalApi.md#runmaterializeweeklyplanworkflowactivity) | **POST** /internal/workflows/materialize-weekly-plan | Execute weekly plan materialization activity. |
| [**runRewardEvaluationWorkflowActivity**](InternalApi.md#runrewardevaluationworkflowactivity) | **POST** /internal/workflows/evaluate-reward | Execute reward evaluation activity. |



## acceptGenerateMemoryWorkflowActivity

> WorkflowAcceptedResponse acceptGenerateMemoryWorkflowActivity(generateMemoryWorkflowRequest)

Execute weekly memory workflow activity.

### Example

```ts
import {
  Configuration,
  InternalApi,
} from '@wishpool/api-client';
import type { AcceptGenerateMemoryWorkflowActivityRequest } from '@wishpool/api-client';

async function example() {
  console.log("🚀 Testing @wishpool/api-client SDK...");
  const config = new Configuration({ 
    // To configure API key authorization: internalToken
    apiKey: "YOUR API KEY",
  });
  const api = new InternalApi(config);

  const body = {
    // GenerateMemoryWorkflowRequest
    generateMemoryWorkflowRequest: ...,
  } satisfies AcceptGenerateMemoryWorkflowActivityRequest;

  try {
    const data = await api.acceptGenerateMemoryWorkflowActivity(body);
    console.log(data);
  } catch (error) {
    console.error(error);
  }
}

// Run the test
example().catch(console.error);
```

### Parameters


| Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **generateMemoryWorkflowRequest** | [GenerateMemoryWorkflowRequest](GenerateMemoryWorkflowRequest.md) |  | |

### Return type

[**WorkflowAcceptedResponse**](WorkflowAcceptedResponse.md)

### Authorization

[internalToken](../README.md#internalToken)

### HTTP request headers

- **Content-Type**: `application/json`
- **Accept**: `application/json`, `application/problem+json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Memory workflow activity completed. |  -  |
| **0** | Error response. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## acceptPrivacyDeletionWorkflowActivity

> WorkflowAcceptedResponse acceptPrivacyDeletionWorkflowActivity(privacyDeletionWorkflowRequest)

Execute privacy deletion workflow activity.

### Example

```ts
import {
  Configuration,
  InternalApi,
} from '@wishpool/api-client';
import type { AcceptPrivacyDeletionWorkflowActivityRequest } from '@wishpool/api-client';

async function example() {
  console.log("🚀 Testing @wishpool/api-client SDK...");
  const config = new Configuration({ 
    // To configure API key authorization: internalToken
    apiKey: "YOUR API KEY",
  });
  const api = new InternalApi(config);

  const body = {
    // PrivacyDeletionWorkflowRequest
    privacyDeletionWorkflowRequest: ...,
  } satisfies AcceptPrivacyDeletionWorkflowActivityRequest;

  try {
    const data = await api.acceptPrivacyDeletionWorkflowActivity(body);
    console.log(data);
  } catch (error) {
    console.error(error);
  }
}

// Run the test
example().catch(console.error);
```

### Parameters


| Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **privacyDeletionWorkflowRequest** | [PrivacyDeletionWorkflowRequest](PrivacyDeletionWorkflowRequest.md) |  | |

### Return type

[**WorkflowAcceptedResponse**](WorkflowAcceptedResponse.md)

### Authorization

[internalToken](../README.md#internalToken)

### HTTP request headers

- **Content-Type**: `application/json`
- **Accept**: `application/json`, `application/problem+json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Privacy deletion workflow activity completed. |  -  |
| **0** | Error response. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## claimNotificationEvents

> ClaimNotificationEventsResponse claimNotificationEvents(claimNotificationEventsRequest)

Claim pending notification events for dispatch.

### Example

```ts
import {
  Configuration,
  InternalApi,
} from '@wishpool/api-client';
import type { ClaimNotificationEventsOperationRequest } from '@wishpool/api-client';

async function example() {
  console.log("🚀 Testing @wishpool/api-client SDK...");
  const config = new Configuration({ 
    // To configure API key authorization: internalToken
    apiKey: "YOUR API KEY",
  });
  const api = new InternalApi(config);

  const body = {
    // ClaimNotificationEventsRequest
    claimNotificationEventsRequest: ...,
  } satisfies ClaimNotificationEventsOperationRequest;

  try {
    const data = await api.claimNotificationEvents(body);
    console.log(data);
  } catch (error) {
    console.error(error);
  }
}

// Run the test
example().catch(console.error);
```

### Parameters


| Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **claimNotificationEventsRequest** | [ClaimNotificationEventsRequest](ClaimNotificationEventsRequest.md) |  | |

### Return type

[**ClaimNotificationEventsResponse**](ClaimNotificationEventsResponse.md)

### Authorization

[internalToken](../README.md#internalToken)

### HTTP request headers

- **Content-Type**: `application/json`
- **Accept**: `application/json`, `application/problem+json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Claimed notifications. |  -  |
| **0** | Error response. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## claimOutboxEvents

> OutboxClaimResponse claimOutboxEvents(claimOutboxEventsRequest)

Claim unpublished outbox events for an internal publisher.

### Example

```ts
import {
  Configuration,
  InternalApi,
} from '@wishpool/api-client';
import type { ClaimOutboxEventsOperationRequest } from '@wishpool/api-client';

async function example() {
  console.log("🚀 Testing @wishpool/api-client SDK...");
  const config = new Configuration({ 
    // To configure API key authorization: internalToken
    apiKey: "YOUR API KEY",
  });
  const api = new InternalApi(config);

  const body = {
    // ClaimOutboxEventsRequest
    claimOutboxEventsRequest: ...,
  } satisfies ClaimOutboxEventsOperationRequest;

  try {
    const data = await api.claimOutboxEvents(body);
    console.log(data);
  } catch (error) {
    console.error(error);
  }
}

// Run the test
example().catch(console.error);
```

### Parameters


| Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **claimOutboxEventsRequest** | [ClaimOutboxEventsRequest](ClaimOutboxEventsRequest.md) |  | |

### Return type

[**OutboxClaimResponse**](OutboxClaimResponse.md)

### Authorization

[internalToken](../README.md#internalToken)

### HTTP request headers

- **Content-Type**: `application/json`
- **Accept**: `application/json`, `application/problem+json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Claimed outbox events. |  -  |
| **0** | Error response. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## createNotification

> NotificationEvent createNotification(createNotificationEventRequest)

Create or deduplicate an internal notification event.

### Example

```ts
import {
  Configuration,
  InternalApi,
} from '@wishpool/api-client';
import type { CreateNotificationRequest } from '@wishpool/api-client';

async function example() {
  console.log("🚀 Testing @wishpool/api-client SDK...");
  const config = new Configuration({ 
    // To configure API key authorization: internalToken
    apiKey: "YOUR API KEY",
  });
  const api = new InternalApi(config);

  const body = {
    // CreateNotificationEventRequest
    createNotificationEventRequest: ...,
  } satisfies CreateNotificationRequest;

  try {
    const data = await api.createNotification(body);
    console.log(data);
  } catch (error) {
    console.error(error);
  }
}

// Run the test
example().catch(console.error);
```

### Parameters


| Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **createNotificationEventRequest** | [CreateNotificationEventRequest](CreateNotificationEventRequest.md) |  | |

### Return type

[**NotificationEvent**](NotificationEvent.md)

### Authorization

[internalToken](../README.md#internalToken)

### HTTP request headers

- **Content-Type**: `application/json`
- **Accept**: `application/json`, `application/problem+json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Notification event. |  -  |
| **0** | Error response. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## markNotificationDispatchResult

> NotificationEvent markNotificationDispatchResult(notificationId, notificationDispatchResultRequest)

Mark notification dispatch result.

### Example

```ts
import {
  Configuration,
  InternalApi,
} from '@wishpool/api-client';
import type { MarkNotificationDispatchResultRequest } from '@wishpool/api-client';

async function example() {
  console.log("🚀 Testing @wishpool/api-client SDK...");
  const config = new Configuration({ 
    // To configure API key authorization: internalToken
    apiKey: "YOUR API KEY",
  });
  const api = new InternalApi(config);

  const body = {
    // string
    notificationId: 38400000-8cf0-11bd-b23e-10b96e4ef00d,
    // NotificationDispatchResultRequest
    notificationDispatchResultRequest: ...,
  } satisfies MarkNotificationDispatchResultRequest;

  try {
    const data = await api.markNotificationDispatchResult(body);
    console.log(data);
  } catch (error) {
    console.error(error);
  }
}

// Run the test
example().catch(console.error);
```

### Parameters


| Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **notificationId** | `string` |  | [Defaults to `undefined`] |
| **notificationDispatchResultRequest** | [NotificationDispatchResultRequest](NotificationDispatchResultRequest.md) |  | |

### Return type

[**NotificationEvent**](NotificationEvent.md)

### Authorization

[internalToken](../README.md#internalToken)

### HTTP request headers

- **Content-Type**: `application/json`
- **Accept**: `application/json`, `application/problem+json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Notification event. |  -  |
| **0** | Error response. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## markOutboxEventPublished

> OutboxAckResponse markOutboxEventPublished(eventId)

Mark an outbox event as published.

### Example

```ts
import {
  Configuration,
  InternalApi,
} from '@wishpool/api-client';
import type { MarkOutboxEventPublishedRequest } from '@wishpool/api-client';

async function example() {
  console.log("🚀 Testing @wishpool/api-client SDK...");
  const config = new Configuration({ 
    // To configure API key authorization: internalToken
    apiKey: "YOUR API KEY",
  });
  const api = new InternalApi(config);

  const body = {
    // string
    eventId: 38400000-8cf0-11bd-b23e-10b96e4ef00d,
  } satisfies MarkOutboxEventPublishedRequest;

  try {
    const data = await api.markOutboxEventPublished(body);
    console.log(data);
  } catch (error) {
    console.error(error);
  }
}

// Run the test
example().catch(console.error);
```

### Parameters


| Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **eventId** | `string` |  | [Defaults to `undefined`] |

### Return type

[**OutboxAckResponse**](OutboxAckResponse.md)

### Authorization

[internalToken](../README.md#internalToken)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`, `application/problem+json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Outbox event publication acknowledged. |  -  |
| **0** | Error response. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## retryOutboxEvent

> OutboxEvent retryOutboxEvent(eventId, retryOutboxEventRequest)

Release an outbox event lease and schedule retry.

### Example

```ts
import {
  Configuration,
  InternalApi,
} from '@wishpool/api-client';
import type { RetryOutboxEventOperationRequest } from '@wishpool/api-client';

async function example() {
  console.log("🚀 Testing @wishpool/api-client SDK...");
  const config = new Configuration({ 
    // To configure API key authorization: internalToken
    apiKey: "YOUR API KEY",
  });
  const api = new InternalApi(config);

  const body = {
    // string
    eventId: 38400000-8cf0-11bd-b23e-10b96e4ef00d,
    // RetryOutboxEventRequest
    retryOutboxEventRequest: ...,
  } satisfies RetryOutboxEventOperationRequest;

  try {
    const data = await api.retryOutboxEvent(body);
    console.log(data);
  } catch (error) {
    console.error(error);
  }
}

// Run the test
example().catch(console.error);
```

### Parameters


| Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **eventId** | `string` |  | [Defaults to `undefined`] |
| **retryOutboxEventRequest** | [RetryOutboxEventRequest](RetryOutboxEventRequest.md) |  | |

### Return type

[**OutboxEvent**](OutboxEvent.md)

### Authorization

[internalToken](../README.md#internalToken)

### HTTP request headers

- **Content-Type**: `application/json`
- **Accept**: `application/json`, `application/problem+json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Outbox event retry scheduled. |  -  |
| **0** | Error response. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## runAiPrecheckWorkflowActivity

> WorkflowAcceptedResponse runAiPrecheckWorkflowActivity(runAiPrecheckWorkflowRequest)

Execute submission AI precheck activity.

### Example

```ts
import {
  Configuration,
  InternalApi,
} from '@wishpool/api-client';
import type { RunAiPrecheckWorkflowActivityRequest } from '@wishpool/api-client';

async function example() {
  console.log("🚀 Testing @wishpool/api-client SDK...");
  const config = new Configuration({ 
    // To configure API key authorization: internalToken
    apiKey: "YOUR API KEY",
  });
  const api = new InternalApi(config);

  const body = {
    // RunAiPrecheckWorkflowRequest
    runAiPrecheckWorkflowRequest: ...,
  } satisfies RunAiPrecheckWorkflowActivityRequest;

  try {
    const data = await api.runAiPrecheckWorkflowActivity(body);
    console.log(data);
  } catch (error) {
    console.error(error);
  }
}

// Run the test
example().catch(console.error);
```

### Parameters


| Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **runAiPrecheckWorkflowRequest** | [RunAiPrecheckWorkflowRequest](RunAiPrecheckWorkflowRequest.md) |  | |

### Return type

[**WorkflowAcceptedResponse**](WorkflowAcceptedResponse.md)

### Authorization

[internalToken](../README.md#internalToken)

### HTTP request headers

- **Content-Type**: `application/json`
- **Accept**: `application/json`, `application/problem+json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | AI precheck completed. |  -  |
| **0** | Error response. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## runMaterializeWeeklyPlanWorkflowActivity

> WeeklyPlan runMaterializeWeeklyPlanWorkflowActivity(materializeWeeklyPlanWorkflowRequest)

Execute weekly plan materialization activity.

### Example

```ts
import {
  Configuration,
  InternalApi,
} from '@wishpool/api-client';
import type { RunMaterializeWeeklyPlanWorkflowActivityRequest } from '@wishpool/api-client';

async function example() {
  console.log("🚀 Testing @wishpool/api-client SDK...");
  const config = new Configuration({ 
    // To configure API key authorization: internalToken
    apiKey: "YOUR API KEY",
  });
  const api = new InternalApi(config);

  const body = {
    // MaterializeWeeklyPlanWorkflowRequest
    materializeWeeklyPlanWorkflowRequest: ...,
  } satisfies RunMaterializeWeeklyPlanWorkflowActivityRequest;

  try {
    const data = await api.runMaterializeWeeklyPlanWorkflowActivity(body);
    console.log(data);
  } catch (error) {
    console.error(error);
  }
}

// Run the test
example().catch(console.error);
```

### Parameters


| Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **materializeWeeklyPlanWorkflowRequest** | [MaterializeWeeklyPlanWorkflowRequest](MaterializeWeeklyPlanWorkflowRequest.md) |  | |

### Return type

[**WeeklyPlan**](WeeklyPlan.md)

### Authorization

[internalToken](../README.md#internalToken)

### HTTP request headers

- **Content-Type**: `application/json`
- **Accept**: `application/json`, `application/problem+json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Weekly plan materialized. |  -  |
| **0** | Error response. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## runRewardEvaluationWorkflowActivity

> WorkflowAcceptedResponse runRewardEvaluationWorkflowActivity(evaluateRewardWorkflowRequest)

Execute reward evaluation activity.

### Example

```ts
import {
  Configuration,
  InternalApi,
} from '@wishpool/api-client';
import type { RunRewardEvaluationWorkflowActivityRequest } from '@wishpool/api-client';

async function example() {
  console.log("🚀 Testing @wishpool/api-client SDK...");
  const config = new Configuration({ 
    // To configure API key authorization: internalToken
    apiKey: "YOUR API KEY",
  });
  const api = new InternalApi(config);

  const body = {
    // EvaluateRewardWorkflowRequest
    evaluateRewardWorkflowRequest: ...,
  } satisfies RunRewardEvaluationWorkflowActivityRequest;

  try {
    const data = await api.runRewardEvaluationWorkflowActivity(body);
    console.log(data);
  } catch (error) {
    console.error(error);
  }
}

// Run the test
example().catch(console.error);
```

### Parameters


| Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **evaluateRewardWorkflowRequest** | [EvaluateRewardWorkflowRequest](EvaluateRewardWorkflowRequest.md) |  | |

### Return type

[**WorkflowAcceptedResponse**](WorkflowAcceptedResponse.md)

### Authorization

[internalToken](../README.md#internalToken)

### HTTP request headers

- **Content-Type**: `application/json`
- **Accept**: `application/json`, `application/problem+json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Reward evaluation completed. |  -  |
| **0** | Error response. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)

