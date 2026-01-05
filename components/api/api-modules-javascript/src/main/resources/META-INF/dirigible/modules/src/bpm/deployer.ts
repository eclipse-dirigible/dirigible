/**
 * API Deployer
 * * Provides methods for managing the lifecycle of Business Process Model and Notation (BPMN) definitions,
 * including deployment, undeployment, and deletion.
 */

const BpmFacade = Java.type("org.eclipse.dirigible.components.api.bpm.BpmFacade");

export class Deployer {

	/**
	 * Deploys a new process definition from a specified location (e.g., a file path).
	 *
	 * @param location The path or location of the BPMN XML file to be deployed.
	 * @returns The deployment ID assigned to the new process definition.
	 */
	public static deployProcess(location: string): string {
		return BpmFacade.deployProcess(location);
	}

	/**
	 * Undeploys a process definition previously deployed from the specified location.
	 *
	 * @param location The path or location associated with the deployed BPMN file.
	 */
	public static undeployProcess(location: string): void {
		BpmFacade.undeployProcess(location);
	}

	/**
	 * Deletes a deployed process definition by its ID.
	 *
	 * > **Note:** This permanently removes the process definition and all its associated history and runtime data.
	 *
	 * @param id The ID of the process definition to delete.
	 * @param reason The reason for deleting the process definition (e.g., "Obsolete").
	 */
	public static deleteProcess(id: string, reason: string): void {
		BpmFacade.deleteProcess(id, reason);
	}
}

// @ts-ignore
if (typeof module !== 'undefined') {
	// @ts-ignore
	module.exports = Deployer;
}