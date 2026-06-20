/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors
 * SPDX-License-Identifier: EPL-2.0
 */
const inboxApp = angular.module('app', ['platformView', 'blimpKit', 'platformLocale', 'platformSplit']);
inboxApp.filter('trusted', ['$sce', ($sce) => (url) => url ? $sce.trustAsResourceUrl(url) : url]);
inboxApp.controller('ApplicationController', ($scope, $http, $window, $interval, LocaleService) => {
    const Notifications = new NotificationHub();
    const dateTimeUtil = new DateTimeUtil();
    const REFRESH_INTERVAL = 3000;

    $scope.tasks = [];
    $scope.selectedTask = null;
    $scope.formUrl = null;
    $scope.filterText = '';
    $scope.autoRefresh = true;
    $scope.lastUpdated = null;

    let refreshPromise = null;

    $scope.filteredTasks = () => {
        if (!$scope.filterText) return $scope.tasks;
        const needle = $scope.filterText.toLowerCase();
        return $scope.tasks.filter((task) => [task.processInstanceBusinessKey, task.processDefinitionName, task.name]
            .some((value) => value && value.toLowerCase().includes(needle)));
    };

    $scope.reload = () => $scope.fetchData();

    $scope.fetchData = () => {
        const assignedReq = $http.get('/services/inbox/tasks?type=assignee', { params: { 'limit': 100 } });
        const candidateReq = $http.get('/services/inbox/tasks?type=groups', { params: { 'limit': 100 } });

        Promise.all([assignedReq, candidateReq]).then(([assignedResp, candidateResp]) => {
            const merged = [];
            const seen = new Set();
            // Assigned tasks take precedence: they are "mine" and carry the form to fill in.
            for (const task of assignedResp.data) {
                task.mine = true;
                merged.push(task);
                seen.add(task.id);
            }
            for (const task of candidateResp.data) {
                if (seen.has(task.id)) continue;
                task.mine = false;
                merged.push(task);
                seen.add(task.id);
            }

            $scope.$evalAsync(() => {
                $scope.tasks = merged;
                $scope.lastUpdated = new Date();
                $scope.reconcileSelection();
            });
        }).catch((error) => {
            console.error(error);
        });
    };

    // Keep the current selection (and its open form) intact across silent refreshes,
    // but reset the pane once the task is gone (completed, reassigned, claimed by someone else).
    $scope.reconcileSelection = () => {
        if (!$scope.selectedTask) return;
        const current = $scope.tasks.find((task) => task.id === $scope.selectedTask.id);
        if (!current) {
            $scope.clearSelection();
            return;
        }
        const wasMine = $scope.selectedTask.mine;
        $scope.selectedTask = current;
        if (current.mine && !wasMine) $scope.loadForm(current); // claimed elsewhere - surface the form
    };

    $scope.clearSelection = () => {
        $scope.selectedTask = null;
        $scope.formUrl = null;
    };

    $scope.selectTask = (task) => {
        $scope.selectedTask = task;
        if (task.mine) $scope.loadForm(task);
        else $scope.formUrl = null;
    };

    $scope.buildFormUrl = (task) => {
        if (!task || !task.formKey) return null;
        const separator = task.formKey.indexOf('?') >= 0 ? '&' : '?';
        return task.formKey + separator + 'taskId=' + encodeURIComponent(task.id) + '&processInstanceId=' + encodeURIComponent(task.processInstanceId);
    };

    $scope.loadForm = (task) => {
        $scope.formUrl = $scope.buildFormUrl(task);
    };

    $scope.openFormInNewTab = (task) => {
        const url = $scope.buildFormUrl(task);
        if (url) $window.open(url, '_blank');
    };

    $scope.formatTime = (isoDate) => isoDate ? dateTimeUtil.format(isoDate, 'YYYY-MM-DD HH:mm:ss') : '';

    $scope.claimTask = () => {
        const task = $scope.selectedTask;
        if (!task) return;
        $scope.executeAction(task.id, { 'action': 'CLAIM' }, true, () => {
            task.mine = true;
            $scope.loadForm(task);
            $scope.fetchData();
        });
    };

    $scope.unclaimTask = () => {
        const task = $scope.selectedTask;
        if (!task) return;
        $scope.executeAction(task.id, { 'action': 'UNCLAIM' }, false, () => {
            task.mine = false;
            $scope.formUrl = null;
            $scope.fetchData();
        });
    };

    $scope.executeAction = (taskId, requestBody, claimed, successCallback) => {
        $http({
            method: 'POST',
            url: '/services/inbox/tasks/' + taskId,
            data: requestBody,
            headers: { 'Content-Type': 'application/json' }
        }).then(() => {
            Notifications.show({
                type: 'positive',
                title: LocaleService.t('inbox:actionConfirm', 'Action confirmation'),
                description: LocaleService.t(claimed ? 'inbox:actionClaimSuccess' : 'inbox:actionUnclaimSuccess', claimed ? 'Task claimed successfully' : 'Task unclaimed successfully')
            });
            successCallback();
        }).catch((error) => {
            Notifications.show({
                type: 'negative',
                title: LocaleService.t('inbox:errMsg.actionTitle', 'Action failed'),
                description: LocaleService.t(claimed ? 'inbox:errMsg.actionClaim' : 'inbox:errMsg.actionUnclaim', { name: error.message })
            });
            console.error('Error making POST request:', error);
        });
    };

    $scope.toggleAutoRefresh = () => {
        $scope.autoRefresh = !$scope.autoRefresh;
        if ($scope.autoRefresh) startAutoRefresh();
        else stopAutoRefresh();
    };

    function startAutoRefresh() {
        stopAutoRefresh();
        refreshPromise = $interval(() => $scope.fetchData(), REFRESH_INTERVAL);
    }

    function stopAutoRefresh() {
        if (refreshPromise) {
            $interval.cancel(refreshPromise);
            refreshPromise = null;
        }
    }

    $scope.$on('$destroy', stopAutoRefresh);

    $scope.fetchData();
    startAutoRefresh();
});
