import { Controller, Get, response } from "@aerokit/sdk/http"
import { query, sql } from "@aerokit/sdk/db";

@Controller
class ReaderViewService {

    @Get("/")
    public getAll() {
        try {
            const sqlQuery = sql.getDialect().select().from("READERS_VIEW").build();
            return query.execute(sqlQuery, []);
        } catch (error: any) {
            this.handleError(error);
        }
    }

    private handleError(error: Error) {
        ReaderViewService.sendInternalServerError(error.message);
        throw error;
    }

    private static sendInternalServerError(message: string): void {
        ReaderViewService.sendResponse(500, {
            "code": 500,
            "message": message
        });
    }

    private static sendResponse(status: number, body?: any): void {
        response.setContentType("application/json");
        response.setStatus(status);
        if (body) {
            response.println(JSON.stringify(body));
        }
    }

}
