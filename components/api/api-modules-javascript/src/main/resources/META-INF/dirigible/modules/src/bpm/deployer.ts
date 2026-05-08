/**
 * @module bpm/deployer
 * @package @aerokit/sdk/bpm
 * @name Deployer
 * @overview
 * 
 * This module provides functionalities for managing Business Process Model and Notation (BPMN) definitions,
 * including deployment, undeployment, and deletion of process definitions.
 * 
 * ### Key Features
 * - Deploy BPMN process definitions from specified locations (e.g., file paths).
 * - Undeploy previously deployed process definitions based on their deployment location.
 * - Permanently delete process definitions by their ID, with a reason for deletion.
 * 
 * ### Use Cases
 * - Managing the lifecycle of BPMN process definitions in a workflow engine.
 * - Automating deployment and cleanup of process definitions in development and production environments.
 * - Integrating BPMN management into larger application workflows or administrative tools.
 * 
 * ### Example Usage
 * ```ts
 * import { Deployer } from "@aerokit/sdk/bpm";
 * 
 * // Deploy a new process definition
 * const deploymentId = Deployer.deployProcess("/path/to/process.bpmn");
 * console.log(`Deployed process with ID: ${deploymentId}`);
 * 
 * // Undeploy the process definition
 * Deployer.undeployProcess("/path/to/process.bpmn");
 * console.log("Undeployed process from location: /path/to/process.bpmn");
 * 
 * // Delete a deployed process definition by ID
 * Deployer.deleteProcess(deploymentId, "Obsolete");
 * console.log(`Deleted process with ID: ${deploymentId} for reason: Obsolete`);
 * ```
 *
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