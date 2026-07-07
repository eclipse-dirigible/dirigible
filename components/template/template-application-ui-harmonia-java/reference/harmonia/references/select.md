# Select

Allows users to choose one or more items from a predefined list of options. This component provides a compact way to present choices without overwhelming the interface.

Part of the Harmonia Alpine.js component library. Every directive uses the `x-h-` prefix.

## Directives

`x-h-select` is the root. The directives compose one component and must be nested as shown in the Example below (the library throws at runtime when a required ancestor is missing):

- `x-h-select`
- `x-h-select-input`
- `x-h-select-content`
- `x-h-select-search`
- `x-h-select-group`
- `x-h-select-label`
- `x-h-select-option`
- `x-h-select-separator`

## API

### Attributes

#### x-h-select

| Attribute      | Type               | Required | Description                                                                                                                                                            |
| -------------- | ------------------ | -------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| data-size      | `sm`<br/>`default` | false    | Changes the size of the select button.                                                                                                                                 |
| data-clearable | boolean            | false    | When set to `true`, allows the current selection to be cleared by clicking the already-selected option again. By default, re-clicking a selected option has no effect. |

#### x-h-select-input

| Attribute | Type   | Required | Description                                                                                                                                                         |
| --------- | ------ | -------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| data-id   | string | false    | Since the native input is hidden, this attribute provides an ID for proper labeling and accessibility. The value is forwarded to the actual select trigger element. |

#### x-h-select-content

| Attribute  | Type                                                                                                                                                                          | Required | Description                                     |
| ---------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | -------- | ----------------------------------------------- |
| data-align | `bottom-start`<br/>`bottom`<br/>`bottom-end`<br/>`right-start`<br/>`right`<br/>`right-end`<br/>`left-start`<br/>`left`<br/>`left-end`<br/>`top-start`<br/>`top`<br/>`top-end` | false    | Aligns the select body relative to the trigger. |

#### x-h-select-option

| Attribute     | Type    | Required | Description                                                          |
| ------------- | ------- | -------- | -------------------------------------------------------------------- |
| `self`        | string  | false    | Sets the label of the option. Either a string literal or a variable. |
| data-value    | string  | false    | Sets the value of the option.                                        |
| data-disabled | boolean | false    | Disables the option.                                                 |

#### x-h-select-search

| Attribute   | Type                                                           | Required | Description                                                                                                                                                                                   |
| ----------- | -------------------------------------------------------------- | -------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| data-filter | `starts-with`<br />`contains`<br />`contains-each`<br />`none` | false    | Defines the search matching strategy. Use `none` to disable built-in filtering and implement custom search behavior. With the 'contains-each' filter, search terms are separated using space. |

### Modifiers

#### x-h-select

| Modifier | Description                                 |
| -------- | ------------------------------------------- |
| table    | Use when the select input is inside a table |

## Binding

Binds through Alpine `x-model`. See the Example for the expected value shape.

## Example

```html
<div x-data="{ selected: 'opt-1' }">
  <div x-h-select data-clearable="true">
    <input x-h-select-input placeholder="Select" x-model="selected" />
    <div x-h-select-content>
      <div x-h-select-option="'Option 1'" data-value="opt-1"></div>
      <div x-h-select-option="'Option 2'" data-value="opt-2"></div>
      <div x-h-select-option="'Option 3'" data-value="opt-3"></div>
    </div>
  </div>
</div>
```

More examples in the docs site: With model, Multiple, No model, With groups.

## Notes

- Directive values are Alpine expressions, so quote string literals: `x-h-...="'Label'"`.
- Components render only after Alpine has registered Harmonia. See SKILL.md for setup.
