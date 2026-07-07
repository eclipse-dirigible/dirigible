# Toolbar

A container that groups actions (like buttons, inputs, or popovers) relevant to the current view. Toolbars are typically positioned at the top or bottom of a section, providing convenient access to context-specific functionality.

Part of the Harmonia Alpine.js component library. Every directive uses the `x-h-` prefix.

## Directives

`x-h-toolbar` is the root. The directives compose one component and must be nested as shown in the Example below (the library throws at runtime when a required ancestor is missing):

- `x-h-toolbar`
- `x-h-toolbar-image`
- `x-h-toolbar-title`
- `x-h-toolbar-subtitle`
- `x-h-toolbar-branding`
- `x-h-toolbar-spacer`
- `x-h-toolbar-separator`

## API

### Attributes

#### x-h-toolbar

| Attribute       | Type                          | Required | Description                                               |
| --------------- | ----------------------------- | -------- | --------------------------------------------------------- |
| data-variant    | `default`<br />`transparent`  | false    | Transparent background color. Does not remove the border. |
| data-size       | `default`<br />`md`<br />`sm` | false    | Make the toolbar smaller.                                 |
| data-floating   | boolean                       | false    | Floating style toolbar.                                   |
| data-borderless | boolean                       | false    | Removes toolbar borders.                                  |

### Modifiers

#### x-h-toolbar

| Modifier | Description          |
| -------- | -------------------- |
| footer   | Footer-style toolbar |

## Binding

Binds through Alpine `x-model`. See the Example for the expected value shape.

## Example

```html
<div x-h-toolbar.footer>
  <div x-h-toolbar-spacer></div>
  <button x-h-button data-size="md" data-variant="primary">Apply</button>
  <button x-h-button data-size="md" data-variant="transparent">Cancel</button>
</div>
```

More examples in the docs site: Default, Borderless, Floating, Transparent, Floating Transparent, Small, Medium, As page header (shellbar).

## Notes

- Directive values are Alpine expressions, so quote string literals: `x-h-...="'Label'"`.
- Components render only after Alpine has registered Harmonia. See SKILL.md for setup.
