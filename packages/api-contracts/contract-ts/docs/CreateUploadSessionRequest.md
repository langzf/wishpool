
# CreateUploadSessionRequest


## Properties

Name | Type
------------ | -------------
`familyId` | string
`childId` | string
`purpose` | string
`contentType` | string
`sizeBytes` | number
`relatedResource` | [RelatedResource](RelatedResource.md)

## Example

```typescript
import type { CreateUploadSessionRequest } from '@wishpool/api-client'

// TODO: Update the object below with actual values
const example = {
  "familyId": null,
  "childId": null,
  "purpose": null,
  "contentType": null,
  "sizeBytes": null,
  "relatedResource": null,
} satisfies CreateUploadSessionRequest

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as CreateUploadSessionRequest
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


