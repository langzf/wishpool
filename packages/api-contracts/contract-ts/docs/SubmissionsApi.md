# SubmissionsApi

All URIs are relative to *http://localhost:8080*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**createSubmission**](SubmissionsApi.md#createsubmissionoperation) | **POST** /submissions | Create a child submission. |
| [**getSubmission**](SubmissionsApi.md#getsubmission) | **GET** /submissions/{submissionId} | Get submission details. |



## createSubmission

> Submission createSubmission(createSubmissionRequest, idempotencyKey)

Create a child submission.

### Example

```ts
import {
  Configuration,
  SubmissionsApi,
} from '@wishpool/api-client';
import type { CreateSubmissionOperationRequest } from '@wishpool/api-client';

async function example() {
  console.log("🚀 Testing @wishpool/api-client SDK...");
  const config = new Configuration({ 
    // Configure HTTP bearer authorization: bearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new SubmissionsApi(config);

  const body = {
    // CreateSubmissionRequest
    createSubmissionRequest: ...,
    // string (optional)
    idempotencyKey: idempotencyKey_example,
  } satisfies CreateSubmissionOperationRequest;

  try {
    const data = await api.createSubmission(body);
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
| **createSubmissionRequest** | [CreateSubmissionRequest](CreateSubmissionRequest.md) |  | |
| **idempotencyKey** | `string` |  | [Optional] [Defaults to `undefined`] |

### Return type

[**Submission**](Submission.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

- **Content-Type**: `application/json`
- **Accept**: `application/json`, `application/problem+json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **201** | Submission created. |  -  |
| **0** | Error response. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## getSubmission

> SubmissionDetail getSubmission(submissionId)

Get submission details.

### Example

```ts
import {
  Configuration,
  SubmissionsApi,
} from '@wishpool/api-client';
import type { GetSubmissionRequest } from '@wishpool/api-client';

async function example() {
  console.log("🚀 Testing @wishpool/api-client SDK...");
  const config = new Configuration({ 
    // Configure HTTP bearer authorization: bearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new SubmissionsApi(config);

  const body = {
    // string
    submissionId: 38400000-8cf0-11bd-b23e-10b96e4ef00d,
  } satisfies GetSubmissionRequest;

  try {
    const data = await api.getSubmission(body);
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
| **submissionId** | `string` |  | [Defaults to `undefined`] |

### Return type

[**SubmissionDetail**](SubmissionDetail.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`, `application/problem+json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Submission. |  -  |
| **0** | Error response. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)

