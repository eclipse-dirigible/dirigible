import { request, response, rs } from "@aerokit/sdk/http";
import { workspace, lifecycle } from "@aerokit/sdk/platform";
import { user } from "@aerokit/sdk/security";

let templatePayload = request.getJSON();
let template;
try {
    // Fallback to require()
    template = dirigibleRequire(templatePayload.template);
} catch (e) {
    template = await import(templatePayload.template.replace(".js", ""));
}

class HttpError extends Error {

    constructor(message, status) {
        super(message);
        this.status = status;
    }
}

//class ForbiddenError extends HttpError {
//
//    constructor(message) {
//        super(message, response.FORBIDDEN);
//    }
//}

class BadRequestError extends HttpError {

    constructor(message) {
        super(message, response.BAD_REQUEST);
    }
}


rs.service()
    .resource("model/{workspace}/{project}")
    .post(onGenerateModel)
    .before((context) => {
        //if (!user.isInRole("Developer")) {
        //    throw new ForbiddenError("Forbidden");
        //} else 
        if (!context.queryParameters.path) {
            throw new BadRequestError("Missing 'path' query parameter");
        }
    })
    .catch(onErrorOccurred)
    .execute();

function onErrorOccurred(ctx, err, request, response) {
    let status = err.status || response.INTERNAL_SERVER_ERROR;
    let message = err.message || "Internal Server Error";
    response.setStatus(status);
    response.println(JSON.stringify({
        status: status,
        message: message
    }));
	console.error(message);
}

function onGenerateModel(context, request, response) {
    let workspace = context.pathParameters.workspace;
    let project = context.pathParameters.project;
    let path = context.queryParameters.path;

    let model = getModel(workspace, project, path);

    let parameters = templatePayload.parameters;
    parameters.projectName = project;
    parameters.workspaceName = workspace;
    parameters.filePath = path;
    parameters.templateId = templatePayload.template;
    parameters.fileName = path;
    if (parameters.fileName.indexOf(".") > 0) {
        parameters.fileName = parameters.fileName.substring(0, path.indexOf("."))
    }
    parameters.genFolderName = parameters.fileName;

    let generatedFiles = template.generate(model, parameters);

    // Clean only the gen/<subfolder>s this generation actually writes to (derived from the output
    // paths), not gen/<modelFileName> blindly. The standard templates write under gen/<modelFileName>
    // so this is unchanged for them, but a template that targets a sibling folder (e.g. the events
    // glue under gen/events) must not wipe another template's output (e.g. gen/<model>) for the same
    // model file.
    let genSubfolders = new Set();
    for (let i = 0; i < generatedFiles.length; i++) {
        let path = generatedFiles[i].path;
        if (path && path.startsWith("gen/")) {
            let rest = path.substring("gen/".length);
            let slash = rest.indexOf("/");
            let subfolder = slash >= 0 ? rest.substring(0, slash) : rest;
            if (subfolder) {
                genSubfolders.add(subfolder);
            }
        }
    }
    genSubfolders.forEach(subfolder => cleanGenFolder(workspace, project, subfolder));

    for (let i = 0; i < generatedFiles.length; i++) {
        createFile(workspace, project, generatedFiles[i].path, generatedFiles[i].content);
    }

    createFile(workspace, project, parameters.fileName + ".gen", JSON.stringify(parameters, null, 2));

    lifecycle.publish(user.getName(), workspace, project);

    response.setStatus(response.CREATED);

    response.flush();
    response.close();
}

function getModel(workspaceName, projectName, path) {
    let projectWorkspace = workspace.getWorkspace(workspaceName);
    if (!projectWorkspace.exists()) {
        throw new BadRequestError(`Workspace '${workspaceName}' does not exist.`);
    }
    let project = projectWorkspace.getProject(projectName);
    if (!project.exists()) {
        throw new BadRequestError(`Project '${projectName}' does not exist in Workspace '${workspaceName}'.`);
    }
    let model = project.getFile(path);
    if (!model.exists()) {
        throw new BadRequestError(`Model file '${path}' does not exist in Project '${projectName}' in Workspace '${workspaceName}'.`);
    }
    return augmentWithExtensions(model.getText(), projectWorkspace, projectName);
}

// Cross-model entity extension (the compose pre-pass). Another project may contribute an EXTENSION
// entity that adds fields to one of THIS project's entities (e.g. a localisation module adds an EGN
// field to the Employee entity owned here). Such an extension cannot rewrite this project's model at
// authoring time, so we fold it in at generation time: scan every sibling project's <name>.model for
// EXTENSION entities whose `extensionReferencedModel` names THIS project, and append them to this
// model's entity list. `generateUtils.generateFiles` then merges each EXTENSION's properties into its
// base entity (matched by `extensionReferencedEntity`) and drops the EXTENSION itself - so the added
// fields become real columns on the base table, natively filterable/sortable/formable, with no join.
function augmentWithExtensions(modelText, projectWorkspace, projectName) {
    let root;
    try {
        root = JSON.parse(modelText);
    } catch (e) {
        return modelText; // not JSON (e.g. a non-model artifact) - leave untouched
    }
    if (!root.model || !Array.isArray(root.model.entities)) {
        return modelText;
    }
    let extensions = [];
    let projects = projectWorkspace.getProjects();
    for (let i = 0; i < projects.size(); i++) {
        let sibling = projects.get(i);
        let siblingName = sibling.getName();
        if (siblingName === projectName) {
            continue; // an entity is not extended from within its own project
        }
        let modelFile = sibling.getFile(siblingName + ".model");
        if (!modelFile.exists()) {
            continue;
        }
        let siblingRoot;
        try {
            siblingRoot = JSON.parse(modelFile.getText());
        } catch (e) {
            continue;
        }
        let entities = siblingRoot && siblingRoot.model && siblingRoot.model.entities;
        if (!Array.isArray(entities)) {
            continue;
        }
        for (const entity of entities) {
            if (entity.type === "EXTENSION" && entity.extensionReferencedModel === projectName) {
                extensions.push(entity);
            }
        }
    }
    if (extensions.length === 0) {
        return modelText;
    }
    // Deterministic order so the merged column order is reproducible across regenerations.
    extensions.sort((a, b) => `${a.extensionReferencedEntity}.${a.name}`.localeCompare(`${b.extensionReferencedEntity}.${b.name}`));
    for (const ext of extensions) {
        root.model.entities.push(ext);
    }
    return JSON.stringify(root);
}

function cleanGenFolder(workspaceName, projectName, genFolderName) {
    let projectWorkspace = workspace.getWorkspace(workspaceName);
    let project = projectWorkspace.getProject(projectName);

    const genFolder = project.getFolder("gen");
    if (genFolder.exists() && genFolder.existsFolder(genFolderName)) {
        genFolder.deleteFolder(genFolderName);
        lifecycle.unpublish(projectName + "/gen/" + genFolderName);
    }
}

function createFile(workspaceName, projectName, path, content) {
    let projectWorkspace = workspace.getWorkspace(workspaceName);
    let project = projectWorkspace.getProject(projectName);

    let pathSegments = path.split("/");
    let fileName = pathSegments[pathSegments.length - 1];
    let folder = null;
    for (let i = 0; i < pathSegments.length - 1; i++) {
        if (folder == null) {
            if (!project.existsFolder(pathSegments[i])) {
                folder = project.createFolder(pathSegments[i])
            }
            folder = project.getFolder(pathSegments[i])
            continue;
        }
        if (!folder.existsFolder(pathSegments[i])) {
            folder = folder.createFolder(pathSegments[i]);
        } else {
            folder = folder.getFolder(pathSegments[i]);
        }
    }
    let file = null;
    if (folder == null) {
        file = project.createFile(fileName);
    } else {
        file = folder.createFile(fileName);
    }
    file.setText(content);
}
