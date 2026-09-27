
# PairingSessionCreated


## Properties

Name | Type
------------ | -------------
`sessionId` | string
`pairingCode` | string
`expiresAt` | Date

## Example

```typescript
import type { PairingSessionCreated } from '@wishpool/api-client'

// TODO: Update the object below with actual values
const example = {
  "sessionId": null,
  "pairingCode": null,
  "expiresAt": null,
} satisfies PairingSessionCreated

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as PairingSessionCreated
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


