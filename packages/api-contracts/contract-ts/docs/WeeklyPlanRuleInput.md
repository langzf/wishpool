
# WeeklyPlanRuleInput


## Properties

Name | Type
------------ | -------------
`taskTemplateId` | string
`title` | string
`category` | [TaskCategory](TaskCategory.md)
`submissionType` | [SubmissionType](SubmissionType.md)
`description` | string
`targetText` | string
`weekdays` | Array&lt;number&gt;
`isCore` | boolean
`requireReview` | boolean
`sortOrder` | number

## Example

```typescript
import type { WeeklyPlanRuleInput } from '@wishpool/api-client'

// TODO: Update the object below with actual values
const example = {
  "taskTemplateId": null,
  "title": null,
  "category": null,
  "submissionType": null,
  "description": null,
  "targetText": null,
  "weekdays": null,
  "isCore": null,
  "requireReview": null,
  "sortOrder": null,
} satisfies WeeklyPlanRuleInput

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as WeeklyPlanRuleInput
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


