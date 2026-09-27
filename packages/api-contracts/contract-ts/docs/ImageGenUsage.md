
# ImageGenUsage


## Properties

Name | Type
------------ | -------------
`usageCode` | string
`providerCode` | string
`providerDisplayName` | string
`createdAt` | Date
`updatedAt` | Date

## Example

```typescript
import type { ImageGenUsage } from '@wishpool/api-client'

// TODO: Update the object below with actual values
const example = {
  "usageCode": null,
  "providerCode": null,
  "providerDisplayName": null,
  "createdAt": null,
  "updatedAt": null,
} satisfies ImageGenUsage

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as ImageGenUsage
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


