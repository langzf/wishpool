
# WishImageCandidateRequest


## Properties

Name | Type
------------ | -------------
`familyId` | string
`childId` | string
`title` | string
`note` | string
`limit` | number

## Example

```typescript
import type { WishImageCandidateRequest } from '@wishpool/api-client'

// TODO: Update the object below with actual values
const example = {
  "familyId": null,
  "childId": null,
  "title": null,
  "note": null,
  "limit": null,
} satisfies WishImageCandidateRequest

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as WishImageCandidateRequest
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


