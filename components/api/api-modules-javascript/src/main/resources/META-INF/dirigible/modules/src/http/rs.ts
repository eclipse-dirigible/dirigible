/**
 * @module http/rs
 * @package @aerokit/sdk/http
 * @name RestService
 * @overview
 *
 * This module provides REST service utilities for the Aerokit SDK.
 * It includes decorators and classes for building HTTP controllers,
 * defining resource methods, and managing RESTful endpoints.
 *
 * The main components of this module are:
 * - service: Service decorator for HTTP services.
 * - HttpController: Base class for HTTP controllers.
 * - ResourceMethod: Enum or decorator for HTTP methods (GET, POST, etc.).
 * - ResourceMappings: Utilities for mapping resources to endpoints.
 * - Resource: Decorator for defining REST resources.
 * 
 * ### Example Usage:
 * ```ts
 * import { Controller, Get, Post, Documentation } from "@aerokit/sdk/http";
 * 
 * @Controller
 * @Documentation("UserController handles user-related operations.")
 * class UserController {
 *     @Get("/users/{id}")
 *     @Documentation("Retrieves a user by ID.")
 *     getUserById(id: string) {
 *         // Logic to retrieve user by ID
 *     }
 * 
 *     @Post("/users")
 *     @Documentation("Creates a new user.")
 *     createUser(userData: any) {
 *         // Logic to create a new user
 *     }
 * }
 * ```
 */
export { service, HttpController } from "./rs/resource-http-controller";
export { ResourceMethod } from "./rs/resource-method";
export { ResourceMappings } from "./rs/resource-mappings";
export { Resource } from "./rs/resource";