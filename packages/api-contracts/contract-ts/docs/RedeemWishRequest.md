
# RedeemWishRequest


## Properties

Name | Type
------------ | -------------
`redeemedDate` | Date
`photoMediaIds` | Array&lt;string&gt;
`parentNote` | string
`childNote` | string

## Example

```typescript
import type { RedeemWishRequest } from '@wishpool/api-client'

// TODO: Update the object below with actual values
const example = {
  "redeemedDate": null,
  "photoMediaIds": null,
  "parentNote": null,
  "childNote": null,
} satisfies RedeemWishRequest

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as RedeemWishRequest
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


