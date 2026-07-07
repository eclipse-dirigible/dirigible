# Theme

Utility functions for retrieving and updating the color scheme.

Part of the Harmonia Alpine.js component library. Every directive uses the `x-h-` prefix.

## API

### Functions

| Property                  | Arguments                       | Returns                         | Description                                                       |
| ------------------------- | ------------------------------- | ------------------------------- | ----------------------------------------------------------------- |
| getColorScheme            | none                            | `light`<br />`dark`<br />`auto` | Retrieves the currently active color scheme.                      |
| setColorScheme            | `light`<br />`dark`<br />`auto` | none                            | Updates the application’s color scheme to the specified value.    |
| addColorSchemeListener    | callbackFunction                | none                            | Registers a callback to be invoked when the color scheme changes. |
| removeColorSchemeListener | callbackFunction                | none                            | Unregisters a previously registered callback.                     |
| getSystemColorScheme      | none                            | `light`<br />`dark`             | Retrieves the currently active browser/OS color scheme.           |

::: info Seting the color scheme
The `setColorScheme` function automatically persists the most recently selected color scheme to the browser’s local storage, ensuring the preference is retained and reapplied across page loads without requiring additional work.
:::

::: info Syncing across frames and tabs
A color scheme change made in any frame is automatically applied to every embedded same-origin iframe that uses Harmonia, as well as to other browser tabs of the application, keeping the whole UI consistent.
:::

### callbackFunction

| Arguments | Description                                                   |
| --------- | ------------------------------------------------------------- |
| scheme    | The current color scheme. It can be either `light` or `dark`. |

## Notes

- Directive values are Alpine expressions, so quote string literals: `x-h-...="'Label'"`.
- Components render only after Alpine has registered Harmonia. See SKILL.md for setup.
