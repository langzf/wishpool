# AdminApi

All URIs are relative to *http://localhost:8080*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**createAdminAiTextModelProvider**](AdminApi.md#createadminaitextmodelprovider) | **POST** /internal/admin/ai-text-model-providers |  |
| [**createAdminImageModelProvider**](AdminApi.md#createadminimagemodelprovider) | **POST** /internal/admin/image-model-providers | Create an image generation model provider. |
| [**deleteAdminAiTextModelProvider**](AdminApi.md#deleteadminaitextmodelprovider) | **DELETE** /internal/admin/ai-text-model-providers/{id} |  |
| [**deleteAdminImageModelProvider**](AdminApi.md#deleteadminimagemodelprovider) | **DELETE** /internal/admin/image-model-providers/{id} | Delete an image generation model provider. |
| [**getAdminDashboard**](AdminApi.md#getadmindashboard) | **GET** /internal/admin/dashboard | Get local administration dashboard counters. |
| [**grantAdminMediaAccess**](AdminApi.md#grantadminmediaaccess) | **POST** /internal/admin/media-access-grants | Create an audited short-lived media access grant. |
| [**listAdminAiTextModelProviders**](AdminApi.md#listadminaitextmodelproviders) | **GET** /internal/admin/ai-text-model-providers |  |
| [**listAdminAuditLogs**](AdminApi.md#listadminauditlogs) | **GET** /internal/admin/audit-logs | List audit logs for administration. |
| [**listAdminFamilies**](AdminApi.md#listadminfamilies) | **GET** /internal/admin/families | List family metadata for administration. |
| [**listAdminImageGenUsages**](AdminApi.md#listadminimagegenusages) | **GET** /internal/admin/image-gen-usages | List business usage to image provider mappings. |
| [**listAdminImageModelProviders**](AdminApi.md#listadminimagemodelproviders) | **GET** /internal/admin/image-model-providers | List image generation model provider configurations with masked API keys. |
| [**listAdminMediaAccessGrants**](AdminApi.md#listadminmediaaccessgrants) | **GET** /internal/admin/media-access-grants |  |
| [**listAdminPrivacyRequests**](AdminApi.md#listadminprivacyrequests) | **GET** /internal/admin/privacy-requests | List privacy requests for administration. |
| [**revokeAdminMediaAccessGrant**](AdminApi.md#revokeadminmediaaccessgrantoperation) | **POST** /internal/admin/media-access-grants/{grantId}/revoke |  |
| [**setDefaultAdminAiTextModelProvider**](AdminApi.md#setdefaultadminaitextmodelprovider) | **POST** /internal/admin/ai-text-model-providers/{id}/set-default |  |
| [**setDefaultAdminImageModelProvider**](AdminApi.md#setdefaultadminimagemodelprovider) | **POST** /internal/admin/image-model-providers/{id}/set-default | Set the global default image generation model provider. |
| [**testAdminAiTextModelProvider**](AdminApi.md#testadminaitextmodelprovider) | **POST** /internal/admin/ai-text-model-providers/{id}/test-connection |  |
| [**toggleAdminAiTextModelProvider**](AdminApi.md#toggleadminaitextmodelprovider) | **POST** /internal/admin/ai-text-model-providers/{id}/toggle |  |
| [**toggleAdminImageModelProvider**](AdminApi.md#toggleadminimagemodelprovider) | **POST** /internal/admin/image-model-providers/{id}/toggle | Enable or disable an image generation model provider. |
| [**updateAdminAiTextModelProvider**](AdminApi.md#updateadminaitextmodelprovider) | **PUT** /internal/admin/ai-text-model-providers/{id} |  |
| [**updateAdminImageModelProvider**](AdminApi.md#updateadminimagemodelprovider) | **PUT** /internal/admin/image-model-providers/{id} | Update an image generation model provider. |
| [**upsertAdminImageGenUsage**](AdminApi.md#upsertadminimagegenusage) | **PUT** /internal/admin/image-gen-usages/{usageCode} | Create or update a business usage to image provider mapping. |



## createAdminAiTextModelProvider

> AiTextModelProvider createAdminAiTextModelProvider(aiTextModelProviderWriteRequest)



### Example

```ts
import {
  Configuration,
  AdminApi,
} from '@wishpool/api-client';
import type { CreateAdminAiTextModelProviderRequest } from '@wishpool/api-client';

async function example() {
  console.log("🚀 Testing @wishpool/api-client SDK...");
  const config = new Configuration({ 
    // Configure HTTP bearer authorization: bearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new AdminApi(config);

  const body = {
    // AiTextModelProviderWriteRequest
    aiTextModelProviderWriteRequest: ...,
  } satisfies CreateAdminAiTextModelProviderRequest;

  try {
    const data = await api.createAdminAiTextModelProvider(body);
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
| **aiTextModelProviderWriteRequest** | [AiTextModelProviderWriteRequest](AiTextModelProviderWriteRequest.md) |  | |

### Return type

[**AiTextModelProvider**](AiTextModelProvider.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

- **Content-Type**: `application/json`
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | OK |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## createAdminImageModelProvider

> ImageModelProvider createAdminImageModelProvider(imageModelProviderWriteRequest)

Create an image generation model provider.

### Example

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

### Parameters


| Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **imageModelProviderWriteRequest** | [ImageModelProviderWriteRequest](ImageModelProviderWriteRequest.md) |  | |

### Return type

[**ImageModelProvider**](ImageModelProvider.md)

### Authorization

[internalToken](../README.md#internalToken)

### HTTP request headers

- **Content-Type**: `application/json`
- **Accept**: `application/json`, `application/problem+json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Created provider. |  -  |
| **0** | Error response. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## deleteAdminAiTextModelProvider

> deleteAdminAiTextModelProvider(id)



### Example

```ts
import {
  Configuration,
  AdminApi,
} from '@wishpool/api-client';
import type { DeleteAdminAiTextModelProviderRequest } from '@wishpool/api-client';

async function example() {
  console.log("🚀 Testing @wishpool/api-client SDK...");
  const config = new Configuration({ 
    // Configure HTTP bearer authorization: bearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new AdminApi(config);

  const body = {
    // string
    id: 38400000-8cf0-11bd-b23e-10b96e4ef00d,
  } satisfies DeleteAdminAiTextModelProviderRequest;

  try {
    const data = await api.deleteAdminAiTextModelProvider(body);
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
| **id** | `string` |  | [Defaults to `undefined`] |

### Return type

`void` (Empty response body)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: Not defined


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **204** | Deleted |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## deleteAdminImageModelProvider

> deleteAdminImageModelProvider(id)

Delete an image generation model provider.

### Example

```ts
import {
  Configuration,
  AdminApi,
} from '@wishpool/api-client';
import type { DeleteAdminImageModelProviderRequest } from '@wishpool/api-client';

async function example() {
  console.log("🚀 Testing @wishpool/api-client SDK...");
  const config = new Configuration({ 
    // To configure API key authorization: internalToken
    apiKey: "YOUR API KEY",
  });
  const api = new AdminApi(config);

  const body = {
    // string
    id: 38400000-8cf0-11bd-b23e-10b96e4ef00d,
  } satisfies DeleteAdminImageModelProviderRequest;

  try {
    const data = await api.deleteAdminImageModelProvider(body);
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
| **id** | `string` |  | [Defaults to `undefined`] |

### Return type

`void` (Empty response body)

### Authorization

[internalToken](../README.md#internalToken)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/problem+json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Provider deleted. |  -  |
| **0** | Error response. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## getAdminDashboard

> AdminDashboard getAdminDashboard()

Get local administration dashboard counters.

### Example

```ts
import {
  Configuration,
  AdminApi,
} from '@wishpool/api-client';
import type { GetAdminDashboardRequest } from '@wishpool/api-client';

async function example() {
  console.log("🚀 Testing @wishpool/api-client SDK...");
  const config = new Configuration({ 
    // To configure API key authorization: internalToken
    apiKey: "YOUR API KEY",
  });
  const api = new AdminApi(config);

  try {
    const data = await api.getAdminDashboard();
    console.log(data);
  } catch (error) {
    console.error(error);
  }
}

// Run the test
example().catch(console.error);
```

### Parameters

This endpoint does not need any parameter.

### Return type

[**AdminDashboard**](AdminDashboard.md)

### Authorization

[internalToken](../README.md#internalToken)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`, `application/problem+json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Dashboard counters. |  -  |
| **0** | Error response. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## grantAdminMediaAccess

> AdminMediaAccessGrant grantAdminMediaAccess(adminMediaAccessGrantRequest)

Create an audited short-lived media access grant.

### Example

```ts
import {
  Configuration,
  AdminApi,
} from '@wishpool/api-client';
import type { GrantAdminMediaAccessRequest } from '@wishpool/api-client';

async function example() {
  console.log("🚀 Testing @wishpool/api-client SDK...");
  const config = new Configuration({ 
    // To configure API key authorization: internalToken
    apiKey: "YOUR API KEY",
  });
  const api = new AdminApi(config);

  const body = {
    // AdminMediaAccessGrantRequest
    adminMediaAccessGrantRequest: ...,
  } satisfies GrantAdminMediaAccessRequest;

  try {
    const data = await api.grantAdminMediaAccess(body);
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
| **adminMediaAccessGrantRequest** | [AdminMediaAccessGrantRequest](AdminMediaAccessGrantRequest.md) |  | |

### Return type

[**AdminMediaAccessGrant**](AdminMediaAccessGrant.md)

### Authorization

[internalToken](../README.md#internalToken)

### HTTP request headers

- **Content-Type**: `application/json`
- **Accept**: `application/json`, `application/problem+json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Media access grant. |  -  |
| **0** | Error response. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## listAdminAiTextModelProviders

> Array&lt;AiTextModelProvider&gt; listAdminAiTextModelProviders()



### Example

```ts
import {
  Configuration,
  AdminApi,
} from '@wishpool/api-client';
import type { ListAdminAiTextModelProvidersRequest } from '@wishpool/api-client';

async function example() {
  console.log("🚀 Testing @wishpool/api-client SDK...");
  const config = new Configuration({ 
    // Configure HTTP bearer authorization: bearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new AdminApi(config);

  try {
    const data = await api.listAdminAiTextModelProviders();
    console.log(data);
  } catch (error) {
    console.error(error);
  }
}

// Run the test
example().catch(console.error);
```

### Parameters

This endpoint does not need any parameter.

### Return type

[**Array&lt;AiTextModelProvider&gt;**](AiTextModelProvider.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | OK |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## listAdminAuditLogs

> Array&lt;AdminAuditLog&gt; listAdminAuditLogs(familyId, action, limit)

List audit logs for administration.

### Example

```ts
import {
  Configuration,
  AdminApi,
} from '@wishpool/api-client';
import type { ListAdminAuditLogsRequest } from '@wishpool/api-client';

async function example() {
  console.log("🚀 Testing @wishpool/api-client SDK...");
  const config = new Configuration({ 
    // To configure API key authorization: internalToken
    apiKey: "YOUR API KEY",
  });
  const api = new AdminApi(config);

  const body = {
    // string (optional)
    familyId: 38400000-8cf0-11bd-b23e-10b96e4ef00d,
    // string (optional)
    action: action_example,
    // number (optional)
    limit: 56,
  } satisfies ListAdminAuditLogsRequest;

  try {
    const data = await api.listAdminAuditLogs(body);
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
| **familyId** | `string` |  | [Optional] [Defaults to `undefined`] |
| **action** | `string` |  | [Optional] [Defaults to `undefined`] |
| **limit** | `number` |  | [Optional] [Defaults to `50`] |

### Return type

[**Array&lt;AdminAuditLog&gt;**](AdminAuditLog.md)

### Authorization

[internalToken](../README.md#internalToken)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`, `application/problem+json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Audit log rows. |  -  |
| **0** | Error response. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## listAdminFamilies

> Array&lt;AdminFamilySummary&gt; listAdminFamilies(limit, status)

List family metadata for administration.

### Example

```ts
import {
  Configuration,
  AdminApi,
} from '@wishpool/api-client';
import type { ListAdminFamiliesRequest } from '@wishpool/api-client';

async function example() {
  console.log("🚀 Testing @wishpool/api-client SDK...");
  const config = new Configuration({ 
    // To configure API key authorization: internalToken
    apiKey: "YOUR API KEY",
  });
  const api = new AdminApi(config);

  const body = {
    // number (optional)
    limit: 56,
    // 'active' | 'locked' | 'deleting' | 'deleted' (optional)
    status: status_example,
  } satisfies ListAdminFamiliesRequest;

  try {
    const data = await api.listAdminFamilies(body);
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
| **limit** | `number` |  | [Optional] [Defaults to `50`] |
| **status** | `active`, `locked`, `deleting`, `deleted` |  | [Optional] [Defaults to `undefined`] [Enum: active, locked, deleting, deleted] |

### Return type

[**Array&lt;AdminFamilySummary&gt;**](AdminFamilySummary.md)

### Authorization

[internalToken](../README.md#internalToken)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`, `application/problem+json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Family metadata. |  -  |
| **0** | Error response. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## listAdminImageGenUsages

> Array&lt;ImageGenUsage&gt; listAdminImageGenUsages()

List business usage to image provider mappings.

### Example

```ts
import {
  Configuration,
  AdminApi,
} from '@wishpool/api-client';
import type { ListAdminImageGenUsagesRequest } from '@wishpool/api-client';

async function example() {
  console.log("🚀 Testing @wishpool/api-client SDK...");
  const config = new Configuration({ 
    // To configure API key authorization: internalToken
    apiKey: "YOUR API KEY",
  });
  const api = new AdminApi(config);

  try {
    const data = await api.listAdminImageGenUsages();
    console.log(data);
  } catch (error) {
    console.error(error);
  }
}

// Run the test
example().catch(console.error);
```

### Parameters

This endpoint does not need any parameter.

### Return type

[**Array&lt;ImageGenUsage&gt;**](ImageGenUsage.md)

### Authorization

[internalToken](../README.md#internalToken)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`, `application/problem+json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Usage mappings. |  -  |
| **0** | Error response. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## listAdminImageModelProviders

> Array&lt;ImageModelProvider&gt; listAdminImageModelProviders()

List image generation model provider configurations with masked API keys.

### Example

```ts
import {
  Configuration,
  AdminApi,
} from '@wishpool/api-client';
import type { ListAdminImageModelProvidersRequest } from '@wishpool/api-client';

async function example() {
  console.log("🚀 Testing @wishpool/api-client SDK...");
  const config = new Configuration({ 
    // To configure API key authorization: internalToken
    apiKey: "YOUR API KEY",
  });
  const api = new AdminApi(config);

  try {
    const data = await api.listAdminImageModelProviders();
    console.log(data);
  } catch (error) {
    console.error(error);
  }
}

// Run the test
example().catch(console.error);
```

### Parameters

This endpoint does not need any parameter.

### Return type

[**Array&lt;ImageModelProvider&gt;**](ImageModelProvider.md)

### Authorization

[internalToken](../README.md#internalToken)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`, `application/problem+json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Image model providers. |  -  |
| **0** | Error response. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## listAdminMediaAccessGrants

> AdminMediaAccessGrantPage listAdminMediaAccessGrants(familyId, mediaAssetId, active, from, to, limit, offset)



### Example

```ts
import {
  Configuration,
  AdminApi,
} from '@wishpool/api-client';
import type { ListAdminMediaAccessGrantsRequest } from '@wishpool/api-client';

async function example() {
  console.log("🚀 Testing @wishpool/api-client SDK...");
  const config = new Configuration({ 
    // To configure API key authorization: internalToken
    apiKey: "YOUR API KEY",
  });
  const api = new AdminApi(config);

  const body = {
    // string (optional)
    familyId: 38400000-8cf0-11bd-b23e-10b96e4ef00d,
    // string (optional)
    mediaAssetId: 38400000-8cf0-11bd-b23e-10b96e4ef00d,
    // boolean (optional)
    active: true,
    // Date (optional)
    from: 2013-10-20T19:20:30+01:00,
    // Date (optional)
    to: 2013-10-20T19:20:30+01:00,
    // number (optional)
    limit: 56,
    // number (optional)
    offset: 56,
  } satisfies ListAdminMediaAccessGrantsRequest;

  try {
    const data = await api.listAdminMediaAccessGrants(body);
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
| **familyId** | `string` |  | [Optional] [Defaults to `undefined`] |
| **mediaAssetId** | `string` |  | [Optional] [Defaults to `undefined`] |
| **active** | `boolean` |  | [Optional] [Defaults to `undefined`] |
| **from** | `Date` |  | [Optional] [Defaults to `undefined`] |
| **to** | `Date` |  | [Optional] [Defaults to `undefined`] |
| **limit** | `number` |  | [Optional] [Defaults to `50`] |
| **offset** | `number` |  | [Optional] [Defaults to `0`] |

### Return type

[**AdminMediaAccessGrantPage**](AdminMediaAccessGrantPage.md)

### Authorization

[internalToken](../README.md#internalToken)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`, `application/problem+json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Media access grants. |  -  |
| **0** | Error response. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## listAdminPrivacyRequests

> Array&lt;AdminPrivacyRequest&gt; listAdminPrivacyRequests(limit, status)

List privacy requests for administration.

### Example

```ts
import {
  Configuration,
  AdminApi,
} from '@wishpool/api-client';
import type { ListAdminPrivacyRequestsRequest } from '@wishpool/api-client';

async function example() {
  console.log("🚀 Testing @wishpool/api-client SDK...");
  const config = new Configuration({ 
    // To configure API key authorization: internalToken
    apiKey: "YOUR API KEY",
  });
  const api = new AdminApi(config);

  const body = {
    // number (optional)
    limit: 56,
    // string (optional)
    status: status_example,
  } satisfies ListAdminPrivacyRequestsRequest;

  try {
    const data = await api.listAdminPrivacyRequests(body);
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
| **limit** | `number` |  | [Optional] [Defaults to `50`] |
| **status** | `string` |  | [Optional] [Defaults to `undefined`] |

### Return type

[**Array&lt;AdminPrivacyRequest&gt;**](AdminPrivacyRequest.md)

### Authorization

[internalToken](../README.md#internalToken)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`, `application/problem+json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Privacy request rows. |  -  |
| **0** | Error response. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## revokeAdminMediaAccessGrant

> AdminMediaAccessGrant revokeAdminMediaAccessGrant(grantId, revokeAdminMediaAccessGrantRequest)



### Example

```ts
import {
  Configuration,
  AdminApi,
} from '@wishpool/api-client';
import type { RevokeAdminMediaAccessGrantOperationRequest } from '@wishpool/api-client';

async function example() {
  console.log("🚀 Testing @wishpool/api-client SDK...");
  const config = new Configuration({ 
    // To configure API key authorization: internalToken
    apiKey: "YOUR API KEY",
  });
  const api = new AdminApi(config);

  const body = {
    // string
    grantId: 38400000-8cf0-11bd-b23e-10b96e4ef00d,
    // RevokeAdminMediaAccessGrantRequest
    revokeAdminMediaAccessGrantRequest: ...,
  } satisfies RevokeAdminMediaAccessGrantOperationRequest;

  try {
    const data = await api.revokeAdminMediaAccessGrant(body);
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
| **grantId** | `string` |  | [Defaults to `undefined`] |
| **revokeAdminMediaAccessGrantRequest** | [RevokeAdminMediaAccessGrantRequest](RevokeAdminMediaAccessGrantRequest.md) |  | |

### Return type

[**AdminMediaAccessGrant**](AdminMediaAccessGrant.md)

### Authorization

[internalToken](../README.md#internalToken)

### HTTP request headers

- **Content-Type**: `application/json`
- **Accept**: `application/json`, `application/problem+json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Revoked grant. |  -  |
| **0** | Error response. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## setDefaultAdminAiTextModelProvider

> AiTextModelProvider setDefaultAdminAiTextModelProvider(id)



### Example

```ts
import {
  Configuration,
  AdminApi,
} from '@wishpool/api-client';
import type { SetDefaultAdminAiTextModelProviderRequest } from '@wishpool/api-client';

async function example() {
  console.log("🚀 Testing @wishpool/api-client SDK...");
  const config = new Configuration({ 
    // Configure HTTP bearer authorization: bearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new AdminApi(config);

  const body = {
    // string
    id: 38400000-8cf0-11bd-b23e-10b96e4ef00d,
  } satisfies SetDefaultAdminAiTextModelProviderRequest;

  try {
    const data = await api.setDefaultAdminAiTextModelProvider(body);
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
| **id** | `string` |  | [Defaults to `undefined`] |

### Return type

[**AiTextModelProvider**](AiTextModelProvider.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | OK |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## setDefaultAdminImageModelProvider

> ImageModelProvider setDefaultAdminImageModelProvider(id)

Set the global default image generation model provider.

### Example

```ts
import {
  Configuration,
  AdminApi,
} from '@wishpool/api-client';
import type { SetDefaultAdminImageModelProviderRequest } from '@wishpool/api-client';

async function example() {
  console.log("🚀 Testing @wishpool/api-client SDK...");
  const config = new Configuration({ 
    // To configure API key authorization: internalToken
    apiKey: "YOUR API KEY",
  });
  const api = new AdminApi(config);

  const body = {
    // string
    id: 38400000-8cf0-11bd-b23e-10b96e4ef00d,
  } satisfies SetDefaultAdminImageModelProviderRequest;

  try {
    const data = await api.setDefaultAdminImageModelProvider(body);
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
| **id** | `string` |  | [Defaults to `undefined`] |

### Return type

[**ImageModelProvider**](ImageModelProvider.md)

### Authorization

[internalToken](../README.md#internalToken)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`, `application/problem+json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Updated provider. |  -  |
| **0** | Error response. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## testAdminAiTextModelProvider

> AiTextProviderTestResponse testAdminAiTextModelProvider(id)



### Example

```ts
import {
  Configuration,
  AdminApi,
} from '@wishpool/api-client';
import type { TestAdminAiTextModelProviderRequest } from '@wishpool/api-client';

async function example() {
  console.log("🚀 Testing @wishpool/api-client SDK...");
  const config = new Configuration({ 
    // Configure HTTP bearer authorization: bearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new AdminApi(config);

  const body = {
    // string
    id: 38400000-8cf0-11bd-b23e-10b96e4ef00d,
  } satisfies TestAdminAiTextModelProviderRequest;

  try {
    const data = await api.testAdminAiTextModelProvider(body);
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
| **id** | `string` |  | [Defaults to `undefined`] |

### Return type

[**AiTextProviderTestResponse**](AiTextProviderTestResponse.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | OK |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## toggleAdminAiTextModelProvider

> AiTextModelProvider toggleAdminAiTextModelProvider(id, aiTextModelProviderToggleRequest)



### Example

```ts
import {
  Configuration,
  AdminApi,
} from '@wishpool/api-client';
import type { ToggleAdminAiTextModelProviderRequest } from '@wishpool/api-client';

async function example() {
  console.log("🚀 Testing @wishpool/api-client SDK...");
  const config = new Configuration({ 
    // Configure HTTP bearer authorization: bearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new AdminApi(config);

  const body = {
    // string
    id: 38400000-8cf0-11bd-b23e-10b96e4ef00d,
    // AiTextModelProviderToggleRequest
    aiTextModelProviderToggleRequest: ...,
  } satisfies ToggleAdminAiTextModelProviderRequest;

  try {
    const data = await api.toggleAdminAiTextModelProvider(body);
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
| **id** | `string` |  | [Defaults to `undefined`] |
| **aiTextModelProviderToggleRequest** | [AiTextModelProviderToggleRequest](AiTextModelProviderToggleRequest.md) |  | |

### Return type

[**AiTextModelProvider**](AiTextModelProvider.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

- **Content-Type**: `application/json`
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | OK |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## toggleAdminImageModelProvider

> ImageModelProvider toggleAdminImageModelProvider(id, imageModelProviderToggleRequest)

Enable or disable an image generation model provider.

### Example

```ts
import {
  Configuration,
  AdminApi,
} from '@wishpool/api-client';
import type { ToggleAdminImageModelProviderRequest } from '@wishpool/api-client';

async function example() {
  console.log("🚀 Testing @wishpool/api-client SDK...");
  const config = new Configuration({ 
    // To configure API key authorization: internalToken
    apiKey: "YOUR API KEY",
  });
  const api = new AdminApi(config);

  const body = {
    // string
    id: 38400000-8cf0-11bd-b23e-10b96e4ef00d,
    // ImageModelProviderToggleRequest
    imageModelProviderToggleRequest: ...,
  } satisfies ToggleAdminImageModelProviderRequest;

  try {
    const data = await api.toggleAdminImageModelProvider(body);
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
| **id** | `string` |  | [Defaults to `undefined`] |
| **imageModelProviderToggleRequest** | [ImageModelProviderToggleRequest](ImageModelProviderToggleRequest.md) |  | |

### Return type

[**ImageModelProvider**](ImageModelProvider.md)

### Authorization

[internalToken](../README.md#internalToken)

### HTTP request headers

- **Content-Type**: `application/json`
- **Accept**: `application/json`, `application/problem+json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Updated provider. |  -  |
| **0** | Error response. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## updateAdminAiTextModelProvider

> AiTextModelProvider updateAdminAiTextModelProvider(id, aiTextModelProviderWriteRequest)



### Example

```ts
import {
  Configuration,
  AdminApi,
} from '@wishpool/api-client';
import type { UpdateAdminAiTextModelProviderRequest } from '@wishpool/api-client';

async function example() {
  console.log("🚀 Testing @wishpool/api-client SDK...");
  const config = new Configuration({ 
    // Configure HTTP bearer authorization: bearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new AdminApi(config);

  const body = {
    // string
    id: 38400000-8cf0-11bd-b23e-10b96e4ef00d,
    // AiTextModelProviderWriteRequest
    aiTextModelProviderWriteRequest: ...,
  } satisfies UpdateAdminAiTextModelProviderRequest;

  try {
    const data = await api.updateAdminAiTextModelProvider(body);
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
| **id** | `string` |  | [Defaults to `undefined`] |
| **aiTextModelProviderWriteRequest** | [AiTextModelProviderWriteRequest](AiTextModelProviderWriteRequest.md) |  | |

### Return type

[**AiTextModelProvider**](AiTextModelProvider.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

- **Content-Type**: `application/json`
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | OK |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## updateAdminImageModelProvider

> ImageModelProvider updateAdminImageModelProvider(id, imageModelProviderWriteRequest)

Update an image generation model provider.

### Example

```ts
import {
  Configuration,
  AdminApi,
} from '@wishpool/api-client';
import type { UpdateAdminImageModelProviderRequest } from '@wishpool/api-client';

async function example() {
  console.log("🚀 Testing @wishpool/api-client SDK...");
  const config = new Configuration({ 
    // To configure API key authorization: internalToken
    apiKey: "YOUR API KEY",
  });
  const api = new AdminApi(config);

  const body = {
    // string
    id: 38400000-8cf0-11bd-b23e-10b96e4ef00d,
    // ImageModelProviderWriteRequest
    imageModelProviderWriteRequest: ...,
  } satisfies UpdateAdminImageModelProviderRequest;

  try {
    const data = await api.updateAdminImageModelProvider(body);
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
| **id** | `string` |  | [Defaults to `undefined`] |
| **imageModelProviderWriteRequest** | [ImageModelProviderWriteRequest](ImageModelProviderWriteRequest.md) |  | |

### Return type

[**ImageModelProvider**](ImageModelProvider.md)

### Authorization

[internalToken](../README.md#internalToken)

### HTTP request headers

- **Content-Type**: `application/json`
- **Accept**: `application/json`, `application/problem+json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Updated provider. |  -  |
| **0** | Error response. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## upsertAdminImageGenUsage

> ImageGenUsage upsertAdminImageGenUsage(usageCode, imageGenUsageWriteRequest)

Create or update a business usage to image provider mapping.

### Example

```ts
import {
  Configuration,
  AdminApi,
} from '@wishpool/api-client';
import type { UpsertAdminImageGenUsageRequest } from '@wishpool/api-client';

async function example() {
  console.log("🚀 Testing @wishpool/api-client SDK...");
  const config = new Configuration({ 
    // To configure API key authorization: internalToken
    apiKey: "YOUR API KEY",
  });
  const api = new AdminApi(config);

  const body = {
    // string
    usageCode: usageCode_example,
    // ImageGenUsageWriteRequest
    imageGenUsageWriteRequest: ...,
  } satisfies UpsertAdminImageGenUsageRequest;

  try {
    const data = await api.upsertAdminImageGenUsage(body);
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
| **usageCode** | `string` |  | [Defaults to `undefined`] |
| **imageGenUsageWriteRequest** | [ImageGenUsageWriteRequest](ImageGenUsageWriteRequest.md) |  | |

### Return type

[**ImageGenUsage**](ImageGenUsage.md)

### Authorization

[internalToken](../README.md#internalToken)

### HTTP request headers

- **Content-Type**: `application/json`
- **Accept**: `application/json`, `application/problem+json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Updated usage mapping. |  -  |
| **0** | Error response. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)

