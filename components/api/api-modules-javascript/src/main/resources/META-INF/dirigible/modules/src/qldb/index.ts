/**
 * @module qldb/index
 * @package @aerokit/sdk/qldb
 * @overview
 * 
 * Provides simplified integration with Amazon QLDB (Quantum Ledger Database) for Aerokit SDK.
 * The module exposes repository abstractions to interact with QLDB ledgers, execute statements,
 * manage sessions and handle results in a structured way.
 *
 * The main components of this module are:
 * - QLDBRepository: Primary interface for connecting to and operating on QLDB ledgers.
 */

export * from "./qldb";
export { QLDBRepository as qldb } from "./qldb";