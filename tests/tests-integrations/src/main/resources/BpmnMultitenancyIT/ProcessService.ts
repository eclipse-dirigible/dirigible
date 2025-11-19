import { Controller, Post, Put, response } from "@aerokit/sdk/http"
import { process, tasks } from "@aerokit/sdk/bpm"

@Controller
class ProcessService {

    @Post("/processes")
    public startProcess(parameters: any) {
        const processKey = 'bpmn-multitenancy-it';
        if (!parameters) {
            throw new Error("Missing parameters: " + parameters);
        }
        const processInstanceId = process.start(processKey, 'business-key-bpmn-multitenancy', parameters);

        response.setStatus(response.ACCEPTED);
        return {
            processInstanceId: processInstanceId,
            processKey: processKey,
            parameters: parameters,
            message: `Started process instance with id [${processInstanceId}] for process with key [${processKey}]`
        };
    }
    @Put("/requests/:id/approve")
    public approveRequest(_: any, ctx: any) {
        const taskId = ctx.pathParameters.id;
        this.completeTask(taskId, true);
    }

    @Put("/requests/:id/decline")
    public declineRequest(_: any, ctx: any) {
        const taskId = ctx.pathParameters.id;
        this.completeTask(taskId, false);
    }

    private completeTask(taskId: string, approved: boolean) {
        const variables = {
            requestApproved: approved
        };
        tasks.complete(taskId, variables);
    }

}
