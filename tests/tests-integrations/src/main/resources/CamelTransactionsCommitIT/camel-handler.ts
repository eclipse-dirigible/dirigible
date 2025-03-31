import { BookRepository } from "./gen/edm/dao/Books/BookRepository";

export function onMessage(message: any) {
    const repo = new BookRepository();
    const entity = {
        Title: "test-camel-transactions-title-01",
        Author: "test-camel-transactions-author-01"
    }
    repo.create(entity);

    const entity2 = {
        Title: "test-camel-transactions-title-02",
        Author: "test-camel-transactions-author-02"
    }
    repo.create(entity2);

    const entity3 = {
        Title: "test-camel-transactions-title-03",
        Author: "test-camel-transactions-author-03"
    }
    repo.create(entity3);

    console.log("camel-handler.ts: test entities are saved");
}

