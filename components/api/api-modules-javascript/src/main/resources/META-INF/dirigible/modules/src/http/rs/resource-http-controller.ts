import { Response as response } from "../response";
import { Request as request } from "../request";
import { ResourceMappings } from "./resource-mappings";
import { Logging } from "sdk/log";

// Declaration for the external dirigibleRequire function and its dependency
declare function dirigibleRequire(module: string): any;
const { match } = dirigibleRequire("modules/src/http/path-to-regexp/6.2.1/index.js");

const logger: any = Logging.getLogger('http.rs.controller');

/**
 * Interface for the context object passed to handler functions (before, serve, catch).
 */
interface RequestContext {
    pathParameters: { [key: string]: any };
    queryParameters: { [key: string]: any };
    response: any;
    res: any;
    request: any;
    req: any;
    // Context properties for error handling might also be attached here
    suppressStack?: boolean;
    httpErrorCode?: number;
    errorMessage?: string;
    errorName?: string;
    errorCode?: any;
}

function getRequest(): any {
    return request;
}
function getResponse(): any {
    return response;
}

/**
 * Creates a service (HttpController) instance, optionally initialized with oMappings.
 *
 * @param oConfig Configuration object or configuration builder with configuration() getter function.
 * @returns A new HttpController instance.
 */
export function service(oConfig?: any): HttpController {
    let config: ResourceMappings | any;
    if (oConfig !== undefined) {
        if (typeof oConfig === 'object' || oConfig instanceof ResourceMappings) {
            config = oConfig;
        }
    }
    return new HttpController(config);
}

/**
 * The main class for handling HTTP requests and routing them to the correct resource handlers.
 */
export class HttpController {

    resource: Function;
    resourcePath: Function;
    resourceMappings: ResourceMappings;

    // Index signature to allow dynamic method assignment in the constructor
    [key: string]: any;

    /**
     * Constructor function for HttpController instances.
     *
     * @param oMappings The mappings configuration for this controller.
     */
    constructor(oMappings?: ResourceMappings | any) {
        if (oMappings instanceof ResourceMappings) {
            this.resourceMappings = oMappings;
        } else if (typeof oMappings === 'object' || oMappings === undefined) {
            this.resourceMappings = new ResourceMappings(oMappings, this);
        } else {
            // Default initialization if input is unexpected
            this.resourceMappings = new ResourceMappings({}, this);
        }

        // Alias for resourceMappings.resource
        this.resource = this.resourcePath = this.resourceMappings.resourcePath.bind(this.resourceMappings);

        // weave-in HTTP method-based factory functions - shortcut for service().resource(sPath).method
        ['get', 'post', 'put', 'delete', 'remove', 'method'].forEach((sMethodName: string) => {
            this[sMethodName] = (...allArguments: any[]): HttpController => {
                if (allArguments.length < 1)
                    throw Error('Insufficient arguments provided to HttpController method ' + sMethodName + '.');

                // sPath is always the first argument
                let sPath = allArguments[0];
                if (sPath === undefined)
                    sPath = "";

                // The next arguments (sVerb, arrConsumes, arrProduces) are used by ResourceMappings.find/resource
                const sVerb = allArguments[1];
                const arrConsumes = allArguments[2];
                const arrProduces = allArguments[3];

                // Find existing resource or create a new one based on the path (and other optional constraints)
                const resource: any = this.resourceMappings.find(sPath, sVerb, arrConsumes, arrProduces) || this.resourceMappings.resource(sPath);

                // Pass all arguments *except* the sPath to the corresponding method of the resource
                const resourceMethodArgs = allArguments.slice(1);
                
                // Execute the method on the resource instance
                resource[sMethodName].apply(resource, resourceMethodArgs);
                
                return this;
            };
        });
    }

    /**
     * Alias for execute.
     */
    listen(request: any, response: any): void {
        this.execute(request, response);
    }

    /**
     * Executes the request handling logic, finding the best matching resource and handler.
     */
    execute(request?: any, response?: any): void {
        request = request || getRequest();
        const requestPath: string = request.getResourcePath();
        const method: string = request.getMethod().toLowerCase();
        const _oConfiguration: any = this.resourceMappings.configuration();

        const matches: any[] = matchRequestUrl(requestPath, method, _oConfiguration);
        let resourceHandler: any;

        if (matches && matches[0]) {
            const verbHandlers: any[] = _oConfiguration[matches[0].d][method];
            if (verbHandlers) {
                resourceHandler = verbHandlers.filter((handlerDef) => {
                    return matchMediaType(request, handlerDef.produces, handlerDef.consumes);
                })[0];
            }
        }

        response = response || getResponse();
        const queryParams: { [key: string]: any } = request.getQueryParametersMap() || {};
        const acceptsHeader: string[] = normalizeMediaTypeHeaderValue(request.getHeader('Accept')) || [];
        const contentTypeHeader: string[] = normalizeMediaTypeHeaderValue(request.getHeader('Content-Type')) || [];
        const resourcePath: string = requestPath;

        if (resourceHandler) {
            const ctx: RequestContext = {
                "pathParameters": {},
                "queryParameters": {},
                "response": response,
                "res": response,
                "request": request,
                "req": request
            };
            if (matches[0].pathParams) {
                ctx.pathParameters = matches[0].pathParams;
            }
            ctx.queryParameters = queryParams;

            const noop = function () { };
            let _before: Function, _serve: Function, _catch: Function, _finally: Function;
            _before = resourceHandler.before || noop;
            _serve = resourceHandler.handler || resourceHandler.serve || noop;
            _catch = resourceHandler.catch || catchErrorHandler.bind(this, {
                path: resourcePath,
                method: method.toUpperCase(),
                contentType: contentTypeHeader.join(','),
                accepts: acceptsHeader.join(',')
            });
            _finally = resourceHandler.finally || noop;

            const callbackArgs: any[] = [ctx, request, response, resourceHandler, this];

            try {
                logger.trace('Before serving request for Resource[{}], Method[{}], Content-Type[{}], Accept[{}]', resourcePath, method.toUpperCase(), contentTypeHeader, acceptsHeader);
                _before.apply(this, callbackArgs);

                if (!response.isCommitted()) {
                    logger.trace('Serving request for Resource[{}], Method[{}], Content-Type[{}], Accept[{}]', resourcePath, method.toUpperCase(), contentTypeHeader, acceptsHeader);
                    _serve.apply(this, callbackArgs);
                    logger.trace('Serving request for Resource[{}], Method[{}], Content-Type[{}], Accept[{}] finished', resourcePath, method.toUpperCase(), contentTypeHeader, acceptsHeader);
                }
            } catch (err: any) {
                try {
                    callbackArgs.splice(1, 0, err);
                    _catch.apply(this, callbackArgs);
                } catch (_catchErr) {
                    logger.error('Serving request for Resource[{}], Method[{}], Content-Type[{}], Accept[{}] error handler threw error', _catchErr);
                    throw _catchErr;
                }
            } finally {
                HttpController.prototype.closeResponse.call(this);
                try {
                    _finally.apply(this, []);
                } catch (_finallyErr) {
                    logger.error('Serving request for Resource[{}], Method[{}], Content-Type[{}], Accept[{}] post handler threw error', _finallyErr);
                }
            }
        } else {
            logger.error('No suitable resource handler for Resource[{}], Method[{}], Content-Type[{}], Accept[{}] found', resourcePath, method.toUpperCase(), contentTypeHeader, acceptsHeader);
            this.sendError(response.BAD_REQUEST, undefined, 'Bad Request', 'No suitable processor for this request.');
        }
    }

    /**
     * Returns the ResourceMappings instance of this controller.
     */
    mappings(): ResourceMappings {
        return this.resourceMappings;
    };

    /**
     * Sends an error response to the client, formatted based on the accepted media type.
     */
    sendError(httpErrorCode: number, applicationErrorCode: any, errorName: string, errorDetails: string): void {
        const clientAcceptMediaTypes: string[] = normalizeMediaTypeHeaderValue(request.getHeader('Accept')) || ['application/json'];
        const isHtml: boolean = clientAcceptMediaTypes.some((acceptMediaType) => isMimeTypeCompatible('*/html', acceptMediaType));

        response.setStatus(httpErrorCode || response.INTERNAL_SERVER_ERROR);

        if (isHtml) {
            const message: string = errorName + (applicationErrorCode !== undefined ? '[' + applicationErrorCode + ']' : '') + (errorDetails ? ': ' + errorDetails : '');
            response.sendError(httpErrorCode || response.INTERNAL_SERVER_ERROR, message);
        } else {
            const body = {
                "code": applicationErrorCode,
                "error": errorName,
                "details": errorDetails
            };
            response.setHeader("Content-Type", "application/json");
            response.print(JSON.stringify(body, null, 2));
        }
        this.closeResponse();
    };

    /**
     * Flushes and closes the HTTP response stream.
     */
    closeResponse(): void {
        response.flush();
        response.close();
    };
}

/**
 * Custom sort function for matched route definitions, preferring exact matches over those with placeholders.
 */
function matchedRouteDefinitionsSorter(p: any, n: any): number {
    p.w = calculateMatchedRouteWeight(p);
    n.w = calculateMatchedRouteWeight(n);

    if (n.w === p.w) {
        // The one with less placeholders wins
        const m1 = p.d.match(/{(.*?)}/g);
        const placeholdersCount1 = m1 !== null ? m1.length : 0;
        const m2 = n.d.match(/{(.*?)}/g);
        const placeholdersCount2 = m2 !== null ? m2.length : 0;
        if (placeholdersCount1 > placeholdersCount2) {
            n.w = n.w + 1;
        } else if (placeholdersCount1 < placeholdersCount2) {
            p.w = p.w + 1;
        }
    }
    return n.w - p.w;
}

/**
 * Calculates the initial weight of a matched route definition.
 */
function calculateMatchedRouteWeight(matchedRoute: any): number {
    return (matchedRoute.params && Object.keys(matchedRoute.params).length > 0) ? 0 : 1; // always prefer exact route definitions - set weight to 1
}

/**
 * Transforms path parameters declared in braces (e.g., '/api/{pathParam}') to path-to-regexp format (e.g., '/api/:pathParam').
 */
function transformPathParamsDeclaredInBraces(pathDefinition: string): string {
    const pathParamsInBracesMatcher = /({(\w*\*?)})/g; // matches cases like '/api/{pathParam}' or '/api/{pathParam*}'
    return pathDefinition.replace(pathParamsInBracesMatcher, ":$2"); // transforms matched cases to '/api/:pathParam' or '/api/:pathParam*'
}

/**
 * Finds all routes in the configuration that match the request path and method.
 */
function matchRequestUrl(requestPath: string, method: string, cfg: any): any[] {
    return Object.entries(cfg)
        .filter(([_, handlers]) => handlers && (handlers as any)[method])
        .map(([path, _]) => path)
        .reduce((matches: any[], path: string) => matchingRouteDefinitionsReducer(matches, path, requestPath), [])
        .sort(matchedRouteDefinitionsSorter);
}

/**
 * Reducer function to attempt matching a defined path against the request path using path-to-regexp.
 */
function matchingRouteDefinitionsReducer(matchedDefinitions: any[], definedPath: string, requestPath: string): any[] {
    // 'match' from path-to-regexp is used here
    const pathMatcher = match(transformPathParamsDeclaredInBraces(definedPath));
    const matched = pathMatcher(requestPath);

    if (matched) {
        // Ensure pathParams is an object of key/value pairs
        const pathParams = matched.params;

        const matchedDefinition = {
            p: requestPath,
            d: definedPath,
            pathParams: pathParams
        };
        matchedDefinitions.push(matchedDefinition);
    }
    return matchedDefinitions;
}

/**
 * Normalizes an HTTP media type header value (e.g., "text/plain; q=0.9, application/json").
 */
function normalizeMediaTypeHeaderValue(sMediaType: string | undefined | null): string[] | undefined {
    if (sMediaType === undefined || sMediaType === null)
        return;
    // convert to array of individual types
    let arrMediaType = sMediaType.split(',');
    arrMediaType = arrMediaType.map((mimeTypeEntry) => {
        // remove escaping, remove quality or other attributes (e.g., '; q=0.9')
        return mimeTypeEntry.replace('\\', '').split(';')[0].trim();
    });
    return arrMediaType.filter(type => type.length > 0);
}

/**
 * Checks if a source MIME type is compatible with a target MIME type, supporting wildcards (*).
 */
function isMimeTypeCompatible(source: string, target: string): boolean {
    if (source === target)
        return true;

    const targetM = target.split('/');
    const sourceM = source.split('/');

    // Target is wildcard type, Source has a specific subtype (e.g., target=*/json, source=application/json)
    if (targetM[0] === '*' && targetM[1] === sourceM[1])
        return true;
    // Source is wildcard type, Target has a specific subtype (e.g., source=*/json, target=application/json)
    if (sourceM[0] === '*' && targetM[1] === sourceM[1])
        return true;

    // Target is wildcard subtype, Source has a specific type (e.g., target=application/*, source=application/json)
    if (targetM[1] === '*' && targetM[0] === sourceM[0])
        return true;
    // Source is wildcard subtype, Target has a specific type (e.g., source=application/*, target=application/json)
    if (sourceM[1] === '*' && targetM[0] === sourceM[0])
        return true;

    return false;
}

/**
 * Default error handler function.
 */
const catchErrorHandler = function (this: HttpController, logctx: any, ctx: RequestContext, err: any, request: any, response: any): void {
    if (ctx.suppressStack) {
        const detailsMsg = (ctx.errorName || "") + (ctx.errorCode ? " [" + ctx.errorCode + "]" : "") + (ctx.errorMessage ? ": " + ctx.errorMessage : "");
        logger.info('Serving resource[{}], Verb[{}], Content-Type[{}], Accept[{}] finished in error. {}', logctx.path, logctx.method, logctx.contentType, logctx.accepts, detailsMsg);
    } else {
        logger.error('Serving resource[{}], Verb[{}], Content-Type[{}], Accept[{}] finished in error', logctx.path, logctx.method, logctx.contentType, logctx.accepts, err);
    }

    const httpErrorCode = ctx.httpErrorCode || response.INTERNAL_SERVER_ERROR;
    const errorMessage = ctx.errorMessage || (err && err.message);
    const errorName = ctx.errorName || (err && err.name);
    const errorCode = ctx.errorCode;

    this.sendError(httpErrorCode, errorCode, errorName, errorMessage);
};

/**
 * Checks if the request media types match the resource handler's consumes and produces constraints.
 */
const matchMediaType = function (request: any, producesMediaTypes: string[], consumesMediaTypes: string[]): boolean {
    let isProduceMatched = false;
    const acceptsMediaTypes = normalizeMediaTypeHeaderValue(request.getHeader('Accept'));

    // 1. Check Produces (Accepts header)
    if (!acceptsMediaTypes || acceptsMediaTypes.length === 0 || acceptsMediaTypes.includes('*/*')) {
        // Output media type is not restricted by client or client accepts anything
        isProduceMatched = true;
    } else {
        if (producesMediaTypes && producesMediaTypes.length) {
            const matchedProducesMIME = acceptsMediaTypes.filter((acceptsMediaType) => {
                return producesMediaTypes.some((producesMediaType) => {
                    return isMimeTypeCompatible(acceptsMediaType, producesMediaType);
                });
            });
            isProduceMatched = matchedProducesMIME && matchedProducesMIME.length > 0;
        } else {
            // Resource doesn't specify produces, so it matches if the client accepts anything or if produces is an empty array
            isProduceMatched = true;
        }
    }

    // 2. Check Consumes (Content-Type header)
    let isConsumeMatched = false;
    const contentTypeMediaTypes = normalizeMediaTypeHeaderValue(request.getContentType());

    if (!consumesMediaTypes || consumesMediaTypes.length === 0 || consumesMediaTypes.includes('*/*')) {
        // Input media type is not restricted by resource or resource accepts anything
        isConsumeMatched = true;
    } else {
        if (contentTypeMediaTypes && consumesMediaTypes && consumesMediaTypes.length) {
            const matchedConsumesMIME = contentTypeMediaTypes.filter((contentTypeMediaType) => {
                return consumesMediaTypes.some((consumesMediaType) => {
                    return isMimeTypeCompatible(contentTypeMediaType, consumesMediaType);
                });
            });
            isConsumeMatched = matchedConsumesMIME && matchedConsumesMIME.length > 0;
        }
    }

    return isProduceMatched && isConsumeMatched;
};