
# ImageModelProviderWriteRequest


## Properties

Name | Type
------------ | -------------
`code` | string
`displayName` | string
`providerType` | string
`baseUrl` | string
`apiKey` | string
`modelName` | string
`extraParams` | { [key: string]: any; }
`isDefault` | boolean
`isEnabled` | boolean

## Example

```typescript
import type { ImageModelProviderWriteRequest } from '@wishpool/api-client'

// TODO: Update the object below with actual values
const example = {
  "code": null,
  "displayName": null,
  "providerType": null,
  "baseUrl": null,
  "apiKey": null,
  "modelName": null,
  "extraParams": null,
  "isDefault": null,
  "isEnabled": null,
} satisfies ImageModelProviderWriteRequest

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as ImageModelProviderWriteRequest
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


