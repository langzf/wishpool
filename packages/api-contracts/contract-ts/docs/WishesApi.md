# WishesApi

All URIs are relative to *http://localhost:8080*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**activateWish**](WishesApi.md#activatewish) | **POST** /wishes/{wishId}/activate | Activate a wish. |
| [**attachWishImage**](WishesApi.md#attachwishimageoperation) | **POST** /wishes/{wishId}/image | Attach a finalized wish image to a wish. |
| [**createWish**](WishesApi.md#createwishoperation) | **POST** /wishes | Create a wish. |
| [**createWishImageGeneration**](WishesApi.md#createwishimagegenerationoperation) | **POST** /wishes/image-generations | Create a wish image generation job. |
| [**findWishImageCandidates**](WishesApi.md#findwishimagecandidates) | **POST** /wishes/image-candidates | Find reusable wish image candidates for a new wish. |
| [**getCurrentWish**](WishesApi.md#getcurrentwish) | **GET** /children/{childId}/wishes/current | Get the current wish for a child. |
| [**getWish**](WishesApi.md#getwish) | **GET** /wishes/{wishId} | Get a wish. |
| [**getWishImageGeneration**](WishesApi.md#getwishimagegeneration) | **GET** /wishes/image-generations/{jobId} | Get a wish image generation job. |
| [**listChildWishHistory**](WishesApi.md#listchildwishhistory) | **GET** /children/{childId}/wishes/history | List wish history items for a child. |
| [**listChildWishes**](WishesApi.md#listchildwishes) | **GET** /children/{childId}/wishes | List wishes for a child. |
| [**listWishImageModelProviders**](WishesApi.md#listwishimagemodelproviders) | **GET** /wishes/image-model-providers | List enabled image generation model providers for a business usage. |
| [**redeemWish**](WishesApi.md#redeemwishoperation) | **POST** /wishes/{wishId}/redeem | Redeem an unlocked wish. |



## activateWish

> Wish activateWish(wishId, idempotencyKey)

Activate a wish.

### Example

```ts
import {
  Configuration,
  WishesApi,
} from '@wishpool/api-client';
import type { ActivateWishRequest } from '@wishpool/api-client';

async function example() {
  console.log("🚀 Testing @wishpool/api-client SDK...");
  const config = new Configuration({ 
    // Configure HTTP bearer authorization: bearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new WishesApi(config);

  const body = {
    // string
    wishId: 38400000-8cf0-11bd-b23e-10b96e4ef00d,
    // string (optional)
    idempotencyKey: idempotencyKey_example,
  } satisfies ActivateWishRequest;

  try {
    const data = await api.activateWish(body);
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
| **wishId** | `string` |  | [Defaults to `undefined`] |
| **idempotencyKey** | `string` |  | [Optional] [Defaults to `undefined`] |

### Return type

[**Wish**](Wish.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`, `application/problem+json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Wish activated. |  -  |
| **0** | Error response. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## attachWishImage

> Wish attachWishImage(wishId, attachWishImageRequest)

Attach a finalized wish image to a wish.

### Example

```ts
import {
  Configuration,
  WishesApi,
} from '@wishpool/api-client';
import type { AttachWishImageOperationRequest } from '@wishpool/api-client';

async function example() {
  console.log("🚀 Testing @wishpool/api-client SDK...");
  const config = new Configuration({ 
    // Configure HTTP bearer authorization: bearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new WishesApi(config);

  const body = {
    // string
    wishId: 38400000-8cf0-11bd-b23e-10b96e4ef00d,
    // AttachWishImageRequest
    attachWishImageRequest: ...,
  } satisfies AttachWishImageOperationRequest;

  try {
    const data = await api.attachWishImage(body);
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
| **wishId** | `string` |  | [Defaults to `undefined`] |
| **attachWishImageRequest** | [AttachWishImageRequest](AttachWishImageRequest.md) |  | |

### Return type

[**Wish**](Wish.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

- **Content-Type**: `application/json`
- **Accept**: `application/json`, `application/problem+json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Wish with the attached image. |  -  |
| **0** | Error response. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## createWish

> Wish createWish(createWishRequest, idempotencyKey)

Create a wish.

### Example

```ts
import {
  Configuration,
  WishesApi,
} from '@wishpool/api-client';
import type { CreateWishOperationRequest } from '@wishpool/api-client';

async function example() {
  console.log("🚀 Testing @wishpool/api-client SDK...");
  const config = new Configuration({ 
    // Configure HTTP bearer authorization: bearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new WishesApi(config);

  const body = {
    // CreateWishRequest
    createWishRequest: ...,
    // string (optional)
    idempotencyKey: idempotencyKey_example,
  } satisfies CreateWishOperationRequest;

  try {
    const data = await api.createWish(body);
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
| **createWishRequest** | [CreateWishRequest](CreateWishRequest.md) |  | |
| **idempotencyKey** | `string` |  | [Optional] [Defaults to `undefined`] |

### Return type

[**Wish**](Wish.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

- **Content-Type**: `application/json`
- **Accept**: `application/json`, `application/problem+json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **201** | Wish created. |  -  |
| **0** | Error response. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## createWishImageGeneration

> WishImageGenerationJob createWishImageGeneration(createWishImageGenerationRequest)

Create a wish image generation job.

### Example

```ts
import {
  Configuration,
  WishesApi,
} from '@wishpool/api-client';
import type { CreateWishImageGenerationOperationRequest } from '@wishpool/api-client';

async function example() {
  console.log("🚀 Testing @wishpool/api-client SDK...");
  const config = new Configuration({ 
    // Configure HTTP bearer authorization: bearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new WishesApi(config);

  const body = {
    // CreateWishImageGenerationRequest
    createWishImageGenerationRequest: ...,
  } satisfies CreateWishImageGenerationOperationRequest;

  try {
    const data = await api.createWishImageGeneration(body);
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
| **createWishImageGenerationRequest** | [CreateWishImageGenerationRequest](CreateWishImageGenerationRequest.md) |  | |

### Return type

[**WishImageGenerationJob**](WishImageGenerationJob.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

- **Content-Type**: `application/json`
- **Accept**: `application/json`, `application/problem+json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **201** | Wish image generation job. |  -  |
| **0** | Error response. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## findWishImageCandidates

> WishImageCandidateResponse findWishImageCandidates(wishImageCandidateRequest)

Find reusable wish image candidates for a new wish.

### Example

```ts
import {
  Configuration,
  WishesApi,
} from '@wishpool/api-client';
import type { FindWishImageCandidatesRequest } from '@wishpool/api-client';

async function example() {
  console.log("🚀 Testing @wishpool/api-client SDK...");
  const config = new Configuration({ 
    // Configure HTTP bearer authorization: bearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new WishesApi(config);

  const body = {
    // WishImageCandidateRequest
    wishImageCandidateRequest: ...,
  } satisfies FindWishImageCandidatesRequest;

  try {
    const data = await api.findWishImageCandidates(body);
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
| **wishImageCandidateRequest** | [WishImageCandidateRequest](WishImageCandidateRequest.md) |  | |

### Return type

[**WishImageCandidateResponse**](WishImageCandidateResponse.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

- **Content-Type**: `application/json`
- **Accept**: `application/json`, `application/problem+json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Reusable wish image candidates. |  -  |
| **0** | Error response. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## getCurrentWish

> Wish getCurrentWish(childId)

Get the current wish for a child.

### Example

```ts
import {
  Configuration,
  WishesApi,
} from '@wishpool/api-client';
import type { GetCurrentWishRequest } from '@wishpool/api-client';

async function example() {
  console.log("🚀 Testing @wishpool/api-client SDK...");
  const config = new Configuration({ 
    // Configure HTTP bearer authorization: bearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new WishesApi(config);

  const body = {
    // string
    childId: 38400000-8cf0-11bd-b23e-10b96e4ef00d,
  } satisfies GetCurrentWishRequest;

  try {
    const data = await api.getCurrentWish(body);
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

[**Wish**](Wish.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`, `application/problem+json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Current wish. |  -  |
| **0** | Error response. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## getWish

> Wish getWish(wishId)

Get a wish.

### Example

```ts
import {
  Configuration,
  WishesApi,
} from '@wishpool/api-client';
import type { GetWishRequest } from '@wishpool/api-client';

async function example() {
  console.log("🚀 Testing @wishpool/api-client SDK...");
  const config = new Configuration({ 
    // Configure HTTP bearer authorization: bearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new WishesApi(config);

  const body = {
    // string
    wishId: 38400000-8cf0-11bd-b23e-10b96e4ef00d,
  } satisfies GetWishRequest;

  try {
    const data = await api.getWish(body);
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
| **wishId** | `string` |  | [Defaults to `undefined`] |

### Return type

[**Wish**](Wish.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`, `application/problem+json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Wish. |  -  |
| **0** | Error response. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## getWishImageGeneration

> WishImageGenerationJob getWishImageGeneration(jobId)

Get a wish image generation job.

### Example

```ts
import {
  Configuration,
  WishesApi,
} from '@wishpool/api-client';
import type { GetWishImageGenerationRequest } from '@wishpool/api-client';

async function example() {
  console.log("🚀 Testing @wishpool/api-client SDK...");
  const config = new Configuration({ 
    // Configure HTTP bearer authorization: bearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new WishesApi(config);

  const body = {
    // string
    jobId: 38400000-8cf0-11bd-b23e-10b96e4ef00d,
  } satisfies GetWishImageGenerationRequest;

  try {
    const data = await api.getWishImageGeneration(body);
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
| **jobId** | `string` |  | [Defaults to `undefined`] |

### Return type

[**WishImageGenerationJob**](WishImageGenerationJob.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`, `application/problem+json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Wish image generation job. |  -  |
| **0** | Error response. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## listChildWishHistory

> Array&lt;WishHistoryItem&gt; listChildWishHistory(childId)

List wish history items for a child.

### Example

```ts
import {
  Configuration,
  WishesApi,
} from '@wishpool/api-client';
import type { ListChildWishHistoryRequest } from '@wishpool/api-client';

async function example() {
  console.log("🚀 Testing @wishpool/api-client SDK...");
  const config = new Configuration({ 
    // Configure HTTP bearer authorization: bearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new WishesApi(config);

  const body = {
    // string
    childId: 38400000-8cf0-11bd-b23e-10b96e4ef00d,
  } satisfies ListChildWishHistoryRequest;

  try {
    const data = await api.listChildWishHistory(body);
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

[**Array&lt;WishHistoryItem&gt;**](WishHistoryItem.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`, `application/problem+json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Child wish history. |  -  |
| **0** | Error response. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## listChildWishes

> Array&lt;Wish&gt; listChildWishes(childId, weekId)

List wishes for a child.

### Example

```ts
import {
  Configuration,
  WishesApi,
} from '@wishpool/api-client';
import type { ListChildWishesRequest } from '@wishpool/api-client';

async function example() {
  console.log("🚀 Testing @wishpool/api-client SDK...");
  const config = new Configuration({ 
    // Configure HTTP bearer authorization: bearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new WishesApi(config);

  const body = {
    // string
    childId: 38400000-8cf0-11bd-b23e-10b96e4ef00d,
    // string (optional)
    weekId: weekId_example,
  } satisfies ListChildWishesRequest;

  try {
    const data = await api.listChildWishes(body);
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
| **weekId** | `string` |  | [Optional] [Defaults to `undefined`] |

### Return type

[**Array&lt;Wish&gt;**](Wish.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`, `application/problem+json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Child wishes. |  -  |
| **0** | Error response. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## listWishImageModelProviders

> BusinessImageModelProviderList listWishImageModelProviders(usageCode)

List enabled image generation model providers for a business usage.

### Example

```ts
import {
  Configuration,
  WishesApi,
} from '@wishpool/api-client';
import type { ListWishImageModelProvidersRequest } from '@wishpool/api-client';

async function example() {
  console.log("🚀 Testing @wishpool/api-client SDK...");
  const config = new Configuration({ 
    // Configure HTTP bearer authorization: bearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new WishesApi(config);

  const body = {
    // string (optional)
    usageCode: usageCode_example,
  } satisfies ListWishImageModelProvidersRequest;

  try {
    const data = await api.listWishImageModelProviders(body);
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
| **usageCode** | `string` |  | [Optional] [Defaults to `&#39;wish_card&#39;`] |

### Return type

[**BusinessImageModelProviderList**](BusinessImageModelProviderList.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`, `application/problem+json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Enabled image model providers with the selected default. |  -  |
| **0** | Error response. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## redeemWish

> WishRedemption redeemWish(wishId, redeemWishRequest, idempotencyKey)

Redeem an unlocked wish.

### Example

```ts
import {
  Configuration,
  WishesApi,
} from '@wishpool/api-client';
import type { RedeemWishOperationRequest } from '@wishpool/api-client';

async function example() {
  console.log("🚀 Testing @wishpool/api-client SDK...");
  const config = new Configuration({ 
    // Configure HTTP bearer authorization: bearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new WishesApi(config);

  const body = {
    // string
    wishId: 38400000-8cf0-11bd-b23e-10b96e4ef00d,
    // RedeemWishRequest
    redeemWishRequest: ...,
    // string (optional)
    idempotencyKey: idempotencyKey_example,
  } satisfies RedeemWishOperationRequest;

  try {
    const data = await api.redeemWish(body);
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
| **wishId** | `string` |  | [Defaults to `undefined`] |
| **redeemWishRequest** | [RedeemWishRequest](RedeemWishRequest.md) |  | |
| **idempotencyKey** | `string` |  | [Optional] [Defaults to `undefined`] |

### Return type

[**WishRedemption**](WishRedemption.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

- **Content-Type**: `application/json`
- **Accept**: `application/json`, `application/problem+json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Wish redeemed. |  -  |
| **0** | Error response. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)

