
# ConsumePairingCodeRequest


## Properties

Name | Type
------------ | -------------
`pairingCode` | string
`device` | [DeviceRegistration](DeviceRegistration.md)

## Example

```typescript
import type { ConsumePairingCodeRequest } from '@wishpool/api-client'

// TODO: Update the object below with actual values
const example = {
  "pairingCode": null,
  "device": null,
} satisfies ConsumePairingCodeRequest

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as ConsumePairingCodeRequest
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


