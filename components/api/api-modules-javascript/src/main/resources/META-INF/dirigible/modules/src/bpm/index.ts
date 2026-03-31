/**
 * @module bpm/index
 * @package @aerokit/sdk/bpm
 * @overview
 * 
 * This module provides functionalities for Business Process Management (BPM) within the Aerokit SDK. It includes classes and methods for deploying processes, managing tasks, and tracing process execution.
 * 
 * The main components of this module are:
 * - Deployer: Responsible for deploying BPMN processes.
 * - Process: Represents a BPMN process instance and provides methods to interact with it.
 * - Tasks: Manages tasks associated with BPMN processes, allowing for task completion and retrieval.
 * - Values: Handles the values and variables associated with BPMN processes.
 * - Tracer: Provides tracing capabilities to monitor the execution of BPMN processes.
 */

export { Deployer as deployer } from "./deployer";
export * from "./deployer";
export { Process as process } from "./process";
export * from "./process";
export { Tasks as tasks } from "./tasks";
export * from "./tasks";
export { Values as values } from "./values";
export * from "./values";
export * from "./tracer";
export { Tracer as tracer } from "./tracer";