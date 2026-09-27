
# UploadSession


## Properties

Name | Type
------------ | -------------
`mediaId` | string
`uploadUrl` | string
`storageKey` | string
`expiresAt` | Date
`maxSizeBytes` | number

## Example

```typescript
import type { UploadSession } from '@wishpool/api-client'

// TODO: Update the object below with actual values
const example = {
  "mediaId": null,
  "uploadUrl": null,
  "storageKey": null,
  "expiresAt": null,
  "maxSizeBytes": null,
} satisfies UploadSession

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as UploadSession
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


