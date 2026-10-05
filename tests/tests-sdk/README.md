# dirigible-sdk-test - unit tests for an intent application's hand-written Java

The hand-written logic of a generated application lives in `custom/*.java`: calculated-field
actions, delegates, check predicates, allocation rules. `dirigible-sdk-test` runs that code **in the
test JVM** (dirigible [#7643](https://github.com/eclipse-dirigible/dirigible/issues/7643)): the
project's generated `gen/` and hand-written `custom/` classes on a private in-memory H2 database,
exactly as the platform runs them, with no web server, synchronizer, broker or IDE. A test takes
milliseconds; the application starts once per JVM (about a second).

```java
@IntentSlice
class LineVatActionTest {

    @Test
    void roundsTheVatHalfUpToTheCent(Slice slice) {
        InvoiceLineEntity line = slice.given(InvoiceLineEntity.class)
                                      .with("Description", "Paper")
                                      .with("Quantity", 1)
                                      .with("UnitPrice", "2.90")
                                      .with("VatRate", 5)
                                      .saved();

        assertEquals(new BigDecimal("0.15"), line.VatAmount);
    }
}
```

The reference project is
[`sample-intent-money`](../tests-integrations/src/main/resources/sample-intent-money): this module's
own test suite is that project's `custom/test/`.

## What runs

- **Entities through their generated repositories**: calculated fields and actions, validations,
  declared checks, `UnitOfWork`, the event outbox - Hibernate on H2, tables created from the
  entities as on the platform.
- **Components wired by the platform's own bean container** (`engine-java`'s `ComponentContainer`):
  constructor, field and collection injection, `Beans.get(...)`, extension points.
- **Document numbers** from the project's `.numbers` series.
- **Sent messages are recorded, not delivered**: every entity event a write publishes (once it
  committed, as on the platform) and every message the code sends, readable through
  `slice.sent()` / `slice.sentTo(destination)`. Nothing listens, so a reaction or process the
  event would start on the platform does not run.

Before each test every table is emptied, identities and number series restart, and the recorded
messages are cleared. Slice tests share the application's static entry points, so they must not run
in parallel.

## The API

| Call | What it does |
| ---- | ------------ |
| `@IntentSlice` / `@IntentSlice(project = "...")` | Runs the class against the project in the working directory, or the folder named relative to it |
| `Slice` test-method parameter | The handle on the running application |
| an application component as a parameter | e.g. `InvoiceLineRepository lines`, a `custom/` action |
| `slice.given(Entity.class).with("Field", value).saved()` | Builds a record and saves it through its repository; values convert to the field's type (`"2.90"` to `BigDecimal`, `"2026-01-31"` to `LocalDate`) |
| `slice.given(...).entity()` | The record unsaved, to call an action directly |
| `slice.repository(Entity.class)`, `slice.bean(Type.class)` | A repository, any component |
| `Slice.expectRefused(() -> ...)` | Asserts a validation or check refusal and returns it, `getMessage()` being what a REST caller reads |
| `slice.sent()`, `slice.sentTo(topic)` | The messages sent in this test |

## Setting up a project module

The tests live in the project's `custom/test/` folder - beside the code they test, and skipped by
the platform, which never compiles `custom/test/**` on publish. The module's build compiles the
project folder as a test source root, so `gen/`, `custom/` and `custom/test/` build together and the
tests see the very classes the slice runs. A `pom.xml` at the project root:

```xml
<dependencyManagement>
    <dependencies>
        <!-- The platform's own versions: the application compiles and runs against what it ships with. -->
        <dependency>
            <groupId>org.eclipse.dirigible</groupId>
            <artifactId>dirigible-parent</artifactId>
            <version>${dirigible.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>

<dependencies>
    <dependency>
        <groupId>org.eclipse.dirigible</groupId>
        <artifactId>dirigible-sdk-test</artifactId>
        <version>${dirigible.version}</version>
        <scope>test</scope>
    </dependency>
</dependencies>

<build>
    <testSourceDirectory>${project.basedir}</testSourceDirectory>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-compiler-plugin</artifactId>
            <configuration>
                <testIncludes>
                    <testInclude>gen/**/*.java</testInclude>
                    <testInclude>custom/**/*.java</testInclude>
                </testIncludes>
            </configuration>
        </plugin>
    </plugins>
</build>
```

`mvn test` then runs every `custom/test/**/*Test.java`. Commit `gen/` - it is what the tests compile
against - and regenerate it whenever the intent changes, as for a publish.

## Not covered (yet)

- **Reactions to events** - listeners, process triggers, notifications: the slice records the event
  and stops there. A `MailAsserter` for notify outcomes needs that dispatch first.
- **A fake clock**: generated rules read the system clock (`LocalDate.now()`, `Instant.now()`)
  directly, so a clock the slice could fix would first have to reach every generated time read.
- **REST concerns** - permissions, the controllers' own `validate()`, the personal and partner
  surfaces: cover them with an integration test.
- **Side tables from the generated `.schema`** (`_HISTORY`, `_LANG`): not created. A write of an
  entity declaring `history: true` stores its row but logs that it could not record the change
  history; a read of a `multilingual: true` entity returns the base values with a warning that the
  language table is not accessible.
