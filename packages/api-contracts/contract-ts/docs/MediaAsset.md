
# MediaAsset


## Properties

Name | Type
------------ | -------------
`id` | string
`familyId` | string
`childId` | string
`purpose` | string
`storageKey` | string
`contentType` | string
`status` | string
`downloadUrl` | string

## Example

```typescript
import type { MediaAsset } from '@wishpool/api-client'

// TODO: Update the object below with actual values
const example = {
  "id": null,
  "familyId": null,
  "childId": null,
  "purpose": null,
  "storageKey": null,
  "contentType": null,
  "status": null,
  "downloadUrl": null,
} satisfies MediaAsset

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as MediaAsset
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


