# Tabs

Organizes content into multiple sections, displaying only one section at a time while keeping others easily accessible through a tabbed navigation interface. Tabs help structure information without overwhelming the user.

Part of the Harmonia Alpine.js component library. Every directive uses the `x-h-` prefix.

## Directives

`x-h-tabs` is the root. The directives compose one component and must be nested as shown in the Example below (the library throws at runtime when a required ancestor is missing):

- `x-h-tabs`
- `x-h-tab-bar`
- `x-h-tab-list`
- `x-h-tab`
- `x-h-tab-action`
- `x-h-tab-list-actions`
- `x-h-tab-list-action`
- `x-h-tabs-content`

## API

### Attributes

#### x-h-tabs

| Attribute        | Type                         | Required | Description                             |
| ---------------- | ---------------------------- | -------- | --------------------------------------- |
| data-orientation | `horizontal`<br />`vertical` | true     | Changes the orientation of the tab list |

#### x-h-tab-bar

| Attribute     | Type                          | Required | Description                                                                                 |
| ------------- | ----------------------------- | -------- | ------------------------------------------------------------------------------------------- |
| data-floating | boolean                       | false    | Floating style tab list.                                                                    |
| data-size     | `default`<br />`sm`<br />`lg` | false    | Height of the tab bar. Ignored when the tab bar is floating or the orientation is vertical. |

#### x-h-tabs-content

| Attribute | Type    | Required | Description               |
| --------- | ------- | -------- | ------------------------- |
| hidden    | boolean | false    | Show/hide the tab content |

#### x-h-tab-list-action

| Attribute    | Type                         | Required | Description                                            |
| ------------ | ---------------------------- | -------- | ------------------------------------------------------ |
| data-variant | `outline`<br />`transparent` | false    | Changes the style of the button. Default is `outline`. |

### Modifiers

#### x-h-tab-list-actions

| Modifier | Description                                                     |
| -------- | --------------------------------------------------------------- |
| end      | Tab action will be placed at the end of the tab list container. |

## Example

```html
<div x-h-tabs data-orientation="horizontal" style="height:10rem">
  <div x-h-tab-bar>
    <div x-h-tab-list>
      <button id="stce" x-h-tab aria-controls="stcec" aria-selected="true">Tab 1</button>
    </div>
  </div>
  <div class="relative" x-h-tabs-content id="stcec" aria-labelledby="stce">
    <div class="position-fit absolute overflow-auto">
      <img src="/harmonia/logo/harmonia.svg" alt="@harmonia" width="240px" />
    </div>
  </div>
</div>
```

More examples in the docs site: Sizes, Horizontal tabs, Horizontal tabs with icon and close button, Horizontal tabs with actions, Horizontal tabs with actions (end), Horizontal float tabs, Horizontal float tabs that fit to size, Horizontal float tabs with icon and close button, Horizontal float tabs with actions, Horizontal float tabs with actions (end), Vertical tabs, Vertical tabs with icon and close button, Vertical tabs with actions, Vertical tabs with actions (end), Vertical float tabs, Vertical float tabs with icon and close button, Vertical float tabs with actions, Vertical float tabs with actions (end).

## Notes

- Directive values are Alpine expressions, so quote string literals: `x-h-...="'Label'"`.
- Components render only after Alpine has registered Harmonia. See SKILL.md for setup.
