# NotificationsApi

All URIs are relative to *http://localhost:8080*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**listNotificationPreferences**](NotificationsApi.md#listnotificationpreferences) | **GET** /notification-preferences | List notification preferences for the current user. |
| [**listNotifications**](NotificationsApi.md#listnotifications) | **GET** /notifications | List the current user\&#39;s inbox notifications. |
| [**markNotificationsRead**](NotificationsApi.md#marknotificationsread) | **POST** /notifications/read | Mark inbox notifications as read. |
| [**registerPushToken**](NotificationsApi.md#registerpushtokenoperation) | **POST** /devices/push-token | Register or update a push token for the current device. |
| [**updateNotificationPreference**](NotificationsApi.md#updatenotificationpreferenceoperation) | **PUT** /notification-preferences | Upsert one notification preference for the current user. |



## listNotificationPreferences

> Array&lt;NotificationPreference&gt; listNotificationPreferences(familyId)

List notification preferences for the current user.

### Example

```ts
import {
  Configuration,
  NotificationsApi,
} from '@wishpool/api-client';
import type { ListNotificationPreferencesRequest } from '@wishpool/api-client';

async function example() {
  console.log("🚀 Testing @wishpool/api-client SDK...");
  const config = new Configuration({ 
    // Configure HTTP bearer authorization: bearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new NotificationsApi(config);

  const body = {
    // string
    familyId: 38400000-8cf0-11bd-b23e-10b96e4ef00d,
  } satisfies ListNotificationPreferencesRequest;

  try {
    const data = await api.listNotificationPreferences(body);
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
| **familyId** | `string` |  | [Defaults to `undefined`] |

### Return type

[**Array&lt;NotificationPreference&gt;**](NotificationPreference.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`, `application/problem+json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Notification preferences. |  -  |
| **0** | Error response. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## listNotifications

> NotificationListResponse listNotifications(familyId, status, limit)

List the current user\&#39;s inbox notifications.

### Example

```ts
import {
  Configuration,
  NotificationsApi,
} from '@wishpool/api-client';
import type { ListNotificationsRequest } from '@wishpool/api-client';

async function example() {
  console.log("🚀 Testing @wishpool/api-client SDK...");
  const config = new Configuration({ 
    // Configure HTTP bearer authorization: bearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new NotificationsApi(config);

  const body = {
    // string
    familyId: 38400000-8cf0-11bd-b23e-10b96e4ef00d,
    // 'pending' | 'sent' | 'failed' | 'read' (optional)
    status: status_example,
    // number (optional)
    limit: 56,
  } satisfies ListNotificationsRequest;

  try {
    const data = await api.listNotifications(body);
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
| **familyId** | `string` |  | [Defaults to `undefined`] |
| **status** | `pending`, `sent`, `failed`, `read` |  | [Optional] [Defaults to `undefined`] [Enum: pending, sent, failed, read] |
| **limit** | `number` |  | [Optional] [Defaults to `50`] |

### Return type

[**NotificationListResponse**](NotificationListResponse.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`, `application/problem+json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Inbox notifications. |  -  |
| **0** | Error response. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## markNotificationsRead

> NotificationListResponse markNotificationsRead(markNotificationReadRequest)

Mark inbox notifications as read.

### Example

```ts
import {
  Configuration,
  NotificationsApi,
} from '@wishpool/api-client';
import type { MarkNotificationsReadRequest } from '@wishpool/api-client';

async function example() {
  console.log("🚀 Testing @wishpool/api-client SDK...");
  const config = new Configuration({ 
    // Configure HTTP bearer authorization: bearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new NotificationsApi(config);

  const body = {
    // MarkNotificationReadRequest
    markNotificationReadRequest: ...,
  } satisfies MarkNotificationsReadRequest;

  try {
    const data = await api.markNotificationsRead(body);
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
| **markNotificationReadRequest** | [MarkNotificationReadRequest](MarkNotificationReadRequest.md) |  | |

### Return type

[**NotificationListResponse**](NotificationListResponse.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

- **Content-Type**: `application/json`
- **Accept**: `application/json`, `application/problem+json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Updated notifications. |  -  |
| **0** | Error response. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## registerPushToken

> NotificationDevice registerPushToken(registerPushTokenRequest)

Register or update a push token for the current device.

### Example

```ts
import {
  Configuration,
  NotificationsApi,
} from '@wishpool/api-client';
import type { RegisterPushTokenOperationRequest } from '@wishpool/api-client';

async function example() {
  console.log("🚀 Testing @wishpool/api-client SDK...");
  const config = new Configuration({ 
    // Configure HTTP bearer authorization: bearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new NotificationsApi(config);

  const body = {
    // RegisterPushTokenRequest
    registerPushTokenRequest: ...,
  } satisfies RegisterPushTokenOperationRequest;

  try {
    const data = await api.registerPushToken(body);
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
| **registerPushTokenRequest** | [RegisterPushTokenRequest](RegisterPushTokenRequest.md) |  | |

### Return type

[**NotificationDevice**](NotificationDevice.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

- **Content-Type**: `application/json`
- **Accept**: `application/json`, `application/problem+json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Registered push-capable device. |  -  |
| **0** | Error response. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## updateNotificationPreference

> NotificationPreference updateNotificationPreference(updateNotificationPreferenceRequest)

Upsert one notification preference for the current user.

### Example

```ts
import {
  Configuration,
  NotificationsApi,
} from '@wishpool/api-client';
import type { UpdateNotificationPreferenceOperationRequest } from '@wishpool/api-client';

async function example() {
  console.log("🚀 Testing @wishpool/api-client SDK...");
  const config = new Configuration({ 
    // Configure HTTP bearer authorization: bearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new NotificationsApi(config);

  const body = {
    // UpdateNotificationPreferenceRequest
    updateNotificationPreferenceRequest: ...,
  } satisfies UpdateNotificationPreferenceOperationRequest;

  try {
    const data = await api.updateNotificationPreference(body);
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
| **updateNotificationPreferenceRequest** | [UpdateNotificationPreferenceRequest](UpdateNotificationPreferenceRequest.md) |  | |

### Return type

[**NotificationPreference**](NotificationPreference.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

- **Content-Type**: `application/json`
- **Accept**: `application/json`, `application/problem+json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Notification preference. |  -  |
| **0** | Error response. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)

