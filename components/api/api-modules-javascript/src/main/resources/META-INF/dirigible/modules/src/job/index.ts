/**
 * @module job/index
 * @package @aerokit/sdk/job
 * @overview
 * 
 * This module provides functionalities for job scheduling and execution within the Aerokit SDK. It includes classes and methods for defining, scheduling, and managing jobs in a modular architecture.
 * 
 * The main components of this module are:
 * - Scheduler: Represents a job scheduler that allows you to define and manage scheduled jobs.
 * - Decorators: Provides decorators for defining jobs and their properties in a declarative manner.
 */

export { Scheduler as scheduler } from "./scheduler";
export * from "./scheduler";
export * as decorators from "./decorators";
export * from "./decorators";