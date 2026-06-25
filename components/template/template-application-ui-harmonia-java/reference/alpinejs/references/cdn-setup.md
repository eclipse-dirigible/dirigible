# CDN-Only Setup (No Build Step)

Use this when the user is on a Laravel Blade / Rails ERB / Django / WordPress / plain HTML setup and can't or won't introduce a bundler. The architecture is identical to the build-step version — only the wiring changes.

## The two key adaptations

1. **Script load order, not ES imports.** Without modules, you have to ensure files load in dependency order. Use `defer` on every script and rely on document order.
2. **Register inside `alpine:init`, not in an entry file.** Alpine fires the `alpine:init` event right before booting, with `window.Alpine` already available. Registering there is forgiving of order issues and makes load order matter less.

## Script tags in the layout

Plugins first, then your code (services → stores → components → router), then Alpine **last**. Alpine is loaded with `defer` so it boots after all your `alpine:init` listeners are registered.

```html
<!-- 1. Plugins first (they register themselves via alpine:init) -->
<script src="https://cdn.jsdelivr.net/npm/pinecone-router@latest/dist/index.min.js" defer></script>
<script src="https://cdn.jsdelivr.net/npm/@alpinejs/persist@3.x.x/dist/cdn.min.js" defer></script>
<script src="https://cdn.jsdelivr.net/npm/@alpinejs/intersect@3.x.x/dist/cdn.min.js" defer></script>

<!-- 2. Your code, in dependency order -->
<script src="/js/app.js" defer></script>                          <!-- creates window.App -->
<script src="/js/services/api.js" defer></script>
<script src="/js/services/usersService.js" defer></script>
<script src="/js/stores/auth.js" defer></script>
<script src="/js/stores/notifications.js" defer></script>
<script src="/js/components/shared/modal.js" defer></script>
<script src="/js/components/pages/usersPage.js" defer></script>
<script src="/js/router/handlers.js" defer></script>

<!-- 3. Alpine LAST -->
<script src="https://cdn.jsdelivr.net/npm/alpinejs@3.x.x/dist/cdn.min.js" defer></script>
```

All `defer` scripts execute in document order, after HTML parsing, before `DOMContentLoaded`. This is the predictable window you want.

> ⚠️ The Alpine docs explicitly recommend hardcoding the version (e.g. `alpinejs@3.14.9`) in production rather than `@3.x.x`. Floating versions get you surprise breakage on a CDN cache miss. Use `@3.x.x` while prototyping; pin before you ship.

## The `window.App` namespace

Without imports, give yourself one global namespace and hang everything off it. This file loads first among your code:

```js
// /js/app.js
window.App = {
  services: {},
  utils: {},
  routes: {}
}
```

Now every other file attaches to it. No collisions, one place to look, no order coupling beyond "app.js loads first."

## Services

```js
// /js/services/api.js
App.services.api = {
  async request(method, url, body) {
    const r = await fetch(url, {
      method,
      headers: {
        'Content-Type': 'application/json',
        'Accept': 'application/json'
      },
      body: body ? JSON.stringify(body) : undefined,
      credentials: 'same-origin'
    })
    if (!r.ok) throw new Error(`${r.status} ${r.statusText}`)
    return r.status === 204 ? null : r.json()
  },
  get:    function (url, params) { return this.request('GET', params ? `${url}?${new URLSearchParams(params)}` : url) },
  post:   function (url, body)   { return this.request('POST', url, body) },
  patch:  function (url, body)   { return this.request('PATCH', url, body) },
  delete: function (url)         { return this.request('DELETE', url) }
}
```

```js
// /js/services/usersService.js
App.services.users = {
  fetch:  (filters) => App.services.api.get('/api/users', filters),
  remove: (id)      => App.services.api.delete(`/api/users/${id}`)
}
```

## Stores via `alpine:init`

```js
// /js/stores/auth.js
document.addEventListener('alpine:init', () => {
  Alpine.store('auth', {
    user: window.__INITIAL_AUTH__ || null,
    can(perm) { return this.user?.permissions?.includes(perm) ?? false },
    isLoggedIn() { return this.user !== null }
  })
})
```

```js
// /js/stores/notifications.js
document.addEventListener('alpine:init', () => {
  Alpine.store('notifications', {
    queue: [],
    push(message, type = 'info') {
      const id = Date.now() + Math.random()
      this.queue.push({ id, message, type })
      setTimeout(() => this.dismiss(id), 5000)
    },
    dismiss(id) { this.queue = this.queue.filter(n => n.id !== id) },
    success(m) { this.push(m, 'success') },
    error(m)   { this.push(m, 'error') },
    info(m)    { this.push(m, 'info') }
  })
})
```

## Components via `alpine:init`

```js
// /js/components/pages/usersPage.js
document.addEventListener('alpine:init', () => {
  Alpine.data('usersPage', (initial = {}) => ({
    users: initial.users ?? (window.__INITIAL_USERS__ ?? []),
    filters: { search: '', role: 'all' },
    loading: false,

    async init() {
      if (!this.$store.auth.can('users.view')) {
        this.$store.notifications.error('Forbidden')
        return
      }
      if (this.users.length === 0) await this.load()
      // $watch is deep by default — nested filter changes trigger this
      this.$watch('filters', () => this.load())

      this._onRefresh = () => this.load()
      window.addEventListener('users:refresh', this._onRefresh)
    },

    destroy() {
      // Clean up listeners attached in init() so navigations don't leak
      window.removeEventListener('users:refresh', this._onRefresh)
    },

    async load() {
      this.loading = true
      try {
        this.users = await App.services.users.fetch(this.filters)
      } catch (e) {
        this.$store.notifications.error(e.message)
      } finally {
        this.loading = false
      }
    }
  }))
})
```

> **`$persist` + `Alpine.data` gotcha:** if a component uses `this.$persist(...)`, the factory must be a regular `function () { ... }`, not an arrow function. Alpine binds `this` for the magic to resolve, and arrow functions ignore that binding. Use the global form `Alpine.$persist(...)` inside `Alpine.store()` definitions instead.

## Routes

```js
// /js/router/handlers.js
App.routes = {
  users(ctx) {
    if (!Alpine.store('auth').can('users.view')) ctx.redirect('/')
  },
  userDetail(ctx) {
    Alpine.store('currentUserId', ctx.params.id)
  }
}
```

```html
<template x-route="/users" x-handler="App.routes.users">
  <div x-data="usersPage()">…</div>
</template>
```

## Server-rendered initial state (the no-build "hydration")

Inject JSON into a global from the backend template so the page is interactive on first paint without a fetch round-trip:

```html
<!-- In your layout, before the JS bundles -->
<script>
  window.__INITIAL_AUTH__  = <%= current_user.to_json.html_safe %>;
  window.__INITIAL_USERS__ = <%= @users.to_json.html_safe %>;
</script>
```

Components and stores read from these globals in `init()`. This matches the role of `<div x-data="component({{ data|json }})">` in the build setup.

## Cache-busting

Without a bundler producing hashed filenames, version your URLs manually so deploys reach users:

```html
<script src="/js/app.js?v=2026-05-04" defer></script>
```

Or use the deployment commit SHA. Pick one convention and stick to it.

## Trade-offs to be honest about

| Concern | CDN/no-build | Build step |
|---|---|---|
| Setup time | 0 | 5 min (`npm create vite`) |
| Tree-shaking / minification | Limited to what CDNs ship | Full |
| TypeScript | No | Yes |
| Hot reload | No | Yes |
| Adding a new file | Add `<script>` tag in layout | Just save it |
| Order coupling | Real but managed by `alpine:init` | None |
| Best for | Small/medium server-rendered apps | Anything bigger or growing |

## When to migrate to a build step

Push toward Vite/esbuild when:
- The app has more than ~15–20 component/store files (the layout's `<script>` block becomes painful).
- You want TypeScript or stricter linting.
- You need plugin features that require configuration (custom `@alpinejs/persist` storage drivers).
- Asset size starts mattering and you'd benefit from tree-shaking.

The migration is mostly mechanical: replace `document.addEventListener('alpine:init', () => { Alpine.data(...) })` with direct `Alpine.data(...)` calls in an entry file, replace `App.services.foo` references with ES imports. Component and store *bodies* don't change.
