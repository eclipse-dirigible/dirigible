# Include

The include directive makes it easy to fetch and insert an external HTML fragment inside an element. The request is restricted to the same domain, protocol and port as the application.

Part of the Harmonia Alpine.js component library. Every directive uses the `x-h-` prefix.

## Directive

- `x-h-include`

## API

### Arguments

| Attribute | Type   | Required | Description                         |
| --------- | ------ | -------- | ----------------------------------- |
| `self`    | string | true     | Relative path to the HTML fragment. |

### Attributes

| Attribute | Type    | Required | Description                                                                                                                                     |
| --------- | ------- | -------- | ----------------------------------------------------------------------------------------------------------------------------------------------- |
| data-js   | boolean | false    | By default, the directive does not execute any JavaScript code.<br />If the fragment includes a script that should execute, set this to `true`. |

### Modifiers

| Modifier  | Description           |
| --------- | --------------------- |
| <s>js</s> | Replaced by `data-js` |

### Events

| Event             | Bubbles | Detail    | Description                                                                                                    |
| ----------------- | ------- | --------- | -------------------------------------------------------------------------------------------------------------- |
| `fragment:loaded` | No      | `{ url }` | Dispatched on the element after the fragment is inserted into the DOM and Alpine has initialized the new tree. |

## Example

```html
<div x-h-include="'/harmonia/components/include/fragment.html'"></div>
```

More examples in the docs site: Reacting after load.

## Notes

- Directive values are Alpine expressions, so quote string literals: `x-h-...="'Label'"`.
- Components render only after Alpine has registered Harmonia. See SKILL.md for setup.
