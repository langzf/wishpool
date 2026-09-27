
# NotificationDevice


## Properties

Name | Type
------------ | -------------
`id` | string
`platform` | string
`deviceName` | string
`pushProvider` | string
`pushToken` | string

## Example

```typescript
import type { NotificationDevice } from '@wishpool/api-client'

// TODO: Update the object below with actual values
const example = {
  "id": null,
  "platform": null,
  "deviceName": null,
  "pushProvider": null,
  "pushToken": null,
} satisfies NotificationDevice

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as NotificationDevice
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


