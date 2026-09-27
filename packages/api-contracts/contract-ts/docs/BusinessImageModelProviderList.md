
# BusinessImageModelProviderList


## Properties

Name | Type
------------ | -------------
`usageCode` | string
`selectedProviderCode` | string
`providers` | [Array&lt;ImageModelProvider&gt;](ImageModelProvider.md)

## Example

```typescript
import type { BusinessImageModelProviderList } from '@wishpool/api-client'

// TODO: Update the object below with actual values
const example = {
  "usageCode": null,
  "selectedProviderCode": null,
  "providers": null,
} satisfies BusinessImageModelProviderList

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as BusinessImageModelProviderList
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


