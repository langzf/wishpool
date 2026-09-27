
# PrivacyRequestCreate


## Properties

Name | Type
------------ | -------------
`familyId` | string
`requestType` | string
`confirmationText` | string
`reason` | string

## Example

```typescript
import type { PrivacyRequestCreate } from '@wishpool/api-client'

// TODO: Update the object below with actual values
const example = {
  "familyId": null,
  "requestType": null,
  "confirmationText": null,
  "reason": null,
} satisfies PrivacyRequestCreate

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as PrivacyRequestCreate
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


