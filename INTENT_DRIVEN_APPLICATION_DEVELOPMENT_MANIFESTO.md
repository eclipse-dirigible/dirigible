# The Intent-Driven Application Development Manifesto

*Business intent, captured as a canonical model, regenerated deterministically into
running software.*

---

## Preamble

Model-Driven Development was never the wrong idea. It was the right idea with the
wrong front door.

For three decades the industry chased the same promise: describe a system once, at a
high level of abstraction, and let a machine generate the running application from that
description. The promise is sound. Where it has repeatedly failed is not in the
generation - generation has always worked - but in the act of **authoring the models by
hand**. The modellers were too heavy, the configuration too deep, the learning curve too
steep. So the abstraction that was supposed to save time became the thing developers
routed around.

This manifesto states what we are doing in Eclipse Dirigible: keeping everything that
made Model-Driven Development correct, and replacing the one part that made it fail. We
call the result **Intent-Driven Application Development** - the AI-native successor to
MDD, not a revival of it. Artificial intelligence is not bolted on as a feature. It is
the **authoring layer** the discipline was always missing.

---

## I. What Model-Driven Development Got Right

Strip away the tooling and MDD rests on three claims that are simply true:

1. **Two altitudes of model are enough to describe most business software.**
   An *abstract* model says *what* the system is (entities, relationships, processes,
   permissions, the data it shows and the actions it permits). A *specific* model says
   *how* a given platform realises it (an EDM document, a BPMN diagram, a form
   definition, a report query, a role and access file). The abstract model is portable
   intent; the specific model is a platform's deterministic interpretation of it.

2. **Generation from a model is deterministic and repeatable.**
   Given the same specific model, a generator produces the same full-stack application,
   every time, byte for byte. There is no creativity in this step and there should not
   be. Determinism is the entire value: the model is the source of truth, the code is a
   pure function of the model, and regeneration is safe.

3. **The full stack, and its extensions, are derivable.**
   Database schema, persistence, REST services, UI, dashboards, scheduled jobs, message
   listeners, process flows, security - all of it follows from the models. So do the
   extension points. A platform that owns the generators owns a complete, consistent
   application factory.

None of this is in dispute. Dirigible has shipped it for years: the synchronizer model
turns declarations on disk into a running application, and the "Generate from EDM /
Schema / BPMN" templates turn models into TypeScript, Java, SQL and HTML.

The models are not the problem. They never were.

---

## II. Why Model-Driven Development Failed

The failure was always at the keyboard, not in the pipeline.

- **Too many configurations.** Every entity needs a perspective, a layout type, widget
  types, icons, menu keys, audit settings, relationship cardinalities, dropdown data
  sources, referenced properties. Every one is a correct and necessary detail of the
  *specific* model. Asked of a human, for every field of every entity, they become a
  tax that dwarfs the problem being solved.

- **Too many clicks and tweaks.** A graphical modeller turns a five-second thought
  ("add a country field to Customer") into a sequence of dialogs, property panels,
  drag-drops and save steps. The cost of *expressing* an intent came to exceed the cost
  of the intent itself.

- **Too steep a learning curve.** Driving the BPMN modeller, the Entity Data Model
  modeller, the Forms and Reports designers *correctly* is a specialist skill. Gateways,
  sequence flows, compositions versus associations, perspective inheritance, report
  joins and aggregations - each modeller is a small discipline of its own. The promise
  of MDD was *less* expertise required, but the modellers demanded *more*.

- **The escape hatch always won.** Because authoring the model was so costly, developers
  dropped to hand-written code at the first sign of friction - and once code and model
  diverge, the model is dead. Thirty years of MDE projects died precisely here: pure
  model-driven approaches with no graceful escape, abandoned the moment one real-world
  requirement did not fit the modeller.

The common thread: **the abstraction was sound, but the only way to reach it was through
a surface humans found hostile.** MDD asked people to think like a generator. Most would
not, and the ones who could were too few and too expensive.

---

## III. The Diagnosis: The Models Were Never the Problem

This is the pivot of the whole argument.

> The specific models and their deterministic generators are an asset, not a liability.
> The defect was the **authoring surface** - the human-operated modeller standing between
> a person's intent and the model.

So the fix is not to throw away MDD. It is not to replace deterministic generation with
a model that writes code directly (that trades a solved problem - reliable generation -
for an unsolved one - reliable code synthesis). The fix is to **remove the hostile
authoring surface and put a better one in its place.**

The better authoring surface is one a human can drive in seconds and a machine can keep
compliant: natural language in, a structured abstract model out, deterministic specific
models and a full application after that. The graphical modellers do not disappear - they
remain as the precise, read-only-by-default view and the power-user's escape - but they
stop being the *only* door.

---

## IV. The Shift: AI as the Authoring Layer

Artificial intelligence is good at exactly the thing MDD was bad at, and bad at exactly
the thing MDD was good at. They are complementary, not competing.

- AI is excellent at **turning ambiguous human language into a structured abstract
  model**. "I need a small lending library: members, books, loans, and a loan needs
  approval over thirty days" becomes a precise declaration of entities, relations, a
  process and its gateway. This is the step humans found painful and AI finds natural.

- AI is **not** asked to generate the application. Code generation stays where it
  belongs: in the platform's deterministic generators. The model is still the contract;
  the application is still a pure function of the model.

So the division of labour is clean:

| Concern | Owner | Property |
|---|---|---|
| Natural language to abstract model | **AI** | helpful, fallible, supervised |
| Abstract model to specific models | **Deterministic generator** | pure function |
| Specific models to full-stack app | **Low-Code Platform templates** | pure function |

AI lives only in the first row, and even there it **proposes**; a human **accepts**. The
abstract model is structured and human-authorable, so AI is an accelerator, never a
single point of failure. Turn the AI off and the surface is still a structured editor a
person can drive directly.

This is the sentence the manifesto exists to say:

> **AI helps a human define an easy, high-level abstract model that is provably compliant
> with the specific models and generators a Low-Code Platform already implements.**

---

## V. Prompts Are Not the Source of Truth

This is why the architecture matters specifically now, in the AI era.

The fastest way to get an application from an AI today is to prompt for code directly. It
demos beautifully and fails in the organisation, for a reason that has nothing to do with
how capable the model is:

- **A prompt is not a system of record.** It is a transient instruction, phrased
  differently by every person, gone the moment the conversation scrolls away. You cannot
  version it, review it as the authoritative specification, or regenerate from it
  reliably. Ask twice and you get two different programs.

- **Generated code is not a stable abstraction.** It is the *output*, not the *intent*.
  It is voluminous, it drifts the instant a human edits it, and it bakes in a thousand
  incidental decisions the prompt never expressed. Governing software by reading its
  generated code is governing at the wrong altitude.

So "AI writes the code" leaves an organisation holding two artefacts it cannot govern - a
disposable prompt and an unstable mass of code - with nothing canonical in between.

The intent model is that missing canonical thing: **a versionable, reviewable, diffable
representation of what the business actually asked for** - small enough to read,
structured enough to validate, stable enough to regenerate from deterministically. It is
the system of record a prompt can never be, and the abstraction generated code should
never be asked to be.

```
AI Coding - the trap
────────────────────
   Prompt  ->  Code

   A transient prompt and code that drifts on first edit.
   Nothing canonical in between, nothing to govern.


Intent-Driven Application Development
─────────────────────────────────────
   Business Intent
        │
        ▼
       AI
        │
        ▼
   Canonical Intent Model   <-- the asset
        │
        ▼
   Platform Models
        │
        ▼
   Java / Spring Application

   The intent model is canonical: versioned, reviewed, regenerated from.
   The prompt is thrown away; the application is rebuilt deterministically.
```

The difference is one line:

> **Prompts are not the source of truth. Intent models are.**

The prompt becomes disposable - a convenient way to *edit* the intent model, and nothing
more. It lands its patch and is thrown away. The intent model is what you keep, version,
review, and regenerate from. The asset was never the conversation, and never the
generated code. The asset is the model in the middle.

---

## VI. The Three Altitudes

Dirigible expresses the discipline as three layers, each strictly above the next, each
the deterministic input to the one below.

```
   Natural language / prompt
            │  (AI proposes a patch; human accepts)
            ▼
┌───────────────────────────────────────────────┐
│  1. INTENT  -  the abstract model             │   one file, human + AI authored
│     entities · relations · processes ·        │   structured, diff-stable, portable
│     forms · reports · permissions · seeds     │
└───────────────────────────────────────────────┘
            │  (deterministic generation - pure functions)
            ▼
┌───────────────────────────────────────────────┐
│  2. MODELS  -  the specific models            │   .edm / .model · .bpmn · .form ·
│     the platform's native, openable artefacts │   .report · .roles · .csvim
└───────────────────────────────────────────────┘
            │  (deterministic generation - the existing template engine)
            ▼
┌───────────────────────────────────────────────┐
│  3. APPLICATION  -  the full stack            │   schema · persistence · REST · UI ·
│     plus its extensions                       │   jobs · listeners · processes · security
└───────────────────────────────────────────────┘
```

The abstract model is **one altitude above** the models a Low-Code Platform already
generates from. It does not replace them - it *authors* them. Everything below the top
layer is machinery the platform has run reliably for years. AI only changes how the top
layer gets written.

Critically, the abstract model emits **specific models**, never code. It stops at the
`.edm`, the `.bpmn`, the `.form`. The platform's own template engine takes it from there.
The intent layer is deliberately ignorant of which template will consume its output, so
it can never couple itself to one generator's choices. Wrong altitude is the cardinal
sin: the moment an abstract-model generator emits a `Controller.ts` or a `*.java`, the
layering has collapsed and the discipline is lost.

---

## VII. Principles

These are the tenets that keep Intent-Driven Application Development honest. Violate one
and you slide back into either the old modeller trap or the new "let the AI write the
app" trap.

1. **The specific models stay canonical.** The platform's deterministic generators are
   the contract. AI authors *toward* them; it does not bypass them and it does not get to
   invent new ones at will.

2. **Generation is deterministic, top to bottom.** Identical abstract model produces
   identical specific models produces an identical application. Determinism is the
   property that makes regeneration safe and the model trustworthy as the source of
   truth.

3. **AI proposes; the human disposes.** Every AI contribution is a *patch* to the
   abstract model, previewed and accepted by a person. The AI is replaceable and
   optional. The abstract model is structured enough to be authored by hand when the AI
   misunderstands.

4. **Edit the shape of intent, not the shape of files.** "Add a country field" must be a
   one-line change to the abstract model, not a re-emitted, re-ordered file. The model's
   on-disk form is diff-stable so that AI edits stay minimal, reviewable and trustworthy.

5. **One abstract model, whole-picture.** The entire application's intent lives in one
   place so the AI always diffs against the full context and the human always sees the
   whole system at once.

6. **Structured intent, not free text.** Natural language is the *input*; the artefact is
   structured. The transform from abstract model to specific models is a pure
   deterministic function, not a second act of interpretation.

7. **Honour the escape hatch.** Real applications always have one weird requirement that
   no model expresses. Pure MDD that forbids escape dies on that requirement. The
   discipline must provide a first-class place for hand-written code that survives
   regeneration - declared as hook points in the abstract model, implemented alongside
   the generated output, never overwritten. When something cannot be expressed, the
   answer is to *extend the abstract model* (and teach a generator to consume it), not to
   leak hand-edits into generated folders.

8. **Visualisation is read-only.** Diagrams render the abstract model for a fast human
   read. They are not a round-trip authoring surface. Authoring is the prompt plus the
   structured editor; the graphical view confirms, it does not capture.

9. **The platform line is sacred: authoring artefacts get editors and an explicit
   generate step; only runtime artefacts get reconciled automatically.** The abstract
   model is an authoring artefact. It is generated in the workspace, reviewed, and
   published as inert source alongside the models and code it produced - never silently
   materialised behind the developer's back.

---

## VIII. The Role of the Low-Code Platform

Intent-Driven Application Development is not possible without a Low-Code Platform
underneath it, and this is the part that is easy to miss.

The AI can only propose an abstract model that is *compliant* because the platform has
already defined, in code and proven by integration tests, exactly what a valid specific
model is and exactly how it becomes an application. The generators are the grammar the AI
writes against. Without them, "AI builds your app" is a demo that works once; with them,
it is a factory that works every time.

So the Low-Code Platform contributes the two things AI cannot:

- **A fixed, deterministic target.** The set of specific models and their generators is
  finite, versioned and tested. The AI has a closed world to be correct about, not an
  open-ended code-writing task to be plausibly-wrong about.

- **A complete runtime.** Once the models exist, the platform supplies the database,
  persistence, services, security, scheduling, messaging and UI runtime that turn them
  into a live system. The AI never touches any of this.

Dirigible is that platform. The abstract model (intent) is the new top layer; the EDM /
BPMN / form / report / roles / CSVIM models and their template generators are the layer
that was always here. Intent-Driven Application Development is the *composition* of the
two.

---

## IX. What This Is Not

To say clearly what we are doing, it helps to say what we are refusing to do.

- **This is not "AI writes the code."** Code is generated deterministically from models,
  as it always was. The AI operates one or two altitudes above the code and never emits
  it.

- **This is not a replacement for the platform.** The generators, runtime and models are
  the foundation, not legacy to be superseded. AI is an authoring convenience on top of a
  proven factory.

- **This is not a black box.** Every step is inspectable. The abstract model is
  human-readable and human-editable; the specific models open in the existing editors;
  the generated code is ordinary code. A developer can stop at any layer and take over.

- **This is not pure MDD reborn.** The escape hatch is first-class and designed in from
  the start, because the lesson of thirty years is that the escape hatch always wins and
  the only question is whether the platform planned for it or got destroyed by it.

- **This is not non-deterministic generation.** The only non-deterministic, fallible step
  is natural-language-to-abstract-model, and that step is always supervised and always
  optional.

---

## X. The Workflow

In practice, for the developer, the whole discipline collapses to this:

1. Describe what you want, in plain language, to the AI.
2. The AI proposes a patch to the abstract model. You see the change and a diagram of its
   effect. You accept or refine.
3. Generate. The platform deterministically writes the specific models next to your
   abstract model, in your workspace.
4. Generate again, one level down. The platform's template engine turns those models into
   the full-stack application and its extensions.
5. Where reality does not fit, extend the abstract model or drop a hand-written
   implementation into the designated escape-hatch location. Regenerate freely; your
   override survives.
6. Publish. The platform brings the application live exactly as it would for any
   hand-modelled project.

No modeller expertise required to start. No clicks-and-tweaks tax to pay. No divergence
between model and code. The abstraction MDD always promised, reached through a door
humans will actually walk through.

---

## XI. The Dream, Stated Plainly

> No code. No modelling. Just intent.

A person says what the system should do. AI shapes that into a precise, compliant
abstract model. The Low-Code Platform deterministically turns the abstract model into
specific models, and the specific models into a complete, running, extensible
application. Each layer is inspectable, each transform below the top is a pure function,
and a human is in the loop at the only point where judgement is required.

Model-Driven Development was right all along. It was waiting for an authoring layer worth
using. That layer is here, and the discipline it unlocks is Intent-Driven Application
Development.

---

*Eclipse Dirigible - the polyglot, intent-driven application platform.*
