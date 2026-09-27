
# Wish


## Properties

Name | Type
------------ | -------------
`id` | string
`familyId` | string
`childId` | string
`weekId` | string
`title` | string
`note` | string
`imageMedia` | [MediaAsset](MediaAsset.md)
`requiredFragments` | number
`earnedFragments` | number
`rewardMode` | string
`status` | string
`fragmentVisual` | [WishFragmentVisual](WishFragmentVisual.md)

## Example

```typescript
import type { Wish } from '@wishpool/api-client'

// TODO: Update the object below with actual values
const example = {
  "id": null,
  "familyId": null,
  "childId": null,
  "weekId": null,
  "title": null,
  "note": null,
  "imageMedia": null,
  "requiredFragments": null,
  "earnedFragments": null,
  "rewardMode": null,
  "status": null,
  "fragmentVisual": null,
} satisfies Wish

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as Wish
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


