# Step Indicator

Communicates progress through a sequence of steps, showing which steps are completed, which one is active, and which are still ahead. Useful for multi-step forms, wizards, and onboarding flows.

Part of the Harmonia Alpine.js component library. Every directive uses the `x-h-` prefix.

## Directives

`x-h-step-indicator` is the root. The directives compose one component and must be nested as shown in the Example below (the library throws at runtime when a required ancestor is missing):

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

## Example

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

More examples in the docs site: Default, Vertical.

## Notes

- Directive values are Alpine expressions, so quote string literals: `x-h-...="'Label'"`.
- Components render only after Alpine has registered Harmonia. See SKILL.md for setup.
