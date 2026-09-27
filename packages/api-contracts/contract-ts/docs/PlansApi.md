# PlansApi

All URIs are relative to *http://localhost:8080*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**getWeeklyPlan**](PlansApi.md#getweeklyplan) | **GET** /plans/{planId} | Get a weekly plan. |
| [**saveWeeklyPlan**](PlansApi.md#saveweeklyplanoperation) | **POST** /plans | Create or update a weekly plan. |



## getWeeklyPlan

> WeeklyPlan getWeeklyPlan(planId)

Get a weekly plan.

### Example

```ts
import {
  Configuration,
  PlansApi,
} from '@wishpool/api-client';
import type { GetWeeklyPlanRequest } from '@wishpool/api-client';

async function example() {
  console.log("🚀 Testing @wishpool/api-client SDK...");
  const config = new Configuration({ 
    // Configure HTTP bearer authorization: bearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new PlansApi(config);

  const body = {
    // string
    planId: 38400000-8cf0-11bd-b23e-10b96e4ef00d,
  } satisfies GetWeeklyPlanRequest;

  try {
    const data = await api.getWeeklyPlan(body);
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
| **planId** | `string` |  | [Defaults to `undefined`] |

### Return type

[**WeeklyPlan**](WeeklyPlan.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`, `application/problem+json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Weekly plan. |  -  |
| **0** | Error response. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## saveWeeklyPlan

> WeeklyPlan saveWeeklyPlan(saveWeeklyPlanRequest, idempotencyKey)

Create or update a weekly plan.

### Example

```ts
import {
  Configuration,
  PlansApi,
} from '@wishpool/api-client';
import type { SaveWeeklyPlanOperationRequest } from '@wishpool/api-client';

async function example() {
  console.log("🚀 Testing @wishpool/api-client SDK...");
  const config = new Configuration({ 
    // Configure HTTP bearer authorization: bearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new PlansApi(config);

  const body = {
    // SaveWeeklyPlanRequest
    saveWeeklyPlanRequest: ...,
    // string (optional)
    idempotencyKey: idempotencyKey_example,
  } satisfies SaveWeeklyPlanOperationRequest;

  try {
    const data = await api.saveWeeklyPlan(body);
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
| **saveWeeklyPlanRequest** | [SaveWeeklyPlanRequest](SaveWeeklyPlanRequest.md) |  | |
| **idempotencyKey** | `string` |  | [Optional] [Defaults to `undefined`] |

### Return type

[**WeeklyPlan**](WeeklyPlan.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

- **Content-Type**: `application/json`
- **Accept**: `application/json`, `application/problem+json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Weekly plan saved. |  -  |
| **0** | Error response. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)

