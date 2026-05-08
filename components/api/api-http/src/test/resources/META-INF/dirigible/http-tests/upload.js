import { Request, Response, Upload } from "@aerokit/sdk/http";

if (Request.getMethod() === "POST") {
    if (Upload.isMultipartContent()) {
        const fileItems = Upload.parseRequest();
        for (let i = 0; i < fileItems.size(); i++) {
            var fileItem = fileItems.get(i);
            if (!fileItem.isEmpty()) {
                Response.println("File Name: " + fileItem.getName());
                Response.println("File Bytes (as text): " + String.fromCharCode.apply(null, fileItem.getBytes()));
            } else {
                Response.println("Field Name: " + fileItem.getName());
                Response.println("Field Text: " + fileItem.getText());
            }
        }
    } else {
        Response.println("The request's content must be 'multipart'");
    }
} else if (Request.getMethod() === "GET") {
    Response.println("Use POST Request.");
}

Response.flush();
Response.close();
