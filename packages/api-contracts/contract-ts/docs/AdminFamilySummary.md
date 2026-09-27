
# AdminFamilySummary


## Properties

Name | Type
------------ | -------------
`id` | string
`name` | string
`timezone` | string
`status` | string
`childCount` | number
`memberCount` | number
`createdAt` | Date

## Example

```typescript
import type { AdminFamilySummary } from '@wishpool/api-client'

// TODO: Update the object below with actual values
const example = {
  "id": null,
  "name": null,
  "timezone": null,
  "status": null,
  "childCount": null,
  "memberCount": null,
  "createdAt": null,
} satisfies AdminFamilySummary

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as AdminFamilySummary
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


