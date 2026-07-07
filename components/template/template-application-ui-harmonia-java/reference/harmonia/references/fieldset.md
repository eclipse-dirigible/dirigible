# Fieldset

A container that groups related form elements, including labels, controls, and helper text, to create accessible and organized input sections. Fieldsets improve the structure and clarity of complex forms.

Part of the Harmonia Alpine.js component library. Every directive uses the `x-h-` prefix.

## Usage

Use fieldsets to logically group related inputs, such as multiple options within a survey or sections of a settings form. Each fieldset should include a descriptive legend or label to maintain accessibility and provide context for users navigating the form.

## Directives

`x-h-fieldset` is the root. The directives compose one component and must be nested as shown in the Examples below (the library throws at runtime when a required ancestor is missing):

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

## Examples

```html
<form>
  <div x-h-field-group>
    <fieldset x-h-fieldset>
      <legend x-h-legend>Card Payment</legend>
      <p x-h-field-description>Enter your card and billing information</p>
      <div x-h-field-group>
        <div x-h-field>
          <label x-h-label for="formCardName" data-state="checked">Name on Card</label>
          <input x-h-input id="formCardName" placeholder="John Doe" required />
        </div>
        <div x-h-field>
          <label x-h-label for="formCardNumber">Card Number</label>
          <input x-h-input type="text" id="formCardNumber" placeholder="2141 9614 2401 7895" required />
          <p x-h-field-error>Enter your 16-digit card number</p>
          <p x-h-field-description data-hide-on-error="true">This is just a demo. Do NOT enter your real card number.</p>
        </div>
        <div class="grid grid-cols-3 gap-4">
          <div x-h-field>
            <label x-h-label for="formCardMonth">Month</label>
            <div x-h-select>
              <input id="formCardMonth" x-h-select-input placeholder="MM" required />
              <div x-h-select-content>
                <div x-h-select-option="'01'" data-value="01"></div>
                <div x-h-select-option="'02'" data-value="02"></div>
                <div x-h-select-option="'03'" data-value="03"></div>
                <div x-h-select-option="'04'" data-value="04"></div>
                <div x-h-select-option="'05'" data-value="05"></div>
                <div x-h-select-option="'06'" data-value="06"></div>
                <div x-h-select-option="'07'" data-value="07"></div>
                <div x-h-select-option="'08'" data-value="08"></div>
                <div x-h-select-option="'09'" data-value="09"></div>
                <div x-h-select-option="'10'" data-value="10"></div>
                <div x-h-select-option="'11'" data-value="11"></div>
                <div x-h-select-option="'12'" data-value="12"></div>
              </div>
            </div>
          </div>
          <div x-h-field>
            <label x-h-label for="formCardYear">Year</label>
            <div x-h-select>
              <input id="formCardYear" x-h-select-input placeholder="YYYY" required />
              <div
                x-h-select-content
                x-data="{
                  years: (() => {
                    let year = new Date().getFullYear();
                    let allYears = [];
                    for (let y = 0; y <= 10; y++) allYears.push(year + y);
                    return allYears;
                  })(),
                }"
              >
                <template x-for="year in years">
                  <div x-h-select-option="year" :data-value="year"></div>
                </template>
              </div>
            </div>
          </div>
          <div x-h-field>
            <label x-h-label for="formCVV">CVV</label>
            <input x-h-input id="formCVV" placeholder="123" required />
          </div>
        </div>
      </div>
    </fieldset>
    <div x-h-separator data-orientation="horizontal"></div>
    <fieldset x-h-fieldset>
      <legend x-h-legend>Delivery time</legend>
      <p x-h-field-description>The date and time when the package will be delivered to you</p>
      <div x-h-field-group>
        <div class="grid grid-cols-2 gap-4">
          <div x-h-field>
            <label x-h-label for="delivery-dp">Date</label>
            <div x-h-date-picker>
              <input type="text" id="delivery-dp" required />
              <button x-h-date-picker-trigger aria-label="Choose date"></button>
              <div x-h-date-picker-popup></div>
            </div>
          </div>
          <div x-h-field>
            <label x-h-label for="delivery-tp">Time</label>
            <div x-h-time-picker>
              <input type="text" id="delivery-tp" x-h-time-picker-input required />
              <div x-h-time-picker-popup></div>
            </div>
          </div>
        </div>
      </div>
    </fieldset>
    <div x-h-separator data-orientation="horizontal"></div>
    <fieldset x-h-fieldset>
      <legend x-h-legend>Billing Address</legend>
      <p x-h-field-description>The billing address associated with your payment method</p>
      <div x-h-field-group>
        <div x-h-field data-orientation="horizontal">
          <span x-h-checkbox>
            <input type="checkbox" id="formShippingCheckbox" checked />
          </span>
          <label x-h-label for="formShippingCheckbox" class="font-normal">Same as shipping address</label>
        </div>
      </div>
    </fieldset>
    <fieldset x-h-fieldset>
      <div x-h-field-group>
        <div x-h-field>
          <label x-h-label for="formComments">Comments</label>
          <textarea x-h-textarea id="formComments" placeholder="Add any additional comments" class="resize-none"></textarea>
        </div>
      </div>
    </fieldset>
    <div x-h-field data-orientation="horizontal">
      <span class="flex-1"></span>
      <button x-h-button data-variant="primary" type="submit">Submit</button>
      <button x-h-button data-variant="outline" type="button">Cancel</button>
    </div>
  </div>
</form>
```

### Invalid field with error message

When you type something in the input below, it will no longer be invalid and the error message will disappear.

```html
<form>
  <fieldset x-h-fieldset data-validate="immediate">
    <div x-h-field-group>
      <div x-h-field>
        <label x-h-label for="errorMessage">Invalid</label>
        <input x-h-input id="errorMessage" placeholder="Input invalid" required />
        <p x-h-field-error>The input cannot be empty</p>
      </div>
    </div>
  </fieldset>
</form>
```

### Field with description and error message

When you type something in the input below, the error message will dissapear and the description will be shown.

```html
<form>
  <fieldset x-h-fieldset data-validate="immediate">
    <div x-h-field-group>
      <div x-h-field>
        <label x-h-label for="descError">Word</label>
        <input x-h-input id="descError" placeholder="Something" required />
        <p x-h-field-error>Must not be empty!</p>
        <p x-h-field-description data-hide-on-error="true">This filed holds random text.</p>
      </div>
    </div>
  </fieldset>
</form>
```

### Responsive field

_You may need to resize the window to see it switching between horizintal and vertical._

```html
<form>
  <fieldset x-h-fieldset>
    <div x-h-field-group>
      <div x-h-field data-orientation="responsive">
        <label x-h-label for="responsiveDemo">Word</label>
        <input x-h-input id="responsiveDemo" placeholder="Something" />
      </div>
    </div>
  </fieldset>
</form>
```

### Horizontal field

```html
<form>
  <fieldset x-h-fieldset>
    <div x-h-field-group>
      <div x-h-field data-orientation="horizontal">
        <label x-h-label for="horizontalDemo">Word</label>
        <input x-h-input id="horizontalDemo" placeholder="Something" />
      </div>
    </div>
  </fieldset>
</form>
```

### Horizontal field with description and error message

In horizontal orientation the label stays beside the input, while the error and description are shown below the input. When you type something in the input below, the error message will disappear and the description will be shown.

```html
<form>
  <fieldset x-h-fieldset data-validate="immediate">
    <div x-h-field-group>
      <div x-h-field data-orientation="horizontal">
        <label x-h-label for="horizontalDescError">Word</label>
        <input x-h-input id="horizontalDescError" placeholder="Something" required />
        <p x-h-field-error>Must not be empty!</p>
        <p x-h-field-description data-hide-on-error="true">This field holds random text.</p>
      </div>
    </div>
  </fieldset>
</form>
```

### Disabled field with enabled input

<br/>

```html
<form>
  <fieldset x-h-fieldset>
    <div x-h-field-group>
      <div x-h-field data-disabled="true">
        <label x-h-label for="visuallyDisabled">Label Disabled</label>
        <input x-h-input id="visuallyDisabled" placeholder="Input still active" />
      </div>
    </div>
  </fieldset>
</form>
```

### Disabled field with disabled input

<br/>

```html
<form>
  <fieldset x-h-fieldset>
    <div x-h-field-group>
      <div x-h-field data-disabled="true">
        <label x-h-label for="visuallyDisabled">Label Disabled</label>
        <input x-h-input id="visuallyDisabled" placeholder="Input inactive" disabled />
      </div>
    </div>
  </fieldset>
</form>
```

Full docs: https://www.codbex.com/harmonia/components/fieldset.html

## Notes

- Directive values are Alpine expressions, so quote string literals: `x-h-...="'Label'"`.
- Components render only after Alpine has registered Harmonia. See SKILL.md for setup.
