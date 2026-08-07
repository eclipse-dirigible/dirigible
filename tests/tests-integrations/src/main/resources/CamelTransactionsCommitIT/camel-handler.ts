import { Update } from "@aerokit/sdk/db";

// The BOOK table comes from the generated schema; the rows are written with plain SQL rather than a
// generated DAO because what this test asserts is the Camel transaction boundary, not the
// persistence layer above it.
const INSERT_BOOK = "INSERT INTO BOOK (BOOK_TITLE, BOOK_AUTHOR) VALUES (?, ?)";

export function onMessage(message: any) {
    Update.execute(INSERT_BOOK, ["test-camel-transactions-title-01", "test-camel-transactions-author-01"]);
    Update.execute(INSERT_BOOK, ["test-camel-transactions-title-02", "test-camel-transactions-author-02"]);
    Update.execute(INSERT_BOOK, ["test-camel-transactions-title-03", "test-camel-transactions-author-03"]);

    console.log("camel-handler.ts: test entities are saved");
}
