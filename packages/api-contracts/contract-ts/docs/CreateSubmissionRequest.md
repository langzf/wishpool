
# CreateSubmissionRequest


## Properties

Name | Type
------------ | -------------
`taskInstanceId` | string
`clientMutationId` | string
`mediaAssetIds` | Array&lt;string&gt;
`submittedAtClient` | Date

## Example

```typescript
import type { CreateSubmissionRequest } from '@wishpool/api-client'

// TODO: Update the object below with actual values
const example = {
  "taskInstanceId": null,
  "clientMutationId": null,
  "mediaAssetIds": null,
  "submittedAtClient": null,
} satisfies CreateSubmissionRequest

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as CreateSubmissionRequest
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


