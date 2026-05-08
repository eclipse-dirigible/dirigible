/**
 * @module db/index
 * @package @aerokit/sdk/db
 * @overview
 * 
 * This module provides functionalities for database management within the Aerokit SDK. It includes classes and methods for interacting with databases, executing SQL queries, managing transactions, and working with data repositories.
 * 
 * The main components of this module are:
 * - Database: Represents a database connection and provides methods to interact with it, such as executing queries and managing transactions.
 * - SQLBuilder: Provides a fluent API for building SQL queries programmatically.
 * - Procedure: Represents a stored procedure in the database and provides methods to execute it.
 * - Sequence: Represents a database sequence and provides methods to interact with it.
 * - Query: Represents a database query and provides methods to execute it and retrieve results.
 * - Update: Represents an update operation in the database and provides methods to execute it.
 * - Insert: Represents an insert operation in the database and provides methods to execute it.
 * - Store: Represents a data store that can be used for caching or temporary storage of data.
 * - Repository: Provides a repository pattern implementation for managing data entities in the database.
 * - Decorators: Provides decorators for defining database entities, repositories, and other related components in a declarative manner
 */

export * as dao from "./dao";
export * from "./database";
export { Database as database } from "./database";
export * as orm from "./orm";
export * as ormstatements from "./ormstatements";
export * from "./sql";
export { SQLBuilder as sql } from "./sql";
export * from "./procedure";
export { Procedure as procedure } from "./procedure";
export * from "./sequence";
export { Sequence as sequence } from "./sequence";
export * from "./query";
export { Query as query } from "./query";
export * from "./update";
export { Update as update } from "./update";
export * from "./insert";
export { Insert as insert } from "./insert";
export * from "./store";
export { Store as store } from "./store";
export * from "./repository";
export { Repository as repository } from "./repository";
export * from "./decorators";
export * as decorators from "./decorators";
export * from "./translator";
export { Translator as translator } from "./translator";