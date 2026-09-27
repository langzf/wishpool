# RoomApi

All URIs are relative to *http://localhost:8080*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**arrangeRoomItem**](RoomApi.md#arrangeroomitemoperation) | **POST** /room/items/{itemId}/arrange | Arrange a room item. |
| [**getRoomState**](RoomApi.md#getroomstate) | **GET** /room/state | Get room state for a child. |
| [**hideRoomItem**](RoomApi.md#hideroomitem) | **POST** /room/items/{itemId}/hide | Hide a room item without deleting its history or position. |
| [**unhideRoomItem**](RoomApi.md#unhideroomitem) | **POST** /room/items/{itemId}/unhide | Restore a hidden room item without changing its position. |



## arrangeRoomItem

> RoomItem arrangeRoomItem(itemId, arrangeRoomItemRequest, idempotencyKey)

Arrange a room item.

### Example

```ts
import {
  Configuration,
  RoomApi,
} from '@wishpool/api-client';
import type { ArrangeRoomItemOperationRequest } from '@wishpool/api-client';

async function example() {
  console.log("🚀 Testing @wishpool/api-client SDK...");
  const config = new Configuration({ 
    // Configure HTTP bearer authorization: bearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new RoomApi(config);

  const body = {
    // string
    itemId: 38400000-8cf0-11bd-b23e-10b96e4ef00d,
    // ArrangeRoomItemRequest
    arrangeRoomItemRequest: ...,
    // string (optional)
    idempotencyKey: idempotencyKey_example,
  } satisfies ArrangeRoomItemOperationRequest;

  try {
    const data = await api.arrangeRoomItem(body);
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
| **itemId** | `string` |  | [Defaults to `undefined`] |
| **arrangeRoomItemRequest** | [ArrangeRoomItemRequest](ArrangeRoomItemRequest.md) |  | |
| **idempotencyKey** | `string` |  | [Optional] [Defaults to `undefined`] |

### Return type

[**RoomItem**](RoomItem.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

- **Content-Type**: `application/json`
- **Accept**: `application/json`, `application/problem+json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Room item arranged. |  -  |
| **0** | Error response. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## getRoomState

> RoomState getRoomState(childId)

Get room state for a child.

### Example

```ts
import {
  Configuration,
  RoomApi,
} from '@wishpool/api-client';
import type { GetRoomStateRequest } from '@wishpool/api-client';

async function example() {
  console.log("🚀 Testing @wishpool/api-client SDK...");
  const config = new Configuration({ 
    // Configure HTTP bearer authorization: bearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new RoomApi(config);

  const body = {
    // string
    childId: 38400000-8cf0-11bd-b23e-10b96e4ef00d,
  } satisfies GetRoomStateRequest;

  try {
    const data = await api.getRoomState(body);
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

### Return type

[**RoomState**](RoomState.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`, `application/problem+json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Room state. |  -  |
| **0** | Error response. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## hideRoomItem

> RoomItem hideRoomItem(itemId)

Hide a room item without deleting its history or position.

### Example

```ts
import {
  Configuration,
  RoomApi,
} from '@wishpool/api-client';
import type { HideRoomItemRequest } from '@wishpool/api-client';

async function example() {
  console.log("🚀 Testing @wishpool/api-client SDK...");
  const config = new Configuration({ 
    // Configure HTTP bearer authorization: bearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new RoomApi(config);

  const body = {
    // string
    itemId: 38400000-8cf0-11bd-b23e-10b96e4ef00d,
  } satisfies HideRoomItemRequest;

  try {
    const data = await api.hideRoomItem(body);
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
| **itemId** | `string` |  | [Defaults to `undefined`] |

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
| **200** | Room item hidden. |  -  |
| **0** | Error response. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## unhideRoomItem

> RoomItem unhideRoomItem(itemId)

Restore a hidden room item without changing its position.

### Example

```ts
import {
  Configuration,
  RoomApi,
} from '@wishpool/api-client';
import type { UnhideRoomItemRequest } from '@wishpool/api-client';

async function example() {
  console.log("🚀 Testing @wishpool/api-client SDK...");
  const config = new Configuration({ 
    // Configure HTTP bearer authorization: bearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new RoomApi(config);

  const body = {
    // string
    itemId: 38400000-8cf0-11bd-b23e-10b96e4ef00d,
  } satisfies UnhideRoomItemRequest;

  try {
    const data = await api.unhideRoomItem(body);
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
| **itemId** | `string` |  | [Defaults to `undefined`] |

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
| **200** | Room item visible. |  -  |
| **0** | Error response. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)

