# Input Group

Combines an input or textarea field with related elements, such as buttons, icons, or labels, to create a cohesive and interactive form control. Input Groups help visually associate related functionality with a single field.

Part of the Harmonia Alpine.js component library. Every directive uses the `x-h-` prefix.

## Directives

`x-h-input-group` is the root. The directives compose one component and must be nested as shown in the Example below (the library throws at runtime when a required ancestor is missing):

- `x-h-input-group`
- `x-h-input-group-addon`
- `x-h-input-group-text`

## API

### Attributes

#### x-h-input-group

| Attribute | Values             | Required | Description                    |
| --------- | ------------------ | -------- | ------------------------------ |
| data-size | `sm`<br/>`default` | false    | Changes the size of the input. |

#### x-h-input-group-addon

| Attribute     | Values                                                            | Required | Description                                       |
| ------------- | ----------------------------------------------------------------- | -------- | ------------------------------------------------- |
| data-align    | `inline-start`<br/>`inline-end`<br/>`block-start`<br/>`block-end` | false    | Aligns the addon relative to the group. See note. |
| data-disabled | boolean                                                           | false    | Disables the addon.                               |

::: info Focus Navigation
In order to achieve proper focus navigation, place the group addon after the input and then set the align prop to position it.
:::

## Example

```html
<div x-h-input-group>
  <input x-h-input.group placeholder="Searching..." disabled />
  <div x-h-input-group-addon data-align="inline-end">
    <span x-h-spinner></span>
  </div>
</div>
```

More examples in the docs site: Search bar, Search bar with tags, Search bar with buttons and popover, Textarea with top and bottom toolbars.

## Notes

- Directive values are Alpine expressions, so quote string literals: `x-h-...="'Label'"`.
- Components render only after Alpine has registered Harmonia. See SKILL.md for setup.
