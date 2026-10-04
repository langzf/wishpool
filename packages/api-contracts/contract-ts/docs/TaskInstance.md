
# TaskInstance


## Properties

Name | Type
------------ | -------------
`id` | string
`familyId` | string
`childId` | string
`scheduledDate` | Date
`title` | string
`category` | [TaskCategory](TaskCategory.md)
`submissionType` | [SubmissionType](SubmissionType.md)
`description` | string
`targetText` | string
`isCore` | boolean
`requireReview` | boolean
`status` | string
`latestSubmissionId` | string
`rewardAmount` | number

## Example

```typescript
import type { TaskInstance } from '@wishpool/api-client'

// TODO: Update the object below with actual values
const example = {
  "id": null,
  "familyId": null,
  "childId": null,
  "scheduledDate": null,
  "title": null,
  "category": null,
  "submissionType": null,
  "description": null,
  "targetText": null,
  "isCore": null,
  "requireReview": null,
  "status": null,
  "latestSubmissionId": null,
  "rewardAmount": null,
} satisfies TaskInstance

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as TaskInstance
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


