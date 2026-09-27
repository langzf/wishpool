
# OutboxEvent


## Properties

Name | Type
------------ | -------------
`id` | string
`type` | string
`aggregateType` | string
`aggregateId` | string
`payload` | { [key: string]: any; }
`availableAt` | Date
`leasedUntil` | Date
`retryCount` | number
`createdAt` | Date

## Example

```typescript
import type { OutboxEvent } from '@wishpool/api-client'

// TODO: Update the object below with actual values
const example = {
  "id": null,
  "type": null,
  "aggregateType": null,
  "aggregateId": null,
  "payload": null,
  "availableAt": null,
  "leasedUntil": null,
  "retryCount": null,
  "createdAt": null,
} satisfies OutboxEvent

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as OutboxEvent
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


