const WORKSPACE_API = "/services/ide/workspaces";
const REPOSITORY_API = "/services/core/repository";
const REGISTRY_API = "/services/core/repository/registry/public";

const themingHub = new ThemingHub();
const statusBarHub = new StatusBarHub();
const layoutHub = new LayoutHub();
const workspaceHub = new WorkspaceHub();

const brandingInfo = getBrandingInfo();

let csrfToken;
let lineDecorations = [];

let monacoTheme = 'vs-light';
let themeId = 'vs-light';
let headElement = document.getElementsByTagName('head')[0];

let autoThemeListener = false;

function setTheme(theme, monaco) {
    if (!theme) theme = themingHub.getSavedTheme();
    themeId = theme.id;
    let themeLinks = headElement.querySelectorAll("link[data-type='theme']");
    for (let i = 0; i < themeLinks.length; i++) {
        headElement.removeChild(themeLinks[i]);
    }
    for (let i = 0; i < theme.links.length; i++) {
        const link = document.createElement('link');
        link.type = 'text/css';
        link.href = theme.links[i];
        link.rel = 'stylesheet';
        link.setAttribute("data-type", "theme");
        headElement.appendChild(link);
    }
    if (theme.type === 'light') {
        monacoTheme = 'vs-light';
        if (monaco) monaco.editor.setTheme(monacoTheme);
        autoThemeListener = false;
    } else if (theme.type === 'dark') {
        if (themeId === 'classic-dark') monacoTheme = 'classic-dark';
        else monacoTheme = 'blimpkit-dark';
        if (monaco) monaco.editor.setTheme(monacoTheme);
        autoThemeListener = false;
    } else {
        if (window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches) {
            if (themeId.startsWith('classic')) monacoTheme = 'classic-dark';
            else monacoTheme = 'blimpkit-dark';
        } else monacoTheme = 'vs-light';
        if (monaco) monaco.editor.setTheme(monacoTheme);
        autoThemeListener = true;
    }
}

setTheme();

function getViewParameters() {
    const dataParameters = window.frameElement?.getAttribute("data-parameters");
    if (dataParameters) {
        const params = JSON.parse(dataParameters);
        const resourcePath = params["filePath"] || "";
        return {
            resourceType: params["resourceType"] ?? "workspace",
            contentType: params["contentType"] || "",
            readOnly: params["readOnly"] || false,
            gitName: params["gitName"] || "",
            resourcePath: resourcePath,
        }
    }
    return {};
}

let editorParameters = getViewParameters();
const tabId = window.frameElement?.getAttribute('tab-id');

window.addEventListener('focus', () => {
    layoutHub.focusEditor({
        id: tabId,
        path: editorParameters.resourcePath,
    });
});

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
        const fileName = fileIO.resolveResourcePath();
        const readOnly = fileIO.isReadOnly();
        const fileType = await fileIO.getFileType(fileName);
        const isTemplate = fileIO.isFileTemplate(editorParameters.resourcePath);
        const fileObject = await fileIO.loadText(editorParameters.resourcePath, true);

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
        statusBarHub.showMessage(message);
    }

    static logErrorMessage(errorMessage) {
        console.error(errorMessage);
        statusBarHub.showError(errorMessage);
    }

    static setEditorDirty(dirty) {
        layoutHub.setEditorDirty({
            path: editorParameters.resourcePath,
            dirty: dirty,
        });
    }
}

class FileIO {

    static EDITOR_URL = new URL(window.location.href);

    isReadOnly() {
        if (editorParameters.resourceType !== 'workspace') return true;
        return editorParameters.readOnly || false;
    }

    resolveResourcePath() {
        return editorParameters.resourcePath;
    }

    resolveProjectName(filePath) {
        let path = this.resolveResourcePath();
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

    getWorkspacePath(filePath) {
        if (this.#getResourceType() === 'workspace') {
            return `/${editorParameters.resourcePath.split('/')[1]}/${filePath}`
        }
        return filePath;
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
        let path = this.resolveResourcePath();
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
        const response = await fetch('/services/js/editor-monaco/api/fileTypes.js');
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
            path = path ?? this.resolveResourcePath();
            if (!path) {
                throw new Error(`Unable to load file [${path}], file query parameter is not present in the URL`);
            }
            const projectName = this.resolveProjectName(path);

            const requestMetadata = this.#buildFileRequestMetadata(path, loadGitMetadata);
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
                console.error(e);
                console.error('Failed to load file. Reatempting using the Registry API.');
                response = await fetch(this.#buildRegistryUrl(path), {
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
                project: projectName,
                filePath: path,
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
            if (!this.isReadOnly() && TypeScriptUtils.isTypeScriptFile(path)) {
                // @ts-ignore
                fileMetadata.importedFilesNames.push(...TypeScriptUtils.getImportedModuleFiles(fileMetadata.sourceCode));
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
            fileName = fileName || this.resolveResourcePath();
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

            Utils.setEditorDirty(false);

            workspaceHub.announceFileSaved({
                path: editorParameters.resourcePath,
                contentType: editorParameters.contentType,
                status: editorParameters.gitName && lineDecorations.length ? 'modified' : 'unmodified'
            });

            Utils.logMessage(`File '${fileName}' saved`);

            if (TypeScriptUtils.isTypeScriptFile(fileName)) {
                themingHub.postMessage({
                    topic: 'monaco.ts.reload',
                    data: fileName
                });
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
        return editorParameters.resourceType;
    }

    #resolveGitProjectName() {
        return editorParameters.gitName;
    }

    #buildRegistryUrl(filePath) {
        return UriBuilder().path(REGISTRY_API.split('/')).path(filePath.split('/')).build();
    }

    #buildRepositoryUrl(path) {
        return UriBuilder().path(REPOSITORY_API.split('/')).path(path.split('/')).build();
    }

    #buildWorkspaceUrl(filePath) {
        return UriBuilder().path(WORKSPACE_API.split('/')).path(filePath.split('/')).build();
    }

    #buildFileRequestMetadata(filePath, loadGitMetadata) {
        let url;
        let isGitProject = false;
        switch (this.#getResourceType()) {
            case "registry":
                url = this.#buildRegistryUrl(filePath);
                break;
            case "repository":
                url = this.#buildRepositoryUrl(filePath);
                break;
            default:
                url = this.#buildWorkspaceUrl(filePath);
        }

        const gitProject = this.#resolveGitProjectName();
        if (loadGitMetadata && gitProject) {
            const workspace = filePath.split('/')[1];
            const gitFolder = "/.git/";
            const isGitFolderLocation = filePath.indexOf(gitFolder) > 0;
            if (isGitFolderLocation) {
                filePath = filePath.substring(path.indexOf(gitFolder) + gitFolder.length);
            }

            let diffPath;
            if (isGitFolderLocation) {
                diffPath = filePath.substring(filePath.indexOf(gitProject) + gitProject.length + 1);
            } else {
                diffPath = filePath.substring(filePath.indexOf(`/${workspace}/`) + `/${workspace}/`.length);
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
    static #autoFormatExcludedKey = `${brandingInfo.prefix}.code-editor.autoFormat.excluded`;
    static _toggleAutoFormattingActionRegistration = undefined;
    static #autoFormatEnabled = undefined;

    static isAutoFormattingEnabled() {
        if (EditorActionsProvider.#autoFormatEnabled === undefined) {
            const autoFormat = window.localStorage.getItem(`${brandingInfo.prefix}.code-editor.autoFormat`);
            return autoFormat === null || autoFormat === 'true';
        }
        return EditorActionsProvider.#autoFormatEnabled;
    }

    static setAutoFormattingEnabled(enabled) {
        EditorActionsProvider.#autoFormatEnabled = enabled;
    }

    static createSaveAction(formatEnabled = true) {
        const loadingMessage = document.getElementById('loadingMessage');
        return {
            id: 'code-editor-files-save',
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
                    DirigibleEditor.loadingOverview.classList.remove("bk-hidden");
                }
                if (formatEnabled && EditorActionsProvider.isAutoFormattingEnabled() && EditorActionsProvider.#isAutoFormattingEnabledForCurrentFile()) {
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
            id: 'code-editor-search',
            label: 'Search',
            keybindings: [
                monaco.KeyMod.CtrlCmd | monaco.KeyMod.Shift | monaco.KeyCode.KeyF
            ],
            precondition: null,
            keybindingContext: null,
            contextMenuGroupId: 'fileIO',
            contextMenuOrder: 1.5,
            run: function () {
                layoutHub.openView({ id: 'search' });
            }
        };
    }

    static createToggleAutoFormattingAction() {
        return {
            id: 'code-editor-toggle-auto-formatting',
            label: "Toggle Auto-Formatting",
            keybindings: [
                monaco.KeyMod.CtrlCmd | monaco.KeyMod.Shift | monaco.KeyCode.KeyD
            ],
            precondition: null,
            keybindingContext: null,
            contextMenuGroupId: 'fileIO',
            contextMenuOrder: 1.5,
            run: function () {
                const fileIO = new FileIO();
                const fileName = fileIO.resolveResourcePath();

                const filesWithDisabledFormattingListJson = window.localStorage.getItem(EditorActionsProvider.#autoFormatExcludedKey);
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

                window.localStorage.setItem(EditorActionsProvider.#autoFormatExcludedKey, jsonString);
                themingHub.postMessage({ topic: 'code-editor.settings.update', data: { fileName: fileName } });
            }
        };
    }

    static #isAutoFormattingEnabledForCurrentFile() {
        const fileIO = new FileIO();
        const fileName = fileIO.resolveResourcePath();
        const filesWithDisabledFormattingListJson = window.localStorage.getItem(EditorActionsProvider.#autoFormatExcludedKey);
        let filesWithDisabledFormattingList = undefined;
        if (filesWithDisabledFormattingListJson) {
            filesWithDisabledFormattingList = JSON.parse(filesWithDisabledFormattingListJson);
        }

        return !filesWithDisabledFormattingList || !filesWithDisabledFormattingList.includes(fileName);
    }
}

class DirigibleEditor {
    static fileIO = new FileIO();
    static dirty = false;
    static lastSavedVersionId = undefined;
    static loadingOverview = document.getElementById('loadingOverview');
    static sourceBeingChangedProgramatically = false;
    static computeDiff = new Worker("js/workers/computeDiff.js");

    static isAutoBracketsEnabled() {
        const autoBracketsEnabled = window.localStorage.getItem(`${brandingInfo.prefix}.code-editor.autoBrackets`);
        return autoBracketsEnabled === null || autoBracketsEnabled === 'true';
    }

    static isMinimapAutohideEnabled() {
        const minimapAutohideEnabled = window.localStorage.getItem(`${brandingInfo.prefix}.code-editor.minimapAutohide`);
        return minimapAutohideEnabled === null || minimapAutohideEnabled === 'true';
    }

    static getRenderWhitespace() {
        const whitespace = window.localStorage.getItem(`${brandingInfo.prefix}.code-editor.whitespace`);
        return whitespace ?? 'trailing';
    }

    static getWordWrap() {
        const wordWrap = window.localStorage.getItem(`${brandingInfo.prefix}.code-editor.wordWrap`);
        return wordWrap ?? 'off';
    }

    static closeEditor() {
        layoutHub.closeEditor({
            path: editorParameters.resourcePath,
        });
    }

    static saveFileContent(editor) {
        DirigibleEditor.fileIO.saveText(editor.getModel().getValue()).then(() => {
            DirigibleEditor.lastSavedVersionId = editor.getModel().getAlternativeVersionId();
            DirigibleEditor.dirty = false;
        });
        if (DirigibleEditor.loadingOverview) {
            DirigibleEditor.loadingOverview.classList.add("bk-hidden")
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
        if (!this.fileName) return;

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
                        autoClosingBrackets: DirigibleEditor.isAutoBracketsEnabled(),
                        renderWhitespace: DirigibleEditor.getRenderWhitespace(),
                        wordWrap: DirigibleEditor.getWordWrap(),
                        minimap: {
                            autohide: DirigibleEditor.isMinimapAutohideEnabled(),
                        }
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
                        DirigibleEditor.loadingOverview.classList.add("bk-hidden")
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
                        if (!fileObject.importedFilesNames.includes(module)) {
                            newImportedModules.push(module);
                        }
                    }
                    if (newImportedModules.length > 0) {
                        fileObject.importedFilesNames.length = 0;
                        fileObject.importedFilesNames.push(...importedModuleFiles);
                        TypeScriptUtils.loadImportedFiles(monaco, newImportedModules.sort());
                    }
                }, 1000);
            }

            const dirty = DirigibleEditor.#isDirty(editor.getModel());
            if (dirty !== DirigibleEditor.dirty) {
                DirigibleEditor.dirty = dirty;
                Utils.setEditorDirty(fileName, dirty);
            }
        });

        editor.onDidChangeCursorPosition(function (e) {
            statusBarHub.showLabel(`Line ${e.position.lineNumber}, Column ${e.position.column}`);
        });

        if (!this.readOnly) {
            editor.addAction(EditorActionsProvider.createSaveAction(!this.isTemplate));
        }
        editor.addAction(EditorActionsProvider.createSearchAction());
        if (!this.isTemplate && EditorActionsProvider.isAutoFormattingEnabled()) {
            EditorActionsProvider._toggleAutoFormattingActionRegistration = editor.addAction(EditorActionsProvider.createToggleAutoFormattingAction());
        }

        DirigibleEditor.computeDiff.onmessage = function (event) {
            lineDecorations = editor.deltaDecorations(lineDecorations, event.data);
        };
        return editor;
    }

    configureMonaco() {
        const fileObject = this.fileObject;

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

        this.monaco.languages.typescript.javascriptDefaults.addExtraLib('/** $. XSJS API */ var $: any;', 'ts:$.js');

        this.monaco.languages.html.registerHTMLLanguageService('xml', {}, { documentFormattingEdits: true });

        this.monaco.languages.html.htmlDefaults.setOptions({
            format: {
                tabSize: 2,
                insertSpaces: true,
                endWithNewline: true,
                indentHandlebars: true,
                indentInnerHtml: true,
                wrapLineLength: 240,
                wrapAttributes: "auto",
                extraLiners: "head, body, /html",
                maxPreserveNewLines: null
            }
        });

        this.monaco.editor.defineTheme('blimpkit-dark', {
            base: 'vs-dark',
            inherit: true,
            rules: [{ background: '1d1d1d' }],
            colors: {
                'editor.background': '#1d1d1d',
                'breadcrumb.background': '#1d1d1d',
                'minimap.background': '#1d1d1d',
                'editorGutter.background': '#1d1d1d',
                'editorMarkerNavigation.background': '#1d1d1d',
                'input.background': '#242424',
                'input.border': '#4e4e4e',
                'editorWidget.background': '#1d1d1d',
                'editorWidget.border': '#313131',
                'editorSuggestWidget.background': '#262626',
                'dropdown.background': '#262626',
            }
        });

        this.monaco.editor.defineTheme('classic-dark', {
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
        window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', event => {
            if (autoThemeListener) {
                if (event.matches) {
                    if (themeId.startsWith('classic')) monacoTheme = 'classic-dark';
                    else monacoTheme = 'blimpkit-dark';
                } else monacoTheme = 'vs-light';
                this.monaco.editor.setTheme(monacoTheme);
            }
        });

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
                    layoutHub.openEditor({
                        path: DirigibleEditor.fileIO.getWorkspacePath(filePath),
                        params: { resourceType: 'workspace' },
                        contentType: "plain/text",
                    });
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
        const fileName = this.fileName;
        const editor = this.editor;
        const monaco = this.monaco;
        const fileObject = this.fileObject;

        if (!this.isTemplate) {
            themingHub.addMessageListener({
                topic: 'code-editor.settings.update', handler: (data) => {
                    if (data.setting === 'autoFormatAll') {
                        EditorActionsProvider.setAutoFormattingEnabled(data.value);
                        if (data.value && !EditorActionsProvider._toggleAutoFormattingActionRegistration) {
                            EditorActionsProvider._toggleAutoFormattingActionRegistration = editor.addAction(EditorActionsProvider.createToggleAutoFormattingAction());
                        } else if (EditorActionsProvider._toggleAutoFormattingActionRegistration) {
                            EditorActionsProvider._toggleAutoFormattingActionRegistration.dispose();
                            EditorActionsProvider._toggleAutoFormattingActionRegistration = undefined;
                        }
                    } else if (data.setting === 'autoFormat') {
                        if (data.fileName && data.fileName === fileName && EditorActionsProvider._toggleAutoFormattingActionRegistration) {
                            // @ts-ignore
                            EditorActionsProvider._toggleAutoFormattingActionRegistration.dispose();
                            EditorActionsProvider._toggleAutoFormattingActionRegistration = editor.addAction(EditorActionsProvider.createToggleAutoFormattingAction());
                        }
                    } else if (data.setting === 'autoBrackets') {
                        editor.updateOptions({ autoClosingBrackets: data.value });
                    } else if (data.setting === 'minimapAutohide') {
                        editor.updateOptions({ minimap: { autohide: data.value } });
                    } else if (data.setting === 'whitespace') {
                        editor.updateOptions({ renderWhitespace: data.value });
                    } else if (data.setting === 'wordWrap') {
                        editor.updateOptions({ wordWrap: data.value });
                    }
                }
            });
        }

        themingHub.onThemeChange((theme) => {
            setTheme(theme, this.monaco);
        });

        workspaceHub.onSaveFile(async function (data) {
            if (data.path && data.path === fileIO.resolveResourcePath()) {
                const model = editor.getModel();
                if (DirigibleEditor.#isDirty(model)) {
                    fileIO.saveText(model.getValue()).then(() => {
                        DirigibleEditor.lastSavedVersionId = model.getAlternativeVersionId();
                        DirigibleEditor.dirty = false;
                    });
                }
            }
        });

        workspaceHub.onSaveAll(async function () {
            const model = editor.getModel();
            if (DirigibleEditor.#isDirty(model)) {
                fileIO.saveText(model.getValue());
                DirigibleEditor.lastSavedVersionId = model.getAlternativeVersionId();
                DirigibleEditor.dirty = false;
            }
        });

        layoutHub.onReloadEditorParams((data) => {
            if (data.path === editorParameters.resourcePath) {
                editorParameters = getViewParameters();
            }
        });

        layoutHub.onFocusEditor((data) => {
            if (data.path === fileIO.resolveResourcePath()) editor.focus();
        });

        themingHub.addMessageListener({
            topic: 'monaco.ts.reload',
            handler: (fileData) => {
                if (fileData === editorParameters.resourcePath) {
                    return;
                }
                DirigibleEditor.sourceBeingChangedProgramatically = true;
                const model = editor.getModel();
                model.setValue(model.getValue());
                DirigibleEditor.lastSavedVersionId = model.getAlternativeVersionId();
                TypeScriptUtils.clearImportedFiles();
                TypeScriptUtils.loadImportedFiles(monaco, fileObject.importedFilesNames, true);
            },
        });
    }
}

class TypeScriptUtils {

    // Find all ES6 import statements and their modules
    static #IMPORT_REGEX = /import\s+(?:\{(?:\s*\w?\s*,?)*\}|(?:\*\s+as\s+\w+)|\w+)\s+from\s+['"]([^'"]+)['"]/g;
    // If we want side-effect imports:
    // import\s+(?:\{(?:\s*\w+\s*,?)*\}|(?:\*\s+as\s+\w+)|\w+)?\s*(?:from\s+['"]([^'"]+)['"]|['"]([^'"]+)['"])

    static #IMPORTED_FILES = new Set();

    // Must call this before calling `loadImportedFiles` with isReload = true
    static clearImportedFiles() {
        TypeScriptUtils.#IMPORTED_FILES.clear();
    }

    static isTypeScriptFile(fileName) {
        return fileName && fileName.endsWith(".ts");
    }

    static getImportedModuleFiles(content) {
        const importedModules = [];

        const contentWithoutComments = content.replace(/\/\/.*$/gm, '').replace(/\/\*[\s\S]*?\*\//g, '');

        let match;
        while ((match = TypeScriptUtils.#IMPORT_REGEX.exec(contentWithoutComments)) !== null) {
            let modulePath = match[1].trim();
            if (!modulePath.startsWith('@aerokit/sdk/') && modulePath !== '' && modulePath !== '.' && modulePath !== './') {
                if (!modulePath.endsWith(".json")) {
                    modulePath += ".ts";
                }
                importedModules.push(modulePath);
            }
        }
        return importedModules.sort();
    }

    static isGlobalImport(path) {
        return !path.startsWith("/") && !path.startsWith("./") && !path.startsWith("../") && !path.startsWith("@aerokit/sdk/")
    }

    // The `importedFiles` array must be sorted before passed
    static loadImportedFiles = async (monaco, importedFiles, isReload = false) => {
        const fileIO = new FileIO();
        function createModel(sourceCode, uri) {
            const fileType = uri.path.endsWith(".json") ? "json" : "typescript";
            monaco.editor.createModel(sourceCode, fileType, uri);
        }

        async function relativeImports(importedFile, importedFileMetadata) {
            if (importedFileMetadata.importedFilesNames?.length > 0) {
                const relativeImportedPaths = importedFileMetadata.importedFilesNames.map(e => fileIO.resolveRelativePath(importedFile, e)).sort();
                if (JSON.stringify(importedFiles) !== JSON.stringify(relativeImportedPaths)) {
                    await TypeScriptUtils.loadImportedFiles(monaco, relativeImportedPaths, isReload);
                }
            }
        }

        for (const importedFile of importedFiles) {
            const importedFilePath = fileIO.getWorkspacePath(fileIO.resolveFilePath(importedFile));
            if (editorParameters.resourcePath === importedFilePath) {
                continue;
            } else if (TypeScriptUtils.#IMPORTED_FILES.has(importedFilePath)) {
                continue;
            }
            try {
                const importedFileMetadata = await fileIO.loadText(importedFilePath);
                let uriPath = importedFileMetadata.filePath;
                if (TypeScriptUtils.isGlobalImport(importedFile)) {
                    monaco.languages.typescript.typescriptDefaults.addExtraLib(
                        importedFileMetadata.sourceCode,
                        `file:///node_modules/@types/${importedFile.substring(0, importedFile.indexOf('.ts'))}.d.ts`
                    );
                }

                const uri = new monaco.Uri().with({ path: uriPath });
                if (isReload) {
                    const model = monaco.editor.getModel(uri);
                    if (model) {
                        model.setValue(importedFileMetadata.sourceCode);
                    } else {
                        createModel(importedFileMetadata.sourceCode, uri);
                    }
                } else {
                    createModel(importedFileMetadata.sourceCode, uri);
                }
                TypeScriptUtils.#IMPORTED_FILES.add(importedFilePath);
                await relativeImports(importedFile, importedFileMetadata);
            } catch (e) {
                Utils.logErrorMessage(e);
            }
        }
        DirigibleEditor.sourceBeingChangedProgramatically = false;
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
            xhrModules.open('GET', '/services/js/editor-monaco-extensions/api/dts.js');
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
                statusBarHub.showError('Error loading DTS');
            };
            xhrModules.send();
        }
    }
}
