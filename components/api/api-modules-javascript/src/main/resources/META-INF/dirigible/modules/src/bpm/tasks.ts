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
 * API Tasks
 */

const BpmFacade = Java.type("org.eclipse.dirigible.components.api.bpm.BpmFacade");

export class Tasks {

	public static list(): TaskData[] {
		const tasks: any[] = JSON.parse(BpmFacade.getTasks());
		return tasks.map(e => new TaskData(e));
	}

	public static getVariables(taskId: string): { [key: string]: any } {
		return JSON.parse(BpmFacade.getTaskVariables(taskId));
	}

	public static setVariables(taskId: string, variables: { [key: string]: any } = {}): void {
		BpmFacade.setTaskVariables(taskId, JSON.stringify(variables));
	}

	public static complete(taskId: string, variables: { [key: string]: any } = {}): void {
		BpmFacade.completeTask(taskId, JSON.stringify(variables));
	}
}


class TaskData {

	private data: any;

	constructor(data: any) {
		this.data = data;
	}

	public getId(): string | undefined {
		return this.data.id ?? undefined;
	}

	public getRevision(): number {
		return this.data.revision;
	}

	public getOwner(): string | undefined {
		return this.data.owner ?? undefined;
	}

	public getAssigneeUpdatedCount(): number {
		return this.data.assigneeUpdatedCount;
	}

	public getOriginalAssignee(): string | undefined {
		return this.data.originalAssignee ?? undefined;
	}

	public getAssignee(): string | undefined {
		return this.data.assignee ?? undefined;
	}

	public getDelegationState(): string | undefined {
		return this.data.delegationState ?? undefined;
	}

	public getParentTaskId(): string | undefined {
		return this.data.parentTaskId ?? undefined;
	}

	public getName(): string | undefined {
		return this.data.name ?? undefined;
	}

	public getLocalizedName(): string | undefined {
		return this.data.localizedName ?? undefined;
	}

	public getDescription(): string | undefined {
		return this.data.description ?? undefined;
	}

	public getLocalizedDescription(): string | undefined {
		return this.data.localizedDescription ?? undefined;
	}

	public getPriority(): number {
		return this.data.priority;
	}

	public getCreateTime(): Date | undefined {
		return this.data.createTime ?? undefined;
	}

	public getDueDate(): Date | undefined {
		return this.data.dueDate ?? undefined;
	}

	public getSuspensionState(): number {
		return this.data.suspensionState ?? undefined;
	}

	public getCategory(): string | undefined {
		return this.data.category ?? undefined;
	}

	public isIdentityLinksInitialized(): boolean {
		return this.data.isIdentityLinksInitialized;
	}

	public getExecutionId(): string | undefined {
		return this.data.executionId ?? undefined;
	}

	public getProcessInstanceId(): string | undefined {
		return this.data.processInstanceId ?? undefined;
	}

	public getProcessDefinitionId(): string | undefined {
		return this.data.processDefinitionId ?? undefined;
	}

	public getScopeId(): string | undefined {
		return this.data.scopeId ?? undefined;
	}

	public getSubScopeId(): string | undefined {
		return this.data.subScopeId ?? undefined;
	}

	public getScopeType(): string | undefined {
		return this.data.scopeType ?? undefined;
	}

	public getScopeDefinitionId(): string | undefined {
		return this.data.scopeDefinitionId ?? undefined;
	}

	public getTaskDefinitionKey(): string | undefined {
		return this.data.taskDefinitionKey ?? undefined;
	}

	public getFormKey(): string | undefined {
		return this.data.formKey ?? undefined;
	}

	public isDeleted(): boolean {
		return this.data.isDeleted;
	}

	public isCanceled(): boolean {
		return this.data.isCanceled;
	}

	public isCountEnabled(): boolean {
		return this.data.isCountEnabled;
	}

	public getVariableCount(): number {
		return this.data.variableCount;
	}

	public getIdentityLinkCount(): number {
		return this.data.identityLinkCount;
	}

	public getClaimTime(): Date | undefined {
		return this.data.claimTime ?? undefined;
	}

	public getTenantId(): string {
		return this.data.tenantId;
	}

	public getEventName(): string | undefined {
		return this.data.eventName ?? undefined;
	}

	public getEventHandlerId(): string | undefined {
		return this.data.eventHandlerId ?? undefined;
	}

	public isForcedUpdate(): boolean {
		return this.data.forcedUpdate;
	}
}

// @ts-ignore
if (typeof module !== 'undefined') {
	// @ts-ignore
	module.exports = Tasks;
}
