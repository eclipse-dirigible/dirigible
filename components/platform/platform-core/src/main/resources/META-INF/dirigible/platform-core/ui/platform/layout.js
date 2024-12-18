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
if (typeof perspectiveData === 'undefined' && (!perspectiveData.id || !perspectiveData.label)) {
    console.error('Layout requires perspectiveData config');
} else angular.module('platformLayout', ['platformEditors', 'platformView', 'platformSplit'])
    .constant('layoutConstants', { version: 3.0, stateKey: 'platform.layout.state' })
    .constant('Layout', new LayoutHub(perspectiveData.id, true))
    .constant('Workspace', new WorkspaceHub())
    .constant('Dialog', new DialogHub())
    .directive('layout', (Views, Editors, SplitPaneState, layoutConstants, uuid, Layout, Workspace, Dialog, ButtonStates) => ({
        restrict: 'E',
        replace: true,
        scope: {
            config: '<',
        },
        controller: ['$scope', function ($scope) {
            const VIEW = 'view';
            const EDITOR = 'editor';

            const layoutStateKey = `${brandingInfo.keyPrefix}.${layoutConstants.stateKey}.${perspectiveData.id}`;

            $scope.perspectiveName = '';
            $scope.perspectiveStyle = {
                '-webkit-box-sizing': 'border-box',
                'box-sizing': 'border-box',
                width: '100%',
                height: '100%',
            };
            $scope.views = [];
            $scope.leftTabs = [];
            $scope.bottomTabs = [];
            $scope.rightTabs = [];
            $scope.centerSplittedTabViews = {
                direction: 'horizontal',
                panes: [
                    {
                        tabs: [],
                        selectedTab: null
                    }
                ]
            };

            $scope.layoutSettings = {
                leftPaneSize: 20,
                leftPaneMinSize: 0,
                leftPaneMaxSize: undefined,
                rightPaneSize: 20,
                rightPaneMinSize: 0,
                rightPaneMaxSize: undefined,
                bottomPaneSize: 30,
                hideCenterPane: false,
                hideCenterTabs: false,
                hideBottomTabs: false,
                ...($scope.config.layoutSettings || {})
            };
            $scope.selection = {
                selectedBottomTab: null
            };
            $scope.splitPanesState = {
                main: [
                    $scope.layoutSettings.bottomPaneSize === 100 ? SplitPaneState.COLLAPSED : SplitPaneState.EXPANDED,
                    $scope.layoutSettings.bottomPaneSize === 0 ? SplitPaneState.COLLAPSED : SplitPaneState.EXPANDED
                ]
            };

            $scope.initialOpenViews = $scope.config.views;
            $scope.focusedTabView = null;

            let closingFileArgs;
            let reloadingFileArgs;
            let viewSettings = $scope.config.viewSettings || {};

            $scope.perspectiveName = perspectiveData.label;
            Views.getViews().then(function (views) {
                $scope.views = views;

                for (let v = 0; v < $scope.views.length; v++) {
                    if ($scope.views[v].params) {
                        $scope.views[v].params['container'] = 'layout';
                        $scope.views[v].params['perspectiveId'] = perspectiveData.id;
                    } else $scope.views[v].params = {
                        container: 'layout',
                        perspectiveId: perspectiveData.id,
                    };
                }

                const viewExists = (v) => views.some(x => x.id === v.id);
                const viewById = (ret, viewId) => {
                    const v = $scope.views.find(v => v.id === viewId);
                    if (v) ret.push(v);
                    return ret;
                };
                const mapViewToTabById = (v) => {
                    if (v.type !== VIEW) return v;
                    let view = views.find(x => x.id === v.id);
                    return view ? { ...v, ...mapViewToTab(view) } : v;
                }
                const byLeftRegion = view => !view.region || view.region === 'left';
                const byRightRegion = view => view.region === 'right';
                const byBottomRegion = view => view.region === 'bottom';
                const byCenterRegion = view => view.region === 'center';

                const savedState = loadLayoutState();
                if (savedState && !hasMajorVersionChanged(savedState)) {
                    const restoreCenterSplittedTabViews = function (state, removedViewsIds) {
                        if (state.panes) {
                            state.panes.forEach(pane => restoreCenterSplittedTabViews(pane, removedViewsIds));
                        } else {
                            state.tabs = state.tabs
                                .filter(v => v.type === EDITOR || (viewExists(v) && (!removedViewsIds || !removedViewsIds.includes(v.id))))
                                .map(mapViewToTabById);

                            if (!state.tabs.some(x => x.id === state.selectedTab)) {
                                state.selectedTab = null;
                            }
                        }

                        return state;
                    }

                    $scope.leftTabs = savedState.left.tabs.filter(viewExists).map(mapViewToTabById);
                    $scope.bottomTabs = savedState.bottom.tabs.filter(viewExists).map(mapViewToTabById);
                    $scope.rightTabs = (savedState.right || { tabs: [] }).tabs.filter(viewExists).map(mapViewToTabById);

                    let newlyAddedViews, removedViewsIds;
                    let initialOpenViewsChanged = !angular.equals(savedState.initialOpenViews, $scope.initialOpenViews);
                    if (initialOpenViewsChanged) {
                        newlyAddedViews = $scope.initialOpenViews.filter(x => savedState.initialOpenViews.every(y => x !== y)).reduce(viewById, []);
                        removedViewsIds = savedState.initialOpenViews.filter(x => $scope.initialOpenViews.every(y => x !== y));

                        $scope.leftTabs = $scope.leftTabs
                            .filter(x => !removedViewsIds.includes(x.id))
                            .concat(newlyAddedViews.filter(byLeftRegion).map(mapViewToTab));

                        $scope.rightTabs = $scope.rightTabs
                            .filter(x => !removedViewsIds.includes(x.id))
                            .concat(newlyAddedViews.filter(byRightRegion).map(mapViewToTab));

                        $scope.bottomTabs = $scope.bottomTabs
                            .filter(x => !removedViewsIds.includes(x.id))
                            .concat(newlyAddedViews.filter(byBottomRegion).map(mapViewToTab));
                    }

                    $scope.centerSplittedTabViews = restoreCenterSplittedTabViews(savedState.center, removedViewsIds);

                    if (newlyAddedViews) {
                        $scope.centerSplittedTabViews.panes[0].tabs.push(...newlyAddedViews.filter(byCenterRegion).map(mapViewToTab));
                    }

                    forEachCenterSplittedTabView(pane => {
                        pane.tabs = pane.tabs.map(applyCenterViewSettings);
                    });

                    $scope.leftTabs = $scope.leftTabs.map(applySideViewSettings);
                    $scope.rightTabs = $scope.rightTabs.map(applySideViewSettings);

                    if ($scope.bottomTabs.some(x => x.id === savedState.bottom.selected))
                        $scope.selection.selectedBottomTab = savedState.bottom.selected;

                    if (initialOpenViewsChanged) {
                        saveLayoutState();
                    }

                    shortenCenterTabsLabels();

                } else {
                    let openViews = $scope.initialOpenViews.reduce(viewById, []);

                    $scope.leftTabs = openViews
                        .filter(byLeftRegion)
                        .map(mapViewToTab)
                        .map(applySideViewSettings);

                    $scope.rightTabs = openViews
                        .filter(byRightRegion)
                        .map(mapViewToTab)
                        .map(applySideViewSettings);

                    $scope.bottomTabs = openViews
                        .filter(byBottomRegion)
                        .map(mapViewToTab);

                    $scope.centerSplittedTabViews.panes[0].tabs = openViews
                        .filter(byCenterRegion)
                        .map(mapViewToTab)
                        .map(applyCenterViewSettings);
                }

                $scope.focusedTabView = getFirstCenterSplittedTabViewPane($scope.centerSplittedTabViews);

                $scope.$watch('selection', (newSelection, oldSelection) => {
                    if (!angular.equals(newSelection, oldSelection)) {
                        saveLayoutState();
                    }
                }, true);

                $scope.$watch('centerSplittedTabViews', (newValue, oldValue) => {
                    if (!angular.equals(newValue, oldValue)) {
                        saveLayoutState();
                    }
                }, true);

                $scope.$watch('leftTabs', (newValue, oldValue) => {
                    const collectionsEqual = (a, b, ...props) => {
                        if (a && !b || !a && b) return false;
                        if (!a && !b) return true;
                        if (a.length !== b.length) return false;
                        for (let i = 0; i < a.length; i++) {
                            const x = a[i];
                            const y = b[i];
                            for (let p of props) {
                                if (!angular.equals(x[p], y[p]))
                                    return false;
                            }
                        }
                        return true;
                    }

                    if (!collectionsEqual(newValue, oldValue, 'expanded')) {
                        saveLayoutState();
                    }
                }, true);
            });

            $scope.closeCenterTab = (tab) => tryCloseCenterTabs([tab]);

            $scope.splitCenterTabs = (direction, pane) => splitTabs(direction, pane);

            $scope.collapseBottomPane = () => {
                updateSplitPanesState({
                    editorsPaneState: SplitPaneState.EXPANDED,
                    bottomPanesState: SplitPaneState.COLLAPSED,
                });
            };

            $scope.expandBottomPane = () => {
                updateSplitPanesState({
                    editorsPaneState: SplitPaneState.EXPANDED,
                    bottomPanesState: SplitPaneState.EXPANDED,
                });
            };

            $scope.toggleCenterPane = () => {
                const editorsPaneCollapsed = $scope.isCenterPaneCollapsed();

                updateSplitPanesState({
                    editorsPaneState: editorsPaneCollapsed ? SplitPaneState.EXPANDED : SplitPaneState.COLLAPSED,
                    bottomPanesState: SplitPaneState.EXPANDED
                });
            };

            $scope.isCenterPaneCollapsed = () => $scope.splitPanesState.main[0] == SplitPaneState.COLLAPSED;

            $scope.isBottomPaneCollapsed = () => {
                return $scope.splitPanesState.main.length < 2 || $scope.splitPanesState.main[1] == SplitPaneState.COLLAPSED;
            };

            $scope.isMoreTabsButtonVisible = (tabs) => tabs.some(x => x.isHidden);

            $scope.sideViewStateChanged = () => saveLayoutState();

            function hasMajorVersionChanged(savedState) {
                const newVersion = parseInt(layoutConstants.version);
                const oldVersion = parseInt(savedState.version);
                return newVersion > oldVersion;
            }

            function loadLayoutState() {
                let savedState = localStorage.getItem(layoutStateKey);

                let state = null;

                if (savedState !== null) {
                    try {
                        state = JSON.parse(savedState);

                        if (state.version === undefined)
                            state.version = 1.0;

                    } catch (ex) {
                        console.error(`Failed to parse layout state: ${ex.message}, state: ${savedState}`);
                        return null;
                    }
                }

                return state;
            }

            function saveLayoutState() {

                const saveCenterSplittedTabViews = function (parent) {
                    let ret;
                    if (parent.panes) {
                        ret = {
                            direction: parent.direction,
                            panes: []
                        };
                        for (let i = 0; i < parent.panes.length; i++) {
                            const pane = parent.panes[i];
                            ret.panes.push(saveCenterSplittedTabViews(pane));
                        }
                    } else {
                        ret = {
                            tabs: parent.tabs.map(({ id, type, label, path, lazyLoad, params }) => {
                                return type === VIEW ?
                                    { id, type } :
                                    { id, type, label, path, lazyLoad, params };
                            }),
                            selectedTab: parent.selectedTab
                        };
                    }
                    return ret;
                }

                const state = {
                    version: layoutConstants.version,
                    initialOpenViews: $scope.initialOpenViews,
                    left: {
                        tabs: $scope.leftTabs.map(({ id, type, expanded }) => ({ id, type, expanded }))
                    },
                    right: {
                        tabs: $scope.rightTabs.map(({ id, type, expanded }) => ({ id, type, expanded }))
                    },
                    bottom: {
                        tabs: $scope.bottomTabs.map(({ id, type }) => ({ id, type })),
                        selected: $scope.selection.selectedBottomTab
                    },
                    center: saveCenterSplittedTabViews($scope.centerSplittedTabViews)
                };

                localStorage.setItem(layoutStateKey, JSON.stringify(state));
            }

            function updateSplitPanesState(args) {
                if ($scope.splitPanesState.main.length > 1) {
                    $scope.splitPanesState.main[0] = args.editorsPaneState;
                    $scope.splitPanesState.main[1] = args.bottomPanesState;
                }
            }

            function findView(views, view) {
                return views.find(v => v.id === view.id);
            }

            function mapViewToTab(view) {
                return {
                    id: view.id,
                    type: VIEW,
                    label: view.label,
                    path: view.path,
                    lazyLoad: view.lazyLoad,
                    params: view.params,
                };
            }

            function applySideViewSettings(view, index) {
                const settings = viewSettings[view.id];
                if (settings || index === 0) {
                    return {
                        expanded: settings && settings.expanded !== undefined ? settings.expanded : index === 0,  // the first view is expanded by default unless explicitly specified
                        ...view
                    }
                }
                return view;
            }

            function applyCenterViewSettings(view) {
                if (view.type !== VIEW)
                    return view;

                const settings = viewSettings[view.id];
                if (settings) {
                    return {
                        closable: settings.closable === undefined ? true : settings.closable,
                        ...view
                    }
                }
                view.closable = true;
                return view;
            }

            function findCenterSplittedTabViewById(id, pane = null, parent = null, indexInParent = -1) {

                let currentPane = pane || $scope.centerSplittedTabViews;

                if (currentPane.tabs) {
                    const index = currentPane.tabs.findIndex(f => f.id === id);
                    if (index >= 0)
                        return { tabsView: currentPane, parent, index };

                } else if (currentPane.panes) {
                    for (let i = 0; i < currentPane.panes.length; i++) {
                        let childPane = currentPane.panes[i];
                        let result = findCenterSplittedTabViewById(id, childPane, { parent, indexInParent, ...currentPane }, i);
                        if (result)
                            return result;
                    }
                }

                return null;
            }

            function findCenterSplittedTabViewByPath(resourcePath, pane = null, parent = null, indexInParent = -1) {

                let currentPane = pane || $scope.centerSplittedTabViews;

                if (currentPane.tabs) {
                    const index = currentPane.tabs.findIndex(f => f.params.filePath === resourcePath);
                    if (index >= 0)
                        return { tabsView: currentPane, parent, index };

                } else if (currentPane.panes) {
                    for (let i = 0; i < currentPane.panes.length; i++) {
                        let childPane = currentPane.panes[i];
                        let result = findCenterSplittedTabViewByPath(resourcePath, childPane, { parent, indexInParent, ...currentPane }, i);
                        if (result)
                            return result;
                    }
                }

                return null;
            }

            /**
             * Returnes a list of all files whose path starts with 'basePath', from the currently opened editors.
             * If basePath is not specified, all files will be listed.
             */
            function getCurrentlyOpenedFiles(basePath = '/') {
                let fileList = [];
                for (let childIndex = 0; childIndex < $scope.centerSplittedTabViews.panes.length; childIndex++) {
                    let childPane = $scope.centerSplittedTabViews.panes[childIndex];
                    for (let tabIndex = 0; tabIndex < childPane.tabs.length; tabIndex++) {
                        if (childPane.tabs[tabIndex].type === EDITOR && childPane.tabs[tabIndex].params.filePath.startsWith(basePath)) {
                            fileList.push(childPane.tabs[tabIndex].params.filePath);
                        }
                    }
                }
                return fileList;
            }

            function getFirstCenterSplittedTabViewPane(parent) {
                let pane = parent;
                while (pane.panes) {
                    pane = pane.panes[0];
                }
                return pane;
            }

            function getCurrentCenterSplittedTabViewPane() {
                return $scope.focusedTabView || getFirstCenterSplittedTabViewPane($scope.centerSplittedTabViews);
            }

            function forEachCenterSplittedTabView(callback, parent = undefined) {
                let parentNode = parent || $scope.centerSplittedTabViews;

                if (parentNode.tabs) {
                    callback(parentNode);
                } else if (parentNode.panes) {
                    for (let pane of parentNode.panes) {
                        forEachCenterSplittedTabView(callback, pane);
                    }
                }
            }

            function splitTabs(direction, pane) {
                const { selectedTab } = pane;
                const result = findCenterSplittedTabViewById(selectedTab);
                if (result) {
                    const splitView = result.parent;
                    const srcTabsView = result.tabsView;
                    const tabIndex = result.index;
                    const srcTabs = srcTabsView.tabs;

                    if (srcTabs.length === 1)
                        return;

                    const tab = srcTabs[tabIndex];

                    srcTabs.splice(tabIndex, 1);

                    const destTabsView = {
                        tabs: [tab],
                        selectedTab
                    };

                    const index = splitView.panes.indexOf(srcTabsView);

                    if (splitView.panes.length === 1 || splitView.direction === direction) {
                        splitView.direction = direction;
                        splitView.panes.splice(index + 1, 0, destTabsView);
                    } else {
                        const selectedTabIndex = tabIndex < srcTabs.length ? tabIndex : srcTabs.length - 1;
                        srcTabsView.selectedTab = srcTabs[selectedTabIndex].id;

                        splitView.panes[index] = {
                            direction,
                            panes: [
                                srcTabsView,
                                destTabsView
                            ]
                        }
                    }
                }
            }

            function removeSplitPane(splitView, splitPane) {
                if (splitView.panes.length > 1) {
                    const index = splitView.panes.indexOf(splitPane);
                    splitView.panes.splice(index, 1);

                    let focusedSplitView;
                    let focusedPaneIndex;

                    if (splitView.parent && splitView.panes.length === 1) {
                        //if this is the last split pane then remove the wrapping split view
                        const pane = splitView.panes[0];
                        if (pane.panes && pane.direction === splitView.parent.direction) {
                            //if the last split pane contains another split view and it's direction is the same as the parent split view
                            //then concatenate both split views
                            splitView.parent.panes.splice(splitView.indexInParent, 1, ...pane.panes);
                        } else {
                            //... otherwise just replace the wrapping split view with the tabs view 
                            splitView.parent.panes[splitView.indexInParent] = pane;
                        }

                        focusedSplitView = splitView.parent;
                        focusedPaneIndex = splitView.indexInParent;
                    } else {
                        focusedSplitView = splitView;
                        focusedPaneIndex = index < splitView.panes.length ? index : splitView.panes.length - 1;
                    }

                    $scope.focusedTabView = getFirstCenterSplittedTabViewPane(focusedSplitView.panes[focusedPaneIndex]);
                }
            }

            // function moveTab(tabId) {
            //     const result = findCenterSplittedTabViewById(tabId);
            //     if (result) {
            //         const splitView = result.parent;
            //         const srcTabsView = result.tabsView;

            //         if (srcTabsView.tabs.length === 1 && splitView.panes.length === 1)
            //             return;

            //         const tab = srcTabsView.tabs[result.index];

            //         srcTabsView.tabs.splice(result.index, 1);

            //         let destTabsView;
            //         if (splitView.panes.length === 1) {
            //             destTabsView = {
            //                 tabs: [tab],
            //                 selectedTab: tabId
            //             }
            //             splitView.panes.push(destTabsView);
            //         } else {
            //             const srcIndex = splitView.panes.indexOf(srcTabsView);
            //             destTabsView = splitView.panes[srcIndex === 0 ? 1 : 0];
            //             destTabsView.selectedTab = tabId;
            //             destTabsView.tabs.push(tab);

            //             if (srcTabsView.tabs.length === 0) {
            //                 splitView.panes.splice(srcIndex, 1);
            //             }
            //         }
            //     }
            // }

            function showFileSaveDialog(fileName, filePath, args = {}) {
                return new Promise((resolve, reject) => {
                    Dialog.showAlert({
                        title: 'You have unsaved changes',
                        message: `Do you want to save the changes you made to ${fileName}?`,
                        type: AlertTypes.Warning,
                        preformatted: false,
                        buttons: [
                            { id: JSON.stringify({ id: 'save', filePath: filePath, params: args }), label: 'Save', state: ButtonStates.Emphasized },
                            { id: JSON.stringify({ id: 'ignore', filePath: filePath, params: args }), label: 'Don\'t Save' },
                            { id: JSON.stringify({ id: 'cancel' }), label: 'Cancel', state: ButtonStates.Transparent },
                        ]
                    }).then((buttonData) => {
                        const data = JSON.parse(buttonData);
                        if (data.id === 'save') {
                            Workspace.saveFile({
                                path: data.filePath,
                                params: data.params
                            });
                        } else if (data.id === 'ignore') {
                            $scope.setEditorDirty(data.filePath, false);
                        }
                        resolve(data);
                    }, (error) => {
                        reject(error);
                    });
                });
            }

            function tryReloadCenterTab(tab, editorPath, params) {
                if (tab.dirty) {
                    showFileSaveDialog(tab.label, tab.params.filePath, { editorPath, params })
                        .then(args => {
                            switch (args.id) {
                                case 'save':
                                    reloadingFileArgs = args;
                                    break;
                                case 'ignore':
                                    $scope.$evalAsync(() => {
                                        reloadCenterTab(args.params.editorPath, args.params.params);
                                    });
                                    break;
                            }
                        });
                } else {
                    reloadCenterTab(editorPath, params);
                }
            }

            function tryCloseCenterTabs(tabs) {
                let dirtyFiles = tabs.filter(tab => tab.dirty);
                if (dirtyFiles.length > 0) {

                    let tab = dirtyFiles[0];
                    let result = findCenterSplittedTabViewById(tab.id);
                    if (result) {
                        result.tabsView.selectedTab = tab.id;
                    }

                    showFileSaveDialog(tab.label, tab.params.filePath, { tabs })
                        .then(args => {
                            switch (args.id) {
                                case 'save':
                                    closingFileArgs = args;
                                    break;
                                case 'ignore':
                                    closeCenterTab(args.filePath)

                                    let rest = args.params.tabs.filter(x => x.params.filePath !== args.filePath);
                                    if (rest.length > 0)
                                        if (tryCloseCenterTabs(rest)) {
                                            $scope.$evalAsync();
                                        }

                                    break;
                            }
                        });
                } else {
                    for (let i = 0; i < tabs.length; i++) {
                        removeCenterTab(tabs[i].id);
                    }

                    return true;
                }

                return false;
            }

            window.onbeforeunload = () => {
                for (let childIndex = 0; childIndex < $scope.centerSplittedTabViews.panes.length; childIndex++) {
                    let childPane = $scope.centerSplittedTabViews.panes[childIndex];
                    for (let tabIndex = 0; tabIndex < childPane.tabs.length; tabIndex++) {
                        if (childPane.tabs[tabIndex].type === EDITOR && childPane.tabs[tabIndex].dirty) {
                            return 'You have unsaved files. Do you still want to leave?';
                        }
                    }
                }
                return;
            };

            function removeCenterTab(id, filePath) {
                let result;
                if (id) result = findCenterSplittedTabViewById(id);
                else result = findCenterSplittedTabViewByPath(filePath);
                if (result) {
                    const { tabsView, parent: splitView } = result;
                    tabsView.tabs.splice(result.index, 1);
                    if (tabsView.tabs.length === 0) {
                        //that's the last tab in the tabs view -> remove the wrapping split pane
                        removeSplitPane(splitView, tabsView);
                    }

                    shortenCenterTabsLabels();
                    return true;
                }

                return false;
            }

            function closeCenterTab(filePath) {
                if (removeCenterTab(undefined, filePath)) {
                    $scope.$evalAsync();
                }
            }

            function reloadCenterTab(editorPath, params) {
                let result = findCenterSplittedTabViewByPath(params.filePath);
                if (result) {
                    const tab = result.tabsView.tabs[result.index];
                    tab.path = editorPath;
                    tab.params = params;
                }
            }

            const viewSetDirtyListener = Layout.onSetEditorDirty((data) => {
                $scope.$evalAsync($scope.setEditorDirty(data.path, data.dirty));
            });

            const editorCloseListener = Layout.onCloseEditor((data) => {
                $scope.$evalAsync(() => {
                    if (data.params && data.params.closeOthers) $scope.closeOtherEditors(data.path);
                    else $scope.closeEditor(data.path);
                });
            });

            const editorCloseAllListener = Layout.onCloseAllEditors(() => {
                $scope.$apply($scope.closeAllEditors());
            });

            const viewOpenListener = Layout.onOpenView((data) => {
                $scope.$apply($scope.openView(data.id, data.params));
            });

            const onOpenEditorListener = Layout.onOpenEditor((data) => {
                $scope.$apply(() => {
                    $scope.openEditor(
                        data.path,
                        data.name,
                        data.contentType,
                        data.editorId,
                        data.params
                    );
                });
            });

            const onFileSavedListener = Workspace.onFileSaved((fileDescriptor) => {
                if (closingFileArgs) {
                    if (fileDescriptor.path === closingFileArgs.filePath) {
                        closeCenterTab(fileDescriptor.path);

                        let rest = closingFileArgs.tabs.filter(x => x.params.filePath !== closingFileArgs.filePath);
                        if (rest.length > 0) {
                            if (tryCloseCenterTabs(rest)) {
                                $scope.$evalAsync();
                            }
                        }

                        closingFileArgs = null;
                    }
                }

                if (reloadingFileArgs) {
                    const fileName = msg.data;
                    const { filePath, editorPath, params } = reloadingFileArgs;

                    if (fileName === filePath) {
                        $scope.$apply(() => {
                            reloadCenterTab(editorPath, params);
                            reloadingFileArgs = null;
                        });
                    }
                }
            });

            const onFileMovedListener = Workspace.onFileMoved((fileDescriptor) => {
                updateEditor(
                    fileDescriptor.oldPath,
                    fileDescriptor.newPath,
                    fileDescriptor.newName,
                );
            });

            const onFileRenamedListener = Workspace.onFileRenamed((fileDescriptor) => {
                updateEditor(
                    fileDescriptor.oldPath,
                    fileDescriptor.newPath,
                    fileDescriptor.newName,
                );
            });

            const onFileDeletedListener = Workspace.onFileDeleted((fileDescriptor) => {
                $scope.$apply($scope.closeEditor(fileDescriptor.path));
            });

            const onGetOpenedEditorsListener = Layout.onGetCurrentlyOpenedEditors((data) => {
                Layout.postMessage({ topic: data.topic, data: getCurrentlyOpenedFiles(data.basePath) });
            });

            const onIsViewOpenListener = Layout.onIsViewOpen((data) => {
                const result = findCenterSplittedTabViewById(data.id);
                if (result) {
                    Layout.postMessage({
                        topic: data.topic, data: {
                            isOpen: true,
                            isDirty: result.tabsView.tabs[result.index].dirty || false,
                        }
                    });
                } else Layout.postMessage({ topic: data.topic, data: { isOpen: false } });
            });

            const onIsEditorOpenListener = Layout.onIsEditorOpen((data) => {
                const result = findCenterSplittedTabViewByPath(data.path);
                if (result) {
                    Layout.postMessage({
                        topic: data.topic, data: {
                            isOpen: true,
                            isDirty: result.tabsView.tabs[result.index].dirty || false,
                        }
                    });
                } else Layout.postMessage({ topic: data.topic, data: { isOpen: false } });
            });

            const onFocusTab = (data) => {
                if ($scope.focusedTabView.selectedTab !== data.id) {
                    const result = findCenterSplittedTabViewById(data.id);
                    if (result) $scope.$apply(() => {
                        $scope.focusedTabView = result.tabsView;
                    });
                }
            };

            const onFocusViewListener = Layout.onFocusView(onFocusTab);
            const onFocusEditorListener = Layout.onFocusEditor(onFocusTab);

            function shortenCenterTabsLabels() {

                const getTabPath = tab => {
                    const index = tab.params.filePath.lastIndexOf('/');
                    return tab.params.filePath.substring(0, index > 0 ? index : tab.params.filePath.length);
                }

                const allTabs = [];
                forEachCenterSplittedTabView(pane => allTabs.push(...pane.tabs.filter(x => x.type === EDITOR)), $scope.centerSplittedTabViews);

                const duplicatedTabs = allTabs.reduce((ret, tab) => {
                    let duplicates = ret.get(tab.label);
                    if (duplicates === undefined) {
                        duplicates = [];
                        ret.set(tab.label, duplicates);
                    }

                    duplicates.push(tab);

                    return ret;
                }, new Map());

                duplicatedTabs.forEach(tabs => {
                    if (tabs.length == 1) {
                        // no duplication so just reset the hint
                        tabs[0].hint = '';
                        return;
                    }

                    const paths = tabs.map(getTabPath);
                    const shortenedPaths = shortenPaths(paths);

                    tabs.forEach((tab, index) => tab.hint = shortenedPaths[index]);
                });
            }

            function shortenPaths(paths) {
                const shortenedPaths = [];
                const pathSeparator = '/';
                const ellipsis = '…';

                let match;
                for (let pathIndex = 0; pathIndex < paths.length; pathIndex++) {
                    let path = paths[pathIndex];

                    if (path.indexOf(pathSeparator) === 0) {
                        prefix = path.substring(0, path.indexOf(pathSeparator) + pathSeparator.length);
                        path = path.substring(path.indexOf(pathSeparator) + pathSeparator.length);
                    }

                    match = true;

                    const segments = path.split(pathSeparator);
                    for (let subpathLength = 1; match && subpathLength <= segments.length; subpathLength++) {
                        for (let start = segments.length - subpathLength; match && start >= 0; start--) {
                            match = false;

                            let subpath = segments.slice(start, start + subpathLength).join(pathSeparator);

                            for (let otherPathIndex = 0; !match && otherPathIndex < paths.length; otherPathIndex++) {

                                if (otherPathIndex !== pathIndex && paths[otherPathIndex] && paths[otherPathIndex].indexOf(subpath) > -1) {
                                    const isSubpathEnding = (start + subpathLength === segments.length);
                                    const subpathWithSep = (start > 0 && paths[otherPathIndex].indexOf(pathSeparator) > -1) ? pathSeparator + subpath : subpath;
                                    const isOtherPathEnding = paths[otherPathIndex].endsWith(subpathWithSep);

                                    match = !isSubpathEnding || isOtherPathEnding;
                                }
                            }

                            if (!match) {
                                let result = '';

                                if (start > 0) {
                                    result += ellipsis + pathSeparator;
                                }

                                result += subpath;

                                if (start + subpathLength < segments.length) {
                                    result += pathSeparator + ellipsis;
                                }

                                shortenedPaths[pathIndex] = result;
                            }
                        }
                    }

                    if (match) {
                        shortenedPaths[pathIndex] = path;
                    }
                }

                return shortenedPaths;
            }

            $scope.openEditor = (resourcePath, resourceLabel, contentType, editorId, extraArgs = null) => {
                if (resourcePath) {
                    let editorPath = Editors.editorProviders[editorId];
                    let eId = editorId;
                    if (!editorPath) {
                        let editors = Editors.editorsForContentType[contentType];
                        if (editors && editors.length > 0) {
                            for (let i = 0; i < editors.length; i++) {
                                if (editors[i].id !== Editors.defaultEditor.id) {
                                    eId = editors[i].id;
                                    break;
                                }
                            }
                            if (!eId) eId = Editors.defaultEditor.id;
                        } else {
                            eId = Editors.defaultEditor.id;
                        }
                        editorPath = Editors.editorProviders[eId];
                    }

                    let params = Object.assign({
                        filePath: resourcePath,
                        contentType: contentType
                    }, extraArgs || {});

                    let result = findCenterSplittedTabViewByPath(resourcePath);
                    let currentTabsView = result ? result.tabsView : getCurrentCenterSplittedTabViewPane();
                    if (result) {
                        let fileTab = currentTabsView.tabs[result.index];
                        currentTabsView.selectedTab = fileTab.id;
                        $scope.focusedTabView = result.tabsView;
                        if (fileTab.path !== editorPath) {
                            tryReloadCenterTab(fileTab, editorPath, params);
                        }
                    } else {
                        let fileTab = {
                            id: `ET${uuid.generate()}`,
                            type: EDITOR,
                            label: resourceLabel,
                            path: editorPath,
                            params: params
                        };

                        currentTabsView.selectedTab = fileTab.id;
                        currentTabsView.tabs.push(fileTab);
                    }

                    shortenCenterTabsLabels();
                } else {
                    console.error('openEditor: resourcePath is undefined');
                }
            };
            function updateEditor(oldPath, newPath, resourceLabel) {
                if (oldPath === undefined && oldPath === null && oldPath.trim() === '')
                    console.error('updateEditor: oldPath is undefined');
                else if (newPath === undefined && newPath === null && newPath.trim() === '')
                    console.error('updateEditor: newPath is undefined');
                else if (resourceLabel === undefined && resourceLabel === null && resourceLabel.trim() === '')
                    console.error('updateEditor: resourceLabel is undefined');
                else {
                    const result = findCenterSplittedTabViewByPath(oldPath);
                    if (result) {
                        $scope.$apply(() => {
                            result.tabsView.tabs[result.index].label = resourceLabel;
                            result.tabsView.tabs[result.index].params.filePath = newPath;
                            Layout.reloadEditorParams({ path: oldPath });
                            shortenCenterTabsLabels();
                        })
                    }
                }
            };
            $scope.closeEditor = (resourcePath) => {
                let result = findCenterSplittedTabViewByPath(resourcePath);
                if (result) {
                    let tab = result.tabsView.tabs[result.index];
                    if (tryCloseCenterTabs([tab])) {
                        $scope.$evalAsync();
                    }
                }
            };
            $scope.closeOtherEditors = (resourcePath) => {
                let result = findCenterSplittedTabViewByPath(resourcePath);
                if (result) {
                    let rest = result.tabsView.tabs.filter(x => x.type === EDITOR && x.params.filePath !== resourcePath);
                    if (rest.length > 0) {
                        if (tryCloseCenterTabs(rest)) {
                            $scope.$evalAsync();
                        }
                    }
                }
            };
            $scope.closeAllEditors = () => {
                forEachCenterSplittedTabView(pane => {
                    if (tryCloseCenterTabs(pane.tabs.filter(x => x.type === EDITOR))) {
                        $scope.$evalAsync();
                    }
                }, $scope.centerSplittedTabViews);
            };
            $scope.setEditorDirty = (resourcePath, dirty) => {
                let result = findCenterSplittedTabViewByPath(resourcePath);
                if (result) {
                    $scope.$apply(() => {
                        const fileTab = result.tabsView.tabs[result.index];
                        fileTab.dirty = dirty;
                    });
                }
            };
            $scope.openView = (viewId, params = {}) => {
                if (params !== undefined && !(typeof params === 'object' && !Array.isArray(params) && params !== null))
                    throw Error("openView: params must be an object");
                let view = $scope.views.find(v => v.id === viewId);
                if (view) {
                    view.params = params;
                    if (view.region === 'left') {
                        let leftViewTab = findView($scope.leftTabs, view);
                        if (leftViewTab) {
                            leftViewTab.expanded = true;
                        } else {
                            leftViewTab = mapViewToTab(view);
                            leftViewTab.expanded = true;
                            $scope.leftTabs.push(leftViewTab);
                        }

                    } else if (view.region === 'right') {
                        let rightViewTab = findView($scope.rightTabs, view);
                        if (rightViewTab) {
                            rightViewTab.expanded = true;
                        } else {
                            rightViewTab = mapViewToTab(view);
                            rightViewTab.expanded = true;
                            $scope.rightTabs.push(rightViewTab);
                        }

                    } else if (view.region === 'center') {
                        let result = findCenterSplittedTabViewById(view.id);
                        let currentTabsView = result ? result.tabsView : getCurrentCenterSplittedTabViewPane();
                        if (result) {
                            currentTabsView.selectedTab = view.id;
                        } else {
                            let centerViewTab = mapViewToTab(view);
                            currentTabsView.selectedTab = view.id;
                            currentTabsView.tabs.push(centerViewTab);
                        }
                    } else {
                        let bottomViewTab = findView($scope.bottomTabs, view);
                        if (bottomViewTab) {
                            $scope.selection.selectedBottomTab = bottomViewTab.id;
                        } else {
                            bottomViewTab = mapViewToTab(view);
                            $scope.selection.selectedBottomTab = bottomViewTab.id;
                            $scope.bottomTabs.push(bottomViewTab);
                        }

                        if ($scope.isBottomPaneCollapsed())
                            $scope.expandBottomPane();
                    }
                }
            };
            $scope.$on('$destroy', () => {
                Layout.removeMessageListener(viewOpenListener);
                Layout.removeMessageListener(viewSetDirtyListener);
                Layout.removeMessageListener(editorCloseListener);
                Layout.removeMessageListener(editorCloseAllListener);
                Layout.removeMessageListener(viewOpenListener);
                Layout.removeMessageListener(onGetOpenedEditorsListener);
                Layout.removeMessageListener(onIsViewOpenListener);
                Layout.removeMessageListener(onIsEditorOpenListener);
                Layout.removeMessageListener(onOpenEditorListener);
                Workspace.removeMessageListener(onFileSavedListener);
                Workspace.removeMessageListener(onFileMovedListener);
                Workspace.removeMessageListener(onFileRenamedListener);
                Workspace.removeMessageListener(onFileDeletedListener);
                Layout.removeMessageListener(onFocusViewListener);
                Layout.removeMessageListener(onFocusEditorListener);
            });
        }],
        templateUrl: '/services/web/platform-core/ui/templates/layout.html',
    }))
    .directive('accordion', () => ({
        restrict: 'E',
        replace: true,
        transclude: true,
        scope: { views: '=' },
        link: (scope) => {
            scope.getParams = (view) => JSON.stringify({
                ...view.params,
                container: 'layout',
                perspectiveId: perspectiveData.id,
            });
        },
        template: `<div class="bk-vbox bk-full-height pf-accordion">
            <bk-panel class="pf-accordion-panel" ng-attr-shrink="{{!view.expanded}}" compact="::true" expanded="view.expanded" ng-repeat="view in views track by view.id">
                <bk-panel-header>
                    <bk-panel-expand hint="{{view.expanded ? 'Collapse' : 'Expand' }} '{{::view.label}}' view"></bk-panel-expand>
                    <h4 bk-panel-title>{{::view.label}}</h4>
                </bk-panel-header>
                <bk-panel-content class="bk-full-height" aria-label="{{::view.label}} content">
                    <iframe title="{{::view.label}}" loading="{{::view.lazyLoad ? 'lazy' : 'eager'}}" ng-src="{{::view.path}}" data-parameters="{{::getParams(view)}}"></iframe>
                </bk-panel-content>
            </bk-panel>
        </div>`,
    }))
    .directive('layoutTabContent', ($timeout) => ({
        restrict: 'E',
        replace: true,
        scope: { tab: '=' },
        link: (scope, element) => {
            scope.tab.focusFrame = () => {
                $timeout(() => { element[0].contentWindow.focus() }, 0);
            };
            scope.getParams = () => JSON.stringify({
                ...scope.tab.params,
                container: 'layout',
                perspectiveId: perspectiveData.id,
            });
        },
        template: `<iframe title="{{::tab.label}}" tab-id={{::tab.id}} loading="{{::tab.lazyLoad ? 'lazy' : 'eager'}}" ng-src="{{::tab.path}}" data-parameters="{{getParams()}}"></iframe>`,
    }))
    .directive('splittedTabs', () => ({
        restrict: 'E',
        transclude: true,
        replace: true,
        scope: {
            direction: '=',
            panes: '=',
            focusedPane: '<',
            removeTab: '&',
            moveTab: '&',
            splitTabs: '&',
            hideTabs: '<',
        },
        link: (scope) => {
            scope.onRemoveTab = (pane) => {
                scope.removeTab({ pane: pane });
            };

            scope.onSplitTabs = (direction, pane) => {
                scope.splitTabs({ direction, pane });
            };

            scope.splitHorizontally = (pane) => {
                scope.splitTabs({ direction: 'horizontal', pane });
            };

            scope.splitVertically = (pane) => {
                scope.splitTabs({ direction: 'vertical', pane });
            };

            scope.canSplit = (pane) => {
                if (pane.tabs.length < 2) return false;

                const tab = pane.tabs.find(x => x.id === pane.selectedTab);
                return tab && !tab.dirty;
            };

            scope.isFocused = (pane) => {
                return pane === scope.focusedPane;
            };

            scope.isMoreTabsButtonVisible = (pane) => pane.tabs.some(x => x.isHidden);

            scope.onTabClick = (pane, tabId) => {
                pane.selectedTab = tabId;
                scope.focusedPane = pane;
                for (let t = 0; t < pane.tabs.length; t++) {
                    if (pane.tabs[t].id === tabId) pane.tabs[t].focusFrame();
                }
            };
        },
        templateUrl: '/services/web/platform-core/ui/templates/splitted-tabs.html',
    }));