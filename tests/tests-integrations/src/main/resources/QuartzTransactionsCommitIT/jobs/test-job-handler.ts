import { Update } from "@aerokit/sdk/db";

// The BOOK table comes from the generated schema; the rows are written with plain SQL rather than a
// generated DAO because what this test asserts is the Quartz transaction boundary, not the
// persistence layer above it.
const INSERT_BOOK = "INSERT INTO BOOK (BOOK_TITLE, BOOK_AUTHOR) VALUES (?, ?)";

Update.execute(INSERT_BOOK, ["test-title-01", "test-author-01"]);
Update.execute(INSERT_BOOK, ["test-title-02", "test-author-02"]);
Update.execute(INSERT_BOOK, ["test-title-03", "test-author-03"]);

console.log("test-job-handler.ts: test entities are saved");
