
# WishFragmentMask


## Properties

Name | Type
------------ | -------------
`version` | number
`mode` | string
`rows` | number
`cols` | number
`total` | number
`revealOrder` | Array&lt;number&gt;
`cells` | [Array&lt;WishFragmentCell&gt;](WishFragmentCell.md)

## Example

```typescript
import type { WishFragmentMask } from '@wishpool/api-client'

// TODO: Update the object below with actual values
const example = {
  "version": null,
  "mode": null,
  "rows": null,
  "cols": null,
  "total": null,
  "revealOrder": null,
  "cells": null,
} satisfies WishFragmentMask

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as WishFragmentMask
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


