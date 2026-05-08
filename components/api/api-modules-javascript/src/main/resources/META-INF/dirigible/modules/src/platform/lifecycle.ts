/**
 * @module platform/lifecycle
 * @package @aerokit/sdk/platform
 * @name Lifecycle
 * @overview
 * 
 * The Lifecycle module provides a static utility class for managing the lifecycle of projects on the platform, including publishing and unpublishing projects. It abstracts the complexities of project deployment, allowing developers to easily control the availability of their projects through simple method calls.
 * 
 * ### Key Features:
 * - **Project Publishing**: The `publish` method allows developers to publish a specific project or all projects within a workspace for a given user.
 * - **Project Unpublishing**: The `unpublish` method enables developers to unpublish a specific project or all currently deployed projects, making them unavailable for use.
 * 
 * ### Use Cases:
 * - **Deployment Management**: This module is ideal for applications that need to manage the deployment status of projects on the platform, allowing for dynamic control over which projects are available at any given time.
 * - **Automated Deployment**: Developers can use this module to automate the publishing and unpublishing of projects as part of their development and deployment workflows.
 * 
 * ### Example Usage:
 * ```ts
 * import { Lifecycle } from "@aerokit/sdk/platform";
 * 
 * // Publish a specific project
 * const publishResult = Lifecycle.publish("john_doe", "my_workspace", "my_project");
 * console.log("Publish Result:", publishResult);
 * 
 * // Unpublish all currently deployed projects
 * const unpublishResult = Lifecycle.unpublish();
 * console.log("Unpublish Result:", unpublishResult);
 * ``` 
 */

const LifecycleFacade = Java.type("org.eclipse.dirigible.components.api.platform.LifecycleFacade");

/**
 * @class Lifecycle
 * @description Static utility class to publish and unpublish projects on the platform.
 */
export class Lifecycle {

    /**
     * Publishes a project for a specific user and workspace.
     *
     * @param {string} user The username of the owner of the workspace.
     * @param {string} workspace The name of the workspace to publish from.
     * @param {string} [project="*"] The specific project name to publish. Use "*" to publish all projects in the workspace.
     * @returns {boolean} True if the publish operation was successful, false otherwise.
     */
    public static publish(user: string, workspace: string, project: string = "*"): boolean {
        return LifecycleFacade.publish(user, workspace, project);
    }

    /**
     * Unpublishes a currently deployed project.
     *
     * @param {string} [project="*"] The specific project name to unpublish. Use "*" to unpublish all currently deployed projects.
     * @returns {boolean} True if the unpublish operation was successful, false otherwise.
     */
    public static unpublish(project: string = "*"): boolean {
        return LifecycleFacade.unpublish(project);
    }
}


// @ts-ignore
if (typeof module !== 'undefined') {
	// @ts-ignore
	module.exports = Lifecycle;
}