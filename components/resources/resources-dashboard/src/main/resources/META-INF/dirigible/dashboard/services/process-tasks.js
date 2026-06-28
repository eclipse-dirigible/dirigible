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
/**
 * Shared support for surfacing a record's BPM user tasks as in-context actions on generated entity
 * views. A process-aware entity carries a system-managed ProcessId (the started process-instance id,
 * written back by the intent process trigger); the current user's actionable inbox tasks are fetched
 * once and bucketed by processInstanceId, so any view - list, manage, or a master-detail pane - can
 * surface the tasks for a given record regardless of its layout.
 *
 * Usage in a generated view: depend on the 'ProcessTasks' module and drop
 * <entity-process-tasks entity="<record>"></entity-process-tasks> next to the record's actions.
 */
angular.module('ProcessTasks', ['platformLocale'])
    .factory('ProcessTasks', ['$http', 'LocaleService', function ($http, LocaleService) {
        const Dialogs = new DialogHub();
        let byProcessId = {};
        let loadPromise = null;

        const bucket = (responses) => {
            const map = {};
            const seen = new Set();
            const collect = (tasks, mine) => (tasks || []).forEach((task) => {
                if (!task.processInstanceId || seen.has(task.id)) return;
                seen.add(task.id);
                task.mine = mine;
                (map[task.processInstanceId] = map[task.processInstanceId] || []).push(task);
            });
            collect(responses[0].data, true);
            collect(responses[1].data, false);
            return map;
        };

        const load = () => {
            loadPromise = Promise.all([
                $http.get('/services/inbox/tasks?type=assignee', { params: { limit: 100 } }),
                $http.get('/services/inbox/tasks?type=groups', { params: { limit: 100 } })
            ]).then((responses) => {
                byProcessId = bucket(responses);
                return byProcessId;
            }, (error) => {
                byProcessId = {};
                console.error('ProcessTasks: unable to load inbox tasks', error);
                return byProcessId;
            });
            return loadPromise;
        };

        const openForm = (task) => {
            if (!task.formKey) {
                Dialogs.showAlert({
                    title: task.name,
                    message: LocaleService.t('dashboard.processTasks.noForm', {}, 'This task has no form to display.'),
                    type: AlertTypes.Information
                });
                return;
            }
            const separator = task.formKey.indexOf('?') >= 0 ? '&' : '?';
            const formUrl = task.formKey + separator + 'taskId=' + encodeURIComponent(task.id) + '&processInstanceId=' + encodeURIComponent(task.processInstanceId);
            // The generated task form completes the task and closes this window itself (DialogHub.closeWindow);
            // we just re-fetch when it closes so the originating view's badge drops the completed task.
            const closeTopic = 'dashboard.processTasks.window.' + task.id;
            const closeListener = Dialogs.addMessageListener({
                topic: closeTopic,
                handler: () => {
                    Dialogs.removeMessageListener(closeListener);
                    load();
                }
            });
            Dialogs.showWindow({
                hasHeader: true,
                title: task.name,
                path: formUrl,
                closeButton: true,
                callbackTopic: closeTopic
            });
        };

        return {
            /** Force a re-fetch of the current user's tasks (call after a list reload / record change). */
            refresh: () => load(),
            /** Fetch once if not already loaded; subsequent calls reuse the in-flight / cached result. */
            ensureLoaded: () => loadPromise || load(),
            /** The cached actionable tasks for a record, matched by entity.ProcessId === task.processInstanceId. */
            getTasks: (entity) => (entity && entity.ProcessId && byProcessId[entity.ProcessId]) || [],
            /** Open a task's form; a candidate (not-yet-assigned) task is claimed for the user first. */
            openTask: (task) => {
                if (task.mine) {
                    openForm(task);
                    return;
                }
                $http.post('/services/inbox/tasks/' + task.id, { action: 'CLAIM' }).then(() => {
                    task.mine = true;
                    openForm(task);
                    load();
                }, (error) => {
                    const message = error.data ? error.data.message : '';
                    Dialogs.showAlert({
                        title: task.name,
                        message: LocaleService.t('dashboard.processTasks.unableToClaim', { message: message }, `Unable to claim task: '${message}'`),
                        type: AlertTypes.Error
                    });
                    console.error('ProcessTasks: unable to claim task', error);
                });
            }
        };
    }])
    .directive('entityProcessTasks', ['ProcessTasks', 'LocaleService', function (ProcessTasks, LocaleService) {
        return {
            restrict: 'E',
            scope: { entity: '<' },
            template: `<bk-popover ng-if="tasks().length">
    <bk-popover-control>
        <bk-button compact="true" glyph="sap-icon--inbox" state="transparent" label="{{label()}}" aria-label="{{ariaLabel}}"></bk-button>
    </bk-popover-control>
    <bk-popover-body align="bottom-right">
        <bk-menu no-backdrop="true" no-shadow="true">
            <bk-menu-item ng-repeat="task in tasks() track by task.id" title="{{task.name}}" ng-click="openTask(task)"></bk-menu-item>
        </bk-menu>
    </bk-popover-body>
</bk-popover>`,
            link: (scope) => {
                scope.ariaLabel = LocaleService.t('dashboard.processTasks.pending', {}, 'Pending tasks');
                scope.tasks = () => ProcessTasks.getTasks(scope.entity);
                // Surface the current step inline: a single actionable task shows its name (answers
                // "why is there a task here?" at a glance), several collapse to a count.
                scope.label = () => {
                    const open = scope.tasks();
                    return open.length === 1 ? open[0].name : LocaleService.t('dashboard.processTasks.count', { count: open.length }, open.length + ' tasks');
                };
                scope.openTask = (task) => ProcessTasks.openTask(task);
                ProcessTasks.ensureLoaded().then(() => scope.$applyAsync());
            }
        };
    }]);
