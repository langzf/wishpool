# TasksApi

All URIs are relative to *http://localhost:8080*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**getToday**](TasksApi.md#gettoday) | **GET** /children/{childId}/today | Get child today dashboard data. |
| [**postponeTask**](TasksApi.md#postponetaskoperation) | **POST** /tasks/{taskId}/postpone | Postpone a task to another date. |
| [**skipTask**](TasksApi.md#skiptaskoperation) | **POST** /tasks/{taskId}/skip | Mark a task as skipped. |



## getToday

> TodaySnapshot getToday(childId, date)

Get child today dashboard data.

### Example

```ts
import {
  Configuration,
  TasksApi,
} from '@wishpool/api-client';
import type { GetTodayRequest } from '@wishpool/api-client';

async function example() {
  console.log("🚀 Testing @wishpool/api-client SDK...");
  const config = new Configuration({ 
    // Configure HTTP bearer authorization: bearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new TasksApi(config);

  const body = {
    // string
    childId: 38400000-8cf0-11bd-b23e-10b96e4ef00d,
    // Date (optional)
    date: 2013-10-20,
  } satisfies GetTodayRequest;

  try {
    const data = await api.getToday(body);
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
| **childId** | `string` |  | [Defaults to `undefined`] |
| **date** | `Date` |  | [Optional] [Defaults to `undefined`] |

### Return type

[**TodaySnapshot**](TodaySnapshot.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`, `application/problem+json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Today snapshot. |  -  |
| **0** | Error response. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## postponeTask

> TaskInstance postponeTask(taskId, postponeTaskRequest, idempotencyKey)

Postpone a task to another date.

### Example

```ts
import {
  Configuration,
  TasksApi,
} from '@wishpool/api-client';
import type { PostponeTaskOperationRequest } from '@wishpool/api-client';

async function example() {
  console.log("🚀 Testing @wishpool/api-client SDK...");
  const config = new Configuration({ 
    // Configure HTTP bearer authorization: bearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new TasksApi(config);

  const body = {
    // string
    taskId: 38400000-8cf0-11bd-b23e-10b96e4ef00d,
    // PostponeTaskRequest
    postponeTaskRequest: ...,
    // string (optional)
    idempotencyKey: idempotencyKey_example,
  } satisfies PostponeTaskOperationRequest;

  try {
    const data = await api.postponeTask(body);
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
| **taskId** | `string` |  | [Defaults to `undefined`] |
| **postponeTaskRequest** | [PostponeTaskRequest](PostponeTaskRequest.md) |  | |
| **idempotencyKey** | `string` |  | [Optional] [Defaults to `undefined`] |

### Return type

[**TaskInstance**](TaskInstance.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

- **Content-Type**: `application/json`
- **Accept**: `application/json`, `application/problem+json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Task postponed. |  -  |
| **0** | Error response. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## skipTask

> TaskInstance skipTask(taskId, skipTaskRequest, idempotencyKey)

Mark a task as skipped.

### Example

```ts
import {
  Configuration,
  TasksApi,
} from '@wishpool/api-client';
import type { SkipTaskOperationRequest } from '@wishpool/api-client';

async function example() {
  console.log("🚀 Testing @wishpool/api-client SDK...");
  const config = new Configuration({ 
    // Configure HTTP bearer authorization: bearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new TasksApi(config);

  const body = {
    // string
    taskId: 38400000-8cf0-11bd-b23e-10b96e4ef00d,
    // SkipTaskRequest
    skipTaskRequest: ...,
    // string (optional)
    idempotencyKey: idempotencyKey_example,
  } satisfies SkipTaskOperationRequest;

  try {
    const data = await api.skipTask(body);
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
| **taskId** | `string` |  | [Defaults to `undefined`] |
| **skipTaskRequest** | [SkipTaskRequest](SkipTaskRequest.md) |  | |
| **idempotencyKey** | `string` |  | [Optional] [Defaults to `undefined`] |

### Return type

[**TaskInstance**](TaskInstance.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

- **Content-Type**: `application/json`
- **Accept**: `application/json`, `application/problem+json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Task skipped. |  -  |
| **0** | Error response. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)

