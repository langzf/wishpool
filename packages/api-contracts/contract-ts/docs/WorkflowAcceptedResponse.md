
# WorkflowAcceptedResponse


## Properties

Name | Type
------------ | -------------
`workflow` | string
`status` | string
`acceptedAt` | Date

## Example

```typescript
import type { WorkflowAcceptedResponse } from '@wishpool/api-client'

// TODO: Update the object below with actual values
const example = {
  "workflow": null,
  "status": null,
  "acceptedAt": null,
} satisfies WorkflowAcceptedResponse

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as WorkflowAcceptedResponse
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


