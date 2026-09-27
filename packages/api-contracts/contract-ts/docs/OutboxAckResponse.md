
# OutboxAckResponse


## Properties

Name | Type
------------ | -------------
`id` | string
`status` | string
`publishedAt` | Date

## Example

```typescript
import type { OutboxAckResponse } from '@wishpool/api-client'

// TODO: Update the object below with actual values
const example = {
  "id": null,
  "status": null,
  "publishedAt": null,
} satisfies OutboxAckResponse

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as OutboxAckResponse
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


