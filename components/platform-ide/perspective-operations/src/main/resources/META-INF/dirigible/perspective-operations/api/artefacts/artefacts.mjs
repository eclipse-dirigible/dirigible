import { extensions } from "sdk/extensions";
import { response } from "sdk/http";

const artefacts = [];
const artefactsExtensions = await extensions.loadExtensionModules("platform-operations-artefacts");
for (let i = 0; i < artefactsExtensions?.length; i++) {
    artefacts.push(...artefactsExtensions[i].getArtefacts());
}
response.setContentType("application/json");
response.println(JSON.stringify(artefacts));
response.flush();
response.close();