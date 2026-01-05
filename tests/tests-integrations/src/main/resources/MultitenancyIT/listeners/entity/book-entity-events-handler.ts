import { BookRepository } from "../../gen/edm/dao/Books/BookRepository";
import { logging } from "@aerokit/sdk/log";

const logger = logging.getLogger("book-entity-events-handler.ts");

export function onMessage(message: string) {
    logger.info("Listener: received message [{}]", message);
    const repo = new BookRepository();
    const books = repo.findAll();
    logger.info("Listener: found [{}] books. Books: [{}]", books.length, JSON.stringify(books));
}

export function onError(error: string) {
    logger.info("Listener: received error [{}]", error);

    const repo = new BookRepository();
    const books = repo.findAll();
    logger.info("Listener: found [{}] books. Books: [{}]", books.length, JSON.stringify(books));

}
