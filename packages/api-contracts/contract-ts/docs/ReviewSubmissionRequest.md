
# ReviewSubmissionRequest


## Properties

Name | Type
------------ | -------------
`submissionId` | string
`decision` | string
`feedback` | [FeedbackInput](FeedbackInput.md)

## Example

```typescript
import type { ReviewSubmissionRequest } from '@wishpool/api-client'

// TODO: Update the object below with actual values
const example = {
  "submissionId": null,
  "decision": null,
  "feedback": null,
} satisfies ReviewSubmissionRequest

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as ReviewSubmissionRequest
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


