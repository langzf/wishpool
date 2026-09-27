
# SaveWeeklyPlanRequest


## Properties

Name | Type
------------ | -------------
`familyId` | string
`childId` | string
`weekId` | string
`startDate` | Date
`endDate` | Date
`rewardMode` | string
`wishId` | string
`rules` | [Array&lt;WeeklyPlanRuleInput&gt;](WeeklyPlanRuleInput.md)

## Example

```typescript
import type { SaveWeeklyPlanRequest } from '@wishpool/api-client'

// TODO: Update the object below with actual values
const example = {
  "familyId": null,
  "childId": null,
  "weekId": null,
  "startDate": null,
  "endDate": null,
  "rewardMode": null,
  "wishId": null,
  "rules": null,
} satisfies SaveWeeklyPlanRequest

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as SaveWeeklyPlanRequest
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


