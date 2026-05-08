/**
 * @module platform/index
 * @package @aerokit/sdk/platform
 * @overview
 * 
 * This module provides platform-specific utilities and abstractions for the Aerokit SDK.
 * It exposes command execution, engine management, lifecycle control, OS information,
 * problem reporting, registry/repository handling, and workspace operations.
 *
 * The main components of this module are:
 * - Command: Execution of platform commands and process interaction.
 * - Engine: Management of pluggable execution engines.
 * - Lifecycle: Application/service lifecycle helpers.
 * - OS: Operating system information and utilities.
 * - Problems: Unified problem reporting and diagnostics collection.
 * - Registry: Package/extension registry interfaces.
 * - Repository: Repository layout and access abstractions.
 * - Workspace: Workspace path and file handling helpers.
 */

export * from "./command";
export { Command as command } from "./command";
export * from "./engines";
export { Engine as engines } from "./engines";
export * from "./lifecycle";
export { Lifecycle as lifecycle } from "./lifecycle";
export * from "./os";
export { OS as os } from "./os";
export * from "./problems";
export { Problems as problems } from "./problems";
export * from "./registry";
export { Registry as registry } from "./registry";
export * from "./repository";
export { Repository as repository } from "./repository";
export * from "./workspace";
export { Workspace as workspace } from "./workspace";
