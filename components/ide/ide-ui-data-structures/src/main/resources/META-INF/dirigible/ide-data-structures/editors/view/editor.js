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
angular.module('page', ["ideUI", "ideView", "ideWorkspace"])
	.controller('PageController', function ($scope, $window, workspaceApi, messageHub, ViewParameters) {
		let contents;
		$scope.errorMessage = 'An unknown error was encountered. Please see console for more information.';
		$scope.forms = {
			editor: {},
		};
		$scope.state = {
			isBusy: true,
			error: false,
			busyText: "Loading...",
		};
		$scope.nameRegex = { patterns: ['^[a-zA-Z0-9_.:"-]*$'] };
		$scope.types = [
			{ value: "TABLE", label: "Table" },
			{ value: "VIEW", label: "View" },
		];
		$scope.editDependencyIndex = 0;

		angular.element($window).bind("focus", function () {
			messageHub.setFocusedEditor($scope.dataParameters.file);
			messageHub.setStatusCaret('');
		});

		$scope.load = function () {
			if (!$scope.state.error) {
				workspaceApi.loadContent('', $scope.dataParameters.file).then(function (response) {
					if (response.status === 200) {
						$scope.view = response.data;
						contents = JSON.stringify($scope.view, null, 4);
						$scope.$apply(() => $scope.state.isBusy = false);
					} else if (response.status === 404) {
						messageHub.closeEditor($scope.dataParameters.file);
					} else {
						$scope.$apply(function () {
							$scope.state.error = true;
							$scope.errorMessage = 'There was a problem with loading the file';
							$scope.state.isBusy = false;
						});
					}
				});
			}
		};

		function saveContents(text) {
			workspaceApi.saveContent('', $scope.dataParameters.file, text).then(function (response) {
				if (response.status === 200) {
					messageHub.announceFileSaved({
						name: $scope.dataParameters.file.substring($scope.dataParameters.file.lastIndexOf('/') + 1),
						path: $scope.dataParameters.file.substring($scope.dataParameters.file.indexOf('/', 1)),
						contentType: $scope.dataParameters.contentType,
						workspace: $scope.dataParameters.file.substring(1, $scope.dataParameters.file.indexOf('/', 1)),
					});
					messageHub.setStatusMessage(`File '${$scope.dataParameters.file}' saved`);
					messageHub.setEditorDirty($scope.dataParameters.file, false);
					$scope.$apply(function () {
						$scope.state.isBusy = false;
					});
				} else {
					messageHub.setStatusError(`Error saving '${$scope.dataParameters.file}'`);
					messageHub.showAlertError('Error while saving the file', 'Please look at the console for more information');
					$scope.$apply(function () {
						$scope.state.isBusy = false;
					});
				}
			});
		}

		$scope.save = function (_keySet, event) {
			if (event) event.preventDefault();
			if ($scope.forms.editor.$valid && !$scope.state.error) {
				$scope.state.busyText = "Saving...";
				$scope.state.isBusy = true;
				contents = JSON.stringify($scope.view, null, 4);
				saveContents(contents);
			}
		};

		messageHub.onEditorFocusGain(function (msg) {
			if (msg.resourcePath === $scope.dataParameters.file) messageHub.setStatusCaret('');
		});

		messageHub.onEditorReloadParameters(
			function (event) {
				$scope.$apply(() => {
					if (event.resourcePath === $scope.dataParameters.file) {
						$scope.dataParameters = ViewParameters.get();
					}
				});
			}
		);

		messageHub.onDidReceiveMessage(
			"editor.file.save.all",
			function () {
				if (!$scope.state.error && $scope.forms.editor.$valid) {
					let view = JSON.stringify($scope.view);
					if (contents !== view) {
						$scope.save();
					}
				}
			},
			true,
		);

		messageHub.onDidReceiveMessage(
			"editor.file.save",
			function (msg) {
				if (!$scope.state.error && $scope.forms.editor.$valid) {
					let file = msg.data && typeof msg.data === 'object' && msg.data.file;
					if (file && file === $scope.dataParameters.file) {
						let view = JSON.stringify($scope.view, null, 4);
						if (contents !== view) $scope.save();
					}
				}
			},
			true,
		);

		messageHub.onDidReceiveMessage(
			"viewEditor.dependency.add",
			function (msg) {
				if (msg.data.buttonId === "b1") {
					$scope.$apply(function () {
						$scope.view.dependencies.push({
							name: msg.data.formData[1].value,
							type: msg.data.formData[0].value,
						});
					});
				}
				messageHub.hideFormDialog("viewEditorAddDependency");
			},
			true
		);

		messageHub.onDidReceiveMessage(
			"viewEditor.dependency.edit",
			function (msg) {
				if (msg.data.buttonId === "b1") {
					$scope.$apply(function () {
						$scope.view.dependencies[$scope.editDependencyIndex].name = msg.data.formData[1].value;
						$scope.view.dependencies[$scope.editDependencyIndex].type = msg.data.formData[0].value;
					});
				}
				messageHub.hideFormDialog("viewEditorEditDependency");
			},
			true
		);

		$scope.$watch('view', function () {
			if (!$scope.state.error && !$scope.state.isBusy) {
				messageHub.setEditorDirty($scope.dataParameters.file, contents !== JSON.stringify($scope.view, null, 4));
			}
		}, true);

		$scope.addDependency = function () {
			let excludedNames = [];
			for (let i = 0; i < $scope.view.dependencies.length; i++) {
				excludedNames.push($scope.view.dependencies[i].name);
			}
			messageHub.showFormDialog(
				"viewEditorAddDependency",
				"Add dependency",
				[{
					id: "vedType",
					type: "dropdown",
					label: "Type",
					required: true,
					value: $scope.types[0].value,
					items: $scope.types,
				}, {
					id: "veiName",
					type: "input",
					submitOnEnterId: "b1",
					label: "Name",
					required: true,
					placeholder: "Enter name",
					minlength: 1,
					maxlength: 255,
					inputRules: {
						excluded: excludedNames,
						patterns: $scope.nameRegex.patterns,
					},
					value: '',
				}],
				[{
					id: "b1",
					type: "emphasized",
					label: "Add",
					whenValid: true,
				},
				{
					id: "b2",
					type: "transparent",
					label: "Cancel",
				}],
				"viewEditor.dependency.add",
				"Adding dependency..."
			);
		};

		$scope.editDependency = function (index) {
			$scope.editDependencyIndex = index;
			let excludedNames = [];
			for (let i = 0; i < $scope.view.dependencies.length; i++) {
				if (i !== index)
					excludedNames.push($scope.view.dependencies[i].name);
			}
			messageHub.showFormDialog(
				"viewEditorEditDependency",
				"Edit dependency",
				[{
					id: "vedType",
					type: "dropdown",
					label: "Type",
					required: true,
					value: $scope.view.dependencies[index].type,
					items: $scope.types,
				}, {
					id: "veiName",
					type: "input",
					submitOnEnterId: "b1",
					label: "Name",
					required: true,
					placeholder: "Enter name",
					minlength: 1,
					maxlength: 255,
					inputRules: {
						excluded: excludedNames,
						patterns: $scope.nameRegex.patterns,
					},
					value: $scope.view.dependencies[index].name,
				}],
				[{
					id: "b1",
					type: "emphasized",
					label: "Update",
					whenValid: true,
				},
				{
					id: "b2",
					type: "transparent",
					label: "Cancel",
				}],
				"viewEditor.dependency.edit",
				"Updating dependency..."
			);
		};

		$scope.deleteDependency = function (index) {
			messageHub.showDialogAsync(
				`Delete ${$scope.view.dependencies[index].name}?`,
				'This action cannot be undone.',
				[{
					id: 'b1',
					type: 'negative',
					label: 'Delete',
				},
				{
					id: 'b2',
					type: 'transparent',
					label: 'Cancel',
				}],
			).then(function (dialogResponse) {
				if (dialogResponse.data === 'b1') {
					$scope.$apply(function () {
						$scope.view.dependencies.splice(index, 1);
					});
				}
			});
		};

		$scope.dataParameters = ViewParameters.get();
		if (!$scope.dataParameters.hasOwnProperty('file')) {
			$scope.state.error = true;
			$scope.errorMessage = "The 'file' data parameter is missing.";
		} else if (!$scope.dataParameters.hasOwnProperty('contentType')) {
			$scope.state.error = true;
			$scope.errorMessage = "The 'contentType' data parameter is missing.";
		} else $scope.load();

	});
