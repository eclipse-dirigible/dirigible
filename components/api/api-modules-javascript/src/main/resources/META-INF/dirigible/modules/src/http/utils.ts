/**
 * @module http/utils
 * @package @aerokit/sdk/http
 * @name HttpUtils
 * @overview
 * 
 * The HttpUtils class provides a collection of static utility methods for sending standardized HTTP responses in a consistent format. These methods abstract away the details of setting response status codes, content types, and formatting response bodies as JSON, allowing developers to easily send common success and error responses with minimal code.
 * 
 * ### Key Features:
 * - **Standardized Responses**: Provides methods for sending common HTTP responses such as 200 OK, 201 Created, 204 No Content, 400 Bad Request, 403 Forbidden, 404 Not Found, and 500 Internal Server Error.
 * - **JSON Formatting**: Automatically formats response bodies as JSON, ensuring a consistent response structure across the application.
 * 
 * ### Use Cases:
 * - **API Development**: These utilities are essential for API development, allowing developers to quickly send appropriate responses based on the outcome of request processing.
 * - **Error Handling**: Simplifies error handling by providing methods to send standardized error responses with descriptive messages.
 * 
 * ### Example Usage:
 * ```ts
 * import { HttpUtils } from "@aerokit/sdk/http";
 * 
 * // Sending a successful response with data
 * HttpUtils.sendResponseOk({ id: "123", name: "Example" });
 * 
 * // Sending a created response after creating a resource
 * HttpUtils.sendResponseCreated({ id: "123", name: "New Resource" });
 * 
 * // Sending a no content response after deleting a resource
 * HttpUtils.sendResponseNoContent();
 * 
 * // Sending a bad request error response
 * HttpUtils.sendResponseBadRequest("Invalid input data");
 * 
 * // Sending a forbidden error response
 * HttpUtils.sendForbiddenRequest("You do not have permission to access this resource");
 * 
 * // Sending a not found error response
 * HttpUtils.sendResponseNotFound("Resource not found");
 * 
 * // Sending an internal server error response
 * HttpUtils.sendInternalServerError("An unexpected error occurred");
 * ```
 */

import { response } from "@aerokit/sdk/http";

/**
 * Provides convenient static methods for sending standard HTTP responses.
 * All responses are automatically formatted as 'application/json'.
 */
export class HttpUtils {

    /**
     * Sends a successful response with HTTP status 200 (OK).
     * The provided entity is serialized as the JSON response body.
     * @param entity The data entity to return in the response body.
     */
    public static sendResponseOk(entity: any): void {
        HttpUtils.sendResponse(200, entity);
    }

    /**
     * Sends a successful response with HTTP status 201 (Created).
     * Typically used after a resource has been successfully created.
     * @param entity The data entity of the newly created resource.
     */
    public static sendResponseCreated(entity: any): void {
        HttpUtils.sendResponse(201, entity);
    }

    /**
     * Sends a successful response with HTTP status 204 (No Content).
     * Typically used for successful DELETE requests or updates that do not return a body.
     */
    public static sendResponseNoContent(): void {
        HttpUtils.sendResponse(204);
    }

    /**
     * Sends an error response with HTTP status 400 (Bad Request).
     * Used when the request could not be understood or processed due to client-side errors (e.g., validation failure).
     * @param message A descriptive error message explaining why the request was invalid.
     */
    public static sendResponseBadRequest(message: string): void {
        HttpUtils.sendResponse(400, {
            "code": 400,
            "message": message
        });
    }

    /**
     * Sends an error response with HTTP status 403 (Forbidden).
     * Used when the client is authenticated but does not have the necessary permissions to access the resource.
     * @param message A descriptive error message.
     */
    public static sendForbiddenRequest(message: string): void {
        HttpUtils.sendResponse(403, {
            "code": 403,
            "message": message
        });
    }

    /**
     * Sends an error response with HTTP status 404 (Not Found).
     * Used when the requested resource could not be found.
     * @param message A descriptive error message.
     */
    public static sendResponseNotFound(message: string): void {
        HttpUtils.sendResponse(404, {
            "code": 404,
            "message": message
        });
    }

    /**
     * Sends an error response with HTTP status 500 (Internal Server Error).
     * Used for unexpected server-side conditions encountered during processing.
     * @param message A descriptive error message (should mask internal details in production).
     */
    public static sendInternalServerError(message: string): void {
        HttpUtils.sendResponse(500, {
            "code": 500,
            "message": message
        });
    }

    /**
     * Generic private method to set the response status, content type, and body.
     * If a body is provided, it is stringified into JSON and written to the response.
     * @param status The HTTP status code to set (e.g., 200, 404, 500).
     * @param body The JavaScript object or string to be serialized as the response body (optional).
     */
    private static sendResponse(status: number, body?: any): void {
        response.setContentType("application/json");
        response.setStatus(status);
        if (body) {
            response.println(JSON.stringify(body));
        }
    }
}
