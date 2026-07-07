# Expansion Panel

The Expansion Panel is a container component that manages multiple collapsible panels within a layout. Unlike the accordion component, expanded panels do not resize the container but instead fill the available space and become scrollable when needed.

Part of the Harmonia Alpine.js component library. Every directive uses the `x-h-` prefix.

## Directives

`x-h-exp-panel` is the root. The directives compose one component and must be nested as shown in the Example below (the library throws at runtime when a required ancestor is missing):

- `x-h-exp-panel`
- `x-h-exp-panel-item`
- `x-h-exp-panel-trigger`
- `x-h-exp-panel-content`

## API

### Attributes

#### x-h-exp-panel

| Attribute     | Type                          | Required | Description                                               |
| ------------- | ----------------------------- | -------- | --------------------------------------------------------- |
| data-size     | `default`<br />`md`<br />`sm` | false    | Height of the panel header items.                         |
| data-variant  | `default`<br />`transparent`  | false    | Transparent background color. Does not remove the border. |
| data-floating | boolean                       | false    | Floating style panels.                                    |

#### x-h-exp-panel-item

| Attribute | Type    | Required | Description                                                                                                         |
| --------- | ------- | -------- | ------------------------------------------------------------------------------------------------------------------- |
| `self`    | boolean | false    | Default expanded state. This is a two-way binding, so if a variable is provided, it will get updated automatically. |

#### x-h-exp-panel-trigger

| Attribute | Type   | Required | Description                                                                        |
| --------- | ------ | -------- | ---------------------------------------------------------------------------------- |
| `self`    | string | true     | Sets the title of the item. Expects a string literal or a reference to a variable. |

## Example

```html
<div x-h-exp-panel data-variant="transparent">
  <div x-h-exp-panel-item>
    <h3 x-h-exp-panel-trigger="'Panel 1'"></h3>
    <div x-h-exp-panel-content>Panel content</div>
  </div>
  <div x-h-exp-panel-item>
    <h3 x-h-exp-panel-trigger="'Panel 2'"></h3>
    <div x-h-exp-panel-content>Panel content</div>
  </div>
</div>
```

More examples in the docs site: Default Panels, Floating Panels, Transparent Floating Panels, Medium Panels, Small Panels, Two-Way Binding And Watching For Changes.

## Notes

- Directive values are Alpine expressions, so quote string literals: `x-h-...="'Label'"`.
- Components render only after Alpine has registered Harmonia. See SKILL.md for setup.
