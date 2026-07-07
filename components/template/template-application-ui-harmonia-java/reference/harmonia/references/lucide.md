# Lucide

An optional plugin that keeps [Lucide](https://lucide.dev) icons in sync with the Alpine/Harmonia lifecycle. Place `x-h-lucide` on a Lucide placeholder and it renders that icon when Alpine initializes the element, including markup added dynamically through `x-h-include`, a router (e.g. Pinecone-router) or Alpine's `x-for` / `x-if`. No global `lucide.createIcons` scans and no event wiring are needed. Use an `<svg>` placeholder when the icon carries other Alpine directives: it is rendered in place, while any other tag is replaced by the rendered svg.

Part of the Harmonia Alpine.js component library. Every directive uses the `x-h-` prefix.

## Usage

This plugin is opt-in, so you load it yourself. Load Lucide and Harmonia first, then add the plugin.

### CDN

```html
<script src="https://unpkg.com/lucide@latest"></script>
<script src="https://unpkg.com/@codbex/harmonia/dist/harmonia.min.js"></script>
<!-- opt in: registers x-h-lucide on alpine:init -->
<script src="https://unpkg.com/@codbex/harmonia/dist/harmonia-lucide.min.js"></script>
```

### ESM

```js
import Alpine from 'alpinejs';
import { Lucide } from '@codbex/harmonia';

Alpine.plugin(Lucide); // Lucide (window.lucide) must be loaded separately
Alpine.start();
```

## Directive

- `x-h-lucide`

## API

### Arguments

| Attribute  | Type   | Required | Description                                                                                    |
| ---------- | ------ | -------- | ---------------------------------------------------------------------------------------------- |
| expression | string | false    | The icon name, e.g. `x-h-lucide="'home'"`. Used only when there is no `data-lucide` attribute. |

### Attributes

| Attribute     | Type   | Required | Description                                                                                |
| ------------- | ------ | -------- | ------------------------------------------------------------------------------------------ |
| `data-lucide` | string | false    | The icon name. Takes priority over the expression, so existing Lucide markup is a drop-in. |

How the placeholder is rendered depends on its tag:

- An `<svg x-h-lucide>` placeholder is rendered **in place**: the icon's shapes are inserted into the element, Lucide's identifying classes are merged with yours, and Lucide's default attributes are applied only where you have not set that attribute yourself. Because the element is never replaced, Alpine directives on it (`x-show`, `:class`, `x-transition`, `@click`, ...) keep working.
- Any other tag (typically `<i>`, matching Lucide's own markup) is **replaced** by the rendered `<svg>`; the placeholder's attributes (`class`, `role`, `aria-*`, sizing, etc.) are copied onto it. Alpine bindings cannot survive that replacement, so combining another directive with `x-h-lucide` on such a placeholder throws an error. The one exception is `:data-lucide`, whose bound name is consumed when the icon renders. Use the `<svg>` form for anything reactive.

## Examples

### Drop-in for existing markup

Add `x-h-lucide` to any existing Lucide placeholder; the `data-lucide` name is reused.

<!-- prettier-ignore -->
```html
<i data-lucide="home" x-h-lucide></i>
<i data-lucide="settings" x-h-lucide class="size-5 text-primary" role="img" aria-label="Settings"></i>
```

### Name from the expression

```html
<i x-h-lucide="'arrow-up-right'"></i>
```

### Reactive icons

Put `x-h-lucide` on an `<svg>` element when the icon needs other Alpine directives. The element is rendered in place instead of being replaced, so bindings like `x-show` stay live; here the visible icon follows the current theme choice:

```html
<div x-data="{ theme: 'light' }">
  <button x-h-button data-variant="outline" @click="theme = theme === 'light' ? 'dark' : 'light'">
    <svg x-h-lucide x-show="theme === 'light'" role="presentation" data-lucide="sun"></svg>
    <svg x-h-lucide x-show="theme === 'dark'" role="presentation" data-lucide="moon"></svg>
    <span x-text="theme === 'light' ? 'Light' : 'Dark'"></span>
  </button>
</div>
```

On a replaced placeholder like `<i>`, the same markup would silently break (the binding would keep toggling the replaced element), which is why the plugin throws for it and points you here.

### Inside dynamic content

Because each icon renders when Alpine initializes its element, icons work automatically in dynamically loaded fragments and loops, with no manual `createIcons` call.

```html
<!-- fragment loaded at runtime -->
<div x-h-include="'/fragments/menu.html'"></div>

<!-- a list rendered by Alpine -->
<template x-for="item in items" :key="item.id">
  <button x-h-button>
    <i :data-lucide="item.icon" x-h-lucide></i>
    <span x-text="item.label"></span>
  </button>
</template>
```

> **Tip:**
> The icon name is read once, so prefer static names. For an icon whose name changes at runtime, re-create the element (for example with `x-if` / `template`) so the directive runs again.

Full docs: https://www.codbex.com/harmonia/plugins/lucide.html

## Notes

- Directive values are Alpine expressions, so quote string literals: `x-h-...="'Label'"`.
- Components render only after Alpine has registered Harmonia. See SKILL.md for setup.
