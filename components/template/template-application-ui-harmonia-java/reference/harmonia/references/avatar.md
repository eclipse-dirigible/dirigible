# Avatar

Represents a person, entity, or object using an image, icon, or text, such as a user photo, initials, or symbolic graphic. Avatars help provide visual context and identity within the interface.

Part of the Harmonia Alpine.js component library. Every directive uses the `x-h-` prefix.

## Directives

`x-h-avatar` is the root. The directives compose one component and must be nested as shown in the Example below (the library throws at runtime when a required ancestor is missing):

- `x-h-avatar`
- `x-h-avatar-image`
- `x-h-avatar-fallback`

## API

### Attributes

#### x-h-avatar

| Attribute    | Type                                                         | Required | Description          |
| ------------ | ------------------------------------------------------------ | -------- | -------------------- |
| data-variant | `positive`<br />`negative`<br />`warning`<br />`information` | false    | Semantic color state |

## Example

```html
<span x-h-avatar>HM</span>
```

More examples in the docs site: Default, Big avatar, Square avatar, Icon-only, Variants, Interactive.

## Notes

- Directive values are Alpine expressions, so quote string literals: `x-h-...="'Label'"`.
- Components render only after Alpine has registered Harmonia. See SKILL.md for setup.
