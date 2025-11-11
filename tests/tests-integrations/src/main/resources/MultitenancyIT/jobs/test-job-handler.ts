import { BookRepository } from "../gen/edm/dao/Books/BookRepository";
import { logging } from "@aerokit/sdk/log";

const logger = logging.getLogger("test-job-handler.ts");

const repo = new BookRepository();
const books = repo.findAll();
logger.info("Job: found [{}] books. Books: [{}]", books.length, JSON.stringify(books));
