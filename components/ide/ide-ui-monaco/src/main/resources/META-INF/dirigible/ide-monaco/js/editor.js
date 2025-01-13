const messageHub = new FramesMessageHub();

const WORKSPACE_API = "/services/ide/workspaces";
const REPOSITORY_API = "/services/core/repository";
const REGISTRY_API = "/services/core/repository/registry/public";

let csrfToken;
let lineDecorations = [];

// @ts-ignore
require.config({
    paths: {
        'vs': '/webjars/monaco-editor/min/vs',
        'parser': 'js/parser'
    }
});

// @ts-ignore
require(['vs/editor/editor.main', 'parser/acorn-loose'], async function (monaco, acornLoose) {
    try {
        const fileIO = new FileIO();
        const fileName = fileIO.resolvePath();
        const readOnly = fileIO.isReadOnly();
        const fileType = await fileIO.getFileType(fileName);
        const isTemplate = fileIO.isFileTemplate(fileName);
        const fileObject = await fileIO.loadText(fileName, true);

        const dirigibleEditor = new DirigibleEditor(monaco, acornLoose, fileName, readOnly, fileType, isTemplate, fileObject);
        await dirigibleEditor.init();
        dirigibleEditor.configureMonaco();
        dirigibleEditor.subscribeEvents();

    } catch (e) {
        console.error(e);
        DirigibleEditor.closeEditor();
    }
});

class Utils {

    static logMessage(message) {
        messageHub.post({
            message: message
        }, 'ide.status.message');
    }

    static logErrorMessage(errorMessage) {
        console.error(errorMessage);
        messageHub.post({
            message: errorMessage
        }, 'ide.status.error');
    }

    static setEditorDirty(fileName, isDirty) {
        messageHub.post({
            resourcePath: fileName,
            isDirty: isDirty
        }, 'ide-core.setEditorDirty');
    }
}

class ViewParameters {

    static useParameters = false;
    static parameters = {
        resourceType: "",
        contentType: "",
        readOnly: false,
        gitName: "",
        file: ""
    };

    constructor() {
        if (window.frameElement && window.frameElement.hasAttribute("data-parameters")) {
            const dataParameters = window.frameElement.getAttribute("data-parameters");
            if (dataParameters) {
                const params = JSON.parse(dataParameters);
                ViewParameters.parameters.resourceType = params["resourceType"] || "/services/ide/workspaces";
                ViewParameters.parameters.contentType = params["contentType"] || "";
                ViewParameters.parameters.readOnly = params["readOnly"] || false;
                ViewParameters.parameters.gitName = params["gitName"] || "";
                ViewParameters.parameters.file = params["file"] || "";
                ViewParameters.useParameters = true;
            }
        }
    }
}

class FileIO {

    static EDITOR_URL = new URL(window.location.href);

    constructor() {
        new ViewParameters();
    }

    isReadOnly() {
        if (ViewParameters.useParameters) {
            return ViewParameters.parameters.readOnly || false;
        }
        return FileIO.EDITOR_URL.searchParams.get('readOnly') || false;
    }

    resolvePath() {
        if (ViewParameters.useParameters) {
            this.readOnly = ViewParameters.parameters.readOnly || false;
            return ViewParameters.parameters.file;
        }
        this.readOnly = FileIO.EDITOR_URL.searchParams.get('readOnly') || false;
        return FileIO.EDITOR_URL.searchParams.get('file');
    }

    resolveWorkspace() {
        const fileName = this.resolvePath();
        if (fileName) {
            return fileName.replaceAll('\\', '/').split('/')[1]
        }
        return '';
    }

    resolveProjectName(filePath) {
        let path = this.resolvePath();
        if (path && filePath.startsWith('../')) {
            const fileNameTokens = path.split('/');
            const relativeTokens = filePath.split('../');
            for (let i = 0; i < relativeTokens.length; i++) {
                fileNameTokens.pop();
            }
            path = fileNameTokens.join('/') + '/' + relativeTokens.filter(e => e != '').join('/');
        }
        if (path) {
            return path.replaceAll('\\', '/').split('/')[2]
        }
        return '';
    }

    resolveRelativePath(basePath, relativePath) {
        if (relativePath.startsWith('../')) {
            const fileNameTokens = basePath.split('/');
            const relativeTokens = relativePath.split('../');
            for (let i = 0; i < relativeTokens.length; i++) {
                fileNameTokens.pop();
            }
            basePath = fileNameTokens.join('/') + '/' + relativeTokens.filter(e => e != '').join('/');
        } else if (relativePath.startsWith('./')) {
            const fileNameTokens = basePath.split('/');
            fileNameTokens.pop();
            const relativeTokens = relativePath.split('./');
            basePath = fileNameTokens.join('/') + '/' + relativeTokens.filter(e => e != '').join('/');
        } else if (TypeScriptUtils.isGlobalImport(relativePath)) {
            return relativePath;
        }
        return basePath.replaceAll('\\', '/').split('/').join('/');
    }

    resolveFilePath(filePath) {
        let path = this.resolvePath();
        if (path && filePath.startsWith('../')) {
            const fileNameTokens = path.split('/');
            const relativeTokens = filePath.split('../');
            for (let i = 0; i < relativeTokens.length; i++) {
                fileNameTokens.pop();
            }
            path = fileNameTokens.join('/') + '/' + relativeTokens.filter(e => e != '').join('/');
        } else if (path && filePath.startsWith('./')) {
            const fileNameTokens = path.split('/');
            fileNameTokens.pop();
            const relativeTokens = filePath.split('./');
            path = fileNameTokens.join('/') + '/' + relativeTokens.filter(e => e != '').join('/');
        } else if (TypeScriptUtils.isGlobalImport(filePath)) {
            return filePath;
        }
        if (path) {
            return path.replaceAll('\\', '/').split('/').slice(2).join('/');
        }
        return '';
    }

    isFileTemplate(fileName) {
        return fileName.endsWith('.template');
    };

    async getFileType(fileName) {
        const response = await fetch('/services/js/ide-monaco/api/fileTypes.js');
        csrfToken = response.headers.get("x-csrf-token");
        if (response.ok) {
            const fileTypes = await response.json();
            let fileType = "text";
            if (fileName) {
                for (let fileExtension in fileTypes) {
                    if (fileName.endsWith(fileExtension)) {
                        fileType = fileTypes[fileExtension];
                    }
                }
            }
            if (fileName && fileName.indexOf(".") === -1 && fileName.toLowerCase().indexOf("dockerfile") > 0) {
                fileType = "dockerfile";
            }
            return fileType;
        }
        Utils.logErrorMessage(`Unable to determine the fileType of [${fileName}], HTTP: ${response.status}, ${response.statusText}]`);
    };

    async loadText(path, loadGitMetadata = false) {
        try {
            path = path ?? this.resolvePath();
            if (!path) {
                throw new Error(`Unable to load file [${path}], file query parameter is not present in the URL`);
            }
            const workspace = this.resolveWorkspace();
            const projectName = this.resolveProjectName(path);
            const filePath = this.resolveFilePath(path);

            const requestMetadata = this.#buildFileRequestMetadata(path, workspace, filePath, loadGitMetadata);
            let response;

            try {
                response = await fetch(requestMetadata.url, {
                    method: 'GET',
                    headers: {
                        'X-Requested-With': 'Fetch',
                        'Dirigible-Editor': 'Monaco',
                    }
                });
                if (!response.ok) {
                    throw new Error(`Unable to load [${path}, HTTP: ${response.status}, ${response.statusText}]`);
                }
            } catch (e) {
                // Fallback to file in Registry
                response = await fetch(this.#buildRegistryUrl(filePath), {
                    method: 'GET',
                    headers: {
                        'X-Requested-With': 'Fetch',
                        'Dirigible-Editor': 'Monaco',
                    }
                });
                requestMetadata.isGitProject = false;
            }
            if (!response.ok) {
                throw new Error(`Unable to load [${path}, HTTP: ${response.status}, ${response.statusText}]`);
            }

            csrfToken = response.headers.get("x-csrf-token");

            const fileMetadata = {
                workspace: workspace,
                project: projectName,
                filePath: filePath,
                isGit: requestMetadata.isGitProject,
                git: '',
                modified: '',
                sourceCode: '',
                importedFilesNames: [],
            };

            if (requestMetadata.isGitProject) {
                const fileObject = await response.json();
                fileMetadata.git = fileObject.original ?? ""; // File is not in git
                fileMetadata.modified = fileObject.modified;
                fileMetadata.sourceCode = fileObject.modified;
            } else {
                const fileContent = await response.text();
                fileMetadata.modified = fileContent;
                fileMetadata.sourceCode = fileContent;
            }
            if (TypeScriptUtils.isTypeScriptFile(path)) {
                const importedFilesNames = TypeScriptUtils.getImportedModuleFiles(fileMetadata.sourceCode);
                // @ts-ignore
                fileMetadata.importedFilesNames.push(...importedFilesNames);
            }
            return fileMetadata;
        } catch (e) {
            // @ts-ignore
            Utils.logErrorMessage(e.message);
            throw e;
        }
    };

    async saveText(text, fileName) {
        try {
            fileName = fileName || this.resolvePath();
            if (!fileName) {
                throw new Error(`Unable to save file [${fileName}], file query parameter is not present in the URL`);
            }

            const response = await fetch(`${WORKSPACE_API}${fileName}`, {
                method: 'PUT',
                body: text,
                headers: {
                    'X-Requested-With': 'Fetch',
                    'X-CSRF-Token': csrfToken,
                    'Dirigible-Editor': 'Monaco',
                    'Content-Type': 'text/plain'
                }
            });

            if (!response.ok) {
                throw new Error(`Unable to save [${fileName}, HTTP: ${response.status}, ${response.statusText}]`);
            }

            Utils.setEditorDirty(fileName, false);

            messageHub.post({
                name: fileName.substring(fileName.lastIndexOf('/') + 1),
                path: fileName.substring(fileName.indexOf('/', 1)),
                contentType: ViewParameters.parameters.contentType,
                workspace: fileName.substring(1, fileName.indexOf('/', 1)),
                status: ViewParameters.parameters.gitName && lineDecorations.length ? 'modified' : 'unmodified'
            }, 'ide.file.saved');

            Utils.logMessage(`File '${fileName}' saved`);

            if (TypeScriptUtils.isTypeScriptFile(fileName)) {
                messageHub.post({
                    fileName
                }, 'ide.ts.reload');
            }
        } catch (e) {
            // @ts-ignore
            Utils.logErrorMessage(e.message);
            throw e;
        }
    };

    /**
     * "workspace", "repository" or "registry"
     */
    #getResourceType() {
        return ViewParameters.useParameters ? ViewParameters.parameters.resourceType : FileIO.EDITOR_URL.searchParams.get('rtype');
    }

    #resolveGitProjectName() {
        return ViewParameters.useParameters ? ViewParameters.parameters.gitName : FileIO.EDITOR_URL.searchParams.get('gitName')
    }

    #buildRegistryUrl(filePath) {
        return `${REGISTRY_API}${filePath.startsWith('/') ? '' : '/'}${filePath}`;
    }

    #buildRepositoryUrl(path) {
        return `${REPOSITORY_API}${path.startsWith('/') ? '' : '/'}${path}`;
    }

    #buildWorkspaceUrl(workspace, filePath) {
        return `${WORKSPACE_API}/${workspace}/${filePath}`;
    }

    #buildFileRequestMetadata(path, workspace, filePath, loadGitMetadata) {
        let url;
        let isGitProject = false;

        switch (this.#getResourceType()) {
            case "registry":
                url = this.#buildRegistryUrl(filePath);
                break;
            case "repository":
                url = this.#buildRepositoryUrl(path);
                break;
            default:
                url = this.#buildWorkspaceUrl(workspace, filePath);
        }

        const gitProject = this.#resolveGitProjectName();
        if (loadGitMetadata && gitProject) {
            const gitFolder = "/.git/";
            const isGitFolderLocation = path.indexOf(gitFolder) > 0;
            if (isGitFolderLocation) {
                path = path.substring(path.indexOf(gitFolder) + gitFolder.length);
            }

            let diffPath;
            if (isGitFolderLocation) {
                diffPath = path.substring(path.indexOf(gitProject) + gitProject.length + 1);
            } else {
                diffPath = path.substring(path.indexOf(`/${workspace}/`) + `/${workspace}/`.length);
            }
            url = `/services/ide/git/${workspace}/${gitProject}/diff?path=${diffPath}`;
            isGitProject = true;
        }
        return {
            url: url,
            isGitProject: isGitProject,
        };
    }
}

class EditorActionsProvider {

    static #autoRevealActionEnabled = true;

    static _toggleAutoFormattingActionRegistration = undefined;
    static _toggleAutoRevealActionRegistration = undefined;

    static isAutoRevealEnabled() {
        const autoRevealEnabled = window.localStorage.getItem('DIRIGIBLE.autoRevealActionEnabled');
        return autoRevealEnabled === null || autoRevealEnabled === 'true';
    }

    static createSaveAction(formatEnabled = true) {
        const loadingMessage = document.getElementById('loadingMessage');
        return {
            id: 'dirigible-files-save',
            label: 'Save',
            keybindings: [
                monaco.KeyMod.CtrlCmd | monaco.KeyCode.KeyS
            ],
            precondition: null,
            keybindingContext: null,
            contextMenuGroupId: 'fileIO',
            contextMenuOrder: 1.5,
            run: function (editor) {
                if (loadingMessage) {
                    loadingMessage.innerText = 'Saving...';
                }
                if (DirigibleEditor.loadingOverview) {
                    DirigibleEditor.loadingOverview.classList.remove("dg-hidden");
                }
                if (formatEnabled && EditorActionsProvider.#isAutoFormattingEnabledForCurrentFile()) {
                    editor.getAction('editor.action.formatDocument').run().then(() => {
                        DirigibleEditor.saveFileContent(editor);
                    });
                } else {
                    DirigibleEditor.saveFileContent(editor);
                }
            }
        };
    }

    static createSearchAction() {
        return {
            id: 'dirigible-search',
            label: 'Search',
            keybindings: [
                monaco.KeyMod.CtrlCmd | monaco.KeyMod.Shift | monaco.KeyCode.KeyF
            ],
            precondition: null,
            keybindingContext: null,
            contextMenuGroupId: 'fileIO',
            contextMenuOrder: 1.5,
            run: function () {
                messageHub.post({
                    viewId: "search"
                }, 'ide-core.openView');
            }
        };
    }

    static createToggleAutoFormattingAction() {
        return {
            id: 'dirigible-toggle-auto-formatting',
            label: EditorActionsProvider.#isAutoFormattingEnabledForCurrentFile() ? "Disable Auto-Formatting" : "Enable Auto-Formatting",
            keybindings: [
                monaco.KeyMod.CtrlCmd | monaco.KeyMod.Shift | monaco.KeyCode.KeyD
            ],
            precondition: null,
            keybindingContext: null,
            contextMenuGroupId: 'fileIO',
            contextMenuOrder: 1.5,
            run: function (editor) {
                let fileIO = new FileIO();
                let fileName = fileIO.resolvePath();

                const filesWithDisabledFormattingListJson = window.localStorage.getItem('DIRIGIBLE.filesWithDisabledFormattingList');
                let filesWithDisabledFormattingList = undefined;
                if (filesWithDisabledFormattingListJson) {
                    filesWithDisabledFormattingList = JSON.parse(filesWithDisabledFormattingListJson);
                }

                let jsonString = null;

                if (filesWithDisabledFormattingList) {
                    if (filesWithDisabledFormattingList.includes(fileName)) {
                        const removed = filesWithDisabledFormattingList.filter(entry => entry !== fileName);
                        jsonString = JSON.stringify(removed);
                    } else {
                        filesWithDisabledFormattingList.push(fileName);
                        jsonString = JSON.stringify(filesWithDisabledFormattingList);
                    }
                } else {
                    let initialFilesWithDisabledFormattingList = new Array(fileName);
                    jsonString = JSON.stringify(initialFilesWithDisabledFormattingList);
                }

                window.localStorage.setItem('DIRIGIBLE.filesWithDisabledFormattingList', jsonString);

                if (EditorActionsProvider._toggleAutoFormattingActionRegistration) {
                    // @ts-ignore
                    EditorActionsProvider._toggleAutoFormattingActionRegistration.dispose();
                    EditorActionsProvider._toggleAutoFormattingActionRegistration = editor.addAction(EditorActionsProvider.createToggleAutoFormattingAction());
                }
            }
        };
    }

    static createToggleAutoRevealAction() {
        return {
            id: 'dirigible-toggle-auto-reveal',
            label: EditorActionsProvider.#autoRevealActionEnabled ? "Disable Auto-Reveal Active File" : "Enable Auto-Reveal Active File",
            keybindings: [],
            precondition: null,
            keybindingContext: null,
            contextMenuGroupId: 'fileIO',
            contextMenuOrder: 1.5,
            run: function (editor) {
                EditorActionsProvider.#autoRevealActionEnabled = !EditorActionsProvider.#autoRevealActionEnabled;
                window.localStorage.setItem('DIRIGIBLE.autoRevealActionEnabled', `${EditorActionsProvider.#autoRevealActionEnabled}`);

                if (EditorActionsProvider._toggleAutoRevealActionRegistration) {
                    // @ts-ignore
                    EditorActionsProvider._toggleAutoRevealActionRegistration.dispose();
                    EditorActionsProvider._toggleAutoRevealActionRegistration = editor.addAction(EditorActionsProvider.createToggleAutoRevealAction());
                }
            }
        };
    }

    static #isAutoFormattingEnabledForCurrentFile() {
        const fileIO = new FileIO();
        const fileName = fileIO.resolvePath();
        const filesWithDisabledFormattingListJson = window.localStorage.getItem('DIRIGIBLE.filesWithDisabledFormattingList');
        let filesWithDisabledFormattingList = undefined;
        if (filesWithDisabledFormattingListJson) {
            filesWithDisabledFormattingList = JSON.parse(filesWithDisabledFormattingListJson);
        }

        return !filesWithDisabledFormattingList || !filesWithDisabledFormattingList.includes(fileName);
    }
}

class DirigibleEditor {

    static dirty = false;
    static lastSavedVersionId = undefined;
    static loadingOverview = document.getElementById('loadingOverview');
    static sourceBeingChangedProgramatically = false;
    static computeDiff = new Worker("js/workers/computeDiff.js");

    static closeEditor() {
        messageHub.post({ resourcePath: ViewParameters.parameters.file }, 'ide-core.closeEditor');
    }

    static saveFileContent(editor) {
        const fileIO = new FileIO();
        fileIO.saveText(editor.getModel().getValue()).then(() => {
            DirigibleEditor.lastSavedVersionId = editor.getModel().getAlternativeVersionId();
            DirigibleEditor.dirty = false;
        });
        if (DirigibleEditor.loadingOverview) {
            DirigibleEditor.loadingOverview.classList.add("dg-hidden")
        };
    }

    static #isDirty(model) {
        return DirigibleEditor.lastSavedVersionId !== model.getAlternativeVersionId();
    }

    constructor(monaco, acornLoose, fileName, readOnly, fileType, isTemplate, fileObject) {
        this.monaco = monaco;
        this.acornLoose = acornLoose;
        this.fileName = fileName;
        this.readOnly = readOnly;
        this.fileType = fileType;
        this.isTemplate = isTemplate;
        this.fileObject = fileObject;
    }

    async init() {
        if (!this.fileName) {
            return
        }

        this.editor = await this.#createEditorInstance();

        if (TypeScriptUtils.isTypeScriptFile(this.fileName)) {
            TypeScriptUtils.loadImportedFiles(this.monaco, this.fileObject.importedFilesNames);
        }

        const mainFileUri = new this.monaco.Uri().with({ path: this.fileName });
        const model = this.monaco.editor.createModel(this.fileObject.modified, this.fileType || 'text', mainFileUri);
        DirigibleEditor.lastSavedVersionId = model.getAlternativeVersionId();

        this.editor.setModel(model);

        await TypeScriptUtils.loadDTS(this.monaco);
    }

    async #createEditorInstance() {
        const fileName = this.fileName;
        const readOnly = this.readOnly;
        const fileObject = this.fileObject;
        let diffTimeoutId;
        let importTimeoutId;

        const editor = await new Promise((resolve, reject) => {
            setTimeout(function () {
                try {
                    let containerEl = document.getElementById('embeddedEditor');
                    if (containerEl && containerEl.childElementCount > 0) {
                        for (let i = 0; i < containerEl.childElementCount; i++)
                            // @ts-ignore
                            containerEl.removeChild(containerEl.children.item(i));
                    }
                    const editorConfig = {
                        value: '',
                        automaticLayout: true,
                        readOnly: readOnly,
                    };
                    if (TypeScriptUtils.isTypeScriptFile(fileName)) {
                        // @ts-ignore
                        editorConfig.language = 'typescript';
                    }

                    const editor = this.monaco.editor.create(containerEl, editorConfig);
                    resolve(editor);
                    window.onresize = function () {
                        editor.layout();
                    };
                    if (DirigibleEditor.loadingOverview) {
                        DirigibleEditor.loadingOverview.classList.add("dg-hidden")
                    };
                } catch (err) {
                    reject(err);
                }
            });
        });

        editor.onDidChangeModel(function () {
            if (fileObject.isGit) {
                DirigibleEditor.computeDiff.postMessage({
                    oldText: fileObject.git,
                    newText: fileObject.modified
                });
            }
        });

        editor.onDidChangeModelContent(function (e) {
            if (DirigibleEditor.sourceBeingChangedProgramatically) {
                return;
            }

            if (fileObject.isGit && e.changes) {
                if (diffTimeoutId) {
                    clearTimeout(diffTimeoutId);
                }
                diffTimeoutId = setTimeout(function () {
                    DirigibleEditor.computeDiff.postMessage({
                        oldText: fileObject.git,
                        newText: editor.getValue()
                    });
                }, 200);
            }

            if (e.changes) {
                if (importTimeoutId) {
                    clearTimeout(importTimeoutId);
                }
                importTimeoutId = setTimeout(async function () {
                    const importedModuleFiles = TypeScriptUtils.getImportedModuleFiles(editor.getModel().getValue());
                    const newImportedModules = [];
                    for (const module of importedModuleFiles) {
                        let found = false;
                        for (const importedModule of fileObject.importedFilesNames) {
                            if (module === importedModule) {
                                found = true;
                            }
                        }
                        if (!found) {
                            newImportedModules.push(module);
                        }
                    }
                    if (newImportedModules.length > 0) {
                        TypeScriptUtils.loadImportedFiles(monaco, newImportedModules);
                        fileObject.importedFilesNames.push(...newImportedModules);
                    }
                }, 1000);
            }

            const dirty = DirigibleEditor.#isDirty(editor.getModel());
            if (dirty !== DirigibleEditor.dirty) {
                DirigibleEditor.dirty = dirty;
                Utils.setEditorDirty(fileName, dirty);
            }
        });

        editor.onDidFocusEditorText(function () {
            messageHub.post({ resourcePath: fileName }, 'ide-core.setFocusedEditor');
            if (EditorActionsProvider.isAutoRevealEnabled()) {
                setTimeout(() => {
                    messageHub.post({
                        data: {
                            filePath: fileName
                        }
                    }, 'projects.tree.select');
                }, 100);
            }
        });

        editor.onDidChangeCursorPosition(function (e) {
            messageHub.post(
                {
                    text: `Line ${e.position.lineNumber}, Column ${e.position.column}`
                },
                'ide.status.caret',
            );
        });

        if (!this.readOnly) {
            editor.addAction(EditorActionsProvider.createSaveAction(!this.isTemplate));
        }
        editor.addAction(EditorActionsProvider.createSearchAction());
        if (!this.isTemplate)
            EditorActionsProvider._toggleAutoFormattingActionRegistration = editor.addAction(EditorActionsProvider.createToggleAutoFormattingAction());
        EditorActionsProvider._toggleAutoRevealActionRegistration = editor.addAction(EditorActionsProvider.createToggleAutoRevealAction());

        DirigibleEditor.computeDiff.onmessage = function (event) {
            lineDecorations = editor.deltaDecorations(lineDecorations, event.data);
        };
        return editor;
    }

    configureMonaco() {
        const fileObject = this.fileObject;
        setTheme();
        this.monaco.languages.typescript.javascriptDefaults.setDiagnosticsOptions({
            noSemanticValidation: false,
            noSyntaxValidation: false,
            noSuggestionDiagnostics: false,
            diagnosticCodesToIgnore: [
                2792, // Cannot find module - for local module imports
                6196, // declared but never used - class
                1219, // Experimental support for decorators
                2307,
                2304, // Cannot find name 'exports'.(2304)
                2683, // 'this' implicitly has type 'any' because it does not have a type annotation.(2683)
                7005, // Variable 'ctx' implicitly has an 'any' type.(7005)
                7006, // Parameter 'ctx' implicitly has an 'any' type.(7006),
                7009, // 'new' expression, whose target lacks a construct signature, implicitly has an 'any' type.(7009)
                7034, // Variable 'ctx' implicitly has type 'any' in some locations where its type cannot be determined.(7034)
            ]
        });

        this.monaco.languages.typescript.typescriptDefaults.setDiagnosticsOptions({
            noSemanticValidation: false,
            noSyntaxValidation: false,
            noSuggestionDiagnostics: false,
            diagnosticCodesToIgnore: [
                6196, // declared but never used - class
                1219, // Experimental support for decorators
            ]
        });

        this.monaco.languages.typescript.javascriptDefaults.setCompilerOptions({
            target: this.monaco.languages.typescript.ScriptTarget.ESNext,
            strict: true,
            strictNullChecks: true,
            strictPropertyInitialization: true,
            alwaysStrict: true,
            allowNonTsExtensions: true,
            allowUnreachableCode: false,
            allowUnusedLabels: false,
            noUnusedParameters: true,
            noUnusedLocals: true,
            checkJs: true,
            noFallthroughCasesInSwitch: true,
            module: (this.fileName?.endsWith(".mjs") === true) ? this.monaco.languages.typescript.ModuleKind.ESNext : this.monaco.languages.typescript.ModuleKind.CommonJS,
            moduleResolution: this.monaco.languages.typescript.ModuleResolutionKind.NodeJs,
            resolveJsonModule: true,
            jsx: (this.fileName?.endsWith(".jsx") === true) ? "react" : undefined
        });

        this.monaco.languages.typescript.typescriptDefaults.setCompilerOptions({
            target: this.monaco.languages.typescript.ScriptTarget.ESNext,
            strict: true,
            strictNullChecks: true,
            strictPropertyInitialization: true,
            alwaysStrict: true,
            allowNonTsExtensions: true,
            allowUnreachableCode: false,
            allowUnusedLabels: false,
            noUnusedParameters: true,
            noUnusedLocals: true,
            checkJs: true,
            noFallthroughCasesInSwitch: true,
            module: this.monaco.languages.typescript.ModuleKind.ESNext,
            moduleResolution: this.monaco.languages.typescript.ModuleResolutionKind.NodeJs,
            esModuleInterop: true,
            resolveJsonModule: true,
            jsx: (this.fileName?.endsWith(".tsx") === true) ? "react" : undefined
        });

        this.monaco.languages.typescript.javascriptDefaults.addExtraLib('/** Loads external module: \n\n> ```\nlet res = require("http/v8/response");\nres.println("Hello World!");``` */ var require = function(moduleName: string) {return new Module();};', 'js:require.js');

        this.monaco.languages.typescript.javascriptDefaults.addExtraLib('/** $. XSJS API */ var $: any;', 'ts:$.js');

        this.monaco.languages.html.registerHTMLLanguageService('xml', {}, { documentFormattingEdits: true });

        this.monaco.languages.html.htmlDefaults.setOptions({
            format: {
                tabSize: 2,
                insertSpaces: true,
                endWithNewline: true,
                indentHandlebars: true,
                indentInnerHtml: true,
                wrapLineLength: 120,
                wrapAttributes: "auto",
                extraLiners: "head, body, /html",
                maxPreserveNewLines: null
            }
        });

        this.monaco.editor.defineTheme('quartz-dark', {
            base: 'vs-dark',
            inherit: true,
            rules: [{ background: '1c2228' }],
            colors: {
                'editor.background': '#1c2228',
                'breadcrumb.background': '#1c2228',
                'minimap.background': '#1c2228',
                'editorGutter.background': '#1c2228',
                'editorMarkerNavigation.background': '#1c2228',
                'input.background': '#29313a',
                'input.border': '#8696a9',
                'editorWidget.background': '#1c2228',
                'editorWidget.border': '#495767',
                'editorSuggestWidget.background': '#29313a',
                'dropdown.background': '#29313a',
            }
        });

        this.monaco.editor.setTheme(monacoTheme);

        messageHub.subscribe(function () {
            setTheme(false);
            monaco.editor.setTheme(monacoTheme);
        }, 'ide.themeChange');

        const getTypeScriptFileImport = (model, position, fileObject) => {
            const lineContent = model.getLineContent(position.lineNumber);
            for (const next of fileObject.importedFilesNames) {
                if (lineContent.includes(next.replace(".ts", ""))) {
                    return new FileIO().resolveFilePath(next);
                }
            }
            return undefined;
        }

        this.monaco.languages.registerImplementationProvider('typescript', {
            provideImplementation: function (model, position) {
                const filePath = getTypeScriptFileImport(model, position, fileObject);
                if (filePath) {
                    const workspace = new FileIO().resolveWorkspace();
                    const filePathTokens = filePath.split("/");
                    const fileName = filePathTokens[filePathTokens.length - 1];
                    messageHub.post({
                        resourcePath: `/${workspace}/${filePath}`,
                        resourceLabel: fileName,
                        contentType: "typescript",
                        editorId: undefined,
                        extraArgs: {
                            workspace: workspace
                        },
                    }, 'ide-core.openEditor');
                }
            }
        });

        this.monaco.languages.registerDefinitionProvider('typescript', {
            provideDefinition: function (model, position) {
                const filePath = getTypeScriptFileImport(model, position, fileObject);
                if (filePath) {
                    return [{
                        uri: model.uri,
                        range: new monaco.Range(position.lineNumber, position.column, position.lineNumber, position.column),
                    }];
                }
            }
        });
    }

    subscribeEvents() {
        const fileIO = new FileIO();
        const editor = this.editor;
        const monaco = this.monaco;
        const fileObject = this.fileObject;

        messageHub.subscribe(async function (msg) {
            const file = msg.data && typeof msg.data === 'object' && msg.data.file;
            if (file && file !== fileIO.resolvePath()) {
                return;
            }

            const model = editor.getModel();
            if (DirigibleEditor.#isDirty(model)) {
                fileIO.saveText(model.getValue()).then(() => {
                    DirigibleEditor.lastSavedVersionId = model.getAlternativeVersionId();
                    DirigibleEditor.dirty = false;
                });
            }
        }, "editor.file.save");

        messageHub.subscribe(async function () {
            const model = editor.getModel();
            if (DirigibleEditor.#isDirty(model)) {
                fileIO.saveText(model.getValue());
                DirigibleEditor.lastSavedVersionId = model.getAlternativeVersionId();
                DirigibleEditor.dirty = false;
            }
        }, "editor.file.save.all");

        messageHub.subscribe(function (event) {
            if (event.resourcePath === fileIO.resolvePath()) {
                new ViewParameters();
            }
        }, "core.editors.reloadParams");

        messageHub.subscribe(function (msg) {
            const file = msg.resourcePath;
            if (file !== fileName) {
                return;
            }
            editor.focus();
        }, "ide-core.setEditorFocusGain");

        messageHub.subscribe((event) => {
            if (event.fileName === this.fileName) {
                return;
            }
            DirigibleEditor.sourceBeingChangedProgramatically = true;
            const model = editor.getModel();
            model.setValue(model.getValue());
            DirigibleEditor.lastSavedVersionId = model.getAlternativeVersionId();
            DirigibleEditor.sourceBeingChangedProgramatically = false;

            TypeScriptUtils.loadImportedFiles(monaco, fileObject.importedFilesNames, true);
        }, "ide.ts.reload");
    }
}

class TypeScriptUtils {

    // Find all ES6 import statements and their modules
    static #IMPORT_REGEX = /import\s+(?:\{(?:\s*\w?\s*,?)*\}|(?:\*\s+as\s+\w+)|\w+)\s+from\s+['"]([^'"]+)['"]/g;

    static #IMPORTED_FILES = new Set();

    static isTypeScriptFile(fileName) {
        return fileName && fileName.endsWith(".ts");
    }

    static getImportedModuleFiles(content) {
        const importedModules = [];

        let match;
        while ((match = TypeScriptUtils.#IMPORT_REGEX.exec(content)) !== null) {
            let modulePath = match[1];
            if (!modulePath.startsWith('sdk/')) {
                if (!modulePath.endsWith(".json")) {
                    modulePath += ".ts";
                }
                importedModules.push(modulePath);
            }
        }
        return importedModules;
    }

    static isGlobalImport(path) {
        return !path.startsWith("/") && !path.startsWith("./") && !path.startsWith("../") && !path.startsWith("sdk/")
    }

    static loadImportedFiles = async (monaco, importedFiles, isReload = false) => {
        if (isReload) {
            TypeScriptUtils.#IMPORTED_FILES.clear();
        }
        const fileIO = new FileIO();
        for (const importedFile of importedFiles) {
            try {
                const importedFilePath = fileIO.resolveFilePath(importedFile);
                if (TypeScriptUtils.#IMPORTED_FILES.has(importedFilePath)) {
                    continue;
                }
                const importedFileMetadata = await fileIO.loadText(importedFile);
                let uriPath = `/${importedFileMetadata.workspace}/${importedFileMetadata.filePath}`;
                if (TypeScriptUtils.isGlobalImport(importedFile)) {
                    monaco.languages.typescript.typescriptDefaults.addExtraLib(
                        importedFileMetadata.sourceCode,
                        `file:///node_modules/@types/${importedFile.substring(0, importedFile.indexOf('.ts'))}.d.ts`
                    );
                }

                const uri = new monaco.Uri().with({ path: uriPath });
                if (isReload) {
                    const model = monaco.editor.getModel(uri);
                    model.setValue(importedFileMetadata.sourceCode);
                } else {
                    const fileType = uri.path.endsWith(".json") ? "json" : "typescript";
                    monaco.editor.createModel(importedFileMetadata.sourceCode, fileType, uri);
                }
                if (importedFileMetadata.importedFilesNames?.length > 0) {
                    const relativeImportedPaths = importedFileMetadata.importedFilesNames.map(e => fileIO.resolveRelativePath(importedFile, e));
                    TypeScriptUtils.loadImportedFiles(monaco, relativeImportedPaths, isReload);
                }
                TypeScriptUtils.#IMPORTED_FILES.add(importedFilePath);
            } catch (e) {
                Utils.logErrorMessage(e);
            }
        }
    }

    static async loadDTS(monaco) {
        const res = await fetch('/services/js/all-dts');
        const allDts = await res.json();
        for (const dts of allDts) {
            monaco.languages.typescript.javascriptDefaults.addExtraLib(dts.content, dts.filePath);
            monaco.languages.typescript.typescriptDefaults.addExtraLib(dts.content, dts.filePath);
        }

        const cachedDts = window.sessionStorage.getItem('dtsContent');
        if (cachedDts) {
            monaco.languages.typescript.javascriptDefaults.addExtraLib(cachedDts, "");
            monaco.languages.typescript.typescriptDefaults.addExtraLib(cachedDts, "");
        } else {
            const xhrModules = new XMLHttpRequest();
            xhrModules.open('GET', '/services/js/ide-monaco-extensions/api/dts.js');
            xhrModules.setRequestHeader('X-CSRF-Token', 'Fetch');
            xhrModules.onload = function (xhrModules) {
                // @ts-ignore
                const dtsContent = xhrModules.target.responseText;
                monaco.languages.typescript.javascriptDefaults.addExtraLib(dtsContent, "");
                monaco.languages.typescript.typescriptDefaults.addExtraLib(dtsContent, "");
                window.sessionStorage.setItem('dtsContent', dtsContent);
            };
            xhrModules.onerror = function (error) {
                console.error('Error loading DTS', error);
                messageHub.post({
                    message: 'Error loading DTS'
                }, 'ide.status.error');
            };
            xhrModules.send();
        }
    }
}