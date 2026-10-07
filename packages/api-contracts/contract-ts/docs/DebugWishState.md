
# DebugWishState


## Properties

Name | Type
------------ | -------------
`wishId` | string
`familyId` | string
`childId` | string
`earnedFragments` | number
`requiredFragments` | number
`status` | string
`fragmentMask` | string
`auditId` | string

## Example

```typescript
import type { DebugWishState } from '@wishpool/api-client'

// TODO: Update the object below with actual values
const example = {
  "wishId": null,
  "familyId": null,
  "childId": null,
  "earnedFragments": null,
  "requiredFragments": null,
  "status": null,
  "fragmentMask": null,
  "auditId": null,
} satisfies DebugWishState

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as DebugWishState
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


