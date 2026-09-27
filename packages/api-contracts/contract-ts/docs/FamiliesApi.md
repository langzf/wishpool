# FamiliesApi

All URIs are relative to *http://localhost:8080*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**createFamily**](FamiliesApi.md#createfamilyoperation) | **POST** /families | Create a family. |
| [**getFamily**](FamiliesApi.md#getfamily) | **GET** /families/{familyId} | Get family details. |
| [**inviteParent**](FamiliesApi.md#inviteparentoperation) | **POST** /families/{familyId}/invites | Invite a parent to a family. |
| [**listFamilyMembers**](FamiliesApi.md#listfamilymembers) | **GET** /families/{familyId}/members | List family members. |



## createFamily

> Family createFamily(createFamilyRequest, idempotencyKey)

Create a family.

### Example

```ts
import {
  Configuration,
  FamiliesApi,
} from '@wishpool/api-client';
import type { CreateFamilyOperationRequest } from '@wishpool/api-client';

async function example() {
  console.log("🚀 Testing @wishpool/api-client SDK...");
  const config = new Configuration({ 
    // Configure HTTP bearer authorization: bearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new FamiliesApi(config);

  const body = {
    // CreateFamilyRequest
    createFamilyRequest: ...,
    // string (optional)
    idempotencyKey: idempotencyKey_example,
  } satisfies CreateFamilyOperationRequest;

  try {
    const data = await api.createFamily(body);
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
| **createFamilyRequest** | [CreateFamilyRequest](CreateFamilyRequest.md) |  | |
| **idempotencyKey** | `string` |  | [Optional] [Defaults to `undefined`] |

### Return type

[**Family**](Family.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

- **Content-Type**: `application/json`
- **Accept**: `application/json`, `application/problem+json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **201** | Family created. |  -  |
| **0** | Error response. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## getFamily

> Family getFamily(familyId)

Get family details.

### Example

```ts
import {
  Configuration,
  FamiliesApi,
} from '@wishpool/api-client';
import type { GetFamilyRequest } from '@wishpool/api-client';

async function example() {
  console.log("🚀 Testing @wishpool/api-client SDK...");
  const config = new Configuration({ 
    // Configure HTTP bearer authorization: bearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new FamiliesApi(config);

  const body = {
    // string
    familyId: 38400000-8cf0-11bd-b23e-10b96e4ef00d,
  } satisfies GetFamilyRequest;

  try {
    const data = await api.getFamily(body);
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

[**Family**](Family.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`, `application/problem+json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Family. |  -  |
| **0** | Error response. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## inviteParent

> FamilyInvite inviteParent(familyId, inviteParentRequest, idempotencyKey)

Invite a parent to a family.

### Example

```ts
import {
  Configuration,
  FamiliesApi,
} from '@wishpool/api-client';
import type { InviteParentOperationRequest } from '@wishpool/api-client';

async function example() {
  console.log("🚀 Testing @wishpool/api-client SDK...");
  const config = new Configuration({ 
    // Configure HTTP bearer authorization: bearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new FamiliesApi(config);

  const body = {
    // string
    familyId: 38400000-8cf0-11bd-b23e-10b96e4ef00d,
    // InviteParentRequest
    inviteParentRequest: ...,
    // string (optional)
    idempotencyKey: idempotencyKey_example,
  } satisfies InviteParentOperationRequest;

  try {
    const data = await api.inviteParent(body);
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
| **inviteParentRequest** | [InviteParentRequest](InviteParentRequest.md) |  | |
| **idempotencyKey** | `string` |  | [Optional] [Defaults to `undefined`] |

### Return type

[**FamilyInvite**](FamilyInvite.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

- **Content-Type**: `application/json`
- **Accept**: `application/json`, `application/problem+json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **201** | Invite created. |  -  |
| **0** | Error response. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## listFamilyMembers

> Array&lt;FamilyMember&gt; listFamilyMembers(familyId)

List family members.

### Example

```ts
import {
  Configuration,
  FamiliesApi,
} from '@wishpool/api-client';
import type { ListFamilyMembersRequest } from '@wishpool/api-client';

async function example() {
  console.log("🚀 Testing @wishpool/api-client SDK...");
  const config = new Configuration({ 
    // Configure HTTP bearer authorization: bearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new FamiliesApi(config);

  const body = {
    // string
    familyId: 38400000-8cf0-11bd-b23e-10b96e4ef00d,
  } satisfies ListFamilyMembersRequest;

  try {
    const data = await api.listFamilyMembers(body);
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

[**Array&lt;FamilyMember&gt;**](FamilyMember.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`, `application/problem+json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Family members. |  -  |
| **0** | Error response. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)

