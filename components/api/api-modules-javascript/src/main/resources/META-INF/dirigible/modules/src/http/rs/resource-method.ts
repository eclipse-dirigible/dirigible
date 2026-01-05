import { handlerFunction } from "./resource-common";

/**
 * Interface for the internal configuration object of a ResourceMethod.
 */
interface ResourceMethodConfig {
    consumes?: string[];
    produces?: string[];
    before?: Function;
    serve?: Function; // Could also be named 'handler'
    catch?: Function;
    finally?: Function;
    // Add other relevant configuration properties as they become known
    [key: string]: any;
}

/**
 * Constructor function for ResourceMethod instances.
 * This class handles the fluent configuration for a single HTTP method handler (e.g., GET)
 * attached to a Resource.
 * 
 * /**
 * Constructor function for ResourceMethod instances.
 * All parameters of the function are optional.
 *
 * Providing oConfiguration will initialize this instance with some initial configuration instead of starting
 * entirely from scratch. Note that the configuration object schema must be compliant with the one produced by
 * the ResourceMethod itself. If this parameter is omited, setup will start from scratch.
 *
 * Provisioning controller, will inject a reference to the execute method of the controller so that it can be
 * fluently invoked in the scope of this ResourceMehtod instance as part of the method chaining flow. The execute
 * function scope is bound to the controller instance for this ResourceMethod.
 *
 * @example
 * ```js
 * rs.service()
 *  .resource('')
 * 		.get()
 * 	.execute();
 * ```
 *
 * Provisioning resource, will inject a reference ot the HTTP method functions of the Resource class (get, post,
 * put, delete, remove, method) so that they can be fluently invoked in the scope of this ResourceMethod instance
 * as part of the method chaining flow. The functions are bound to the resource instance for this ResourceMethod.
 *
 * @example
 * ```js
 * rs.service()
 *  .resource('')
 * 		.get(function(){})
 * 		.post(function(){})
 * 		.put(function(){})
 * 		.remove(function(){})
 * .execute();
 * ```
 *
 * Provisioning mappings, will inject a reference ot the resource method of the ResourceMappings class so that
 * it can be fluently invoked in the scope of this ResourceMethod instance as part of the method chaining flow.
 * The function is bound to the mappings instance for this ResourceMethod.
 *
 * @example
 * ```js
 * rs.service()
 *  .resource('')
 * 		.get(function(){})
 * 	.resource('{id}')
 * 		.get(function(){})
 * .execute();
 * ``
 * 
 */
export class ResourceMethod {
    cfg: ResourceMethodConfig;
    _resource: any; // Reference to the parent Resource instance
    controller: any; // Reference to the HttpController instance

    // Fluent API aliases/delegates bound to ResourceMappings/Resource
    resource: Function;
    resourcePath: Function;
    path: Function;

    /**
     * @param oConfiguration Initial configuration object.
     * @param controller The HttpController instance.
     * @param resource The parent Resource instance.
     * @param mappings The parent ResourceMappings instance.
     * @returns {ResourceMethod}
     */
    constructor(oConfiguration: any, controller: any, resource: any, mappings: any) {
        this.cfg = oConfiguration || {};
        this._resource = resource;
        this.controller = controller;

        if (mappings) {
            // Bind the resource/path methods from ResourceMappings for method chaining
            this.resource = mappings.resource.bind(mappings);
            this.resourcePath = this.path = this.resource; // aliases
        }
    }

    /**
     * Delegates to the HttpController's execute function to process the request.
     */
    execute(): void {
        // Use optional chaining as controller might not be available in all setups
        this.controller?.execute?.(...arguments);
    }

    // --- Delegation to Resource HTTP Methods for Chaining ---

    /**
     * Delegates to the parent Resource's 'get' method.
     */
    get(): any {
        return this._resource?.["get"]?.(...arguments);
    }

    /**
     * Delegates to the parent Resource's 'post' method.
     */
    post(): any {
        return this._resource?.["post"]?.(...arguments);
    }

    /**
     * Delegates to the parent Resource's 'put' method.
     */
    put(): any {
        return this._resource?.["put"]?.(...arguments);
    }

    /**
     * Delegates to the parent Resource's 'delete' method.
     */
    delete(): any {
        return this._resource?.["delete"]?.(...arguments);
    }

    /**
     * Delegates to the parent Resource's 'remove' method.
     */
    remove(): any {
        return this._resource?.["remove"]?.(...arguments);
    }

    /**
     * Delegates to the parent Resource's 'method' method.
     */
    method(): any {
        return this._resource?.["method"]?.(...arguments);
    }

    /**
     * Returns the configuration object for this ResourceMethod instance.
     *
     * @returns The configuration object.
     */
    configuration(): ResourceMethodConfig {
        return this.cfg;
    };

    // --- Handler Definition Methods ---

    /**
     * Applies a callback function for the **before** phase of processing a matched resource request.
     *
     * @param fHandler Callback function for the before phase.
     * @returns The ResourceMethod instance for method chaining.
     */
    before(fHandler: Function): ResourceMethod {
        return handlerFunction(this, this.configuration(), 'before', fHandler);
    };

    /**
     * Applies a callback function for processing a matched resource request (**serve** phase).
     *
     * @param fHandler Callback function for the serve phase.
     * @returns The ResourceMethod instance for method chaining.
     */
    serve(fHandler: Function): ResourceMethod {
        return handlerFunction(this, this.configuration(), 'serve', fHandler);
    };

    /**
     * Applies a callback function for the **catch** errors phase of processing a matched resource request.
     *
     * @param fHandler Callback function for the catch phase.
     * @returns The ResourceMethod instance for method chaining.
     */
    catch(fHandler: Function): ResourceMethod {
        return handlerFunction(this, this.configuration(), 'catch', fHandler);
    };

    /**
     * Applies a callback function for the **finally** phase of processing a matched resource request.
     *
     * @param fHandler Callback function for the finally phase.
     * @returns The ResourceMethod instance for method chaining.
     */
    finally(fHandler: Function): ResourceMethod {
        return handlerFunction(this, this.configuration(), 'finally', fHandler);
    };

    // --- MIME Type Configuration Methods ---

    /**
     * Defines the content MIME type(s), which this ResourceMethod expects as input (**consumes**).
     *
     * @param mimeTypes Sets the mime types that this ResourceMethod is capable to consume.
     * @returns The ResourceMethod instance for method chaining.
     */
    consumes(mimeTypes: string | string[]): ResourceMethod {
        return this.mimeSetting('consumes', mimeTypes);
    };

    /**
	 * Defines the HTTP response payload MIME type(s), which this ResourceMethod request processing function outputs, i.e.
	 * those that it 'produces'. At runtime, the Accept request header will be matched for compatibility with this setting
	 * to elicit request processing functions.
	 * Note that the matching is performed by compatibility, not strict equality, i.e. the MIME type format wildcards are
	 * considered too. For example, a request Accept header "*\/json" will match a produces setting "application\/json".
	 * 
     * @example
     * ```js
     * rs.service()
     *	.resource("")
     * 		.get(function(){})
     * 			.produces(["application\/json"])
     * .execute();
     * 	.
     * ```
     *
     * Take care to make sure that the produces constraint correctly describes the response contenty MIME types that the request
     * processing function can produce so that only client request that can accept them land there.
     *
     * A note about method argument multiplicity (string vs array of strings).
     * One of the arguments of the produce method will translate to the response Content-Type property, which is known to be a
     * single value header by [specification](https://tools.ietf.org/html/rfc7231#section-3.1.1.5). There are two reasons why
     * the method accepts array and not a single value only:
     *
     * 1. Normally, when matched, content types are evaluated for semantic compatibility and not strict equality on both sides
     *  - client and server. Providing a range of compatible MIME types instead of single value, increases the range of acceptable
     * requests for procesing, while reducing the stricness of the requirements on the client making the request. For example,
     * declaring ["text/json,"application/json"] as produced types makes requests with any of these accept headers (or a combination
     * of them) acceptable for processing: "*\/json", "text/json", "application/json", "*\/*".
     *
     * 2. Although in most cases a handler function will produce payload in single format (media type), it is quite possible to
     * desgin it also as a controller that produces alternative payload in different formats. In these cases you need produces
     * that declares all supported media types so that the request with a relaxed Accept header matching any of them can land
     * in this function. That makes the routing a bit less transparent and dependent on the client, but may prove valuable for
     * certian cases.
     *
     * In any case it is responsibility of the request processing function to set the correct Content-Type header.
     *
     * @param mimeTypes Sets the mime type(s) that this ResourceMethod may produce.
     * @returns The ResourceMethod instance for method chaining.
     */
    produces(mimeTypes: string | string[]): ResourceMethod {
        return this.mimeSetting('produces', mimeTypes);
    };

    /**
     * Commmon function for initializng the 'consumes' and 'produces' arrays in the ResourceMethod instances.
     * Before finalizing the configuration setup the function will remove duplicates with exact match filtering.
     *
     * @param mimeSettingName must be either 'consumes' or 'produces'.
     * @param mimeTypes An array of strings formatted as mime types (type/subtype) or a single string.
     * @returns The ResourceMethod instance to which the function is bound.
     * @private
     */
    private mimeSetting(mimeSettingName: 'consumes' | 'produces', mimeTypes: string | string[]): ResourceMethod {

        let arrMimeTypes: string[];

        if (typeof mimeTypes === 'string') {
            arrMimeTypes = [mimeTypes];
        } else if (Array.isArray(mimeTypes)) {
            arrMimeTypes = mimeTypes;
        } else {
            throw new Error('Invalid argument: ' + mimeSettingName + ' mime type argument must be valid MIME type string or array of such strings, but instead is ' + (typeof mimeTypes));
        }

        arrMimeTypes.forEach((mimeType) => {
            const mt = mimeType.split('/');
            if (mt.length !== 2 || !mt[0] || !mt[1])
                throw new Error('Invalid argument. Not a valid MIME type format type/subtype: ' + mimeType);
            // Basic format check is sufficient
        });

        if (!this.cfg[mimeSettingName]) {
            this.cfg[mimeSettingName] = [];
        }

        // Deduplicate entries before concatenation
        const existingMimes: string[] = this.cfg[mimeSettingName];
        const newMimeTypes = arrMimeTypes.filter((mimeType) => {
            return existingMimes.indexOf(mimeType) < 0;
        });

        this.cfg[mimeSettingName] = existingMimes.concat(newMimeTypes);

        return this;
    };
}
