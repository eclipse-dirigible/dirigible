import { response } from "sdk/http";

export class HttpUtils {

    // HTTP 200
    public static sendResponseOk(entity: any): void {
        HttpUtils.sendResponse(200, entity);
    }

    // HTTP 201
    public static sendResponseCreated(entity): void {
        HttpUtils.sendResponse(201, entity);
    }

    // HTTP 204
    public static sendResponseNoContent(): void {
        HttpUtils.sendResponse(204);
    }

    // HTTP 400
    public static sendResponseBadRequest(message): void {
        HttpUtils.sendResponse(400, {
            "code": 400,
            "message": message
        });
    }

    // HTTP 403
    public static sendForbiddenRequest(message): void {
        HttpUtils.sendResponse(403, {
            "code": 403,
            "message": message
        });
    }

    // HTTP 404
    public static sendResponseNotFound(message): void {
        HttpUtils.sendResponse(404, {
            "code": 404,
            "message": message
        });
    }

    // HTTP 500
    public static sendInternalServerError(message): void {
        HttpUtils.sendResponse(500, {
            "code": 500,
            "message": message
        });
    }

    // Generic
    private static sendResponse(status: number, body?: any): void {
        response.setContentType("application/json");
        response.setStatus(status);
        if (body) {
            response.println(JSON.stringify(body));
        }
    }
}