
# NotificationEvent


## Properties

Name | Type
------------ | -------------
`id` | string
`familyId` | string
`recipientUserId` | string
`recipientDeviceId` | string
`type` | string
`title` | string
`body` | string
`relatedResourceType` | string
`relatedResourceId` | string
`status` | string
`errorMessage` | string
`providerMessageId` | string
`sentAt` | Date
`readAt` | Date
`createdAt` | Date

## Example

```typescript
import type { NotificationEvent } from '@wishpool/api-client'

// TODO: Update the object below with actual values
const example = {
  "id": null,
  "familyId": null,
  "recipientUserId": null,
  "recipientDeviceId": null,
  "type": null,
  "title": null,
  "body": null,
  "relatedResourceType": null,
  "relatedResourceId": null,
  "status": null,
  "errorMessage": null,
  "providerMessageId": null,
  "sentAt": null,
  "readAt": null,
  "createdAt": null,
} satisfies NotificationEvent

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as NotificationEvent
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


