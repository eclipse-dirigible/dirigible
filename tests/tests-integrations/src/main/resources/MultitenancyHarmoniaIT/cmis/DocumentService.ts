import { Controller, Get, Post, Delete } from "@aerokit/sdk/http"
import { cmis, Document } from "@aerokit/sdk/cms";
import { streams } from "@aerokit/sdk/io";
import { response } from "@aerokit/sdk/http";

@Controller
class DocumentService {

    private isDocument(doc: Document | Folder): boolean {
        return doc.getType().getId() === 'cmis:document';
    }

    @Get("/documents")
    public getAll(_: any, ctx: any) {
        try {
            const cmisSession = cmis.getSession();
            const rootFolder = cmisSession.getRootFolder();

            const children = rootFolder.getChildren();

            const documents = [];
            for (let i in children) {
                const doc = children[i];
                if (this.isDocument(doc)) {
                    documents.push({
                        id: doc.getId(),
                        name: doc.getName(),
                        "type": doc.getType().getId(),
                        path: doc.getPath()

                    });
                }
            }

            return documents;
        } catch (error: any) {
            this.handleError(error);
        }
    }

    @Get("/documents/:id")
    public getById(_: any, ctx: any) {
        try {
            const id = ctx.pathParameters.id;
            const cmisSession = cmis.getSession();
            const doc: Document = cmisSession.getObjectByPath(id);

            const inputStream = doc.getContentStream()?.getStream();
            const outputStream = streams.createByteArrayOutputStream();
            streams.copy(inputStream, outputStream);

            const copiedBytes = outputStream.getBytes();
            const documentContent = String.fromCharCode.apply(String, copiedBytes);

            return documentContent;
        } catch (error: any) {
            this.handleError(error);
        }
    }

    private createRandomFileName(): string {
        return (Math.random() + 1).toString(36).substring(2) + ".txt";
    }

    @Post("/documents")
    public create(entity: any) {
        try {
            const cmisSession = cmis.getSession();
            const rootFolder = cmisSession.getRootFolder();

            const textFileName = entity.documentName ? entity.documentName : this.createRandomFileName();

            const mimetype = "text/plain; charset=UTF-8";
            const content = entity.content ? entity.content : "This is some test content.";
            const filename = textFileName;

            const outputStream = streams.createByteArrayOutputStream();
            outputStream.writeText(content);
            const bytes = outputStream.getBytes();
            const inputStream = streams.createByteArrayInputStream(bytes);

            const contentStream = cmisSession.getObjectFactory().createContentStream(filename, bytes.length, mimetype, inputStream);

            const properties = { "cmis:name": "", "cmis:objectTypeId": "" };
            properties[cmis.OBJECT_TYPE_ID] = cmis.OBJECT_TYPE_DOCUMENT;
            properties[cmis.NAME] = filename;
            const newDocument = rootFolder.createDocument(properties, contentStream, cmis.VERSIONING_STATE_MAJOR);

            return "Created document with id [" + newDocument.getId() + "], name [" + newDocument.getName() + "] and path: " + newDocument.getPath();
        } catch (error: any) {
            this.handleError(error);
        }
    }

    @Delete("/documents/:id")
    public deleteById(_: any, ctx: any) {
        try {
            const id = ctx.pathParameters.id;
            const cmisSession = cmis.getSession();
            const obj = cmisSession.getObjectByPath(id);
            obj.delete();
            return `Document with id [${id}] was deleted`;
        } catch (error: any) {
            this.handleError(error);
        }
    }

    private handleError(error: Error) {
        DocumentService.sendInternalServerError(error.message);
        throw error;
    }

    private static sendInternalServerError(message: string): void {
        DocumentService.sendResponse(500, {
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
