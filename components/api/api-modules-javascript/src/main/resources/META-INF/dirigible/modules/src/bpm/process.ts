/*
 * Copyright (c) 2023 SAP SE or an SAP affiliate company and Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: 2023 SAP SE or an SAP affiliate company and Eclipse Dirigible contributors
 * SPDX-License-Identifier: EPL-2.0
 */
/**
 * API Process
 */

const BpmFacade = Java.type("org.eclipse.dirigible.components.api.bpm.BpmFacade");

export class Process {

	public static start(key: string, parameters: { [key: string]: any } = {}): string {
		return BpmFacade.startProcess(key, JSON.stringify(parameters));
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

	public static getExecutionContext() {
		const data = JSON.parse(__context.get('execution'));
		return new ExecutionContext(data);
	}
}

/**
 * ExecutionContext object
 */
class ExecutionContext {

	private data: any;

	constructor(data: any) {
		this.data = data;
	}

	public getId(): string {
		return this.data.id;
	}

	public getRevision(): number {
		return this.data.revision;
	}

	public isInserted(): boolean {
		return this.data.isInserted;
	}

	public isUpdated(): boolean {
		return this.data.isUpdated;
	}

	public isDeleted(): boolean {
		return this.data.isDeleted;
	}

	public getTenantId(): string | undefined {
		return this.data.tenantId ?? undefined;
	}

	public getName(): string | undefined {
		return this.data.name ?? undefined;
	}

	public getDescription(): string | undefined {
		return this.data.description ?? undefined;
	}

	public getLocalizedName(): string | undefined {
		return this.data.localizedName ?? undefined;
	}

	public getLocalizedDescription(): string | undefined {
		return this.data.localizedDescription ?? undefined;
	}

	public getLockTime(): Date | undefined {
		return this.data.lockTime ?? undefined;
	}

	public isActive(): boolean {
		return this.data.isActive;
	}

	public isScope(): boolean {
		return this.data.isScope;
	}

	public isConcurrent(): boolean {
		return this.data.isConcurrent;
	}

	public isEnded(): boolean {
		return this.data.isEnded;
	}

	public isEventScope(): boolean {
		return this.data.isEventScope;
	}

	public isMultiInstanceRoot(): boolean {
		return this.data.isMultiInstanceRoot;
	}

	public isCountEnabled(): boolean {
		return this.data.isCountEnabled;
	}

	public getEventName(): string | undefined {
		return this.data.eventName ?? undefined;
	}

	public getDeleteReason(): string | undefined {
		return this.data.deleteReason ?? undefined;
	}

	public getSuspensionState(): number {
		return this.data.suspensionState;
	}

	public getStartActivityId(): string | undefined {
		return this.data.startActivityId ?? undefined;
	}

	public getStartUserId(): string | undefined {
		return this.data.startUserId ?? undefined;
	}

	public getStartTime(): Date | undefined {
		return this.data.startTime ?? undefined;
	}

	public getEventSubscriptionCount(): number {
		return this.data.eventSubscriptionCount;
	}

	public getTaskCount(): number {
		return this.data.taskCount;
	}

	public getJobCount(): number {
		return this.data.jobCount;
	}

	public getTimerJobCount(): number {
		return this.data.timerJobCount;
	}

	public getSuspendedJobCount(): number {
		return this.data.suspendedJobCount;
	}

	public getDeadLetterJobCount(): number {
		return this.data.deadLetterJobCount;
	}

	public getVariableCount(): number {
		return this.data.variableCount;
	}

	public getIdentityLinkCount(): number {
		return this.data.identityLinkCount;
	}

	public getProcessDefinitionId(): string | undefined {
		return this.data.processDefinitionId ?? undefined;
	}

	public getProcessDefinitionKey(): string | undefined {
		return this.data.processDefinitionKey ?? undefined;
	}

	public getProcessDefinitionName(): string | undefined {
		return this.data.processDefinitionName ?? undefined;
	}

	public getProcessDefinitionVersion(): number | undefined {
		return this.data.processDefinitionVersion ?? undefined;
	}

	public getDeploymentId(): string | undefined {
		return this.data.deploymentId ?? undefined;
	}

	public getActivityId(): string | undefined {
		return this.data.activityId ?? undefined;
	}

	public getActivityName(): string | undefined {
		return this.data.activityName ?? undefined;
	}

	public getProcessInstanceId(): string | undefined {
		return this.data.processInstanceId ?? undefined;
	}

	public getBusinessKey(): string | undefined {
		return this.data.businessKey ?? undefined;
	}

	public getParentId(): string | undefined {
		return this.data.parentId ?? undefined;
	}

	public getSuperExecutionId(): string | undefined {
		return this.data.superExecutionId ?? undefined;
	}

	public getRootProcessInstanceId(): string | undefined {
		return this.data.rootProcessInstanceId ?? undefined;
	}

	public isForcedUpdate(): boolean {
		return this.data.forcedUpdate;
	}

	public getCallbackId(): string | undefined {
		return this.data.callbackId ?? undefined;
	}

	public getCallbackType(): string | undefined {
		return this.data.callbackType ?? undefined;
	}

}

// @ts-ignore
if (typeof module !== 'undefined') {
	// @ts-ignore
	module.exports = Process;
}
