# Routing with Pinecone Router

Pinecone Router is the standard router for Alpine. It works at the DOM level: routes are declared as `<template x-route>` elements with `x-handler` attributes, and Pinecone matches the URL and renders the matching template.

## The clean separation of concerns

| Layer | What it owns |
|---|---|
| `<template x-route>` | The route definition — path, handler reference |
| Handler function | Guards, redirects, route-level prep (auth, fetching shared data into stores) |
| `x-data` on the rendered template | Page state (filters, list, selected row, modal open/closed) |
| `Alpine.store()` | Cross-cutting state read by handlers and pages alike |

Pinecone re-evaluates `x-data` on every route entry, so `init()` runs every time the user navigates in. That's the point: fresh page state per visit, no manual reset code.

## Basic setup

**Build step:**

```js
// router/index.js
import PineconeRouter from 'pinecone-router'
import { routes } from './handlers'

export default function (Alpine) {
  Alpine.plugin(PineconeRouter)
  // Expose handlers globally so x-handler can reference them
  window.routes = routes
}
```

**CDN:** include the Pinecone script tag (see `cdn-setup.md`) — it self-registers.

## Route declarations

```html
<template x-route="/" x-handler="routes.home">
  <div x-data="homePage()">…</div>
</template>

<template x-route="/users" x-handler="routes.users">
  <div x-data="usersPage()">…</div>
</template>

<template x-route="/users/:id" x-handler="routes.userDetail">
  <div x-data="userDetailPage()">…</div>
</template>

<template x-route="/users/:id/edit" x-handler="routes.userEdit">
  <div x-data="userEditPage()">…</div>
</template>

<template x-route="notfound" x-handler="routes.notFound">
  <div x-data="notFoundPage()">
    <h1>Not found</h1>
  </div>
</template>
```

## Handlers

Handlers receive a `context` object with `params`, `query`, `redirect()`, etc. Keep handlers small — guards and prep, nothing else.

```js
// router/handlers.js (build-step version)
export const routes = {
  home() {},

  users(ctx) {
    if (!Alpine.store('auth').can('users.view')) {
      return ctx.redirect('/')
    }
  },

  userDetail(ctx) {
    // Optionally pre-warm a cache or expose the active ID
    Alpine.store('currentUserId', ctx.params.id)
  },

  userEdit(ctx) {
    if (!Alpine.store('auth').can('users.edit')) {
      return ctx.redirect(`/users/${ctx.params.id}`)
    }
  },

  notFound() {}
}
```

## Reading route params inside a page component

Inside a page component, the easiest way to read params is via the `$params` magic helper (or `$router.context.params`). Use it in `init()`:

```js
// components/pages/userDetailPage.js
import { fetchUser } from '../../services/usersService'

export default function userDetailPage() {
  return {
    user: null,
    loading: false,
    error: null,

    async init() {
      const id = this.$params.id   // or: this.$router.context.params.id
      this.loading = true
      try {
        this.user = await fetchUser(id)
      } catch (e) {
        this.error = e.message
      } finally {
        this.loading = false
      }
    },

    destroy() {
      // If init() started anything that needs cleanup (intervals, listeners,
      // websocket subscriptions), tear it down here.
    }
  }
}
```

> Pinecone exposes route params three ways: the `$params` magic helper inside Alpine components, `context.params.foo` inside route handlers, and `PineconeRouter.context.params.foo` from anywhere in JS.

## Programmatic navigation

```js
// In a component method
this.$router.navigate('/users/42')

// Or from anywhere
window.PineconeRouter.context.navigate('/users/42')
```

## Preserving list-page UX across back navigation

Default Pinecone behavior: navigating back to a list page re-runs `init()`, which means filters and scroll position reset. That's usually wrong UX.

**Fix:** hoist *just* the UI state that needs to survive into a small, dedicated store. Don't promote the whole page.

```js
// stores/usersListUI.js
export default {
  filters: { search: '', role: 'all' },
  page: 1,
  scrollY: 0
}
```

```js
// components/pages/usersPage.js
export default function usersPage() {
  return {
    users: [],
    loading: false,

    // Bind to store, not local
    get filters() { return this.$store.usersListUI.filters },
    get page()    { return this.$store.usersListUI.page },

    async init() {
      await this.load()
      this.$watch('filters', () => this.load())
      // Restore scroll on next tick after content renders
      this.$nextTick(() => window.scrollTo(0, this.$store.usersListUI.scrollY))
      // Save scroll on leave
      window.addEventListener('beforeunload', () => {
        this.$store.usersListUI.scrollY = window.scrollY
      })
    },

    async load() { /* ... */ }
  }
}
```

The list state survives, the page component doesn't. Cache the actual user data separately if refetch latency matters (see "Cross-page caches" below).

## Cross-page caches

For "user navigates back, don't show a loading spinner for 800ms while we refetch what we just had," use a dedicated cache store with TTL:

```js
// stores/usersCache.js
export default {
  data: null,
  fetchedAt: 0,
  ttl: 30_000, // 30s

  isFresh() {
    return this.data && (Date.now() - this.fetchedAt) < this.ttl
  },

  set(users) {
    this.data = users
    this.fetchedAt = Date.now()
  },

  invalidate() {
    this.data = null
    this.fetchedAt = 0
  }
}
```

```js
// In usersPage.init()
if (this.$store.usersCache.isFresh()) {
  this.users = this.$store.usersCache.data
} else {
  this.users = await fetchUsers(this.filters)
  this.$store.usersCache.set(this.users)
}
```

After mutations (delete, edit), call `this.$store.usersCache.invalidate()` so the next visit refetches.

## Rendering templates with `x-template`

Pinecone Router has a built-in `x-template` directive (since v7) for displaying content when a route matches. Two modes — pick whichever fits the page.

### Inline templates

Use an empty `x-template` attribute when the page markup is small enough to keep inline. Children render after the `<template>` tag, the same way `x-if` works:

```html
<template x-route="/" x-template>
  <div x-data="homePage()">
    <h1>Welcome</h1>
    <!-- … -->
  </div>
</template>
```

Add the `.target.app` modifier to render into a specific container instead:

```html
<template x-route="/" x-template.target.app>
  <div x-data="homePage()">…</div>
</template>
<div id="app"></div>
```

You can also set a default target globally via settings (`$router.settings({ targetID: 'app' })`) so every route renders into `#app` without repeating `.target.app`.

### External templates

For larger apps where inlining every page in `index.html` gets heavy, point `x-template` at an HTML file path. Pinecone fetches it on demand and caches it in memory until page reload:

```html
<template x-route="/users" x-handler="routes.users" x-template="/views/users.html"></template>
<template x-route="/dashboard" x-template="/views/dashboard.html"></template>
<template x-route="notfound" x-template="/views/404.html"></template>

<div id="app"><!-- views render here when targetID is set --></div>
```

`/views/users.html` contains just the page markup — no `<html>`/`<body>` wrapper, just `<div x-data="usersPage()">…</div>`. The file can include its own `<script>` tags that run when the route is matched.

Use external templates when:
- Inline templates are making `index.html` huge.
- You want a clear file-per-page boundary in version control.
- The HTML is server-rendered by your backend and you want it generated per-request.

Don't bother for small apps with 3–5 pages — inline templates are simpler.

### Useful modifiers

- **`.preload`** — fetches templates at `low` priority after first page load, without waiting for the route to be visited. Trade an extra request now for instant subsequent navigation. `x-template.preload="/views/users.html"`.
- **`.target.app`** — render into the element with `id="app"` instead of after the template tag.
- **`.interpolate`** — substitute route params into the URL. `x-template.interpolate="/api/dynamic/:name.html"` fetches `/api/dynamic/foo.html` when the URL is `/dynamic/foo`. Useful when the backend generates HTML per-resource. Cannot combine with `.preload`.

Modifiers stack: `x-template.preload.target.app="/views/users.html"`.

### Pairing with handlers

`x-template` and `x-handler` work on the same route. Handlers run first, so guards/redirects fire before any template fetch:

```html
<template
  x-route="/users"
  x-handler="routes.users"
  x-template.target.app="/views/users.html"
></template>
```

If a handler calls `$router.navigate(...)` or `controller.abort()`, the template never fetches or renders. That's the right ordering.

### Loading state

Pinecone fires events on `document` during template fetching:
- `pinecone:start` — fetching starts
- `pinecone:end` — fetching ends
- `pinecone:fetch-error` — fetch failed

Wire up a progress bar with a couple of lines:

```js
document.addEventListener('pinecone:start', () => NProgress.start())
document.addEventListener('pinecone:end',   () => NProgress.done())
```

Or read `$router.loading` reactively in templates. Set sensible `Cache-Control` headers on the view files since Pinecone uses standard `fetch` — the browser HTTP cache is your second layer beyond Pinecone's in-memory cache.

### `x-run` for embedded scripts

A `<script>` inside an external template runs every time the route is matched. To run only once across visits, use `x-run.once`:

```html
<script x-run.once>
  initSomeLibrary()
</script>
```

To run conditionally, use `x-run:on="condition"`:

```html
<script x-run:on="$router.context.route == '/profile'">
  // only runs when actually on /profile, even if this template
  // is included in multiple routes
</script>
```

Combine: `<script x-run.once:on="condition">`.

> ⚠️ Heads up if you've used Pinecone before v7: the old `pinecone-router-middleware-views` package and its `x-view` directive are deprecated — that repo is archived. The functionality moved into core as `x-template`. If you find tutorials or older code referencing `x-view`, port to `x-template`.

## When Alpine + Pinecone may be the wrong choice

Pinecone is great for "server-rendered app with a few SPA-style pages bolted on" or "small-to-medium SPA where Alpine's simplicity is a feature." It is **not** trying to be Vue Router or React Router. If you find yourself wanting nested routes, route transitions, lazy-loaded chunks per route, route-level data loaders with `<Suspense>` semantics — you've outgrown the tool. Be honest with the user.
