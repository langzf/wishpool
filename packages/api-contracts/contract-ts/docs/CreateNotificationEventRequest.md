
# CreateNotificationEventRequest


## Properties

Name | Type
------------ | -------------
`familyId` | string
`recipientUserId` | string
`recipientDeviceId` | string
`type` | string
`title` | string
`body` | string
`relatedResourceType` | string
`relatedResourceId` | string
`dedupeKey` | string

## Example

```typescript
import type { CreateNotificationEventRequest } from '@wishpool/api-client'

// TODO: Update the object below with actual values
const example = {
  "familyId": null,
  "recipientUserId": null,
  "recipientDeviceId": null,
  "type": null,
  "title": null,
  "body": null,
  "relatedResourceType": null,
  "relatedResourceId": null,
  "dedupeKey": null,
} satisfies CreateNotificationEventRequest

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as CreateNotificationEventRequest
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


