
# ClaimOutboxEventsRequest


## Properties

Name | Type
------------ | -------------
`limit` | number
`leaseSeconds` | number

## Example

```typescript
import type { ClaimOutboxEventsRequest } from '@wishpool/api-client'

// TODO: Update the object below with actual values
const example = {
  "limit": null,
  "leaseSeconds": null,
} satisfies ClaimOutboxEventsRequest

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as ClaimOutboxEventsRequest
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


