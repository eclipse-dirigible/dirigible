/**
 * @module http/client
 * @package @aerokit/sdk/http
 * @overview
 * 
 * This module provides functionalities for making HTTP requests within the Aerokit SDK. It includes classes and methods for sending HTTP requests, handling responses, managing sessions, and configuring HTTP clients.
 * 
 * The main components of this module are:
 * - HttpClient: Represents an HTTP client for sending requests and receiving responses.
 * - Request: Represents an HTTP request with methods for setting headers, body, and other parameters.
 * - Response: Represents an HTTP response with methods for accessing status, headers, and body content.
 * - Session: Provides session management for maintaining state across multiple HTTP requests.
 * - Upload: Provides functionalities for handling file uploads in HTTP requests.
 * - Errors: Contains classes for handling various HTTP-related errors and exceptions.
 * - Utils: Provides utility functions for working with HTTP requests and responses.
 * - AsyncClient: Represents an asynchronous HTTP client for sending requests and receiving responses in a non-blocking manner.
 * - Decorators: Provides decorators for defining HTTP endpoints and their properties in a declarative manner.
 */

export * as asyncClient from "./client-async";
export * from "./client";
export { HttpClient as client } from "./client";
export * as decorators from "./decorators";
export * from "./decorators";
export * from "./request";
export { Request as request } from "./request";
export * from "./response";
export { Response as response } from "./response";
export * as rs from "./rs";
export * from "./session";
export { Session as session } from "./session";
export * from "./upload";
export { Upload as upload } from "./upload";
export * as errors from "./errors";
export { HttpUtils as utils } from "./utils";
