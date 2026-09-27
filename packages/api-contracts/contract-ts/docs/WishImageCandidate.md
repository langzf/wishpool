
# WishImageCandidate


## Properties

Name | Type
------------ | -------------
`media` | [MediaAsset](MediaAsset.md)
`score` | number
`reason` | string
`sourceWishId` | string
`sourceWishTitle` | string
`lastUsedAt` | Date

## Example

```typescript
import type { WishImageCandidate } from '@wishpool/api-client'

// TODO: Update the object below with actual values
const example = {
  "media": null,
  "score": null,
  "reason": null,
  "sourceWishId": null,
  "sourceWishTitle": null,
  "lastUsedAt": null,
} satisfies WishImageCandidate

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as WishImageCandidate
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


