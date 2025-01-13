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
angular.module('deployer', [])
	.controller('DeployController', function ($scope) {

		let defaultDirigibleInstances = [{
			'id': 1,
			'name': 'Local',
			'host': 'http://localhost:8080',
			'displayName': 'Local (http://localhost:8080)'
		}, {
			'id': 2,
			'name': 'Trial',
			'host': 'http://trial.dirigible.io',
			'displayName': 'Trial'
		}, {
			'id': 2,
			'name': 'Trial (Eclipse)',
			'host': 'http://dirigible.eclipse.org',
			'displayName': 'Trial (Eclipse)'
		}];

		function setDefaultDirigibleInstnaces() {
			let dirigibleInstnaces = getDirigibleInstances();
			if (dirigibleInstnaces.length === 0) {
				for (let i = 0; i < defaultDirigibleInstances.length; i++) {
					addDirigibleInstance(defaultDirigibleInstances[i]);
				}
			}
		}
		setDefaultDirigibleInstnaces();

		$scope.queryParams = getQueryParams();
		$scope.env = getEnvParams();

		function loadDirigibleInstance() {
			$scope.dirigibleInstances = getDirigibleInstances();
		}
		loadDirigibleInstance();

		function getEnvParams() {
			let env = [];
			$scope.envParamsProvided = Boolean($scope.queryParams.env);
			if ($scope.envParamsProvided) {
				let envKeys = $scope.queryParams.env.split(",");
				for (let i = 0; i < envKeys.length; i++) {
					env.push({
						key: envKeys[i]
					});
				}
			}
			return env;
		}

		function getDirigibleInstances() {
			let dirigibleInstances = JSON.parse(window.localStorage.getItem('DIRIGIBLE.instances'));
			return dirigibleInstances !== null ? dirigibleInstances : [];
		}

		function addDirigibleInstance(instance) {
			let dirigibleInstances = getDirigibleInstances();
			dirigibleInstances.push(instance);
			window.localStorage.setItem('DIRIGIBLE.instances', JSON.stringify(dirigibleInstances));
		}

		function getQueryParams() {
			let result = {};
			$scope.queryParamsProvided = false;
			if (window.location.search.length > 0) {
				let queryParams = window.location.search.slice(1).split('&');
				for (let i = 0; i < queryParams.length; i++) {
					let pair = queryParams[i].split('=');
					result[pair[0]] = pair[1];
				}
			}

			let respositoryFound = false;
			let uriFound = false;
			for (let next in result) {
				if (next === 'repository') {
					respositoryFound = true;
				} else if (next === 'uri') {
					uriFound = true;
				}
			}
			$scope.queryParamsProvided = respositoryFound && uriFound;
			return result;
		}

		$scope.addInstance = function () {
			addDirigibleInstance({
				'id': generateUUID(),
				'name': $scope.newInstanceName,
				'host': $scope.newInstanceHost.substring(0, $scope.newInstanceHost.indexOf("/", 8)),
				'displayName': $scope.newInstanceName
			});
			loadDirigibleInstance();
			toggleModal();
		};

		$scope.run = function () {
			window.location.href = getDeployUrl($scope.selectedDirigibleInstance);
		};

		function generateUUID() {
			let dt = new Date().getTime();
			let uuid = 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function (c) {
				let r = (dt + Math.random() * 16) % 16 | 0;
				dt = Math.floor(dt / 16);
				return (c === 'x' ? r : r & 0x3 | 0x8).toString(16);
			});
			return uuid;
		}

		function getDeployUrl(instance) {
			let url = instance.host + '/services/web/ide-git/index.html?repository=' + $scope.queryParams.repository;
			if ($scope.queryParams.uri) {
				url += '&uri=' + $scope.queryParams.uri;
			}
			if ($scope.queryParams.branch) {
				url += '&branch=' + $scope.queryParams.branch;
			}
			if ($scope.env && $scope.env.length > 0) {
				url += "&env=" + encodeURIComponent(JSON.stringify($scope.env));
			}
			return url;
		}

		$scope.openAddInstanceDialog = function () {
			toggleModal();
		};

		function toggleModal() {
			$('#addInstanceModal').modal('toggle');
		}
	});