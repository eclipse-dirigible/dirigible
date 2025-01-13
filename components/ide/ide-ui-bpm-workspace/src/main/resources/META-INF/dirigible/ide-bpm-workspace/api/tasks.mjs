import { rs } from "sdk/http";
import { tasks as tasksService} from "sdk/bpm";

rs.service()
    .resource("tasks")
    .get(getUserTasks)
    .execute();

function getUserTasks(ctx, request, response) {
    const tasks = tasksService.list().map(t => {
        return {
            taskId: t.id,
            operationType: t.name,
            createdAt: t.createTime,
            finishedAt: t.claimTime,
            email: t.assignee || "support",
            isFinished: (t.suspensionState !== 1) ? "Yes" : "No"
        }
    })

    const tasksRes = JSON.stringify(tasks);
    response.setContentType("application/json");
    response.print(tasksRes);
}