
# CreateTaskTemplateRequest


## Properties

Name | Type
------------ | -------------
`familyId` | string
`title` | string
`category` | [TaskCategory](TaskCategory.md)
`submissionType` | [SubmissionType](SubmissionType.md)
`description` | string
`targetText` | string
`defaultDurationSec` | number

## Example

```typescript
import type { CreateTaskTemplateRequest } from '@wishpool/api-client'

// TODO: Update the object below with actual values
const example = {
  "familyId": null,
  "title": null,
  "category": null,
  "submissionType": null,
  "description": null,
  "targetText": null,
  "defaultDurationSec": null,
} satisfies CreateTaskTemplateRequest

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as CreateTaskTemplateRequest
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


