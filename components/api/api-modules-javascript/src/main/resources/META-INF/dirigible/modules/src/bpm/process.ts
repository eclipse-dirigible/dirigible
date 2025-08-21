/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors
 * SPDX-License-Identifier: EPL-2.0
 */
/**
 * API Process
 */

import { Values } from "sdk/bpm/values";

const BpmFacade = Java.type("org.eclipse.dirigible.components.api.bpm.BpmFacade");

export class Process {

	public static start(key: string, businessKey: string = '', parameters: { [key: string]: any } = {}): string {
		return BpmFacade.startProcess(key, businessKey, JSON.stringify(parameters));
	}

	public static setProcessInstanceName(processInstanceId: string, name: string): void {
		BpmFacade.setProcessInstanceName(processInstanceId, name);
	}

	public static updateBusinessKey(processInstanceId: string, businessKey: string): void {
		BpmFacade.updateBusinessKey(processInstanceId, businessKey);
	}

	public static updateBusinessStatus(processInstanceId: string, businessStatus: string): void {
		BpmFacade.updateBusinessStatus(processInstanceId, businessStatus);
	}

	public static getVariable(processInstanceId: string, variableName: string): any {
		return BpmFacade.getVariable(processInstanceId, variableName);
	}

	public static getVariables(processInstanceId: string): any {
		return BpmFacade.getVariables(processInstanceId);
	}

	public static setVariable(processInstanceId: string, variableName: string, value: any): void {
		BpmFacade.setVariable(processInstanceId, variableName, value);
	}

	public static removeVariable(processInstanceId: string, variableName: string): void {
		BpmFacade.removeVariable(processInstanceId, variableName);
	}

	public static correlateMessageEvent(processInstanceId: string, messageName: string, variables: Map<string, any>): void {
        BpmFacade.correlateMessageEvent(processInstanceId, messageName, Values.stringifyValuesMap(variables));
    }

	public static getExecutionContext() {
		return new ExecutionContext();
	}
}

/**
 * ExecutionContext object
 */
class ExecutionContext {

	private execution: any;

	constructor() {
		this.execution = __context.get('execution');
	}

	/**
	 * Unique id of this path of execution that can be used as a handle to provide external signals back into the engine after wait states.
	 */
	public getId(): string {
		return this.execution.getId();
	}

	/** Reference to the overall process instance */
	public getProcessInstanceId(): string {
		return this.execution.getProcessInstanceId();
	}

	/**
	 * The 'root' process instance. When using call activity for example, the processInstance set will not always be the root. This method returns the topmost process instance.
	 */
	public getRootProcessInstanceId(): string {
		return this.execution.getRootProcessInstanceId();
	}

	/**
	 * Will contain the event name in case this execution is passed in for an {@link ExecutionListener}.
	 */
	public getEventName(): string {
		return this.execution.getEventName();
	}

	/**
	 * Sets the current event (typically when execution an {@link ExecutionListener}).
	 */
	public setEventName(eventName: string): void {
		this.execution.setEventName(eventName);
	}

	/**
	 * The business key for the process instance this execution is associated with.
	 */
	public getProcessInstanceBusinessKey(): string {
		return this.execution.getProcessInstanceBusinessKey();
	}

	/**
	 * The business status for the process instance this execution is associated with.
	 */
	public getProcessInstanceBusinessStatus(): string {
		return this.execution.getProcessInstanceBusinessStatus();
	}

	/**
	 * The process definition key for the process instance this execution is associated with.
	 */
	public getProcessDefinitionId(): string {
		return this.execution.getProcessDefinitionId();
	}

	/**
	 * If this execution runs in the context of a case and stage, this method returns it's closest parent stage instance id (the stage plan item instance id to be
	 * precise).
	 *
	 * @return the stage instance id this execution belongs to or null, if this execution is not part of a case at all or is not a child element of a stage
	 */
	public getPropagatedStageInstanceId(): string {
		return this.execution.getPropagatedStageInstanceId();
	}

	/**
	 * Gets the id of the parent of this execution. If null, the execution represents a process-instance.
	 */
	public getParentId(): string {
		return this.execution.getParentId();
	}

	/**
	 * Gets the id of the calling execution. If not null, the execution is part of a subprocess.
	 */
	public getSuperExecutionId(): string {
		return this.execution.getSuperExecutionId();
	}

	/**
	 * Gets the id of the current activity.
	 */
	public getCurrentActivityId(): string {
		return this.execution.getCurrentActivityId();
	}

	/**
	 * Returns the tenant id, if any is set before on the process definition or process instance.
	 */
	public getTenantId(): string {
		return this.execution.getTenantId();
	}

	/**
	 * The BPMN element where the execution currently is at.
	 */
	public getCurrentFlowElement(): any {
		return this.execution.getCurrentFlowElement();
	}

	/**
	 * Change the current BPMN element the execution is at.
	 */
	public setCurrentFlowElement(flowElement: any): void {
		this.execution.setCurrentFlowElement(flowElement);
	}

	/**
	 * Returns the {@link FlowableListener} instance matching an {@link ExecutionListener} if currently an execution listener is being execution. Returns null otherwise.
	 */
	public getCurrentFlowableListener(): any {
		return this.execution.getCurrentFlowableListener();
	}

	/**
	 * Called when an {@link ExecutionListener} is being executed.
	 */
	public setCurrentFlowableListener(currentListener: any): void {
		this.execution.setCurrentFlowableListener(currentListener);
	}

	/**
	 * Create a snapshot read only delegate execution of this delegate execution.
	 *
	 * @return a {@link ReadOnlyDelegateExecution}
	 */
	public snapshotReadOnly(): any {
		return this.execution.snapshotReadOnly();
	}

	/**
	 * returns the parent of this execution, or null if there no parent.
	 */
	public getParent(): any {
		return this.execution.getParent();
	}

	/**
	 * returns the list of execution of which this execution the parent of.
	 */
	public getExecutions(): any[] {
		return this.execution.getExecutions();
	}

	/**
	 * makes this execution active or inactive.
	 */
	public setActive(isActive: boolean): void {
		this.execution.setActive(isActive);
	}

	/**
	 * returns whether this execution is currently active.
	 */
	public isActive(): boolean {
		return this.execution.isActive();
	}

	/**
	 * returns whether this execution has ended or not.
	 */
	public isEnded(): boolean {
		return this.execution.isEnded();
	}

	/**
	 * changes the concurrent indicator on this execution.
	 */
	public setConcurrent(isConcurrent: boolean): void {
		this.execution.setConcurrent(isConcurrent);
	}

	/**
	 * returns whether this execution is concurrent or not.
	 */
	public isConcurrent(): boolean {
		return this.execution.isConcurrent();
	}

	/**
	 * returns whether this execution is a process instance or not.
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
	 */
	public isScope(): boolean {
		return this.execution.isScope();
	}

	/**
	 * Changes whether this execution is a scope or not.
	 */
	public setScope(isScope: boolean): void {
		this.execution.setScope(isScope);
	}

	/**
	 * Returns whether this execution is the root of a multi instance execution.
	 */
	public isMultiInstanceRoot(): boolean {
		return this.execution.isMultiInstanceRoot();
	}

	/**
	 * Changes whether this execution is a multi instance root or not.
	 * 
	 * @param isMultiInstanceRoot
	 */
	public setMultiInstanceRoot(isMultiInstanceRoot: boolean): void {
		this.execution.setMultiInstanceRoot(isMultiInstanceRoot);
	}

	/**
	 * Returns all variables. This will include all variables of parent scopes too.
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
	 */
	public getVariableInstances(): Map<string, any> {
		return this.execution.getVariableInstances();
	}

	/**
	 * Returns the variable local to this scope only. So, in contrary to {@link #getVariables()}, the variables from the parent scope won't be returned.
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
	 */
	public getVariableInstancesLocal(): Map<string, any> {
		return this.execution.getVariableInstancesLocal();
	}

	/**
	 * Returns the variable value for one specific variable. Will look in parent scopes when the variable does not exist on this particular scope.
	 */
	public getVariable(variableName: string): any {
		return Values.parseValue(this.execution.getVariable(variableName));
	}

	/**
	 * Similar to {@link #getVariable(String)}, but returns a {@link VariableInstance} instance, which contains more information than just the value.
	 */
	public getVariableInstance(variableName: string): any {
		return this.execution.getVariableInstance(variableName);
	}

	/**
	 * Returns the value for the specific variable and only checks this scope and not any parent scope.
	 */
	public getVariableLocal(variableName: string): any {
		return Values.parseValue(this.execution.getVariableLocal(variableName));
	}

	/**
	 * Similar to {@link #getVariableLocal(String)}, but returns an instance of {@link VariableInstance}, which has some additional information beyond the value.
	 */
	public getVariableInstanceLocal(variableName: string): any {
		return this.execution.getVariableInstanceLocal(variableName);
	}

	/**
	 * Returns all the names of the variables for this scope and all parent scopes.
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
	 */
	public getVariableNamesLocal(): Set<string> {
		const variableNamesLocal = new Set<string>();
		for (const next of this.execution.getVariableNamesLocal().toArray()) {
			variableNamesLocal.add(next);
		}
		return variableNamesLocal;
	}

	/**
	 * Sets the variable with the provided name to the provided value. In the case when variable name is an expression
	 * which is resolved by expression manager, the value is set in the object resolved from the expression.
	 * 
	 * <p>
	 * A variable is set according to the following algorithm:
	 *
	 * <ul>
	 * <li>If variable name is an expression, resolve expression and set the value on the resolved object.</li>
	 * <li>If this scope already contains a variable by the provided name as a <strong>local</strong> variable, its value is overwritten to the provided value.</li>
	 * <li>If this scope does <strong>not</strong> contain a variable by the provided name as a local variable, the variable is set to this scope's parent scope, if there is one. If there is no parent
	 * scope (meaning this scope is the root scope of the hierarchy it belongs to), this scope is used. This applies recursively up the parent scope chain until, if no scope contains a local variable
	 * by the provided name, ultimately the root scope is reached and the variable value is set on that scope.</li>
	 * </ul>
	 * In practice for most cases, this algorithm will set variables to the scope of the execution at the process instance’s root level, if there is no execution-local variable by the provided name.
	 * 
	 * @param variableName
	 *            the name of the variable to be set
	 * @param value
	 *            the value of the variable to be set
	 */
	public setVariable(variableName: string, value: any): void {
		this.execution.setVariable(variableName, Values.stringifyValue(value));
	}

	/**
	 * Similar to {@link #setVariable(String, Object)}, but the variable is set to this scope specifically. Variable name
	 is handled as a variable name string without resolving an expression.
	 */
	public setVariableLocal(variableName: string, value: any): any {
		return this.execution.setVariableLocal(variableName, Values.stringifyValue(value));
	}

	/**
	 * Sets the provided variables to the variable scope.
	 * 
	 * <p>
	 * Variables are set according algorithm for {@link #setVariable(String, Object)}, applied separately to each variable.
	 * 
	 * @param variables
	 *            a map of keys and values for the variables to be set
	 */
	public setVariables(variables: Map<string, any>): void {
		for (const [key, value] of variables) {
			variables.set(key, Values.stringifyValue(value));
		}
		this.execution.setVariables(variables);
	}

	/**
	 * Similar to {@link #setVariables(Map)}, but the variable are set on this scope specifically.
	 */
	public setVariablesLocal(variables: Map<string, any>): void {
		for (const [key, value] of variables) {
			variables.set(key, Values.stringifyValue(value));
		}
		this.execution.setVariablesLocal(variables);
	}

	/**
	 * Returns whether this scope or any parent scope has variables.
	 */
	public hasVariables(): boolean {
		return this.execution.hasVariables();
	}

	/**
	 * Returns whether this scope has variables.
	 */
	public hasVariablesLocal(): boolean {
		return this.execution.hasVariablesLocal();
	}

	/**
	 * Returns whether this scope or any parent scope has a specific variable.
	 */
	public hasVariable(variableName: string): boolean {
		return this.execution.hasVariable(variableName);
	}

	/**
	 * Returns whether this scope has a specific variable.
	 */
	public hasVariableLocal(variableName: string): boolean {
		return this.execution.hasVariableLocal(variableName);
	}

	/**
	 * Removes the variable and creates a new HistoricVariableUpdate.
	 */
	public removeVariable(variableName: string): void {
		this.execution.removeVariable(variableName);
	}

	/**
	 * Removes the local variable and creates a new HistoricVariableUpdate.
	 */
	public removeVariableLocal(variableName: string): void {
		this.execution.removeVariableLocal(variableName);
	}

	/**
	 * Removes the variables and creates a new HistoricVariableUpdate for each of them.
	 */
	public removeVariables(variableNames: string[]): void {
		this.execution.removeVariables(variableNames);
	}

	/**
	 * Removes the local variables and creates a new HistoricVariableUpdate for each of them.
	 */
	public removeVariablesLocal(variableNames: string[]): void {
		this.execution.removeVariablesLocal(variableNames);
	}

	/**
	 * Similar to {@link #setVariable(String, Object)}, but the variable is transient:
	 * 
	 * - no history is kept for the variable - the variable is only available until a waitstate is reached in the process - transient variables 'shadow' persistent variable (when getVariable('abc')
	 * where 'abc' is both persistent and transient, the transient value is returned.
	 */
	public setTransientVariable(variableName: string, variableValue: any): void {
		this.execution.setTransientVariable(variableName, Values.stringifyValue(variableValue));
	}

	/**
	 * Similar to {@link #setVariableLocal(String, Object)}, but for a transient variable. See {@link #setTransientVariable(String, Object)} for the rules on 'transient' variables.
	 */
	public setTransientVariableLocal(variableName: string, variableValue: any): void {
		this.execution.setTransientVariableLocal(variableName, Values.stringifyValue(variableValue));
	}

	/**
	 * Similar to {@link #setVariables(Map)}, but for transient variables. See {@link #setTransientVariable(String, Object)} for the rules on 'transient' variables.
	 */
	public setTransientVariables(transientVariables: Map<string, any>): void {
		for (const [key, value] of transientVariables) {
			transientVariables.set(key, Values.stringifyValue(value));
		}
		this.execution.setTransientVariables(transientVariables);
	}

	/**
	 * Similar to {@link #getVariable(String)}, including the searching via the parent scopes, but for transient variables only. See {@link #setTransientVariable(String, Object)} for the rules on
	 * 'transient' variables.
	 */
	public getTransientVariable(variableName: string): any {
		return Values.parseValue(this.execution.getTransientVariable(variableName));
	}

	/**
	 * Similar to {@link #getVariables()}, but for transient variables only. See {@link #setTransientVariable(String, Object)} for the rules on 'transient' variables.
	 */
	public getTransientVariables(): Map<string, any> {
		const transientVariables = this.execution.getTransientVariables();
		for (const [key, value] of transientVariables) {
			transientVariables.set(key, Values.parseValue(value));
		}
		return transientVariables;
	}

	/**
	 * Similar to {@link #setVariablesLocal(Map)}, but for transient variables. See {@link #setTransientVariable(String, Object)} for the rules on 'transient' variables.
	 */
	public setTransientVariablesLocal(transientVariables: Map<string, any>): void {
		for (const [key, value] of transientVariables) {
			transientVariables.set(key, Values.stringifyValue(value));
		}
		this.execution.setTransientVariablesLocal(transientVariables);
	}

	/**
	 * Similar to {@link #getVariableLocal(String)}, but for a transient variable. See {@link #setTransientVariable(String, Object)} for the rules on 'transient' variables.
	 */
	public getTransientVariableLocal(variableName: string): any {
		return Values.parseValue(this.execution.getTransientVariableLocal(variableName));
	}

	/**
	 * Similar to {@link #getVariableLocal(String)}, but for transient variables only. See {@link #setTransientVariable(String, Object)} for the rules on 'transient' variables.
	 */
	public getTransientVariablesLocal(): Map<string, any> {
		const transientVariablesLocal = this.execution.getTransientVariablesLocal();
		for (const [key, value] of transientVariablesLocal) {
			transientVariablesLocal.set(key, Values.parseValue(value));
		}
		return transientVariablesLocal;
	}

	/**
	 * Removes a specific transient variable (also searching parent scopes). See {@link #setTransientVariable(String, Object)} for the rules on 'transient' variables.
	 */
	public removeTransientVariableLocal(variableName: string): void {
		this.execution.removeTransientVariableLocal(variableName);
	}

	/**
	 * Removes a specific transient variable. See {@link #setTransientVariable(String, Object)} for the rules on 'transient' variables.
	 */
	public removeTransientVariable(variableName: string): void {
		this.execution.removeTransientVariable(variableName);
	}

	/**
	 * Remove all transient variable of this scope and its parent scopes. See {@link #setTransientVariable(String, Object)} for the rules on 'transient' variables.
	 */
	public removeTransientVariables(): void {
		this.execution.removeTransientVariables();
	}

	/**
	 * Removes all local transient variables. See {@link #setTransientVariable(String, Object)} for the rules on 'transient' variables.
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
