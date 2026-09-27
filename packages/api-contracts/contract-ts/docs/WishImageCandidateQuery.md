
# WishImageCandidateQuery


## Properties

Name | Type
------------ | -------------
`normalizedTitle` | string
`keywords` | Array&lt;string&gt;
`category` | string

## Example

```typescript
import type { WishImageCandidateQuery } from '@wishpool/api-client'

// TODO: Update the object below with actual values
const example = {
  "normalizedTitle": null,
  "keywords": null,
  "category": null,
} satisfies WishImageCandidateQuery

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as WishImageCandidateQuery
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


