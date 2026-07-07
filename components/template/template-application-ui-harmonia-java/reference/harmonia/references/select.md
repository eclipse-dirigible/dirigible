# Select

Allows users to choose one or more items from a predefined list of options. This component provides a compact way to present choices without overwhelming the interface.

Part of the Harmonia Alpine.js component library. Every directive uses the `x-h-` prefix.

## Usage

Use the Select component without a search option when there are a limited number of options, ideally 12 or fewer. For longer lists, enable the search feature.

## Directives

`x-h-select` is the root. The directives compose one component and must be nested as shown in the Examples below (the library throws at runtime when a required ancestor is missing):

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

## Keyboard Handling

The user can use the following keyboard shortcuts in order to navigate through the select:

- `Up` / `Down` - Moves focus to the previous or next option.
- `Home` / `Page Up` - Moves focus to the first option.
- `End` / `Page Down` - Moves focus to the last option.
- `Enter` / `Space` - Selects the focused option. If the list is closed, opens it.
- `Esc` - Closes the list without changing the current selection.
- `Tab` - Closes the list and moves focus to the next focusable element.
- `Character keys (A-Z)` - Moves focus to the next option whose label starts with the typed character.

## Binding

Binds through Alpine `x-model`. See the Examples for the expected value shape.

## Examples

### With model

```html
<div x-data="selectData">
  <div x-h-select>
    <input x-h-select-input :placeholder="placeholder" x-model="selected" />
    <div x-h-select-content>
      <div x-h-select-search></div>
      <div x-h-select-group>
        <div x-h-select-label>Fruits</div>
        <template x-for="option in items">
          <div x-h-select-option="option.label" :data-value="option.value"></div>
        </template>
      </div>
    </div>
  </div>
</div>
<script>
  Alpine.data('selectData', () => ({
    getOriginalItems() {
      return [
        { label: 'Apple', value: 'apple' },
        { label: 'Banana', value: 'banana' },
        { label: 'Blueberry', value: 'blueberry' },
        { label: 'Grapes', value: 'grapes' },
        { label: 'Pineapple', value: 'pineapple' },
        { label: 'Jamaican tangelo', value: 'jamaicanTangelo' },
      ];
    },
    selected: 'banana',
    placeholder: 'Select',
    items: [],
    addFromSearch(event) {
      let nItems = this.getOriginalItems();
      nItems.forEach((element) => {
        element.label = `${event.target.value}${element.label}`;
      });
      this.items = nItems;
    },
    init() {
      this.items = this.getOriginalItems();
    },
  }));
</script>
```

### Clearable

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

### Multiple

The input automatically switches modes based on the model. If you want to select multiple items, pass an array as the model.

```html
<div x-data="selectMultipleData">
  <div x-h-select>
    <input x-h-select-input :placeholder="placeholder" x-model="selected" />
    <div x-h-select-content>
      <div x-h-select-search></div>
      <div x-h-select-group>
        <div x-h-select-label>Fruits</div>
        <template x-for="option in items">
          <div x-h-select-option="option.label" :data-value="option.value"></div>
        </template>
      </div>
    </div>
  </div>
</div>
<script>
  Alpine.data('selectMultipleData', () => ({
    getOriginalItems() {
      return [
        { label: 'Apple', value: 'apple' },
        { label: 'Banana', value: 'banana' },
        { label: 'Blueberry', value: 'blueberry' },
        { label: 'Grapes', value: 'grapes' },
        { label: 'Pineapple', value: 'pineapple' },
        { label: 'Jamaican tangelo', value: 'jamaicanTangelo' },
      ];
    },
    selected: ['apple', 'banana'],
    placeholder: 'Select',
    items: [],
    addFromSearch(event) {
      let nItems = this.getOriginalItems();
      nItems.forEach((element) => {
        element.label = `${event.target.value}${element.label}`;
      });
      this.items = nItems;
    },
    init() {
      this.items = this.getOriginalItems();
    },
  }));
</script>
```

### No model

```html
<div x-h-select>
  <input x-h-select-input placeholder="Select" />
  <div x-h-select-content>
    <div x-h-select-option="'Option 1'" data-value="1"></div>
    <div x-h-select-option="'Option 2'" data-value="2"></div>
    <div x-h-select-option="'Option 3'" data-value="3"></div>
    <div x-h-select-option="'Option 4'" data-value="4" data-disabled="true"></div>
    <div x-h-select-option="'Option 5'" data-value="5"></div>
  </div>
</div>
```

### With groups

```html
<div x-h-select>
  <input x-h-select-input placeholder="Select" />
  <div x-h-select-content>
    <div x-h-select-group>
      <div x-h-select-label>First two options</div>
      <div x-h-select-option="'Option 1'" data-value="1"></div>
      <div x-h-select-option="'Option 2'" data-value="2"></div>
    </div>
    <div x-h-select-group>
      <div x-h-select-label>The rest</div>
      <div x-h-select-option="'Option 3'" data-value="3"></div>
      <div x-h-select-option="'Option 4'" data-value="4"></div>
      <div x-h-select-separator></div>
      <div x-h-select-option="'Option 5'" data-value="5"></div>
    </div>
  </div>
</div>
```

Full docs: https://www.codbex.com/harmonia/components/select.html

## Notes

- Directive values are Alpine expressions, so quote string literals: `x-h-...="'Label'"`.
- Components render only after Alpine has registered Harmonia. See SKILL.md for setup.
