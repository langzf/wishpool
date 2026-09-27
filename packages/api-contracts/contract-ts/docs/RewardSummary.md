
# RewardSummary


## Properties

Name | Type
------------ | -------------
`childId` | string
`weekId` | string
`fromDate` | Date
`toDate` | Date
`starLight` | number
`wishFragment` | number
`adjustment` | number
`total` | number

## Example

```typescript
import type { RewardSummary } from '@wishpool/api-client'

// TODO: Update the object below with actual values
const example = {
  "childId": null,
  "weekId": null,
  "fromDate": null,
  "toDate": null,
  "starLight": null,
  "wishFragment": null,
  "adjustment": null,
  "total": null,
} satisfies RewardSummary

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as RewardSummary
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


