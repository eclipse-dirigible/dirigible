import { BookRepository } from "./gen/edm/dao/Books/BookRepository";

export function onMessage(message: any) {
    const repo = new BookRepository();
    const entity = {
        Title: "test-camel-transactions-title-01",
        Author: "test-camel-transactions-author-01"
    }
    repo.create(entity);

    console.log("camel-handler.ts: an entity is saved");

    throw new Error("Intentionally throw error to check the Camel transactions logic");
}

