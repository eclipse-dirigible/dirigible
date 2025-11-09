import { handlerFunction } from "./resource-common";
import { ResourceMethod } from "./resource-method";

/**
 * Compares two arrays for equality by inspecting if they are arrays, refer to the same instance,
 * have same length and contain equal components in the same order.
 *
 * @param source The source array to compare to.
 * @param target The target array to compare with.
 * @returns true if the arrays are equal, false otherwise.
 */
function arrayEquals(source: any[] | undefined, target: any[] | undefined): boolean {
	if (source === target)
		return true;
	if (!Array.isArray(source) || !Array.isArray(target))
		return false;
	if (source !== undefined && target === undefined || source === undefined && target !== undefined)
		return false;
	if (source.length !== target.length)
		return false;
	for (let i = 0; i < source.length; i++) {
		if (source[i] !== target[i])
			return false;
	}
	return true;
}

/**
 * Constructs a new Resource instance, initialized with the supplied path parameter and optionally with the second, configuration object parameter.
 */
export class Resource {
    /** The URL path for this resource. */
	sPath: string;
    /** The resource configuration mapping methods to handler specifications. */
	cfg: any;
    /** The optional controller instance. */
	controller: any;
    /** Bound execute function from the controller. */
	execute: Function | undefined;
    /** Additional mappings object. */
	mappings: any;

	/**
     * @param sPath The base URL path for the resource.
     * @param oConfiguration Optional configuration object (map of method handlers).
     * @param controller Optional controller instance containing an execute method.
     * @param mappings Optional object for resource mappings.
     */
	constructor(sPath: string, oConfiguration?: any, controller?: any, mappings?: any) {
		this.sPath = sPath;
		this.cfg = oConfiguration || {};

		if (controller) {
			this.controller = controller;
			// Bind the controller's execute function
			this.execute = controller.execute.bind(controller);
		}
		if (mappings) {
			this.mappings = mappings;
		}
	}

	/**
     * Sets the URL path for this resource, overriding the one specified upon its construction,
     * if a path string is provided as argument ot the method (i.e. acts as setter),
     * or returns the path set for this resource, if the method is invoked without arguments (i.e. acts as getter).
     *
     * @param sPath The path property to be set for this resource.
     * @returns The resource instance for method chaining (setter mode), or the path set for this resource (getter mode).
     */
	path(sPath?: string): Resource | string {
		if (arguments.length === 0)
			return this.sPath;
		this.sPath = sPath!;
		return this;
	}

	/**
	 * Creates a new HTTP method handling specification.
	 *
	 * @param sHttpMethod The HTTP method (method) (e.g., "GET").
	 * @param oConfiguration The handler specification(s) for this HTTP method. Can be a single object or array.
	 * @returns The ResourceMethod instance, or an array of ResourceMethod instances.
	 */
	method(sHttpMethod: string, oConfiguration?: any | any[]): ResourceMethod | ResourceMethod[] {
		if (sHttpMethod === undefined)
			throw new Error('Illegal sHttpMethod argument: ' + sHttpMethod);

		const method = sHttpMethod.toLowerCase();

		if (!this.cfg[method])
			this.cfg[method] = [];

		let arrConfig: any[] = oConfiguration || {};
		if (!Array.isArray(arrConfig)) {
			arrConfig = [arrConfig];
		}

		const handlers: ResourceMethod[] = [];

		arrConfig.forEach((handlerSpec) => {
			// Type casting to handle object property access for consumes/produces
			const consumes = (handlerSpec as { consumes?: string[] }).consumes;
			const produces = (handlerSpec as { produces?: string[] }).produces;

			const _h = this.find(sHttpMethod, consumes, produces);
			if (!_h) {
				// create new
				this.cfg[method].push(handlerSpec);
			} else {
				// update existing spec in cfg
				const existingSpec = this.cfg[method].find(
                    (spec: any) => arrayEquals(spec.consumes, consumes) && arrayEquals(spec.produces, produces)
                );
				if (existingSpec) {
					for (const propName in handlerSpec) {
						if (Object.prototype.hasOwnProperty.call(handlerSpec, propName)) {
							existingSpec[propName] = handlerSpec[propName];
						}
					}
				}
			}

            // Create a new ResourceMethod instance for the (potentially updated) spec
            const finalHandlerSpec = this.cfg[method].find(
                (spec: any) => arrayEquals(spec.consumes, consumes) && arrayEquals(spec.produces, produces)
            );

            if (finalHandlerSpec) {
                handlers.push(new ResourceMethod(finalHandlerSpec, this.controller, this, this.mappings));
            }
		});

		return handlers.length > 1 ? handlers : handlers[0];
	};

	/**
     * Internal utility method to handle the logic for HTTP verb shortcut methods (get, post, etc.).
     *
     * @param sMethodName The HTTP method name (e.g., 'get').
     * @param args The arguments passed to the shortcut method.
     * @returns The ResourceMethod instance or an array of instances.
     */
	private buildMethod(sMethodName: string, ...args: (Function | any)[]): ResourceMethod | ResourceMethod[] {
		if (args.length > 0) {
			if (typeof args[0] === 'function') {
				// .get(function) -> .method('get').serve(function)
                // Note: We need to cast the result of method() to access serve() as it returns a ResourceMethod.
				return (this.method(sMethodName) as ResourceMethod).serve(args[0]);
			} else if (typeof args[0] === 'object' && args[0] !== null) {
				// .get({config}) -> .method('get', {config})
				return this.method(sMethodName, args[0]);
			} else {
				throw new Error(`Invalid argument: Resource.${sMethodName} method first argument must be valid javascript function or configuration object, but instead is ${typeof args[0]} ${args[0]}`);
			}
		} else {
			// .get() -> .method('get')
			return this.method(sMethodName) as ResourceMethod;
		}
	};

	/**
	 * Creates a handling specification for the HTTP method "GET".
	 * @param fServeCb|oConfiguration Serve function callback or configuration object to initialize the method.
	 * @returns The ResourceMethod instance or array.
	 */
	get(fServeCb: Function): ResourceMethod;
    get(oConfiguration: any): ResourceMethod | ResourceMethod[];
    get(): ResourceMethod;
	get(): ResourceMethod | ResourceMethod[] {
		return this.buildMethod('get', ...arguments);
	};

	/**
	 * Creates a handling specification for the HTTP method "POST".
	 * @param fServeCb|oConfiguration Serve function callback or configuration object to initialize the method.
	 * @returns The ResourceMethod instance or array.
	 */
	post(fServeCb: Function): ResourceMethod;
    post(oConfiguration: any): ResourceMethod | ResourceMethod[];
    post(): ResourceMethod;
	post(): ResourceMethod | ResourceMethod[] {
		return this.buildMethod('post', ...arguments);
	};

	/**
	 * Creates a handling specification for the HTTP method "PUT".
	 * @param fServeCb|oConfiguration Serve function callback or configuration object to initialize the method.
	 * @returns The ResourceMethod instance or array.
	 */
	put(fServeCb: Function): ResourceMethod;
    put(oConfiguration: any): ResourceMethod | ResourceMethod[];
    put(): ResourceMethod;
	put(): ResourceMethod | ResourceMethod[] {
		return this.buildMethod('put', ...arguments);
	};

	/**
	 * Creates a handling specification for the HTTP method "DELETE".
	 * @param fServeCb|oConfiguration Serve function callback or configuration object to initialize the method.
	 * @returns The ResourceMethod instance or array.
	 */
	delete(fServeCb: Function): ResourceMethod;
    delete(oConfiguration: any): ResourceMethod | ResourceMethod[];
    delete(): ResourceMethod;
	delete(): ResourceMethod | ResourceMethod[] {
		return this.buildMethod('delete', ...arguments);
	};

    /**
	 * Creates a handling specification for the HTTP method "DELETE" (alias for delete()).
	 * @param fServeCb|oConfiguration Serve function callback or configuration object to initialize the method.
	 * @returns The ResourceMethod instance or array.
	 */
    remove(fServeCb: Function): ResourceMethod;
    remove(oConfiguration: any): ResourceMethod | ResourceMethod[];
    remove(): ResourceMethod;
	remove(): ResourceMethod | ResourceMethod[] {
		return this.buildMethod('delete', ...arguments);
	}

	/**
	 * Finds a ResourceMethod with the given constraints.
	 *
	 * @param sVerb The name of the method property of the ResourceMethod in search (e.g., "GET").
	 * @param arrConsumesMimeTypeStrings The consumes constraint property of the ResourceMethod in search.
	 * @param arrProducesMimeTypeStrings The produces constraint property of the ResourceMethod in search.
	 * @returns The found ResourceMethod instance, or undefined if not found.
	 */
	find(sVerb: string, arrConsumesMimeTypeStrings?: string[], arrProducesMimeTypeStrings?: string[]): ResourceMethod | undefined {
		let hit: ResourceMethod | undefined;
		const sVerbLower = sVerb.toLowerCase();

		Object.keys(this.cfg)
            .filter((sVerbName) => sVerbName === sVerbLower)
            .forEach((sVerbName) => {
			    this.cfg[sVerbName].forEach((verbHandlerSpec: any) => {
				    if (arrayEquals(verbHandlerSpec.consumes, arrConsumesMimeTypeStrings) && arrayEquals(verbHandlerSpec.produces, arrProducesMimeTypeStrings)) {
					    hit = new ResourceMethod(verbHandlerSpec, this.controller, this, this.mappings);
					    return;
				    }
			    });
			    if (hit)
				    return;
		});
		return hit;
	};

	/**
	 * Returns the configuration of this resource.
	 *
	 * @returns The resource configuration object.
	 */
	configuration(): any {
		return this.cfg;
	};

	/**
	 * Instructs redirection of the request base don the parameter. If it is a stirng representing URI, the request will be
	 * redirected to this URI for any method. If it's a function it will be invoked and epxected to return a URI string to redirect to.
	 *
	 * @param fRedirector The function or string URI to redirect to.
     * @returns The resource instance for method chaining.
	 */
	redirect(fRedirector: string | Function): Resource {
		if (typeof fRedirector === 'string') {
			const redirectUri = fRedirector;
			fRedirector = function () {
				return redirectUri;
			}
		}
		// The imported handlerFunction is used here
		return handlerFunction(this, this.configuration(), 'redirect', fRedirector);
	};

	/**
	 * Disables the ResourceMethods that match the given constraints
     *
     * @param sVerb The HTTP verb (e.g., "GET").
	 * @param arrConsumesTypeStrings The consumes constraint property of the ResourceMethod in search.
	 * @param arrProducesTypeStrings The produces constraint property of the ResourceMethod in search.
     * @returns The resource instance for method chaining.
	 */
	disable(sVerb?: string, arrConsumesTypeStrings?: string[], arrProducesTypeStrings?: string[]): Resource {
		Object.keys(this.cfg)
            .filter((sVerbName) => sVerb === undefined || (sVerb && sVerb.toLowerCase() === sVerbName))
            .forEach((sVerbName) => {
			    // Use a reverse loop for safe splicing
			    for (let i = this.cfg[sVerbName].length - 1; i >= 0; i--) {
                    const verbHandlerSpec = this.cfg[sVerbName][i];
				    if (arrayEquals(verbHandlerSpec.consumes, arrConsumesTypeStrings) && arrayEquals(verbHandlerSpec.produces, arrProducesTypeStrings)) {
					    this.cfg[sVerbName].splice(i, 1);
                    }
                }
		});
		return this;
	};

	/**
	 * Disables all but 'read' HTTP methods in this resource (GET, HEAD, TRACE).
     *
     * @returns The resource instance for method chaining.
	 */
	readonly(): Resource {
		Object.keys(this.cfg).forEach((method) => {
			if (!['get', 'head', 'trace'].includes(method)) {
				delete this.cfg[method];
            }
		});
		return this;
	};
}