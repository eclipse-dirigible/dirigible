/**
 * @module bpm/tracer
 * @package @aerokit/sdk/bpm
 * @name Tracer
 * @overview
 * 
 * This module provides a `Tracer` class for logging and tracing the execution of BPMN processes within the Dirigible environment. The `Tracer` class allows developers to log informational messages, warnings, and errors, as well as to track the duration of process execution from start to completion or failure.
 * 
 * ### Key Features
 * - Context-aware logging: Automatically includes process definition ID, instance ID, business key, and activity ID in log messages.
 * - Execution timing: Tracks the duration of process execution and logs it upon completion or failure.
 * - Flexible logging levels: Supports informational, warning, and error logging.
 * 
 * ### Use Cases
 * - Debugging and monitoring BPMN process execution in development and production environments.
 * - Providing insights into process performance and identifying bottlenecks or issues.
 * - Enhancing observability of BPMN processes for better maintenance and support.
 * 
 * ### Example Usage
 * ```ts
 * import { Tracer } from "@aerokit/sdk/bpm";
 * 
 * const tracer = new Tracer();
 * try {
 *   // Perform some operations related to the BPMN process
 *   tracer.log("Performing step 1");
 *   // ...
 *   tracer.log("Performing step 2");
 *  // ...
 *  tracer.complete("Process completed successfully");
 * } catch (error) {
 *  tracer.fail(`Process failed with error: ${error.message}`);
 * }
 * ```
 */

import { Process } from '@aerokit/sdk/bpm';
import { Logging, Logger } from "@aerokit/sdk/log";

export class Tracer {
    private readonly startTime: Date;
    private readonly logger: Logger;

    constructor() {
        this.startTime = new Date();
        this.logger = Logging.getLogger('bpm.tracer');
        this.log('Started');
    }

    public log(message: string) {
        this.logger.info(`${this.getId()} - ${message ?? ''}`);
    }

    public warn(message: string) {
        this.logger.warn(`${this.getId()} - ${message ?? ''}`);
    }

    public error(message: string) {
        this.logger.error(`${this.getId()} - ${message ?? ''}`);
    }

    public complete(message?: string) {
        const endTime = new Date();
        const seconds = Math.ceil((endTime.getTime() - this.startTime.getTime()) / 1000);
        this.logger.info(`${this.getId()} - Completed after ${seconds} seconds. ${message ?? ''}`);
    }

    public fail(message?: string) {
        const endTime = new Date();
        const seconds = Math.ceil((endTime.getTime() - this.startTime.getTime()) / 1000);
        this.logger.error(`${this.getId()} - Failed after ${seconds} seconds. ${message ?? ''}`);
    }

    private getId(): string {
        const executionContext = Process.getExecutionContext();
        if (executionContext) {
            const processDefinitionId = executionContext.getProcessDefinitionId();
            const processInstanceId = executionContext.getProcessInstanceId();
            const businessKey = executionContext.getProcessInstanceBusinessKey();
            const activityId = executionContext.getCurrentActivityId();
            return `[${processDefinitionId}][${processInstanceId}][${businessKey}][${activityId}]`;
        }
        return '[no-execution-context]';
    }
}