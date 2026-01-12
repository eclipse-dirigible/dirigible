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
