import { process } from "sdk/bpm"
import { sendMail } from "./mail-util"

const execution = process.getExecutionContext();
const executionId = execution.getId();

const processVariables = process.getVariables(executionId);

const fromDate = processVariables.fromDate;
const toDate = processVariables.toDate;
const approver = processVariables.approver;
const requester = processVariables.requester;

const subject = "Your leave request has been declined";
const content = `<h4>Your leave request from [${fromDate}] to [${toDate}] has been declined by [${approver}]</h4>`;

sendMail(requester, subject, content)