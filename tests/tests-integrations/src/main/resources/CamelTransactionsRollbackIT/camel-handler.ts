import { Update } from "@aerokit/sdk/db";

// The BOOK table comes from the generated schema; the row is written with plain SQL rather than a
// generated DAO because what this test asserts is the Camel transaction boundary, not the
// persistence layer above it.
// The identifiers are quoted because the generated schema creates them in upper case and
// PostgreSQL folds an unquoted name to lower case.
const INSERT_BOOK = 'INSERT INTO "BOOK" ("BOOK_TITLE", "BOOK_AUTHOR") VALUES (?, ?)';

export function onMessage(message: any) {
    Update.execute(INSERT_BOOK, ["test-camel-transactions-title-01", "test-camel-transactions-author-01"]);

    console.log("camel-handler.ts: an entity is saved");

    throw new Error("Intentionally throw error to check the Camel transactions logic");
}
