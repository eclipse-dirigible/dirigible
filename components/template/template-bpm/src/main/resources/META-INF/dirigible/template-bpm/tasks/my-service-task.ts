import { process } from "@aerokit/sdk/bpm"

const execution = process.getExecutionContext();
const executionId = execution.getId();

const processVariables = process.getVariables(executionId);

console.log("Hello World! Process variables: " + processVariables);
