/**
 * API Process
 * * Provides methods for interacting with process instances,
 * including starting, updating metadata, and managing variables.
 */

import { Values } from "./values";

const BpmFacade = Java.type("org.eclipse.dirigible.components.api.bpm.BpmFacade");

export class Process {

	/**
	 * Starts a new process instance for a given process definition key.
	 *
	 * @param key The process definition key (ID) of the process to start.
	 * @param businessKey An optional business key to associate with the process instance. Defaults to an empty string.
	 * @param parameters An optional map of process variables to pass to the process instance upon starting. Defaults to an empty object.
	 * @returns The unique ID of the newly started process instance.
	 */
	public static start(key: string, businessKey: string = '', parameters: { [key: string]: any } = {}): string {
		return BpmFacade.startProcess(key, businessKey, JSON.stringify(parameters));
	}

	/**
	 * Sets a human-readable name for an existing process instance.
	 *
	 * @param processInstanceId The ID of the process instance to update.
	 * @param name The new name for the process instance.
	 */
	public static setProcessInstanceName(processInstanceId: string, name: string): void {
		BpmFacade.setProcessInstanceName(processInstanceId, name);
	}

	/**
	 * Updates the business key of an existing process instance.
	 *
	 * @param processInstanceId The ID of the process instance to update.
	 * @param businessKey The new business key.
	 */
	public static updateBusinessKey(processInstanceId: string, businessKey: string): void {
		BpmFacade.updateBusinessKey(processInstanceId, businessKey);
	}

	/**
	 * Updates the business status of an existing process instance.
	 *
	 * @param processInstanceId The ID of the process instance to update.
	 * @param businessStatus The new business status.
	 */
	public static updateBusinessStatus(processInstanceId: string, businessStatus: string): void {
		BpmFacade.updateBusinessStatus(processInstanceId, businessStatus);
	}

	/**
	 * Retrieves the value of a specific variable from a process instance.
	 *
	 * @param processInstanceId The ID of the process instance.
	 * @param variableName The name of the variable to retrieve.
	 * @returns The value of the variable, or `null` if the variable does not exist. The type is `any` as it depends on the stored value.
	 */
	public static getVariable(processInstanceId: string, variableName: string): any {
		return BpmFacade.getVariable(processInstanceId, variableName);
	}

	/**
	 * Retrieves all variables associated with a process instance.
	 *
	 * @param processInstanceId The ID of the process instance.
	 * @returns An object containing all variables for the process instance, where keys are variable names and values are the variable values.
	 */
	public static getVariables(processInstanceId: string): any {
		return BpmFacade.getVariables(processInstanceId);
	}

	/**
	 * Sets or updates the value of a variable in a process instance.
	 *
	 * @param processInstanceId The ID of the process instance.
	 * @param variableName The name of the variable to set.
	 * @param value The new value for the variable. The type is `any` to accommodate different data types.
	 */
	public static setVariable(processInstanceId: string, variableName: string, value: any): void {
		BpmFacade.setVariable(processInstanceId, variableName, value);
	}

	/**
	 * Removes a variable from a process instance.
	 *
	 * @param processInstanceId The ID of the process instance.
	 * @param variableName The name of the variable to remove.
	 */
	public static removeVariable(processInstanceId: string, variableName: string): void {
		BpmFacade.removeVariable(processInstanceId, variableName);
	}

	/**
	 * Correlates a message event with a running process instance.
	 *
	 * @param processInstanceId The ID of the process instance to correlate the message to.
	 * @param messageName The name of the message event defined in the BPMN process.
	 * @param variables A map of variables (`Map<string, any>`) to pass along with the message event.
	 */
	public static correlateMessageEvent(processInstanceId: string, messageName: string, variables: Map<string, any>): void {
		BpmFacade.correlateMessageEvent(processInstanceId, messageName, Values.stringifyValuesMap(variables));
	}

	/**
	 * Retrieves the current execution context object, typically used within an execution listener or service task.
	 *
	 * @returns A new instance of the `ExecutionContext` containing details about the current process execution path.
	 */
	public static getExecutionContext() {
		return new ExecutionContext();
	}
}

/**
 * ExecutionContext object
 * * Provides detailed information and control over the current process execution path.
 */
class ExecutionContext {

	private execution: any;

	constructor() {
		this.execution = __context.get('execution');
	}

	/**
	 * Unique id of this path of execution that can be used as a handle to provide external signals back into the engine after wait states.
	 *
	 * @returns The unique ID of the current execution.
	 */
	public getId(): string {
		return this.execution.getId();
	}

	/**
	 * Reference to the overall process instance.
	 * @returns The ID of the process instance.
	 */
	public getProcessInstanceId(): string {
		return this.execution.getProcessInstanceId();
	}

	/**
	 * The 'root' process instance. When using call activity for example, the processInstance set will not always be the root. This method returns the topmost process instance.
	 *
	 * @returns The ID of the root process instance.
	 */
	public getRootProcessInstanceId(): string {
		return this.execution.getRootProcessInstanceId();
	}

	/**
	 * Will contain the event name in case this execution is passed in for an {@link ExecutionListener}.
	 * @returns The current event name, or `null`/empty string if not executing an event listener.
	 */
	public getEventName(): string {
		return this.execution.getEventName();
	}

	/**
	 * Sets the current event (typically when execution an {@link ExecutionListener}).
	 *
	 * @param eventName The name of the event.
	 */
	public setEventName(eventName: string): void {
		this.execution.setEventName(eventName);
	}

	/**
	 * The business key for the process instance this execution is associated with.
	 * @returns The business key.
	 */
	public getProcessInstanceBusinessKey(): string {
		return this.execution.getProcessInstanceBusinessKey();
	}

	/**
	 * The business status for the process instance this execution is associated with.
	 * @returns The business status.
	 */
	public getProcessInstanceBusinessStatus(): string {
		return this.execution.getProcessInstanceBusinessStatus();
	}

	/**
	 * The process definition key for the process instance this execution is associated with.
	 * @returns The process definition ID.
	 */
	public getProcessDefinitionId(): string {
		return this.execution.getProcessDefinitionId();
	}

	/**
	 * If this execution runs in the context of a case and stage, this method returns it's closest parent stage instance id (the stage plan item instance id to be
	 * precise).
	 *
	 * @returns The stage instance id this execution belongs to or `null`, if this execution is not part of a case at all or is not a child element of a stage.
	 */
	public getPropagatedStageInstanceId(): string {
		return this.execution.getPropagatedStageInstanceId();
	}

	/**
	 * Gets the id of the parent of this execution. If null, the execution represents a process-instance.
	 * @returns The parent execution ID, or `null`.
	 */
	public getParentId(): string {
		return this.execution.getParentId();
	}

	/**
	 * Gets the id of the calling execution. If not null, the execution is part of a subprocess.
	 * @returns The super execution ID, or `null`.
	 */
	public getSuperExecutionId(): string {
		return this.execution.getSuperExecutionId();
	}

	/**
	 * Gets the id of the current activity.
	 * @returns The current activity ID.
	 */
	public getCurrentActivityId(): string {
		return this.execution.getCurrentActivityId();
	}

	/**
	 * Returns the tenant id, if any is set before on the process definition or process instance.
	 * @returns The tenant ID, or `null`/empty string.
	 */
	public getTenantId(): string {
		return this.execution.getTenantId();
	}

	/**
	 * The BPMN element where the execution currently is at.
	 * @returns The current flow element object (type is Java object).
	 */
	public getCurrentFlowElement(): any {
		return this.execution.getCurrentFlowElement();
	}

	/**
	 * Change the current BPMN element the execution is at.
	 * @param flowElement The new flow element object (Java type).
	 */
	public setCurrentFlowElement(flowElement: any): void {
		this.execution.setCurrentFlowElement(flowElement);
	}

	/**
	 * Returns the {@link FlowableListener} instance matching an {@link ExecutionListener} if currently an execution listener is being execution. Returns null otherwise.
	 * @returns The current Flowable Listener object (Java type), or `null`.
	 */
	public getCurrentFlowableListener(): any {
		return this.execution.getCurrentFlowableListener();
	}

	/**
	 * Called when an {@link ExecutionListener} is being executed.
	 * @param currentListener The current listener object (Java type).
	 */
	public setCurrentFlowableListener(currentListener: any): void {
		this.execution.setCurrentFlowableListener(currentListener);
	}

	/**
	 * Create a snapshot read only delegate execution of this delegate execution.
	 *
	 * @returns A {@link ReadOnlyDelegateExecution} object (Java type).
	 */
	public snapshotReadOnly(): any {
		return this.execution.snapshotReadOnly();
	}

	/**
	 * returns the parent of this execution, or null if there no parent.
	 * @returns The parent execution object (Java type), or `null`.
	 */
	public getParent(): any {
		return this.execution.getParent();
	}

	/**
	 * returns the list of execution of which this execution the parent of.
	 * @returns An array of child execution objects (Java type).
	 */
	public getExecutions(): any[] {
		return this.execution.getExecutions();
	}

	/**
	 * makes this execution active or inactive.
	 * @param isActive A boolean indicating whether the execution should be active.
	 */
	public setActive(isActive: boolean): void {
		this.execution.setActive(isActive);
	}

	/**
	 * returns whether this execution is currently active.
	 * @returns `true` if active, `false` otherwise.
	 */
	public isActive(): boolean {
		return this.execution.isActive();
	}

	/**
	 * returns whether this execution has ended or not.
	 * @returns `true` if ended, `false` otherwise.
	 */
	public isEnded(): boolean {
		return this.execution.isEnded();
	}

	/**
	 * changes the concurrent indicator on this execution.
	 * @param isConcurrent A boolean indicating whether the execution should be concurrent.
	 */
	public setConcurrent(isConcurrent: boolean): void {
		this.execution.setConcurrent(isConcurrent);
	}

	/**
	 * returns whether this execution is concurrent or not.
	 * @returns `true` if concurrent, `false` otherwise.
	 */
	public isConcurrent(): boolean {
		return this.execution.isConcurrent();
	}

	/**
	 * returns whether this execution is a process instance or not.
	 * @returns `true` if it's a process instance, `false` otherwise.
	 */
	public isProcessInstanceType(): boolean {
		return this.execution.isProcessInstanceType();
	}

	/**
	 * Inactivates this execution. This is useful for example in a join: the execution still exists, but it is not longer active.
	 */
	public inactivate(): void {
		this.execution.inactivate();
	}

	/**
	 * Returns whether this execution is a scope.
	 * @returns `true` if it is a scope, `false` otherwise.
	 */
	public isScope(): boolean {
		return this.execution.isScope();
	}

	/**
	 * Changes whether this execution is a scope or not.
	 * @param isScope A boolean indicating whether the execution should be a scope.
	 */
	public setScope(isScope: boolean): void {
		this.execution.setScope(isScope);
	}

	/**
	 * Returns whether this execution is the root of a multi instance execution.
	 * @returns `true` if it's a multi instance root, `false` otherwise.
	 */
	public isMultiInstanceRoot(): boolean {
		return this.execution.isMultiInstanceRoot();
	}

	/**
	 * Changes whether this execution is a multi instance root or not.
	 * @param isMultiInstanceRoot A boolean indicating whether the execution is the root of a multi-instance execution.
	 */
	public setMultiInstanceRoot(isMultiInstanceRoot: boolean): void {
		this.execution.setMultiInstanceRoot(isMultiInstanceRoot);
	}

	/**
	 * Returns all variables. This will include all variables of parent scopes too.
	 * @returns A Map of variable names (`string`) to variable values (`any`).
	 */
	public getVariables(): Map<string, any> {
		const variables = this.execution.getVariables();
		for (const [key, value] of variables) {
			variables.set(key, Values.parseValue(value));
		}
		return variables;
	}

	/**
	 * Returns all variables, as instances of the {@link VariableInstance} interface, which gives more information than only the value (type, execution id, etc.)
	 * @returns A Map of variable names (`string`) to {@link VariableInstance} objects (Java type).
	 */
	public getVariableInstances(): Map<string, any> {
		return this.execution.getVariableInstances();
	}

	/**
	 * Returns the variable local to this scope only. So, in contrary to {@link #getVariables()}, the variables from the parent scope won't be returned.
	 * @returns A Map of variable names (`string`) to local variable values (`any`).
	 */
	public getVariablesLocal(): Map<string, any> {
		const variablesLocal = this.execution.getVariablesLocal();
		for (const [key, value] of variablesLocal) {
			variablesLocal.set(key, Values.parseValue(value));
		}
		return variablesLocal;
	}

	/**
	 * Returns the variables local to this scope as instances of the {@link VariableInstance} interface, which provided additional information about the variable.
	 * @returns A Map of variable names (`string`) to local {@link VariableInstance} objects (Java type).
	 */
	public getVariableInstancesLocal(): Map<string, any> {
		return this.execution.getVariableInstancesLocal();
	}

	/**
	 * Returns the variable value for one specific variable. Will look in parent scopes when the variable does not exist on this particular scope.
	 * @param variableName The name of the variable to retrieve.
	 * @returns The value of the variable (`any`), or `null` if not found.
	 */
	public getVariable(variableName: string): any {
		return Values.parseValue(this.execution.getVariable(variableName));
	}

	/**
	 * Similar to {@link #getVariable(String)}, but returns a {@link VariableInstance} instance, which contains more information than just the value.
	 * @param variableName The name of the variable to retrieve.
	 * @returns The {@link VariableInstance} object (Java type), or `null`.
	 */
	public getVariableInstance(variableName: string): any {
		return this.execution.getVariableInstance(variableName);
	}

	/**
	 * Returns the value for the specific variable and only checks this scope and not any parent scope.
	 * @param variableName The name of the local variable to retrieve.
	 * @returns The value of the local variable (`any`), or `null` if not found locally.
	 */
	public getVariableLocal(variableName: string): any {
		return Values.parseValue(this.execution.getVariableLocal(variableName));
	}

	/**
	 * Similar to {@link #getVariableLocal(String)}, but returns an instance of {@link VariableInstance}, which has some additional information beyond the value.
	 * @param variableName The name of the local variable to retrieve.
	 * @returns The local {@link VariableInstance} object (Java type), or `null`.
	 */
	public getVariableInstanceLocal(variableName: string): any {
		return this.execution.getVariableInstanceLocal(variableName);
	}

	/**
	 * Returns all the names of the variables for this scope and all parent scopes.
	 * @returns A Set of all variable names (`string`).
	 */
	public getVariableNames(): Set<string> {
		const variableNames = new Set<string>();
		for (const next of this.execution.getVariableNames().toArray()) {
			variableNames.add(next);
		}
		return variableNames;
	}

	/**
	 * Returns all the names of the variables for this scope (no parent scopes).
	 * @returns A Set of local variable names (`string`).
	 */
	public getVariableNamesLocal(): Set<string> {
		const variableNamesLocal = new Set<string>();
		for (const next of this.execution.getVariableNamesLocal().toArray()) {
			variableNamesLocal.add(next);
		}
		return variableNamesLocal;
	}

	/**
	 * Sets the variable with the provided name to the provided value. It checks parent scopes for an existing variable before setting on the current scope.
	 * @param variableName The name of the variable to be set.
	 * @param value The value of the variable to be set (`any`).
	 */
	public setVariable(variableName: string, value: any): void {
		this.execution.setVariable(variableName, Values.stringifyValue(value));
	}

	/**
	 * Similar to {@link #setVariable(String, Object)}, but the variable is set to this scope specifically (local variable).
	 * @param variableName The name of the variable to be set locally.
	 * @param value The value of the variable to be set (`any`).
	 * @returns The old value of the local variable (Java type), or `null`.
	 */
	public setVariableLocal(variableName: string, value: any): any {
		return this.execution.setVariableLocal(variableName, Values.stringifyValue(value));
	}

	/**
	 * Sets the provided variables to the variable scope, using the default scoping algorithm.
	 * @param variables A map of keys (`string`) and values (`any`) for the variables to be set.
	 */
	public setVariables(variables: Map<string, any>): void {
		for (const [key, value] of variables) {
			variables.set(key, Values.stringifyValue(value));
		}
		this.execution.setVariables(variables);
	}

	/**
	 * Similar to {@link #setVariables(Map)}, but the variables are set on this scope specifically (local variables).
	 * @param variables A map of keys (`string`) and values (`any`) for the local variables to be set.
	 */
	public setVariablesLocal(variables: Map<string, any>): void {
		for (const [key, value] of variables) {
			variables.set(key, Values.stringifyValue(value));
		}
		this.execution.setVariablesLocal(variables);
	}

	/**
	 * Returns whether this scope or any parent scope has variables.
	 * @returns `true` if variables exist in scope hierarchy, `false` otherwise.
	 */
	public hasVariables(): boolean {
		return this.execution.hasVariables();
	}

	/**
	 * Returns whether this scope has variables.
	 * @returns `true` if local variables exist, `false` otherwise.
	 */
	public hasVariablesLocal(): boolean {
		return this.execution.hasVariablesLocal();
	}

	/**
	 * Returns whether this scope or any parent scope has a specific variable.
	 * @param variableName The name of the variable to check.
	 * @returns `true` if the variable is found in the scope hierarchy, `false` otherwise.
	 */
	public hasVariable(variableName: string): boolean {
		return this.execution.hasVariable(variableName);
	}

	/**
	 * Returns whether this scope has a specific variable.
	 * @param variableName The name of the local variable to check.
	 * @returns `true` if the variable is found locally, `false` otherwise.
	 */
	public hasVariableLocal(variableName: string): boolean {
		return this.execution.hasVariableLocal(variableName);
	}

	/**
	 * Removes the variable from the process instance and creates a new HistoricVariableUpdate. Searches up the scope hierarchy.
	 * @param variableName The name of the variable to remove.
	 */
	public removeVariable(variableName: string): void {
		this.execution.removeVariable(variableName);
	}

	/**
	 * Removes the local variable and creates a new HistoricVariableUpdate.
	 * @param variableName The name of the local variable to remove.
	 */
	public removeVariableLocal(variableName: string): void {
		this.execution.removeVariableLocal(variableName);
	}

	/**
	 * Removes the variables and creates a new HistoricVariableUpdate for each of them. Searches up the scope hierarchy.
	 * @param variableNames An array of variable names (`string[]`) to remove.
	 */
	public removeVariables(variableNames: string[]): void {
		this.execution.removeVariables(variableNames);
	}

	/**
	 * Removes the local variables and creates a new HistoricVariableUpdate for each of them.
	 * @param variableNames An array of local variable names (`string[]`) to remove.
	 */
	public removeVariablesLocal(variableNames: string[]): void {
		this.execution.removeVariablesLocal(variableNames);
	}

	/**
	 * Sets a transient variable using the default scoping mechanism (up the hierarchy). Transient variables have no history and are cleared at wait states.
	 * @param variableName The name of the transient variable.
	 * @param variableValue The value of the transient variable (`any`).
	 */
	public setTransientVariable(variableName: string, variableValue: any): void {
		this.execution.setTransientVariable(variableName, Values.stringifyValue(variableValue));
	}

	/**
	 * Sets a local transient variable. Transient variables have no history and are cleared at wait states.
	 * @param variableName The name of the local transient variable.
	 * @param variableValue The value of the local transient variable (`any`).
	 */
	public setTransientVariableLocal(variableName: string, variableValue: any): void {
		this.execution.setTransientVariableLocal(variableName, Values.stringifyValue(variableValue));
	}

	/**
	 * Sets multiple transient variables using the default scoping mechanism (up the hierarchy).
	 * @param transientVariables A map of keys (`string`) and values (`any`) for the transient variables to be set.
	 */
	public setTransientVariables(transientVariables: Map<string, any>): void {
		for (const [key, value] of transientVariables) {
			transientVariables.set(key, Values.stringifyValue(value));
		}
		this.execution.setTransientVariables(transientVariables);
	}

	/**
	 * Retrieves the value of a transient variable, searching up the parent scopes.
	 * @param variableName The name of the transient variable to retrieve.
	 * @returns The value of the transient variable (`any`), or `null` if not found.
	 */
	public getTransientVariable(variableName: string): any {
		return Values.parseValue(this.execution.getTransientVariable(variableName));
	}

	/**
	 * Retrieves all transient variables in the current scope hierarchy.
	 * @returns A Map of transient variable names (`string`) to values (`any`).
	 */
	public getTransientVariables(): Map<string, any> {
		const transientVariables = this.execution.getTransientVariables();
		for (const [key, value] of transientVariables) {
			transientVariables.set(key, Values.parseValue(value));
		}
		return transientVariables;
	}

	/**
	 * Sets multiple local transient variables.
	 * @param transientVariables A map of keys (`string`) and values (`any`) for the local transient variables to be set.
	 */
	public setTransientVariablesLocal(transientVariables: Map<string, any>): void {
		for (const [key, value] of transientVariables) {
			transientVariables.set(key, Values.stringifyValue(value));
		}
		this.execution.setTransientVariablesLocal(transientVariables);
	}

	/**
	 * Retrieves the value of a local transient variable.
	 * @param variableName The name of the local transient variable to retrieve.
	 * @returns The value of the local transient variable (`any`), or `null` if not found.
	 */
	public getTransientVariableLocal(variableName: string): any {
		return Values.parseValue(this.execution.getTransientVariableLocal(variableName));
	}

	/**
	 * Retrieves all local transient variables.
	 * @returns A Map of local transient variable names (`string`) to values (`any`).
	 */
	public getTransientVariablesLocal(): Map<string, any> {
		const transientVariablesLocal = this.execution.getTransientVariablesLocal();
		for (const [key, value] of transientVariablesLocal) {
			transientVariablesLocal.set(key, Values.parseValue(value));
		}
		return transientVariablesLocal;
	}

	/**
	 * Removes a specific local transient variable.
	 * @param variableName The name of the local transient variable to remove.
	 */
	public removeTransientVariableLocal(variableName: string): void {
		this.execution.removeTransientVariableLocal(variableName);
	}

	/**
	 * Removes a specific transient variable, searching up the scope hierarchy.
	 * @param variableName The name of the transient variable to remove.
	 */
	public removeTransientVariable(variableName: string): void {
		this.execution.removeTransientVariable(variableName);
	}

	/**
	 * Remove all transient variables of this scope and its parent scopes.
	 */
	public removeTransientVariables(): void {
		this.execution.removeTransientVariables();
	}

	/**
	 * Removes all local transient variables.
	 */
	public removeTransientVariablesLocal(): void {
		this.execution.removeTransientVariablesLocal();
	}

}

// @ts-ignore
if (typeof module !== 'undefined') {
	// @ts-ignore
	module.exports = Process;
}
