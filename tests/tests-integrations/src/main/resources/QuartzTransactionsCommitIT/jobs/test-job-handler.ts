import { BookRepository } from "../gen/edm/dao/Books/BookRepository";
import { logging } from "@aerokit/sdk/log";

const logger = logging.getLogger("test-job-handler.ts");


const repo = new BookRepository();
const entity = {
    Title: "test-title-01",
    Author: "test-author-01"
}
repo.create(entity);

const entity2 = {
    Title: "test-title-02",
    Author: "test-author-02"
}
repo.create(entity2);

const entity3 = {
    Title: "test-title-03",
    Author: "test-author-03"
}
repo.create(entity3);

console.log("test-job-handler.ts: test entities are saved");

