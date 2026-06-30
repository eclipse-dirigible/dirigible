# Build-Step Setup (Vite, esbuild, bundled)

Use this when the project has a bundler — Vite, esbuild, Rollup, Webpack, Laravel Vite, Rails importmaps with bundling, etc. ES modules and `import`/`export` are available.

## Entry point

`src/js/app.js` is the single entry point. It registers stores, components, directives, and plugins **before** calling `Alpine.start()`. Order matters: plugins first (they extend Alpine), then stores and components, then start.

```js
// src/js/app.js
import Alpine from 'alpinejs'
import persist from '@alpinejs/persist'
import intersect from '@alpinejs/intersect'
import PineconeRouter from 'pinecone-router'

import registerStores from './stores'
import registerComponents from './components'
import registerDirectives from './directives'
import registerRoutes from './router'

// Plugins
Alpine.plugin(persist)
Alpine.plugin(intersect)
Alpine.plugin(PineconeRouter)

// Expose for debugging
window.Alpine = Alpine

// Your code
registerStores(Alpine)
registerComponents(Alpine)
registerDirectives(Alpine)
registerRoutes(Alpine)

Alpine.start()
```

## Stores

Each store is a module that exports a plain object. An `index.js` registers them all.

```js
// src/js/stores/cart.js
import { fetchCart, postCartItem } from '../services/cartService'

export default {
  items: [],
  loading: false,

  get total() {
    return this.items.reduce((sum, i) => sum + i.price * i.qty, 0)
  },

  get count() {
    return this.items.reduce((n, i) => n + i.qty, 0)
  },

  async load() {
    this.loading = true
    try {
      this.items = await fetchCart()
    } finally {
      this.loading = false
    }
  },

  async add(productId) {
    const item = await postCartItem(productId)
    this.items.push(item)
  }
}
```

```js
// src/js/stores/notifications.js
export default {
  queue: [],

  push(message, type = 'info') {
    const id = Date.now() + Math.random()
    this.queue.push({ id, message, type })
    setTimeout(() => this.dismiss(id), 5000)
  },

  dismiss(id) {
    this.queue = this.queue.filter(n => n.id !== id)
  },

  success(msg) { this.push(msg, 'success') },
  error(msg)   { this.push(msg, 'error') },
  info(msg)    { this.push(msg, 'info') }
}
```

```js
// src/js/stores/index.js
import auth from './auth'
import cart from './cart'
import notifications from './notifications'

export default function (Alpine) {
  Alpine.store('auth', auth)
  Alpine.store('cart', cart)
  Alpine.store('notifications', notifications)
}
```

### Store `init()` and persistence

Stores can have their own `init()` method that fires right after registration — useful for reading from `localStorage`, `prefers-color-scheme`, or kicking off a fetch:

```js
// src/js/stores/theme.js
import Alpine from 'alpinejs'

export default {
  // Alpine.$persist is the global form of $persist, usable inside stores.
  mode: Alpine.$persist('light').as('theme.mode'),

  init() {
    // Honor the OS preference if no saved value yet
    if (!localStorage.getItem('_x_theme.mode')) {
      this.mode = window.matchMedia('(prefers-color-scheme: dark)').matches
        ? 'dark' : 'light'
    }
  },

  toggle() { this.mode = this.mode === 'dark' ? 'light' : 'dark' }
}
```

For trivial state, **single-value stores** are also supported — pass a primitive instead of an object:

```js
Alpine.store('darkMode', false)
// access: $store.darkMode  / mutate: $store.darkMode = true
```

> **`$persist` + `Alpine.data` gotcha:** When using `$persist` *inside* a component (rather than a store), the factory must be a regular `function () { ... }`, not an arrow function. Alpine binds `this` for `this.$persist(...)` to work, and arrow functions don't accept that binding:
>
> ```js
> // ❌ Won't work — `this.$persist` is undefined
> Alpine.data('settings', () => ({ open: this.$persist(false) }))
>
> // ✅ Works
> Alpine.data('settings', function () {
>   return { open: this.$persist(false) }
> })
> ```

## Components

Each component is a factory function. The factory takes initial state and returns the reactive object Alpine will use.

```js
// src/js/components/shared/modal.js
export default function modal(opts = {}) {
  return {
    open: opts.open ?? false,

    init() {
      // Close on Escape
      this.$watch('open', (val) => {
        if (val) document.body.style.overflow = 'hidden'
        else document.body.style.overflow = ''
      })
    },

    show()   { this.open = true },
    hide()   { this.open = false },
    toggle() { this.open = !this.open }
  }
}
```

```js
// src/js/components/pages/usersPage.js
import { fetchUsers, deleteUser } from '../../services/usersService'

export default function usersPage(initial = {}) {
  return {
    users: initial.users ?? [],
    filters: { search: '', role: 'all' },
    selected: null,
    loading: false,

    async init() {
      if (!this.$store.auth.can('users.view')) {
        this.$store.notifications.error('Forbidden')
        return
      }
      // If server didn't pre-render, fetch on mount
      if (this.users.length === 0) await this.load()
      // $watch is deep by default — nested filter changes trigger this
      this.$watch('filters', () => this.load())

      // Track external listeners for cleanup
      this._onRefresh = () => this.load()
      window.addEventListener('users:refresh', this._onRefresh)
    },

    destroy() {
      // Runs when Pinecone unmounts the route's x-data on navigation,
      // or when the component is removed by x-if. Without this the
      // window listener leaks across page visits.
      window.removeEventListener('users:refresh', this._onRefresh)
    },

    async load() {
      this.loading = true
      try {
        this.users = await fetchUsers(this.filters)
      } catch (e) {
        this.$store.notifications.error(e.message)
      } finally {
        this.loading = false
      }
    },

    async remove(id) {
      if (!confirm('Delete this user?')) return
      await deleteUser(id)
      this.users = this.users.filter(u => u.id !== id)
      this.$store.notifications.success('User deleted')
    }
  }
}
```

```js
// src/js/components/index.js
import modal from './shared/modal'
import dataTable from './shared/dataTable'

import usersPage from './pages/usersPage'
import dashboardPage from './pages/dashboardPage'

import navbar from './layout/navbar'

export default function (Alpine) {
  // shared
  Alpine.data('modal', modal)
  Alpine.data('dataTable', dataTable)

  // pages
  Alpine.data('usersPage', usersPage)
  Alpine.data('dashboardPage', dashboardPage)

  // layout
  Alpine.data('navbar', navbar)
}
```

## Services (framework-agnostic)

```js
// src/js/services/api.js
async function request(method, url, body) {
  const r = await fetch(url, {
    method,
    headers: {
      'Content-Type': 'application/json',
      'Accept': 'application/json',
      'X-CSRF-Token': document.querySelector('meta[name=csrf-token]')?.content
    },
    body: body ? JSON.stringify(body) : undefined,
    credentials: 'same-origin'
  })
  if (!r.ok) throw new Error(`${r.status} ${r.statusText}`)
  return r.status === 204 ? null : r.json()
}

export const api = {
  get:    (url, params) => request('GET', params ? `${url}?${new URLSearchParams(params)}` : url),
  post:   (url, body)   => request('POST', url, body),
  patch:  (url, body)   => request('PATCH', url, body),
  delete: (url)         => request('DELETE', url)
}
```

```js
// src/js/services/usersService.js
import { api } from './api'

export const fetchUsers = (filters) => api.get('/api/users', filters)
export const deleteUser = (id)      => api.delete(`/api/users/${id}`)
export const updateUser = (id, data) => api.patch(`/api/users/${id}`, data)
```

## HTML usage

```html
<div x-data="usersPage({ users: {{ @users|json }} })">
  <input x-model.debounce.300ms="filters.search" placeholder="Search…" />

  <select x-model="filters.role">
    <option value="all">All roles</option>
    <option value="admin">Admin</option>
    <option value="user">User</option>
  </select>

  <template x-if="loading"><p>Loading…</p></template>

  <template x-for="user in users" :key="user.id">
    <div>
      <span x-text="user.name"></span>
      <button @click="remove(user.id)">Delete</button>
    </div>
  </template>
</div>
```
