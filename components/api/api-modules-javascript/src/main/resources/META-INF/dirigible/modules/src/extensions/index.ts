/**
 * @module extensions/index
 * @package @aerokit/sdk/extensions
 * @overview
 * 
 * This module provides functionalities for managing extensions within the Aerokit SDK. It includes classes and methods for defining, registering, and managing extensions that can enhance the capabilities of the SDK.
 * 
 * The main components of this module are:
 * - Extensions: Represents a registry for managing extensions and provides methods to register and retrieve extensions.
 * - Decorators: Provides decorators for defining extensions and their properties in a declarative manner.
 */

export * from "./extensions";
export { Extensions as extensions } from "./extensions";
export * as decorators from "./decorators";
export * from "./decorators";