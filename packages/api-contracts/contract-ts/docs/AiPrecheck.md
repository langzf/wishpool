
# AiPrecheck


## Properties

Name | Type
------------ | -------------
`id` | string
`submissionId` | string
`type` | string
`summary` | string
`confidence` | number
`flags` | Array&lt;{ [key: string]: any; }&gt;
`model` | { [key: string]: any; }

## Example

```typescript
import type { AiPrecheck } from '@wishpool/api-client'

// TODO: Update the object below with actual values
const example = {
  "id": null,
  "submissionId": null,
  "type": null,
  "summary": null,
  "confidence": null,
  "flags": null,
  "model": null,
} satisfies AiPrecheck

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as AiPrecheck
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


