
# CreateWishRequest


## Properties

Name | Type
------------ | -------------
`familyId` | string
`childId` | string
`weekId` | string
`title` | string
`note` | string
`imageMediaId` | string
`requiredFragments` | number
`rewardMode` | string
`fragmentVisualMode` | string
`fragmentGridRows` | number
`fragmentGridCols` | number

## Example

```typescript
import type { CreateWishRequest } from '@wishpool/api-client'

// TODO: Update the object below with actual values
const example = {
  "familyId": null,
  "childId": null,
  "weekId": null,
  "title": null,
  "note": null,
  "imageMediaId": null,
  "requiredFragments": null,
  "rewardMode": null,
  "fragmentVisualMode": null,
  "fragmentGridRows": null,
  "fragmentGridCols": null,
} satisfies CreateWishRequest

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as CreateWishRequest
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


