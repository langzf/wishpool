# PairingApi

All URIs are relative to *http://localhost:8080*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**consumePairingCode**](PairingApi.md#consumepairingcodeoperation) | **POST** /pairing/consume | Pair a child device with a family. |
| [**createPairingSession**](PairingApi.md#createpairingsessionoperation) | **POST** /families/{familyId}/pairing-sessions | Create a child device pairing session. |



## consumePairingCode

> AuthTokenPair consumePairingCode(consumePairingCodeRequest, idempotencyKey)

Pair a child device with a family.

### Example

```ts
import {
  Configuration,
  PairingApi,
} from '@wishpool/api-client';
import type { ConsumePairingCodeOperationRequest } from '@wishpool/api-client';

async function example() {
  console.log("🚀 Testing @wishpool/api-client SDK...");
  const api = new PairingApi();

  const body = {
    // ConsumePairingCodeRequest
    consumePairingCodeRequest: ...,
    // string (optional)
    idempotencyKey: idempotencyKey_example,
  } satisfies ConsumePairingCodeOperationRequest;

  try {
    const data = await api.consumePairingCode(body);
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
| **consumePairingCodeRequest** | [ConsumePairingCodeRequest](ConsumePairingCodeRequest.md) |  | |
| **idempotencyKey** | `string` |  | [Optional] [Defaults to `undefined`] |

### Return type

[**AuthTokenPair**](AuthTokenPair.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: `application/json`
- **Accept**: `application/json`, `application/problem+json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Device paired. |  -  |
| **0** | Error response. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## createPairingSession

> PairingSessionCreated createPairingSession(familyId, createPairingSessionRequest, idempotencyKey)

Create a child device pairing session.

### Example

```ts
import {
  Configuration,
  PairingApi,
} from '@wishpool/api-client';
import type { CreatePairingSessionOperationRequest } from '@wishpool/api-client';

async function example() {
  console.log("🚀 Testing @wishpool/api-client SDK...");
  const config = new Configuration({ 
    // Configure HTTP bearer authorization: bearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new PairingApi(config);

  const body = {
    // string
    familyId: 38400000-8cf0-11bd-b23e-10b96e4ef00d,
    // CreatePairingSessionRequest
    createPairingSessionRequest: ...,
    // string (optional)
    idempotencyKey: idempotencyKey_example,
  } satisfies CreatePairingSessionOperationRequest;

  try {
    const data = await api.createPairingSession(body);
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
| **createPairingSessionRequest** | [CreatePairingSessionRequest](CreatePairingSessionRequest.md) |  | |
| **idempotencyKey** | `string` |  | [Optional] [Defaults to `undefined`] |

### Return type

[**PairingSessionCreated**](PairingSessionCreated.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

- **Content-Type**: `application/json`
- **Accept**: `application/json`, `application/problem+json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **201** | Pairing session created. |  -  |
| **0** | Error response. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)

