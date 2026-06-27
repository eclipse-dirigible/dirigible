# Store vs `x-data` Decision Checklist

Use this when the user asks about a specific piece of state and wants a yes/no answer on where to put it.

## The 4-question test

For any piece of state, ask in order:

### 1. Do unrelated components need this?
"Unrelated" means components that don't share a parent in the DOM. A sidebar reading the cart count and a checkout page reading the cart items are unrelated — they're not nested.

- **Yes →** likely a store.
- **No →** stay local with `x-data`.

### 2. Does it need to outlive any single page/component?
If the user navigates away and back, should the state still be there?

- **Yes →** store (or persisted store via `@alpinejs/persist`).
- **No →** `x-data`. Let it die with the component — that's a feature, not a bug.

### 3. Does it represent a domain concept or a UI concern?
Auth, cart, current locale, feature flags = domain. Modal-open, dropdown-active, accordion-expanded, form-step = UI.

- **Domain →** lean store.
- **UI →** lean `x-data`.

### 4. Will multiple components write to it (not just read)?
Reading from many places is fine via either option. Writing from many places is the real signal that you need a single source of truth.

- **Many writers →** store.
- **One writer (or read-only consumers) →** `x-data` is fine.

## Quick lookup table

| State | Where | Why |
|---|---|---|
| Logged-in user, permissions | **Store** | Read by navbar, page guards, multiple page components |
| Cart items, count, total | **Store** | Read by mini-cart, cart page, checkout — all unrelated |
| Toast/notification queue | **Store** | Pushed from anywhere, rendered once in layout |
| Theme (dark/light) | **Store** + persist | Read everywhere, survives reload |
| Locale / i18n | **Store** | Read everywhere |
| Feature flags | **Store** | Read everywhere |
| Modal open/closed | **`x-data`** | Only the modal and its trigger care |
| Dropdown expanded | **`x-data`** | Local UI concern |
| Form field values, validation errors | **`x-data`** | Belongs to the form |
| Page filters, pagination, sort | **`x-data`** | Belongs to the page; reset on leave is desired |
| List of users on the users page | **`x-data`** | Owned by the page |
| Selected row, opened editor | **`x-data`** | Local interaction state |
| "Did the user dismiss this banner?" | **Store** + persist | Cross-page, survives reload |
| WebSocket connection | **Store** | Singleton resource |
| Active tab in a tabset | **`x-data`** | Local UI |
| Wizard step | **`x-data`** | Belongs to the wizard component |
| "Currently viewed item ID" (e.g. for breadcrumbs) | **Store** (small, dedicated) | Read by breadcrumbs *and* the page |

## Edge case: list page UX preservation

"I want filters and scroll to survive when the user navigates back to the users list."

- **Don't** put the whole users page in a store. That breaks the lifecycle and keeps stale data forever.
- **Do** create a tiny `usersListUI` store with just `{ filters, page, scrollY }`. The page component reads/writes it via getters. List data itself stays local OR goes in a separate cache store with TTL.

This is the "hoist just what needs to survive" rule. See `routing.md` for the full pattern.

## Edge case: shared form across pages

"My checkout has 3 steps on 3 different routes — where does the form data live?"

- **Store.** Steps are unrelated components; form data must survive route changes; multiple components write to it. Three out of four checks.
- Add `@alpinejs/persist` if you want it to survive a reload mid-checkout.

## Edge case: "I want to access this from the console for debugging"

That's not a reason to put it in a store. `window.Alpine` is exposed; you can inspect components via `Alpine.$data($0)` (where `$0` is the selected DOM element in DevTools).

## Edge case: "My 'store' is just a single boolean"

Alpine supports **single-value stores** — pass a primitive to `Alpine.store()` instead of an object:

```js
Alpine.store('darkMode', false)
// usage: $store.darkMode    mutate: $store.darkMode = true
```

This is the right shape for binary global flags (dark mode, sidebar collapsed, debug overlay) where the object-with-methods ceremony is overkill. Combine with `Alpine.$persist(...)` if it should survive reloads.

## The smell test

If you find yourself writing this kind of code, you've put state in the wrong place:

```js
// Smell: manually resetting a "page store" because singletons don't reset
Alpine.store('usersPage').reset()
this.$router.navigate('/users')
```

The page should be a component. Navigating to it gives you a fresh instance for free.

```js
// Smell: passing data from store to component just because it lives in the store
x-data="userCard({ user: $store.users.list[0] })"
```

The component owns its own data here. The store is being used as a parking lot.

```js
// Smell: components reaching up into a store to mutate UI state
$store.app.modalOpen = true
```

Modals are local. The trigger and the modal share a parent or use `$dispatch`.
