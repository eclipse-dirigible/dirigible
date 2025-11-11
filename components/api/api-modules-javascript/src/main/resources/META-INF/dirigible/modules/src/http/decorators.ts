import * as rs from "@aerokit/sdk/http/rs"

const router = rs.service();
let instance = null;

export function Controller(ctr: { new() }, context: ClassDecoratorContext): void {
    instance = new ctr();
    router.execute();
}

export function Documentation(documentation: string) {
  return function (
    value: any,
    context: ClassDecoratorContext | ClassFieldDecoratorContext | ClassMethodDecoratorContext
  ) {};
}

export const Get = createRequestDecorator("get")
export const Post = createRequestDecorator("post")
export const Put = createRequestDecorator("put")
export const Patch = createRequestDecorator("patch")
export const Delete = createRequestDecorator("delete")
export const Head = createRequestDecorator("head")
export const Options = createRequestDecorator("options")

function createRequestDecorator(httpMethod) {
    return function (path: string, consumesMimeTypes: undefined | string | string[] = ['*/*'], producesMimeTypes: undefined | string | string[] = ['application/json']): any {
        return function (target, propertyKey, descriptor) {
            const handler = descriptor ? descriptor.value : target;
            router.resource(path)[httpMethod]((ctx, req, res) => {
                handleRequest(req, res, ctx, handler);
            }).consumes(consumesMimeTypes).produces(producesMimeTypes);
        };
    }
}

function handleRequest(req, res, ctx, handler) {
    const body = req.json();
    const maybeResponseBody = handler.apply(instance || {}, [body, ctx, req, res]);
    if (maybeResponseBody) {
        res.json(maybeResponseBody);
    }
}
