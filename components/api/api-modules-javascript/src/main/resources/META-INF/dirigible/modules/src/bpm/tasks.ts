/**
 * API Tasks
 */

import { Streams } from "@aerokit/sdk/io";
import { Values } from "@aerokit/sdk/bpm/values";

const BpmFacade = Java.type("org.eclipse.dirigible.components.api.bpm.BpmFacade");

export class Tasks {

	public static list(): TaskData[] {
		const tasks: any[] = JSON.parse(BpmFacade.getTasks());
		return tasks.map(e => new TaskData(e));
	}

	public static getVariable(taskId: string, variableName: string): any {
		return Values.parseValue(BpmFacade.getTaskVariable(taskId, variableName));
	}

	/**
	 * Returns all variables. This will include all variables of parent scopes too.
	 */
	public static getVariables(taskId: string): Map<string, any> {
		return Values.parseValuesMap(BpmFacade.getTaskVariables(taskId));
	}

	public static setVariable(taskId: string, variableName: string, value: any): void {
		BpmFacade.setTaskVariable(taskId, variableName, Values.stringifyValue(value));
	}

	public static setVariables(taskId: string, variables: Map<string, any>): void {
		BpmFacade.setTaskVariables(taskId, Values.stringifyValuesMap(variables));
	}

	public static complete(taskId: string, variables: { [key: string]: any } = {}): void {
		BpmFacade.completeTask(taskId, JSON.stringify(variables));
	}

	public static getTaskService(): TaskService {
		return new TaskService(BpmFacade.getBpmProviderFlowable().getTaskService());
	}
}

/**
 * Service which provides access to {@link Task} and form related operations.
 *
 */
export class TaskService {

	private readonly taskService: any;

	constructor(taskService: any) {
		this.taskService = taskService;
	}
	/**
	 * Creates a new task that is not related to any process instance.
	 * 
	 * The returned task is transient and must be saved with {@link #saveTask(Task)} 'manually'.
	 */
	public newTask(taskId?: string): Task {
		if (this.isNotNull(taskId)) {
			return this.taskService.newTask(taskId);
		}
		return this.taskService.newTask();
	}

	/**
	 * Create a builder for the task
	 * 
	 * @return task builder
	 */
	public createTaskBuilder(): TaskBuilder {
		return this.taskService.createTaskBuilder();
	}

	/**
	 * Saves the given task to the persistent data store. If the task is already present in the persistent store, it is updated. After a new task has been saved, the task instance passed into this
	 * method is updated with the id of the newly created task.
	 * 
	 * @param task
	 *            the task, cannot be null.
	 */
	public saveTask(task: Task): void {
		this.taskService.saveTask(task);
	}

	/**
	 * Saves the given tasks to the persistent data store. If the tasks are already present in the persistent store, it is updated. After a new task has been saved, the task instance passed into this
	 * method is updated with the id of the newly created task.
	 *
	 * @param taskList the list of task instances, cannot be null.
	 */
	public bulkSaveTasks(taskList: Task[]): void {
		this.taskService.bulkSaveTasks(taskList);
	}

	/**
	 * Deletes the given task, not deleting historic information that is related to this task.
	 * 
	 * @param taskId
	 *            The id of the task that will be deleted, cannot be null. If no task exists with the given taskId, the operation is ignored.
	 * @param cascade
	 *            If cascade is true, also the historic information related to this task is deleted.
	 * @throws FlowableObjectNotFoundException
	 *             when the task with given id does not exist.
	 * @throws FlowableException
	 *             when an error occurs while deleting the task or in case the task is part of a running process.
	 */
	public deleteTask(taskId: string, cascade?: boolean): void {
		if (this.isNotNull(cascade)) {
			this.taskService.deleteTask(taskId, cascade);
		} else {
			this.taskService.deleteTask(taskId);
		}
	}

	/**
	 * Deletes all tasks of the given collection, not deleting historic information that is related to these tasks.
	 * 
	 * @param taskIds
	 *            The id's of the tasks that will be deleted, cannot be null. All id's in the list that don't have an existing task will be ignored.
	 * @param cascade
	 *            If cascade is true, also the historic information related to this task is deleted.
	 * @throws FlowableObjectNotFoundException
	 *             when one of the task does not exist.
	 * @throws FlowableException
	 *             when an error occurs while deleting the tasks or in case one of the tasks is part of a running process.
	 */
	public deleteTasks(taskIds: string[], cascade?: boolean): void {
		if (this.isNotNull(cascade)) {
			this.taskService.deleteTasks(taskIds, cascade);
		} else {
			this.taskService.deleteTasks(taskIds);
		}
	}

	/**
	 * Deletes the given task, not deleting historic information that is related to this task..
	 * 
	 * @param taskId
	 *            The id of the task that will be deleted, cannot be null. If no task exists with the given taskId, the operation is ignored.
	 * @param deleteReason
	 *            reason the task is deleted. Is recorded in history, if enabled.
	 * @throws FlowableObjectNotFoundException
	 *             when the task with given id does not exist.
	 * @throws FlowableException
	 *             when an error occurs while deleting the task or in case the task is part of a running process
	 */
	public deleteTaskWithReason(taskId: string, deleteReason: string): void {
		this.taskService.deleteTask(taskId, deleteReason)
	}

	/**
	 * Deletes all tasks of the given collection, not deleting historic information that is related to these tasks.
	 * 
	 * @param taskIds
	 *            The id's of the tasks that will be deleted, cannot be null. All id's in the list that don't have an existing task will be ignored.
	 * @param deleteReason
	 *            reason the task is deleted. Is recorded in history, if enabled.
	 * @throws FlowableObjectNotFoundException
	 *             when one of the tasks does not exist.
	 * @throws FlowableException
	 *             when an error occurs while deleting the tasks or in case one of the tasks is part of a running process.
	 */
	public deleteTasksWithReason(taskIds: string[], deleteReason: string): void {
		this.taskService.deleteTasks(taskIds, deleteReason);
	}

	/**
	 * Claim responsibility for a task: the given user is made assignee for the task. The difference with {@link #setAssignee(String, String)} is that here a check is done if the task already has a
	 * user assigned to it. No check is done whether the user is known by the identity component.
	 * 
	 * @param taskId
	 *            task to claim, cannot be null.
	 * @param userId
	 *            user that claims the task. When userId is null the task is unclaimed, assigned to no one.
	 * @throws FlowableObjectNotFoundException
	 *             when the task doesn't exist.
	 * @throws org.flowable.common.engine.api.FlowableTaskAlreadyClaimedException
	 *             when the task is already claimed by another user
	 */
	public claim(taskId: string, userId: string): void {
		this.taskService.claim(taskId, userId);
	}

	/**
	 * A shortcut to {@link #claim} with null user in order to unclaim the task
	 * 
	 * @param taskId
	 *            task to unclaim, cannot be null.
	 * @throws FlowableObjectNotFoundException
	 *             when the task doesn't exist.
	 */
	public unclaim(taskId: string): void {
		this.taskService.unclaim(taskId);
	}

	/**
	 * Set the task state to in progress. No check is done whether the user is known by the identity component.
	 * 
	 * @param taskId
	 *            task to change the state, cannot be null.
	 * @param userId
	 *            user that puts the task in progress.
	 * @throws FlowableObjectNotFoundException
	 *             when the task doesn't exist.
	 */
	public startProgress(taskId: string, userId: string): void {
		this.taskService.startProgress(taskId, userId);
	}

	/**
	 * Suspends the task. No check is done whether the user is known by the identity component.
	 * 
	 * @param taskId
	 *            task to suspend, cannot be null.
	 * @param userId
	 *            user that suspends the task.
	 * @throws FlowableObjectNotFoundException
	 *             when the task doesn't exist.
	 */
	public suspendTask(taskId: string, userId: string): void {
		this.taskService.suspendTask(taskId, userId);
	}

	/**
	 * Activates the task. No check is done whether the user is known by the identity component.
	 * 
	 * @param taskId
	 *            task to activate, cannot be null.
	 * @param userId
	 *            user that activates the task.
	 * @throws FlowableObjectNotFoundException
	 *             when the task doesn't exist.
	 */
	public activateTask(taskId: string, userId: string): void {
		this.taskService.activateTask(taskId, userId);
	}

	/**
	 * Delegates the task to another user. This means that the assignee is set and the delegation state is set to {@link DelegationState#PENDING}. If no owner is set on the task, the owner is set to
	 * the current assignee of the task.
	 * 
	 * @param taskId
	 *            The id of the task that will be delegated.
	 * @param userId
	 *            The id of the user that will be set as assignee.
	 * @throws FlowableObjectNotFoundException
	 *             when no task exists with the given id.
	 */
	public delegateTask(taskId: string, userId: string): void {
		this.taskService.delegateTask(taskId, userId);
	}

	/**
	 * Marks that the assignee is done with this task and that it can be send back to the owner. Can only be called when this task is {@link DelegationState#PENDING} delegation. After this method
	 * returns, the {@link Task#getDelegationState() delegationState} is set to {@link DelegationState#RESOLVED}.
	 * 
	 * @param taskId
	 *            the id of the task to resolve, cannot be null.
	 * @param variables
	 * @param transientVariables
	 * @throws FlowableObjectNotFoundException
	 *             when no task exists with the given id.
	 */
	public resolveTask(taskId: string, variables?: Map<string, any>, transientVariables?: Map<string, any>): void {
		if (this.isNotNull(variables) && this.isNotNull(transientVariables)) {
			this.taskService.resolveTask(taskId, variables, transientVariables);
		} else if (this.isNotNull(variables)) {
			this.taskService.resolveTask(taskId, variables);
		} else {
			this.taskService.resolveTask(taskId);
		}
	}

	/**
	 * Called when the task is successfully executed.
	 * 
	 * @param taskId
	 *            the id of the task to complete, cannot be null.
	 * @param userId
	 *            user that completes the task.
	 * @param variables
	 *            task parameters. May be null or empty.
	 * @param transientVariables
	 *            task parameters. May be null or empty.
	 * @param localScope
	 *            If true, the provided variables will be stored task-local, instead of process instance wide (which is the default behaviour).
	 * @throws FlowableObjectNotFoundException
	 *             when no task exists with the given id.
	 * @throws FlowableException
	 *             when this task is {@link DelegationState#PENDING} delegation.
	 */
	public complete(taskId: string, userId?: string, variables?: Map<string, any>, transientVariables?: Map<string, any>, localScope?: boolean): void {
		if (this.isNotNull(userId) && this.isNotNull(variables) && this.isNotNull(localScope)) {
			this.taskService.complete(taskId, userId, variables, localScope);
		} else if (this.isNotNull(variables) && this.isNotNull(localScope)) {
			this.taskService.complete(taskId, variables, localScope);
		} else if (this.isNotNull(userId) && this.isNotNull(variables) && this.isNotNull(transientVariables)) {
			this.taskService.complete(taskId, userId, variables, transientVariables);
		} else if (this.isNotNull(variables) && this.isNotNull(transientVariables)) {
			this.taskService.complete(taskId, variables, transientVariables);
		} else if (this.isNotNull(userId) && this.isNotNull(variables)) {
			this.taskService.complete(taskId, userId, variables);
		} else if (this.isNotNull(variables)) {
			this.taskService.complete(taskId, variables);
		} else if (this.isNotNull(userId)) {
			this.taskService.complete(taskId, userId);
		} else {
			this.taskService.complete(taskId);
		}
	}

	/**
	 * Called when the task is successfully executed, and the task form has been submitted.
	 * 
	 * @param taskId
	 *            the id of the task to complete, cannot be null.
	 * @param formDefinitionId
	 *            the id of the form definition that is filled-in to complete the task, cannot be null.
	 * @param outcome
	 *            the outcome of the completed form, can be null.
	 * @param variables
	 *            values of the completed form. May be null or empty.
	 * @param userId
	 *            user that completes the task.
	 * @param transientVariables
	 *            additional transient values that need to added to the process instance transient variables. May be null or empty.
	 * @param localScope
	 *            If true, the provided variables will be stored task-local, instead of process instance wide (which is the default for {@link #complete(String, Map)}).
	 * @throws FlowableObjectNotFoundException
	 *             when no task exists with the given id.
	 */
	public completeTaskWithForm(taskId: string, formDefinitionId: string, outcome: string, variables: Map<String, any>, userId?: string, transientVariables?: Map<String, any>, localScope?: boolean): void {
		if (this.isNotNull(userId) && this.isNotNull(localScope)) {
			this.taskService.completeTaskWithForm(taskId, formDefinitionId, outcome, userId, variables, localScope);
		} else if (this.isNotNull(localScope)) {
			this.taskService.completeTaskWithForm(taskId, formDefinitionId, outcome, variables, localScope);
		} else if (this.isNotNull(userId) && this.isNotNull(transientVariables)) {
			this.taskService.completeTaskWithForm(taskId, formDefinitionId, outcome, userId, variables, transientVariables);
		} else if (this.isNotNull(transientVariables)) {
			this.taskService.completeTaskWithForm(taskId, formDefinitionId, outcome, variables, transientVariables);
		} else if (this.isNotNull(userId)) {
			this.taskService.completeTaskWithForm(taskId, formDefinitionId, outcome, userId, variables);
		} else {
			this.taskService.completeTaskWithForm(taskId, formDefinitionId, outcome, variables);
		}
	}
	/**
	 * Gets a Form model instance of the task form of a specific task
	 * 
	 * @param taskId
	 *            id of the task, cannot be null.
	 * @param ignoreVariables
	 *            should the variables be ignored when fetching the form model?
	 * @throws FlowableObjectNotFoundException
	 *             when the task or form definition doesn't exist.
	 */
	public getTaskFormModel(taskId: string, ignoreVariables?: boolean): FormInfo {
		if (this.isNotNull(ignoreVariables)) {
			return this.taskService.getTaskFormModel(taskId, ignoreVariables);
		} else {
			return this.taskService.getTaskFormModel(taskId);
		}
	}

	/**
	 * Changes the assignee of the given task to the given userId. No check is done whether the user is known by the identity component.
	 * 
	 * @param taskId
	 *            id of the task, cannot be null.
	 * @param userId
	 *            id of the user to use as assignee.
	 * @throws FlowableObjectNotFoundException
	 *             when the task or user doesn't exist.
	 */
	public setAssignee(taskId: string, userId: string): void {
		this.taskService.setAssignee(taskId, userId);
	}

	/**
	 * Transfers ownership of this task to another user. No check is done whether the user is known by the identity component.
	 * 
	 * @param taskId
	 *            id of the task, cannot be null.
	 * @param userId
	 *            of the person that is receiving ownership.
	 * @throws FlowableObjectNotFoundException
	 *             when the task or user doesn't exist.
	 */
	public setOwner(taskId: string, userId: string): void {
		this.taskService.setOwner(taskId, userId);
	}

	/**
	 * Retrieves the {@link IdentityLink}s associated with the given task. Such an {@link IdentityLink} informs how a certain identity (eg. group or user) is associated with a certain task (eg. as
	 * candidate, assignee, etc.)
	 */
	public getIdentityLinksForTask(taskId: string): IdentityLink[] {
		return this.taskService.getIdentityLinksForTask(taskId);
	}

	/**
	 * Convenience shorthand for {@link #addUserIdentityLink(String, String, String)}; with type {@link IdentityLinkType#CANDIDATE}
	 * 
	 * @param taskId
	 *            id of the task, cannot be null.
	 * @param userId
	 *            id of the user to use as candidate, cannot be null.
	 * @throws FlowableObjectNotFoundException
	 *             when the task or user doesn't exist.
	 */
	public addCandidateUser(taskId: string, userId: string): void {
		this.taskService.addCandidateUser(taskId, userId);
	}

	/**
	 * Convenience shorthand for {@link #addGroupIdentityLink(String, String, String)}; with type {@link IdentityLinkType#CANDIDATE}
	 * 
	 * @param taskId
	 *            id of the task, cannot be null.
	 * @param groupId
	 *            id of the group to use as candidate, cannot be null.
	 * @throws FlowableObjectNotFoundException
	 *             when the task or group doesn't exist.
	 */
	public addCandidateGroup(taskId: string, groupId: string): void {
		this.taskService.addCandidateGroup(taskId, groupId);
	}

	/**
	 * Involves a user with a task. The type of identity link is defined by the given identityLinkType.
	 * 
	 * @param taskId
	 *            id of the task, cannot be null.
	 * @param userId
	 *            id of the user involve, cannot be null.
	 * @param identityLinkType
	 *            type of identityLink, cannot be null (@see {@link IdentityLinkType}).
	 * @throws FlowableObjectNotFoundException
	 *             when the task or user doesn't exist.
	 */
	public addUserIdentityLink(taskId: string, userId: string, identityLinkType: string): void {
		this.taskService.addUserIdentityLink(taskId, userId, identityLinkType);
	}

	/**
	 * Involves a group with a task. The type of identityLink is defined by the given identityLink.
	 * 
	 * @param taskId
	 *            id of the task, cannot be null.
	 * @param groupId
	 *            id of the group to involve, cannot be null.
	 * @param identityLinkType
	 *            type of identity, cannot be null (@see {@link IdentityLinkType}).
	 * @throws FlowableObjectNotFoundException
	 *             when the task or group doesn't exist.
	 */
	public addGroupIdentityLink(taskId: string, groupId: string, identityLinkType: string): void {
		this.taskService.addGroupIdentityLink(taskId, groupId, identityLinkType);
	}

	/**
	 * Convenience shorthand for {@link #deleteUserIdentityLink(String, String, String)}; with type {@link IdentityLinkType#CANDIDATE}
	 * 
	 * @param taskId
	 *            id of the task, cannot be null.
	 * @param userId
	 *            id of the user to use as candidate, cannot be null.
	 * @throws FlowableObjectNotFoundException
	 *             when the task or user doesn't exist.
	 */
	public deleteCandidateUser(taskId: string, userId: string): void {
		this.taskService.deleteCandidateUser(taskId, userId);
	}

	/**
	 * Convenience shorthand for {@link #deleteGroupIdentityLink(String, String, String)}; with type {@link IdentityLinkType#CANDIDATE}
	 * 
	 * @param taskId
	 *            id of the task, cannot be null.
	 * @param groupId
	 *            id of the group to use as candidate, cannot be null.
	 * @throws FlowableObjectNotFoundException
	 *             when the task or group doesn't exist.
	 */
	public deleteCandidateGroup(taskId: string, groupId: string): void {
		this.taskService.deleteCandidateGroup(taskId, groupId);
	}

	/**
	 * Removes the association between a user and a task for the given identityLinkType.
	 * 
	 * @param taskId
	 *            id of the task, cannot be null.
	 * @param userId
	 *            id of the user involve, cannot be null.
	 * @param identityLinkType
	 *            type of identityLink, cannot be null (@see {@link IdentityLinkType}).
	 * @throws FlowableObjectNotFoundException
	 *             when the task or user doesn't exist.
	 */
	public deleteUserIdentityLink(taskId: string, userId: string, identityLinkType: string): void {
		this.taskService.deleteUserIdentityLink(taskId, userId, identityLinkType);
	}

	/**
	 * Removes the association between a group and a task for the given identityLinkType.
	 * 
	 * @param taskId
	 *            id of the task, cannot be null.
	 * @param groupId
	 *            id of the group to involve, cannot be null.
	 * @param identityLinkType
	 *            type of identity, cannot be null (@see {@link IdentityLinkType}).
	 * @throws FlowableObjectNotFoundException
	 *             when the task or group doesn't exist.
	 */
	public deleteGroupIdentityLink(taskId: string, groupId: string, identityLinkType: string): void {
		this.taskService.deleteGroupIdentityLink(taskId, groupId, identityLinkType);
	}

	/**
	 * Changes the priority of the task.
	 * 
	 * Authorization: actual owner / business admin
	 * 
	 * @param taskId
	 *            id of the task, cannot be null.
	 * @param priority
	 *            the new priority for the task.
	 * @throws FlowableObjectNotFoundException
	 *             when the task doesn't exist.
	 */
	public setPriority(taskId: string, priority: number): void {
		this.taskService.setPriority(taskId, priority);
	}

	/**
	 * Changes the due date of the task
	 * 
	 * @param taskId
	 *            id of the task, cannot be null.
	 * @param dueDate
	 *            the new due date for the task
	 * @throws FlowableException
	 *             when the task doesn't exist.
	 */
	public setDueDate(taskId: string, dueDate: Date): void {
		this.taskService.setDueDate(taskId, dueDate);
	}

	/**
	 * set variable on a task. If the variable is not already existing, it will be created in the most outer scope. This means the process instance in case this task is related to an execution.
	 */
	public setVariable(taskId: string, variableName: string, value: any): void {
		this.taskService.setVariable(taskId, variableName, Values.stringifyValue(value));
	}

	/**
	 * set variables on a task. If the variable is not already existing, it will be created in the most outer scope. This means the process instance in case this task is related to an execution.
	 */
	public setVariables(taskId: string, variables: Map<string, any>): void {
		this.taskService.setVariables(taskId, Values.stringifyValuesMap(variables));
	}

	/**
	 * set variable on a task. If the variable is not already existing, it will be created in the task.
	 */
	public setVariableLocal(taskId: string, variableName: string, value: any): void {
		this.taskService.setVariableLocal(taskId, variableName, Values.stringifyValue(value));
	}

	/**
	 * set variables on a task. If the variable is not already existing, it will be created in the task.
	 */
	public setVariablesLocal(taskId: string, variables: Map<string, any>): void {
		this.taskService.setVariablesLocal(taskId, Values.stringifyValuesMap(variables));
	}

	/**
	 * get a variables and search in the task scope and if available also the execution scopes.
	 */
	public getVariable(taskId: string, variableName: string): any {
		return Values.parseValue(this.taskService.getVariable(taskId, variableName));
	}

	/**
	 * The variable. Searching for the variable is done in all scopes that are visible to the given task (including parent scopes). Returns null when no variable value is found with the given name.
	 *
	 * @param taskId
	 *            id of task, cannot be null.
	 * @param variableName
	 *            name of variable, cannot be null.
	 * @return the variable or null if the variable is undefined.
	 * @throws FlowableObjectNotFoundException
	 *             when no execution is found for the given taskId.
	 */
	public getVariableInstance(taskId: string, variableName: string): VariableInstance {
		return this.taskService.getVariableInstance(taskId, variableName);
	}

	/**
	 * checks whether or not the task has a variable defined with the given name, in the task scope and if available also the execution scopes.
	 */
	public hasVariable(taskId: string, variableName: string): boolean {
		return this.taskService.hasVariable(taskId, variableName);
	}

	/**
	 * checks whether or not the task has a variable defined with the given name.
	 */
	public getVariableLocal(taskId: string, variableName: string): any {
		return Values.parseValue(this.taskService.getVariableLocal(taskId, variableName));
	}

	/**
	 * The variable for a task. Returns the variable when it is set for the task (and not searching parent scopes). Returns null when no variable is found with the given name.
	 *
	 * @param taskId
	 *            id of task, cannot be null.
	 * @param variableName
	 *            name of variable, cannot be null.
	 * @return the variable or null if the variable is undefined.
	 * @throws FlowableObjectNotFoundException
	 *             when no task is found for the given taskId.
	 */
	public getVariableInstanceLocal(taskId: string, variableName: string): VariableInstance {
		return this.taskService.getVariableInstanceLocal(taskId, variableName);
	}

	/**
	 * checks whether or not the task has a variable defined with the given name, local task scope only.
	 */
	public hasVariableLocal(taskId: string, variableName: string): boolean {
		return this.taskService.hasVariableLocal(taskId, variableName);
	}

	/**
	 * get all variables and search in the task scope and if available also the execution scopes. If you have many variables and you only need a few, consider using
	 * {@link #getVariables(String, Collection)} for better performance.
	 */
	public getVariables(taskId: string, variableNames?: string[]): Map<string, any> {
		if (this.isNotNull(variableNames)) {
			return Values.parseValuesMap(this.taskService.getVariables(taskId, variableNames));
		} else {
			return Values.parseValuesMap(this.taskService.getVariables(taskId));
		}
	}

	/**
	 * All variables visible from the given task scope (including parent scopes).
	 *
	 * @param taskId
	 *            id of task, cannot be null.
	 * @param variableNames
	 *            the collection of variable names that should be retrieved.
	 * @return the variable instances or an empty map if no such variables are found.
	 * @throws FlowableObjectNotFoundException
	 *             when no task is found for the given taskId.
	 */
	public getVariableInstances(taskId: string, variableNames?: string[]): Map<string, VariableInstance> {
		if (this.isNotNull(variableNames)) {
			return this.taskService.getVariableInstances(taskId, variableNames);
		} else {
			return this.taskService.getVariableInstances(taskId);
		}
	}

	/**
	 * get all variables and search only in the task scope. If you have many task local variables and you only need a few, consider using {@link #getVariablesLocal(String, Collection)} for better
	 * performance.
	 */
	public getVariablesLocal(taskId: string, variableNames?: string[]): Map<string, any> {
		if (this.isNotNull(variableNames)) {
			return Values.parseValuesMap(this.taskService.getVariablesLocal(taskId, variableNames));
		} else {
			return Values.parseValuesMap(this.taskService.getVariablesLocal(taskId));
		}
	}

	/** get all variables and search only in the task scope. */
	public getVariableInstancesLocalByTaskIds(taskIds: Set<string>): VariableInstance[] {
		return this.taskService.getVariableInstancesLocalByTaskIds(taskIds);
	}

	/**
	 * All variable values that are defined in the task scope, without taking outer scopes into account. If you have many task local variables and you only need a few, consider using
	 * {@link #getVariableInstancesLocal(String, Collection)} for better performance.
	 *
	 * @param taskId
	 *            id of task, cannot be null.
	 * @return the variables or an empty map if no such variables are found.
	 * @throws FlowableObjectNotFoundException
	 *             when no task is found for the given taskId.
	 */
	public getVariableInstancesLocal(taskId: string, variableNames?: string[]): Map<string, VariableInstance> {
		if (this.isNotNull(variableNames)) {
			return this.taskService.getVariableInstancesLocal(taskId, variableNames);
		} else {
			return this.taskService.getVariableInstancesLocal(taskId);
		}
	}

	/**
	 * Removes the variable from the task. When the variable does not exist, nothing happens.
	 */
	public removeVariable(taskId: string, variableName: string): void {
		this.taskService.removeVariable(taskId, variableName);
	}

	/**
	 * Removes the variable from the task (not considering parent scopes). When the variable does not exist, nothing happens.
	 */
	public removeVariableLocal(taskId: string, variableName: string): void {
		this.taskService.removeVariableLocal(taskId, variableName);
	}

	/**
	 * Removes all variables in the given collection from the task. Non existing variable names are simply ignored.
	 */
	public removeVariables(taskId: string, variableNames: string[]): void {
		this.taskService.removeVariables(taskId, variableNames);
	}

	/**
	 * Removes all variables in the given collection from the task (not considering parent scopes). Non existing variable names are simply ignored.
	 */
	public removeVariablesLocal(taskId: string, variableNames: string[]): void {
		this.taskService.removeVariablesLocal(taskId, variableNames);
	}

	/**
	 * All DataObjects visible from the given execution scope (including parent scopes).
	 *
	 * @param taskId
	 *            id of task, cannot be null.
	 * @param dataObjectNames
	 *            the collection of DataObject names that should be retrieved.
	 * @param locale
	 *            locale the DataObject name and description should be returned in (if available).
	 * @param withLocalizationFallback
	 *            When true localization will fallback to more general locales if the specified locale is not found.
	 * @return the DataObjects or an empty map if no such variables are found.
	 * @throws FlowableObjectNotFoundException
	 *             when no task is found for the given taskId.
	 */
	public getDataObjects(taskId: string, dataObjectNames?: string[], locale?: string, withLocalizationFallback?: boolean): Map<string, DataObject> {
		if (this.isNotNull(dataObjectNames) && this.isNotNull(locale) && this.isNotNull(withLocalizationFallback)) {
			return this.taskService.getDataObjects(taskId, dataObjectNames, locale, withLocalizationFallback);
		} else if (this.isNotNull(dataObjectNames)) {
			return this.taskService.getDataObjects(taskId, dataObjectNames);
		} else if (this.isNotNull(locale) && this.isNotNull(withLocalizationFallback)) {
			return this.taskService.getDataObjects(taskId, locale, withLocalizationFallback);
		} else {
			return this.taskService.getDataObjects(taskId);
		}
	}

	/**
	 * The DataObject. Searching for the DataObject is done in all scopes that are visible to the given task (including parent scopes). Returns null when no DataObject value is found with the given
	 * name.
	 *
	 * @param taskId
	 *            id of task, cannot be null.
	 * @param dataObject
	 *            name of DataObject, cannot be null.
	 * @param locale
	 *            locale the DataObject name and description should be returned in (if available).
	 * @param withLocalizationFallback
	 *            When true localization will fallback to more general locales including the default locale of the JVM if the specified locale is not found.
	 * @return the DataObject or null if the variable is undefined.
	 * @throws FlowableObjectNotFoundException
	 *             when no task is found for the given taskId.
	 */
	public getDataObject(taskId: string, dataObject: string, locale?: string, withLocalizationFallback?: boolean): DataObject {
		if (this.isNotNull(locale) && this.isNotNull(withLocalizationFallback)) {
			return this.taskService.getDataObject(taskId, dataObject, locale, withLocalizationFallback);
		} else {
			return this.taskService.getDataObject(taskId, dataObject);
		}
	}

	/** Add a comment to a task and/or process instance. */
	public addComment(taskId: string, processInstanceId: string, message: string, type?: string): Comment {
		if (this.isNotNull(type)) {
			return this.taskService.addComment(taskId, processInstanceId, type, message);
		} else {
			return this.taskService.addComment(taskId, processInstanceId, message);
		}
	}

	/** Update a comment to a task and/or process instance. */
	public saveComment(comment: Comment): void {
		this.taskService.saveComment(comment);
	}

	/**
	 * Returns an individual comment with the given id. Returns null if no comment exists with the given id.
	 */
	public getComment(commentId: string): Comment {
		return this.taskService.getComment(commentId);
	}

	/** Removes all comments from the provided task and/or process instance */
	public deleteComments(taskId: string, processInstanceId: string): void {
		this.taskService.deleteComments(taskId, processInstanceId);
	}

	/**
	 * Removes an individual comment with the given id.
	 * 
	 * @throws FlowableObjectNotFoundException
	 *             when no comment exists with the given id.
	 */
	public deleteComment(commentId: string): void {
		this.taskService.deleteComment(commentId);
	}

	/** The comments related to the given task. */
	public getTaskComments(taskId: string, type?: string): Comment[] {
		if (this.isNotNull(type)) {
			return this.taskService.getTaskComments(taskId, type);
		} else {
			return this.taskService.getTaskComments(taskId);
		}
	}

	/** All comments of a given type. */
	public getCommentsByType(type: string): Comment[] {
		return this.taskService.getCommentsByType(type);
	}

	/** The all events related to the given task. */
	public getTaskEvents(taskId: string): TaskEvent[] {
		return this.taskService.getTaskEvents(taskId);
	}

	/**
	 * Returns an individual event with the given id. Returns null if no event exists with the given id.
	 */
	public getEvent(eventId: string): TaskEvent {
		return this.taskService.getEvent(eventId);
	}

	/** The comments related to the given process instance. */
	public getProcessInstanceComments(processInstanceId: string, type?: string): Comment[] {
		if (this.isNotNull(type)) {
			return this.taskService.getProcessInstanceComments(processInstanceId, type);
		} else {
			return this.taskService.getProcessInstanceComments(processInstanceId);
		}
	}

	/**
	 * Add a new attachment to a task and/or a process instance and use an input stream to provide the content
	 */
	public createAttachment(attachmentType: string, taskId: string, processInstanceId: string, attachmentName: string, attachmentDescription: string, content?: any[], url?: string): Attachment {
		if (this.isNotNull(url)) {
			return this.taskService.createAttachment(attachmentType, taskId, processInstanceId, attachmentName, attachmentDescription, url);
		} else {
			return this.taskService.createAttachment(attachmentType, taskId, processInstanceId, attachmentName, attachmentDescription, Streams.createByteArrayInputStream(content));
		}
	}

	/** Update the name and description of an attachment */
	public saveAttachment(attachment: Attachment): void {
		this.taskService.saveAttachment(attachment);
	}

	/** Retrieve a particular attachment */
	public getAttachment(attachmentId: string): Attachment {
		return this.taskService.getAttachment(attachmentId);
	}

	/** Retrieve stream content of a particular attachment */
	public getAttachmentContent(attachmentId: string): any[] {
		const content = this.taskService.getAttachmentContent(attachmentId);
		const baos = Streams.createByteArrayOutputStream();
		Streams.copyLarge(content, baos);
		return baos.getBytes()
	};

	/** The list of attachments associated to a task */
	public getTaskAttachments(taskId: string): Attachment[] {
		return this.taskService.getTaskAttachments(taskId);
	}

	/** The list of attachments associated to a process instance */
	public getProcessInstanceAttachments(processInstanceId: string): Attachment[] {
		return this.taskService.getProcessInstanceAttachments(processInstanceId);
	}

	/** Delete an attachment */
	public deleteAttachment(attachmentId: string): void {
		this.taskService.deleteAttachment(attachmentId);
	}

	/** The list of subtasks for this parent task */
	public getSubTasks(parentTaskId: string): Task[] {
		return this.taskService.getSubTasks(parentTaskId);
	}

	private isNotNull(property: any): boolean {
		return property !== null && property !== undefined;
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

/**
 * Represents one task for a human user.
 */
export interface Task extends TaskInfo {

	/** Name or title of the task. */
	setName(name: string): void;

	/** Sets an optional localized name for the task. */
	setLocalizedName(name: string): void;

	/** Change the description of the task */
	setDescription(description: string): void;

	/** Sets an optional localized description for the task. */
	setLocalizedDescription(description: string): void;

	/** Sets the indication of how important/urgent this task is */
	setPriority(priority: number): void;

	/**
	 * The user id of the person that is responsible for this task.
	 */
	setOwner(owner: string): void;

	/**
	 * The user id of the person to which this task is delegated.
	 */
	setAssignee(assignee: string): void;

	/** The current {@link DelegationState} for this task. */
	getDelegationState(): DelegationState;

	/** The current {@link DelegationState} for this task. */
	setDelegationState(delegationState: DelegationState): void;

	/** Change due date of the task. */
	setDueDate(dueDate: Date): void;

	/**
	 * Change the category of the task. This is an optional field and allows to 'tag' tasks as belonging to a certain category.
	 */
	setCategory(category: string): void;

	/** the parent task for which this task is a subtask */
	setParentTaskId(parentTaskId: string): void;

	/** Change the tenantId of the task */
	setTenantId(tenantId: string): void;

	/** Change the form key of the task */
	setFormKey(formKey: string): void;

	/** Indicates whether this task is suspended or not. */
	isSuspended(): boolean;

}

export interface TaskInfo {

	/** DB id of the task. */
	getId(): string;

	/**
	 * Name or title of the task.
	 */
	getName(): string;

	/**
	 * Free text description of the task.
	 */
	getDescription(): string;

	/**
	 * Indication of how important/urgent this task is
	 */
	getPriority(): number;

	/**
	 * The user id of the person that is responsible for this task.
	 */
	getOwner(): string;

	/**
	 * The user id of the person to which this task is delegated.
	 */
	getAssignee(): string;

	/**
	 * Reference to the process instance or null if it is not related to a process instance.
	 */
	getProcessInstanceId(): string;

	/**
	 * Reference to the path of execution or null if it is not related to a process instance.
	 */
	getExecutionId(): string;

	/**
	 * Reference to the task definition or null if it is not related to any task definition.
	 */
	getTaskDefinitionId(): string;

	/**
	 * Reference to the process definition or null if it is not related to a process.
	 */
	getProcessDefinitionId(): string;

	/**
	 * Reference to a scope identifier or null if none is set (e.g. for bpmn process task it is null)
	 */
	getScopeId(): string;

	/**
	 * Reference to a sub scope identifier or null if none is set (e.g. for bpmn process task it is null)
	 */
	getSubScopeId(): string;

	/**
	 * Reference to a scope type or null if none is set (e.g. for bpmn process task it is null)
	 */
	getScopeType(): string;

	/**
	 * Reference to a scope definition identifier or null if none is set (e.g. for bpmn process task it is null)
	 */
	getScopeDefinitionId(): string;

	/**
	 * If this task runs in the context of a case and stage, this method returns it's closest parent stage instance id (the stage plan item instance id to be
	 * precise). Even if the direct parent of the task is a process which itself might have been created out of a process task of a case, its stage instance
	 * is reflected in the task.
	 *
	 * @return the stage instance id this task belongs to or null, if this task is not part of a case at all or is not a child element of a stage
	 */
	getPropagatedStageInstanceId(): string;

	/**
	 * The state of this task
	 */
	getState(): string;

	/** The date/time when this task was created */
	getCreateTime(): Date;

	/** The date/time when this task was put in progress */
	getInProgressStartTime(): Date;

	/**
	 * The user reference that put this task in progress
	 */
	getInProgressStartedBy(): string;

	/**
	 * The claim time of this task
	 */
	getClaimTime(): Date;

	/**
	 * The user reference that claimed this task
	 */
	getClaimedBy(): String;

	/**
	 * The suspended time of this task
	 */
	getSuspendedTime(): Date;

	/**
	 * The user reference that suspended this task
	 */
	getSuspendedBy(): string;

	/**
	 * The id of the activity in the process defining this task or null if this is not related to a process
	 */
	getTaskDefinitionKey(): string;

	/**
	 * In progress start due date of the task.
	 */
	getInProgressStartDueDate(): Date;

	/**
	 * Due date of the task.
	 */
	getDueDate(): Date;

	/**
	 * The category of the task. This is an optional field and allows to 'tag' tasks as belonging to a certain category.
	 */
	getCategory(); string;

	/**
	 * The parent task for which this task is a subtask
	 */
	getParentTaskId(): string;

	/**
	 * The tenant identifier of this task
	 */
	getTenantId(): string;

	/**
	 * The form key for the user task
	 */
	getFormKey(): string;

	/**
	 * Returns the local task variables if requested in the task query
	 */
	getTaskLocalVariables(): Map<string, Object>;

	/**
	 * Returns the process variables if requested in the task query
	 */
	getProcessVariables(): Map<string, Object>;

	/**
	 * Returns the case variables if requested in the task query
	 */
	getCaseVariables(): Map<string, Object>;

	/**
	 * Returns the identity links.
	 */
	getIdentityLinks(): IdentityLinkInfo[];
}

export interface IdentityLinkInfo {
	getType(): string;

	getUserId(): string;

	getGroupId(): string;

	getTaskId(): string;

	getProcessInstanceId(): string;

	getScopeId(): string;

	getSubScopeId(): string;

	getScopeType(): string;

	getScopeDefinitionId(): string;
}

export interface IdentityLink extends IdentityLinkInfo {

	/**
	 * The process definition id associated with this identity link.
	 */
	getProcessDefinitionId(): string;

}

export enum DelegationState {

	/**
	 * The owner delegated the task and wants to review the result after the assignee has resolved the task. When the assignee completes the task, the task is marked as {@link #RESOLVED} and sent back
	 * to the owner. When that happens, the owner is set as the assignee so that the owner gets this task back in the ToDo.
	 */
	PENDING,

	/**
	 * The assignee has resolved the task, the assignee was set to the owner again and the owner now finds this task back in the ToDo list for review. The owner now is able to complete the task.
	 */
	RESOLVED
}

/**
 * Wraps {@link TaskInfo} to the builder.
 * 
 */
export interface TaskBuilder {

	/**
	 * Creates task instance according values set in the builder
	 * 
	 * @return task instance
	 */
	create(): Task;

	/**
	 * DB id of the task.
	 */
	id(id: string): TaskBuilder;
	getId(): string;

	/**
	 * Name or title of the task.
	 */
	name(name: string): TaskBuilder;
	getName(): string;

	/**
	 * Free text description of the task.
	 */
	description(description: string): TaskBuilder;
	getDescription(): string;

	/**
	 * Indication of how important/urgent this task is
	 */
	priority(priority: number): TaskBuilder;
	getPriority(): number;

	/**
	 * The userId of the person that is responsible for this task.
	 */
	owner(ownerId: string): TaskBuilder;
	getOwner(): string;

	/**
	 * The userId of the person to which this task is delegated.
	 */
	assignee(assigneId: string): TaskBuilder;
	getAssignee(): string;

	/**
	 * Change due date of the task.
	 */
	dueDate(dueDate: Date): TaskBuilder;
	getDueDate(): Date;

	/**
	 * Change the category of the task. This is an optional field and allows to 'tag' tasks as belonging to a certain category.
	 */
	category(category: string): TaskBuilder;
	getCategory(): string;

	/**
	 * the parent task for which this task is a subtask
	 */
	parentTaskId(parentTaskId: string): TaskBuilder;
	getParentTaskId(): string;

	/**
	 * Change the tenantId of the task
	 */
	tenantId(tenantId: string): TaskBuilder;
	getTenantId(): string;

	/**
	 * Change the form key of the task
	 */
	formKey(formKey: string): TaskBuilder;
	getFormKey(): string;

	/**
	 * task definition id to create task from
	 */
	taskDefinitionId(taskDefinitionId: string): TaskBuilder;
	getTaskDefinitionId(): string;

	/**
	 * task definition key to create task from
	 */
	taskDefinitionKey(taskDefinitionKey: string): TaskBuilder;
	getTaskDefinitionKey(): string;

	/**
	 * add identity links to the task
	 */
	identityLinks(identityLinks: Set<IdentityLinkInfo>): TaskBuilder;
	getIdentityLinks(): Set<IdentityLinkInfo>;

	/**
	 * add task scopeId
	 */
	scopeId(scopeId: string): TaskBuilder;
	getScopeId(): string;

	/**
	 * Add scope type
	 */
	scopeType(scopeType: string): TaskBuilder;
	getScopeType(): string;
}

export interface FormInfo {

	getId(): string;

	setId(id: string): void;

	getName(): string;

	setName(name: string): void;

	getDescription(): string;

	setDescription(description: string): void;

	getKey(): string;

	setKey(key: string): void;

	getVersion(): number;

	setVersion(version: number): void;

	getFormModel(): any;

	setFormModel(formModel: any): void;
}

export interface ValueFields {

	/**
	 * @return the name of the variable
	 */
	getName(): string;

	/**
	 * @return the process instance id of the variable
	 */
	getProcessInstanceId(): string;

	/**
	 * @return the execution id of the variable
	 */
	getExecutionId(): string;

	/**
	 * @return the scope id of the variable
	 */
	getScopeId(): string;

	/**
	 * @return the sub scope id of the variable
	 */
	getSubScopeId(): string;

	/**
	 * @return the scope type of the variable
	 */
	getScopeType(): string;

	/**
	 * @return the task id of the variable
	 */
	getTaskId(): string;

	/**
	 * @return the first text value, if any, or null.
	 */
	getTextValue(): string;

	/**
	 * Sets the first text value. A value of null is allowed.
	 */
	setTextValue(textValue: string): void;

	/**
	 * @return the second text value, if any, or null.
	 */
	getTextValue2(): string;

	/**
	 * Sets second text value. A value of null is allowed.
	 */
	setTextValue2(textValue2: string): void;

	/**
	 * @return the long value, if any, or null.
	 */
	getLongValue(): number;

	/**
	 * Sets the long value. A value of null is allowed.
	 */
	setLongValue(longValue: number): void;

	/**
	 * @return the double value, if any, or null.
	 */
	getDoubleValue(): number;

	/**
	 * Sets the double value. A value of null is allowed.
	 */
	setDoubleValue(doubleValue: number): void;

	/**
	 * @return the byte array value, if any, or null.
	 */
	getBytes(): any[];

	/**
	 * Sets the byte array value. A value of null is allowed.
	 */
	setBytes(bytes: any[]): void;

	getCachedValue(): any;

	setCachedValue(cachedValue: any): void;

}

export interface VariableInstance extends ValueFields {

	getId(): string;

	setId(id: string): void;

	setName(name: string): void;

	setExecutionId(executionId: string): void;

	setProcessInstanceId(processInstanceId: string): void;

	setProcessDefinitionId(processDefinitionId: string): void;

	getProcessDefinitionId(): string;

	getValue(): any;

	setValue(value: any): void;

	getTypeName(): string;

	setTypeName(typeName: string): void;

	isReadOnly(): boolean;

	setTaskId(taskId: string): void;

	setScopeId(scopeId: string): void;

	setSubScopeId(subScopeId: string): void;

	setScopeType(scopeType: string): void;

	setScopeDefinitionId(scopeDefinitionId: string): void;

	getScopeDefinitionId(): string;

	getMetaInfo(): string;

	setMetaInfo(metaInfo: string): void;

}

export interface DataObject {

	/**
	 * The unique id of this Data Object.
	 */
	getId(): string;

	/**
	 /**
	 * The id of the process instance that this Data Object is associated with.
	 */
	getProcessInstanceId(): string;

	/**
	 * The id of the execution in which this Data Object resides. A DataObject only resides on a process instance
	 * execution or a subprocess execution.
	 */
	getExecutionId(): string;

	/**
	 * Name of the DataObject.
	 */
	getName(): string;

	/**
	 * Localized Name of the DataObject.
	 */
	getLocalizedName(): string;

	/**
	 * Description of the DataObject.
	 */
	getDescription(): string;

	/**
	 * Value of the DataObject.
	 */
	getValue(): any;

	/**
	 * Type of the DataObject.
	 */
	getType(): string;

	/**
	 * The id of the flow element in the process defining this data object.
	 */
	getDataObjectDefinitionKey(): string;
}

export interface Comment {

	/** unique identifier for this comment */
	getId(): string;

	/** reference to the user that made the comment */
	getUserId(): string;

	setUserId(userId: string): void;

	getTime(): Date;

	setTime(time: Date);

	/** reference to the task on which this comment was made */
	getTaskId(): string;

	setTaskId(taskId: string): void;

	/** reference to the process instance on which this comment was made */
	getProcessInstanceId(): string;

	setProcessInstanceId(processInstanceId: string): void;

	/** reference to the type given to the comment */
	getType(): string;

	setType(type: string): void;

	/**
	 * the full comment message the user had related to the task and/or process instance
	 * 
	 * @see TaskService#getTaskComments(String)
	 */
	getFullMessage(): string;

	setFullMessage(fullMessage: string): void;
}

export interface TaskEvent {

	/** Unique identifier for this event */
	getId(): string;

	/**
	 * Indicates the type of of action and also indicates the meaning of the parts as exposed in {@link #getMessageParts()}
	 */
	getAction(): string;

	/**
	 * The meaning of the message parts is defined by the action as you can find in {@link #getAction()}
	 */
	getMessageParts(): string[];

	/**
	 * The message that can be used in case this action only has a single message part.
	 */
	getMessage(): string;

	/** reference to the user that made the comment */
	getUserId(): string;

	/** time and date when the user made the comment */
	getTime(): Date;

	/** reference to the task on which this comment was made */
	getTaskId(): string;

	/** reference to the process instance on which this comment was made */
	getProcessInstanceId(): string;

}

export interface Attachment {

	/** unique id for this attachment */
	getId(): string;

	/** free user defined short (max 255 chars) name for this attachment */
	getName(): string;

	/** free user defined short (max 255 chars) name for this attachment */
	setName(name: string): void;

	/**
	 * long (max 255 chars) explanation what this attachment is about in context of the task and/or process instance it's linked to.
	 */
	getDescription(): string;

	/**
	 * long (max 255 chars) explanation what this attachment is about in context of the task and/or process instance it's linked to.
	 */
	setDescription(description: string): void;

	/**
	 * indication of the type of content that this attachment refers to. Can be mime type or any other indication.
	 */
	getType(): string;

	/** reference to the task to which this attachment is associated. */

	getTaskId(): string;
	/**
	 * reference to the process instance to which this attachment is associated.
	 */
	getProcessInstanceId(): string;

	/**
	 * the remote URL in case this is remote content. If the attachment content was {@link TaskService#createAttachment(String, String, String, String, String, java.io.InputStream) uploaded with an
	 * input stream}, then this method returns null and the content can be fetched with {@link TaskService#getAttachmentContent(String)}.
	 */
	getUrl(): string;

	/** reference to the user who created this attachment. */
	getUserId(): string;

	/** timestamp when this attachment was created */
	getTime(): Date;

	/** timestamp when this attachment was created */
	setTime(time: Date): void;

	/** the id of the byte array entity storing the content */
	getContentId(): string;

}

export enum NullHandlingOnOrder {
	NULLS_FIRST, NULLS_LAST
}

export interface QueryProperty {

	getName(): string;
}

export interface Query<T, U> {

	/**
	 * Order the results ascending on the given property as defined in this class (needs to come after a call to one of the orderByXxxx methods).
	 */
	asc(): T;

	/**
	 * Order the results descending on the given property as defined in this class (needs to come after a call to one of the orderByXxxx methods).
	 */
	desc(): T;

	orderBy(property: QueryProperty): T;

	orderBy(property: QueryProperty, nullHandlingOnOrder: NullHandlingOnOrder): T;

	/**
	 * Executes the query and returns the number of results
	 */
	count(): number;

	/**
	 * Executes the query and returns the resulting entity or null if no entity matches the query criteria.
	 *
	 * @throws org.flowable.common.engine.api.FlowableException when the query results in more than one entities.
	 */
	singleResult(): U;

	/**
	 * Executes the query and get a list of entities as the result.
	 */
	list(): U[];

	/**
	 * Executes the query and get a list of entities as the result.
	 */
	listPage(firstResult: number, maxResults: number): U[];
}


export interface TaskInfoQuery<T, V extends TaskInfo> extends Query<T, V> {

	/**
	 * Only select tasks with the given task id (in practice, there will be maximum one of this kind)
	 */
	taskId(taskId: string): T;

	/**
	 * Only select tasks with an id that is in the given list
	 *
	 * @throws FlowableIllegalArgumentException
	 *             When passed id list is empty or <code>null</code> or contains <code>null String</code>.
	 */
	taskIds(taskIds: string[]): T;

	/** Only select tasks with the given name */
	taskName(name: string): T;

	/**
	 * Only select tasks with a name that is in the given list
	 * 
	 * @throws FlowableIllegalArgumentException
	 *             When passed name list is empty or <code>null</code> or contains <code>null String</code>.
	 */
	taskNameIn(nameList: string[]): T;

	/**
	 * Only select tasks with a name that is in the given list
	 * 
	 * This method, unlike the {@link #taskNameIn(Collection)} method will not take in account the upper/lower case: both the input parameters as the column value are lowercased when the query is executed.
	 * 
	 * @throws FlowableIllegalArgumentException
	 *             When passed name list is empty or <code>null</code> or contains <code>null String</code>.
	 */
	taskNameInIgnoreCase(nameList: string[]): T;

	/**
	 * Only select tasks with a name matching the parameter. The syntax is that of SQL: for example usage: nameLike(%test%)
	 */
	taskNameLike(nameLike: string): T;

	/**
	 * Only select tasks with a name matching the parameter. The syntax is that of SQL: for example usage: nameLike(%test%)
	 * 
	 * This method, unlike the {@link #taskNameLike(String)} method will not take in account the upper/lower case: both the input parameter as the column value are lowercased when the query is
	 * executed.
	 */
	taskNameLikeIgnoreCase(nameLike: string): T;

	/** Only select tasks with the given description. */
	taskDescription(description: string): T;

	/**
	 * Only select tasks with a description matching the parameter . The syntax is that of SQL: for example usage: descriptionLike(%test%)
	 */
	taskDescriptionLike(descriptionLike: string): T;

	/**
	 * Only select tasks with a description matching the parameter . The syntax is that of SQL: for example usage: descriptionLike(%test%)
	 * 
	 * This method, unlike the {@link #taskDescriptionLike(String)} method will not take in account the upper/lower case: both the input parameter as the column value are lowercased when the query is
	 * executed.
	 */
	taskDescriptionLikeIgnoreCase(descriptionLike: string): T;

	/** Only select tasks with the given priority. */
	taskPriority(priority: number): T;

	/** Only select tasks with the given priority or higher. */
	taskMinPriority(minPriority: number): T;

	/** Only select tasks with the given priority or lower. */
	taskMaxPriority(maxPriority: number): T;

	/** Only select tasks which are assigned to the given user. */
	taskAssignee(assignee: string): T;

	/**
	 * Only select tasks which were last assigned to an assignee like the given value. The syntax that should be used is the same as in SQL, eg. %test%.
	 */
	taskAssigneeLike(assigneeLike: string): T;

	/**
	 * Only select tasks which were last assigned to an assignee like the given value. The syntax that should be used is the same as in SQL, eg. %test%.
	 * 
	 * This method, unlike the {@link #taskAssigneeLike(String)} method will not take in account the upper/lower case: both the input parameter as the column value are lowercased when the query is
	 * executed.
	 */
	taskAssigneeLikeIgnoreCase(assigneeLikeIgnoreCase: string): T;

	/** Only select tasks which don't have an assignee. */
	taskUnassigned(): T;

	/** Only select tasks which are assigned to any user */
	taskAssigned(): T;

	/**
	 * Only select tasks with an assignee that is in the given list
	 * 
	 * @throws FlowableIllegalArgumentException
	 *             When passed name list is empty or <code>null</code> or contains <code>null String</code>.
	 */
	taskAssigneeIds(assigneeListIds: string[]): T;

	/** Only select tasks for which the given user is the owner. */
	taskOwner(owner: string): T;

	/**
	 * Only select tasks which were last assigned to an owner like the given value. The syntax that should be used is the same as in SQL, eg. %test%.
	 */
	taskOwnerLike(ownerLike: string): T;

	/**
	 * Only select tasks which were last assigned to an owner like the given value. The syntax that should be used is the same as in SQL, eg. %test%.
	 * 
	 * This method, unlike the {@link #taskOwnerLike(String)} method will not take in account the upper/lower case: both the input parameter as the column value are lowercased when the query is
	 * executed.
	 */
	taskOwnerLikeIgnoreCase(ownerLikeIgnoreCase: string): T;

	/** Only select tasks for which the given user is a candidate. */
	taskCandidateUser(candidateUser: string): T;

	/**
	 * Only select tasks for which there exist an {@link IdentityLink} with the given user, including tasks which have been assigned to the given user (assignee) or owned by the given user (owner).
	 */
	taskInvolvedUser(involvedUser: string): T;

	/**
	 * Only select tasks for which there exist an {@link IdentityLink} with the given Groups.
	 */
	taskInvolvedGroups(involvedGroup: string[]): T;

	/**
	 * Allows to select a task using {@link #taskCandidateGroup(String)} {@link #taskCandidateGroupIn(Collection)} or {@link #taskCandidateUser(String)} but ignore the assignee value instead of querying for an empty assignee.
	 */
	ignoreAssigneeValue(): T;

	/** Only select tasks for which users in the given group are candidates. */
	taskCandidateGroup(candidateGroup: string): T;

	/**
	 * Only select tasks for which the 'candidateGroup' is one of the given groups.
	 * 
	 * @throws FlowableIllegalArgumentException
	 *             When query is executed and {@link #taskCandidateGroup(String)} or {@link #taskCandidateUser(String)} has been executed on the query instance. When passed group list is empty or
	 *             <code>null</code>.
	 */
	taskCandidateGroupIn(candidateGroups: string[]): T

	/**
	 * Only select tasks that have the given tenant id.
	 */
	taskTenantId(tenantId: string): T;

	/**
	 * Only select tasks with a tenant id like the given one.
	 */
	taskTenantIdLike(tenantIdLike: string): T;

	/**
	 * Only select tasks that do not have a tenant id.
	 */
	taskWithoutTenantId(): T;

	/**
	 * Only select tasks for the given process instance id.
	 */
	processInstanceId(processInstanceId: string): T;

	/**
	 * Only select tasks for the given process ids.
	 */
	processInstanceIdIn(processInstanceIds: string[]): T;

	/**
	 * Only select tasks without a process instance id.
	 */
	withoutProcessInstanceId(): T;

	/**
	 * Only select tasks for the given business key
	 */
	processInstanceBusinessKey(processInstanceBusinessKey: string): T;

	/**
	 * Only select tasks with a business key like the given value The syntax is that of SQL: for example usage: processInstanceBusinessKeyLike("%test%").
	 */
	processInstanceBusinessKeyLike(processInstanceBusinessKeyLike: string): T;

	/**
	 * Only select tasks with a business key like the given value The syntax is that of SQL: for example usage: processInstanceBusinessKeyLike("%test%").
	 * 
	 * This method, unlike the {@link #processInstanceBusinessKeyLike(String)} method will not take in account the upper/lower case: both the input parameter as the column value are lowercased when
	 * the query is executed.
	 */
	processInstanceBusinessKeyLikeIgnoreCase(processInstanceBusinessKeyLikeIgnoreCase: string): T;

	/**
	 * Only select tasks for the given execution.
	 */
	executionId(executionId: string): T;

	/**
	 * Only select tasks for the given case instance.
	 */
	caseInstanceId(caseInstanceId: string): T;

	/**
	 * Only select tasks for the given case definition.
	 */
	caseDefinitionId(caseDefinitionId: string): T;

	/**
	 * Only select tasks which are part of a case instance which has the given case definition key.
	 */
	caseDefinitionKey(caseDefinitionKey: string): T;

	/**
	 * Only select tasks which are part of a case instance which has a case definition key like the given value. The syntax that should be used is the same as in SQL, eg. %test%.
	 */
	caseDefinitionKeyLike(caseDefinitionKeyLike: string): T;

	/**
	 * Only select tasks which are part of a case instance which has a case definition key like the given value. The syntax that should be used is the same as in SQL, eg. %test%.
	 *
	 * This method, unlike the {@link #caseDefinitionKeyLike(String)} method will not take in account the upper/lower case: both the input parameter as the column value are lowercased when the
	 * query is executed.
	 */
	caseDefinitionKeyLikeIgnoreCase(caseDefinitionKeyLikeIgnoreCase: string): T;

	/** Only select tasks that have a case definition for which the key is present in the given list **/
	caseDefinitionKeyIn(caseDefinitionKeys: string[]): T;

	/**
	 * Only select tasks for the given plan item instance. 
	 */
	planItemInstanceId(planItemInstanceId: string): T;

	/**
	 * Only select tasks for the given scope identifier. 
	 */
	scopeId(scopeId: string): T;

	/**
	 * Only select tasks for the given sub scope identifier. 
	 */
	subScopeId(subScopeId: string): T;

	/**
	 * Only select tasks for the given scope type. 
	 */
	scopeType(scopeType: string): T;

	/**
	 * Only select tasks for the given scope definition identifier. 
	 */
	scopeDefinitionId(scopeDefinitionId: string): T;

	/**
	 * Only select tasks for the given stage, defined through its stage instance id.
	 */
	propagatedStageInstanceId(propagatedStageInstanceId: string): T;

	/**
	 * Select all tasks for the given process instance id and its children.
	 */
	processInstanceIdWithChildren(processInstanceId: string): T;

	/**
	 * Select all tasks for the given case instance id and its children.
	 */
	caseInstanceIdWithChildren(caseInstanceId: string): T;

	/**
	 * Only select tasks that are created on the given date.
	 */
	taskCreatedOn(createTime: Date): T;

	/**
	 * Only select tasks that are created before the given date.
	 */
	taskCreatedBefore(before: Date): T;

	/**
	 * Only select tasks that are created after the given date.
	 */
	taskCreatedAfter(after: Date): T;

	/**
	 * Only select tasks that are started in progress on the given date.
	 */
	taskInProgressStartTimeOn(claimedTime: Date): T;

	/**
	 * Only select tasks that are started in progress before the given date.
	 */
	taskInProgressStartTimeBefore(before: Date): T;

	/**
	 * Only select tasks that are started in progress after the given date.
	 */
	taskInProgressStartTimeAfter(after: Date): T;

	/**
	 * Select all tasks that have an in progress started user reference for the given value.
	 */
	taskInProgressStartedBy(startedBy: string): T;

	/**
	 * Only select tasks that are claimed on the given date.
	 */
	taskClaimedOn(claimedTime: Date): T;

	/**
	 * Only select tasks that are claimed before the given date.
	 */
	taskClaimedBefore(before: Date): T;

	/**
	 * Only select tasks that are claimed after the given date.
	 */
	taskClaimedAfter(after: Date): T;

	/**
	 * Select all tasks that have a claimed by user reference for the given value.
	 */
	taskClaimedBy(claimedBy: string): T;

	/**
	 * Only select tasks that are suspended on the given date.
	 */
	taskSuspendedOn(suspendedTime: Date): T;

	/**
	 * Only select tasks that are suspended before the given date.
	 */
	taskSuspendedBefore(before: Date): T;

	/**
	 * Only select tasks that are suspended after the given date.
	 */
	taskSuspendedAfter(after: Date): T;

	/**
	 * Select all tasks that have a suspended by user reference for the given value.
	 */
	taskSuspendedBy(suspendedBy: string): T;

	/**
	 * Only select tasks with the given category.
	 */
	taskCategory(category: string): T;

	/**
	 * Only select tasks belonging to one of the categories in the given list.
	 *
	 * @param taskCategoryInList
	 * @throws FlowableIllegalArgumentException When passed category list is empty or <code>null</code> or contains <code>null</code> String.
	 */
	taskCategoryIn(taskCategoryInList: string[]): T;

	/**
	 * Only select tasks with a defined category which do not belong to a category present in the given list.
	 * <p>
	 * NOTE: This method does <b>not</b> return tasks <b>without</b> category e.g. tasks having a <code>null</code> category.
	 * To include <code>null</code> categories, use <code>query.or().taskCategoryNotIn(...).taskWithoutCategory().endOr()</code>
	 * </p>
	 *
	 * @param taskCategoryNotInList
	 * @throws FlowableIllegalArgumentException When passed category list is empty or <code>null</code> or contains <code>null String</code>.
	 * @see #taskWithoutCategory
	 */
	taskCategoryNotIn(taskCategoryNotInList: string[]): T;

	/**
	 * Selects tasks without category.
	 * <p>
	 * Can also be used in conjunction with other filter criteria to include tasks without category e.g. in <code>or</code> queries.
	 * </p>
	 * @see #taskCategoryNotIn(Collection)
	 */
	taskWithoutCategory(): T;

	/**
	 * Only select tasks with form key.
	 */
	taskWithFormKey(): T;

	/**
	 * Only select tasks with the given formKey.
	 */
	taskFormKey(formKey: string): T;

	/**
	 * Only select tasks with the given taskDefinitionKey. The task definition key is the id of the userTask: &lt;userTask id="xxx" .../&gt;
	 **/
	taskDefinitionKey(key: string): T;

	/**
	 * Only select tasks with a taskDefinitionKey that match the given parameter. The syntax is that of SQL: for example usage: taskDefinitionKeyLike("%test%"). The task definition key is the id of
	 * the userTask: &lt;userTask id="xxx" .../&gt;
	 **/
	taskDefinitionKeyLike(keyLike: string): T;

	/**
	 * Only select tasks with the given taskDefinitionKeys. The task definition key is the id of the userTask: &lt;userTask id="xxx" .../&gt;
	 **/
	taskDefinitionKeys(keys: string[]): T;

	/**
	 * Only select tasks with the given state.
	 **/
	taskState(state: string): T;

	/**
	 * Only select tasks with the given in progress start due date.
	 */
	taskInProgressStartDueDate(dueDate: Date): T;

	/**
	 * Only select tasks which have an in progress start due date before the given date.
	 */
	taskInProgressStartDueBefore(dueDate: Date): T;

	/**
	 * Only select tasks which have an in progress start due date after the given date.
	 */
	taskInProgressStartDueAfter(dueDate: Date): T;

	/**
	 * Only select tasks with no in progress start due date.
	 */
	withoutTaskInProgressStartDueDate(): T;

	/**
	 * Only select tasks with the given due date.
	 */
	taskDueDate(dueDate: Date): T;

	/**
	 * Only select tasks which have a due date before the given date.
	 */
	taskDueBefore(dueDate: Date): T;

	/**
	 * Only select tasks which have a due date after the given date.
	 */
	taskDueAfter(dueDate: Date): T;

	/**
	 * Only select tasks with no due date.
	 */
	withoutTaskDueDate(): T;

	/**
	 * Only select tasks which are part of a process instance which has the given process definition key.
	 */
	processDefinitionKey(processDefinitionKey: string): T;

	/**
	 * Only select tasks which are part of a process instance which has a process definition key like the given value. The syntax that should be used is the same as in SQL, eg. %test%.
	 */
	processDefinitionKeyLike(processDefinitionKeyLike: string): T;

	/**
	 * Only select tasks which are part of a process instance which has a process definition key like the given value. The syntax that should be used is the same as in SQL, eg. %test%.
	 * 
	 * This method, unlike the {@link #processDefinitionKeyLike(String)} method will not take in account the upper/lower case: both the input parameter as the column value are lowercased when the
	 * query is executed.
	 */
	processDefinitionKeyLikeIgnoreCase(processDefinitionKeyLikeIgnoreCase: string): T;

	/** Only select tasks that have a process definition for which the key is present in the given list **/
	processDefinitionKeyIn(processDefinitionKeys: string[]): T;

	/**
	 * Only select tasks which created from the given task definition referenced by id.
	 */
	taskDefinitionId(taskDefinitionId: string): T;

	/**
	 * Only select tasks which are part of a process instance which has the given process definition id.
	 */
	processDefinitionId(processDefinitionId: string): T;

	/**
	 * Only select tasks which are part of a process instance which has the given process definition name.
	 */
	processDefinitionName(processDefinitionName: string): T;

	/**
	 * Only select tasks which are part of a process instance which has a process definition name like the given value. The syntax that should be used is the same as in SQL, eg. %test%.
	 */
	processDefinitionNameLike(processDefinitionNameLike: string): T;

	/**
	 * Only select tasks which are part of a process instance whose definition belongs to the category which is present in the given list.
	 * 
	 * @throws FlowableIllegalArgumentException
	 *             When passed category list is empty or <code>null</code> or contains <code>null String</code>.
	 * @param processCategoryInList
	 */
	processCategoryIn(processCategoryInList: string[]): T;

	/**
	 * Only select tasks which are part of a process instance whose definition does not belong to the category which is present in the given list.
	 * 
	 * @throws FlowableIllegalArgumentException
	 *             When passed category list is empty or <code>null</code> or contains <code>null String</code>.
	 * @param processCategoryNotInList
	 */
	processCategoryNotIn(processCategoryNotInList: string[]): T;

	/**
	 * Only select tasks which are part of a process instance which has the given deployment id.
	 */
	deploymentId(deploymentId: string): T;

	/**
	 * Only select tasks which are part of a process instance which has the given deployment id.
	 */
	deploymentIdIn(deploymentIds: string[]): T;

	/**
	 * Only select tasks which are related to a case instance for to the given deployment id.
	 */
	cmmnDeploymentId(cmmnDeploymentId: string): T;

	/**
	 * Only select tasks which are related to a case instances for the given deployment id.
	 */
	cmmnDeploymentIdIn(cmmnDeploymentIds: string[]): T;

	/**
	 * Only select tasks which don't have a scope id set.
	 */
	withoutScopeId(): T;

	/**
	 * Only select tasks which have a local task variable with the given name set to the given value.
	 */
	taskVariableValueEquals(variableName: string, variableValue: any): T;

	/**
	 * Only select tasks which have at least one local task variable with the given value.
	 */
	taskVariableValueEquals(variableValue: any): T;

	/**
	 * Only select tasks which have a local string variable with the given value, case insensitive.
	 * <p>
	 * This method only works if your database has encoding/collation that supports case-sensitive queries. For example, use "collate UTF-8" on MySQL and for MSSQL, select one of the case-sensitive
	 * Collations available (<a href="http://msdn.microsoft.com/en-us/library/ms144250(v=sql.105).aspx" >MSDN Server Collation Reference</a>).
	 * </p>
	 */
	taskVariableValueEqualsIgnoreCase(name: string, value: string): T;

	/**
	 * Only select tasks which have a local task variable with the given name, but with a different value than the passed value. Byte-arrays and {@link Serializable} objects (which are not primitive
	 * type wrappers) are not supported.
	 */
	taskVariableValueNotEquals(variableName: string, variableValue: any): T;

	/**
	 * Only select tasks which have a local string variable with is not the given value, case insensitive.
	 * <p>
	 * This method only works if your database has encoding/collation that supports case-sensitive queries. For example, use "collate UTF-8" on MySQL and for MSSQL, select one of the case-sensitive
	 * Collations available (<a href="http://msdn.microsoft.com/en-us/library/ms144250(v=sql.105).aspx" >MSDN Server Collation Reference</a>).
	 * </p>
	 */
	taskVariableValueNotEqualsIgnoreCase(name: string, value: string): T;

	/**
	 * Only select tasks which have a local variable value greater than the passed value when they ended. Booleans, Byte-arrays and {@link Serializable} objects (which are not primitive type wrappers)
	 * are not supported.
	 * 
	 * @param name
	 *            cannot be null.
	 * @param value
	 *            cannot be null.
	 */
	taskVariableValueGreaterThan(name: string, value: any): T;

	/**
	 * Only select tasks which have a local variable value greater than or equal to the passed value when they ended. Booleans, Byte-arrays and {@link Serializable} objects (which are not primitive
	 * type wrappers) are not supported.
	 * 
	 * @param name
	 *            cannot be null.
	 * @param value
	 *            cannot be null.
	 */
	taskVariableValueGreaterThanOrEqual(name: string, value: any): T;

	/**
	 * Only select tasks which have a local variable value less than the passed value when the ended.Booleans, Byte-arrays and {@link Serializable} objects (which are not primitive type wrappers) are
	 * not supported.
	 * 
	 * @param name
	 *            cannot be null.
	 * @param value
	 *            cannot be null.
	 */
	taskVariableValueLessThan(name: string, value: any): T;

	/**
	 * Only select tasks which have a local variable value less than or equal to the passed value when they ended. Booleans, Byte-arrays and {@link Serializable} objects (which are not primitive type
	 * wrappers) are not supported.
	 * 
	 * @param name
	 *            cannot be null.
	 * @param value
	 *            cannot be null.
	 */
	taskVariableValueLessThanOrEqual(name: string, value: any): T;

	/**
	 * Only select tasks which have a local variable value like the given value when they ended. This can be used on string variables only.
	 * 
	 * @param name
	 *            cannot be null.
	 * @param value
	 *            cannot be null. The string can include the wildcard character '%' to express like-strategy: starts with (string%), ends with (%string) or contains (%string%).
	 */
	taskVariableValueLike(name: string, value: string): T;

	/**
	 * Only select tasks which have a local variable value like the given value (case insensitive) when they ended. This can be used on string variables only.
	 * 
	 * @param name
	 *            cannot be null.
	 * @param value
	 *            cannot be null. The string can include the wildcard character '%' to express like-strategy: starts with (string%), ends with (%string) or contains (%string%).
	 */
	taskVariableValueLikeIgnoreCase(name: string, value: string): T;

	/**
	 * Only select tasks which have a local variable with the given name.
	 * 
	 * @param name
	 *            cannot be null.
	 */
	taskVariableExists(name: string): T;

	/**
	 * Only select tasks which does not have a local variable with the given name.
	 * 
	 * @param name
	 *            cannot be null.
	 */
	taskVariableNotExists(name: string): T;

	/**
	 * Only select tasks which are part of a process that has a variable with the given name set to the given value.
	 */
	processVariableValueEquals(variableName: string, variableValue: any): T;

	/**
	 * Only select tasks which are part of a process that has at least one variable with the given value.
	 */
	processVariableValueEquals(variableValue: any): T;

	/**
	 * Only select tasks which are part of a process that has a local string variable which is not the given value, case insensitive.
	 * <p>
	 * This method only works if your database has encoding/collation that supports case-sensitive queries. For example, use "collate UTF-8" on MySQL and for MSSQL, select one of the case-sensitive
	 * Collations available (<a href="http://msdn.microsoft.com/en-us/library/ms144250(v=sql.105).aspx" >MSDN Server Collation Reference</a>).
	 * </p>
	 */
	processVariableValueEqualsIgnoreCase(name: string, value: string): T;

	/**
	 * Only select tasks which have a variable with the given name, but with a different value than the passed value. Byte-arrays and {@link Serializable} objects (which are not primitive type
	 * wrappers) are not supported.
	 */
	processVariableValueNotEquals(variableName: string, variableValue: any): T;

	/**
	 * Only select tasks which are part of a process that has a string variable with the given value, case insensitive.
	 * <p>
	 * This method only works if your database has encoding/collation that supports case-sensitive queries. For example, use "collate UTF-8" on MySQL and for MSSQL, select one of the case-sensitive
	 * Collations available (<a href="http://msdn.microsoft.com/en-us/library/ms144250(v=sql.105).aspx" >MSDN Server Collation Reference</a>).
	 * </p>
	 */
	processVariableValueNotEqualsIgnoreCase(name: string, value: string): T;

	/**
	 * Only select tasks which have a global variable value greater than the passed value when they ended. Booleans, Byte-arrays and {@link Serializable} objects (which are not primitive type
	 * wrappers) are not supported.
	 * 
	 * @param name
	 *            cannot be null.
	 * @param value
	 *            cannot be null.
	 */
	processVariableValueGreaterThan(name: string, value: any): T;

	/**
	 * Only select tasks which have a global variable value greater than or equal to the passed value when they ended. Booleans, Byte-arrays and {@link Serializable} objects (which are not primitive
	 * type wrappers) are not supported.
	 * 
	 * @param name
	 *            cannot be null.
	 * @param value
	 *            cannot be null.
	 */
	processVariableValueGreaterThanOrEqual(name: string, value: any): T;

	/**
	 * Only select tasks which have a global variable value less than the passed value when the ended.Booleans, Byte-arrays and {@link Serializable} objects (which are not primitive type wrappers) are
	 * not supported.
	 * 
	 * @param name
	 *            cannot be null.
	 * @param value
	 *            cannot be null.
	 */
	processVariableValueLessThan(name: string, value: any): T;

	/**
	 * Only select tasks which have a global variable value less than or equal to the passed value when they ended. Booleans, Byte-arrays and {@link Serializable} objects (which are not primitive type
	 * wrappers) are not supported.
	 * 
	 * @param name
	 *            cannot be null.
	 * @param value
	 *            cannot be null.
	 */
	processVariableValueLessThanOrEqual(name: string, value: any): T;

	/**
	 * Only select tasks which have a global variable value like the given value when they ended. This can be used on string variables only.
	 * 
	 * @param name
	 *            cannot be null.
	 * @param value
	 *            cannot be null. The string can include the wildcard character '%' to express like-strategy: starts with (string%), ends with (%string) or contains (%string%).
	 */
	processVariableValueLike(name: string, value: string): T;

	/**
	 * Only select tasks which have a global variable value like the given value (case insensitive) when they ended. This can be used on string variables only.
	 * 
	 * @param name
	 *            cannot be null.
	 * @param value
	 *            cannot be null. The string can include the wildcard character '%' to express like-strategy: starts with (string%), ends with (%string) or contains (%string%).
	 */
	processVariableValueLikeIgnoreCase(name: string, value: string): T;

	/**
	 * Only select tasks which have a global variable with the given name.
	 * 
	 * @param name
	 *            cannot be null.
	 */
	processVariableExists(name: string): T;

	/**
	 * Only select tasks which does not have a global variable with the given name.
	 * 
	 * @param name
	 *            cannot be null.
	 */
	processVariableNotExists(name: string): T;

	/**
	 * Only select tasks which are part of a case that has a variable with the given name set to the given value.
	 */
	caseVariableValueEquals(variableName: string, variableValue: any): T;

	/**
	 * Only select tasks which are part of a case that has at least one variable with the given value.
	 */
	caseVariableValueEquals(variableValue: any): T;

	/**
	 * Only select tasks which are part of a case that has a local string variable which is not the given value, case insensitive.
	 * <p>
	 * This method only works if your database has encoding/collation that supports case-sensitive queries. For example, use "collate UTF-8" on MySQL and for MSSQL, select one of the case-sensitive
	 * Collations available (<a href="http://msdn.microsoft.com/en-us/library/ms144250(v=sql.105).aspx" >MSDN Server Collation Reference</a>).
	 * </p>
	 */
	caseVariableValueEqualsIgnoreCase(name: string, value: string): T;

	/**
	 * Only select tasks which have a variable with the given name, but with a different value than the passed value. Byte-arrays and {@link Serializable} objects (which are not primitive type
	 * wrappers) are not supported.
	 */
	caseVariableValueNotEquals(variableName: string, variableValue: any): T;

	/**
	 * Only select tasks which are part of a case that has a string variable with the given value, case insensitive.
	 * <p>
	 * This method only works if your database has encoding/collation that supports case-sensitive queries. For example, use "collate UTF-8" on MySQL and for MSSQL, select one of the case-sensitive
	 * Collations available (<a href="http://msdn.microsoft.com/en-us/library/ms144250(v=sql.105).aspx" >MSDN Server Collation Reference</a>).
	 * </p>
	 */
	caseVariableValueNotEqualsIgnoreCase(name: string, value: string): T;

	/**
	 * Only select tasks which have a global variable value greater than the passed value when they ended. Booleans, Byte-arrays and {@link Serializable} objects (which are not primitive type
	 * wrappers) are not supported.
	 *
	 * @param name cannot be null.
	 * @param value cannot be null.
	 */
	caseVariableValueGreaterThan(name: string, value: any): T;

	/**
	 * Only select tasks which have a global variable value greater than or equal to the passed value when they ended. Booleans, Byte-arrays and {@link Serializable} objects (which are not primitive
	 * type wrappers) are not supported.
	 *
	 * @param name cannot be null.
	 * @param value cannot be null.
	 */
	caseVariableValueGreaterThanOrEqual(name: string, value: any): T;

	/**
	 * Only select tasks which have a global variable value less than the passed value when the ended.Booleans, Byte-arrays and {@link Serializable} objects (which are not primitive type wrappers) are
	 * not supported.
	 *
	 * @param name cannot be null.
	 * @param value cannot be null.
	 */
	caseVariableValueLessThan(name: string, value: any): T;

	/**
	 * Only select tasks which have a global variable value less than or equal to the passed value when they ended. Booleans, Byte-arrays and {@link Serializable} objects (which are not primitive type
	 * wrappers) are not supported.
	 *
	 * @param name cannot be null.
	 * @param value cannot be null.
	 */
	caseVariableValueLessThanOrEqual(name: string, value: any): T;

	/**
	 * Only select tasks which have a global variable value like the given value when they ended. This can be used on string variables only.
	 *
	 * @param name cannot be null.
	 * @param value cannot be null. The string can include the wildcard character '%' to express like-strategy: starts with (string%), ends with (%string) or contains (%string%).
	 */
	caseVariableValueLike(name: string, value: string): T;

	/**
	 * Only select tasks which have a global variable value like the given value (case insensitive) when they ended. This can be used on string variables only.
	 *
	 * @param name cannot be null.
	 * @param value cannot be null. The string can include the wildcard character '%' to express like-strategy: starts with (string%), ends with (%string) or contains (%string%).
	 */
	caseVariableValueLikeIgnoreCase(name: string, value: string): T;

	/**
	 * Only select tasks which have a global variable with the given name.
	 *
	 * @param name cannot be null.
	 */
	caseVariableExists(name: string): T;

	/**
	 * Only select tasks which does not have a global variable with the given name.
	 *
	 * @param name cannot be null.
	 */
	caseVariableNotExists(name: string): T;

	/**
	 * Only selects tasks which with the given root scope id
	 */
	taskRootScopeId(parentScopeId: string): T;

	/**
	 * Only selects tasks which with the given parent scope id
	 */
	taskParentScopeId(parentScopeId: string): T;

	/**
	 * Include local task variables in the task query result
	 */
	includeTaskLocalVariables(): T;

	/**
	 * Include global process variables in the task query result
	 */
	includeProcessVariables(): T;

	/**
	 * Include global case variables in the task query result
	 */
	includeCaseVariables(): T;

	/**
	 * Include identity links in the task query result
	 */
	includeIdentityLinks(): T;

	/**
	 * Localize task name and description to specified locale.
	 */
	locale(locale: string): T;

	/**
	 * Instruct localization to fallback to more general locales including the default locale of the JVM if the specified locale is not found.
	 */
	withLocalizationFallback(): T;

	/**
	 * All query clauses called will be added to a single or-statement. This or-statement will be included with the other already existing clauses in the query, joined by an 'and'.
	 * <p>
	 * Calling endOr() will add all clauses to the regular query again. Calling or() after or() has been called or calling endOr() after endOr() has been called will result in an exception.
	 * It is possible to call or() endOr() several times if each or() has a matching endOr(), e.g.:
	 * </p>
	 * {@code query.<ConditionA>}
	 * {@code  .or().<conditionB>.<conditionC>.endOr()}
	 * {@code  .<conditionD>.<conditionE>}
	 * {@code  .or().<conditionF>.<conditionG>.endOr()}
	 * <p>
	 * will result in: conditionA &amp; (conditionB | conditionC) &amp; conditionD &amp; conditionE &amp; (conditionF | conditionG)
	 */
	or(): T;

	endOr(): T;

	// ORDERING

	/**
	 * Order by task id (needs to be followed by {@link #asc()} or {@link #desc()}).
	 */
	orderByTaskId(): T;

	/**
	 * Order by task name (needs to be followed by {@link #asc()} or {@link #desc()}).
	 */
	orderByTaskName(): T;

	/**
	 * Order by description (needs to be followed by {@link #asc()} or {@link #desc()}).
	 */
	orderByTaskDescription(): T;

	/**
	 * Order by priority (needs to be followed by {@link #asc()} or {@link #desc()}).
	 */
	orderByTaskPriority(): T;

	/**
	 * Order by assignee (needs to be followed by {@link #asc()} or {@link #desc()}).
	 */
	orderByTaskAssignee(): T;

	/**
	 * Order by the time on which the tasks were created (needs to be followed by {@link #asc()} or {@link #desc()}).
	 */
	orderByTaskCreateTime(): T;

	/**
	 * Order by process instance id (needs to be followed by {@link #asc()} or {@link #desc()}).
	 */
	orderByProcessInstanceId(): T;

	/**
	 * Order by execution id (needs to be followed by {@link #asc()} or {@link #desc()}).
	 */
	orderByExecutionId(): T;

	/**
	 * Order by process definition id (needs to be followed by {@link #asc()} or {@link #desc()}).
	 */
	orderByProcessDefinitionId(): T;

	/**
	 * Order by task due date (needs to be followed by {@link #asc()} or {@link #desc()}).
	 */
	orderByTaskDueDate(): T;

	/**
	 * Order by task owner (needs to be followed by {@link #asc()} or {@link #desc()}).
	 */
	orderByTaskOwner(): T;

	/**
	 * Order by task definition key (needs to be followed by {@link #asc()} or {@link #desc()}).
	 */
	orderByTaskDefinitionKey(): T;

	/**
	 * Order by tenant id (needs to be followed by {@link #asc()} or {@link #desc()}).
	 */
	orderByTenantId(): T;

	/**
	 * Order by due date (needs to be followed by {@link #asc()} or {@link #desc()}). If any of the tasks have null for the due date, these will be first in the result.
	 */
	orderByDueDateNullsFirst(): T;

	/**
	 * Order by due date (needs to be followed by {@link #asc()} or {@link #desc()}). If any of the tasks have null for the due date, these will be last in the result.
	 */
	orderByDueDateNullsLast(): T;

	/**
	 * Order by category (needs to be followed by {@link #asc()} or {@link #desc()}).
	 */
	orderByCategory(): T;

}

// @ts-ignore
if (typeof module !== 'undefined') {
	// @ts-ignore
	module.exports = Tasks;
}
