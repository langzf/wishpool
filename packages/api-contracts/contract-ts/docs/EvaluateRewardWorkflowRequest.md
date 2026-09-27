
# EvaluateRewardWorkflowRequest


## Properties

Name | Type
------------ | -------------
`eventType` | string
`taskInstanceId` | string
`reviewId` | string
`actorUserId` | string
`triggeredByEventId` | string

## Example

```typescript
import type { EvaluateRewardWorkflowRequest } from '@wishpool/api-client'

// TODO: Update the object below with actual values
const example = {
  "eventType": null,
  "taskInstanceId": null,
  "reviewId": null,
  "actorUserId": null,
  "triggeredByEventId": null,
} satisfies EvaluateRewardWorkflowRequest

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as EvaluateRewardWorkflowRequest
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


