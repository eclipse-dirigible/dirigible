# Separator

A simple visual divider used to separate content or sections within an interface. Separators help improve layout clarity and guide the user’s eye through the content.

Part of the Harmonia Alpine.js component library. Every directive uses the `x-h-` prefix.

## Directive

- `x-h-separator`

## API

### Attributes

| Attribute        | Type                         | Required | Description                               |
| ---------------- | ---------------------------- | -------- | ----------------------------------------- |
| data-orientation | `horizontal`<br />`vertical` | false    | Changes the orientation of the separator. |

## Example

```html
<div class="flex flex-col gap-3">
  <div class="flex h-6 items-center gap-4">
    <p x-h-text>Text</p>
    <div x-h-separator data-orientation="vertical"></div>
    <p x-h-text>Text</p>
    <div x-h-separator data-orientation="vertical"></div>
    <p x-h-text>Text</p>
  </div>
  <div x-h-separator data-orientation="horizontal"></div>
  <div class="flex h-6 items-center gap-4">
    <p x-h-text>Text</p>
    <div x-h-separator data-orientation="vertical"></div>
    <p x-h-text>Text</p>
    <div x-h-separator data-orientation="vertical"></div>
    <p x-h-text>Text</p>
  </div>
</div>
```

## Notes

- Directive values are Alpine expressions, so quote string literals: `x-h-...="'Label'"`.
- Components render only after Alpine has registered Harmonia. See SKILL.md for setup.
