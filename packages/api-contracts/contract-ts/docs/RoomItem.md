
# RoomItem


## Properties

Name | Type
------------ | -------------
`id` | string
`childId` | string
`type` | string
`title` | string
`media` | [MediaAsset](MediaAsset.md)
`position` | { [key: string]: any; }
`visible` | boolean
`unlockedAt` | Date

## Example

```typescript
import type { RoomItem } from '@wishpool/api-client'

// TODO: Update the object below with actual values
const example = {
  "id": null,
  "childId": null,
  "type": null,
  "title": null,
  "media": null,
  "position": null,
  "visible": null,
  "unlockedAt": null,
} satisfies RoomItem

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as RoomItem
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


