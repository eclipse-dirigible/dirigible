/**
 * @module mongodb/index
 * @package @aerokit/sdk/mongodb
 * @overview
 * 
 * This module provides functionalities for MongoDB integration within the Aerokit SDK. It includes classes and methods for connecting to MongoDB databases, performing CRUD operations, and managing MongoDB collections and documents.
 * 
 * The main components of this module are:
 * - Client: Represents a client for connecting to a MongoDB database and provides methods to perform operations on the database.
 * - Dao: Represents a Data Access Object (DAO) for managing MongoDB collections and documents, providing methods for CRUD operations and query execution.
 */

export * as client from "./client";
export * as dao from "./dao";