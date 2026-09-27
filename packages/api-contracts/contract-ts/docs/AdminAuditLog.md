
# AdminAuditLog


## Properties

Name | Type
------------ | -------------
`id` | string
`familyId` | string
`actorUserId` | string
`actorRole` | string
`action` | string
`resourceType` | string
`resourceId` | string
`metadata` | { [key: string]: any; }
`createdAt` | Date

## Example

```typescript
import type { AdminAuditLog } from '@wishpool/api-client'

// TODO: Update the object below with actual values
const example = {
  "id": null,
  "familyId": null,
  "actorUserId": null,
  "actorRole": null,
  "action": null,
  "resourceType": null,
  "resourceId": null,
  "metadata": null,
  "createdAt": null,
} satisfies AdminAuditLog

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as AdminAuditLog
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


