
# NotificationPreference


## Properties

Name | Type
------------ | -------------
`id` | string
`familyId` | string
`userId` | string
`notificationType` | string
`enabled` | boolean
`quietHours` | { [key: string]: any; }
`channels` | { [key: string]: any; }
`updatedAt` | Date

## Example

```typescript
import type { NotificationPreference } from '@wishpool/api-client'

// TODO: Update the object below with actual values
const example = {
  "id": null,
  "familyId": null,
  "userId": null,
  "notificationType": null,
  "enabled": null,
  "quietHours": null,
  "channels": null,
  "updatedAt": null,
} satisfies NotificationPreference

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as NotificationPreference
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


