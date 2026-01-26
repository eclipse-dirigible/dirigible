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
