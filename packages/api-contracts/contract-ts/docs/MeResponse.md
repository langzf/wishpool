
# MeResponse


## Properties

Name | Type
------------ | -------------
`user` | [User](User.md)
`families` | [Array&lt;FamilyMemberContext&gt;](FamilyMemberContext.md)

## Example

```typescript
import type { MeResponse } from '@wishpool/api-client'

// TODO: Update the object below with actual values
const example = {
  "user": null,
  "families": null,
} satisfies MeResponse

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as MeResponse
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


