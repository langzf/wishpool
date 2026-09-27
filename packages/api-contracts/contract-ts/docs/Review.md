
# Review


## Properties

Name | Type
------------ | -------------
`id` | string
`submissionId` | string
`taskInstanceId` | string
`decision` | string
`reviewedBy` | string
`feedback` | [Feedback](Feedback.md)
`createdAt` | Date

## Example

```typescript
import type { Review } from '@wishpool/api-client'

// TODO: Update the object below with actual values
const example = {
  "id": null,
  "submissionId": null,
  "taskInstanceId": null,
  "decision": null,
  "reviewedBy": null,
  "feedback": null,
  "createdAt": null,
} satisfies Review

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as Review
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


