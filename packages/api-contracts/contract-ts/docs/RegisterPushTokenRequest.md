
# RegisterPushTokenRequest


## Properties

Name | Type
------------ | -------------
`familyId` | string
`platform` | string
`pushProvider` | string
`pushToken` | string
`deviceName` | string

## Example

```typescript
import type { RegisterPushTokenRequest } from '@wishpool/api-client'

// TODO: Update the object below with actual values
const example = {
  "familyId": null,
  "platform": null,
  "pushProvider": null,
  "pushToken": null,
  "deviceName": null,
} satisfies RegisterPushTokenRequest

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as RegisterPushTokenRequest
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


