
# FamilyEvent


## Properties

Name | Type
------------ | -------------
`seq` | number
`familyId` | string
`type` | string
`aggregateType` | string
`aggregateId` | string
`occurredAt` | Date
`payload` | { [key: string]: any; }

## Example

```typescript
import type { FamilyEvent } from '@wishpool/api-client'

// TODO: Update the object below with actual values
const example = {
  "seq": null,
  "familyId": null,
  "type": null,
  "aggregateType": null,
  "aggregateId": null,
  "occurredAt": null,
  "payload": null,
} satisfies FamilyEvent

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as FamilyEvent
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


