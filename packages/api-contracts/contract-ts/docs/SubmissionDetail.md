
# SubmissionDetail


## Properties

Name | Type
------------ | -------------
`id` | string
`familyId` | string
`childId` | string
`taskInstanceId` | string
`attemptNo` | number
`submissionType` | [SubmissionType](SubmissionType.md)
`status` | string
`submittedAt` | Date
`task` | [TaskInstance](TaskInstance.md)
`media` | [Array&lt;MediaAsset&gt;](MediaAsset.md)
`aiPrecheck` | [AiPrecheck](AiPrecheck.md)
`review` | [Review](Review.md)

## Example

```typescript
import type { SubmissionDetail } from '@wishpool/api-client'

// TODO: Update the object below with actual values
const example = {
  "id": null,
  "familyId": null,
  "childId": null,
  "taskInstanceId": null,
  "attemptNo": null,
  "submissionType": null,
  "status": null,
  "submittedAt": null,
  "task": null,
  "media": null,
  "aiPrecheck": null,
  "review": null,
} satisfies SubmissionDetail

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as SubmissionDetail
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


