import { response } from "sdk/http"
import { extensions } from "sdk/extensions";

const fileTypeExtensions = await extensions.loadExtensionModules("ide-file-types");

const mappings = {};
for (let i = 0; i < fileTypeExtensions?.length; i++) {
    const fileTypes = fileTypeExtensions[i].getFileTypes();

    for (const fileExtension in fileTypes) {
        if (!mappings[fileExtension]) {
            mappings[fileExtension] = fileTypes[fileExtension];
        }
    }
}
response.setContentType("application/json");
response.println(JSON.stringify(mappings));
response.flush();
response.close();