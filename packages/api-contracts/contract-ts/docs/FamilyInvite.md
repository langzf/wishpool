
# FamilyInvite


## Properties

Name | Type
------------ | -------------
`id` | string
`familyId` | string
`status` | string
`expiresAt` | Date

## Example

```typescript
import type { FamilyInvite } from '@wishpool/api-client'

// TODO: Update the object below with actual values
const example = {
  "id": null,
  "familyId": null,
  "status": null,
  "expiresAt": null,
} satisfies FamilyInvite

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as FamilyInvite
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


