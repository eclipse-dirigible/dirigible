---
name: harmonia
description: Build web apps with the codbex-harmonia Alpine.js UI component library. Use when creating HTML pages, app layouts, forms, tables, dialogs, menus, or any UI with Harmonia components. Covers all 48 components, theming, Alpine.js integration, and no-build-step setup.
tools: Read, Write, Edit, Bash
---
> Reference copied from **codbex/codbex-athena-app** (`.claude/skills/`) for the Harmonia runtime UI work in this repo. Source of truth lives in that project; keep in sync when Harmonia changes.


# Harmonia UI Library — App Builder

Build modern web apps using **codbex-harmonia**, a zero-build-step Alpine.js component library styled with Tailwind CSS.

---

## Quick Setup (No Build Step)

```html
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
  <title>My App</title>
  <!-- 1. Harmonia CSS (Tailwind compiled) -->
  <link href="/node_modules/@codbex/harmonia/dist/harmonia.css" rel="stylesheet" />
</head>
<body>
  <!-- your markup here -->

  <!-- 2. Alpine.js MUST be deferred and loaded BEFORE Harmonia -->
  <script defer src="https://unpkg.com/alpinejs@3/dist/cdn.min.js"></script>
  <!-- 3. Harmonia IIFE bundle — registers all directives on Alpine automatically -->
  <script src="/node_modules/@codbex/harmonia/dist/harmonia.min.js"></script>
</body>
</html>
```

### CDN alternative (no npm at all)
```html
<link href="https://unpkg.com/@codbex/harmonia@1.11.0/dist/harmonia.css" rel="stylesheet" />
<script defer src="https://unpkg.com/alpinejs@3/dist/cdn.min.js"></script>
<script src="https://unpkg.com/@codbex/harmonia@1.11.0/dist/harmonia.min.js"></script>
```

### Import map (modern browsers)
```html
<script type="importmap">
{
  "imports": {
    "alpinejs": "https://unpkg.com/alpinejs@3/dist/module.esm.js",
    "@codbex/harmonia": "https://unpkg.com/@codbex/harmonia@1.11.0/dist/harmonia.esm.min.js"
  }
}
</script>
<script type="module">
  import Alpine from 'alpinejs';
  import registerComponents from '@codbex/harmonia';
  registerComponents(Alpine.plugin);
  Alpine.start();
</script>
```

**Critical rule**: Alpine must load and defer before Harmonia. The IIFE bundle auto-starts; don't call `Alpine.start()` when using the IIFE.

---

## Directive Naming Convention

All Harmonia directives follow the `x-h-{component}` pattern. Compound components use `x-h-{parent}-{child}`. Configuration is done via `data-*` attributes, never via direct props.

```html
<!-- ✓ Correct — use data-* for config -->
<button x-h-button data-variant="primary" data-size="md">Save</button>

<!-- ✓ Dynamic binding works too -->
<button x-h-button :data-variant="isActive ? 'primary' : 'outline'">Toggle</button>
```

---

## All 50 Components

### Button (`x-h-button`)

| `data-variant` | `data-size` | Notes |
|---|---|---|
| `default` | `default` | |
| `primary` | `sm` | Main CTA |
| `positive` | `md` | |
| `negative` | `icon-sm` | Destructive |
| `warning` | `icon-md` | |
| `information` | `icon` | Square icon-only |
| `outline` | | Ghost style |
| `transparent` | | No background |
| `link` | | Looks like a link |

**Attributes:**
- `data-toggled` — boolean; on/pressed state for toggle buttons (bind with `:data-toggled`).

**Modifiers:**
- `.addon` — use when the button is inside an `x-h-input-group-addon`

**Usage:** Use exactly one `primary` button for the main action in a given context; render lower-emphasis actions as `outline`, `transparent`, or `link`. The `link` variant also works on an `<a>` element for real navigation. Icon (`icon`, `icon-sm`, `icon-md`) sizes are for icon-only buttons and require an `aria-label`.

**Icon size inside buttons is automatic.** `x-h-button` sets icon size via `[&_svg]:size-*` CSS. Never add `class="size-4"` (or any other size class) to an `<i>` or `<svg>` that is a direct child of a button — it is redundant and may conflict.

```html
<!-- ✓ Correct — no size class on the icon -->
<button x-h-button data-variant="primary">
  <i role="img" data-lucide="plus"></i>
  Save changes
</button>

<!-- ✗ Wrong — size-4 is set automatically, this is redundant -->
<button x-h-button data-variant="primary">
  <i role="img" data-lucide="plus" class="size-4"></i>
  Save changes
</button>
```

Only set an explicit size on icons that live **outside** a button (card headers, table cells, input-group addons, standalone decorative icons).

```html
<!-- Primary action -->
<button x-h-button data-variant="primary">Save changes</button>

<!-- Icon-only — must have aria-label -->
<button x-h-button data-variant="outline" data-size="icon" aria-label="Settings">
  <svg>...</svg>
</button>

<!-- Link variant as a real anchor -->
<a x-h-button data-variant="link" href="#">Link</a>

<!-- Loading state — embed a spinner; it inherits the button variant color -->
<button x-h-button data-variant="primary" disabled>
  <span x-h-spinner></span>
  <span>Saving…</span>
</button>

<!-- Toggle state -->
<div x-data="{ active: false }">
  <button x-h-button data-variant="outline" :data-toggled="active"
          @click="active = !active">Toggle</button>
</div>

<!-- Disabled -->
<button x-h-button data-variant="primary" disabled>Saving…</button>
```

---

### Button Group (`x-h-button-group`)

Groups related buttons into a single container. Use for actions that share a common context or hierarchy.

| Attribute | Values | Description |
|---|---|---|
| `data-orientation` | `horizontal` (default) · `vertical` | Layout direction |

```html
<!-- Horizontal with separator -->
<div x-h-button-group>
  <button x-h-button>Left</button>
  <div x-h-button-group-separator></div>
  <button x-h-button>Right</button>
</div>

<!-- Vertical icon group -->
<div x-h-button-group data-orientation="vertical">
  <button x-h-button data-size="icon" data-variant="outline" aria-label="Zoom in">
    <svg x-h-icon.plus role="img" aria-label="plus"></svg>
  </button>
  <button x-h-button data-size="icon" data-variant="outline" aria-label="Zoom out">
    <svg x-h-icon.minus role="img" aria-label="minus"></svg>
  </button>
</div>
```

---

### Input (`x-h-input`)

Single-line input — supports `type="text"` and `type="color"`. For numeric
entry use [`x-h-input-number`](#input-number-x-h-input-number) instead.

- `data-size="sm"` — compact size (default omitted)
- `.group` modifier — when nested in an `x-h-input-group`
- `.table` modifier — borderless styling inside a table cell
- `aria-invalid="true"` — error state

```html
<!-- Basic -->
<input x-h-input type="text" placeholder="Enter value" />

<!-- Small size -->
<input x-h-input data-size="sm" type="text" />

<!-- Color picker -->
<input x-h-input type="color" value="#26a269" />

<!-- Invalid state -->
<input x-h-input type="text" aria-invalid="true" />

<!-- Bound to Alpine model -->
<div x-data="{ name: '' }">
  <input x-h-input type="text" x-model="name" />
</div>

<!-- Inside input-group (use .group modifier) -->
<div x-h-input-group>
  <div x-h-input-group-addon data-align="inline-start">
    <span x-h-input-group-text>$</span>
  </div>
  <input x-h-input.group type="text" />
</div>

<!-- Inside table cell (use .table modifier) -->
<td><input x-h-input.table type="text" /></td>
```

---

### Input Number (`x-h-input-number`)

Numeric input with built-in step controls — auto-generates increment/decrement
buttons. Requires a child `<input type="number">`. Supports native `min`, `max`,
`step`, `value`, and `aria-invalid`.

- `data-size="sm"` — compact size (default omitted)
- `.table` modifier — borderless styling inside a table cell

```html
<!-- Basic -->
<div x-data="{ qty: 1 }">
  <div x-h-input-number>
    <input type="number" x-model.number="qty" min="0" max="99" step="1" />
  </div>
</div>

<!-- Invalid state -->
<div x-h-input-number>
  <input type="number" value="12" min="0" max="10" aria-invalid="true" />
</div>

<!-- Inside table cell (use .table modifier) -->
<td x-h-table-cell>
  <div x-h-input-number.table>
    <input type="number" x-model.number="row.qty" min="0" />
  </div>
</td>
```

---

### Textarea (`x-h-textarea`)

```html
<textarea x-h-textarea rows="4" placeholder="Add notes…"></textarea>
```

---

### Checkbox (`x-h-checkbox`)

```html
<!-- Basic -->
<div x-data="{ agreed: false }">
  <div class="flex items-center gap-3">
    <span x-h-checkbox>
      <input type="checkbox" id="agree" x-model="agreed" />
    </span>
    <label x-h-label for="agree">I agree to terms</label>
  </div>
</div>

<!-- Inside x-h-field -->
<div x-data="{ agreed: false }">
  <div x-h-field data-orientation="horizontal">
    <span x-h-checkbox>
      <input type="checkbox" id="agree" x-model="agreed" />
    </span>
    <label x-h-label for="agree">I agree to terms</label>
  </div>
</div>
```

---

### Radio (`x-h-radio`)

```html
<!-- Basic -->
<div x-data="{ color: 'red' }">
  <div class="flex items-center gap-3">
    <span x-h-radio>
      <input type="radio" id="color-red" name="color" value="red" x-model="color" />
    </span>
    <label x-h-label for="color-red">Red</label>
  </div>
  <div class="flex items-center gap-3">
    <span x-h-radio>
      <input type="radio" id="color-blue" name="color" value="blue" x-model="color" />
    </span>
    <label x-h-label for="color-blue">Blue</label>
  </div>
</div>

<!-- Inside x-h-field -->
<div x-data="{ color: 'red' }">
  <div x-h-field data-orientation="horizontal">
    <span x-h-radio>
      <input type="radio" id="color-red" name="color" value="red" x-model="color" />
    </span>
    <label x-h-label for="color-red">Red</label>
  </div>
  <div x-h-field data-orientation="horizontal">
    <span x-h-radio>
      <input type="radio" id="color-blue" name="color" value="blue" x-model="color" />
    </span>
    <label x-h-label for="color-blue">Blue</label>
  </div>
</div>
```

---

### Switch (`x-h-switch`)

```html
<!-- Basic -->
<div x-data="{ enabled: false }">
  <span x-h-switch>
    <input type="checkbox" x-model="enabled" />
  </span>
</div>

<!-- With label -->
<div x-data="{ enabled: false }">
  <div class="flex items-center gap-3">
    <span x-h-switch>
      <input type="checkbox" id="notifications" x-model="enabled" />
    </span>
    <label x-h-label for="notifications">Enable notifications</label>
  </div>
</div>

<!-- Inside x-h-field -->
<div x-data="{ enabled: false }">
  <div x-h-field data-orientation="horizontal">
    <span x-h-switch>
      <input type="checkbox" id="notifications" x-model="enabled" />
    </span>
    <label x-h-label for="notifications">Enable notifications</label>
  </div>
</div>
```

---

### Select (`x-h-select` + children)

The most complex component. Uses floating-UI for dropdown positioning.

**Attributes on `x-h-select`:**
- `data-filter="starts-with|contains|contains-each|none"` — search strategy (default: `contains`)
- `data-open` — controlled open state expression

**Children:**
- `x-h-select-input` — the visible text/search input (bind `x-model` here)
- `x-h-select-clear` — clear button
- `x-h-select-content` — dropdown panel
- `x-h-select-option="'Label'"` + `data-value="val"` — each option
- `x-h-select-group` + `x-h-select-group-label` — option groups
- `x-h-select-separator` — visual divider

```html
<!-- Single select -->
<div x-data="{ fruit: '' }">
  <div x-h-select>
    <input x-h-select-input x-model="fruit" placeholder="Choose fruit…" />
    <button x-h-select-clear></button>
    <div x-h-select-content>
      <div x-h-select-option="'Apple'" data-value="apple"></div>
      <div x-h-select-option="'Banana'" data-value="banana"></div>
      <div x-h-select-option="'Cherry'" data-value="cherry"></div>
    </div>
  </div>
</div>

<!-- Grouped options -->
<div x-h-select>
  <input x-h-select-input placeholder="Select country…" />
  <div x-h-select-content>
    <div x-h-select-group>
      <div x-h-select-group-label>Europe</div>
      <div x-h-select-option="'Germany'" data-value="de"></div>
      <div x-h-select-option="'France'" data-value="fr"></div>
    </div>
    <div x-h-select-separator></div>
    <div x-h-select-group>
      <div x-h-select-group-label>Americas</div>
      <div x-h-select-option="'USA'" data-value="us"></div>
    </div>
  </div>
</div>

<!-- Dynamic options via x-for -->
<div x-data="{ selected: '', options: [{label:'Alpha',value:'a'},{label:'Beta',value:'b'}] }">
  <div x-h-select>
    <input x-h-select-input x-model="selected" />
    <div x-h-select-content>
      <template x-for="opt in options" :key="opt.value">
        <div x-h-select-option="opt.label" :data-value="opt.value"></div>
      </template>
    </div>
  </div>
</div>

<!-- Multi-select: bind array to x-model -->
<div x-data="{ tags: [] }">
  <div x-h-select>
    <input x-h-select-input x-model="tags" placeholder="Pick tags…" />
    <div x-h-select-content>
      <div x-h-select-option="'Design'" data-value="design"></div>
      <div x-h-select-option="'Engineering'" data-value="eng"></div>
    </div>
  </div>
</div>

<!-- Searchable with filter strategies -->
<div x-data="{ selected: 'banana', items: [{label:'Apple',value:'apple'},{label:'Banana',value:'banana'},{label:'Cherry',value:'cherry'}], placeholder: 'Select a fruit…' }">
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
```

### Input Group (`x-h-input-group`)

Wraps an input/textarea with prefixes, suffixes, icons, counters, or buttons.

- `x-h-input-group` — container; `data-size="sm"` for compact
- `x-h-input-group-addon` — holds an action/indicator; positioned via `data-align`:
  - `inline-start` (left prefix), `inline-end` (right suffix), `block-start` (above), `block-end` (below)
  - `data-disabled` — disable the addon
- `x-h-input-group-text` — plain text content inside the group
- Children use modifiers: inputs `x-h-input.group`, textareas `x-h-textarea.group`, buttons `x-h-button.addon`

> Place addons **after** the input in the DOM (so focus order stays correct),
> then position them visually with `data-align`.

```html
<!-- Prefix text + suffix button -->
<div x-h-input-group>
  <input x-h-input.group type="text" placeholder="domain.com" />
  <div x-h-input-group-addon data-align="inline-start">
    <span x-h-input-group-text>https://</span>
  </div>
  <div x-h-input-group-addon data-align="inline-end">
    <button x-h-button.addon aria-label="go">Go</button>
  </div>
</div>

<!-- Search with leading icon + trailing counter -->
<div x-h-input-group>
  <input x-h-input.group placeholder="Search..." />
  <div x-h-input-group-addon data-align="inline-start">
    <i role="img" data-lucide="search"></i>
  </div>
  <div x-h-input-group-addon data-align="inline-end">12 results</div>
</div>

<!-- Textarea with toolbar (top) + actions (bottom) -->
<div x-h-input-group>
  <textarea x-h-textarea.group placeholder="Message..."></textarea>
  <div x-h-input-group-addon data-align="block-start" class="border-b">
    <button x-h-button.addon data-size="icon-sm">Bold</button>
  </div>
  <div x-h-input-group-addon data-align="block-end" class="border-t">
    <button x-h-button.addon>Send</button>
  </div>
</div>
```

---

### Badge (`x-h-badge`)

Inline label. Same variants as Button.

```html
<span x-h-badge>Default</span>
<span x-h-badge data-variant="primary">New</span>
<span x-h-badge data-variant="positive">Active</span>
<span x-h-badge data-variant="negative">Deleted</span>
<span x-h-badge data-variant="warning">Pending</span>
<span x-h-badge data-variant="information">Info</span>
<span x-h-badge data-variant="outline">Draft</span>
```

---

### Card (`x-h-card`)

```html
<div x-h-card>
  <div x-h-card-header>
    <h3 x-h-card-title>Invoice #1042</h3>
    <p x-h-card-description>Due 30 Apr 2026</p>
    <div x-h-card-action>
      <button x-h-button data-variant="outline" data-size="icon" aria-label="More">…</button>
    </div>
  </div>
  <div x-h-card-content>
    <!-- body -->
  </div>
  <div x-h-card-footer>
    <button x-h-button data-variant="primary">Pay now</button>
  </div>
</div>
```

---

### Alert (`x-h-alert`)

```html
<!-- Variants: default | positive | negative | warning | information -->
<div x-h-alert data-variant="negative" role="alert">
  <i role="img" data-lucide="circle-x"></i>
  <div x-h-alert-title>Negative</div>
  <div x-h-alert-description>Negative variant description</div>
</div>

<!-- Floating (absolutely positioned overlay) -->
<div x-h-alert.floating>
  <i role="img" data-lucide="files"></i>
  <div x-h-alert-title>Floating</div>
  <div x-h-alert-description>Usually used as a notification</div>
  <div x-h-alert-actions>
    <button x-h-button data-size="icon-sm" data-variant="outline"><i role="img" data-lucide="x"></i></button>
  </div>
</div>
```

---

### Toolbar (`x-h-toolbar`)

Sub-directives: `x-h-toolbar-image` · `x-h-toolbar-title` · `x-h-toolbar-subtitle` · `x-h-toolbar-branding` · `x-h-toolbar-spacer` · `x-h-toolbar-separator`

> **Breaking change from 1.4.x**: `x-h-toolbar-actions` was removed. Replace with `x-h-toolbar-spacer` followed by directly-placed buttons.

| Attribute | Values | Description |
|---|---|---|
| `data-variant` | `default` · `transparent` | Transparent removes background fill |
| `data-size` | `default` · `md` · `sm` | Smaller toolbar |
| `data-floating` | boolean | Floating (rounded, shadowed) style |

**Modifiers:** `.footer` — footer-style toolbar

```html
<!-- Default toolbar -->
<div x-h-toolbar>
  <span x-h-toolbar-title>Documents</span>
  <div x-h-toolbar-spacer></div>
  <button x-h-button data-variant="transparent">Save</button>
  <div x-h-toolbar-separator></div>
  <button x-h-button data-variant="primary">New</button>
</div>

<!-- Floating -->
<div x-h-toolbar data-floating="true">
  <span x-h-toolbar-title>Title</span>
  <div x-h-toolbar-spacer></div>
  <button x-h-button data-variant="transparent">Action</button>
</div>

<!-- Footer style -->
<div x-h-toolbar.footer>
  <div x-h-toolbar-spacer></div>
  <button x-h-button data-size="md" data-variant="primary">Apply</button>
  <button x-h-button data-size="md" data-variant="transparent">Cancel</button>
</div>

<!-- Page header / shellbar -->
<header x-h-toolbar>
  <img x-h-toolbar-image src="/logo.svg" alt="App" />
  <span x-h-toolbar-branding>
    <h1 x-h-toolbar-title>My App</h1>
    <h2 x-h-toolbar-subtitle>by codbex</h2>
  </span>
  <div x-h-toolbar-spacer></div>
  <button x-h-button data-variant="transparent" data-size="icon" aria-label="Notifications">
    <svg x-h-icon.bell role="img" aria-label="bell"></svg>
  </button>
</header>
```

---

### Dialog (`x-h-dialog-overlay` + children)

Overlay state is controlled via `:data-open`. Auto-focuses first interactive element inside.

```html
<div x-data="{ open: false }">
  <button x-h-button data-variant="primary" @click="open = true">Open dialog</button>

  <div x-h-dialog-overlay :data-open="open">
    <div x-h-dialog>
      <div x-h-dialog-header>
        <h2 x-h-dialog-title>Confirm deletion</h2>
        <p x-h-dialog-description>This action cannot be undone.</p>
        <button x-h-dialog-close @click="open = false" aria-label="Close"></button>
      </div>
      <div x-h-dialog-content>
        <p>Are you sure you want to delete this item?</p>
      </div>
      <div x-h-dialog-footer>
        <button x-h-button data-variant="outline" @click="open = false">Cancel</button>
        <button x-h-button data-variant="negative" @click="/* delete */; open = false">Delete</button>
      </div>
    </div>
  </div>
</div>
```

---

### Popover (`x-h-popover-trigger` + `x-h-popover`)

Floating panel anchored to trigger. Uses @floating-ui/dom.

**Critical placement rule:** `x-h-popover` must appear **after** `x-h-popover-trigger` and they must share the **same direct parent**.

**Attributes on `x-h-popover`:**
- `data-align` — 12 positions: `bottom-start` · `bottom` · `bottom-end` · `right-start` · `right` · `right-end` · `left-start` · `left` · `left-end` · `top-start` · `top` · `top-end`
- `data-innerclicks="true"` — prevents the popover from closing when its content is clicked (replaces the old `@click.stop` workaround)

**Modifiers on `x-h-popover`:** `.no-scroll`

**Modifiers on `x-h-popover-trigger`:** `.chevron` — animates a chevron icon inside the trigger

#### Auto mode — `x-h-popover-trigger` with no expression

Harmonia owns the state. Closes on the next click anywhere. Use only for read-only content.

```html
<button x-h-button x-h-popover-trigger>More info</button>
<div class="w-64 p-4" x-h-popover>Read-only content.</div>
```

#### With `data-innerclicks` — interactive content

For popovers with buttons, inputs, or any clickable content, use `data-innerclicks="true"` so internal clicks don't close the popover:

```html
<button x-h-button x-h-popover-trigger>Filter</button>
<div class="w-64 p-4" x-h-popover data-innerclicks="true">
  <input x-h-input type="text" placeholder="Search…" />
  <button x-h-button data-variant="primary">Apply</button>
</div>
```

#### Controlled mode — `x-h-popover-trigger="expression"`

Pass an expression as the directive value. Harmonia evaluates it reactively and uses it to control visibility. Wire open/close yourself:

```html
<div x-data="{ open: false }">
  <button x-h-button x-h-popover-trigger="open" @click="open = !open">Filter</button>
  <div class="w-64 p-4" x-h-popover data-innerclicks="true">
    <button @click="open = false">Close</button>
  </div>
</div>
```

Add `.chevron` modifier to trigger for animated chevron icon:
```html
<button x-h-popover-trigger.chevron="open" @click="open = !open">Options</button>
```

---

### Tooltip (`x-h-tooltip-trigger` + `x-h-tooltip`)

Shows on hover. Positioned above by default.

```html
<span x-h-tooltip-trigger>
  <button x-h-button data-variant="outline" data-size="icon" aria-label="Help">?</button>
  <div x-h-tooltip>Click to open the help center</div>
</span>
```

---

### Menu (`x-h-menu-trigger` + `x-h-menu` + items)

Dropdown/context menu with keyboard navigation. Supports submenus, separators, checkbox/radio items.

**Attributes on `x-h-menu`:**
- `data-align` — 12 positions: `bottom-start` · `bottom` · `bottom-end` · `right-start` · `right` · `right-end` · `left-start` · `left` · `left-end` · `top-start` · `top` · `top-end`

**Sub-directives:** `x-h-menu-item` · `x-h-menu-item-secondary` · `x-h-menu-sub` · `x-h-menu-separator` · `x-h-menu-label` · `x-h-menu-checkbox-item` · `x-h-menu-radio-item`

> **Breaking change from 1.4.x**: `x-h-menu-item-checkbox` was renamed to `x-h-menu-checkbox-item`.

#### Usage
Use menus to present a set of related actions or navigation links. Menu items should be clearly labeled and grouped logically.

```html
<div>
  <button x-h-button x-h-menu-trigger.dropdown>Actions</button>
  <ul x-h-menu aria-label="dropdown">
    <div x-h-menu-label>Profile</div>
    <li x-h-menu-item @click="edit()">Edit</li>
    <li x-h-menu-item @click="duplicate()">Duplicate</li>
    <li x-h-menu-sub id="pnsm">
      Share
      <ul x-h-menu.sub aria-labelledby="pnsm">
        <li x-h-menu-item>Copy link</li>
        <li x-h-menu-item>Send email</li>
      </ul>
    </li>
    <li x-h-menu-separator></li>
    <li x-h-menu-item data-variant="negative" @click="remove()">Delete</li>
  </ul>
</div>

<!-- Checkbox items -->
<div x-data="{ bold: false }">
  <ul x-h-menu>
    <li x-h-menu-checkbox-item x-model="bold">Bold</li>
  </ul>
</div>

<!-- Radio items -->
<div x-data="{ radioItems: [{ label: 'Radio 1', value: 'r1' }, { label: 'Radio 2', value: 'r2' }], radioSelected: 'r1' }">
  <template x-for="radio in radioItems">
    <li x-h-menu-radio-item="radio.value" name="rg1" x-model="radioSelected" x-text="radio.label"></li>
  </template>
</div>

<!-- Context menu -->
<div x-data>
  <button x-h-menu-trigger>Right click for context menu</button>
  <ul x-h-menu aria-label="context menu">
    <li x-h-menu-item @click="edit()">Edit</li>
    <li x-h-menu-item @click="duplicate()">Duplicate</li>
    <li x-h-menu-separator></li>
    <li x-h-menu-item data-variant="negative" @click="remove()">Delete</li>
  </ul>
</div>
```

#### Behavior & Gotchas

**Auto-close uses a `window`-level click listener.** When the menu opens, Harmonia registers `top.addEventListener('click', closer)` (added in `nextTick` so the opening click doesn't immediately close it). The menu closes on any subsequent click that reaches `window`.

**Consequence:** **never put `@click.stop` on an ancestor of the menu trigger or items.** It will halt the bubble before it reaches `window`, and the menu will get stuck open. This trips up the common "row in a clickable table" pattern:

```html
<!-- BAD: @click.stop on the cell blocks the menu's auto-close -->
<tr @click="openRow(row)">
  …
  <td @click.stop>
    <button x-h-menu-trigger.dropdown>⋯</button>
    <ul x-h-menu>…</ul>
  </td>
</tr>

<!-- GOOD: gate the row's @click by target, let the click bubble freely -->
<tr @click="!$event.target.closest('[data-row-actions]') && openRow(row)">
  …
  <td data-row-actions>
    <button x-h-menu-trigger.dropdown>⋯</button>
    <ul x-h-menu>…</ul>
  </td>
</tr>
```

**Disabled menu items use `data-disabled`, not `disabled`.** Binding `:disabled` on an `<li>` does nothing useful. Bind `:data-disabled` instead:

```html
<!-- Right: :data-disabled triggers Harmonia's built-in disabled styling -->
<li x-h-menu-item
    :data-disabled="!canDelete(row)"
    @click="canDelete(row) && confirmDelete(row)">
  Delete
</li>
```

Alpine removes the attribute entirely when the bound value is `false`, so toggling between presence/absence works cleanly.

---

### Tabs (`x-h-tabs` + children)

```html
<!-- Horizontal (default) -->
<div x-h-tabs data-default-value="overview">
  <div x-h-tab-list>
    <button x-h-tab data-value="overview">Overview</button>
    <button x-h-tab data-value="analytics">Analytics</button>
    <button x-h-tab data-value="settings">Settings</button>
  </div>
  <div x-h-tab-panel data-value="overview">Overview content</div>
  <div x-h-tab-panel data-value="analytics">Analytics content</div>
  <div x-h-tab-panel data-value="settings">Settings content</div>
</div>

<!-- Vertical orientation -->
<div x-h-tabs data-orientation="vertical" data-default-value="profile">
  <div x-h-tab-list>
    <button x-h-tab data-value="profile">Profile</button>
    <button x-h-tab data-value="security">Security</button>
  </div>
  <div x-h-tab-panel data-value="profile">…</div>
  <div x-h-tab-panel data-value="security">…</div>
</div>

<!-- Floating tab bar -->
<div x-h-tabs>
  <div x-h-tab-list.floating>
    <button x-h-tab data-value="a">Tab A</button>
  </div>
  …
</div>
```

---

### Accordion (`x-h-accordion` + items)

```html
<!-- Multiple open at once (default) -->
<div x-h-accordion>
  <div x-h-accordion-item data-value="item-1">
    <button x-h-accordion-trigger>What is Harmonia?</button>
    <div x-h-accordion-content>
      Harmonia is an Alpine.js component library…
    </div>
  </div>
  <div x-h-accordion-item data-value="item-2">
    <button x-h-accordion-trigger>How do I install it?</button>
    <div x-h-accordion-content>
      Via npm or CDN…
    </div>
  </div>
</div>

<!-- Single mode — only one open at a time -->
<div x-h-accordion.single data-default-value="item-1">
  …
</div>
```

---

### Collapsible (`x-h-collapsible-trigger` + `x-h-collapsible-content`)

```html
<div x-data="{ open: false }">
  <button x-h-collapsible-trigger :data-open="open" @click="open = !open"
          x-h-button data-variant="outline">
    Advanced options
  </button>
  <div x-h-collapsible-content :data-open="open">
    <!-- hidden until open -->
    <input x-h-input type="text" placeholder="Custom option" />
  </div>
</div>
```

---

### Sidebar (`x-h-sidebar` + children)

Full application sidebar with collapsible/floating states.

```html
<div x-data="{ collapsed: false }">
  <nav x-h-sidebar :data-collapsed="collapsed">
    <div x-h-sidebar-header>
      <span x-h-sidebar-title>My App</span>
    </div>

    <div x-h-sidebar-content>
      <div x-h-sidebar-group>
        <div x-h-sidebar-group-label>Navigation</div>
        <div x-h-sidebar-menu>
          <div x-h-sidebar-menu-item>
            <a x-h-sidebar-menu-button href="/dashboard" data-active="true">
              Dashboard
            </a>
          </div>
          <div x-h-sidebar-menu-item>
            <a x-h-sidebar-menu-button href="/settings">Settings</a>
          </div>
        </div>
      </div>
    </div>

    <div x-h-sidebar-footer>
      <button x-h-sidebar-trigger @click="collapsed = !collapsed"
              :aria-label="collapsed ? 'Expand' : 'Collapse'">
      </button>
    </div>
  </nav>
</div>
```

Sidebar with sub-items (nested menu):
```html
<div x-h-sidebar-menu-item>
  <button x-h-sidebar-menu-button>Products</button>
  <div x-h-sidebar-menu-sub>
    <div x-h-sidebar-menu-sub-item>
      <a x-h-sidebar-menu-sub-button href="/products/list">All Products</a>
    </div>
    <div x-h-sidebar-menu-sub-item>
      <a x-h-sidebar-menu-sub-button href="/products/new">New Product</a>
    </div>
  </div>
</div>
```

Sidebar badge / skeleton:
```html
<a x-h-sidebar-menu-button href="/inbox">
  Inbox
  <span x-h-badge data-variant="primary" x-h-sidebar-menu-badge>5</span>
</a>

<!-- Loading state -->
<div x-h-sidebar-menu-skeleton></div>
```

---

### Table (`x-h-table-container` + children)

Sub-components: `x-h-table-container` (wrapper) → `x-h-table` (`<table>`) → `x-h-table-header` (`<thead>`), `x-h-table-body` (`<tbody>`), `x-h-table-footer` (`<tfoot>`), `x-h-table-caption` (`<caption>`), `x-h-table-row` (`<tr>`), `x-h-table-head` (`<th>`), `x-h-table-cell` (`<td>`), `x-h-table-cell-button` (`<button>` inside a cell).

**Attributes:**
| Element | Attribute | Values | Effect |
|---|---|---|---|
| `x-h-table-container` | `.scroll` (modifier) | — | makes the container scrollable (pair with `style="max-height: …"`) |
| `x-h-table-container` | `data-border` | `true` | adds an outer border around the table |
| `x-h-table` | `data-borders` | `rows` \| `columns` \| `both` | inner dividers between rows, columns, or a full grid |
| `x-h-table` | `data-fixed` | `true` | fixed table layout — **incompatible with `.scroll`** |
| `x-h-table-row` | `data-state` | `selected` | selected-row styling |
| `x-h-table-row` / `-head` / `-cell` | `data-hoverable` | `true` | hover styling |
| `x-h-table-row` / `-head` / `-cell` | `data-activable` | `true` | active/pressed styling |

> ⚠️ **Border attribute names differ by element.** The container takes boolean `data-border="true"` (outer border). The table takes `data-borders="rows|columns|both"` (plural — inner dividers). Mixing these up is the most common table mistake.

```html
<div x-h-table-container data-border="true">
  <table x-h-table data-borders="both">
    <caption x-h-table-caption>Invoices</caption>
    <thead x-h-table-header>
      <tr x-h-table-row>
        <th x-h-table-head scope="col">Name</th>
        <th x-h-table-head scope="col">Status</th>
        <th x-h-table-head scope="col">Amount</th>
      </tr>
    </thead>
    <tbody x-h-table-body>
      <template x-for="row in rows" :key="row.id">
        <tr x-h-table-row data-hoverable="true" data-activable="true">
          <th x-h-table-head x-text="row.name"></th>
          <td x-h-table-cell>
            <span x-h-badge :data-variant="row.status === 'active' ? 'positive' : 'outline'"
                  x-text="row.status"></span>
          </td>
          <td x-h-table-cell x-text="row.amount"></td>
        </tr>
      </template>
    </tbody>
    <tfoot x-h-table-footer>
      <tr x-h-table-row>
        <th x-h-table-head scope="row">Total</th>
        <td x-h-table-cell colspan="2" x-text="rows.length"></td>
      </tr>
    </tfoot>
  </table>
</div>

<!-- Scrollable container (header stays put while the body scrolls; do NOT combine with data-fixed) -->
<div x-h-table-container.scroll data-border="true" style="max-height: 688px">
  <table x-h-table data-borders="both">
    <thead x-h-table-header>…</thead>
    <tbody x-h-table-body>…</tbody>
  </table>
</div>

<!-- Border variants (plural data-borders, on the TABLE) -->
<table x-h-table data-borders="rows">…</table>    <!-- row dividers only -->
<table x-h-table data-borders="columns">…</table> <!-- column dividers only -->
<table x-h-table data-borders="both">…</table>    <!-- grid -->

<!-- Fixed layout (equal column widths; not for scroll containers) -->
<table x-h-table data-borders="both" data-fixed="true">…</table>

<!-- The first cell of a body row is conventionally a <th x-h-table-head> (row header), the rest <td x-h-table-cell> -->

<!-- Clickable cell button -->
<td x-h-table-cell>
  <button x-h-table-cell-button @click="open(row)">Edit</button>
</td>

<!-- Editable cell (uses .table input modifier) -->
<td x-h-table-cell>
  <input x-h-input.table type="text" x-model="row.name" />
</td>

<!-- Sortable column headers — use data-hoverable + data-activable on the <th>, NOT a <button> inside it -->
<thead x-h-table-header>
  <tr x-h-table-row>
    <th x-h-table-head scope="col" data-hoverable="true" data-activable="true" @click="cycleSort('name')">
      <div class="hbox items-center justify-between">
        Name
        <i role="img" data-lucide="arrow-up"      class="size-4" x-show="isSortedAsc('name')"></i>
        <i role="img" data-lucide="arrow-down"    class="size-4" x-show="isSortedDesc('name')"></i>
        <i role="img" data-lucide="arrow-up-down" class="size-4" x-show="isUnsorted('name')"></i>
      </div>
    </th>
    <th x-h-table-head scope="col">Non-sortable column</th>
  </tr>
</thead>
```

**Sortable header rule:** Do not put a `<button>` inside `<th x-h-table-head>`. Make the `<th>` itself interactive instead with `data-hoverable="true"`, `data-activable="true"`, and `@click` directly on it.

Cells can host other Harmonia components with their `.table` modifier for borderless in-cell styling: `x-h-input.table`, `x-h-select.table`, `x-h-date-picker.table`, `x-h-time-picker.table`, plus `x-h-menu-trigger.dropdown` on a `x-h-table-cell-button`.

---

### Split (`x-h-split`)

Two-pane layout with draggable gutter.

```html
<!-- Horizontal split (default) -->
<div x-h-split style="height: 500px;">
  <div x-h-split-panel data-default-size="30">Left panel</div>
  <div x-h-split-panel data-default-size="70">Right panel</div>
</div>

<!-- Vertical split -->
<div x-h-split data-orientation="vertical" style="height: 500px;">
  <div x-h-split-panel data-default-size="50">Top panel</div>
  <div x-h-split-panel data-default-size="50">Bottom panel</div>
</div>

<!-- Gutterless (no visible drag handle) -->
<div x-h-split data-gutterless="true">
  <div x-h-split-panel>Left</div>
  <div x-h-split-panel>Right</div>
</div>
```

---

### Pagination (`x-h-pagination`)

```html
<div x-data="{ page: 1, total: 10 }">
  <nav x-h-pagination>
    <button x-h-pagination-prev @click="page > 1 && page--" :disabled="page === 1">Previous</button>
    <span x-h-pagination-item x-text="page + ' / ' + total"></span>
    <button x-h-pagination-next @click="page < total && page++" :disabled="page === total">Next</button>
  </nav>
</div>
```

---

### Breadcrumb (`x-h-breadcrumb` + children)

*Added in 1.11.0.* Displays the current page's location within a navigational hierarchy, helping users understand where they are and navigate back to parent pages. Separators between items are inserted automatically by the component — do **not** add separator markup yourself.

**Sub-directives:**
- `x-h-breadcrumb` — container (`<nav>`)
- `x-h-breadcrumb-list` — the list (`<ol>`)
- `x-h-breadcrumb-item` — a single item (`<li>`)
- `x-h-breadcrumb-link` — an interactive link (`<a>` or `<button>`); the navigable ancestor segments
- `x-h-breadcrumb-page` — the current page (`<span>`, non-interactive); use for the last/active segment

| Attribute | Values | On | Purpose |
|---|---|---|---|
| `data-variant` | `default` · `outline` | `x-h-breadcrumb` | Visual styling |
| `data-size` | `default` · `sm` · `md` | `x-h-breadcrumb` | Size (outline variant only) |
| `data-overflow` | `scroll` · `nowrap` | `x-h-breadcrumb` | Overflow handling for long trails |

```html
<!-- Basic -->
<nav x-h-breadcrumb>
  <ol x-h-breadcrumb-list>
    <li x-h-breadcrumb-item>
      <a x-h-breadcrumb-link href="#">Home</a>
    </li>
    <li x-h-breadcrumb-item>
      <a x-h-breadcrumb-link href="#">Components</a>
    </li>
    <li x-h-breadcrumb-item>
      <span x-h-breadcrumb-page>Breadcrumb</span>
    </li>
  </ol>
</nav>

<!-- Outline variant -->
<nav x-h-breadcrumb data-variant="outline" data-size="sm">
  <ol x-h-breadcrumb-list>…</ol>
</nav>

<!-- Button-based links (SPA navigation) -->
<li x-h-breadcrumb-item>
  <button x-h-breadcrumb-link @click="$router.navigate('/invoices')">Invoices</button>
</li>

<!-- nowrap with a dropdown menu for overflowed segments -->
<nav x-h-breadcrumb data-overflow="nowrap">
  <ol x-h-breadcrumb-list>
    <li x-h-breadcrumb-item>
      <a x-h-breadcrumb-link href="#"><svg x-h-icon.home role="img" aria-label="home"></svg>Home</a>
    </li>
    <li x-h-breadcrumb-item>
      <button x-h-breadcrumb-link x-h-menu-trigger.dropdown>
        <svg x-h-icon.ellipsis role="img" aria-label="ellipsis"></svg>
        <span class="sr-only">Breadcrumb overflow menu</span>
      </button>
      <ul x-h-menu>
        <li x-h-menu-item><a href="#">Page 1</a></li>
        <li x-h-menu-item><a href="#">Page 2</a></li>
        <li x-h-menu-item><a href="#">Page 3</a></li>
      </ul>
    </li>
    <li x-h-breadcrumb-item>
      <a x-h-breadcrumb-link href="#">Page 8</a>
    </li>
    <li x-h-breadcrumb-item>
      <span x-h-breadcrumb-page>Breadcrumb</span>
    </li>
  </ol>
</nav>

<!-- Dynamic trail via x-for: last segment is the current page -->
<nav x-h-breadcrumb x-data="{ trail: [{label:'Home',href:'/'},{label:'Invoices',href:'/invoices'},{label:'INV-0039'}] }">
  <ol x-h-breadcrumb-list>
    <template x-for="(crumb, i) in trail" :key="i">
      <li x-h-breadcrumb-item>
        <a x-show="i < trail.length - 1" x-h-breadcrumb-link :href="crumb.href" x-text="crumb.label"></a>
        <span x-show="i === trail.length - 1" x-h-breadcrumb-page x-text="crumb.label"></span>
      </li>
    </template>
  </ol>
</nav>
```

---

### List (`x-h-list`)

Displays a list of items for viewing. Use `x-h-list-header` for group labels.

```html
<ul x-h-list>
  <li x-h-list-header>Group A</li>
  <li x-h-list-item>Item one</li>
  <li x-h-list-item>Item two</li>
</ul>
```

---

### Listbox (`x-h-listbox`)

Single-selection list with keyboard navigation (↑ ↓ Home End Enter/Space). Wrap one or more `x-h-list` elements inside:

```html
<div x-h-listbox>
  <ul x-h-list>
    <li x-h-list-header>Group 1</li>
    <li x-h-list-item>List Item 1</li>
    <li x-h-list-item>List Item 2</li>
    <li x-h-list-item>List Item 3</li>
  </ul>
  <ul x-h-list>
    <li x-h-list-header>Group 2</li>
    <li x-h-list-item>List Item 4</li>
    <li x-h-list-item>List Item 5</li>
  </ul>
</div>
```

---

### DatePicker (`x-h-date-picker`)

```html
<div x-h-date-picker x-data="{ date: new Date().toISOString() }">
  <input type="text" id="date-input-1" />
  <button x-h-date-picker-trigger aria-label="Choose date"></button>
  <div x-h-calendar x-model="date"></div>
</div>

<!-- Inside a table cell -->
<td x-h-table-cell>
  <div x-h-date-picker.table x-data="{ date: new Date().toISOString() }">
    <input type="text" id="tableDate" />
    <button x-h-date-picker-trigger aria-label="Choose date"></button>
    <div x-h-calendar x-model="date"></div>
  </div>
</td>
```

---

### TimePicker (`x-h-time-picker`)

```html
<div x-data="{ time: '' }">
  <div x-h-time-picker>
    <input x-h-input type="text" x-model="time" placeholder="HH:MM" readonly />
  </div>
</div>
```

---

### Calendar (`x-h-calendar`)

Standalone calendar (not a picker popup).

```html
<div x-data="{ selected: null }">
  <div x-h-calendar x-model="selected"></div>
</div>
```

---

### Range (`x-h-range`)

Slider powered by noUiSlider.

```html
<div x-data="{ value: 50 }">
  <div x-h-range x-model="value" data-min="0" data-max="100" data-step="1"></div>
  <span x-text="value"></span>
</div>

<!-- Multi-handle range -->
<div x-data="{ range: [20, 80] }">
  <div x-h-range x-model="range" data-min="0" data-max="100"></div>
</div>
```

---

### Progress (`x-h-progress`)

```html
<div x-data="{ pct: 65 }">
  <div x-h-progress :data-value="pct" data-max="100"></div>
</div>
```

---

### Separator (`x-h-separator`)

```html
<hr x-h-separator />
<!-- Vertical -->
<div x-h-separator data-orientation="vertical"></div>
```

---

### Spinner (`x-h-spinner`)

```html
<div x-show="loading">
  <div x-h-spinner></div>
</div>
```

---

### Skeleton (`x-h-skeleton`)

```html
<div x-show="!loaded">
  <div x-h-skeleton class="h-4 w-48"></div>
  <div x-h-skeleton class="h-4 w-32 mt-2"></div>
</div>
```

---

### Avatar (`x-h-avatar`)

`data-variant="positive|negative|warning|information"` for semantic color states (useful in notification badges).

```html
<span x-h-avatar>
  <img x-h-avatar-image src="user.jpg" alt="Jane Doe" />
  <span x-h-avatar-fallback>JD</span>
</span>

<!-- Semantic color variant -->
<span x-h-avatar class="rounded-lg" data-variant="positive">
  <svg x-h-icon.circle-success role="img" aria-label="success"></svg>
</span>
```

---

### Tag (`x-h-tag`)

```html
<span x-h-tag>TypeScript</span>
<span x-h-tag data-variant="primary">Alpine.js</span>
```

---

### Chip (`x-h-chip` + `x-h-chip-close`)

*Added in 1.10.0.* A rounded-full, **interactive** pill — unlike `x-h-badge`/`x-h-tag` (static labels), a chip is a real focusable, clickable element with hover/active states and an optional removable affordance.

**Critical element rules:**
- **`x-h-chip` must be a `<button>`** — it throws otherwise. It auto-sets `type="button"`.
- **`x-h-chip-close` must be a `<span>`** — it throws otherwise. Use a `<span>`, and keep it **empty**: Harmonia injects the "×" icon itself. Always give it an `aria-label` (e.g. `aria-label="remove chip"`). Clicking it stops propagation so it won't trigger the chip's own `@click` — except while the chip is an open popover trigger (`aria-expanded="true"`), where clicks propagate.
- **Wrap the text label in a `<span>`** when next to an icon or close button or when text truncation is required.

| `data-variant` | Notes |
|---|---|
| *(none)* | Default neutral chip |
| `primary` | |
| `positive` | |
| `negative` | |
| `warning` | |
| `information` | |
| `outline` | Bordered, no fill |

`data-variant` is watched reactively, so `:data-variant` bindings update live. Padding adjusts automatically for a leading icon, a `x-h-chip-close`, or a `x-h-spinner` (loading chip).

```html
<!-- Text only -->
<button x-h-chip>Chip</button>

<!-- Icon + text -->
<button x-h-chip>
  <svg x-h-icon.mail role="img" aria-label="mail"></svg>
  <span>Chip</span>
</button>

<!-- Icon + text + close button (chip-close is an EMPTY span — no icon inside) -->
<button x-h-chip data-variant="information">
  <svg x-h-icon.circle-info role="img" aria-label="information"></svg>
  <span>Chip</span>
  <span x-h-chip-close aria-label="remove chip"></span>
</button>

<!-- Outline variant -->
<button x-h-chip data-variant="outline">
  <svg x-h-icon.circle-info role="img" aria-label="information"></svg>
  <span>Chip</span>
  <span x-h-chip-close aria-label="remove chip"></span>
</button>

<!-- Chip as a popover trigger -->
<button x-h-chip x-h-popover-trigger data-variant="information">
  <svg x-h-icon.circle-info role="img" aria-label="information"></svg>
  <span class="text-muted-foreground">Chip:</span>
  <span>Enabled</span>
  <span x-h-chip-close aria-label="remove chip"></span>
</button>
<div class="w-64 p-4" x-h-popover>Chip Popover</div>

<!-- Dynamic list of removable filter chips -->
<div x-data="{ tags: ['design','eng','qa'] }" class="hbox gap-2 flex-wrap">
  <template x-for="tag in tags" :key="tag">
    <button x-h-chip data-variant="outline">
      <span x-text="tag"></span>
      <span x-h-chip-close aria-label="remove chip" @click="tags = tags.filter(t => t !== tag)"></span>
    </button>
  </template>
</div>
```

---

### Icon (`x-h-icon`)

Applied directly on `<svg>` elements. `role` is required (`img` or `presentation`).

```html
<!-- External SVG via data-link -->
<svg x-h-icon data-link="/logo.svg" role="img" aria-label="App logo"></svg>

<!-- Decorative / presentation only -->
<svg x-h-icon class="size-8" data-link="/icon.svg" role="presentation"></svg>

<!-- With semantic fill color -->
<svg x-h-icon class="size-8 fill-positive" data-link="/icon.svg" role="presentation"></svg>
```

**Built-in icons via modifiers** — no Lucide dependency needed:

| Modifier | Modifier | Modifier | Modifier |
|---|---|---|---|
| `.calendar` | `.check` | `.chevron-down` | `.chevron-left` |
| `.chevron-right` | `.chevrons-left` | `.chevrons-right` | `.clock` |
| `.search` | `.ellipsis` | `.minus` | `.plus` |
| `.close` | `.bell` | `.trash` | `.mail` |
| `.send` | `.export` | `.import` | `.edit` |
| `.menu` | `.reply` | `.refresh` | `.circle-info` |
| `.circle-warning` | `.circle-error` | `.circle-success` | `.circle-unknown` |
| `.circle-user` | `.home` | | |

```html
<svg x-h-icon.circle-info class="size-6" role="img" aria-label="info"></svg>
<svg x-h-icon.close class="size-4" role="img" aria-label="close"></svg>
<svg x-h-icon.bell class="size-5" role="img" aria-label="notifications"></svg>
```

---

### Text (`x-h-text`)

Semantic typography wrapper. Style is selected via a **modifier** (only the
first modifier is applied), not a data attribute.

```html
<p x-h-text>Body text</p>            <!-- default: leading-7 -->
<p x-h-text.lead>Lead paragraph</p>  <!-- text-muted-foreground text-xl -->
<p x-h-text.lg>Large text</p>        <!-- text-lg font-semibold -->
<p x-h-text.sm>Small text</p>        <!-- text-sm leading-none font-medium -->
<p x-h-text.xs>Extra small</p>       <!-- text-xs leading-none font-medium -->
<span x-h-text.muted>Secondary</span><!-- text-muted-foreground text-sm -->
<code x-h-text.code-inline>inline</code>
<pre x-h-text.code>block</pre>
<blockquote x-h-text.blockquote>Quote</blockquote>
```

Heading modifiers `.h1`…`.h6` apply heading sizes/weights (e.g. `x-h-text.h3`).

---

### Tile (`x-h-tile`)

```html
<div x-h-tile>
  <div x-h-tile-header>Metric</div>
  <div x-h-tile-value>$12,400</div>
</div>
```

---

### Sheet (`x-h-sheet`)

Slide-in panel (drawer).

```html
<div x-data="{ open: false, side: 'right' }">
  <button x-h-button @click="open = true">Open sheet</button>
  <div x-h-sheet-overlay="open">
    <div x-h-sheet :data-align="side">
      <div>sheet content</div>
      <button x-h-button data-variant="outline" @click="open = false">Close</button>
    </div>
  </div>
</div>
```

---

### Info Page (`x-h-info-page`)

#### Usage
Use Info Pages to guide users, explain empty states, or report errors in a visually distinct and informative way. Include clear messaging and actionable steps when appropriate.

**Sub-directives:**
- `x-h-info-page-header` — wraps the media, title, and description
- `x-h-info-page-media` — media container. Add the `.icon` modifier (`x-h-info-page-media.icon`) when the content is an inline SVG/icon; omit it for `<img>`.
- `x-h-info-page-title`
- `x-h-info-page-description`
- `x-h-info-page-content` — the action/content area (buttons, etc.), placed **after** the header

An optional footer link/button (e.g. `data-variant="link"`) goes **outside** the header, after the content. The `border` utility class on the root produces a bordered variant.

```html
<!-- Empty state with an icon and two actions -->
<div x-h-info-page>
  <div x-h-info-page-header>
    <div x-h-info-page-media.icon>
      <i role="img" data-lucide="folder"></i>
    </div>
    <div x-h-info-page-title>No Projects Yet</div>
    <div x-h-info-page-description>You haven't created any projects yet. Get started by creating your first project.</div>
  </div>
  <div x-h-info-page-content>
    <div class="flex gap-2">
      <button x-h-button data-variant="primary">Create Project</button>
      <button x-h-button data-variant="outline">Import Project</button>
    </div>
  </div>
  <a href="#" x-h-button data-size="sm" data-variant="link">Learn More<i role="img" data-lucide="arrow-up-right"></i></a>
</div>

<!-- With an image (no .icon modifier on media) -->
<div x-h-info-page>
  <div x-h-info-page-header>
    <div x-h-info-page-media>
      <img src="/logo/harmonia.svg" alt="@harmonia" width="256px" />
    </div>
    <div x-h-info-page-title>Harmonia</div>
    <div x-h-info-page-description>UI component library for Alpine.js</div>
  </div>
  <div x-h-info-page-content>
    <button x-h-button data-variant="primary">GitHub Page</button>
  </div>
</div>

<!-- Bordered variant -->
<div x-h-info-page class="border">
  <div x-h-info-page-header>
    <div x-h-info-page-media>
      <i role="img" data-lucide="upload"></i>
    </div>
    <div x-h-info-page-title>Upload file(s)</div>
    <div x-h-info-page-description>Drag & drop your file(s) or use the button below</div>
  </div>
  <div x-h-info-page-content>
    <button x-h-button data-variant="primary">Upload</button>
  </div>
</div>
```

---

### Fieldset (`x-h-fieldset`)

#### Usage
Use fieldsets to logically group related inputs. Each fieldset should include a descriptive legend or label.

#### Vertical form layout with grouped fields:
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
      </div>
    </fieldset>
  </div>
</form>
```

#### Horizontal form layout with grouped fields:
```html
<form>
  <div x-h-field-group>
    <fieldset x-h-fieldset>
      <legend x-h-legend>Shipping Address</legend>
      <div x-h-field-group>
        <div x-h-field data-orientation="horizontal">
          <label x-h-label for="formStreet">Street Address</label>
          <input x-h-input id="formStreet" placeholder="123 Main St" required />
          <p x-h-field-error>Street address is required</p>
          <p x-h-field-description data-hide-on-error="true">Include apartment or suite number if applicable.</p>
        </div>
      </div>
    </fieldset>
  </div>
</form>
```

---

### Notifications (`x-h-notification-overlay`)

The notification system uses a `$notifications` Alpine magic method. Place one `x-h-notification-overlay` per app (in body or top-level Alpine element). Templates are defined inside it and read once on init — changes after initialization are not picked up.

**`$notifications` API:**
- `$notifications.add({ id?, template, position?, timeout?, data? })` — show a notification
- `$notifications.update({ id, data })` — merge new data into an existing notification
- `$notifications.remove(id)` — remove by id
- `$notifications.addListener({ added?, updated?, removed? })` — returns listener ref
- `$notifications.removeListener(ref)` — cleanup

**Positions:** `top-left` · `top-center` · `top-right` · `bottom-left` · `bottom-center` · `bottom-right`

On medium screens, center positions shift to right. On mobile, they shift to top/bottom center.

**Sub-directives:** `x-h-notification` · `x-h-notification-media` · `x-h-notification-title` · `x-h-notification-description` · `x-h-notification-actions` · `x-h-notification-close` · `x-h-notification-list`

**`x-h-notification` attributes:** `data-unread="true"` · `data-variant="toast"`

**`x-h-notification-actions` attributes:** `data-orientation="vertical|horizontal"`

**`x-h-notification` modifier:** `.floating` — rounded corners and shadow

```html
<!-- 1. Place overlay once in your app -->
<section x-h-notification-overlay>
  <template id="basic">
    <li x-h-notification.floating>
      <div x-h-notification-title x-text="title"></div>
    </li>
  </template>
  <template id="closable">
    <li x-h-notification.floating class="hbox">
      <div class="vbox flex-1">
        <h1 x-h-notification-title x-text="title"></h1>
        <p x-h-notification-description x-text="message"></p>
      </div>
      <div x-h-notification-actions data-orientation="vertical">
        <button x-h-button x-h-notification-close data-variant="transparent" data-size="icon-sm" aria-label="close">
          <svg x-h-icon.close role="img" aria-label="close"></svg>
        </button>
      </div>
    </li>
  </template>
</section>

<!-- 2. Trigger from anywhere -->
<button x-h-button
  @click="$notifications.add({ template: 'basic', data: { title: 'Done!' }, timeout: 3000 })">
  Notify
</button>

<!-- Notification list inside a popover -->
<div x-h-popover data-innerclicks="true">
  <ol x-h-notification-list class="min-w-md">
    <li x-h-notification class="hbox" data-unread="true">
      <div x-h-notification-title>Unread notification</div>
    </li>
  </ol>
</div>
```

If using Lucide icons inside templates: `lucide.createIcons({ inTemplates: true })`.

---

### Tree (`x-h-tree`)

Hierarchical expandable tree with keyboard navigation (↑ ↓ → ← Enter/Space).

**Attributes:**
- `data-border="true"` on `x-h-tree.sub` — adds right border to indicate nesting
- `data-indicator` on `x-h-tree-button` — `positive|negative|warning|information`
- `.expanded="true"` modifier on `x-h-tree-item` — default expanded state

```html
<ul x-h-tree>
  <li x-h-tree-item.expanded="true">
    <button x-h-tree-button data-indicator="positive">
      <i role="img" data-lucide="folder"></i>
      <span>Folder 1</span>
    </button>
    <ul x-h-tree.sub data-border="true">
      <li x-h-tree-item>
        <button x-h-tree-button>
          <i role="img" data-lucide="file-text"></i>
          <span>File 1</span>
        </button>
      </li>
      <li x-h-tree-item.expanded="true">
        <button x-h-tree-button>
          <i role="img" data-lucide="folder"></i>
          <span>Subfolder</span>
        </button>
        <ul x-h-tree.sub data-border="true">
          <li x-h-tree-item>
            <button x-h-tree-button>
              <i role="img" data-lucide="file-text"></i>
              <span>File 2</span>
            </button>
          </li>
        </ul>
      </li>
    </ul>
  </li>
</ul>
```

---

## Utilities

### Theme (Light / Dark / Auto)

```javascript
// Get current scheme: 'light' | 'dark' | 'auto'
Harmonia.getColorScheme();

// Set scheme — persists to localStorage
Harmonia.setColorScheme('dark');
Harmonia.setColorScheme('light');
Harmonia.setColorScheme('auto');  // follows OS preference

// Listen for changes
const remove = Harmonia.addColorSchemeListener((scheme) => {
  console.log('Scheme changed to:', scheme);
});
remove(); // cleanup

// Alpine integration example
<div x-data="{ scheme: Harmonia.getColorScheme() }">
  <button @click="Harmonia.setColorScheme('dark'); scheme = 'dark'">Dark</button>
  <button @click="Harmonia.setColorScheme('light'); scheme = 'light'">Light</button>
</div>
```

### Breakpoint Listener

```javascript
const bp = Harmonia.getBreakpointListener(
  (isMobile) => {
    console.log('Is mobile:', isMobile);
  },
  768  // optional, defaults to 768px
);
bp.remove(); // cleanup when done
```

### `x-h-focus` directive

Auto-focuses the element when expression is truthy.

```html
<div x-data="{ showSearch: false }">
  <input x-h-input x-h-focus="showSearch" type="search" />
</div>
```

### `x-h-template` directive

Clones a `<template>` element with Alpine scope.

```html
<template id="my-card">
  <div x-h-card>...</div>
</template>

<div x-h-template="my-card" x-data="{ title: 'Hello' }"></div>
```

### `x-h-include` directive

Fetches and injects an external same-origin HTML fragment. Executes before Alpine binding.

> ⚠ Only use on trusted content. Never on dynamic/user-provided content (XSS risk).

**Modifier:** `.js` — allows `<script>` tags inside the fragment to execute (disabled by default).

```html
<div x-h-include="'/components/header.html'"></div>

<!-- Execute scripts inside fragment -->
<div x-h-include.js="'/components/widget.html'"></div>
```

---

## Theming via CSS Variables

Override in your own stylesheet — only redefine what you need.

```css
:root {
  /* Brand colors (oklch recommended) */
  --primary: oklch(0.55 0.22 240);
  --primary-foreground: oklch(1 0 0);
  --primary-hover: oklch(0.50 0.22 240);
  --primary-active: oklch(0.45 0.22 240);

  /* Shape */
  --radius-control: 0.375rem;   /* buttons, inputs */
  --radius: 0.5rem;              /* cards, panels */

  /* Typography */
  --font-sans: 'Inter', sans-serif;
}

.dark {
  --primary: oklch(0.60 0.22 240);
  --background: oklch(0.13 0 0);
  --foreground: oklch(0.95 0 0);
}
```

Key token groups:
- `--background` / `--foreground` — page base
- `--primary` / `--positive` / `--negative` / `--warning` / `--information` — semantic colors (each has `-foreground`, `-hover`, `-active` variants)
- `--card` / `--popover` — surface colors
- `--input-inner` / `--input-border` — input colors
- `--sidebar-*` — sidebar-specific
- `--table-header` / `--table-hover` / `--table-active`
- `--shadow-xs` through `--shadow-xl`
- `--spacing` — base spacing unit

---

## Typical App Structure

```html
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
  <title>My App</title>
  <link href="/node_modules/@codbex/harmonia/dist/harmonia.css" rel="stylesheet" />
  <style>
    :root { --primary: oklch(0.55 0.22 260); }
    html, body { height: 100%; margin: 0; }
    .app-layout { display: flex; height: 100vh; overflow: hidden; }
    .app-main { flex: 1; overflow-y: auto; }
  </style>
</head>
<body>
  <div x-data="appState()" class="app-layout">
    <!-- Sidebar -->
    <nav x-h-sidebar :data-collapsed="sidebarCollapsed">
      <div x-h-sidebar-header>
        <span x-h-sidebar-title>App Name</span>
      </div>
      <div x-h-sidebar-content>
        <div x-h-sidebar-menu>
          <div x-h-sidebar-menu-item>
            <a x-h-sidebar-menu-button href="#" :data-active="page === 'home'"
               @click.prevent="page = 'home'">Home</a>
          </div>
        </div>
      </div>
      <div x-h-sidebar-footer>
        <button x-h-sidebar-trigger @click="sidebarCollapsed = !sidebarCollapsed"></button>
      </div>
    </nav>

    <!-- Main area -->
    <div class="app-main">
      <!-- Toolbar -->
      <div x-h-toolbar>
        <span x-h-toolbar-title x-text="pageTitle"></span>
        <div x-h-toolbar-spacer></div>
        <button x-h-button data-variant="primary" @click="openCreate()">New</button>
      </div>

      <!-- Page content -->
      <div class="p-4">
        <div x-h-table-container.scroll data-border="true" style="max-height: 70vh">
          <table x-h-table data-borders="rows">
            <thead x-h-table-header>
              <tr x-h-table-row>
                <th x-h-table-head scope="col">Name</th>
                <th x-h-table-head scope="col">Status</th>
              </tr>
            </thead>
            <tbody x-h-table-body>
              <template x-for="item in items" :key="item.id">
                <tr x-h-table-row data-hoverable="true">
                  <th x-h-table-head x-text="item.name"></th>
                  <td x-h-table-cell>
                    <span x-h-badge :data-variant="item.active ? 'positive' : 'default'"
                          x-text="item.active ? 'Active' : 'Inactive'"></span>
                  </td>
                </tr>
              </template>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  </div>

  <!-- Create dialog -->
  <div x-h-dialog-overlay :data-open="createOpen">
    <div x-h-dialog>
      <div x-h-dialog-header>
        <h2 x-h-dialog-title>New item</h2>
        <button x-h-dialog-close @click="createOpen = false"></button>
      </div>
      <div x-h-dialog-content>
        <fieldset x-h-fieldset>
          <label x-h-label>Name</label>
          <input x-h-input type="text" x-model="form.name" />
        </fieldset>
      </div>
      <div x-h-dialog-footer>
        <button x-h-button data-variant="outline" @click="createOpen = false">Cancel</button>
        <button x-h-button data-variant="primary" @click="save()">Save</button>
      </div>
    </div>
  </div>

  <script defer src="https://unpkg.com/alpinejs@3/dist/cdn.min.js"></script>
  <script src="/node_modules/@codbex/harmonia/dist/harmonia.min.js"></script>
  <script>
    function appState() {
      return {
        page: 'home',
        sidebarCollapsed: false,
        createOpen: false,
        items: [],
        form: { name: '' },
        get pageTitle() { return this.page.charAt(0).toUpperCase() + this.page.slice(1); },
        openCreate() { this.form = { name: '' }; this.createOpen = true; },
        save() { this.items.push({ id: Date.now(), name: this.form.name, active: true }); this.createOpen = false; }
      };
    }
  </script>
</body>
</html>
```

---

## Best Practices & Gotchas

1. **Script load order matters**: Alpine must have `defer` and come before Harmonia's script tag. Harmonia patches Alpine on load.

2. **Icon-only buttons need `aria-label`**: `<button x-h-button data-size="icon" aria-label="Delete">`.

3. **Dialog auto-focus**: Harmonia auto-focuses the first focusable child inside `x-h-dialog`. Place the primary action button first, or your first input.

4. **Select with dynamic options**: Wrap `x-h-select-option` in `<template x-for>`. The label is the expression value (`x-h-select-option="opt.label"`), not a static string.

5. **Multi-select bind an array**: `x-model` on `x-h-select-input` should point to an array `[]` to enable multi-select mode.

6. **Table editable cells**: Use `x-h-input.table` (with the `.table` modifier) inside `x-h-table-cell` to get proper borderless table-cell styling.

7. **Input inside input-group**: Use `x-h-input.group` (with the `.group` modifier) for inputs inside an `x-h-input-group`. Use `x-h-button.addon` for buttons inside `x-h-input-group-addon`.

8. **Popover placement**: `x-h-popover` must be placed **after** its trigger and they must share the same direct parent. Use `data-innerclicks="true"` to prevent closing on internal clicks — no `@click.stop` needed. For programmatic control, pass an expression to `x-h-popover-trigger="open"` and wire toggle with `@click`.

9. **Sidebar collapse**: Bind `:data-collapsed` on `x-h-sidebar`. The sidebar handles the animation itself.

10. **Bind state via `:data-*`, not the corresponding standard HTML attribute.** Harmonia's CSS uses Tailwind's `data-[…]:` attribute selectors — `data-[disabled]:opacity-50`, `data-[active=true]:bg-…`, etc. These match the `data-` prefix. Always bind to the data-prefixed name: `:data-disabled`, `:data-active`, `:data-toggled`, `:data-collapsed`, `:data-variant`. Alpine removes the attribute entirely when the bound value is `false`, so toggling between presence/absence works cleanly. (Form elements like `<input>` and `<button>` still need `:disabled` for native semantics.)

11. **Outside-click auto-close in menus uses `top.addEventListener('click', …)`.** Any ancestor with `@click.stop` will permanently break auto-close. Gate ancestor click handlers by target with `closest('[data-marker]')` instead of stopping propagation. See the Menu "Behavior & Gotchas" section for the full pattern.

12. **No `Alpine.start()` with IIFE**: The `harmonia.min.js` IIFE bundle registers components and starts Alpine automatically. Don't call `Alpine.start()` separately.

13. **Color scheme persists**: `Harmonia.setColorScheme()` saves to `localStorage` under `codbex.harmonia.colorMode`. On page load Harmonia reads this and applies the appropriate class to `<html>`.

14. **Floating UI cleanup**: Positioning components auto-clean up their floating-ui update subscriptions when Alpine destroys the element. No manual cleanup needed.

15. **Accordion single mode**: Add `.single` modifier to `x-h-accordion` to allow only one open item.

16. **Split panel sizes**: `data-default-size` values are percentages. They should add up to 100 across all panels.

17. **Notification templates are static**: Templates inside `x-h-notification-overlay` are read once on init. Changes or new templates added after initialization are not picked up.

18. **Tree keyboard navigation**: Arrow keys navigate between visible items. Right expands / focuses first child, Left collapses / focuses parent. Only use `x-h-tree.sub` for nested levels.

---

## Utility Classes (Tailwind subset compiled into Harmonia)

**Critical**: Harmonia's compiled CSS contains a *fixed, curated subset* of Tailwind utilities — not the whole Tailwind universe. Apps built on Harmonia have **no build step**, so you cannot generate new utilities at runtime. **Use only the classes listed below.** Anything outside this list (e.g. `bg-blue-500`, `p-16`, `text-2xl/relaxed`) will silently render as nothing.

**Allowed conventions:**
- Numeric scale for spacing/sizing/gap: **0 to 12** only
- Grid columns: **1 to 12**
- Color palette is **semantic only** (`primary`, `secondary`, `muted`, `positive`, `negative`, `warning`, `information`, `background`, `foreground`) — no hue scales like `gray-500`
- Responsive prefixes: **`sm:`, `md:`, `lg:`, `xl:`** only
- Force a class with `!` prefix (e.g. `!p-2`, `!w-full`) to override component built-in spacing

---

### Display

`flex` · `grid` · `block` · `inline` · `inline-block` · `inline-flex` · `hidden` · `sr-only`

---

### Flex layout

Harmonia adds two shorthands on top of standard flex:

| Class | Meaning |
|---|---|
| `vbox` | `flex flex-col` |
| `hbox` | `flex flex-row` |

Standard flex (must include `flex` for any `flex-*` modifier to apply):

`flex` · `flex-row` · `flex-col` · `flex-row-reverse` · `flex-col-reverse` · `flex-wrap` · `flex-1` · `flex-auto` · `flex-none`

```html
<div class="vbox gap-2">…</div>
<div class="hbox gap-1">…</div>
<div class="flex flex-row flex-wrap gap-2">…</div>
```

---

### Grid layout

| Class | Meaning |
|---|---|
| `grid` | `display: grid` |
| `grid-cols-1` … `grid-cols-12` | Number of columns (responsive: `sm:grid-cols-2`, etc.) |

Item alignment inside grids:

`justify-items-start` · `justify-items-center` · `justify-items-end` · `justify-items-stretch`
`place-items-start` · `place-items-center` · `place-items-end` · `place-items-stretch`

```html
<!-- Responsive grid -->
<div class="grid grid-cols-1 gap-1 sm:grid-cols-2 md:grid-cols-4 lg:grid-cols-8 xl:grid-cols-12">…</div>
```

---

### Justify content (main axis)

`justify-start` · `justify-center` · `justify-end` · `justify-end-safe` · `justify-between` · `justify-around` · `justify-evenly`

`justify-end-safe` falls back to `start` when content overflows so items stay reachable.

---

### Align items (cross axis)

`items-start` · `items-center` · `items-end` · `items-baseline` · `items-stretch`

---

### Align content (multi-row alignment in grid / wrapping flex)

`content-start` · `content-center` · `content-end` · `content-between` · `content-around` · `content-evenly` · `content-stretch`

---

### Place content (justify + align together)

`place-content-start` · `place-content-center` · `place-content-end` · `place-content-between` · `place-content-around` · `place-content-evenly` · `place-content-stretch`

---

### Gap (flex & grid)

| Class | Meaning |
|---|---|
| `gap-0` … `gap-12` | All sides |
| `gap-x-0` … `gap-x-12` | Horizontal only |
| `gap-y-0` … `gap-y-12` | Vertical only |

```html
<div class="grid grid-cols-4 gap-x-1 gap-y-4">…</div>
```

---

### Margin

Range **0–12** for each:

| Class | Meaning |
|---|---|
| `m-0` … `m-12` | All sides |
| `mt-*` / `mb-*` / `ml-*` / `mr-*` | Single side |
| `mx-*` / `my-*` | Two sides |
| `ml-auto` / `mr-auto` | Push with auto-margin |

Responsive prefixes (`sm:`, `md:`, `lg:`, `xl:`) only work on the **double-sided** variants (`mx-*`, `my-*`) and only for values **2–12**. Force with `!` prefix when overriding component defaults: `!mx-4`.

---

### Padding

Same range/rules as margin:

`p-0` … `p-12` · `pt-*` / `pb-*` / `pl-*` / `pr-*` · `px-*` / `py-*`

Responsive `sm:`/`md:`/`lg:`/`xl:` only on `px-*`/`py-*` for values 2–12. Force with `!`.

---

### Width

| Class | Meaning |
|---|---|
| `w-auto` | Auto |
| `w-full` | 100% |
| `w-screen` | 100vw |
| `w-4` … `w-12` | Fixed sizes |
| `w-1/2`, `w-1/3`, `w-2/3`, `w-1/4`, `w-3/4`, `w-1/5`, `w-2/5`, `w-3/5`, `w-4/5` | Fractional |
| `w-3xs` … `w-7xl` | Named container sizes |

Named size reference: `3xs`=16rem · `2xs`=18rem · `xs`=20rem · `sm`=24rem · `md`=28rem · `lg`=32rem · `xl`=36rem · `2xl`=42rem · `3xl`=48rem · `4xl`=56rem · `5xl`=64rem · `6xl`=72rem · `7xl`=80rem

**Min-width:**
`min-w-0` · `min-w-4` … `min-w-12` · `min-w-3xs` … `min-w-7xl`

**Max-width:**
`max-w-screen` · `max-w-dvw` · `max-w-4` … `max-w-12` · `max-w-3xs` … `max-w-7xl`

(Other fractions like `w-2/4` or arbitrary widths like `w-[200px]` are NOT compiled.)

---

### Height

| Class | Meaning |
|---|---|
| `h-auto` | Auto |
| `h-full` | 100% |
| `h-screen` | 100vh |
| `h-1/2` | 50% |
| `h-4` … `h-12` | Fixed sizes |

**Min/max height:** `min-h-4` … `min-h-12` · `max-h-4` … `max-h-12`

(No other height fractions are available.)

---

### Size (square width × height)

| Class | Meaning |
|---|---|
| `size-full` | Full container |
| `size-fit` | Fit to content |
| `size-4` … `size-12` | Equal width & height |
| `min-size-4` | Min square (used for icons) |

Force any of these with `!` prefix: `!size-8`.

---

### Tile (dashboard containers)

| Class | Description |
|---|---|
| `tile-sm` | Small container |
| `tile-md` | Medium container |
| `tile-lg` | Large container |
| `tile-xl` | Extra large container |
| `tile-double-sm` … `tile-double-xl` | Double-width variants |
| `tile-auto-sm` … `tile-auto-xl` | Auto-width variants |

> **Breaking change from 1.4.x**: `tile-xs` was removed; `tile-xl` was added.

---

### Color (semantic only — no hues)

Background:
`bg-background` · `bg-foreground` · `bg-primary` · `bg-secondary` · `bg-muted` · `bg-negative` · `bg-positive` · `bg-warning` · `bg-information`

Text:
`text-background` · `text-foreground` · `text-primary` · `text-primary-foreground` · `text-secondary` · `text-secondary-foreground` · `text-muted-foreground` · `text-negative` · `text-negative-foreground` · `text-positive` · `text-positive-foreground` · `text-warning` · `text-warning-foreground` · `text-information` · `text-information-foreground`

SVG fill:
`fill-current` · `fill-primary` · `fill-primary-foreground` · `fill-secondary` · `fill-secondary-foreground` · `fill-muted` · `fill-muted-foreground` · `fill-negative` · `fill-negative-foreground` · `fill-positive` · `fill-positive-foreground` · `fill-warning` · `fill-warning-foreground` · `fill-information` · `fill-information-foreground`

```html
<!-- Always pair bg-* with the matching *-foreground for legibility -->
<p class="bg-primary p-4 text-primary-foreground">Primary block</p>
<p class="p-4 text-negative">Inline error text</p>
<svg class="size-8 fill-positive">…</svg>
```

---

### Border

| Class | Meaning |
|---|---|
| `box-border` | `box-sizing: border-box` |
| `border` | All four sides |
| `border-t` / `border-r` / `border-b` / `border-l` | Single side |

The border colour comes from the active theme; for dashed/dotted/widths use inline style or extend the theme.

---

### Shadow

`shadow-none` · `shadow-xs` · `shadow-sm` · `shadow-md` · `shadow-lg` · `shadow-xl`

```html
<div class="rounded-md p-4 shadow-md">Card-like</div>
```

---

### Opacity

`opacity-0` · `opacity-25` · `opacity-50` · `opacity-75` · `opacity-100`

---

### Cursor

`cursor-pointer` · `cursor-text` · `cursor-crosshair` · `cursor-wait` · `cursor-not-allowed` · `cursor-grab` · `cursor-grabbing` · `cursor-none`

---

### Text

Size: `text-xs` · `text-sm` · `text-base` · `text-lg` · `text-xl` · `text-2xl` · `text-3xl` · `text-4xl`

Alignment: `text-left` · `text-right` · `text-center` · `text-justify`

Overflow: `text-ellipsis` · `text-wrap` · `text-nowrap` · `truncate`

Weight: `font-light` · `font-normal` · `font-medium` · `font-semibold` · `font-bold`

Style/transform: `italic` · `uppercase` · `lowercase` · `capitalize` · `align-middle`

Whitespace: `whitespace-nowrap` · `whitespace-pre` · `whitespace-pre-wrap`

> Prefer the `x-h-text` component for body text when possible; reach for these utilities only for one-off styling.

---

### Position

| Class | Meaning |
|---|---|
| `absolute` | `position: absolute` |
| `relative` | `position: relative` |
| `absolute-fit` | `top:0; left:0; right:0; bottom:0` (Harmonia-specific) |

```html
<!-- Scrollable content filling its parent -->
<div class="relative">
  <div class="absolute absolute-fit overflow-auto">…long content…</div>
</div>
```

---

### Overflow

`overflow-auto` · `overflow-hidden` · `overflow-visible` · `overflow-scroll`
`overflow-x-auto` · `overflow-x-hidden` · `overflow-x-visible` · `overflow-x-scroll`
`overflow-y-auto` · `overflow-y-hidden` · `overflow-y-visible` · `overflow-y-scroll`
`scrollbar-none` (hide scrollbars while keeping scroll)

---

### Object fit (images)

`object-contain` · `object-cover` · `object-fill`

---

### Rotate

`rotate-90` · `rotate-180` · `rotate-270`

(Note: `rotate-270` is Harmonia-specific; standard Tailwind would use `-rotate-90`.)

---

### `!important` override

Many Harmonia components apply their own paddings/margins/sizes. To force a utility to win, prefix with `!`:

```html
<button x-h-button class="!px-8 !py-4">Force larger padding</button>
<div x-h-card class="!w-full">Force full-width card</div>
```

---

### Responsive prefixes

Only these breakpoint prefixes are compiled:

| Prefix | Min-width |
|---|---|
| `sm:` | small |
| `md:` | medium |
| `lg:` | large |
| `xl:` | extra-large |

(No `2xl:`, no `max-*:`, no arbitrary breakpoints. Use `Harmonia.getBreakpointListener()` for JS-driven responsive logic.)

```html
<div class="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4">…</div>
<div class="px-2 sm:px-4 md:px-8 lg:px-12">…</div>
```

---

### What is NOT available

Avoid these — they will not produce any styling:

- Colour hue scales: `bg-red-500`, `text-blue-700`, `border-gray-300`
- Arbitrary values: `w-[42px]`, `text-[#ff0000]`, `p-[5px]`
- Numeric spacing > 12: `p-16`, `m-20`, `gap-24`
- Other Tailwind utilities not listed above: `divide-*`, `ring-*`, `space-x-*`, `inset-*`, `top-*`, `z-*`, `transition-*`, `duration-*`, `transform`, `scale-*`, `translate-*`, `skew-*`, `aspect-*`, `columns-*`, `backdrop-*`, `blur-*`, etc.

For anything outside the list, use **inline `style="…"`** or extend the theme via CSS variables (see Theming section above).

```html
<!-- Use inline style for one-off values -->
<div style="max-height: 80vh;">…</div>
<div style="z-index: 50;">…</div>
```
