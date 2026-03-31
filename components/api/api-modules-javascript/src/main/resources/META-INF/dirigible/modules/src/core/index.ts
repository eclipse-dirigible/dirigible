/**
 * @module core/index
 * @package @aerokit/sdk/core
 * @overview
 * 
 * This module provides core functionalities for the Aerokit SDK. It includes classes and methods for managing configurations, context, environment variables, and global variables that are essential for the operation of the SDK.
 * 
 * The main components of this module are:
 * - Configurations: Manages the configuration settings for the SDK, allowing for retrieval and management of configuration values.
 * - Context: Provides a context for the execution of SDK operations, allowing for the storage and retrieval of contextual information.
 * - Env: Handles environment variables, providing methods to access and manage environment-specific settings.
 * - Globals: Manages global variables that can be accessed throughout the SDK, allowing for shared state and information across different modules.
 */

export * from "./configurations"
export { Configurations as configurations } from "./configurations"
export * from "./context";
export { Context as context } from "./context";
export * from "./env";
export { Env as env } from "./env";
export * from "./globals";
export { Globals as globals } from "./globals";