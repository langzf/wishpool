
# WishImageGenerationJob


## Properties

Name | Type
------------ | -------------
`id` | string
`familyId` | string
`childId` | string
`wishId` | string
`usageCode` | string
`providerCode` | string
`title` | string
`note` | string
`category` | string
`style` | string
`aspectRatio` | string
`status` | string
`mediaAssetId` | string
`media` | [MediaAsset](MediaAsset.md)
`modelProvider` | string
`modelName` | string
`attemptCount` | number
`errorCode` | string
`errorMessage` | string
`createdAt` | Date
`updatedAt` | Date

## Example

```typescript
import type { WishImageGenerationJob } from '@wishpool/api-client'

// TODO: Update the object below with actual values
const example = {
  "id": null,
  "familyId": null,
  "childId": null,
  "wishId": null,
  "usageCode": null,
  "providerCode": null,
  "title": null,
  "note": null,
  "category": null,
  "style": null,
  "aspectRatio": null,
  "status": null,
  "mediaAssetId": null,
  "media": null,
  "modelProvider": null,
  "modelName": null,
  "attemptCount": null,
  "errorCode": null,
  "errorMessage": null,
  "createdAt": null,
  "updatedAt": null,
} satisfies WishImageGenerationJob

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as WishImageGenerationJob
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


