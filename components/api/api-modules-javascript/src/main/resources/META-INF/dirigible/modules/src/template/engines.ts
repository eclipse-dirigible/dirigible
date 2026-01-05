import { Repository } from "@aerokit/sdk/platform/repository";

const REGISTRY_PUBLIC = "/registry/public/";
const MUSTACHE_FILE_EXTENSION = ".mustache";

const TemplateEnginesFacade = Java.type("org.eclipse.dirigible.components.api.templates.TemplateEnginesFacade");

/**
 * An internal wrapper class that adapts a native template engine implementation.
 * It manages the engine instance and optional custom start/end markers.
 */
class TemplateEngine {

    private engine: any;
    /** The start marker string (e.g., '{{' for Mustache). */
    private sm: string | null;
    /** The end marker string (e.g., '}}' for Mustache). */
    private em: string | null;

    /**
     * Creates an instance of TemplateEngine.
     *
     * @param engine The native template engine object from the Facade.
     * @param type The type of the engine ('mustache', 'velocity', 'javascript').
     */
    constructor(engine: any, type: string) {
        this.engine = engine;
        // Default markers for Mustache engine
        this.sm = type === "mustache" ? "{{" : null;
        this.em = type === "mustache" ? "}}" : null;
    }

    /**
     * Generates the final output by executing the template with the provided parameters.
     * Note: Parameters are internally serialized to JSON before being passed to the native engine.
     *
     * @param location A string identifying the template (used for error reporting/caching, often a file path).
     * @param template The raw template string content to process.
     * @param parameters An object containing the context data to be used in the template.
     * @returns The processed output string.
     */
    public generate(location: string, template: string, parameters: { [key: string]: any }): string {
        return this.engine.generate(location, template, JSON.stringify(parameters), this.sm, this.em);
    }

    /**
     * Sets a custom start marker for the template engine. This is primarily useful for Mustache.
     *
     * @param sm The new start marker string.
     */
    public setSm(sm: any) {
        this.sm = sm;
    }

    /**
     * Sets a custom end marker for the template engine. This is primarily useful for Mustache.
     *
     * @param em The new end marker string.
     */
    public setEm(em: any) {
        this.em = em;
    }
}

/**
 * Provides access to various server-side template engines (Velocity, Mustache, JavaScript).
 * It offers utility methods for generating content from templates directly or from files
 * stored in the registry.
 */
export class TemplateEngines {

    /**
     * Retrieves the default template engine, which is currently the Velocity engine.
     *
     * @returns The default template engine instance.
     */
    public static getDefaultEngine(): TemplateEngine {
        return this.getVelocityEngine();
    }

    /**
     * Retrieves the Mustache template engine instance.
     * Mustache is often used for logic-less templating and uses '{{' and '}}' as default markers.
     *
     * @returns The Mustache template engine instance.
     */
    public static getMustacheEngine(): TemplateEngine {
        const engine = TemplateEnginesFacade.getMustacheEngine();
        return new TemplateEngine(engine, "mustache");
    }

    /**
     * Retrieves the Velocity template engine instance.
     * Velocity is often used for complex templating with directives (e.g., #set, #foreach).
     *
     * @returns The Velocity template engine instance.
     */
    public static getVelocityEngine(): TemplateEngine {
        const engine = TemplateEnginesFacade.getVelocityEngine();
        return new TemplateEngine(engine, "velocity");
    }

    /**
     * Retrieves the JavaScript template engine instance (usually used for server-side evaluation).
     *
     * @returns The JavaScript template engine instance.
     */
    public static getJavascriptEngine(): TemplateEngine {
        const engine = TemplateEnginesFacade.getJavascriptEngine();
        return new TemplateEngine(engine, "javascript");
    }

    /**
     * Generates output by processing a raw template string using the **default template engine (Velocity)**.
     *
     * @param location A string identifying the template (used for error reporting/caching, often a file path).
     * @param template The raw template string content to process.
     * @param parameters An object containing key-value pairs to be used as context variables in the template.
     * @returns The processed output string.
     */
    public static generate(location: string, template: string, parameters: { [key: string]: any }): string {
        return this.getDefaultEngine().generate(location, template, parameters);
    }

    /**
     * Loads a template from the public registry, selects an appropriate engine, and generates output.
     * It uses the **Mustache engine** if the file extension is `.mustache`, otherwise it uses the **default (Velocity)**.
     *
     * @param location The path to the template file within the `/registry/public/` directory (e.g., 'templates/email.mustache').
     * @param parameters An object containing key-value pairs to be used as context variables in the template.
     * @returns The processed output string, or `undefined` if the resource does not exist.
     */
    public static generateFromFile(location: string, parameters: { [key: string]: any }): string | undefined {
        const resource = Repository.getResource(REGISTRY_PUBLIC + location);
        if (resource.exists()) {
            const isMustacheTemplate = location.endsWith(MUSTACHE_FILE_EXTENSION);
            const engine = isMustacheTemplate ? this.getMustacheEngine() : this.getDefaultEngine();
            const template = resource.getText();
            return engine.generate(location, template, parameters);
        }
        return undefined;
    }
}

// @ts-ignore
if (typeof module !== 'undefined') {
	// @ts-ignore
	module.exports = TemplateEngines;
}
