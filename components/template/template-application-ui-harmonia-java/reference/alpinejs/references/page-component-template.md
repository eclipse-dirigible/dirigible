# Page Component Template

A reference template for a "page" component — the kind that backs a route in Pinecone Router. Adapt to the specific resource (users, products, orders, etc.).

This template covers the patterns that come up in nearly every list page:
- Server-rendered initial state OR client fetch on mount
- Filters with debounced refetch
- Loading, error, and empty states
- Modal interaction
- Mutations with optimistic update + rollback on failure
- Auth guard
- Notification dispatch
- Cross-component dispatch via `$dispatch`

## The component

```js
// src/js/components/pages/usersPage.js
import { fetchUsers, deleteUser, updateUser } from '../../services/usersService'

export default function usersPage(initial = {}) {
  return {
    // ── State ───────────────────────────────────────
    users: initial.users ?? [],
    filters: { search: '', role: 'all', sort: 'name' },
    selected: null,           // user object opened in modal
    loading: false,
    error: null,
    page: 1,
    perPage: 25,

    // ── Lifecycle ───────────────────────────────────
    async init() {
      // Auth guard (redundant with router handler, but defensive)
      if (!this.$store.auth.can('users.view')) {
        this.$store.notifications.error('You do not have access to this page')
        return
      }

      // If server pre-rendered, skip the initial fetch
      if (this.users.length === 0) {
        await this.load()
      }

      // Refetch when filters change. $watch is deep by default in Alpine,
      // so nested changes (filters.search, filters.role) trigger this.
      this.$watch('filters', () => this.load())

      // Listen for cross-component events. Stash the handler so we can
      // remove it in destroy() to avoid leaks across navigations.
      this._onUsersRefresh = () => this.load()
      window.addEventListener('users:refresh', this._onUsersRefresh)
    },

    destroy() {
      // Critical for page components: when the user navigates away (Pinecone
      // unmounts the route's x-data), this fires. Anything started in init()
      // that lives outside the component scope must be torn down here.
      window.removeEventListener('users:refresh', this._onUsersRefresh)
    },

    // ── Derived ─────────────────────────────────────
    get isEmpty() {
      return !this.loading && this.users.length === 0
    },

    get hasError() {
      return this.error !== null
    },

    // ── Data loading ────────────────────────────────
    async load() {
      this.loading = true
      this.error = null
      try {
        this.users = await fetchUsers({
          ...this.filters,
          page: this.page,
          per_page: this.perPage
        })
      } catch (e) {
        this.error = e.message
        this.$store.notifications.error('Failed to load users')
      } finally {
        this.loading = false
      }
    },

    // ── Modal ───────────────────────────────────────
    openModal(user) {
      this.selected = { ...user } // clone so edits don't leak
    },

    closeModal() {
      this.selected = null
    },

    // ── Mutations (optimistic) ──────────────────────
    async save() {
      const original = this.users.find(u => u.id === this.selected.id)
      const idx = this.users.indexOf(original)

      // Optimistic update
      this.users[idx] = { ...this.selected }
      const draft = this.selected
      this.closeModal()

      try {
        await updateUser(draft.id, draft)
        this.$store.notifications.success('User updated')
        this.$dispatch('users:changed', { id: draft.id })
      } catch (e) {
        // Roll back
        this.users[idx] = original
        this.$store.notifications.error('Save failed — changes reverted')
      }
    },

    async remove(id) {
      if (!confirm('Delete this user? This cannot be undone.')) return

      // Optimistic remove
      const idx = this.users.findIndex(u => u.id === id)
      const removed = this.users[idx]
      this.users.splice(idx, 1)

      try {
        await deleteUser(id)
        this.$store.notifications.success('User deleted')
        this.$dispatch('users:changed', { id, action: 'delete' })
      } catch (e) {
        this.users.splice(idx, 0, removed) // rollback
        this.$store.notifications.error('Delete failed')
      }
    },

    // ── Pagination ──────────────────────────────────
    nextPage() {
      this.page++
      this.load()
    },
    prevPage() {
      if (this.page > 1) {
        this.page--
        this.load()
      }
    }
  }
}
```

## The HTML

```html
<div x-data="usersPage({ users: {{ @users|json }} })">
  <!-- Filters -->
  <div class="filters">
    <input
      type="search"
      x-model.debounce.300ms="filters.search"
      placeholder="Search users…"
    />
    <select x-model="filters.role">
      <option value="all">All roles</option>
      <option value="admin">Admin</option>
      <option value="user">User</option>
    </select>
  </div>

  <!-- States -->
  <template x-if="loading">
    <p class="loading">Loading…</p>
  </template>

  <template x-if="hasError">
    <div class="error">
      <p x-text="error"></p>
      <button @click="load()">Retry</button>
    </div>
  </template>

  <template x-if="isEmpty">
    <p class="empty">No users match your filters.</p>
  </template>

  <!-- List -->
  <template x-if="!loading && users.length > 0">
    <table>
      <thead><tr><th>Name</th><th>Email</th><th>Role</th><th></th></tr></thead>
      <tbody>
        <template x-for="user in users" :key="user.id">
          <tr>
            <td x-text="user.name"></td>
            <td x-text="user.email"></td>
            <td x-text="user.role"></td>
            <td>
              <button @click="openModal(user)">Edit</button>
              <button @click="remove(user.id)">Delete</button>
            </td>
          </tr>
        </template>
      </tbody>
    </table>
  </template>

  <!-- Pagination -->
  <div class="pagination">
    <button @click="prevPage()" :disabled="page === 1">Prev</button>
    <span x-text="`Page ${page}`"></span>
    <button @click="nextPage()">Next</button>
  </div>

  <!-- Edit modal -->
  <div
    x-show="selected !== null"
    x-transition
    @keydown.escape.window="closeModal()"
    class="modal-backdrop"
    @click.self="closeModal()"
  >
    <div class="modal" x-show="selected !== null">
      <h2>Edit user</h2>
      <template x-if="selected">
        <div>
          <label>Name <input x-model="selected.name" /></label>
          <label>Email <input x-model="selected.email" type="email" /></label>
          <label>Role
            <select x-model="selected.role">
              <option value="admin">Admin</option>
              <option value="user">User</option>
            </select>
          </label>
          <button @click="save()">Save</button>
          <button @click="closeModal()">Cancel</button>
        </div>
      </template>
    </div>
  </div>
</div>
```

## Patterns this template demonstrates

- **Initial state from server, fallback to client fetch.** `initial.users ?? []` plus the `if (users.length === 0) load()` check covers both modes from one component.
- **Watcher-driven refetch.** `$watch` on `filters` reacts to nested changes automatically — Alpine's `$watch` is deep by default (unlike Vue's, which requires `{ deep: true }`). Mutating `filters.search` or `filters.role` triggers the callback.
- **`init()` for setup, `destroy()` for cleanup.** Anything started in `init()` that lives outside component scope (window listeners, intervals, websockets, third-party libraries) gets torn down in `destroy()`. Without this, navigating away from a page leaks its handlers — a real bug in Pinecone-routed apps.
- **Optimistic mutations with rollback.** Saves the original, mutates, calls API, reverts + notifies on failure. The user gets snappy UI, the data stays correct.
- **Modal owned by the page.** No global modal store — `selected` is local because nothing else needs to know about it. The clone (`{ ...user }`) prevents in-progress edits from leaking into the table.
- **`$dispatch` for cross-component signals.** Other components (e.g. a sidebar count, a recently-edited list) can listen for `users:changed` without coupling. Note that custom events bubble up the DOM tree — to catch a dispatch from a sibling, listen on `window` with the `.window` modifier (`@users:changed.window="..."`).
- **Stores read for cross-cutting concerns only.** `$store.auth` for permissions, `$store.notifications` for toasts. The page itself stays local.
