# HomeApi

All URIs are relative to *http://localhost:8080*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**getChildHomeContext**](HomeApi.md#getchildhomecontext) | **GET** /children/{childId}/home-context | Get the child-facing home context used by mobile and tablet clients. |
| [**getParentDashboardContext**](HomeApi.md#getparentdashboardcontext) | **GET** /families/{familyId}/parent-dashboard | Get the parent dashboard context used by mobile and web clients. |



## getChildHomeContext

> ChildHomeContext getChildHomeContext(childId, date)

Get the child-facing home context used by mobile and tablet clients.

### Example

```ts
import {
  Configuration,
  HomeApi,
} from '@wishpool/api-client';
import type { GetChildHomeContextRequest } from '@wishpool/api-client';

async function example() {
  console.log("🚀 Testing @wishpool/api-client SDK...");
  const config = new Configuration({ 
    // Configure HTTP bearer authorization: bearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new HomeApi(config);

  const body = {
    // string
    childId: 38400000-8cf0-11bd-b23e-10b96e4ef00d,
    // Date (optional)
    date: 2013-10-20,
  } satisfies GetChildHomeContextRequest;

  try {
    const data = await api.getChildHomeContext(body);
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

[**ChildHomeContext**](ChildHomeContext.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`, `application/problem+json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Child home context. |  -  |
| **0** | Error response. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## getParentDashboardContext

> ParentDashboardContext getParentDashboardContext(familyId, childId, date)

Get the parent dashboard context used by mobile and web clients.

### Example

```ts
import {
  Configuration,
  HomeApi,
} from '@wishpool/api-client';
import type { GetParentDashboardContextRequest } from '@wishpool/api-client';

async function example() {
  console.log("🚀 Testing @wishpool/api-client SDK...");
  const config = new Configuration({ 
    // Configure HTTP bearer authorization: bearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new HomeApi(config);

  const body = {
    // string
    familyId: 38400000-8cf0-11bd-b23e-10b96e4ef00d,
    // string (optional)
    childId: 38400000-8cf0-11bd-b23e-10b96e4ef00d,
    // Date (optional)
    date: 2013-10-20,
  } satisfies GetParentDashboardContextRequest;

  try {
    const data = await api.getParentDashboardContext(body);
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
| **childId** | `string` |  | [Optional] [Defaults to `undefined`] |
| **date** | `Date` |  | [Optional] [Defaults to `undefined`] |

### Return type

[**ParentDashboardContext**](ParentDashboardContext.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`, `application/problem+json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Parent dashboard context. |  -  |
| **0** | Error response. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)

