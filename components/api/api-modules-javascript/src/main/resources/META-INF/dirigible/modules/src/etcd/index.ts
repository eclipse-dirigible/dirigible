/**
 * @module etcd/index
 * @package @aerokit/sdk/etcd
 * @overview
 * 
 * This module provides functionalities for interacting with an etcd key-value store within the Aerokit SDK. It includes classes and methods for connecting to an etcd server, performing CRUD operations on keys, and managing etcd clusters.
 * 
 * The main components of this module are:
 * - Client: Represents a client for connecting to an etcd server and provides methods to perform operations on the key-value store.
 */

export * from "./client";
export { Client as client } from "./client";
