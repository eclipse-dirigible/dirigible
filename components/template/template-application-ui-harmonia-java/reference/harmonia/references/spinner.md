# Spinner

A visual indicator that signals an ongoing operation or process. Unlike a progress bar, the spinner does not convey the completion status, only that work is in progress.

Part of the Harmonia Alpine.js component library. Every directive uses the `x-h-` prefix.

## Usage

Use spinners to indicate loading, processing, or other indefinite tasks where the duration is unknown. Avoid using spinners for very short operations, as brief displays can be distracting or unnecessary. Pair with text or context when needed to clarify what is loading.

## Directive

- `x-h-spinner`

## Examples

```html
<span x-h-spinner></span>
<span x-h-spinner class="size-8"></span>
<span x-h-spinner class="size-12"></span>
```

Full docs: https://www.codbex.com/harmonia/components/spinner.html

## Notes

- Directive values are Alpine expressions, so quote string literals: `x-h-...="'Label'"`.
- Components render only after Alpine has registered Harmonia. See SKILL.md for setup.
