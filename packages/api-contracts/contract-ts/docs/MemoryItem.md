
# MemoryItem


## Properties

Name | Type
------------ | -------------
`id` | string
`itemType` | string
`sourceType` | string
`sourceId` | string
`mediaAssetId` | string
`downloadUrl` | string
`contentType` | string
`thumbnailUrl` | string
`durationSec` | number
`taskTitle` | string
`submittedAt` | Date

## Example

```typescript
import type { MemoryItem } from '@wishpool/api-client'

// TODO: Update the object below with actual values
const example = {
  "id": null,
  "itemType": null,
  "sourceType": null,
  "sourceId": null,
  "mediaAssetId": null,
  "downloadUrl": null,
  "contentType": null,
  "thumbnailUrl": null,
  "durationSec": null,
  "taskTitle": null,
  "submittedAt": null,
} satisfies MemoryItem

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as MemoryItem
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


