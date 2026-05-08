/**
 * @module qldb/qldb
 * @package @aerokit/sdk/qldb
 * @name QLDBRepository
 * @overview
 *
 * Entry point for Amazon QLDB repository integration in Aerokit SDK.
 *
 * Exposes the `QLDBRepository` class alias to the underlying Java implementation:
 * `org.eclipse.dirigible.components.api.qldb.QLDBRepository`.
 *
 * This object is used to create and manage QLDB ledger sessions, execute statements,
 * and handle transaction lifecycles from JavaScript code.
 */

export const QLDBRepository = Java.type("org.eclipse.dirigible.components.api.qldb.QLDBRepository");