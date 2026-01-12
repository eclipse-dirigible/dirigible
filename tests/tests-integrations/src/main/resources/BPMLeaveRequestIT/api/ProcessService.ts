import { Controller, Post, Put, Get, response } from "@aerokit/sdk/http"
import { process, tasks } from "@aerokit/sdk/bpm"
import { user } from "@aerokit/sdk/security";

@Controller
class ProcessService {

    @Post("/requests")
    public startProcess(parameters: any) {
        const processKey = 'leave-request-id';

        const processParams = {
            "requester": user.getName(),
            "toDate": parameters.toDate,
            "fromDate": parameters.fromDate
        };
        const processInstanceId = process.start(processKey, 'business-key-leave-request', processParams);

        response.setStatus(response.ACCEPTED);
        return {
            processInstanceId: processInstanceId,
            processKey: processKey,
            parameters: processParams,
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
            approver: user.getName(),
            requestApproved: approved
        };
        tasks.complete(taskId, variables);
    }

    @Get("/requests/:id/details")
    public getRequestDetails(_: any, ctx: any) {
        const taskId = ctx.pathParameters.id;
        return tasks.getVariables(taskId);
    }

}
