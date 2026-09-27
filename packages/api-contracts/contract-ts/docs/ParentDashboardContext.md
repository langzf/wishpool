
# ParentDashboardContext


## Properties

Name | Type
------------ | -------------
`family` | [Family](Family.md)
`children` | [Array&lt;ChildProfile&gt;](ChildProfile.md)
`selectedChild` | [ChildProfile](ChildProfile.md)
`today` | [TodaySnapshot](TodaySnapshot.md)
`currentWish` | [Wish](Wish.md)
`weeklyPlan` | [WeeklyPlan](WeeklyPlan.md)
`pendingReviews` | [Array&lt;PendingReviewCard&gt;](PendingReviewCard.md)
`taskTemplates` | [Array&lt;TaskTemplate&gt;](TaskTemplate.md)
`wishHistory` | [Array&lt;WishHistoryItem&gt;](WishHistoryItem.md)
`memories` | [Array&lt;WeeklyMemory&gt;](WeeklyMemory.md)
`room` | [RoomState](RoomState.md)
`notificationInbox` | [NotificationListResponse](NotificationListResponse.md)

## Example

```typescript
import type { ParentDashboardContext } from '@wishpool/api-client'

// TODO: Update the object below with actual values
const example = {
  "family": null,
  "children": null,
  "selectedChild": null,
  "today": null,
  "currentWish": null,
  "weeklyPlan": null,
  "pendingReviews": null,
  "taskTemplates": null,
  "wishHistory": null,
  "memories": null,
  "room": null,
  "notificationInbox": null,
} satisfies ParentDashboardContext

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as ParentDashboardContext
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


