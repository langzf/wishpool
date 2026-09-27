# ChildrenApi

All URIs are relative to *http://localhost:8080*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**createChild**](ChildrenApi.md#createchildoperation) | **POST** /families/{familyId}/children | Create a child profile. |
| [**getChildRewardSummary**](ChildrenApi.md#getchildrewardsummary) | **GET** /children/{childId}/rewards/summary | Get read-only reward totals for a child. |
| [**listChildren**](ChildrenApi.md#listchildren) | **GET** /families/{familyId}/children | List children in a family. |
| [**updateChild**](ChildrenApi.md#updatechildoperation) | **PATCH** /children/{childId} | Update child profile. |



## createChild

> ChildProfile createChild(familyId, createChildRequest, idempotencyKey)

Create a child profile.

### Example

```ts
import {
  Configuration,
  ChildrenApi,
} from '@wishpool/api-client';
import type { CreateChildOperationRequest } from '@wishpool/api-client';

async function example() {
  console.log("🚀 Testing @wishpool/api-client SDK...");
  const config = new Configuration({ 
    // Configure HTTP bearer authorization: bearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new ChildrenApi(config);

  const body = {
    // string
    familyId: 38400000-8cf0-11bd-b23e-10b96e4ef00d,
    // CreateChildRequest
    createChildRequest: ...,
    // string (optional)
    idempotencyKey: idempotencyKey_example,
  } satisfies CreateChildOperationRequest;

  try {
    const data = await api.createChild(body);
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
| **createChildRequest** | [CreateChildRequest](CreateChildRequest.md) |  | |
| **idempotencyKey** | `string` |  | [Optional] [Defaults to `undefined`] |

### Return type

[**ChildProfile**](ChildProfile.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

- **Content-Type**: `application/json`
- **Accept**: `application/json`, `application/problem+json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **201** | Child profile created. |  -  |
| **0** | Error response. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## getChildRewardSummary

> RewardSummary getChildRewardSummary(childId, weekId, fromDate, toDate)

Get read-only reward totals for a child.

### Example

```ts
import {
  Configuration,
  ChildrenApi,
} from '@wishpool/api-client';
import type { GetChildRewardSummaryRequest } from '@wishpool/api-client';

async function example() {
  console.log("🚀 Testing @wishpool/api-client SDK...");
  const config = new Configuration({ 
    // Configure HTTP bearer authorization: bearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new ChildrenApi(config);

  const body = {
    // string
    childId: 38400000-8cf0-11bd-b23e-10b96e4ef00d,
    // string | ISO week identifier such as 2026-W34. (optional)
    weekId: weekId_example,
    // Date | Inclusive lower bound for reward date. (optional)
    fromDate: 2013-10-20,
    // Date | Inclusive upper bound for reward date. (optional)
    toDate: 2013-10-20,
  } satisfies GetChildRewardSummaryRequest;

  try {
    const data = await api.getChildRewardSummary(body);
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
| **weekId** | `string` | ISO week identifier such as 2026-W34. | [Optional] [Defaults to `undefined`] |
| **fromDate** | `Date` | Inclusive lower bound for reward date. | [Optional] [Defaults to `undefined`] |
| **toDate** | `Date` | Inclusive upper bound for reward date. | [Optional] [Defaults to `undefined`] |

### Return type

[**RewardSummary**](RewardSummary.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`, `application/problem+json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Reward ledger totals for the selected child and filters. |  -  |
| **0** | Error response. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## listChildren

> Array&lt;ChildProfile&gt; listChildren(familyId)

List children in a family.

### Example

```ts
import {
  Configuration,
  ChildrenApi,
} from '@wishpool/api-client';
import type { ListChildrenRequest } from '@wishpool/api-client';

async function example() {
  console.log("🚀 Testing @wishpool/api-client SDK...");
  const config = new Configuration({ 
    // Configure HTTP bearer authorization: bearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new ChildrenApi(config);

  const body = {
    // string
    familyId: 38400000-8cf0-11bd-b23e-10b96e4ef00d,
  } satisfies ListChildrenRequest;

  try {
    const data = await api.listChildren(body);
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

[**Array&lt;ChildProfile&gt;**](ChildProfile.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`, `application/problem+json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Children. |  -  |
| **0** | Error response. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## updateChild

> ChildProfile updateChild(childId, updateChildRequest)

Update child profile.

### Example

```ts
import {
  Configuration,
  ChildrenApi,
} from '@wishpool/api-client';
import type { UpdateChildOperationRequest } from '@wishpool/api-client';

async function example() {
  console.log("🚀 Testing @wishpool/api-client SDK...");
  const config = new Configuration({ 
    // Configure HTTP bearer authorization: bearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new ChildrenApi(config);

  const body = {
    // string
    childId: 38400000-8cf0-11bd-b23e-10b96e4ef00d,
    // UpdateChildRequest
    updateChildRequest: ...,
  } satisfies UpdateChildOperationRequest;

  try {
    const data = await api.updateChild(body);
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
| **updateChildRequest** | [UpdateChildRequest](UpdateChildRequest.md) |  | |

### Return type

[**ChildProfile**](ChildProfile.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

- **Content-Type**: `application/json`
- **Accept**: `application/json`, `application/problem+json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Child profile updated. |  -  |
| **0** | Error response. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)

