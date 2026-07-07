# Step Indicator

Communicates progress through a sequence of steps, showing which steps are completed, which one is active, and which are still ahead. Useful for multi-step forms, wizards, and onboarding flows.

Part of the Harmonia Alpine.js component library. Every directive uses the `x-h-` prefix.

## Usage

Use a step indicator when a task is split into a clear, ordered sequence of steps. Steps are numbered starting from 1. The first item is step 1, the second is step 2, and so on. You keep the number of the currently active step in a variable in your Alpine scope and pass that variable to the component, so it stays in sync with "Next"/"Back" controls. Clicking a step also updates the variable, letting users jump straight to that step. The content of each marker is up to you: a number, an icon, or anything else.

## Directives

`x-h-step-indicator` is the root. The directives compose one component and must be nested as shown in the Examples below (the library throws at runtime when a required ancestor is missing):

- `x-h-step-indicator`
- `x-h-step-indicator-item`
- `x-h-step-indicator-trigger`
- `x-h-step-indicator-marker`
- `x-h-step-indicator-content`
- `x-h-step-indicator-title`
- `x-h-step-indicator-description`
- `x-h-step-indicator-separator`

## API

### Attributes

#### x-h-step-indicator

| Attribute        | Type                         | Required | Description                                                                                                        |
| ---------------- | ---------------------------- | -------- | ------------------------------------------------------------------------------------------------------------------ |
| `self`           | number                       | true     | A reference to the variable that holds the number of the active step. Clicking a step writes the new number to it. |
| data-orientation | `horizontal`<br />`vertical` | false    | Layout direction. Defaults to `horizontal`. Reactive, so you can bind it to switch orientation at runtime.         |

#### x-h-step-indicator-item

| Attribute | Type   | Required | Description                                                          |
| --------- | ------ | -------- | -------------------------------------------------------------------- |
| `self`    | number | true     | The number of this step. The first step is 1, the next 2, and so on. |

Each item exposes its state through a `data-state` attribute of `inactive`, `active`, or `completed`, which the child parts use for their styling.

#### x-h-step-indicator-trigger

| Attribute            | Type    | Required | Description                                                                                                                                                                                                          |
| -------------------- | ------- | -------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| data-non-interactive | boolean | false    | When `true`, clicking this step does not change the active step. A common use is to let users return to steps they have already passed while preventing them from skipping ahead to steps they have not reached yet. |

## Examples

### Default

```html
<div x-data="{ step: 2, total: 3 }" class="vbox w-full gap-6">
  <div x-h-step-indicator="step">
    <div x-h-step-indicator-item="1">
      <button x-h-step-indicator-trigger>
        <span x-h-step-indicator-marker>
          <svg x-h-icon data-icon="circle-user" role="img" aria-label="step account"></svg>
        </span>
        <span x-h-step-indicator-content>
          <span x-h-step-indicator-title>Account</span>
          <span x-h-step-indicator-description>Your details</span>
        </span>
      </button>
      <div x-h-step-indicator-separator></div>
    </div>
    <div x-h-step-indicator-item="2">
      <button x-h-step-indicator-trigger>
        <span x-h-step-indicator-marker>
          <i x-h-lucide role="img" data-lucide="map-pin"></i>
        </span>
        <span x-h-step-indicator-content>
          <span x-h-step-indicator-title>Address</span>
          <span x-h-step-indicator-description>Shipping info</span>
        </span>
      </button>
      <div x-h-step-indicator-separator></div>
    </div>
    <div x-h-step-indicator-item="3">
      <button x-h-step-indicator-trigger>
        <span x-h-step-indicator-marker>
          <i x-h-lucide role="img" data-lucide="banknote"></i>
        </span>
        <span x-h-step-indicator-content>
          <span x-h-step-indicator-title>Payment</span>
          <span x-h-step-indicator-description>Confirm order</span>
        </span>
      </button>
    </div>
  </div>
  <div class="hbox justify-between gap-2">
    <button x-h-button data-variant="outline" @click="step = Math.max(step - 1, 1)" :disabled="step === 1">Back</button>
    <button x-h-button data-variant="primary" @click="step = Math.min(step + 1, total)" :disabled="step === total">Next</button>
  </div>
</div>
```

### Markers only

```html
<div x-data="{ step: 2 }">
  <div x-h-step-indicator="step">
    <div x-h-step-indicator-item="1">
      <button x-h-step-indicator-trigger>
        <span x-h-step-indicator-marker>1</span>
      </button>
      <div x-h-step-indicator-separator></div>
    </div>
    <div x-h-step-indicator-item="2">
      <button x-h-step-indicator-trigger>
        <span x-h-step-indicator-marker>2</span>
      </button>
      <div x-h-step-indicator-separator></div>
    </div>
    <div x-h-step-indicator-item="3">
      <button x-h-step-indicator-trigger>
        <span x-h-step-indicator-marker>3</span>
      </button>
      <div x-h-step-indicator-separator></div>
    </div>
    <div x-h-step-indicator-item="4">
      <button x-h-step-indicator-trigger>
        <span x-h-step-indicator-marker>4</span>
      </button>
    </div>
  </div>
</div>
```

### Vertical

```html
<div x-data="{ step: 2 }">
  <div x-h-step-indicator="step" data-orientation="vertical">
    <div x-h-step-indicator-item="1">
      <button x-h-step-indicator-trigger>
        <span x-h-step-indicator-marker>1</span>
        <span x-h-step-indicator-content>
          <span x-h-step-indicator-title>Account</span>
          <span x-h-step-indicator-description>Your details</span>
        </span>
      </button>
      <div x-h-step-indicator-separator></div>
    </div>
    <div x-h-step-indicator-item="2">
      <button x-h-step-indicator-trigger>
        <span x-h-step-indicator-marker>2</span>
        <span x-h-step-indicator-content>
          <span x-h-step-indicator-title>Address</span>
          <span x-h-step-indicator-description>Shipping info</span>
        </span>
      </button>
      <div x-h-step-indicator-separator></div>
    </div>
    <div x-h-step-indicator-item="3">
      <button x-h-step-indicator-trigger>
        <span x-h-step-indicator-marker>3</span>
        <span x-h-step-indicator-content>
          <span x-h-step-indicator-title>Payment</span>
          <span x-h-step-indicator-description>Confirm order</span>
        </span>
      </button>
    </div>
  </div>
</div>
```

Full docs: https://www.codbex.com/harmonia/components/step-indicator.html

## Notes

- Directive values are Alpine expressions, so quote string literals: `x-h-...="'Label'"`.
- Components render only after Alpine has registered Harmonia. See SKILL.md for setup.
