
# AdminMediaAccessGrantRequest


## Properties

Name | Type
------------ | -------------
`familyId` | string
`mediaAssetId` | string
`reason` | string
`expiresInMinutes` | number

## Example

```typescript
import type { AdminMediaAccessGrantRequest } from '@wishpool/api-client'

// TODO: Update the object below with actual values
const example = {
  "familyId": null,
  "mediaAssetId": null,
  "reason": null,
  "expiresInMinutes": null,
} satisfies AdminMediaAccessGrantRequest

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as AdminMediaAccessGrantRequest
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


