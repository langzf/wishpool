
# WishRedemption


## Properties

Name | Type
------------ | -------------
`id` | string
`wishId` | string
`redeemedDate` | Date
`parentNote` | string
`childNote` | string
`photos` | [Array&lt;MediaAsset&gt;](MediaAsset.md)
`createdAt` | Date

## Example

```typescript
import type { WishRedemption } from '@wishpool/api-client'

// TODO: Update the object below with actual values
const example = {
  "id": null,
  "wishId": null,
  "redeemedDate": null,
  "parentNote": null,
  "childNote": null,
  "photos": null,
  "createdAt": null,
} satisfies WishRedemption

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as WishRedemption
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


