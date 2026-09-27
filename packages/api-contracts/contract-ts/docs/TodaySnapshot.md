
# TodaySnapshot


## Properties

Name | Type
------------ | -------------
`child` | [ChildProfile](ChildProfile.md)
`date` | Date
`tasks` | [Array&lt;TaskInstance&gt;](TaskInstance.md)
`dailySummary` | [DailySummary](DailySummary.md)
`currentWish` | [Wish](Wish.md)

## Example

```typescript
import type { TodaySnapshot } from '@wishpool/api-client'

// TODO: Update the object below with actual values
const example = {
  "child": null,
  "date": null,
  "tasks": null,
  "dailySummary": null,
  "currentWish": null,
} satisfies TodaySnapshot

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as TodaySnapshot
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


