
# PhoneCodeCreated


## Properties

Name | Type
------------ | -------------
`verificationToken` | string
`expiresAt` | Date
`debugCode` | string

## Example

```typescript
import type { PhoneCodeCreated } from '@wishpool/api-client'

// TODO: Update the object below with actual values
const example = {
  "verificationToken": null,
  "expiresAt": null,
  "debugCode": null,
} satisfies PhoneCodeCreated

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as PhoneCodeCreated
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


