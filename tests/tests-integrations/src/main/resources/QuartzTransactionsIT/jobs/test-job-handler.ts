import { BookRepository } from "../gen/edm/dao/Books/BookRepository";
import { logging } from "sdk/log";

const logger = logging.getLogger("test-job-handler.ts");

const repo = new BookRepository();
const entity = {
    Title: "test-title-01",
    Author: "test-author-01"
}
repo.create(entity);

throw new Error("Intentionally throw error to check the transactions logic");
