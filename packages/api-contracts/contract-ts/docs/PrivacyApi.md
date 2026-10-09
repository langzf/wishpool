# PrivacyApi

All URIs are relative to *http://localhost:8080*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**listPrivacyRequests**](PrivacyApi.md#listprivacyrequests) | **GET** /families/{familyId}/privacy-requests | List family privacy requests and export download links. |
| [**requestDataExport**](PrivacyApi.md#requestdataexport) | **POST** /privacy/export | Request family data export. |
| [**requestFamilyDeletion**](PrivacyApi.md#requestfamilydeletion) | **POST** /privacy/delete | Request family deletion. |



## listPrivacyRequests

> PrivacyRequestList listPrivacyRequests(familyId)

List family privacy requests and export download links.

### Example

```ts
import {
  Configuration,
  PrivacyApi,
} from '@wishpool/api-client';
import type { ListPrivacyRequestsRequest } from '@wishpool/api-client';

async function example() {
  console.log("🚀 Testing @wishpool/api-client SDK...");
  const config = new Configuration({ 
    // Configure HTTP bearer authorization: bearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new PrivacyApi(config);

  const body = {
    // string
    familyId: 38400000-8cf0-11bd-b23e-10b96e4ef00d,
  } satisfies ListPrivacyRequestsRequest;

  try {
    const data = await api.listPrivacyRequests(body);
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

[**PrivacyRequestList**](PrivacyRequestList.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`, `application/problem+json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Privacy requests. |  -  |
| **0** | Error response. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## requestDataExport

> PrivacyRequest requestDataExport(privacyRequestCreate, idempotencyKey)

Request family data export.

### Example

```ts
import {
  Configuration,
  PrivacyApi,
} from '@wishpool/api-client';
import type { RequestDataExportRequest } from '@wishpool/api-client';

async function example() {
  console.log("🚀 Testing @wishpool/api-client SDK...");
  const config = new Configuration({ 
    // Configure HTTP bearer authorization: bearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new PrivacyApi(config);

  const body = {
    // PrivacyRequestCreate
    privacyRequestCreate: ...,
    // string (optional)
    idempotencyKey: idempotencyKey_example,
  } satisfies RequestDataExportRequest;

  try {
    const data = await api.requestDataExport(body);
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
| **privacyRequestCreate** | [PrivacyRequestCreate](PrivacyRequestCreate.md) |  | |
| **idempotencyKey** | `string` |  | [Optional] [Defaults to `undefined`] |

### Return type

[**PrivacyRequest**](PrivacyRequest.md)

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


## requestFamilyDeletion

> PrivacyRequest requestFamilyDeletion(privacyRequestCreate, idempotencyKey)

Request family deletion.

### Example

```ts
import {
  Configuration,
  PrivacyApi,
} from '@wishpool/api-client';
import type { RequestFamilyDeletionRequest } from '@wishpool/api-client';

async function example() {
  console.log("🚀 Testing @wishpool/api-client SDK...");
  const config = new Configuration({ 
    // Configure HTTP bearer authorization: bearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new PrivacyApi(config);

  const body = {
    // PrivacyRequestCreate
    privacyRequestCreate: ...,
    // string (optional)
    idempotencyKey: idempotencyKey_example,
  } satisfies RequestFamilyDeletionRequest;

  try {
    const data = await api.requestFamilyDeletion(body);
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
| **privacyRequestCreate** | [PrivacyRequestCreate](PrivacyRequestCreate.md) |  | |
| **idempotencyKey** | `string` |  | [Optional] [Defaults to `undefined`] |

### Return type

[**PrivacyRequest**](PrivacyRequest.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

- **Content-Type**: `application/json`
- **Accept**: `application/json`, `application/problem+json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **202** | Deletion requested. |  -  |
| **0** | Error response. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)

