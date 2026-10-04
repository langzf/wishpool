
# AdminMediaAccessGrantPage


## Properties

Name | Type
------------ | -------------
`items` | [Array&lt;AdminMediaAccessGrant&gt;](AdminMediaAccessGrant.md)
`total` | number
`offset` | number
`limit` | number
`hasMore` | boolean

## Example

```typescript
import type { AdminMediaAccessGrantPage } from '@wishpool/api-client'

// TODO: Update the object below with actual values
const example = {
  "items": null,
  "total": null,
  "offset": null,
  "limit": null,
  "hasMore": null,
} satisfies AdminMediaAccessGrantPage

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as AdminMediaAccessGrantPage
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


