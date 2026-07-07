# Info Page

Provides a structured layout to display instructional content, messages, or status information, such as empty states or error notifications. Info Pages combine text, imagery, and optional actions to clearly communicate context to the user.

Part of the Harmonia Alpine.js component library. Every directive uses the `x-h-` prefix.

## Usage

Use Info Pages to guide users, explain empty states, or report errors in a visually distinct and informative way. Include clear messaging and actionable steps when appropriate, and avoid overloading the page with unnecessary details.

## Directives

`x-h-info-page` is the root. The directives compose one component and must be nested as shown in the Examples below (the library throws at runtime when a required ancestor is missing):

- `x-h-info-page`
- `x-h-info-page-header`
- `x-h-info-page-media`
- `x-h-info-page-title`
- `x-h-info-page-description`
- `x-h-info-page-content`

## API

### Modifiers

#### x-h-info-page-media

| Modifier | Description                                                                 |
| -------- | --------------------------------------------------------------------------- |
| icon     | Applies styles for inline svg icons. Do not activate when using an img tag. |

## Examples

### With inline SVG icon

```html
<div x-h-info-page>
  <div x-h-info-page-header>
    <div x-h-info-page-media.icon>
      <i x-h-lucide role="img" data-lucide="folder"></i>
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
  <a href="#" x-h-button data-size="sm" data-variant="link">Learn More<i x-h-lucide role="img" data-lucide="arrow-up-right"></i></a>
</div>
```

### With image

```html
<div x-h-info-page>
  <div x-h-info-page-header>
    <div x-h-info-page-media>
      <img src="/harmonia/logo/harmonia.svg" alt="@harmonia" width="256px" />
    </div>
    <div x-h-info-page-title>Harmonia</div>
    <div x-h-info-page-description>UI component library for Alpine.js</div>
  </div>
  <div x-h-info-page-content>
    <button x-h-button data-variant="primary">GitHub Page</button>
  </div>
</div>
```

### With border

```html
<div x-h-info-page class="border">
  <div x-h-info-page-header>
    <div x-h-info-page-media>
      <i x-h-lucide role="img" data-lucide="upload"></i>
    </div>
    <div x-h-info-page-title>Upload file(s)</div>
    <div x-h-info-page-description>Drag & drop your file(s) or use the button below</div>
  </div>
  <div x-h-info-page-content>
    <button x-h-button data-variant="primary">Upload</button>
  </div>
</div>
```

Full docs: https://www.codbex.com/harmonia/components/info-page.html

## Notes

- Directive values are Alpine expressions, so quote string literals: `x-h-...="'Label'"`.
- Components render only after Alpine has registered Harmonia. See SKILL.md for setup.
