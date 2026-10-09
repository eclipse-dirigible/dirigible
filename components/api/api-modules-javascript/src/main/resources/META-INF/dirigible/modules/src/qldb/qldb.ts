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
 *
 * The QLDB driver is an add-on that the default bundle does not ship: the application adds
 * `org.eclipse.dirigible:dirigible-components-api-qldb-driver` (`<type>pom</type>`). Without it,
 * `new QLDBRepository(...)` throws `QldbNotAvailableException` naming that artifact.
 */

export const QLDBRepository = Java.type("org.eclipse.dirigible.components.api.qldb.QLDBRepository");