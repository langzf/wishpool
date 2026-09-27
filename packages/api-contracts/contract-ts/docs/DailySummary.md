
# DailySummary


## Properties

Name | Type
------------ | -------------
`date` | Date
`coreRequired` | number
`coreApproved` | number
`coreSkipped` | number
`fragmentStatus` | string

## Example

```typescript
import type { DailySummary } from '@wishpool/api-client'

// TODO: Update the object below with actual values
const example = {
  "date": null,
  "coreRequired": null,
  "coreApproved": null,
  "coreSkipped": null,
  "fragmentStatus": null,
} satisfies DailySummary

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as DailySummary
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


