/**
 * Provides the API for managing scheduled jobs and tasks within the platform,
 * allowing users to retrieve, enable, disable, and trigger jobs, as well as log output.
 */

import { configurations } from "../core";

const JobFacade = Java.type("org.eclipse.dirigible.components.api.job.JobFacade");

/**
 * The Scheduler class provides static methods for interacting with the job scheduler,
 * offering global control over the system's defined jobs.
 */
export class Scheduler {

    /**
     * Retrieves all job definitions currently configured in the system.
     *
     * @returns An array of {@link Job} objects.
     */
    public static getJobs(): Job[] {
        const jobs = new Array<Job>();
        const jobDefinitions = JSON.parse(JobFacade.getJobs());
        for (const definition of jobDefinitions) {
            jobs.push(new Job(definition));
        }
        return jobs;
    }

    /**
     * Retrieves a specific job definition by its unique name.
     *
     * @param name The name of the job.
     * @returns A {@link Job} object corresponding to the provided name.
     */
    public static getJob(name: string): Job {
        const jobDefinition = JSON.parse(JobFacade.getJob(name));
        return new Job(jobDefinition);
    }

    /**
     * Enables a job, allowing it to be executed according to its schedule (cron expression).
     *
     * @param name The name of the job to enable.
     */
    public static enable(name: string): void {
        JobFacade.enable(name);
    }

    /**
     * Disables a job, preventing it from executing on its schedule.
     *
     * @param name The name of the job to disable.
     */
    public static disable(name: string): void {
        JobFacade.disable(name);
    }

    /**
     * Triggers the immediate execution of a job.
     *
     * @param name The name of the job to trigger.
     * @param parameters Optional key-value object of parameters to pass to the job execution.
     */
    public static trigger(name: string, parameters: { [key: string]: string } = {}): void {
        JobFacade.trigger(name, JSON.stringify(parameters));
    }

    /**
     * Logs a message at the standard log level for a specific job instance.
     * This is useful when the log context needs to be associated with a running job.
     *
     * @param name The name of the job to associate the log with.
     * @param message The log message content.
     */
    public static log(name: string, message: string): void {
        JobFacade.log(name, message);
    }

    /**
     * Logs an error message for a specific job instance.
     *
     * @param name The name of the job.
     * @param message The error message content.
     */
    public static error(name: string, message: string): void {
        JobFacade.error(name, message);
    }

    /**
     * Logs a warning message for a specific job instance.
     *
     * @param name The name of the job.
     * @param message The warning message content.
     */
    public static warn(name: string, message: string): void {
        JobFacade.warn(name, message);
    }

    /**
     * Logs an informational message for a specific job instance.
     *
     * @param name The name of the job.
     * @param message The information message content.
     */
    public static info(name: string, message: string): void {
        JobFacade.info(name, message);
    }

}

/**
 * Represents a single scheduled job definition.
 */
class Job {

    private data: any;

    /**
     * @param data The raw data object containing job properties.
     */
    constructor(data: any) {
        this.data = data;
    }

    /**
     * Gets the unique name of the job.
     * @returns The job name.
     */
    public getName(): string {
        return this.data.name;
    }

    /**
     * Gets the logical grouping for the job.
     * @returns The job group name.
     */
    public getGroup(): string {
        return this.data.group;
    }

    /**
     * Gets the Java class name (for Java-based jobs) or script file name (for script-based jobs).
     * @returns The job implementation class/file name.
     */
    public getClazz(): string {
        return this.data.clazz;
    }

    /**
     * Gets the description of the job's purpose.
     * @returns The job description.
     */
    public getDescription(): string {
        return this.data.description;
    }

    /**
     * Gets the cron expression defining the job's schedule.
     * @returns The cron expression string.
     */
    public getExpression(): string {
        return this.data.expression;
    }

    /**
     * Gets the handler file path or resource name for script-based jobs.
     * @returns The handler path.
     */
    public getHandler(): string {
        return this.data.handler;
    }

    /**
     * Gets the execution engine type (e.g., 'JavaScript', 'Java').
     * @returns The engine type.
     */
    public getEngine(): string {
        return this.data.engine;
    }

    /**
     * Checks if the job is configured as a singleton (only one instance runs at a time).
     * @returns True if the job is a singleton.
     */
    public getSingleton(): boolean {
        return this.data.singleton;
    }

    /**
     * Checks if the job is currently enabled for scheduled execution.
     * @returns True if the job is enabled.
     */
    public getEnabled(): boolean {
        return this.data.enabled;
    }

    /**
     * Gets the user ID who created the job definition.
     * @returns The creator's user ID.
     */
    public getCreatedBy(): string {
        return this.data.createdBy;
    }

    /**
     * Gets the timestamp when the job definition was created.
     * @returns The creation time as a numerical timestamp.
     */
    public getCreatedAt(): number {
        return this.data.createdAt;
    }

    /**
     * Gets the parameters associated with this job definition.
     * @returns A {@link JobParameters} object containing all parameters.
     */
    public getParameters(): JobParameters {
        return new JobParameters(this.data.parameters);
    }

    /**
     * Retrieves the value for a specific parameter of this job.
     * It checks for an overriding value in the global configurations first,
     * and falls back to the defined default value if the configuration is not set.
     *
     * @param name The name of the parameter to retrieve.
     * @returns The parameter's configured or default value, or null if not found.
     */
    public getParameter(name: string): string {
        if (this.data) {
            for (let i in this.data.parameters) {
                if (this.data.parameters[i].name === name) {
                    // Check global configuration for override
                    let value = configurations.get(name);
                    return value && value !== null ? value : this.data.parameters[i].defaultValue;
                }
            }
        } else {
            console.error("Job is not valid");
        }
        return null;
    }

    /**
     * Enables this specific job instance.
     */
    public enable(): void {
        JobFacade.enable(this.getName());
    }

    /**
     * Disables this specific job instance.
     */
    public disable(): void {
        JobFacade.disable(this.getName());
    }

    /**
     * Triggers the immediate execution of this job instance.
     *
     * @param parameters Optional key-value object of parameters to pass to the job execution.
     */
    public trigger(parameters: { [key: string]: string } = {}): void {
        JobFacade.trigger(this.getName(), JSON.stringify(parameters));
    }

    /**
     * Logs a message at the standard log level for this job instance.
     *
     * @param message The log message content.
     */
    public log(message: string): void {
        JobFacade.log(this.getName(), message);
    }

    /**
     * Logs an error message for this job instance.
     *
     * @param message The error message content.
     */
    public error(message: string): void {
        JobFacade.error(this.getName(), message);
    }

    /**
     * Logs a warning message for this job instance.
     *
     * @param message The warning message content.
     */
    public warn(message: string): void {
        JobFacade.warn(this.getName(), message);
    }

    /**
     * Logs an informational message for this job instance.
     *
     * @param message The information message content.
     */
    public info(message: string): void {
        JobFacade.info(this.getName(), message);
    }
}

/**
 * A container object representing the collection of parameters for a {@link Job}.
 */
class JobParameters {

    private data: any[]

    /**
     * @param data The array of raw job parameter objects.
     */
    constructor(data: any[]) {
        this.data = data;
    }

    /**
     * Retrieves a specific job parameter by its index.
     *
     * @param i The index of the parameter in the array.
     * @returns A {@link JobParameter} object.
     */
    public get(i: number): JobParameter {
        return new JobParameter(this.data[i]);
    }

    /**
     * Gets the total number of parameters defined for the job.
     * @returns The count of parameters.
     */
    public count(): number {
        return this.data.length;
    }
}

/**
 * Represents a single parameter definition for a job.
 */
class JobParameter {

    private data: any;

    /**
     * @param data The raw data object containing parameter properties.
     */
    constructor(data: any) {
        this.data = data;
    }

    /**
     * Gets the name of the parameter.
     * @returns The parameter name.
     */
    public getName(): string {
        return this.data.name;
    }

    /**
     * Gets the description of the parameter.
     * @returns The parameter description.
     */
    public getDescription(): string {
        return this.data.description;
    }

    /**
     * Gets the expected data type of the parameter (e.g., 'String', 'Integer', 'Boolean').
     * @returns The parameter type.
     */
    public getType(): string {
        return this.data.type;
    }

    /**
     * Gets the default value for the parameter.
     * @returns The default value string.
     */
    public getDefaultValue(): string {
        return this.data.defaultValue;
    }

    /**
     * Gets a list of predefined choices for the parameter, if applicable.
     * @returns An array of choice strings.
     */
    public getChoices(): string[] {
        return this.data.choices;
    }

}

// @ts-ignore
if (typeof module !== 'undefined') {
	// @ts-ignore
	module.exports = Scheduler;
}
