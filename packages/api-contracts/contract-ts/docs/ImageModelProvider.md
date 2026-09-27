
# ImageModelProvider


## Properties

Name | Type
------------ | -------------
`id` | string
`code` | string
`displayName` | string
`providerType` | string
`baseUrl` | string
`apiKeyMasked` | string
`modelName` | string
`extraParams` | { [key: string]: any; }
`isDefault` | boolean
`isEnabled` | boolean
`createdAt` | Date
`updatedAt` | Date

## Example

```typescript
import type { ImageModelProvider } from '@wishpool/api-client'

// TODO: Update the object below with actual values
const example = {
  "id": null,
  "code": null,
  "displayName": null,
  "providerType": null,
  "baseUrl": null,
  "apiKeyMasked": null,
  "modelName": null,
  "extraParams": null,
  "isDefault": null,
  "isEnabled": null,
  "createdAt": null,
  "updatedAt": null,
} satisfies ImageModelProvider

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as ImageModelProvider
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


