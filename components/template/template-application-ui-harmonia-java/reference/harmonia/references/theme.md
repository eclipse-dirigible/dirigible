# Theme

Utility functions for retrieving and updating the color scheme.

Part of the Harmonia Alpine.js component library. Every directive uses the `x-h-` prefix.

## Usage

Use these functions to read or change the active color scheme from your own code, for example to build a light/dark toggle or to react when the scheme changes. `setColorScheme` persists the choice to local storage and syncs it across same-origin frames and tabs, so you do not need to manage persistence yourself.

## API

### Functions

| Property                  | Arguments                       | Returns                         | Description                                                       |
| ------------------------- | ------------------------------- | ------------------------------- | ----------------------------------------------------------------- |
| getColorScheme            | none                            | `light`<br />`dark`<br />`auto` | Retrieves the currently active color scheme.                      |
| setColorScheme            | `light`<br />`dark`<br />`auto` | none                            | Updates the application’s color scheme to the specified value.    |
| addColorSchemeListener    | callbackFunction                | none                            | Registers a callback to be invoked when the color scheme changes. |
| removeColorSchemeListener | callbackFunction                | none                            | Unregisters a previously registered callback.                     |
| getSystemColorScheme      | none                            | `light`<br />`dark`             | Retrieves the currently active browser/OS color scheme.           |

> **Note:** Seting the color scheme
> The `setColorScheme` function automatically persists the most recently selected color scheme to the browser’s local storage, ensuring the preference is retained and reapplied across page loads without requiring additional work.

> **Note:** Syncing across frames and tabs
> A color scheme change made in any frame is automatically applied to every embedded same-origin iframe that uses Harmonia, as well as to other browser tabs of the application, keeping the whole UI consistent.

### callbackFunction

| Arguments | Description                                                   |
| --------- | ------------------------------------------------------------- |
| scheme    | The current color scheme. It can be either `light` or `dark`. |

## Examples

### In plain JS

```js
const listener = (scheme) => {
  if (scheme === 'light') {
    console.log('Switched to a light theme!');
  } else {
    console.log('Switched to a dark theme!');
  }
};
Harmonia.addColorSchemeListener(listener);

const isDark = Harmonia.getColorScheme() === 'dark';
if (isDark) {
  Harmonia.setColorScheme('light');
}

Harmonia.removeColorSchemeListener(listener);
```

### In a module

```js
import { addColorSchemeListener, getColorScheme, removeColorSchemeListener, setColorScheme } from '@codbex/harmonia';

const listener = (scheme) => {
  if (scheme === 'light') {
    console.log('Switched to a light theme!');
  } else {
    console.log('Switched to a dark theme!');
  }
};

Harmonia.addColorSchemeListener(listener);

const isDark = getColorScheme() === 'dark';
if (isDark) {
  setColorScheme('light');
}

Harmonia.removeColorSchemeListener(listener);
```

Full docs: https://www.codbex.com/harmonia/utilities/theme.html

## Notes

- Directive values are Alpine expressions, so quote string literals: `x-h-...="'Label'"`.
- Components render only after Alpine has registered Harmonia. See SKILL.md for setup.
