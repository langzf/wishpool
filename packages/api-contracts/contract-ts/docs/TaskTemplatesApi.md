# TaskTemplatesApi

All URIs are relative to *http://localhost:8080*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**createTaskTemplate**](TaskTemplatesApi.md#createtasktemplateoperation) | **POST** /task-templates | Create a task template. |
| [**listTaskTemplates**](TaskTemplatesApi.md#listtasktemplates) | **GET** /task-templates | List task templates for the current family. |



## createTaskTemplate

> TaskTemplate createTaskTemplate(createTaskTemplateRequest, idempotencyKey)

Create a task template.

### Example

```ts
import {
  Configuration,
  TaskTemplatesApi,
} from '@wishpool/api-client';
import type { CreateTaskTemplateOperationRequest } from '@wishpool/api-client';

async function example() {
  console.log("🚀 Testing @wishpool/api-client SDK...");
  const config = new Configuration({ 
    // Configure HTTP bearer authorization: bearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new TaskTemplatesApi(config);

  const body = {
    // CreateTaskTemplateRequest
    createTaskTemplateRequest: ...,
    // string (optional)
    idempotencyKey: idempotencyKey_example,
  } satisfies CreateTaskTemplateOperationRequest;

  try {
    const data = await api.createTaskTemplate(body);
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
| **createTaskTemplateRequest** | [CreateTaskTemplateRequest](CreateTaskTemplateRequest.md) |  | |
| **idempotencyKey** | `string` |  | [Optional] [Defaults to `undefined`] |

### Return type

[**TaskTemplate**](TaskTemplate.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

- **Content-Type**: `application/json`
- **Accept**: `application/json`, `application/problem+json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **201** | Template created. |  -  |
| **0** | Error response. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## listTaskTemplates

> Array&lt;TaskTemplate&gt; listTaskTemplates(familyId)

List task templates for the current family.

### Example

```ts
import {
  Configuration,
  TaskTemplatesApi,
} from '@wishpool/api-client';
import type { ListTaskTemplatesRequest } from '@wishpool/api-client';

async function example() {
  console.log("🚀 Testing @wishpool/api-client SDK...");
  const config = new Configuration({ 
    // Configure HTTP bearer authorization: bearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new TaskTemplatesApi(config);

  const body = {
    // string
    familyId: 38400000-8cf0-11bd-b23e-10b96e4ef00d,
  } satisfies ListTaskTemplatesRequest;

  try {
    const data = await api.listTaskTemplates(body);
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

[**Array&lt;TaskTemplate&gt;**](TaskTemplate.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`, `application/problem+json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Task templates. |  -  |
| **0** | Error response. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)

