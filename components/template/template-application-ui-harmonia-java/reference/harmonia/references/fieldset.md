# Fieldset

A container that groups related form elements, including labels, controls, and helper text, to create accessible and organized input sections. Fieldsets improve the structure and clarity of complex forms.

Part of the Harmonia Alpine.js component library. Every directive uses the `x-h-` prefix.

## Directives

`x-h-fieldset` is the root. The directives compose one component and must be nested as shown in the Example below (the library throws at runtime when a required ancestor is missing):

- `x-h-fieldset`
- `x-h-legend`
- `x-h-field-group`
- `x-h-field`
- `x-h-field-content`
- `x-h-field-title`
- `x-h-field-description`
- `x-h-field-error`

## API

### Attributes

#### x-h-field

| Attribute        | Values                                       | Required | Description                                                               |
| ---------------- | -------------------------------------------- | -------- | ------------------------------------------------------------------------- |
| data-disabled    | boolean                                      | false    | Applies a disabled style to the field and label. Does NOT disable inputs. |
| data-orientation | `vertical`<br/>`horizontal`<br/>`responsive` | false    | Changes the alignment of the label and input.                             |

#### x-h-field-description

| Attribute          | Values  | Required | Description                                                                                                        |
| ------------------ | ------- | -------- | ------------------------------------------------------------------------------------------------------------------ |
| data-hide-on-error | boolean | false    | When enabled, the description will be hidden when the input is invalid, and the error message will appear instead. |

### Modifiers

#### x-h-legend

| Modifier | Description                    |
| -------- | ------------------------------ |
| label    | Makes the legend text smaller. |

### Validation timing

By default a control with a failing native constraint (for example a `required` empty input) only shows its invalid styling after the user has interacted with it (edited and blurred it) or attempted to submit the form, not on page load. Set `data-validate` to change when native-constraint errors appear:

| Value         | Behavior                                                                                    |
| ------------- | ------------------------------------------------------------------------------------------- |
| `interaction` | Default. Invalid styling appears only after user interaction or a submit attempt.           |
| `immediate`   | Invalid styling appears immediately, including on page load, while the constraint is unmet. |

`data-validate` is read from an ancestor, so put it on `x-h-fieldset`, `x-h-field`, or any wrapping element to control every control inside it:

```html
<fieldset x-h-fieldset data-validate="immediate">
  <!-- every control inside validates on load -->
</fieldset>
```

This affects only native constraint validation (`:invalid`). Setting `aria-invalid="true"` yourself always shows the invalid styling immediately, in either mode - use it for programmatic or server-side errors.

## Example

```html
<fieldset x-h-fieldset data-validate="immediate">
  <!-- every control inside validates on load -->
</fieldset>
```

More examples in the docs site: Examples, Invalid field with error message, Field with description and error message, Responsive field, Horizontal field, Horizontal field with description and error message, Disabled field with enabled input, Disabled field with disabled input.

## Notes

- Directive values are Alpine expressions, so quote string literals: `x-h-...="'Label'"`.
- Components render only after Alpine has registered Harmonia. See SKILL.md for setup.
