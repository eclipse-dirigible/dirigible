import { extensions } from "@aerokit/sdk/extensions";
import { response } from "@aerokit/sdk/http";

const artefacts = [];
const artefactsExtensions = await extensions.loadExtensionModules("platform-operations-artefacts");
for (let i = 0; i < artefactsExtensions?.length; i++) {
	try {
		artefacts.push(...artefactsExtensions[i].getArtefacts());	
	} catch(e) {
		// the specific artefact support is not enabled on this instance
	}
}
response.setContentType("application/json");
response.println(JSON.stringify(artefacts));
response.flush();
response.close();
