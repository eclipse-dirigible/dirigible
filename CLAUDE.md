# CLAUDE.md

Provides behavioral guidelines to reduce common LLM coding mistakes and guidance to Claude Code (claude.ai/code) when working with code in this repository.

**Tradeoff:** These guidelines bias toward caution over speed. For trivial tasks, use judgment.

The guidance is split by topic into `.claude/docs/` (each file carries its own headings). Edit the topic files, not this index.

## Always loaded

Guidance that applies to almost every change. Keep this set small: everything imported here is paid for on every turn of every session, and the harness warns once the imported total passes 150k characters.

@.claude/docs/behavioral-guidelines.md
@.claude/docs/take-this-workflow.md
@.claude/docs/project-build-run.md
@.claude/docs/repository-layout.md
@.claude/docs/synchronizer-model.md
@.claude/docs/client-java.md
@.claude/docs/intent-layer.md
@.claude/docs/model-generation.md
@.claude/docs/conventions.md
@.claude/docs/ci.md
@.claude/docs/external-docs.md

## Read on demand

Area-specific guidance. **Read the file before changing anything in the area it names** - these carry the traps that already cost someone a day.

| Topic | Read before touching |
| ----- | -------------------- |
| [`intent-dsl-features.md`](.claude/docs/intent-dsl-features.md) | any intent DSL construct: `engine-intent`, its generators, the templates it drives |
| [`harmonia-ui.md`](.claude/docs/harmonia-ui.md) | `template-application-ui-harmonia-java`, `template-form-builder-harmonia`, `application-core/shell/`, any `x-h-*` markup, a `harmonia.version` bump, or a suspected Harmonia bug (it is fixed upstream, never here) |
| [`shells.md`](.claude/docs/shells.md) | `resources-application`, `resources-builder`, `resources-monitoring`, perspective groups |
| [`blimpkit.md`](.claude/docs/blimpkit.md) | AngularJS IDE views/perspectives (`components/ui/*`, `bk-*` markup, `platform-links`) |
| [`tenants.md`](.claude/docs/tenants.md) | `core-tenants`, tenant resolution/selection, `security-oauth2`, tenant configuration |
| [`document-templates.md`](.claude/docs/document-templates.md) | `parsers/document`, `engine-document`, `.print` templates, PDF rendering |
| [`messaging.md`](.claude/docs/messaging.md) | `engine-listeners` broker config, `ide-messaging-monitoring`, messaging perspective |
| [`java-debugger.md`](.claude/docs/java-debugger.md) | `ide-java-debug`, `view-java-debug`, debug glyphs in `editor-monaco` |
| [`native-apps.md`](.claude/docs/native-apps.md) | `engine-native-apps` |

Add a new topic as a new file plus either an import line (only if it applies to nearly every change) or a row in this table.
