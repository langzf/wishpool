
# WeeklyPlan


## Properties

Name | Type
------------ | -------------
`id` | string
`familyId` | string
`childId` | string
`weekId` | string
`startDate` | Date
`endDate` | Date
`rewardMode` | string
`status` | string
`rules` | [Array&lt;WeeklyPlanRule&gt;](WeeklyPlanRule.md)

## Example

```typescript
import type { WeeklyPlan } from '@wishpool/api-client'

// TODO: Update the object below with actual values
const example = {
  "id": null,
  "familyId": null,
  "childId": null,
  "weekId": null,
  "startDate": null,
  "endDate": null,
  "rewardMode": null,
  "status": null,
  "rules": null,
} satisfies WeeklyPlan

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as WeeklyPlan
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


