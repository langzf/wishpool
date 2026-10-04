
# AdminMediaAccessGrant


## Properties

Name | Type
------------ | -------------
`id` | string
`mediaAssetId` | string
`familyId` | string
`accessUrl` | string
`expiresAt` | Date
`auditLogId` | string
`reason` | string
`revokedAt` | Date
`revokedReason` | string
`createdAt` | Date

## Example

```typescript
import type { AdminMediaAccessGrant } from '@wishpool/api-client'

// TODO: Update the object below with actual values
const example = {
  "id": null,
  "mediaAssetId": null,
  "familyId": null,
  "accessUrl": null,
  "expiresAt": null,
  "auditLogId": null,
  "reason": null,
  "revokedAt": null,
  "revokedReason": null,
  "createdAt": null,
} satisfies AdminMediaAccessGrant

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as AdminMediaAccessGrant
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


