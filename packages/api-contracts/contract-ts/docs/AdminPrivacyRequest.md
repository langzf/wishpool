
# AdminPrivacyRequest


## Properties

Name | Type
------------ | -------------
`id` | string
`familyId` | string
`requestType` | string
`status` | string
`requestedBy` | string
`exportMediaId` | string
`reason` | string
`createdAt` | Date
`completedAt` | Date
`errorMessage` | string

## Example

```typescript
import type { AdminPrivacyRequest } from '@wishpool/api-client'

// TODO: Update the object below with actual values
const example = {
  "id": null,
  "familyId": null,
  "requestType": null,
  "status": null,
  "requestedBy": null,
  "exportMediaId": null,
  "reason": null,
  "createdAt": null,
  "completedAt": null,
  "errorMessage": null,
} satisfies AdminPrivacyRequest

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as AdminPrivacyRequest
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


