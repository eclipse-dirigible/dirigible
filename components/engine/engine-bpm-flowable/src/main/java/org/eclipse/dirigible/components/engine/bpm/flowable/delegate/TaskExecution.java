package org.eclipse.dirigible.components.engine.bpm.flowable.delegate;

import com.google.gson.reflect.TypeToken;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.FlowableListener;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.ExecutionListener;
import org.flowable.engine.delegate.ReadOnlyDelegateExecution;

import java.util.List;
import java.util.Optional;

public class TaskExecution {

    private final DelegateExecution delegateExecution;

    TaskExecution(DelegateExecution delegateExecution) {
        this.delegateExecution = delegateExecution;
    }

    /**
     * Unique id of this path of execution that can be used as a handle to provide external signals back
     * into the engine after wait states.
     */
    public String getId() {
        return delegateExecution.getId();
    }

    /** Reference to the overall process instance */
    public String getProcessInstanceId() {
        return delegateExecution.getProcessInstanceId();
    }

    /**
     * The 'root' process instance. When using call activity for example, the processInstance set will
     * not always be the root. This method returns the topmost process instance.
     */
    public String getRootProcessInstanceId() {
        return delegateExecution.getRootProcessInstanceId();
    }

    /**
     * Will contain the event name in case this execution is passed in for an {@link ExecutionListener}.
     */
    public String getEventName() {
        return delegateExecution.getEventName();
    }

    /**
     * Sets the current event (typically when execution an {@link ExecutionListener}).
     */
    public void setEventName(String eventName) {
        delegateExecution.setEventName(eventName);
    }

    /**
     * The business key for the process instance this execution is associated with.
     */
    public String getProcessInstanceBusinessKey() {
        return delegateExecution.getProcessInstanceBusinessKey();
    }

    /**
     * The business status for the process instance this execution is associated with.
     */
    public String getProcessInstanceBusinessStatus() {
        return delegateExecution.getProcessInstanceBusinessStatus();
    }

    /**
     * The process definition key for the process instance this execution is associated with.
     */
    public String getProcessDefinitionId() {
        return delegateExecution.getProcessDefinitionId();
    }

    /**
     * If this execution runs in the context of a case and stage, this method returns it's closest
     * parent stage instance id (the stage plan item instance id to be precise).
     *
     * @return the stage instance id this execution belongs to or null, if this execution is not part of
     *         a case at all or is not a child element of a stage
     */
    public String getPropagatedStageInstanceId() {
        return delegateExecution.getPropagatedStageInstanceId();
    }

    /**
     * Gets the id of the parent of this execution. If null, the execution represents a
     * process-instance.
     */
    public String getParentId() {
        return delegateExecution.getParentId();
    }

    /**
     * Gets the id of the calling execution. If not null, the execution is part of a subprocess.
     */
    public String getSuperExecutionId() {
        return delegateExecution.getSuperExecutionId();
    }

    /**
     * Gets the id of the current activity.
     */
    public String getCurrentActivityId() {
        return delegateExecution.getCurrentActivityId();
    }

    /**
     * Gets the name of the current activity.
     */
    public String getCurrentActivityName() {
        return delegateExecution.getCurrentActivityName();
    }

    /**
     * Returns the tenant id, if any is set before on the process definition or process instance.
     */
    public String getTenantId() {
        return delegateExecution.getTenantId();
    }

    /**
     * The BPMN element where the execution currently is at.
     */
    public FlowElement getCurrentFlowElement() {
        return delegateExecution.getCurrentFlowElement();
    }

    /**
     * Change the current BPMN element the execution is at.
     */
    public void setCurrentFlowElement(FlowElement flowElement) {
        delegateExecution.setCurrentFlowElement(flowElement);
    }

    /**
     * Returns the {@link FlowableListener} instance matching an {@link ExecutionListener} if currently
     * an execution listener is being execution. Returns null otherwise.
     */
    public FlowableListener getCurrentFlowableListener() {
        return delegateExecution.getCurrentFlowableListener();
    }

    /**
     * Called when an {@link ExecutionListener} is being executed.
     */
    public void setCurrentFlowableListener(FlowableListener currentListener) {
        delegateExecution.setCurrentFlowableListener(currentListener);
    }

    /**
     * Create a snapshot read only delegate execution of this delegate execution.
     *
     * @return a {@link ReadOnlyDelegateExecution}
     */
    public ReadOnlyDelegateExecution snapshotReadOnly() {
        return delegateExecution.snapshotReadOnly();
    }

    /**
     * returns the parent of this execution, or null if there no parent.
     */
    public TaskExecution getParent() {
        return new TaskExecution(delegateExecution.getParent());
    }

    /**
     * returns the list of execution of which this execution the parent of.
     */
    public List<? extends TaskExecution> getExecutions() {
        return delegateExecution.getExecutions()
                                .stream()
                                .map(de -> new TaskExecution(de))
                                .toList();
    }

    /* State management */

    /**
     * returns whether this execution is currently active.
     */
    public boolean isActive() {
        return delegateExecution.isActive();
    }

    /**
     * makes this execution active or inactive.
     */
    public void setActive(boolean isActive) {
        delegateExecution.setActive(isActive);
    }

    /**
     * returns whether this execution has ended or not.
     */
    public boolean isEnded() {
        return delegateExecution.isEnded();
    }

    /**
     * returns whether this execution is concurrent or not.
     */
    public boolean isConcurrent() {
        return delegateExecution.isConcurrent();
    }

    /**
     * changes the concurrent indicator on this execution.
     */
    public void setConcurrent(boolean isConcurrent) {
        delegateExecution.setConcurrent(isConcurrent);
    }

    /**
     * returns whether this execution is a process instance or not.
     */
    public boolean isProcessInstanceType() {
        return delegateExecution.isProcessInstanceType();
    }

    /**
     * Inactivates this execution. This is useful for example in a join: the execution still exists, but
     * it is not longer active.
     */
    public void inactivate() {
        delegateExecution.inactivate();
    }

    /**
     * Returns whether this execution is a scope.
     */
    public boolean isScope() {
        return delegateExecution.isScope();
    }

    /**
     * Changes whether this execution is a scope or not.
     */
    public void setScope(boolean isScope) {
        delegateExecution.setScope(isScope);
    }

    /**
     * Returns whether this execution is the root of a multi instance execution.
     */
    public boolean isMultiInstanceRoot() {
        return delegateExecution.isMultiInstanceRoot();
    }

    /**
     * Changes whether this execution is a multi instance root or not.
     *
     * @param isMultiInstanceRoot
     */
    public void setMultiInstanceRoot(boolean isMultiInstanceRoot) {
        delegateExecution.setMultiInstanceRoot(isMultiInstanceRoot);
    }

    public boolean hasVariables() {
        return delegateExecution.hasVariables();
    }

    public boolean hasVariablesLocal() {
        return delegateExecution.hasVariablesLocal();
    }

    public boolean hasVariable(String variableName) {
        return delegateExecution.hasVariable(variableName);
    }

    public boolean hasVariableLocal(String variableName) {
        return delegateExecution.hasVariableLocal(variableName);
    }

    public <T> T getMandatoryVariable(String variableName, TypeToken<T> type) {
        if (!delegateExecution.hasVariable(variableName)) {
            throw new InvalidVariableException("Missing mandatory variable name [" + variableName + "] of type " + type);
        }

        return getVariable(variableName, type).orElseThrow(
                () -> new InvalidVariableException("Missing mandatory variable name [" + variableName + "] of type " + type));
    }

    public <T> Optional<T> getVariable(String variableName, TypeToken<T> typeToken) {
        if (!delegateExecution.hasVariable(variableName)) {
            return Optional.empty();
        }
        Object raw = delegateExecution.getVariable(variableName);

        T deserializedValue = VariableValueSerializer.deserializeValue(raw, typeToken);
        return Optional.ofNullable(deserializedValue);
    }

    public void setVariable(String variableName, Object value) {
        Object serializedValue = VariableValueSerializer.serializeValue(value);
        delegateExecution.setVariable(variableName, serializedValue);
    }

    private boolean isPrimitiveWrapperOrString(Object value) {
        return value instanceof String || value instanceof Number || value instanceof Boolean;
    }

    public int getLoopCounter() {
        return getMandatoryVariable("loopCounter", Integer.class);
    }

    public <T> T getMandatoryVariable(String variableName, Class<T> type) {
        if (!delegateExecution.hasVariable(variableName)) {
            throw new InvalidVariableException("Missing mandatory variable name [" + variableName + "] of type " + type);
        }

        return getVariable(variableName, type).orElseThrow(
                () -> new InvalidVariableException("Missing mandatory variable name [" + variableName + "] of type " + type));
    }

    public <T> Optional<T> getVariable(String variableName, Class<T> type) {
        if (!delegateExecution.hasVariable(variableName)) {
            return Optional.empty();
        }

        Object raw = delegateExecution.getVariable(variableName);

        T deserializeValue = VariableValueSerializer.deserializeValue(raw, type);
        return Optional.ofNullable(deserializeValue);
    }

    public void removeVariable(String variableName) {
        delegateExecution.removeVariable(variableName);
    }
}
