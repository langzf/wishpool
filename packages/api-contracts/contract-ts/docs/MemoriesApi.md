# MemoriesApi

All URIs are relative to *http://localhost:8080*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**exportMemory**](MemoriesApi.md#exportmemoryoperation) | **POST** /memories/{memoryId}/export | Request memory export. |
| [**featureMemory**](MemoriesApi.md#featurememory) | **POST** /memories/{memoryId}/feature | Feature a memory into the child\&#39;s room (idempotent) |
| [**getMemory**](MemoriesApi.md#getmemory) | **GET** /memories/{memoryId} | Get a memory detail. |
| [**listMemories**](MemoriesApi.md#listmemories) | **GET** /memories | List memories for a child. |



## exportMemory

> MemoryExport exportMemory(memoryId, exportMemoryRequest, idempotencyKey)

Request memory export.

### Example

```ts
import {
  Configuration,
  MemoriesApi,
} from '@wishpool/api-client';
import type { ExportMemoryOperationRequest } from '@wishpool/api-client';

async function example() {
  console.log("🚀 Testing @wishpool/api-client SDK...");
  const config = new Configuration({ 
    // Configure HTTP bearer authorization: bearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new MemoriesApi(config);

  const body = {
    // string
    memoryId: 38400000-8cf0-11bd-b23e-10b96e4ef00d,
    // ExportMemoryRequest
    exportMemoryRequest: ...,
    // string (optional)
    idempotencyKey: idempotencyKey_example,
  } satisfies ExportMemoryOperationRequest;

  try {
    const data = await api.exportMemory(body);
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
| **memoryId** | `string` |  | [Defaults to `undefined`] |
| **exportMemoryRequest** | [ExportMemoryRequest](ExportMemoryRequest.md) |  | |
| **idempotencyKey** | `string` |  | [Optional] [Defaults to `undefined`] |

### Return type

[**MemoryExport**](MemoryExport.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

- **Content-Type**: `application/json`
- **Accept**: `application/json`, `application/problem+json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **202** | Export requested. |  -  |
| **0** | Error response. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## featureMemory

> RoomItem featureMemory(memoryId)

Feature a memory into the child\&#39;s room (idempotent)

### Example

```ts
import {
  Configuration,
  MemoriesApi,
} from '@wishpool/api-client';
import type { FeatureMemoryRequest } from '@wishpool/api-client';

async function example() {
  console.log("🚀 Testing @wishpool/api-client SDK...");
  const config = new Configuration({ 
    // Configure HTTP bearer authorization: bearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new MemoriesApi(config);

  const body = {
    // string
    memoryId: 38400000-8cf0-11bd-b23e-10b96e4ef00d,
  } satisfies FeatureMemoryRequest;

  try {
    const data = await api.featureMemory(body);
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
| **memoryId** | `string` |  | [Defaults to `undefined`] |

### Return type

[**RoomItem**](RoomItem.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`, `application/problem+json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Room item containing the featured memory. |  -  |
| **0** | Error response. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## getMemory

> WeeklyMemory getMemory(memoryId)

Get a memory detail.

### Example

```ts
import {
  Configuration,
  MemoriesApi,
} from '@wishpool/api-client';
import type { GetMemoryRequest } from '@wishpool/api-client';

async function example() {
  console.log("🚀 Testing @wishpool/api-client SDK...");
  const config = new Configuration({ 
    // Configure HTTP bearer authorization: bearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new MemoriesApi(config);

  const body = {
    // string
    memoryId: 38400000-8cf0-11bd-b23e-10b96e4ef00d,
  } satisfies GetMemoryRequest;

  try {
    const data = await api.getMemory(body);
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
| **memoryId** | `string` |  | [Defaults to `undefined`] |

### Return type

[**WeeklyMemory**](WeeklyMemory.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`, `application/problem+json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Weekly memory. |  -  |
| **0** | Error response. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## listMemories

> MemoryTimeline listMemories(childId, cursor)

List memories for a child.

### Example

```ts
import {
  Configuration,
  MemoriesApi,
} from '@wishpool/api-client';
import type { ListMemoriesRequest } from '@wishpool/api-client';

async function example() {
  console.log("🚀 Testing @wishpool/api-client SDK...");
  const config = new Configuration({ 
    // Configure HTTP bearer authorization: bearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new MemoriesApi(config);

  const body = {
    // string
    childId: 38400000-8cf0-11bd-b23e-10b96e4ef00d,
    // string (optional)
    cursor: cursor_example,
  } satisfies ListMemoriesRequest;

  try {
    const data = await api.listMemories(body);
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
| **cursor** | `string` |  | [Optional] [Defaults to `undefined`] |

### Return type

[**MemoryTimeline**](MemoryTimeline.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`, `application/problem+json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Memory timeline. |  -  |
| **0** | Error response. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)

