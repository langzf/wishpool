# ReviewsApi

All URIs are relative to *http://localhost:8080*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**getReviewDetail**](ReviewsApi.md#getreviewdetail) | **GET** /reviews/{submissionId}/detail | Get a review detail view for a submission. |
| [**listPendingReviews**](ReviewsApi.md#listpendingreviews) | **GET** /reviews/pending | List pending reviews. |
| [**reviewSubmission**](ReviewsApi.md#reviewsubmissionoperation) | **POST** /reviews | Approve or reject a submission. |
| [**revokeReview**](ReviewsApi.md#revokereviewoperation) | **POST** /reviews/{reviewId}/revoke | Revoke a review and create adjustment records when needed. |



## getReviewDetail

> SubmissionDetail getReviewDetail(submissionId)

Get a review detail view for a submission.

### Example

```ts
import {
  Configuration,
  ReviewsApi,
} from '@wishpool/api-client';
import type { GetReviewDetailRequest } from '@wishpool/api-client';

async function example() {
  console.log("🚀 Testing @wishpool/api-client SDK...");
  const config = new Configuration({ 
    // Configure HTTP bearer authorization: bearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new ReviewsApi(config);

  const body = {
    // string
    submissionId: 38400000-8cf0-11bd-b23e-10b96e4ef00d,
  } satisfies GetReviewDetailRequest;

  try {
    const data = await api.getReviewDetail(body);
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
| **200** | Review detail. |  -  |
| **0** | Error response. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## listPendingReviews

> Array&lt;PendingReviewCard&gt; listPendingReviews(familyId)

List pending reviews.

### Example

```ts
import {
  Configuration,
  ReviewsApi,
} from '@wishpool/api-client';
import type { ListPendingReviewsRequest } from '@wishpool/api-client';

async function example() {
  console.log("🚀 Testing @wishpool/api-client SDK...");
  const config = new Configuration({ 
    // Configure HTTP bearer authorization: bearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new ReviewsApi(config);

  const body = {
    // string
    familyId: 38400000-8cf0-11bd-b23e-10b96e4ef00d,
  } satisfies ListPendingReviewsRequest;

  try {
    const data = await api.listPendingReviews(body);
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

[**Array&lt;PendingReviewCard&gt;**](PendingReviewCard.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`, `application/problem+json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Pending review cards. |  -  |
| **0** | Error response. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## reviewSubmission

> Review reviewSubmission(reviewSubmissionRequest, idempotencyKey)

Approve or reject a submission.

### Example

```ts
import {
  Configuration,
  ReviewsApi,
} from '@wishpool/api-client';
import type { ReviewSubmissionOperationRequest } from '@wishpool/api-client';

async function example() {
  console.log("🚀 Testing @wishpool/api-client SDK...");
  const config = new Configuration({ 
    // Configure HTTP bearer authorization: bearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new ReviewsApi(config);

  const body = {
    // ReviewSubmissionRequest
    reviewSubmissionRequest: ...,
    // string (optional)
    idempotencyKey: idempotencyKey_example,
  } satisfies ReviewSubmissionOperationRequest;

  try {
    const data = await api.reviewSubmission(body);
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
| **reviewSubmissionRequest** | [ReviewSubmissionRequest](ReviewSubmissionRequest.md) |  | |
| **idempotencyKey** | `string` |  | [Optional] [Defaults to `undefined`] |

### Return type

[**Review**](Review.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

- **Content-Type**: `application/json`
- **Accept**: `application/json`, `application/problem+json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **201** | Review created. |  -  |
| **0** | Error response. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## revokeReview

> Review revokeReview(reviewId, revokeReviewRequest, idempotencyKey)

Revoke a review and create adjustment records when needed.

### Example

```ts
import {
  Configuration,
  ReviewsApi,
} from '@wishpool/api-client';
import type { RevokeReviewOperationRequest } from '@wishpool/api-client';

async function example() {
  console.log("🚀 Testing @wishpool/api-client SDK...");
  const config = new Configuration({ 
    // Configure HTTP bearer authorization: bearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new ReviewsApi(config);

  const body = {
    // string
    reviewId: 38400000-8cf0-11bd-b23e-10b96e4ef00d,
    // RevokeReviewRequest
    revokeReviewRequest: ...,
    // string (optional)
    idempotencyKey: idempotencyKey_example,
  } satisfies RevokeReviewOperationRequest;

  try {
    const data = await api.revokeReview(body);
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
| **reviewId** | `string` |  | [Defaults to `undefined`] |
| **revokeReviewRequest** | [RevokeReviewRequest](RevokeReviewRequest.md) |  | |
| **idempotencyKey** | `string` |  | [Optional] [Defaults to `undefined`] |

### Return type

[**Review**](Review.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

- **Content-Type**: `application/json`
- **Accept**: `application/json`, `application/problem+json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Review revoked. |  -  |
| **0** | Error response. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)

