package org.eclipse.dirigible.components.tracing;

import java.sql.Timestamp;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang3.StringUtils;

import com.google.gson.annotations.Expose;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;

/**
 * The Class TaskState.
 */
@Entity
@jakarta.persistence.Table(name = "DIRIGIBLE_TASK_STATE")
public class TaskState {

    /** The id. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "TASKSTATE_ID", nullable = false)
    private Long id;

    /** The task stauts. */
    @Column(name = "TASKSTATE_TYPE", columnDefinition = "VARCHAR", nullable = false, length = 20)
    @Expose
    @Enumerated(EnumType.STRING)
    protected TaskType type;

    /** The execution key. */
    @Column(name = "TASKSTATE_EXECUTION", columnDefinition = "VARCHAR", nullable = false, length = 255)
    @Expose
    protected String execution;

    /** The execution step. */
    @Column(name = "TASKSTATE_STEP", columnDefinition = "VARCHAR", nullable = false, length = 255)
    @Expose
    protected String step;

    /** The execution definition. */
    @Column(name = "TASKSTATE_DEFINITION", columnDefinition = "VARCHAR", nullable = true, length = 255)
    @Expose
    protected String definition;

    /** The execution instance. */
    @Column(name = "TASKSTATE_INSTANCE", columnDefinition = "VARCHAR", nullable = true, length = 255)
    @Expose
    protected String instance;

    /** The execution tenant. */
    @Column(name = "TASKSTATE_TENANT", columnDefinition = "VARCHAR", nullable = true, length = 255)
    @Expose
    protected String tenant;

    /** The execution tenant. */
    @Column(name = "TASKSTATE_THREAD", columnDefinition = "VARCHAR", nullable = true, length = 255)
    @Expose
    protected String thread;

    /** The execution started timestamp. */
    @Column(name = "TASKSTATE_STARTED", columnDefinition = "TIMESTAMP", nullable = false)
    @Expose
    protected Timestamp started;

    /** The execution started timestamp. */
    @Column(name = "TASKSTATE_ENDED", columnDefinition = "TIMESTAMP", nullable = true)
    @Expose
    protected Timestamp ended;

    /** The task stauts. */
    @Column(name = "TASKSTATE_STATUS", columnDefinition = "INTEGER", nullable = false)
    @Expose
    @Enumerated(EnumType.ORDINAL)
    protected TaskStatus status;

    /** The execution step. */
    @Column(name = "TASKSTATE_ERROR", columnDefinition = "VARCHAR", nullable = true, length = 2000)
    @Expose
    protected String error;

    /** The input. */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "DIRIGIBLE_TASK_STATE_INPUT",
            joinColumns = {@JoinColumn(name = "TASKSTATEIN_TASKSTATE_ID", referencedColumnName = "TASKSTATE_ID")})
    @MapKeyColumn(name = "TASKSTATEIN_NAME")
    @Column(name = "TASKSTATEIN_VALUE")
    private Map<String, String> input = new TreeMap<String, String>();

    /** The output. */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "DIRIGIBLE_TASK_STATE_OUTPUT",
            joinColumns = {@JoinColumn(name = "TASKSTATEOUT_TASKSTATE_ID", referencedColumnName = "TASKSTATE_ID")})
    @MapKeyColumn(name = "TASKSTATEOUT_NAME")
    @Column(name = "TASKSTATEOUT_VALUE")
    private Map<String, String> output = new TreeMap<String, String>();

    /** Differences between the input and output variables. */
    private transient String diff;

    /**
     * Gets the id.
     *
     * @return the id
     */
    public Long getId() {
        return id;
    }

    /**
     * Sets the id.
     *
     * @param id the id to set
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Gets the type.
     *
     * @return the type
     */
    public TaskType getType() {
        return type;
    }

    /**
     * Sets the type.
     *
     * @param type the type to set
     */
    public void setType(TaskType type) {
        this.type = type;
    }

    /**
     * Gets the execution.
     *
     * @return the execution
     */
    public String getExecution() {
        return execution;
    }

    /**
     * Sets the execution.
     *
     * @param execution the execution to set
     */
    public void setExecution(String execution) {
        this.execution = execution;
    }

    /**
     * Gets the step.
     *
     * @return the step
     */
    public String getStep() {
        return step;
    }

    /**
     * Sets the step.
     *
     * @param step the step to set
     */
    public void setStep(String step) {
        this.step = step;
    }

    /**
     * Gets the definition.
     *
     * @return the definition
     */
    public String getDefinition() {
        return definition;
    }

    /**
     * Sets the definition.
     *
     * @param definition the new definition
     */
    public void setDefinition(String definition) {
        this.definition = definition;
    }

    /**
     * Gets the single instance of TaskState.
     *
     * @return single instance of TaskState
     */
    public String getInstance() {
        return instance;
    }

    /**
     * Sets the instance.
     *
     * @param instance the new instance
     */
    public void setInstance(String instance) {
        this.instance = instance;
    }

    public String getTenant() {
        return tenant;
    }

    public void setTenant(String tenant) {
        this.tenant = tenant;
    }

    public String getThread() {
        return thread;
    }

    public void setThread(String thread) {
        this.thread = thread;
    }

    /**
     * Gets the started.
     *
     * @return the started
     */
    public Timestamp getStarted() {
        return started;
    }

    /**
     * Sets the started.
     *
     * @param started the started to set
     */
    public void setStarted(Timestamp started) {
        this.started = started;
    }

    /**
     * Gets the ended.
     *
     * @return the ended
     */
    public Timestamp getEnded() {
        return ended;
    }

    /**
     * Sets the ended.
     *
     * @param ended the ended to set
     */
    public void setEnded(Timestamp ended) {
        this.ended = ended;
    }

    /**
     * Gets the status.
     *
     * @return the status
     */
    public TaskStatus getStatus() {
        return status;
    }

    /**
     * Sets the status.
     *
     * @param status the status to set
     */
    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    /**
     * Gets the error.
     *
     * @return the error
     */
    public String getError() {
        return error;
    }

    /**
     * Sets the error.
     *
     * @param error the error to set
     */
    public void setError(String error) {
        this.error = error;
    }

    /**
     * Gets the input.
     *
     * @return the input
     */
    public Map<String, String> getInput() {
        return input;
    }

    /**
     * Sets the input.
     *
     * @param input the input to set
     */
    public void setInput(Map<String, String> input) {
        this.input = input;
    }

    /**
     * Gets the output.
     *
     * @return the output
     */
    public Map<String, String> getOutput() {
        return output;
    }

    /**
     * Sets the output.
     *
     * @param output the output to set
     */
    public void setOutput(Map<String, String> output) {
        this.output = output;
    }

    /**
     * Gets the diff.
     *
     * @return the diff
     */
    public String getDiff() {
        return diff;
    }

    /**
     * Sets the diff.
     *
     * @param diff the new diff
     */
    public void setDiff(String diff) {
        this.diff = diff;
    }

    /**
     * To string.
     *
     * @return the string
     */
    @Override
    public String toString() {
        return "TaskState [id=" + id + ", type=" + type + ", execution=" + execution + ", step=" + step + ", definition=" + definition
                + ", instance=" + instance + ", tenant=" + tenant + ", thread=" + thread + ", started=" + started + ", ended=" + ended
                + ", status=" + status + ", error=" + error + ", input=" + StringUtils.join(input) + ", output=" + StringUtils.join(output)
                + "]";
    }


}
