# sample-intent-money

The reference project of the in-process unit-test slice
([eclipse-dirigible/dirigible#7643](https://github.com/eclipse-dirigible/dirigible/issues/7643)): one
entity whose money field is computed by hand-written Java, and the unit tests of that Java.

- **`app.intent`** - an `InvoiceLine` whose `vatAmount` is calculated on every create and update by
  `calculatedActionOnCreate`/`OnUpdate: LineVatAction`.
- **`custom/LineVatAction.java`** - quantity x unit price x rate, half-up to the cent, in
  `BigDecimal` throughout.
- **`custom/test/LineVatActionTest.java`** - its unit tests, run in-process by
  [`dirigible-sdk-test`](../../../../../tests-sdk/README.md) (`@IntentSlice`): `1 x 2.90 @ 5%` is
  exactly 0.145, stored as 0.15; computed in `double` it is 0.14499999... and the test fails on the
  0.14 it stores. They run with `mvn test` in `tests/tests-sdk`, whose test suite this folder is.
- **`gen/`** - the Java the intent generates, checked in because it is what the tests compile
  against (only the Java: the UI, schema and roles come back on the next Generate).

`IntentSliceSampleIT` publishes the folder to a running platform and asserts that the application
compiles without its unit tests - the platform never compiles `custom/test/**` - and computes the
same VAT over REST.

## Regenerating `gen/`

After a change to `app.intent`, import the folder into a workspace, open `app.intent` and click
**Generate**, then copy the generated `gen/**/*.java` back here and run `mvn test` in
`tests/tests-sdk`.
