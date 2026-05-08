/**
 * @module job/decorators
 * @package @aerokit/sdk/job
 * @name Decorators
 * @overview
 * 
 * This module provides a set of decorators for defining scheduled jobs in a declarative manner. The decorators allow developers to annotate classes to specify that they should be executed according to a specified schedule, making it easier to create and manage scheduled tasks within the application.
 * 
 * ### Key Features:
 * - **Scheduled Decorator**: The `@Scheduled` decorator is used to mark a class as a scheduled job, which will be executed based on a cron expression defined in the decorator's options.
 * 
 * ### Use Cases:
 * - **Task Scheduling**: These decorators are primarily used for defining tasks that need to run at specific intervals or times, such as data cleanup, report generation, or any recurring background processing.
 * - **Code Organization**: By using decorators, developers can keep their code organized and maintainable, separating the scheduling logic from the business logic within their job classes.
 * 
 * ### Example Usage:
 * ```ts
 * import { Scheduled } from "@aerokit/sdk/job";
 * 
 * @Scheduled({ expression: "0/10 * * * * ?", group: "defined" })
 * class MyScheduledJob {
 *     execute() {
 *         // Logic to be executed every 10 seconds
 *     }
 * }
 * 
 * // The job will be automatically scheduled based on the provided cron expression.
 * ```
 */

const SCHEDULED_METADATA_KEY = Symbol("scheduled:metadata");

export interface ScheduledOptions {
    expression: string; // e.g., "0/10 * * * * ?"
    group?: string;      // e.g., "defined"
}

/**
 * @Scheduled decorator
 * Marks an entire class as a scheduled job with a cron expression.
 *
 * introduced in TypeScript 5.0, which expects a ClassDecoratorContext object.
 */
export function Scheduled(options: ScheduledOptions) {
    
    return function <T extends abstract new (...args: any) => any>(target: T, context: ClassDecoratorContext<T>) {
        
        if (context.kind !== 'class') {
            throw new Error(`@Scheduled can only be used on classes.`);
        }

        Object.defineProperty(target, SCHEDULED_METADATA_KEY, {
            value: {
                expression: options.expression,
                group: options.group
            },
            writable: false,
            configurable: false,
            enumerable: true
        });
    };
}
