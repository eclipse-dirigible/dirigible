/**
 * @module http/decorators
 * @package @aerokit/sdk/http
 * @name Decorators
 * @overview
 * 
 * This module provides a set of decorators for defining HTTP controllers and their associated routes in a declarative manner. The decorators allow developers to annotate classes and methods to specify how they should handle HTTP requests, making it easier to create RESTful APIs with clear and concise code.
 * 
 * ### Key Features:
 * - **Controller Decorator**: The `@Controller` decorator is used to mark a class as an HTTP controller, which can contain multiple route handlers.
 * - **HTTP Method Decorators**: Decorators such as `@Get`, `@Post`, `@Put`, `@Patch`, `@Delete`, `@Head`, and `@Options` are used to define the HTTP method and path for each route handler method within the controller.
 * - **Documentation Decorator**: The `@Documentation` decorator allows developers to add documentation to their controllers or methods, which can be useful for generating API documentation.
 * 
 * ### Use Cases:
 * - **API Development**: These decorators are primarily used in the development of RESTful APIs, allowing developers to define their endpoints and handlers in a clear and structured way.
 * - **Code Organization**: By using decorators, developers can keep their code organized and maintainable, separating the routing logic from the business logic within their controllers.
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

import * as rs from "@aerokit/sdk/http/rs";

const ROUTES_KEY = Symbol.for("dirigible.controller.routes");

const GLOBAL_ROUTES: any[] =
    (globalThis as any)[ROUTES_KEY] ??
    ((globalThis as any)[ROUTES_KEY] = []);

const router = rs.service();

export function Controller(ctr: { new(): any }, _context?: ClassDecoratorContext) {
    const instance = new ctr();
    const routes = GLOBAL_ROUTES;
    for (const route of routes) {
        const fn = instance[route.propertyKey.name];
        if (typeof fn === "function") {
			((fn, instance) => {
		        router.resource(route.path)[route.method]((ctx, req, res) => {
		            const body = req.json ? req.json() : null;
		            const result = fn.call(instance, body, ctx, req, res);
		            if (result !== undefined) {
		                res.json(result);
		            }
		        });
		    })(fn, instance);
        }
    }

    router.execute();
	GLOBAL_ROUTES.length = 0;
}


export function Documentation(documentation: string) {
    return function (
        value: any,
        context: ClassDecoratorContext | ClassFieldDecoratorContext | ClassMethodDecoratorContext
    ) {};
}

function createRequestDecorator(httpMethod: string) {
    return function (path: string) {
        return function (target: any, propertyKey: string | ClassMethodDecoratorContext, descriptor?: PropertyDescriptor) {
            GLOBAL_ROUTES.push({
                controller: target.constructor,
                method: httpMethod,
                path: path || "/",
                propertyKey
            });
        };
    };
}

export const Get = createRequestDecorator("get");
export const Post = createRequestDecorator("post");
export const Put = createRequestDecorator("put");
export const Patch = createRequestDecorator("patch");
export const Delete = createRequestDecorator("delete");
export const Head = createRequestDecorator("head");
export const Options = createRequestDecorator("options");
