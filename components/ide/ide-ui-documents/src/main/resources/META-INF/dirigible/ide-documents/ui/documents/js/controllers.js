/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors
 * SPDX-License-Identifier: EPL-2.0
 */
let uploader = null;

const documentsApp = angular.module('app', ['ideUI', 'ideView', 'angularFileUpload']);

documentsApp.config(['messageHubProvider', function (messageHubProvider) {
	messageHubProvider.eventIdPrefix = 'documents';
}]);

documentsApp.controller('DocServiceCtrl', ['$scope', '$http', '$timeout', '$element', 'messageHub', 'FileUploader', function ($scope, $http, $timeout, $element, messageHub, FileUploader) {
	const documentsApi = '/services/js/ide-documents/api/documents.js';
	const folderApi = '/services/js/ide-documents/api/documents.js/folder';
	const zipApi = '/services/js/ide-documents/api/documents.js/zip';
	const unknownFileTypeIcon = 'sap-icon--document';
	const knownFileTypesIcons = {
		'sap-icon--syntax': ['js', 'mjs', 'xsjs', 'ts', 'json'],
		'sap-icon--number-sign': ['css', 'less', 'scss'],
		'sap-icon--text': ['txt'],
		'sap-icon--pdf-attachment': ['pdf'],
		'sap-icon--picture': ['ico', 'bmp', 'png', 'jpg', 'jpeg', 'gif', 'svg'],
		'sap-icon--document-text': ['extension', 'extensionpoint', 'edm', 'model', 'dsm', 'schema', 'bpmn', 'job', 'listener', 'websocket', 'roles', 'constraints', 'table', 'view'],
		'sap-icon--attachment-html': ['html', 'xhtml', 'xml'],
		'sap-icon--attachment-zip-file': ['zip', 'bzip2', 'gzip', 'tar', 'wim', 'xz', '7z', 'rar'],
		'sap-icon--doc-attachment': ['doc', 'docx', 'odt', 'rtf'],
		'sap-icon--excel-attachment': ['xls', 'xlsx', 'ods'],
		'sap-icon--ppt-attachment': ['ppt', 'pptx', 'odp'],
	};

	const mapFormData = formData => formData.reduce((ret, item) => {
		ret[item.id] = item.value;
		return ret;
	}, {});

	let loading = false;
	let iframe;
	const getIFrame = () => iframe || (iframe = $element.find('#preview-iframe')[0]);

	$scope.canPreview = true;
	$scope.downloadPath = '/services/js/ide-documents/api/documents.js/download'
	$scope.previewPath = '/services/js/ide-documents/api/documents.js/preview';
	$scope.downloadZipPath = zipApi;
	$scope.selection = {
		allSelected: false
	};
	$scope.search = {};

	$scope.breadcrumbs = new Breadcrumbs();
	$scope.history = new HistoryStack();

	$scope.getFileExtension = function (fileName) {
		return fileName.substring(fileName.lastIndexOf('.') + 1, fileName.length).toLowerCase();
	};

	$scope.getFileIcon = function (fileName) {
		const ext = $scope.getFileExtension(fileName);
		const ret = Object.entries(knownFileTypesIcons).find(([icon, exts]) => exts.indexOf(ext) >= 0);
		return ret ? ret[0] : unknownFileTypeIcon;
	};

	$scope.hasBack = () => $scope.history.hasBack();
	$scope.hasForward = () => $scope.history.hasForward();
	$scope.goBack = () => $scope.history.goBack(path => loadFolder(path));
	$scope.goForward = () => $scope.history.goForward(path => loadFolder(path));

	$scope.getFullPath = function (itemName) {
		const path = $scope.folder.path ? ($scope.folder.path + '/' + itemName) : itemName;
		return path.replace(/\/\//g, '/');
	};

	$scope.isDocument = (item) => item && item.type === 'cmis:document';
	$scope.isFolder = (item) => item && item.type === 'cmis:folder';

	$scope.clearSelection = function () {
		setSelectedFile(null);
		$scope.selection.allSelected = false;
		$scope.folder.children.forEach(item => item.selected = false);
	};

	$scope.handleExplorerClick = function (cmisObject, e) {
		e.stopPropagation();

		if ($scope.isFolder(cmisObject)) {
			openFolder($scope.getFullPath(cmisObject.name));
		} else {
			setSelectedFile(cmisObject);
		}
	};

	$scope.writeAccessAllowed = function (document) {
		return !(document.readOnly === true);
	};

	$scope.crumbsChanged = function (entry) {
		openFolder(entry.path);
	};

	$scope.selectAllChanged = function () {
		$scope.folder.children.forEach(item => item.selected = $scope.selection.allSelected);
	};

	$scope.selectionChanged = function () {
		$scope.selection.allSelected = $scope.folder.children.every(item => item.selected);
	};

	$scope.getDeleteItemsButtonState = function () {
		return $scope.folder && $scope.folder.children.some(x => x.selected) ? undefined : 'disabled';
	};

	$scope.getFilePreviewUrl = function (item) {
		return $scope.isDocument(item) ?
			`${$scope.previewPath}?path=${$scope.getFullPath(item.name)}` : 'about:blank';
	};

	$scope.getNoDataMessage = function () {
		return $scope.search.filterBy ? 'No items match your search.' : 'This folder is empty.';
	};

	$scope.showNewFolderDialog = function () {
		messageHub.showFormDialog(
			'newFolderDialog',
			'New Folder',
			[
				{
					id: 'name',
					type: 'input',
					submitOnEnterId: "btnOK",
					label: 'Name',
					required: true,
					placeholder: 'Enter folder name...',
					value: ''
				}
			],
			[{
				id: 'btnOK',
				type: 'emphasized',
				label: 'OK',
				whenValid: true
			},
			{
				id: 'btnCancel',
				type: 'transparent',
				label: 'Cancel',
			}],
			'ide-documents.folder.create',
			'Please, wait...'
		);
	};

	messageHub.onDidReceiveMessage(
		'ide-documents.folder.create',
		function (msg) {
			if (msg.data.buttonId === 'btnOK') {
				let formData = mapFormData(msg.data.formData);
				let postData = { parentFolder: $scope.folder.path, name: formData.name };
				$http.post(folderApi, postData)
					.then(function () {
						messageHub.hideFormDialog('newFolderDialog');
						refreshFolder();
					}, function (data) {
						messageHub.updateFormDialog(
							'newFolderDialog',
							msg.data.formData,
							'Please, wait...',
							data.data.err.message
						);
					});
			} else {
				messageHub.hideFormDialog('newFolderDialog');
			}
		},
		true
	);

	$scope.showRenameItemDialog = function (item, e) {
		e.stopPropagation();

		$scope.itemToRename = {
			name: item.name
		};

		const itemType = $scope.isDocument(item) ? 'file' : 'folder';

		messageHub.showFormDialog(
			'renameItemDialog',
			`Rename ${itemType}`,
			[
				{
					id: 'name',
					type: 'input',
					submitOnEnterId: "btnOK",
					label: 'Name',
					required: true,
					placeholder: `Enter ${itemType} name...`,
					value: item.name
				}
			],
			[{
				id: 'btnOK',
				type: 'emphasized',
				label: 'OK',
				whenValid: true
			},
			{
				id: 'btnCancel',
				type: 'transparent',
				label: 'Cancel',
			}],
			'ide-documents.item.rename',
			'Please, wait...'
		);
	};

	messageHub.onDidReceiveMessage(
		'ide-documents.item.rename',
		function (msg) {
			if (msg.data.buttonId === 'btnOK' && $scope.itemToRename) {
				let formData = mapFormData(msg.data.formData);
				$http({
					url: documentsApi,
					method: 'PUT',
					data: { path: $scope.getFullPath($scope.itemToRename.name), name: formData.name }
				}).then(function () {
					$scope.itemToRename = null;
					messageHub.hideFormDialog('renameItemDialog');
					refreshFolder();
				}, function (data) {
					messageHub.updateFormDialog(
						'renameItemDialog',
						msg.data.formData,
						'Please, wait...',
						data.data.err.message
					);
				});
			} else {
				messageHub.hideFormDialog('renameItemDialog');
			}
		},
		true
	);

	$scope.showDeleteSingleItemDialog = function (item, e) {
		e.stopPropagation();

		$scope.itemsToDelete = [{
			name: item.name
		}];

		const title = $scope.isDocument(item) ? 'Delete file' : 'Delete folder';
		const message = `Are you sure you want to delete '${item.name}'`;

		messageHub.showDialog(title, message,
			[{
				id: 'btnOK',
				type: 'emphasized',
				label: 'OK',
			},
			{
				id: 'btnCancel',
				type: 'transparent',
				label: 'Cancel',
			}],
			'ide-documents.documents.delete'
		);
	};

	$scope.showDeleteItemsDialog = function (e) {
		e.stopPropagation();

		$scope.itemsToDelete = $scope.folder.children
			.filter(item => item.selected)
			.map(item => ({ name: item.name }));

		const message = $scope.itemsToDelete.length < 10 ?
			[`Are you sure you want to delete the following items?`]
				.concat($scope.itemsToDelete.map(item => item.name)) :
			`Are you sure you want to delete the selected (${$scope.itemsToDelete.length}) items?`;

		messageHub.showDialog('Delete items', message,
			[{
				id: 'btnOK',
				type: 'emphasized',
				label: 'OK',
			},
			{
				id: 'btnCancel',
				type: 'transparent',
				label: 'Cancel',
			}],
			'ide-documents.documents.delete'
		);
	};

	$scope.canPreviewFile = function (fileName) {
		let type = fileName.substring(fileName.lastIndexOf('.') + 1);
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
			case 'csv':
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

	messageHub.onDidReceiveMessage(
		'ide-documents.documents.delete',
		function (msg) {
			if (msg.data === 'btnOK' && $scope.itemsToDelete.length > 0) {
				let pathsToDelete = $scope.itemsToDelete.map(item => $scope.getFullPath(item.name));
				let url = documentsApi;

				showProgressDialog();

				$http({
					url: url,
					method: 'DELETE',
					data: pathsToDelete
				}).then(function () {
					refreshFolder();
				}, function (error) {
					hideProgressDialog();
					messageHub.showAlertError('Failed to delete items', error.data.err.message);
				});
			}
		},
		true
	);

	$scope.showUploadFileDialog = function (args) {
		$('#fileUpload').click();
		$scope.unpackZips = args && args.unpackZip;
	}

	function getFolder(folderPath) {
		let requestUrl = documentsApi;
		if (folderPath) {
			requestUrl += '?path=' + folderPath;
		}
		console.log(requestUrl)
		return $http.get(requestUrl);
	};

	function refreshFolder() {
		showProgressDialog();
		getFolder($scope.folder.path)
			.then(function (data) {
				hideProgressDialog();

				$scope.folder = data.data;

				if ($scope.selectedFile && $scope.folder.children.every(item => item.name !== $scope.selectedFile.name)) {
					setSelectedFile(null);
				}
			}, () => hideProgressDialog());
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

	function setSelectedFile(selectedFile) {
		if (selectedFile === null) $scope.selectedFile = selectedFile;
		else if ($scope.canPreviewFile(selectedFile.name)) {
			$scope.selectedFile = selectedFile;

			const iframe = getIFrame();
			if (iframe) {
				iframe.contentWindow.location.replace($scope.getFilePreviewUrl($scope.selectedFile));
			}
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
				messageHub.showAlertError('Failed to open folder', data.data.err.message);
			});
	};

	function showProgressDialog(text = '') {
		if (loading) return;

		messageHub.showBusyDialog('documentsProgressDialog', text);
		loading = true;
	};

	function hideProgressDialog() {
		if (loading) {
			messageHub.hideBusyDialog('documentsProgressDialog');
			loading = false;
		}
	};

	// FILE UPLOADER

	uploader = $scope.uploader = new FileUploader({
		url: documentsApi
	});

	uploader.headers['X-Requested-With'] = 'Fetch';

	// UPLOADER FILTERS

	uploader.filters.push({
		name: 'customFilter',
		fn: function (item /*{File|FileLikeObject}*/, options) {
			return this.queue.length < 100;
		}
	});

	// UPLOADER CALLBACKS
	uploader.onAfterAddingAll = function (addedFileItems) {
		showProgressDialog();
		uploader.uploadAll();
	};
	uploader.onBeforeUploadItem = function (item) {
		if ($scope.unpackZips && item.file.name.endsWith('.zip')) {
			item.url = zipApi + '?path=' + $scope.folder.path;
		}

		if ($scope.overwrite) {
			item.url = item.url + '&overwrite=true';
		}
	};
	uploader.onErrorItem = function (fileItem, response, status, headers) {
		hideProgressDialog();
		messageHub.showAlertError('Failed to upload item', response.err.message);
	};
	uploader.onCompleteAll = function () {
		refreshFolder();
	};

	// Upload with drag&drop
	window.addEventListener('dragenter', (event) => {
		if (![...event.dataTransfer.items].some(item => item.kind === 'file'))
			return;

		$scope.unpackZips = false;

		setupDragDrop();

		$scope.$apply(() => $scope.showDropZone = true);
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
}]);

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
