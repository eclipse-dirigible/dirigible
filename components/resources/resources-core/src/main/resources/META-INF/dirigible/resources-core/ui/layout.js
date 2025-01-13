/*
 * Copyright (c) 2010-2024 SAP and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *   SAP - initial API and implementation
 */
angular.module('ideLayout', ['idePerspective', 'ideEditors', 'ideMessageHub', 'ideView'])
    .constant('perspective', perspectiveData || {})
    .constant('layoutConstants', {
        version: 3.0,
        stateKey: 'ide.layout.state'
    })
    .directive('view', ['Views', 'perspective', function (Views, perspective) {
        return {
            restrict: 'E',
            replace: true,
            scope: {
                id: '@',
                settings: '=',
            },
            link: function (scope) {
                Views.get().then(function (views) {
                    const view = views.find(v => v.id === scope.id);
                    if (view) {
                        scope.path = view.settings.path;
                        scope.loadType = view.settings.loadType;
                        if (!view.params) {
                            scope.params = {
                                container: 'layout',
                                perspectiveId: perspective.id,
                            };
                        } else {
                            scope.params = view.params;
                            scope.params['container'] = 'layout';
                            scope.params['perspectiveId'] = perspective.id;
                        }
                    } else {
                        throw Error(`view: view with id '${scope.id}' not found`);
                    }
                });

                scope.getParams = function () {
                    return JSON.stringify(scope.params);
                };
            },
            template: '<iframe loading="{{loadType}}" ng-src="{{path}}" data-parameters="{{getParams()}}"></iframe>'
        }
    }])
    .directive('ideLayout', ['Views', 'Editors', 'SplitPaneState', 'messageHub', 'perspective', 'layoutConstants', 'branding', 'uuid', function (Views, Editors, SplitPaneState, messageHub, perspective, layoutConstants, branding, uuid) {
        return {
            restrict: 'E',
            replace: true,
            scope: {
                viewsLayoutModel: '<',
                layoutConfig: '<?'
            },
            controller: ['$scope', '$element', function ($scope) {
                if (!perspective.id || !perspective.name)
                    console.error('<ide-layout> requires perspective service data');
                if (!angular.isDefined($scope.layoutConfig)) $scope.layoutConfig = {};

                const VIEW = 'view';
                const EDITOR = 'editor';

                const layoutStateKey = `${branding.keyPrefix}.${layoutConstants.stateKey}.${perspective.id}`;

                $scope.perspectiveName = '';
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
                    leftPaneSize: $scope.layoutConfig.leftPaneSize || 20,
                    leftPaneMinSize: $scope.layoutConfig.leftPaneMinSize || 0,
                    leftPaneMaxSize: $scope.layoutConfig.leftPaneMaxSize || undefined,
                    rightPaneSize: $scope.layoutConfig.rightPaneSize || 20,
                    rightPaneMinSize: $scope.layoutConfig.rightPaneMinSize || 0,
                    rightPaneMaxSize: $scope.layoutConfig.rightPaneMaxSize || undefined,
                    bottomPaneSize: $scope.layoutConfig.bottomPaneSize || 30,
                    hideEditorsPane: false,
                    hideCenterTabs: false,
                    hideBottomTabs: false,
                    ...($scope.viewsLayoutModel.layoutSettings || {})
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

                $scope.initialOpenViews = $scope.viewsLayoutModel.views;
                $scope.focusedTabView = null;

                let closingFileArgs;
                let reloadingFileArgs;
                let eventHandlers = $scope.viewsLayoutModel.events;
                let viewSettings = $scope.viewsLayoutModel.viewSettings || {};

                if (perspective.id && perspective.name) {
                    $scope.perspectiveName = perspective.name;
                    Views.get().then(function (views) {
                        $scope.views = views;

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
                        const byLeftRegion = view => view.region.startsWith('left');
                        const byRightRegion = view => view.region.startsWith('right');
                        const byBottomRegion = view => view.region === 'center-bottom' || view.region === 'bottom';
                        const byCenterRegion = view => view.region === 'center-top' || view.region === 'center-middle' || view.region === 'center';

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

                        $scope.$watch('selection', function (newSelection, oldSelection) {
                            if (!angular.equals(newSelection, oldSelection)) {
                                saveLayoutState();
                            }
                        }, true);

                        $scope.$watch('centerSplittedTabViews', function (newValue, oldValue) {
                            if (!angular.equals(newValue, oldValue)) {
                                saveLayoutState();
                            }
                        }, true);

                        $scope.$watch('leftTabs', function (newValue, oldValue) {
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

                            if (!collectionsEqual(newValue, oldValue, 'hidden')) {
                                saveLayoutState();
                            }
                        }, true);

                        if (eventHandlers) {
                            Object.keys(eventHandlers).forEach(function (evtName) {
                                let handler = eventHandlers[evtName];
                                if (typeof handler === 'function')
                                    messageHub.onDidReceiveMessage(evtName, handler);
                            });
                        }
                    });
                }

                $scope.isLastVisiblePaneTab = function (index) {
                    if (!$scope.leftTabs[index].hidden) {
                        let visibleNum = 0;
                        for (let i = 0; i < $scope.leftTabs.length; i++) {
                            if ($scope.leftTabs[i].hidden) {
                                visibleNum++;
                            }
                        }
                        if (visibleNum === $scope.leftTabs.length - 1) return true;
                    }
                    return false;
                };

                $scope.togglePaneView = function (index) {
                    if (!$scope.isLastVisiblePaneTab(index)) {
                        $scope.leftTabs[index].hidden = !$scope.leftTabs[index].hidden;
                    }
                };

                $scope.closeCenterTab = function (tab) {
                    tryCloseCenterTabs([tab]);
                };

                $scope.splitCenterTabs = function (direction, pane) {
                    splitTabs(direction, pane);
                };

                $scope.collapseBottomPane = function () {
                    updateSplitPanesState({
                        editorsPaneState: SplitPaneState.EXPANDED,
                        bottomPanesState: SplitPaneState.COLLAPSED
                    });
                };

                $scope.expandBottomPane = function () {
                    updateSplitPanesState({
                        editorsPaneState: SplitPaneState.EXPANDED,
                        bottomPanesState: SplitPaneState.EXPANDED
                    });
                };

                $scope.toggleEditorsPane = function () {
                    let editorsPaneCollapsed = $scope.isEditorsPaneCollapsed();

                    updateSplitPanesState({
                        editorsPaneState: editorsPaneCollapsed ? SplitPaneState.EXPANDED : SplitPaneState.COLLAPSED,
                        bottomPanesState: SplitPaneState.EXPANDED
                    });
                };

                $scope.isEditorsPaneCollapsed = function () {
                    return $scope.splitPanesState.main[0] == SplitPaneState.COLLAPSED;
                };

                $scope.isBottomPaneCollapsed = function () {
                    return $scope.splitPanesState.main.length < 2 || $scope.splitPanesState.main[1] == SplitPaneState.COLLAPSED;
                };

                $scope.isMoreTabsButtonVisible = function (tabs) {
                    return tabs.some(x => x.isHidden);
                }

                $scope.sideViewStateChanged = function () {
                    saveLayoutState();
                };

                function hasMajorVersionChanged(savedState) {
                    const newVersion = parseInt(layoutConstants.version);
                    const oldVersion = parseInt(savedState.version);
                    return newVersion > oldVersion;
                }

                function loadLayoutState() {
                    let savedState = localStorage.getItem(layoutStateKey);

                    if (savedState === null) {
                        //fall back on the obosolete key
                        //TODO: code to be removed at some point
                        const obosoleteKey = `DIRIGIBLE.IDE.LAYOUT.state.${perspective.id}`;
                        savedState = localStorage.getItem(obosoleteKey);
                        if (savedState !== null) {
                            localStorage.setItem(layoutStateKey, savedState);
                            localStorage.removeItem(obosoleteKey);
                        }
                    }

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
                                tabs: parent.tabs.map(({ id, type, label, path, loadType, params }) => {
                                    return type === VIEW ?
                                        { id, type } :
                                        { id, type, label, path, loadType, params };
                                }),
                                selectedTab: parent.selectedTab
                            };
                        }
                        return ret;
                    }

                    let state = {
                        version: layoutConstants.version,
                        initialOpenViews: $scope.initialOpenViews,
                        left: {
                            tabs: $scope.leftTabs.map(({ id, type, hidden, expanded }) => ({ id, type, hidden, expanded }))
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
                        path: view.settings.path,
                        loadType: view.settings.loadType,
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
                        const index = currentPane.tabs.findIndex(f => f.params.file === resourcePath);
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
                            if (childPane.tabs[tabIndex].type === EDITOR && childPane.tabs[tabIndex].params.file.startsWith(basePath)) {
                                fileList.push(childPane.tabs[tabIndex].params.file);
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
                        messageHub.showDialogAsync(
                            'You have unsaved changes',
                            `Do you want to save the changes you made to ${fileName}?`,
                            [{
                                id: { id: 'save', file: filePath, ...args },
                                type: 'emphasized',
                                label: 'Save',
                            }, {
                                id: { id: 'ignore', file: filePath, ...args },
                                type: 'normal',
                                label: 'Don\'t Save',
                            }, {
                                id: { id: 'cancel' },
                                type: 'transparent',
                                label: 'Cancel',
                            }]
                        ).then(({ data }) => {
                            const { id, file } = data;
                            switch (id) {
                                case 'save':
                                    messageHub.postMessage('editor.file.save', { file }, true);
                                    break;
                                case 'ignore':
                                    $scope.setEditorDirty(file, false);
                                    break;
                            }
                            resolve(data);

                        }).catch(reject);
                    });
                }

                function tryReloadCenterTab(tab, editorPath, params) {
                    if (tab.dirty) {
                        showFileSaveDialog(tab.label, tab.params.file, { editorPath, params })
                            .then(args => {
                                switch (args.id) {
                                    case 'save':
                                        reloadingFileArgs = args;
                                        break;
                                    case 'ignore':
                                        reloadCenterTab(args.editorPath, args.params);
                                        $scope.$digest();
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

                        showFileSaveDialog(tab.label, tab.params.file, { tabs })
                            .then(args => {
                                switch (args.id) {
                                    case 'save':
                                        closingFileArgs = args;
                                        break;
                                    case 'ignore':
                                        closeCenterTab(args.file)

                                        let rest = args.tabs.filter(x => x.params.file !== args.file);
                                        if (rest.length > 0)
                                            if (tryCloseCenterTabs(rest)) {
                                                $scope.$digest();
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

                window.onbeforeunload = function myFunction() {
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
                        $scope.$digest();
                    }
                }

                function reloadCenterTab(editorPath, params) {
                    let result = findCenterSplittedTabViewByPath(params.file);
                    if (result) {
                        const tab = result.tabsView.tabs[result.index];
                        tab.path = editorPath;
                        tab.params = params;
                    }
                }

                messageHub.onDidReceiveMessage(
                    'ide-core.openEditor',
                    function (data) {
                        $scope.$apply(
                            $scope.openEditor(
                                data.resourcePath,
                                data.resourceLabel,
                                data.contentType,
                                data.editorId,
                                data.extraArgs
                            )
                        );
                    },
                    true
                );

                messageHub.onDidReceiveMessage(
                    'ide-core.setEditorDirty',
                    function (data) {
                        $scope.$apply($scope.setEditorDirty(data.resourcePath, data.isDirty));
                    },
                    true
                );

                messageHub.onDidReceiveMessage(
                    'ide-core.closeEditor',
                    function (data) {
                        $scope.$apply($scope.closeEditor(data.resourcePath));
                    },
                    true
                );

                messageHub.onDidReceiveMessage(
                    'ide-core.closeOtherEditors',
                    function (data) {
                        $scope.$apply($scope.closeOtherEditors(data.resourcePath));
                    },
                    true
                );

                messageHub.onDidReceiveMessage(
                    'ide-core.closeAllEditors',
                    function () {
                        $scope.$apply($scope.closeAllEditors());
                    },
                    true
                );

                messageHub.onDidReceiveMessage(
                    'ide-core.openView',
                    function (data) {
                        $scope.$apply($scope.openView(data.viewId, data.params));
                    },
                    true
                );

                messageHub.onFileSaved(function (fileDescriptor) {
                    if (closingFileArgs) {
                        let fileName = `/${fileDescriptor.workspace}${fileDescriptor.path}`;
                        if (fileName === closingFileArgs.file) {
                            closeCenterTab(fileName);

                            let rest = closingFileArgs.tabs.filter(x => x.params.file !== closingFileArgs.file);
                            if (rest.length > 0) {
                                if (tryCloseCenterTabs(rest)) {
                                    $scope.$digest();
                                }
                            }

                            closingFileArgs = null;
                        }
                    }

                    if (reloadingFileArgs) {
                        const fileName = msg.data;
                        const { file, editorPath, params } = reloadingFileArgs;

                        if (fileName === file) {
                            reloadCenterTab(editorPath, params);
                            $scope.$digest();
                            reloadingFileArgs = null;
                        }
                    }
                });

                messageHub.onFileMoved(
                    function (fileDescriptor) {
                        $scope.$apply(
                            $scope.updateEditor(
                                `/${fileDescriptor.workspace}${fileDescriptor.oldPath}`,
                                `/${fileDescriptor.workspace}${fileDescriptor.path}`,
                                fileDescriptor.name
                            )
                        );
                    }
                );

                messageHub.onFileRenamed(
                    function (fileDescriptor) {
                        $scope.$apply(
                            $scope.updateEditor(
                                `/${fileDescriptor.workspace}${fileDescriptor.oldPath}`,
                                `/${fileDescriptor.workspace}${fileDescriptor.path}`,
                                fileDescriptor.name
                            )
                        );
                    }
                );

                messageHub.onFileDeleted(
                    function (fileDescriptor) {
                        $scope.$apply(
                            $scope.closeEditor(`/${fileDescriptor.workspace}${fileDescriptor.path}`)
                        );
                    }
                );

                messageHub.onDidReceiveMessage('ide-core.setFocusedEditor', function (msg) {
                    const result = findCenterSplittedTabViewByPath(msg.resourcePath);
                    if (result) {
                        $scope.$apply(() => {
                            $scope.focusedTabView = result.tabsView;
                        });
                    } else {
                        console.warn(`Tabview for file '${msg.resourcePath}' not found`);
                    }
                }, true);

                messageHub.onSetTabFocus(
                    function (msg) {
                        const result = findCenterSplittedTabViewById(msg.tabId);
                        $scope.$apply(() => {
                            $scope.focusedTabView = result.tabsView;
                        });
                    }
                );

                messageHub.onDidReceiveMessage('core.editors.isOpen', function (msg) {
                    const result = findCenterSplittedTabViewByPath(msg.resourcePath);
                    if (result) {
                        messageHub.postMessage(msg.callbackTopic, {
                            isOpen: true,
                            isDirty: result.tabsView.tabs[result.index].dirty || false,
                        }, true);
                    } else {
                        messageHub.postMessage(msg.callbackTopic, { isOpen: false }, true);
                    }
                }, true);

                messageHub.onDidReceiveMessage('core.editors.openedFiles', function (msg) {
                    messageHub.postMessage(msg.callbackTopic, { files: getCurrentlyOpenedFiles(msg.basePath) }, true);
                }, true);

                function shortenCenterTabsLabels() {

                    const getTabPath = tab => {
                        const index = tab.params.file.lastIndexOf('/');
                        return tab.params.file.substring(0, index > 0 ? index : tab.params.file.length);
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

                $scope.openEditor = function (resourcePath, resourceLabel, contentType, editorId, extraArgs = null) {
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
                            file: resourcePath,
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

                        $scope.$digest();
                    } else {
                        console.error('openEditor: resourcePath is undefined');
                    }
                };
                $scope.updateEditor = function (resourcePath, newResourcePath, resourceLabel) {
                    if (resourcePath === undefined && resourcePath === null && resourcePath.trim() === '')
                        console.error('updateEditor: resourcePath is undefined');
                    else if (newResourcePath === undefined && newResourcePath === null && newResourcePath.trim() === '')
                        console.error('updateEditor: newResourcePath is undefined');
                    else if (resourceLabel === undefined && resourceLabel === null && resourceLabel.trim() === '')
                        console.error('updateEditor: resourceLabel is undefined');
                    else {
                        let result = findCenterSplittedTabViewByPath(resourcePath);
                        if (result) {
                            result.tabsView.tabs[result.index].label = resourceLabel;
                            result.tabsView.tabs[result.index].params.file = newResourcePath;
                            messageHub.editorReloadParameters(resourcePath);
                            shortenCenterTabsLabels();
                            $scope.$digest();
                        }
                    }
                };
                $scope.closeEditor = function (resourcePath) {
                    let result = findCenterSplittedTabViewByPath(resourcePath);
                    if (result) {
                        let tab = result.tabsView.tabs[result.index];
                        if (tryCloseCenterTabs([tab])) {
                            $scope.$digest();
                        }
                    }
                };
                $scope.closeOtherEditors = function (resourcePath) {
                    let result = findCenterSplittedTabViewByPath(resourcePath);
                    if (result) {
                        let rest = result.tabsView.tabs.filter(x => x.type === EDITOR && x.params.file !== resourcePath);
                        if (rest.length > 0) {
                            if (tryCloseCenterTabs(rest)) {
                                $scope.$digest();
                            }
                        }
                    }
                };
                $scope.closeAllEditors = function () {
                    forEachCenterSplittedTabView(pane => {
                        if (tryCloseCenterTabs(pane.tabs.filter(x => x.type === EDITOR))) {
                            $scope.$digest();
                        }
                    }, $scope.centerSplittedTabViews);
                };
                $scope.setEditorDirty = function (resourcePath, dirty) {
                    let result = findCenterSplittedTabViewByPath(resourcePath);
                    if (result) {
                        let fileTab = result.tabsView.tabs[result.index];
                        fileTab.dirty = dirty;
                        $scope.$digest();
                    }
                };
                $scope.openView = function (viewId, params = {}) {
                    if (params !== undefined && !(typeof params === 'object' && !Array.isArray(params) && params !== null))
                        throw Error("openView: params must be an object");
                    let view = $scope.views.find(v => v.id === viewId);
                    if (view) {
                        view.params = params;
                        if (view.region.startsWith('left')) {
                            let leftViewTab = findView($scope.leftTabs, view);
                            if (leftViewTab) {
                                leftViewTab.hidden = false;
                                leftViewTab.expanded = true;
                            } else {
                                leftViewTab = mapViewToTab(view);
                                leftViewTab.expanded = true;
                                $scope.leftTabs.push(leftViewTab);
                            }

                        } else if (view.region.startsWith('right')) {
                            let rightViewTab = findView($scope.rightTabs, view);
                            if (rightViewTab) {
                                rightViewTab.expanded = true;
                            } else {
                                rightViewTab = mapViewToTab(view);
                                rightViewTab.expanded = true;
                                $scope.rightTabs.push(rightViewTab);
                            }

                        } else if (view.region === 'center-middle' || view.region === 'center-top' || view.region === 'center') {
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
            }],
            templateUrl: '/services/web/resources-core/ui/templates/layout.html'
        };
    }])
    .directive('accordion', ['$window', function ($window) {
        return {
            restrict: 'E',
            replace: true,
            transclude: true,
            scope: {},
            controller: function ($scope, $element) {
                let views = $scope.views = [];

                let availableHeight;

                function updateContentHeights(collapsingView = null) {
                    let expandedViews = $scope.views.filter(view => view.expanded);
                    let expandedViewsCount = expandedViews.length;
                    if (collapsingView) expandedViewsCount--;

                    let panelHeight = expandedViewsCount > 0 ? availableHeight / expandedViewsCount : 0;

                    for (let i = 0; i < $scope.views.length; i++) {
                        let view = $scope.views[i];
                        view.style = {
                            height: view.expanded && view !== collapsingView ? panelHeight + 'px' : '0'
                        };
                    }
                }

                function updateSize() {
                    let totalHeight = $element[0].clientHeight;
                    let headersHeight = getHeadersHeight();

                    availableHeight = totalHeight - headersHeight;

                    updateContentHeights();
                }

                function getHeadersHeight() {
                    let headers = $element[0].querySelectorAll('.fd-panel__header');

                    let h = 0;
                    for (let i = 0; i < headers.length; i++) {
                        h += headers[i].offsetHeight;
                    }
                    return h;
                }

                this.addView = function (view) {
                    views.push(view);

                    updateContentHeights();
                }

                this.removeView = function (view) {
                    let index = views.indexOf(view);
                    if (index >= 0)
                        views.splice(index, 1);

                    updateSize();
                }

                this.updateHeights = function (view) {
                    updateContentHeights(view);
                }

                this.updateSizes = function () {
                    updateSize();
                }

                angular.element($window).on('resize', function () {
                    $scope.$apply(updateSize);
                });

                $scope.$on('$destroy', function () {
                    angular.element($window).off('resize');
                });
            },
            template: '<div class="dg-accordion" ng-transclude></div>'
        };
    }])
    .directive('accordionPane', ['$timeout', 'perspective', function ($timeout, perspective) {
        return {
            restrict: 'E',
            replace: true,
            require: '^accordion',
            scope: {
                view: '=',
                stateChanged: '&'
            },
            link: function (scope, element, attrs, accordionCtrl) {
                accordionCtrl.addView(scope.view);

                scope.toggleView = function (view) {
                    if (!view.expanded) {
                        view.expanded = true;
                        scope.stateChanged({ expanded: view.expanded });
                        $timeout(accordionCtrl.updateHeights);
                    } else {
                        accordionCtrl.updateHeights(view);
                        $timeout(function () {
                            view.expanded = false;
                            scope.stateChanged({ expanded: view.expanded });
                        }, 200);
                    }
                }

                scope.getParams = function () {
                    if (!scope.view.params) {
                        scope.view.params = {
                            container: 'layout',
                            perspectiveId: perspective.id,
                        };
                    } else {
                        scope.view.params['container'] = 'layout';
                        scope.view.params['perspectiveId'] = perspective.id;
                    }
                    return JSON.stringify(scope.view.params);
                }

                scope.$watch('view', function () {
                    accordionCtrl.updateSizes();
                }, true);

                scope.$on('$destroy', function () {
                    accordionCtrl.removeView(scope.view);
                });
            },
            templateUrl: '/services/web/resources-core/ui/templates/accordionPane.html'
        };
    }])
    .directive('layoutTabContent', ['perspective', function (perspective) {
        return {
            restrict: 'E',
            replace: true,
            scope: {
                tab: '='
            },
            link: function (scope) {
                scope.getParams = function () {
                    if (!scope.tab.params) {
                        scope.tab.params = {
                            container: 'layout',
                            perspectiveId: perspective.id,
                        };
                    } else {
                        scope.tab.params['container'] = 'layout';
                        scope.tab.params['perspectiveId'] = perspective.id;
                    }
                    return JSON.stringify(scope.tab.params);
                };
            },
            template: `<iframe loading="{{tab.loadType}}" ng-src="{{tab.path}}" data-parameters="{{getParams()}}"></iframe>`
        }
    }])
    .directive('splittedTabs', ['messageHub', function (messageHub) {
        return {
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
                editorTabs: '<?',
            },
            link: function (scope) {
                scope.onRemoveTab = function (pane) {
                    scope.removeTab({ pane: pane });
                };

                scope.onSplitTabs = function (direction, pane) {
                    scope.splitTabs({ direction, pane });
                };

                scope.splitHorizontally = function (pane) {
                    scope.splitTabs({ direction: 'horizontal', pane });
                };

                scope.splitVertically = function (pane) {
                    scope.splitTabs({ direction: 'vertical', pane });
                };

                scope.canSplit = function (pane) {
                    if (pane.tabs.length < 2)
                        return false;

                    const tab = pane.tabs.find(x => x.id === pane.selectedTab);
                    return tab && !tab.dirty;
                };

                scope.isFocused = function (pane) {
                    return pane === scope.focusedPane;
                };

                scope.isMoreTabsButtonVisible = function (pane) {
                    return pane.tabs.some(x => x.isHidden);
                };

                scope.isEditorTab = function () {
                    return scope.editorTabs;
                };

                scope.onTabClick = function (pane, tabId, file) {
                    pane.selectedTab = tabId;
                    messageHub.setTabFocus(tabId);

                    const autoRevealEnabled = window.localStorage.getItem('DIRIGIBLE.autoRevealActionEnabled');
                    if (file && (autoRevealEnabled === null || autoRevealEnabled === 'true')) {
                        setTimeout(() => {
                            messageHub.postMessage('projects.tree.select', {
                                filePath: file
                            }, true);
                        }, 100);
                    }
                };
            },
            templateUrl: '/services/web/resources-core/ui/templates/splittedTabs.html'
        };
    }]);