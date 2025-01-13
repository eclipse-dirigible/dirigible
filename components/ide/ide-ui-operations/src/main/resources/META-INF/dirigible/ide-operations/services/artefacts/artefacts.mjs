import { extensions } from "sdk/extensions";
import { response } from "sdk/http";

const artefacts = [];
const artefactsExtensions = await extensions.loadExtensionModules("ide-operations-artefacts");
for (let i = 0; i < artefactsExtensions?.length; i++) {
    artefacts.push(...artefactsExtensions[i].getArtefacts());
}

response.println(JSON.stringify(artefacts));