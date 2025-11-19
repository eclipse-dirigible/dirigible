import { BookRepository } from "../gen/edm/dao/Books/BookRepository";
import { logging } from "@aerokit/sdk/log";

const logger = logging.getLogger("test-job-handler.ts");


const repo = new BookRepository();
const entity = {
    Title: "test-title-01",
    Author: "test-author-01"
}
repo.create(entity);
console.log("test-job-handler.ts: an entity is saved");

throw new Error("Intentionally throw error to check the QUARTZ transactions logic");
