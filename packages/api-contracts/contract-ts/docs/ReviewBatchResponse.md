
# ReviewBatchResponse


## Properties

Name | Type
------------ | -------------
`summary` | [ReviewBatchResponseSummary](ReviewBatchResponseSummary.md)
`results` | [Array&lt;ReviewBatchItemResult&gt;](ReviewBatchItemResult.md)

## Example

```typescript
import type { ReviewBatchResponse } from '@wishpool/api-client'

// TODO: Update the object below with actual values
const example = {
  "summary": null,
  "results": null,
} satisfies ReviewBatchResponse

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as ReviewBatchResponse
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


