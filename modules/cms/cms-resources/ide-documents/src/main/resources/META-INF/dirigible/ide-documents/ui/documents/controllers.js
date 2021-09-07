/*
 * Copyright (c) 2021 SAP SE or an SAP affiliate company and Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: 2021 SAP SE or an SAP affiliate company and Eclipse Dirigible contributors
 * SPDX-License-Identifier: EPL-2.0
 */
/*globals angular, $ */

var csrfToken = null;
var uploader = null;

angular
.module('app', ['angularFileUpload'])
.factory('httpRequestInterceptor', function () {
	return {
		request: function (config) {
			config.headers['X-Requested-With'] = 'Fetch';
			config.headers['X-CSRF-Token'] = csrfToken ? csrfToken : 'Fetch';
			return config;
		},
		response: function(response) {
			var token = response.headers()['x-csrf-token'];
			if (token) {
				csrfToken = token;
				if (uploader) {
					uploader.headers['X-CSRF-Token'] = token;
				}
			}
			return response;
		}
	};
})
.config(['$httpProvider', function($httpProvider) {
	$httpProvider.interceptors.push('httpRequestInterceptor');
}])
.controller('DocServiceCtrl', ['$scope', '$http', 'FileUploader', function($scope, $http, FileUploader) {
	const documentsApi = "/services/v4/js/ide-documents/api/documents.js";
	const folderApi = "/services/v4/js/ide-documents/api/documents.js/folder";
	const zipApi = "/services/v4/js/ide-documents/api/documents.js/zip";

	$scope.downloadPath = "/services/v4/js/ide-documents/api/documents.js/download"
	$scope.previewPath = "/services/v4/js/ide-documents/api/documents.js/preview";
	$scope.downloadZipPath = zipApi;

	$scope.breadcrumbs = new Breadcrumbs();

	function getFolder(folderPath){
		var requestUrl = documentsApi;
		if(folderPath){
			requestUrl += '?path=' + folderPath;
		}
		
		return $http.get(requestUrl);
	};
	
	function refreshFolder(){
		getFolder($scope.folder.path)
		.success(function(data){
			$scope.folder = data;
		});
	}
	
	function setUploaderFolder(folderPath){
		$scope.uploader.url = documentsApi + '?path=' + folderPath;
	}
	
	function setCurrentFolder(folderData){
		$scope.folder = folderData;
		$scope.breadcrumbs.parse(folderData.path);
		setUploaderFolder($scope.folder.path);
	};
	
	function openErrorModal(titleText, bodyText){
		$("#errorModal .modal-header #title-text").text(titleText);
     	$("#errorModal .modal-body #body-text").text(bodyText);
     	$('#errorModal').modal('show');
	};
	
	$scope.getFullPath = function(itemName){
		return $scope.folder.path + '/' + itemName;
	}
	
	getFolder()
	.success(function(data){
		setCurrentFolder(data);
	});
	
	$scope.handleExplorerClick = function(cmisObject){
		if (cmisObject.type === "cmis:folder" && !$scope.inDeleteSession){
			getFolder($scope.getFullPath(cmisObject.name))
			.success(function(data){
				setCurrentFolder(data);
			})
			.error(function(data){
				openErrorModal("Failed to open folder", data.err.message);
			});
		}
	};

	$scope.readAccessAllowed = function(document) {
		return !document.restrictedAccess || (document.restrictedAccess && (document.readOnly || document.readOnly));
	};

	$scope.writeAccessAllowed = function(document) {
		return !document.restrictedAccess || (document.restrictedAccess && document.readOnly === undefined);
	};

	$scope.crumbsChanged = function(entry){
		getFolder(entry.path)
		.success(function(data){
			setCurrentFolder(data);
		});
	};
	
	$scope.createFolder = function(newFolderName){
		var postData = { parentFolder: $scope.folder.path, name: newFolderName };
		$http.post(folderApi, postData)
		.success(function(){
			$('#newFolderModal').modal('toggle');
			refreshFolder();
		})
		.error(function(data){
			$('#newFolderModal').modal('toggle');
			openErrorModal("Failed to create folder", data.err.message);
		});
		$scope.newFolderName = undefined;

	};
	
	$scope.enterDeleteSession = function(){
		$scope.inDeleteSession = true; 
	};
	
	$scope.exitDeleteSession = function(){
		$scope.inDeleteSession = false;
	};
	
	$scope.deleteItems = function(forceDelete){
		var pathsToDelete = $scope.itemsToDelete.map(function(item){return $scope.getFullPath(item.name);});
		var url = documentsApi + (forceDelete ? "?force=true" : "");
		$http({ url: url, 
                method: 'DELETE', 
                data: pathsToDelete, 
                headers: {"Content-Type": "application/json;charset=utf-8"}
        }).success(function() {
            $scope.inDeleteSession = false;
            refreshFolder();
        }).error(function(error) {
        	$scope.inDeleteSession = false;
            openErrorModal("Failed to delete items", error.err.message);
        });
	
		$scope.inDeleteSession = false;
	};
	
	$scope.handleSingleDelete = function($event, item){
		$event.stopPropagation();
		$scope.itemsToDelete = [item];
		$scope.forceDelete = false;
		$('#confirmDeleteModal').modal('show');
	};
	
	$scope.handleDeleteButton = function(){
		var itemsToDelete = $scope.folder.children
			.filter(function(child){ return child.selected === true})
			.map(function(child){ return { name: child.name, path: child.path } });
				
		if (itemsToDelete.length > 0){
			$scope.itemsToDelete = itemsToDelete;
			$('#confirmDeleteModal').modal('show');		
		} else {
			$scope.inDeleteSession = false;
		}
	};
	
	$scope.handleRenameButton = function($event, item){
		$event.stopPropagation();
		$scope.itemToRename = item;
		$('#renameModal').modal('show');
	};
	
	$scope.renameItem = function(itemName, newName){
		$http({ url: documentsApi, 
                method: 'PUT', 
                data: { path: $scope.getFullPath(itemName), name: newName },
                headers: {"Content-Type": "application/json;charset=utf-8"}
        }).success(function() {
        	$('#renameModal').modal('toggle');
            refreshFolder();
        }).error(function(error) {
        	$('#renameModal').modal('toggle');
        	var title = "Failed to rename item" + $scope.itemToRename.name;
            openErrorModal(title, error.err.message);
        });
	}
	
	// FILE UPLOADER
	
    uploader = $scope.uploader = new FileUploader({
        url: documentsApi
    });
    
	uploader.headers['X-Requested-With'] = 'Fetch';

    // UPLOADER FILTERS

    uploader.filters.push({
        name: 'customFilter',
        fn: function(item /*{File|FileLikeObject}*/, options) {
            return this.queue.length < 100;
        }
    });

    // UPLOADER CALLBACKS

    uploader.onWhenAddingFileFailed = function(item /*{File|FileLikeObject}*/, filter, options) {
//        console.info('onWhenAddingFileFailed', item, filter, options);
    };
    uploader.onAfterAddingFile = function(fileItem) {
    	
    };
    uploader.onAfterAddingAll = function(addedFileItems) {
//        console.info('onAfterAddingAll', addedFileItems);
    };
    uploader.onBeforeUploadItem = function(item) {
//        console.info('onBeforeUploadItem', item);
		if ($scope.unpackZips && item.file.name.endsWith(".zip")) {
			item.url = zipApi + "?path=" + $scope.folder.path;
		}
		
		if ($scope.overwrite) {
			item.url = item.url + "&overwrite=true";
		}
    };
    uploader.onProgressItem = function(fileItem, progress) {
//        console.info('onProgressItem', fileItem, progress);
    };
    uploader.onProgressAll = function(progress) {
//        console.info('onProgressAll', progress);
    };
    uploader.onSuccessItem = function(fileItem, response, status, headers) {
//        console.info('onSuccessItem', fileItem, response, status, headers);
    };
    uploader.onErrorItem = function(fileItem, response, status, headers) {
//        console.info('onErrorItem', fileItem, response, status, headers);
        var title = "Failed to uplaod item";
        openErrorModal(title, response.err.message);
    };
    uploader.onCancelItem = function(fileItem, response, status, headers) {
//        console.info('onCancelItem', fileItem, response, status, headers);
    };
    uploader.onCompleteItem = function(fileItem, response, status, headers) {
    	refreshFolder();
//        console.info('onCompleteItem', fileItem, response, status, headers);
    };
    uploader.onCompleteAll = function() {
//        console.info('onCompleteAll');
    };

	function Breadcrumbs() {
		this.crumbs = [];
	};

	Breadcrumbs.prototype.parse = function(path){
		var folders = path.split("/");
		var crumbs = [];
		for (var i in folders){
			var index = +i + 1;
			var crumbPath = folders.slice(0, index).join("/");
			var crumb = { name: folders[i], path: crumbPath };
			crumbs.push(crumb);
		}
		crumbs[0].name = 'root';
		crumbs[0].path = '/';
		
		this.crumbs = crumbs;
	};
}]);
