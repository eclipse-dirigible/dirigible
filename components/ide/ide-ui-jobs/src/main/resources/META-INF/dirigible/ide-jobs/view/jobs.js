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
const jobsView = angular.module('jobs', ['ideUI', 'ideView']);

jobsView.config(["messageHubProvider", function (messageHubProvider) {
	messageHubProvider.eventIdPrefix = 'jobs-view';
}]);

jobsView.controller('JobsController', ['$scope', '$http', 'messageHub', function ($scope, $http, messageHub) {

	$http.get('/services/jobs').then(function (response) {
		$scope.list = response.data;
	});

	$scope.enable = function (job) {
		$http.post('/services/jobs/enable/' + job.name)
			.then(function (response) {
				console.info(response.data.name + " has been enabled.");
				job.enabled = true;
			}, function (response) {
				console.error(response.data);
			});
	}

	$scope.disable = function (job) {
		$http.post('/services/jobs/disable/' + job.name)
			.then(function (response) {
				console.info(response.data.name + " has been disabled.");
				job.enabled = false;
			}, function (response) {
				console.error(response.data);
			});
	}

	$scope.showLogsWindow = function (job) {
		messageHub.showDialogWindow(
			"job-logs",
			{ job }
		);
	}

	$scope.showTriggerWindow = function (job) {
		messageHub.showDialogWindow(
			"job-trigger",
			{ job }
		);
	}

	$scope.showAssignWindow = function (job) {
		messageHub.showDialogWindow(
			"job-assign",
			{ job }
		);
	}

	$scope.getEmails = function (job) {
		$scope.job = job;
		$http.get('/services/jobs/emails/' + job.name)
			.then(function (response) {
				$scope.name = job.name;
				$scope.job.email = 'my-email@examle.com';
				$scope.emails = response.data;
			}, function (response) {
				console.error(response.data);
			});
	}

	$scope.addEmail = function () {
		$http.post('/services/jobs/emailadd/' + $scope.job.name, $scope.job.email)
			.then(function (response) {
				$scope.getEmails($scope.job);
			}, function (response) {
				console.error(response.data);
			});
	}

	$scope.removeEmail = function (id) {
		$http.delete('/services/jobs/emailremove/' + id)
			.then(function (response) {
				$scope.getEmails($scope.job);
			}, function (response) {
				console.error(response.data);
			});
	}

	$scope.checkIfValid = function () {
		console.log("Value is = " + $scope.job.email);
	}

}]);