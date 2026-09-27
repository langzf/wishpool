# AuthApi

All URIs are relative to *http://localhost:8080*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**getMe**](AuthApi.md#getme) | **GET** /me | Get the current authenticated user context. |
| [**login**](AuthApi.md#loginoperation) | **POST** /auth/login | Log in a parent user with phone or provider token. |
| [**refreshSession**](AuthApi.md#refreshsession) | **POST** /auth/refresh | Refresh an access token. |
| [**requestPhoneCode**](AuthApi.md#requestphonecode) | **POST** /auth/phone-codes | Request a phone verification code for parent login. |



## getMe

> MeResponse getMe()

Get the current authenticated user context.

### Example

```ts
import {
  Configuration,
  AuthApi,
} from '@wishpool/api-client';
import type { GetMeRequest } from '@wishpool/api-client';

async function example() {
  console.log("🚀 Testing @wishpool/api-client SDK...");
  const config = new Configuration({ 
    // Configure HTTP bearer authorization: bearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new AuthApi(config);

  try {
    const data = await api.getMe();
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

[**MeResponse**](MeResponse.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`, `application/problem+json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Current user context. |  -  |
| **0** | Error response. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## login

> AuthTokenPair login(loginRequest)

Log in a parent user with phone or provider token.

### Example

```ts
import {
  Configuration,
  AuthApi,
} from '@wishpool/api-client';
import type { LoginOperationRequest } from '@wishpool/api-client';

async function example() {
  console.log("🚀 Testing @wishpool/api-client SDK...");
  const api = new AuthApi();

  const body = {
    // LoginRequest
    loginRequest: ...,
  } satisfies LoginOperationRequest;

  try {
    const data = await api.login(body);
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
| **loginRequest** | [LoginRequest](LoginRequest.md) |  | |

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
| **200** | Login succeeded. |  -  |
| **0** | Error response. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## refreshSession

> AuthTokenPair refreshSession(refreshRequest)

Refresh an access token.

### Example

```ts
import {
  Configuration,
  AuthApi,
} from '@wishpool/api-client';
import type { RefreshSessionRequest } from '@wishpool/api-client';

async function example() {
  console.log("🚀 Testing @wishpool/api-client SDK...");
  const api = new AuthApi();

  const body = {
    // RefreshRequest
    refreshRequest: ...,
  } satisfies RefreshSessionRequest;

  try {
    const data = await api.refreshSession(body);
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
| **refreshRequest** | [RefreshRequest](RefreshRequest.md) |  | |

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
| **200** | Token refreshed. |  -  |
| **0** | Error response. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## requestPhoneCode

> PhoneCodeCreated requestPhoneCode(phoneCodeRequest)

Request a phone verification code for parent login.

### Example

```ts
import {
  Configuration,
  AuthApi,
} from '@wishpool/api-client';
import type { RequestPhoneCodeRequest } from '@wishpool/api-client';

async function example() {
  console.log("🚀 Testing @wishpool/api-client SDK...");
  const api = new AuthApi();

  const body = {
    // PhoneCodeRequest
    phoneCodeRequest: ...,
  } satisfies RequestPhoneCodeRequest;

  try {
    const data = await api.requestPhoneCode(body);
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
| **phoneCodeRequest** | [PhoneCodeRequest](PhoneCodeRequest.md) |  | |

### Return type

[**PhoneCodeCreated**](PhoneCodeCreated.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: `application/json`
- **Accept**: `application/json`, `application/problem+json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Phone verification code created. |  -  |
| **0** | Error response. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)

