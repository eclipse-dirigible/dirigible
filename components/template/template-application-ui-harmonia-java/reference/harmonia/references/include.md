# Include

The include directive makes it easy to fetch and insert an external HTML fragment inside an element. The request is restricted to the same domain, protocol and port as the application.

Part of the Harmonia Alpine.js component library. Every directive uses the `x-h-` prefix.

## Usage

Use `x-h-include` to compose a page from smaller same-origin HTML fragments, for example reusable headers, footers, or partial views loaded on demand. Point it at a relative path to the fragment; by default any script in the fragment is ignored, so set `data-js` only when you trust the source and need its scripts to run.

> **Warning:**
>
> - Only use on trusted content and never on dynamic/user-provided content!<br />
> - Dynamically rendering HTML from third parties can easily lead to XSS vulnerabilities.<br />
> - Executing untrusted JavaScript code poses significant security risks and should be strictly avoided.
>

> **Note:**
> The directive executes before any binding.

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

## Examples

```html
<div x-h-include="'/harmonia/components/include/fragment.html'"></div>
```

### Reacting after load

Because `fragment:loaded` does not bubble, attach the listener directly to the element:

```html
<div x-h-include="'/harmonia/components/include/fragment.html'" @fragment:loaded="onFragmentLoaded($event.detail.url)"></div>
```

Or in plain JavaScript:

```js
const el = document.querySelector('#my-include');
el.addEventListener('fragment:loaded', (e) => {
  console.log('Loaded:', e.detail.url);
});
```

Full docs: https://www.codbex.com/harmonia/utilities/include.html

## Notes

- Directive values are Alpine expressions, so quote string literals: `x-h-...="'Label'"`.
- Components render only after Alpine has registered Harmonia. See SKILL.md for setup.
