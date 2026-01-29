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
        const processDefinitionId = executionContext.getProcessDefinitionId();
        const processInstanceId = executionContext.getProcessInstanceId();
        const businessKey = executionContext.getProcessInstanceBusinessKey();
        const activityId = executionContext.getCurrentActivityId();

        return `[${processDefinitionId}][${processInstanceId}][${businessKey}][${activityId}]`;
    }
}