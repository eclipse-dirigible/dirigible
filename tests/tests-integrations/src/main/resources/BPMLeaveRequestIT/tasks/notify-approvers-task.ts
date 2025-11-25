
import { configurations as config } from "@aerokit/sdk/core";
import { process } from "@aerokit/sdk/bpm"
import { sendMail } from "./mail-util"

const execution = process.getExecutionContext();
const executionId = execution.getId();

const requester = process.getVariable(executionId, "requester");

const managersEmail = config.get("LEAVE_REQUEST_MANAGERS_EMAIL", "managers-dl@example.com");
const subject = "New leave request";
const content = `<h4>A new leave request for [${requester}] has been created</h4>Open the inbox <a href="http://localhost:80/services/web/inbox/" target="_blank">here</a> to process the request.`;

sendMail(managersEmail, subject, content);
