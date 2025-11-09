/**
 * Provides a wrapper for managing the application lifecycle (publish/unpublish)
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