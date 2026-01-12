import { process } from "@aerokit/sdk/bpm"
import { sql, insert } from "@aerokit/sdk/db";

const execution = process.getExecutionContext();
const processInstanceId = execution.getProcessInstanceId();

const name = process.getVariable(processInstanceId, "name");
if (!name) {
    throw new Error("Missing parameter name. Value: " + name);
}

console.log(`Will insert employee with name ${name}`);

const insertSql = sql.getDialect()//
    .insert()//
    .into("EMPLOYEES")
    .column('ID')
    .column('NAME')
    .build();

const parameters = [1, name];

insert.execute(insertSql, parameters);
