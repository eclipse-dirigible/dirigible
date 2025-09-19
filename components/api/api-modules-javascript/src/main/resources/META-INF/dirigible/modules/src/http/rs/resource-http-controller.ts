import { Response as response } from "../response";
import { Request as request } from "../request";
import { ResourceMappings } from "./resource-mappings";
import { Logging } from "sdk/log";

const { match } = dirigibleRequire("modules/src/http/path-to-regexp/6.2.1/index.js");

const logger = Logging.getLogger('http.rs.controller');

function getRequest() {
    return request;
}
function getResponse() {
    return response;
}

/**
 * Creates a service, optionally initialized with oMappings
 *
 * @param {Object|ResourceMappings} [oMappings] configuration object or configuration builder with configuration() getter function
 *
 */
export function service(oConfig?) {
    let config;
    if (oConfig !== undefined) {
        if (typeof oConfig === 'object' || oConfig instanceof ResourceMappings) {
            config = oConfig;
        }
    }
    return new HttpController(config);
}

export class HttpController {

    resource: any;
    resourcePath: any;
    resourceMappings: any;

    /**
 * Constructor function for HttpController instances.
 *
 * @class
 * @param {ResourceMappings|Object} [oMappings] the mappings configuration for this controller.
 *
 */
    constructor(oMappings) {
        if (oMappings instanceof ResourceMappings) {
            this.resourceMappings = oMappings;
        } else if (typeof oMappings === 'object' || 'undefined') {
            this.resourceMappings = new ResourceMappings(oMappings, this);
        }

        this.resource = this.resourcePath = this.resourceMappings.resourcePath.bind(this.resourceMappings);

        //weave-in HTTP method-based factory functions - shortcut for service().resource(sPath).method
        ['get', 'post', 'put', 'delete', 'remove', 'method'].forEach((sMethodName) => {
            this[sMethodName] = (sPath, sVerb, arrConsumes, arrProduces, ...args) => {
                const allArguments = [sPath, sVerb, arrConsumes, arrProduces, ...args];
                if (allArguments.length < 1)
                    throw Error('Insufficient arguments provided to HttpController method ' + sMethodName + '.');
                if (sPath === undefined)
                    sPath = "";
                const resource = this.resourceMappings.find(sPath, sVerb, arrConsumes, arrProduces) || this.resourceMappings.resource(sPath);
                resource[sMethodName]['apply'](resource, Array.prototype.slice.call(allArguments, 1));
                return this;
            };
        });

    }

    listen(request, response) {
        return this.execute(request, response);
    }

    execute(request?, response?) {
        request = request || getRequest();
        const requestPath = request.getResourcePath();
        const method = request.getMethod().toLowerCase();
        const _oConfiguration = this.resourceMappings.configuration();

        const matches: any[] = matchRequestUrl(requestPath, method, _oConfiguration);
        let resourceHandler;
        if (matches && matches[0]) {
            const verbHandlers = _oConfiguration[matches[0].d][method];
            if (verbHandlers) {
                resourceHandler = verbHandlers.filter((handlerDef) => {
                    return matchMediaType(request, handlerDef.produces, handlerDef.consumes);
                })[0];
            }
        }

        response = response || getResponse();
        const queryParams = request.getQueryParametersMap() || {};
        const acceptsHeader = normalizeMediaTypeHeaderValue(request.getHeader('Accept')) || '[]';
        const contentTypeHeader = normalizeMediaTypeHeaderValue(request.getHeader('Content-Type')) || '[]';
        const resourcePath = requestPath;

        if (resourceHandler) {
            const ctx = {
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

            const noop = function () {
            };
            let _before, _serve, _catch, _finally;
            _before = resourceHandler.before || noop;
            _serve = resourceHandler.handler || resourceHandler.serve || noop;
            _catch = resourceHandler.catch || catchErrorHandler.bind(this, {
                path: resourcePath,
                method: method.toUpperCase(),
                contentType: contentTypeHeader,
                accepts: acceptsHeader
            });
            _finally = resourceHandler.finally || noop;
            const callbackArgs = [ctx, request, response, resourceHandler, this];
            try {
                logger.trace('Before serving request for Resource[{}], Method[{}], Content-Type[{}], Accept[{}]', resourcePath, method.toUpperCase(), contentTypeHeader, acceptsHeader);
                _before.apply(this, callbackArgs);
                if (!response.isCommitted()) {
                    logger.trace('Serving request for Resource[{}], Method[{}], Content-Type[{}], Accept[{}]', resourcePath, method.toUpperCase(), contentTypeHeader, acceptsHeader);
                    _serve.apply(this, callbackArgs);
                    logger.trace('Serving request for Resource[{}], Method[{}], Content-Type[{}], Accept[{}] finished', resourcePath, method.toUpperCase(), contentTypeHeader, acceptsHeader);
                }
            } catch (err) {
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

    mappings() {
        return this.resourceMappings;
    };

    sendError(httpErrorCode, applicationErrorCode, errorName, errorDetails) {
        const clientAcceptMediaTypes = normalizeMediaTypeHeaderValue(request.getHeader('Accept')) || ['application/json'];
        const isHtml = clientAcceptMediaTypes.some((acceptMediaType) => isMimeTypeCompatible('*/html', acceptMediaType));
        response.setStatus(httpErrorCode || response.INTERNAL_SERVER_ERROR);
        if (isHtml) {
            const message = errorName + (applicationErrorCode !== undefined ? '[' + applicationErrorCode + ']' : '') + (errorDetails ? ': ' + errorDetails : '');
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

    closeResponse() {
        response.flush();
        response.close();
    };
}

function matchedRouteDefinitionsSorter(p, n) {
    p.w = calculateMatchedRouteWeight(p);
    n.w = calculateMatchedRouteWeight(n);

    if (n.w === p.w) {
        //the one with less placeholders wins
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

function calculateMatchedRouteWeight(matchedRoute) {
    return (matchedRoute.params && matchedRoute.params.length > 0) ? 0 : 1; // always prefer exact route definitions - set weight to 1
}

function transformPathParamsDeclaredInBraces(pathDefinition) {
    const pathParamsInBracesMatcher = /({(\w*\*?)})/g; // matches cases like '/api/{pathParam}' or '/api/{pathParam*}'
    return pathDefinition.replace(pathParamsInBracesMatcher, ":$2"); // transforms matched cases to '/api/:pathParam' or '/api/:pathParam*'
}

function matchRequestUrl(requestPath, method, cfg) {
    return Object.entries(cfg)
        .filter(([_, handlers]) => handlers && handlers[method])
        .map(([path, _]) => path)
        .reduce((matches, path) => matchingRouteDefinitionsReducer(matches, path, requestPath), [])
        .sort(matchedRouteDefinitionsSorter);
}

function matchingRouteDefinitionsReducer(matchedDefinitions, definedPath, requestPath) {
    const matches = match(transformPathParamsDeclaredInBraces(definedPath));
    const matched = matches(requestPath);
    if (matched) {
        const matchedDefinition = {
            p: requestPath,
            d: definedPath,
            pathParams: Array.isArray(matched.params) ? matched.params.join("/") : matched.params
        };
        matchedDefinitions.push(matchedDefinition);
    }
    return matchedDefinitions;
}

function normalizeMediaTypeHeaderValue(sMediaType) {
    if (sMediaType === undefined || sMediaType === null)
        return;
    sMediaType = sMediaType.split(',');//convert to array
    sMediaType = sMediaType.map((mimeTypeEntry) => {
        return mimeTypeEntry.replace('\\', '').split(';')[0].trim();//remove escaping, remove quality or other atributes
    });
    return sMediaType;
};

function isMimeTypeCompatible(source, target) {
    if (source === target)
        return true;
    const targetM = target.split('/');
    const sourceM = source.split('/');
    if ((targetM[0] === '*' && targetM[1] === sourceM[1]) || (source[0] === '*' && targetM[1] === sourceM[1]))
        return true;
    if ((targetM[1] === '*' && targetM[0] === sourceM[0]) || (sourceM[1] === '*' && targetM[0] === sourceM[0]))
        return true;
};

const catchErrorHandler = function (logctx, ctx, err, request, response) {
    if (ctx.suppressStack) {
        const detailsMsg = (ctx.errorName || "") + (ctx.errorCode ? " [" + ctx.errorCode + "]" : "") + (ctx.errorMessage ? ": " + ctx.errorMessage : "");
        logger.info('Serving resource[{}], Verb[{}], Content-Type[{}], Accept[{}] finished in error. {}', logctx.path, logctx.method, logctx.contentType, logctx.accepts, detailsMsg);
    } else
        logger.error('Serving resource[{}], Verb[{}], Content-Type[{}], Accept[{}] finished in error', logctx.path, logctx.method, logctx.contentType, logctx.accepts, err);

    const httpErrorCode = ctx.httpErrorCode || response.INTERNAL_SERVER_ERROR;
    const errorMessage = ctx.errorMessage || (err && err.message);
    const errorName = ctx.errorName || (err && err.name);
    const errorCode = ctx.errorCode;
    this.sendError(httpErrorCode, errorCode, errorName, errorMessage);
};

//find MIME types intersections
const matchMediaType = function (request, producesMediaTypes, consumesMediaTypes) {
    let isProduceMatched = false;
    const acceptsMediaTypes = normalizeMediaTypeHeaderValue(request.getHeader('Accept'));
    if (!acceptsMediaTypes || acceptsMediaTypes.length === 0 || acceptsMediaTypes.indexOf('*/*') > -1) { //output media type is not restricted
        isProduceMatched = true;
    } else {
        let matchedProducesMIME;
        if (producesMediaTypes && producesMediaTypes.length) {
            matchedProducesMIME = acceptsMediaTypes.filter((acceptsMediaType) => {
                return producesMediaTypes.filter((producesMediaType) => {
                    return isMimeTypeCompatible(acceptsMediaType, producesMediaType)
                }).length > 0;
            });
            isProduceMatched = matchedProducesMIME && matchedProducesMIME.length > 0;
        }
    }

    let isConsumeMatched = false;
    const contentTypeMediaTypes = normalizeMediaTypeHeaderValue(request.getContentType());
    if (!consumesMediaTypes || consumesMediaTypes.length === 0 || consumesMediaTypes.indexOf('*/*') > -1) { //input media type is not restricted
        isConsumeMatched = true;
    } else {
        let matchedConsumesMIME;
        if (contentTypeMediaTypes && consumesMediaTypes && consumesMediaTypes.length) {
            matchedConsumesMIME = contentTypeMediaTypes.filter((contentTypeMediaType) => {
                return consumesMediaTypes.filter((consumesMediaType) => {
                    return isMimeTypeCompatible(contentTypeMediaType, consumesMediaType);
                }).length > 0;
            });
            isConsumeMatched = matchedConsumesMIME && matchedConsumesMIME.length > 0;
        }
    }
    return isProduceMatched && isConsumeMatched;
};
