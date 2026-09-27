
# ChildHomeContext


## Properties

Name | Type
------------ | -------------
`child` | [ChildProfile](ChildProfile.md)
`today` | [TodaySnapshot](TodaySnapshot.md)
`currentWish` | [Wish](Wish.md)
`room` | [RoomState](RoomState.md)
`latestMemory` | [WeeklyMemory](WeeklyMemory.md)
`latestFeedback` | [ChildFeedbackCard](ChildFeedbackCard.md)
`unreadNotifications` | number

## Example

```typescript
import type { ChildHomeContext } from '@wishpool/api-client'

// TODO: Update the object below with actual values
const example = {
  "child": null,
  "today": null,
  "currentWish": null,
  "room": null,
  "latestMemory": null,
  "latestFeedback": null,
  "unreadNotifications": null,
} satisfies ChildHomeContext

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as ChildHomeContext
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


