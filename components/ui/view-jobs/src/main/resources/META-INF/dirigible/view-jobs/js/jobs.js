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
const jobsView = angular.module('jobs', ['blimpKit', 'platformView']);
jobsView.constant('Dialogs', new DialogHub());
jobsView.controller('JobsController', ($scope, $http, Dialogs) => {
	$http.get('/services/jobs').then((response) => {
		$scope.list = response.data;
	});

	$scope.getIconClasses = (status) => {
		let classes = 'sap-icon ';
		switch (status) {
			case 'TRIGGRED':
				classes += 'sap-icon--play';
				break;
			case 'FINISHED':
				classes += 'sap-icon--status-positive sap-icon--color-positive';
				break;
			case 'FAILED':
				classes += 'sap-icon--status-error sap-icon--color-negative';
				break;
			case 'LOGGED':
				classes += 'sap-icon--information';
				break;
			case 'ERROR':
				classes += 'sap-icon--status-error sap-icon--color-negative';
				break;
			case 'WARN':
				classes += 'sap-icon--status-critical sap-icon--color-critical';
				break;
			case 'INFO':
				classes += 'sap-icon--information sap-icon--color-information';
				break;
			default:
				classes += 'sap-icon--question-mark';
		}
		return classes;
	};

	$scope.toggle = (index) => {
		if ($scope.list[index].enabled) {
			$http.post('/services/jobs/enable/' + $scope.list[index].name).then((response) => {
				console.info(`${response.data.name} has been enabled.`);
			}, (response) => {
				console.error(response.data);
				$scope.$evalAsync(() => {
					$scope.list[index].enabled = false;
				});
				Dialogs.showAlert({
					title: 'Job state error',
					message: 'There was an error while trying to enable the job.',
					type: AlertTypes.Error,
					preformatted: false,
				});
			});
		} else $http.post('/services/jobs/disable/' + $scope.list[index].name).then((response) => {
			console.info(`${response.data.name} has been disabled.`);
		}, (response) => {
			console.error(response.data);
			$scope.$evalAsync(() => {
				$scope.list[index].enabled = true;
			});
			Dialogs.showAlert({
				title: 'Job state error',
				message: 'There was an error while trying to disable the job.',
				type: AlertTypes.Error,
				preformatted: false,
			});
		});
	};

	$scope.showLogsWindow = (job) => {
		Dialogs.showWindow({
			hasHeader: true,
			id: 'job-logs',
			params: job
		});
	};

	$scope.showTriggerWindow = (job) => {
		Dialogs.showWindow({
			hasHeader: true,
			id: 'job-trigger',
			params: job
		});
	};

	$scope.showAssignWindow = (job) => {
		Dialogs.showWindow({
			hasHeader: true,
			id: 'job-assign',
			params: job
		});
	};
});