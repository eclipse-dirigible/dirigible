import { Resource } from "./resource";

/**
 * The ResourceMappings class abstracts the mappings between resource URL path templates
 * and their corresponding resource handler specifications. It acts as the configuration
 * store for the HttpController.
 */
export class ResourceMappings {
    // Map of resource path (string) to the Resource instance (typed as any for flexibility)
    resources: { [key: string]: any } = {};
    controller: any; // Represents the HttpController instance
    execute: any; // Bound execute function from the HttpController

    /**
     * Constructor function for ResourceMappings instances.
     *
     * @param oConfiguration Configuration object defining initial resource paths and their handlers.
     * @param controller The HttpController instance, for which this ResourceMappings handles configuration.
     */
    constructor(oConfiguration: any, controller: any) {
        if (oConfiguration) {
            Object.keys(oConfiguration).forEach((sPath: string) => {
                this.resources[sPath] = this.resource(sPath, oConfiguration[sPath]);
            });
        }

        if (controller) {
            this.controller = controller;
            this.execute = controller.execute.bind(controller);
        }
    }

    /**
     * Creates or retrieves a Resource object corresponding to the given path.
     * The second, optional argument can be used to initialize the resource.
     *
     * @param sPath The URL path template for the resource (e.g., "users/{id}").
     * @param oConfiguration Optional configuration object for initial resource setup.
     * @returns The created or existing Resource instance.
     */
    path(sPath: string, oConfiguration?: any): Resource {
        if (sPath !== "" && sPath[0] === "/") {
            sPath = sPath.substring(1); // transform "/test" into "test"
        }

        if (this.resources[sPath] === undefined) {
            // Resource is the class imported from "./resource"
            this.resources[sPath] = new Resource(sPath, oConfiguration, this.controller, this);
        }

        return this.resources[sPath];
    };

    /**
     * Alias for path().
     */
    resourcePath(sPath: string, oConfiguration?: any): Resource {
        return this.path(sPath, oConfiguration);
    }

    /**
     * Alias for path().
     */
    resource(sPath: string, oConfiguration?: any): Resource {
        return this.path(sPath, oConfiguration);
    }

    /**
     * Returns the compiled configuration object for all resources managed by this ResourceMappings.
     * The configuration is structured to be consumed by the HttpController's routing logic.
     */
    configuration(): { [key: string]: any } {
        const _cfg: { [key: string]: any } = {};
        Object.keys(this.resources).forEach((sPath: string) => {
            // Assuming each Resource object has a configuration() method
            _cfg[sPath] = this.resources[sPath].configuration();
        });
        return _cfg;
    };

    /**
     * Removes all but GET resource handlers from all managed resources, making them read-only.
     *
     * @returns The ResourceMappings instance for method chaining.
     */
    readonly(): this {
        Object.keys(this.resources).forEach((sPath: string) => {
            this.resources[sPath].readonly();
        });
        return this;
    };

    /**
     * Disables resource handling specifications matching the arguments, effectively removing them from this API.
     *
     * @param sPath The path of the resource.
     * @param sVerb The HTTP verb (e.g., 'get', 'post').
     * @param arrConsumes Array of consumed media types.
     * @param arrProduces Array of produced media types.
     * @returns The ResourceMappings instance for method chaining.
     */
    disable(sPath: string, sVerb: string, arrConsumes: string[], arrProduces: string[]): this {
        const resource = this.resources[sPath];
        // Assuming the intention is to find the single Resource instance and call disable on it
        if (resource && typeof resource.disable === 'function') {
             resource.disable(sVerb, arrConsumes, arrProduces);
        }
        return this;
    };

    /**
     * Provides a reference to a handler specification matching the supplied arguments.
     *
     * @param sPath The path of the resource.
     * @param sVerb The HTTP verb (e.g., 'get', 'post').
     * @param arrConsumes Array of consumed media types.
     * @param arrProduces Array of produced media types.
     * @returns The matching Resource handler specification or undefined.
     */
    find(sPath: string, sVerb: string, arrConsumes: string[], arrProduces: string[]): any | undefined {
        if (this.resources[sPath]) {
            // Assuming Resource instance has a find method
            const hit = this.resources[sPath].find(sVerb, arrConsumes, arrProduces);
            if (hit)
                return hit;
        }
        return;
    };
}