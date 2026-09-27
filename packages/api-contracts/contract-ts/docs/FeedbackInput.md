
# FeedbackInput


## Properties

Name | Type
------------ | -------------
`emoji` | string
`text` | string
`audioMediaId` | string

## Example

```typescript
import type { FeedbackInput } from '@wishpool/api-client'

// TODO: Update the object below with actual values
const example = {
  "emoji": null,
  "text": null,
  "audioMediaId": null,
} satisfies FeedbackInput

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as FeedbackInput
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


