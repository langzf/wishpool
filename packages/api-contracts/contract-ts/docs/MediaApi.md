# MediaApi

All URIs are relative to *http://localhost:8080*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**createUploadSession**](MediaApi.md#createuploadsessionoperation) | **POST** /media/upload-sessions | Create a signed upload session. |
| [**finalizeMedia**](MediaApi.md#finalizemediaoperation) | **POST** /media/{mediaId}/finalize | Finalize a media upload. |



## createUploadSession

> UploadSession createUploadSession(createUploadSessionRequest, idempotencyKey)

Create a signed upload session.

### Example

```ts
import {
  Configuration,
  MediaApi,
} from '@wishpool/api-client';
import type { CreateUploadSessionOperationRequest } from '@wishpool/api-client';

async function example() {
  console.log("🚀 Testing @wishpool/api-client SDK...");
  const config = new Configuration({ 
    // Configure HTTP bearer authorization: bearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new MediaApi(config);

  const body = {
    // CreateUploadSessionRequest
    createUploadSessionRequest: ...,
    // string (optional)
    idempotencyKey: idempotencyKey_example,
  } satisfies CreateUploadSessionOperationRequest;

  try {
    const data = await api.createUploadSession(body);
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
| **createUploadSessionRequest** | [CreateUploadSessionRequest](CreateUploadSessionRequest.md) |  | |
| **idempotencyKey** | `string` |  | [Optional] [Defaults to `undefined`] |

### Return type

[**UploadSession**](UploadSession.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

- **Content-Type**: `application/json`
- **Accept**: `application/json`, `application/problem+json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **201** | Upload session created. |  -  |
| **0** | Error response. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## finalizeMedia

> MediaAsset finalizeMedia(mediaId, finalizeMediaRequest, idempotencyKey)

Finalize a media upload.

### Example

```ts
import {
  Configuration,
  MediaApi,
} from '@wishpool/api-client';
import type { FinalizeMediaOperationRequest } from '@wishpool/api-client';

async function example() {
  console.log("🚀 Testing @wishpool/api-client SDK...");
  const config = new Configuration({ 
    // Configure HTTP bearer authorization: bearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new MediaApi(config);

  const body = {
    // string
    mediaId: 38400000-8cf0-11bd-b23e-10b96e4ef00d,
    // FinalizeMediaRequest
    finalizeMediaRequest: ...,
    // string (optional)
    idempotencyKey: idempotencyKey_example,
  } satisfies FinalizeMediaOperationRequest;

  try {
    const data = await api.finalizeMedia(body);
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
| **mediaId** | `string` |  | [Defaults to `undefined`] |
| **finalizeMediaRequest** | [FinalizeMediaRequest](FinalizeMediaRequest.md) |  | |
| **idempotencyKey** | `string` |  | [Optional] [Defaults to `undefined`] |

### Return type

[**MediaAsset**](MediaAsset.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

- **Content-Type**: `application/json`
- **Accept**: `application/json`, `application/problem+json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Media finalized. |  -  |
| **0** | Error response. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)

