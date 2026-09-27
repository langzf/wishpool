
# CreateWishImageGenerationRequest


## Properties

Name | Type
------------ | -------------
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

## Example

```typescript
import type { CreateWishImageGenerationRequest } from '@wishpool/api-client'

// TODO: Update the object below with actual values
const example = {
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
} satisfies CreateWishImageGenerationRequest

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as CreateWishImageGenerationRequest
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


