
# WeeklyMemory


## Properties

Name | Type
------------ | -------------
`id` | string
`childId` | string
`weekId` | string
`title` | string
`summary` | { [key: string]: any; }
`items` | [Array&lt;MemoryItem&gt;](MemoryItem.md)
`status` | string

## Example

```typescript
import type { WeeklyMemory } from '@wishpool/api-client'

// TODO: Update the object below with actual values
const example = {
  "id": null,
  "childId": null,
  "weekId": null,
  "title": null,
  "summary": null,
  "items": null,
  "status": null,
} satisfies WeeklyMemory

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as WeeklyMemory
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


