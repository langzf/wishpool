
# ReviewBatchRequest


## Properties

Name | Type
------------ | -------------
`clientMutationId` | string
`items` | [Array&lt;ReviewBatchItem&gt;](ReviewBatchItem.md)

## Example

```typescript
import type { ReviewBatchRequest } from '@wishpool/api-client'

// TODO: Update the object below with actual values
const example = {
  "clientMutationId": null,
  "items": null,
} satisfies ReviewBatchRequest

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as ReviewBatchRequest
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


