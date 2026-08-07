import { Update } from "@aerokit/sdk/db";

// The BOOK table comes from the generated schema; the row is written with plain SQL rather than a
// generated DAO because what this test asserts is the Quartz transaction boundary, not the
// persistence layer above it.
const INSERT_BOOK = "INSERT INTO BOOK (BOOK_TITLE, BOOK_AUTHOR) VALUES (?, ?)";

Update.execute(INSERT_BOOK, ["test-title-01", "test-author-01"]);

console.log("test-job-handler.ts: an entity is saved");

throw new Error("Intentionally throw error to check the QUARTZ transactions logic");
