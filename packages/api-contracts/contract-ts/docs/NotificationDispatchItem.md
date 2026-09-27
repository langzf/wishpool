
# NotificationDispatchItem


## Properties

Name | Type
------------ | -------------
`notification` | [NotificationEvent](NotificationEvent.md)
`recipientDevice` | [NotificationDevice](NotificationDevice.md)
`preference` | [NotificationPreference](NotificationPreference.md)

## Example

```typescript
import type { NotificationDispatchItem } from '@wishpool/api-client'

// TODO: Update the object below with actual values
const example = {
  "notification": null,
  "recipientDevice": null,
  "preference": null,
} satisfies NotificationDispatchItem

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as NotificationDispatchItem
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


