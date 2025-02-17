export class Utils {

    static getArtefactStatus(artefact) {
        return {
            type: artefact.ARTEFACT_TYPE,
            location: artefact.ARTEFACT_LOCATION,
            name: artefact.ARTEFACT_NAME,
            phase: artefact.ARTEFACT_PHASE,
            running: artefact.ARTEFACT_RUNNING,
            status: artefact.ARTEFACT_STATUS,
        }
    };
}