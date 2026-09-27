
# AdminDashboard


## Properties

Name | Type
------------ | -------------
`generatedAt` | Date
`familyCount` | number
`activeChildCount` | number
`pendingReviewCount` | number
`pendingNotificationCount` | number
`pendingOutboxCount` | number
`processingMediaCount` | number
`runningAiJobCount` | number
`openPrivacyRequestCount` | number

## Example

```typescript
import type { AdminDashboard } from '@wishpool/api-client'

// TODO: Update the object below with actual values
const example = {
  "generatedAt": null,
  "familyCount": null,
  "activeChildCount": null,
  "pendingReviewCount": null,
  "pendingNotificationCount": null,
  "pendingOutboxCount": null,
  "processingMediaCount": null,
  "runningAiJobCount": null,
  "openPrivacyRequestCount": null,
} satisfies AdminDashboard

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as AdminDashboard
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


