/**
 * @module indexing/index
 * @package @aerokit/sdk/indexing
 * @overview
 * 
 * This module provides functionalities for indexing and search within the Aerokit SDK. It includes classes and methods for creating and managing search indexes, performing search queries, and writing data to indexes.
 * 
 * The main components of this module are:
 * - Searcher: Represents a searcher for performing search queries on an index and retrieving results.
 * - Writer: Represents a writer for adding, updating, and deleting documents in an index.
 */

export * from "./searcher";
export { Searcher as searcher } from "./searcher";
export * from "./writer";
export { Writer as writer } from "./writer";
