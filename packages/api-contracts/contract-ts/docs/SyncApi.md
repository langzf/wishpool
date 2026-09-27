# SyncApi

All URIs are relative to *http://localhost:8080*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**pullSyncEvents**](SyncApi.md#pullsyncevents) | **GET** /sync/pull | Pull family events after a sequence number. |



## pullSyncEvents

> SyncPullResponse pullSyncEvents(familyId, afterSeq, limit)

Pull family events after a sequence number.

### Example

```ts
import {
  Configuration,
  SyncApi,
} from '@wishpool/api-client';
import type { PullSyncEventsRequest } from '@wishpool/api-client';

async function example() {
  console.log("🚀 Testing @wishpool/api-client SDK...");
  const config = new Configuration({ 
    // Configure HTTP bearer authorization: bearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new SyncApi(config);

  const body = {
    // string
    familyId: 38400000-8cf0-11bd-b23e-10b96e4ef00d,
    // number
    afterSeq: 789,
    // number (optional)
    limit: 56,
  } satisfies PullSyncEventsRequest;

  try {
    const data = await api.pullSyncEvents(body);
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
| **afterSeq** | `number` |  | [Defaults to `undefined`] |
| **limit** | `number` |  | [Optional] [Defaults to `500`] |

### Return type

[**SyncPullResponse**](SyncPullResponse.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`, `application/problem+json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Family events. |  -  |
| **0** | Error response. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)

