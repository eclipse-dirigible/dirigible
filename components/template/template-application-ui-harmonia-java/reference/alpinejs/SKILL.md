---
name: alpinejs
description: Architect non-trivial Alpine.js applications — deciding between x-data and Alpine.store(), organizing pages/views as components, structuring folders and JS modules, integrating routers like Pinecone Router, and handling both build-step and CDN-only setups. Use this skill whenever the user asks about Alpine.js project structure, file organization, "best practices," scaling beyond a one-page demo, where to put business logic, how to organize stores/components, routing in Alpine, SPA-style page modules, "x-data vs store," CDN versus bundler trade-offs, or how to migrate inline x-data into proper JS modules — even if they don't say "architecture" explicitly. Don't trigger for simple syntax questions or single-component how-tos that have nothing to do with structure.
---
> Reference copied from **codbex/codbex-athena-app** (`.claude/skills/`) for the Harmonia runtime UI work in this repo. Source of truth lives in that project; keep in sync when Harmonia changes.


# Alpine.js Application Architecture

Use this skill to give architectural guidance for real Alpine.js applications — projects beyond single-file demos, where state ownership, file organization, routing, and the build-vs-CDN choice all matter. The goal is to produce advice that scales: code the user can keep adding to without it turning into spaghetti.

## How to use this skill

1. **Diagnose the question.** Most Alpine architecture questions reduce to one of: (a) where does this state live, (b) how do I organize files, (c) how do I do routing, (d) how do I do this without a build step. Identify which one(s) apply.
2. **Apply the decision rules below** to give a concrete answer, not a list of options.
3. **Show code at the right level of detail.** Component skeletons, store skeletons, and folder trees are usually more useful than long working examples.
4. **Load reference files only when relevant** — see "Reference files" at the bottom. Don't dump every reference into one answer.

## Core decision: `x-data` vs `Alpine.store()`

This is the question users ask most, and getting it right unlocks everything else.

**`x-data` (via `Alpine.data()`) — local, component-scoped, DOM-tied.** Each `x-data` element owns its own reactive scope. State is created when the element mounts and destroyed when it unmounts. `init()` runs on mount. `$refs`, `$el`, `$watch`, and `$dispatch` all work naturally.

**`Alpine.store()` — global, app-scoped, DOM-independent.** Stores are singletons. They live for the entire session, are accessed via `$store.name` in templates and `Alpine.store('name')` in JS, and have no concept of mount/unmount.

**The rule:** if two unrelated components need to read or write the same data, it's a store. If only one component (and its descendants) needs it, it's `x-data`. When in doubt, default to `x-data` — promoting local state to a store later is easy; demoting a store back to local state usually means untangling shared references.

**Common store cases:** auth/current user, permissions, cart, theme, locale, toast/notification queue, feature flags, cross-page caches.

**Common `x-data` cases:** modals, dropdowns, forms, tabs, accordions, data tables, *and entire pages/views in an SPA*.

## Pages and views are components, not stores

This deserves its own section because users frequently get it wrong. A "dashboard," "users list," or "settings" page is conceptually a component — it mounts when navigated to, does its work, unmounts on leave. That maps to `x-data`/`Alpine.data()`, not `Alpine.store()`.

**Why pages should not be stores:**
- Stores are singletons that persist for the whole session. A page-as-store keeps stale filters, pagination, selected rows, and form state across navigations unless you manually reset everything.
- Stores have no DOM lifecycle. You'd have to manually call `Alpine.store('users').load()` from your router — which is exactly the work `x-data`'s `init()` does for free.
- `$refs`, `$el`, `$watch` on local DOM, and `$dispatch` from a known element only work inside component scope.
- Memory: stores never get garbage collected.

**What does still belong in a store, even for a "page" feature:**
- Cross-page caches (e.g. a `usersCache` so navigating back doesn't refetch).
- UI preferences that survive navigation (e.g. saved filter sets, last-used sort).
- Anything the layout/shell reads regardless of active page.

A typical page component reads from shared stores (`$store.auth`, `$store.notifications`) but owns its own list, filters, modal state, and selection locally. See `references/page-component-template.md` for a full template.

## Naming conventions and registration

- Use `Alpine.data('name', factory)` for reusable component logic. Inline `x-data="{ ... }"` is fine for trivial toggles but becomes unmaintainable past ~10 lines.
- Component factory functions accept initial state as a parameter — useful for server-rendered data: `<div x-data="productCard({{ product|json }})">`.
- Use `init()` for setup. The magics (`$el`, `$refs`, `$watch`, `$dispatch`, `$store`, `$nextTick`) are available inside any method of the data object — they're bound through `this`. They do NOT work in the factory function body itself, because `this` isn't bound yet there. Put initialization logic in `init()`, not at the top of the factory.
- Use `destroy()` for cleanup. If `init()` set up an interval, websocket, or external library, `destroy()` runs when the component is unmounted (e.g. when an `x-if` becomes false, or when navigating away from a route). This is critical for page components — without it, timers and listeners leak.
- Prefer `get` accessors for derived values and `this.$watch('foo', ...)` for side effects on change.
- Communicate between distant components with `$dispatch` (custom events) for fire-and-forget signals, or stores when state must persist.

## Folder structure

There are two structures depending on whether the project has a build step. Pick one — never mix.

**Default (with build step — Vite, esbuild, Laravel/Rails asset pipeline):**

```
src/js/
├── app.js                  # Entry: registers everything, calls Alpine.start()
├── stores/
│   ├── index.js            # Registers all stores
│   ├── auth.js
│   ├── cart.js
│   └── notifications.js
├── components/
│   ├── index.js            # Registers all Alpine.data components
│   ├── pages/              # One file per route — bound to a single page
│   │   ├── usersPage.js
│   │   └── dashboardPage.js
│   ├── shared/             # Reusable across pages (modal, dataTable, etc.)
│   └── layout/             # Navbar, sidebar, app shell
├── router/
│   ├── index.js            # Registers Pinecone, defines routes
│   ├── handlers.js         # Guards, redirects, route-level prep
│   └── middleware.js
├── directives/             # Custom x- directives
├── magics/                 # Custom $ magics
├── services/               # Framework-agnostic — fetch wrappers, domain logic
│   ├── api.js
│   ├── usersService.js
│   └── authService.js
└── utils/                  # Pure helpers — formatters, validators
```

The `pages/` vs `shared/` split inside `components/` matters: page components are bound to one route and named after it; shared components are dropped into many places.

For full registration code (`app.js`, `stores/index.js`, `components/index.js`), see `references/build-setup.md`.

**Without a build step (CDN-only):** the architecture is the same, but registration uses globals and the `alpine:init` event instead of ES modules. See `references/cdn-setup.md` — load that reference whenever the user is constrained to CDN, can't use npm, or asks how to do this on plain HTML / Laravel Blade / Rails ERB / Django / WordPress.

## Routing

For SPA-style routing inside Alpine, the standard choice is **Pinecone Router**. It works at the DOM level — routes are declared as `<template x-route>` elements and Pinecone shows/hides matched templates as the URL changes.

**The clean separation:**
- **Routes** declare paths and which handler runs.
- **Handlers** do guards, redirects, and route-level prep (auth checks, fetching shared data into stores). Handlers do NOT own page state.
- **`x-data` on the route's template** owns page state, runs `init()` on entry, and is destroyed on exit — giving you fresh state per visit, for free.
- **Stores** hold cross-cutting state (auth, current user, notifications) that handlers and pages both read.

Minimal example:

```html
<template x-route="/users" x-handler="App.routes.users">
  <div x-data="usersPage()">…</div>
</template>
```

```js
// router/handlers.js
export const routes = {
  users(context) {
    if (!Alpine.store('auth').can('users.view')) context.redirect('/')
  }
}
```

For a fuller setup including 404s, route params, list-page UX patterns ("preserve filters on back"), and the `x-template` directive for HTML-file-per-page apps, see `references/routing.md`.

## Services layer (framework-agnostic)

Anything that talks to the API or transforms data lives in `services/` and **knows nothing about Alpine**. Components and stores call into services. Two reasons this matters:

1. **Testability.** Pure functions are testable with plain Jest/Vitest — no Alpine, no DOM.
2. **Portability.** If you ever swap Alpine for something else, your domain logic comes with you.

```js
// services/usersService.js
import { api } from './api'

export const fetchUsers   = (filters) => api.get('/users', filters)
export const deleteUser   = (id)      => api.delete(`/users/${id}`)
```

Components import and call these — they don't `fetch()` directly.

## Server-rendered initial state

A real strength of Alpine is pairing it with a server-rendered backend. Pass initial state into components so the page is interactive without a loading flash:

```html
<div x-data="productCard({{ product|json }})">…</div>
```

In CDN/no-build setups, the equivalent is injecting JSON into a global and reading it from `init()`:

```html
<script>window.__INITIAL_USERS__ = <%= @users.to_json %>;</script>
```

## Plugins

Register Alpine plugins (`@alpinejs/persist`, `@alpinejs/intersect`, `@alpinejs/focus`, Pinecone Router) **before** `Alpine.start()` in build setups, or **before** the Alpine CDN script tag in CDN setups. Same logical place either way: load plugins → register your code → start Alpine.

## Two app-level lifecycle events worth knowing

- **`alpine:init`** fires after Alpine is loaded but *before* it walks the DOM. This is where `Alpine.data()` and `Alpine.store()` registrations belong in CDN setups, and where plugins hook in. Code that runs here can register components that the very first DOM walk will need.
- **`alpine:initialized`** fires *after* Alpine has finished initializing the page. Useful for "Alpine is fully ready, all components have run their `init()`" code — e.g. a loading-screen dismissal, telemetry, or a third-party library that needs to query into the now-rendered DOM.

Both are dispatched on `document`. Don't confuse them with the per-component `init()` / `destroy()` hooks, which run for each `Alpine.data()` instance.

**Always pass `{ once: true }` as the third argument.** Both events fire exactly once per page load — keeping the listener registered beyond that point is wasteful and misleading. Use the `once` option so the browser automatically removes the handler after it fires:

```js
// ✓ Correct
document.addEventListener('alpine:init', () => {
  Alpine.data('myComponent', () => ({ … }));
  Alpine.store('auth', { … });
}, { once: true });

document.addEventListener('alpine:initialized', () => {
  if (window.lucide) lucide.createIcons();
}, { once: true });

// ✗ Wrong — listener remains registered for the lifetime of the page for no reason
document.addEventListener('alpine:init', () => { … });
```

## When to push back on the user

- **"Should I put my whole app state in one giant store?"** No. That recreates Redux's worst patterns and ignores what Alpine is good at. Locality is a feature.
- **"Can my page just be a store so I can access it from anywhere?"** No — see "Pages and views are components, not stores" above. If you need cross-component access, hoist *just* the shared bits (e.g. selected IDs, filter state) into a small dedicated store, not the whole page.
- **"I'll start with CDN and migrate to a build step later."** That's fine and explicitly supported — the architecture is identical, only the wiring changes. The migration is mostly mechanical: replace `document.addEventListener('alpine:init', ...)` with direct `Alpine.data()` calls in an entry file, replace `App.services.foo` with imports.
- **"Should I use Alpine for this?"** If the app has heavy client-side routing, deep nested state, or many highly interactive views, the user may have outgrown Alpine and should consider Vue/Svelte. Be honest about this when the question reveals it. Alpine's sweet spot is server-rendered apps with sprinkled interactivity, lifted to "rich" via stores and Pinecone — not full SPAs replacing a backend-rendered shell.

## Reference files

Load these only when relevant to the user's question:

- `references/build-setup.md` — Full registration code for Vite/esbuild/bundled setups: `app.js`, `stores/index.js`, `components/index.js`, example component and store modules with imports.
- `references/cdn-setup.md` — No-build-step setup: deferred script loading order, `alpine:init` registration pattern, `window.App` namespace convention, server-rendered hydration without modules, trade-offs and migration path.
- `references/routing.md` — Pinecone Router patterns: route declarations, handlers vs page state, guards/redirects, route params, 404s, preserving list-page UX across back navigation, and rendering with `x-template` (inline + external HTML files).
- `references/page-component-template.md` — Full template for a "page" component (`usersPage`-style): filters, loading state, watchers, store interactions, dispatch patterns.
- `references/decision-checklist.md` — Quick checklist for "store or x-data?" — useful when the user asks about a specific piece of state and wants a yes/no answer.
