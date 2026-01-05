/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors
 * SPDX-License-Identifier: EPL-2.0
 */
const documents = angular.module('documents', ['platformView', 'platformSplit', 'blimpKit', 'angularFileUpload', 'platformLocale']);
class HistoryStack {

    history = {
        idx: -1,
        state: []
    };

    hasBack() {
        return this.history.idx > 0;
    }

    hasForward() {
        const { idx, state } = this.history;
        return idx < state.length - 1;
    }

    goBack(callback) {
        if (this.hasBack()) {
            const stateItem = this.history.state[--this.history.idx];

            callback(stateItem);
        }
    }

    goForward(callback) {
        if (this.hasForward()) {
            const stateItem = this.history.state[++this.history.idx];

            callback(stateItem);
        }
    }

    push(stateItem) {
        if (this.history.idx >= 0)
            this.history.state.length = this.history.idx + 1;

        this.history.state.push(stateItem);
        this.history.idx++;
    }
}
documents.controller('DocumentsController', ($scope, $http, $timeout, $element, $document, ButtonStates, FileUploader, LocaleService) => {
    const dialogHub = new DialogHub();
    const notificationHub = new NotificationHub();
    const documentsApi = '/services/js/documents/api/documents.js';
    const folderApi = '/services/js/documents/api/documents.js/folder';
    const zipApi = '/services/js/documents/api/documents.js/zip';
    const unknownFileTypeIcon = 'sap-icon--document';
    const knownFileTypesIcons = {
        'sap-icon--syntax': ['js', 'mjs', 'xsjs', 'ts', 'json'],
        'sap-icon--number-sign': ['css', 'less', 'scss'],
        'sap-icon--text': ['txt'],
        'sap-icon--pdf-attachment': ['pdf'],
        'sap-icon--picture': ['ico', 'bmp', 'png', 'jpg', 'jpeg', 'gif', 'svg', 'webp'],
        'sap-icon--document-text': ['extension', 'extensionpoint', 'edm', 'model', 'dsm', 'schema', 'bpmn', 'job', 'listener', 'websocket', 'roles', 'access', 'table', 'view', 'scheme', 'camel'],
        'sap-icon--attachment-html': ['html', 'xhtml', 'xml'],
        'sap-icon--attachment-zip-file': ['zip', 'bzip2', 'gzip', 'tar', 'wim', 'xz', '7z', 'rar'],
        'sap-icon--doc-attachment': ['doc', 'docx', 'odt', 'rtf'],
        'sap-icon--excel-attachment': ['xls', 'xlsx', 'ods'],
        'sap-icon--ppt-attachment': ['ppt', 'pptx', 'odp'],
    };

    let iframe;

    angular.element($document[0]).ready(() => {
        iframe = $document[0].getElementById('preview-iframe');
        iframe.onload = () => $scope.$evalAsync(() => {
            $scope.previewLoading = false;
        });
        iframe.onerror = () => $scope.$evalAsync(() => {
            console.error(`Error while loading preview for ${$scope.selectedFile.name}`);
            $scope.previewLoading = false;
        });
    });

    $scope.loading = false;
    $scope.canPreview = true;
    $scope.previewType = 'web';
    $scope.csvData = {
        headers: [],
        rows: [],
    };
    $scope.selectedFile = null;
    const papaConfig = {
        worker: false,
        download: true,
        delimitersToGuess: [',', '\t', '|', ';', '#', '~', Papa.RECORD_SEP, Papa.UNIT_SEP],
        header: true,
        skipEmptyLines: true,
        complete: (papa) => {
            $scope.$evalAsync(() => {
                for (let h in papa.meta.fields) {
                    $scope.csvData.headers.push(papa.meta.fields[h]);
                }
                for (let r = 0; r < papa.data.length; r++) {
                    const row = [];
                    for (let ri in papa.data[r]) {
                        row.push(papa.data[r][ri]);
                    }
                    $scope.csvData.rows.push(row);
                }
                $scope.previewLoading = false;
            });
        }
    };
    $scope.downloadPath = '/services/js/documents/api/documents.js/download';
    $scope.previewPath = '/services/js/documents/api/documents.js/preview';
    $scope.downloadZipPath = zipApi;
    $scope.selection = {
        allSelected: false
    };
    $scope.search = {};

    $scope.keyEvent = (event) => {
        if (event.originalEvent.key === 'Escape') {
            $scope.search.filterBy = '';
            $scope.search.displaySearch = false;
            $scope.clearSelection();
        }
    };

    $scope.breadcrumbs = new Breadcrumbs();
    $scope.history = new HistoryStack();

    $scope.getFileExtension = (fileName) => fileName.substring(fileName.lastIndexOf('.') + 1, fileName.length).toLowerCase();

    $scope.getFileIcon = (fileName) => {
        const ext = $scope.getFileExtension(fileName);
        const ret = Object.entries(knownFileTypesIcons).find(([_icon, exts]) => exts.indexOf(ext) >= 0);
        return ret ? ret[0] : unknownFileTypeIcon;
    };

    $scope.hasBack = () => $scope.history.hasBack();
    $scope.hasForward = () => $scope.history.hasForward();
    $scope.goBack = () => $scope.history.goBack(path => loadFolder(path));
    $scope.goForward = () => $scope.history.goForward(path => loadFolder(path));

    $scope.getFullPath = (itemName) => {
        const path = $scope.folder.path ? ($scope.folder.path + '/' + itemName) : itemName;
        return path.replace(/\/\//g, '/');
    };

    const isDocument = (item) => item && item.type === 'cmis:document';
    $scope.isFolder = (item) => item && item.type === 'cmis:folder';

    $scope.clearSelection = () => {
        setSelectedFile(null);
        $scope.selection.allSelected = false;
        $scope.folder.children.forEach(item => item.selected = false);
    };

    $scope.handleExplorerClick = (cmisObject, e) => {
        e.stopPropagation();

        if ($scope.isFolder(cmisObject)) {
            openFolder($scope.getFullPath(cmisObject.name));
        } else {
            setSelectedFile(cmisObject);
        }
    };

    $scope.writeAccessAllowed = (document) => !(document.readOnly === true);

    $scope.crumbsChanged = (entry) => {
        openFolder(entry.path);
    };

    $scope.selectAllChanged = () => {
        $scope.folder.children.forEach(item => item.selected = $scope.selection.allSelected);
    };

    $scope.selectionChanged = () => {
        $scope.selection.allSelected = $scope.folder.children.every(item => item.selected);
    };

    $scope.isDeleteItemsButtonEnabled = () => $scope.folder && $scope.folder.children.some(x => x.selected);

    $scope.isDlFileButtonEnabled = () => {
        if ($scope.folder) {
            let selected = 0;
            for (let i = 0; i < $scope.folder.children.length; i++) {
                if ($scope.folder.children[i].selected && isDocument($scope.folder.children[i])) {
                    if (selected === 1) return false;
                    else selected = 1;
                }
            }
            return selected === 1;
        }
        return false;
    };

    const getFilePreviewUrl = (item) => isDocument(item) ?
        `${$scope.previewPath}?path=${$scope.getFullPath(item.name)}` : 'about:blank';

    $scope.getFileDownloadUrl = (item) => isDocument(item) ?
        `${$scope.downloadPath}?path=${$scope.getFullPath(item.name)}` : 'about:blank';

    $scope.showNewFolderDialog = (value = '', errorMsg, excluded = []) => {
        dialogHub.showFormDialog({
            title: LocaleService.t('documents:newFolder', 'New Folder'),
            form: {
                'name': {
                    label: LocaleService.t('name', 'Name'),
                    controlType: 'input',
                    type: 'text',
                    inputRules: {
                        excluded: excluded,
                    },
                    placeholder: LocaleService.t('documents:enterFolName', 'Enter folder name...'),
                    submitOnEnter: true,
                    focus: true,
                    required: true,
                    value: value,
                    errorMsg: errorMsg,
                },
            },
            submitLabel: LocaleService.t('ok', 'OK'),
            cancelLabel: LocaleService.t('cancel', 'Cancel')
        }).then((form) => {
            if (form) {
                $scope.$evalAsync(() => {
                    $scope.loading = true;
                });
                $http.post(folderApi, { parentFolder: $scope.folder.path, name: form['name'] })
                    .then(() => {
                        refreshFolder();
                    }, (data) => {
                        $scope.$evalAsync(() => {
                            $scope.loading = false;
                        });
                        const message = data.data['err'] && data.data.err.message ? data.data.err.message : LocaleService.t('documents:errMsg.createFolder', 'Could not create folder. Check console for errors.');
                        $scope.showNewFolderDialog(form['name'], message, [form['name']]);
                    });
            }
        }, (error) => {
            console.error(error);
        });
    };

    let itemToRename;

    $scope.showRenameItemDialog = (item, e) => {
        e.stopPropagation();

        itemToRename = {
            name: item.name
        };

        const itemType = isDocument(item) ? 'file' : 'folder';

        dialogHub.showFormDialog({
            title: `${LocaleService.t('rename', 'Rename')} ${itemType}`,
            form: {
                'name': {
                    label: LocaleService.t('name', 'Name'),
                    controlType: 'input',
                    type: 'text',
                    submitOnEnter: true,
                    focus: true,
                    required: true,
                    value: item.name,
                },
            },
            submitLabel: LocaleService.t('ok', 'OK'),
            cancelLabel: LocaleService.t('cancel', 'Cancel')
        }).then((form) => {
            if (form) {
                $scope.$evalAsync(() => {
                    $scope.loading = true;
                });
                $http({
                    url: documentsApi,
                    method: 'PUT',
                    data: { path: $scope.getFullPath(itemToRename.name), name: form['name'] }
                }).then(() => {
                    itemToRename = null;
                    refreshFolder();
                }, (data) => {
                    $scope.$evalAsync(() => {
                        $scope.loading = false;
                    });
                    dialogHub.showAlert({
                        title: LocaleService.t('documents:errMsg.renameTitle', 'Rename failed'),
                        message: data.data['err'] && data.data.err.message ? data.data.err.message : LocaleService.t('documents:errMsg.rename', { name: form['name'] }),
                        type: AlertTypes.Error,
                        preformatted: false,
                    });
                });
            }
        }, (error) => {
            console.error(error);
        });
    };

    let itemsToDelete;

    $scope.showDeleteSingleItemDialog = (item, e) => {
        e.stopPropagation();

        itemsToDelete = [{
            name: item.name
        }];

        const title = isDocument(item) ? LocaleService.t('documents:deleteActions.deleteFile', 'Delete file') : LocaleService.t('documents:deleteActions.deleteFolder', 'Delete folder');
        const message = LocaleService.t('documents:deleteActions.deleteItem', { name: item.name });

        dialogHub.showDialog({
            title: title,
            message: message,
            preformatted: true,
            buttons: [
                { id: 'del', label: LocaleService.t('delete', 'Delete'), state: ButtonStates.Negative },
                { id: 'cancel', label: LocaleService.t('cancel', 'Cancel') }
            ]
        }).then((buttonId) => {
            if (buttonId === 'del') deleteItems();
        }, (error) => {
            console.error(error);
        });
    };

    $scope.showDeleteItemsDialog = (e) => {
        e.stopPropagation();

        itemsToDelete = $scope.folder.children
            .filter(item => item.selected)
            .map(item => ({ name: item.name }));

        function getItemList() {
            let list = '';
            for (let i = 0; i < itemsToDelete.length; i++) {
                list += itemsToDelete[i].name + '\n';
            }
            return list;
        }

        const message = itemsToDelete.length < 10 ?
            LocaleService.t('documents:deleteActions.deleteFollowing', { items: getItemList() }) :
            LocaleService.t('documents:deleteActions.deleteSelected', { num: itemsToDelete.length });

        dialogHub.showDialog({
            title: LocaleService.t('documents:deleteActions.deleteItems', 'Delete items'),
            message: message,
            preformatted: true,
            buttons: [
                { id: 'del', label: LocaleService.t('delete', 'Delete'), state: ButtonStates.Negative },
                { id: 'cancel', label: LocaleService.t('cancel', 'Cancel') }
            ]
        }).then((buttonId) => {
            if (buttonId === 'del') deleteItems();
        }, (error) => {
            console.error(error);
        });
    };

    function deleteItems() {
        if (itemsToDelete.length > 0) {
            $scope.$evalAsync(() => {
                $scope.loading = true;
            });
            let pathsToDelete = itemsToDelete.map(item => $scope.getFullPath(item.name));
            let url = documentsApi;

            $scope.$evalAsync(() => {
                $scope.loading = true;
            });

            $http({
                url: url,
                method: 'DELETE',
                data: pathsToDelete
            }).then(() => {
                refreshFolder();
            }, (error) => {
                console.error(error);
                $scope.$evalAsync(() => {
                    $scope.loading = false;
                });
                dialogHub.showAlert({
                    title: LocaleService.t('documents:errMsg.deleteTitle', 'Failed to delete items'),
                    message: error.data.err.message ?? LocaleService.t('documents:errMsg.delete', 'Could not delete file(s). Check console for errors.'),
                    type: AlertTypes.Error,
                    preformatted: false,
                });
            });
        }
    }

    $scope.canPreviewFile = (fileName) => {
        const type = fileName.substring(fileName.lastIndexOf('.') + 1);
        switch (type) {
            case 'edm':
            case 'dsm':
            case 'bpmn':
            case 'job':
            case 'xsjob':
            case 'calculationview':
            case 'websocket':
            case 'hdi':
            case 'hdbtable':
            case 'hdbstructurе':
            case 'hdbstructure':
            case 'hdbview':
            case 'hdbtablefunction':
            case 'hdbprocedure':
            case 'hdbschema':
            case 'hdbsynonym':
            case 'hdbdd':
            case 'hdbsequence':
            case 'hdbcalculationview':
            case 'xsaccess':
            case 'xsprivileges':
            case 'xshttpdest':
            case 'listener':
            case 'extensionpoint':
            case 'extension':
            case 'table':
            case 'view':
            case 'access':
            case 'roles':
            case 'sh':
            case 'csvim':
            case 'hdbti':
            case 'camel':
            case 'form':
                $scope.canPreview = false;
                return false;
            default:
                $scope.canPreview = true;
                return true;
        }
    };

    $scope.showUploadFileDialog = (args) => {
        $('#fileUpload').click();
        $scope.unpackZips = args && args.unpackZip;
    };

    function getFolder(folderPath) {
        let requestUrl = documentsApi;
        if (folderPath) {
            requestUrl += '?path=' + folderPath;
        }
        return $http.get(requestUrl);
    };

    function refreshFolder() {
        $scope.loading = true;
        getFolder($scope.folder.path)
            .then((data) => {
                $scope.$evalAsync(() => {
                    $scope.loading = false;
                });

                $scope.folder = data.data;

                if ($scope.selectedFile && $scope.folder.children.every(item => item.name !== $scope.selectedFile.name)) {
                    setSelectedFile(null);
                }
            }, (error) => {
                console.error(error);
                notificationHub.show({ type: 'negative', title: LocaleService.t('documents:errMsg.refreshTitle', 'Refresh failed'), description: LocaleService.t('documents:errMsg.refresh', 'Could not refresh folder. Check console for errors.') });
                $scope.$evalAsync(() => {
                    $scope.loading = false;
                });
            });
    };

    function setUploaderFolder(folderPath) {
        $scope.uploader.url = documentsApi + '?path=' + folderPath;
    };

    function setCurrentFolder(folderData) {
        $scope.folder = folderData;
        $scope.breadcrumbs.parse(folderData.path);
        setUploaderFolder($scope.folder.path);
        $scope.clearSelection();
    };

    function setPreviewer() {
        $scope.previewLoading = true;
        $scope.csvData.headers.length = 0;
        $scope.csvData.rows.length = 0;
        if ($scope.selectedFile.name.endsWith('.csv')) {
            $scope.previewType = 'csv';
            Papa.parse($scope.getFileDownloadUrl($scope.selectedFile), papaConfig);
        } else if (iframe) {
            $scope.previewType = 'web';
            iframe.contentWindow.location.replace(getFilePreviewUrl($scope.selectedFile));
        }
    }

    function setSelectedFile(selectedFile) {
        if (selectedFile === null) $scope.selectedFile = selectedFile;
        else if ($scope.canPreviewFile(selectedFile.name)) {
            $scope.selectedFile = selectedFile;
            setPreviewer();
        }
    };

    function openFolder(path) {
        if (path) {
            $scope.history.push(path);
        }
        loadFolder(path);
    };

    function loadFolder(path) {
        getFolder(path)
            .then(data => {
                setCurrentFolder(data.data);
            }, data => {
                dialogHub.showAlert({
                    title: LocaleService.t('documents:errMsg.openFolderTitle', 'Failed to open folder'),
                    message: data.data['err'] && data.data.err.message ? data.data.err.message : LocaleService.t('documents:errMsg.openFolder', 'Could not open folder. Check console for errors.'),
                    type: AlertTypes.Error,
                    preformatted: false,
                });
            });
    };

    // FILE UPLOADER

    $scope.uploader = new FileUploader({
        url: documentsApi
    });

    $scope.uploader.headers['X-Requested-With'] = 'Fetch';

    // UPLOADER FILTERS

    $scope.uploader.filters.push({
        name: 'customFilter',
        fn: function (_item /*{File|FileLikeObject}*/, _options) {
            return this.queue.length < 100;
        }
    });

    // UPLOADER CALLBACKS
    $scope.uploader.onAfterAddingAll = (_addedFileItems) => {
        $scope.$evalAsync(() => {
            $scope.loading = true;
        });
        $scope.uploader.uploadAll();
    };
    $scope.uploader.onBeforeUploadItem = (item) => {
        if ($scope.unpackZips && item.file.name.endsWith('.zip')) {
            item.url = zipApi + '?path=' + $scope.folder.path;
        }

        if ($scope.overwrite) {
            item.url = item.url + '&overwrite=true';
        }
    };
    $scope.uploader.onErrorItem = (_fileItem, response, _status, _headers) => {
        $scope.$evalAsync(() => {
            $scope.loading = false;
        });
        notificationHub.show({ type: 'negative', title: LocaleService.t('documents:errMsg.uploadTitle', 'Failed to upload item'), description: response.err.message ?? LocaleService.t('documents:errMsg.uploadTitle', 'Could not upload item. Check console for errors.') });
    };
    $scope.uploader.onCompleteAll = () => {
        refreshFolder();
    };

    // Upload with drag&drop
    window.addEventListener('dragenter', (event) => {
        if (![...event.dataTransfer.items].some(item => item.kind === 'file'))
            return;

        $scope.unpackZips = false;

        setupDragDrop();

        $scope.$evalAsync(() => $scope.showDropZone = true);
    });

    let backdrop;
    function setupDragDrop() {
        if (backdrop)
            return;

        let hideTimeout;
        const hideDropZone = () => {
            hideTimeout = $timeout(() => {
                $scope.showDropZone = false;
            }, 100);
        };

        const showDropZone = () => {
            if (hideTimeout) {
                $timeout.cancel(hideTimeout);
                hideTimeout = null;
            }

            $scope.$apply(() => $scope.showDropZone = true);
        }

        const handleDrop = (event) => {
            event.preventDefault();
            $scope.$apply(() => $scope.showDropZone = false);
        }

        const handleDragOver = (event, dropEffect) => {
            event.preventDefault();
            event.dataTransfer.dropEffect = dropEffect;

            showDropZone();
        }

        backdrop = $element.find('.drop-zone-backdrop')[0];
        let dropZone = $element.find('.drop-zone')[0];

        backdrop.addEventListener('dragover', (event) => handleDragOver(event, 'none'));
        dropZone.addEventListener('dragover', (event) => handleDragOver(event, 'copy'));

        backdrop.addEventListener('drop', handleDrop);
        dropZone.addEventListener('drop', handleDrop);

        backdrop.addEventListener('dragleave', hideDropZone);
        dropZone.addEventListener('dragleave', hideDropZone);
    }

    function Breadcrumbs() {
        this.crumbs = [];
    };

    Breadcrumbs.prototype.parse = function (path) {
        let folders = path.split('/').filter(x => x);
        let crumbs = [];
        for (let i = 0; i < folders.length; i++) {
            let crumbPath = folders.slice(0, i + 1).join('/');
            let crumb = {
                name: folders[i],
                path: crumbPath + "/"
            };
            crumbs.push(crumb);
        }
        crumbs.splice(0, 0, { name: 'Home', path: '/' });

        this.crumbs = crumbs;
    };

    openFolder('/');
});