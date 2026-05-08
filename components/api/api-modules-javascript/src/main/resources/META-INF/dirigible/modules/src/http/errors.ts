/**
 * @module http/errors
 * @package @aerokit/sdk/http
 * @name Errors
 * @overview
 *
 * This module provides HTTP-specific error classes for the Aerokit SDK.
 * It includes custom error types for handling common HTTP response scenarios
 * like forbidden access and validation failures.
 *
 * The main components of this module are:
 * - ForbiddenError: Represents a 403 Forbidden HTTP error.
 * - ValidationError: Represents validation-related errors in HTTP requests.
 */
export { ForbiddenError } from "./errors/ForbiddenError";
export { ValidationError } from "./errors/ValidationError";