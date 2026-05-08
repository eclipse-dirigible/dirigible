/**
 * @module redis/index
 * @package @aerokit/sdk/redis
 * @overview
 *
 * This module provides Redis client integration for the Aerokit SDK.
 * It exposes capabilities to connect to Redis servers, execute commands,
 * and manage data structures and transactions.
 *
 * The main components of this module are:
 * - Client: Redis client wrapper with command execution and connection management.
 */

export * from "./client";
export { Client as client } from "./client";
