
# UpdateNotificationPreferenceRequest


## Properties

Name | Type
------------ | -------------
`familyId` | string
`notificationType` | string
`enabled` | boolean
`quietHours` | { [key: string]: any; }
`channels` | { [key: string]: any; }

## Example

```typescript
import type { UpdateNotificationPreferenceRequest } from '@wishpool/api-client'

// TODO: Update the object below with actual values
const example = {
  "familyId": null,
  "notificationType": null,
  "enabled": null,
  "quietHours": null,
  "channels": null,
} satisfies UpdateNotificationPreferenceRequest

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as UpdateNotificationPreferenceRequest
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


