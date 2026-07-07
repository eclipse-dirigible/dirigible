# Alert

Communicates important information to the user about a situation or task that requires attention. Alerts can be used to highlight status changes or show critical messages.

Part of the Harmonia Alpine.js component library. Every directive uses the `x-h-` prefix.

## Directives

`x-h-alert` is the root. The directives compose one component and must be nested as shown in the Example below (the library throws at runtime when a required ancestor is missing):

- `x-h-alert`
- `x-h-alert-title`
- `x-h-alert-description`
- `x-h-alert-actions`

## API

### Attributes

#### x-h-alert

| Attribute    | Type                                                         | Required | Description          |
| ------------ | ------------------------------------------------------------ | -------- | -------------------- |
| data-variant | `positive`<br />`negative`<br />`warning`<br />`information` | false    | Semantic color state |

### Modifiers

#### x-h-alert

| Modifier        | Description                                            |
| --------------- | ------------------------------------------------------ |
| <s>floating</s> | Deprecated. Will be removed in the next major version. |

## Example

```html
<div x-h-alert>
  <i x-h-lucide role="img" data-lucide="mail"></i>
  <div x-h-alert-title>Mail Sent</div>
  <div x-h-alert-description>Your mail has been sent</div>
</div>
```

More examples in the docs site: Examples.

## Notes

- Directive values are Alpine expressions, so quote string literals: `x-h-...="'Label'"`.
- Components render only after Alpine has registered Harmonia. See SKILL.md for setup.
