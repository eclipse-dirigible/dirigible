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
const editorView = angular.module('intentEditor', ['blimpKit', 'platformView', 'platformShortcuts', 'WorkspaceService']);
editorView.controller('IntentEditorController', ($scope, $http, ViewParameters, WorkspaceService) => {
    const statusBarHub = new StatusBarHub();
    const workspaceHub = new WorkspaceHub();
    const layoutHub = new LayoutHub();
    const dialogHub = new DialogHub();
    const PARSE_URL = '/services/ide/intent/parse';
    const GENERATE_URL = '/services/ide/intent/generate';

    $scope.state = { isBusy: true, error: false };
    $scope.errorMessage = '';
    $scope.changed = false;
    $scope.text = '';
    $scope.model = { entities: [], processes: [], forms: [], reports: [], permissions: [], seeds: [] };
    $scope.issues = [];
    let savedText = '';
    let parseTimer = null;

    // ----- Diagram palette -----------------------------------------------------
    // The diagram is drawn with mxGraph (the same engine the EDM and BPMN modelers use), so it inherits
    // their robust rendering instead of Mermaid's brittle theming. Colours are fixed brand tones that
    // read equally well on the light and dark IDE themes - solid fills with white labels on a
    // transparent canvas, exactly like the schema/entity modelers - so the diagram looks identical in
    // either theme and needs no recolour on a theme switch. Values mirror editor-entity/css/styles.css.
    const COLOR = {
        entity: '#3584e4',   // blue   - entities, user tasks
        service: '#26a269',  // green  - service / script tasks
        decision: '#e9a319', // amber  - decision gateways
        terminal: '#708090', // slate  - start / end events
        edge: '#7a8896',     // mid-gray - relations and sequence flows, visible on both themes
        label: '#ffffff'     // white  - on-shape text
    };

    // ----- File location -----------------------------------------------------

    /** filePath has the shape /<workspace>/<project>/<path/within/project> */
    const fileLocation = () => {
        const parts = $scope.dataParameters.filePath.split('/');
        return {
            workspace: parts[1],
            project: parts[2],
            path: parts.slice(3)
                       .join('/'),
        };
    };

    // ----- Load / save ---------------------------------------------------------

    const loadFileContents = () => {
        WorkspaceService.loadContent($scope.dataParameters.filePath).then((response) => {
            $scope.$evalAsync(() => {
                $scope.text = typeof response.data === 'string' ? response.data : JSON.stringify(response.data, null, 2);
                savedText = $scope.text;
                $scope.state.isBusy = false;
                refreshPreview();
            });
        }, (response) => {
            console.error(response);
            $scope.$evalAsync(() => {
                $scope.state.error = true;
                $scope.errorMessage = 'Error while loading the intent file. Please look at the console for more information.';
                $scope.state.isBusy = false;
            });
        });
    };

    $scope.save = () => {
        if (!$scope.changed || $scope.state.error) return;
        $scope.state.isBusy = true;
        WorkspaceService.saveContent($scope.dataParameters.filePath, $scope.text).then(() => {
            savedText = $scope.text;
            layoutHub.setEditorDirty({
                path: $scope.dataParameters.filePath,
                dirty: false,
            });
            workspaceHub.announceFileSaved({
                path: $scope.dataParameters.filePath,
                contentType: $scope.dataParameters.contentType,
            });
            $scope.$evalAsync(() => {
                $scope.changed = false;
                $scope.state.isBusy = false;
            });
        }, (response) => {
            console.error(response);
            $scope.$evalAsync(() => {
                $scope.state.error = true;
                $scope.errorMessage = `Error saving "${$scope.dataParameters.filePath}". Please look at the console for more information.`;
                $scope.state.isBusy = false;
            });
        });
    };

    // ----- Live preview --------------------------------------------------------

    $scope.onTextChange = () => {
        const dirty = $scope.text !== savedText;
        if (dirty !== $scope.changed) {
            $scope.changed = dirty;
            layoutHub.setEditorDirty({
                path: $scope.dataParameters.filePath,
                dirty: dirty,
            });
        }
        if (parseTimer) clearTimeout(parseTimer);
        parseTimer = setTimeout(refreshPreview, 600);
    };

    const refreshPreview = () => {
        $http.post(PARSE_URL, $scope.text || '', { headers: { 'Content-Type': 'text/plain' } }).then((response) => {
            $scope.issues = [];
            $scope.model = normalize(response.data);
            render();
        }, (response) => {
            $scope.$evalAsync(() => {
                if (response.status === 422 && response.data && response.data.issues) {
                    $scope.issues = response.data.issues;
                } else {
                    $scope.issues = ['Unable to parse the intent. Please look at the console for more information.'];
                    console.error(response);
                }
            });
        });
    };

    const normalize = (model) => {
        model = model || {};
        model.entities = model.entities || [];
        model.processes = model.processes || [];
        model.forms = model.forms || [];
        model.reports = model.reports || [];
        model.permissions = model.permissions || [];
        model.seeds = model.seeds || [];
        return model;
    };

    // ----- Generate ------------------------------------------------------------

    $scope.generate = () => {
        const location = fileLocation();
        $scope.state.isBusy = true;
        dialogHub.showBusyDialog('Generating model files');
        $http.post(`${GENERATE_URL}?workspace=${encodeURIComponent(location.workspace)}&project=${encodeURIComponent(location.project)}&path=${encodeURIComponent(location.path)}`)
             .then((response) => {
                 dialogHub.closeBusyDialog();
                 const written = (response.data.written || []).length;
                 const scrubbed = (response.data.scrubbed || []).length;
                 statusBarHub.showMessage(`Generated ${written} model file(s)${scrubbed ? `, removed ${scrubbed} stale` : ''} in '${location.project}'`);
                 dialogHub.postMessage({ topic: 'projects.tree.refresh', data: { partial: true, project: location.project, workspace: location.workspace } });
                 $scope.$evalAsync(() => {
                     $scope.state.isBusy = false;
                 });
             }, (response) => {
                 console.error(response);
                 dialogHub.closeBusyDialog();
                 $scope.$evalAsync(() => {
                     $scope.state.isBusy = false;
                     if (response.status === 422 && response.data && response.data.issues) {
                         $scope.issues = response.data.issues;
                     } else {
                         dialogHub.showAlert({
                             title: 'Failed to generate',
                             message: 'Please look at the console for more information',
                             type: AlertTypes.Error,
                             preformatted: false,
                         });
                     }
                 });
             });
    };

    // ----- Diagram rendering (mxGraph) -------------------------------------------
    // One read-only mxGraph per section: an ER-style diagram for the entities and one top-down
    // flowchart per process. Each graph paints fixed-colour cells on a transparent canvas, so the
    // whole pane tracks the IDE theme through its CSS background while the cells stay legible in both.

    const escapeHtml = (s) => String(s == null ? '' : s).replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));

    const diagrams = []; // live mxGraph instances, destroyed before each re-render

    const teardown = () => {
        while (diagrams.length) diagrams.pop().destroy();
        const host = document.getElementById('intent-diagrams');
        if (host) host.innerHTML = '';
    };

    // A read-only graph wired into a freshly created, titled container appended to the diagram host.
    const newSection = (title) => {
        const host = document.getElementById('intent-diagrams');
        const heading = document.createElement('h4');
        heading.className = 'intent-section-title';
        heading.textContent = title;
        host.appendChild(heading);
        const container = document.createElement('div');
        container.className = 'intent-diagram';
        host.appendChild(container);

        const graph = new mxGraph(container);
        graph.setHtmlLabels(true);
        graph.setEnabled(false);            // read-only: no selection, editing or connecting
        graph.setTooltips(false);
        graph.setPanning(false);
        graph.setCellsLocked(true);
        graph.border = 16;
        graph.keepEdgesInBackground = true;
        diagrams.push(graph);
        return graph;
    };

    const nodeStyle = (fill) => `rounded=1;whiteSpace=wrap;html=1;fillColor=${fill};strokeColor=${fill};fontColor=${COLOR.label};verticalAlign=top;spacingTop=2;arcSize=8;`;
    const shapeStyle = (shape, fill) => `shape=${shape};whiteSpace=wrap;html=1;fillColor=${fill};strokeColor=${fill};fontColor=${COLOR.label};`;
    const edgeStyle = (dashed) => `edgeStyle=orthogonalEdgeStyle;rounded=1;html=1;strokeColor=${COLOR.edge};fontColor=${COLOR.edge};endArrow=open;${dashed ? 'dashed=1;' : ''}`;

    // Fit the container to the laid-out graph so it shows at natural size and the pane scrolls.
    const sizeToContent = (graph, container) => {
        const bounds = graph.getGraphBounds();
        container.style.height = `${Math.ceil(bounds.height) + 2 * graph.border}px`;
    };

    // Entity card: a blue panel whose HTML label is the entity name over its field list (PK marked).
    const entityLabel = (entity) => {
        const fields = (entity.fields || []).filter(f => f && f.name);
        const rows = fields.map((f) => {
            const type = escapeHtml(f.type || 'string');
            const pk = f.primaryKey ? ' <strong>PK</strong>' : '';
            return `<div style="opacity:.92">${escapeHtml(f.name)} : ${type}${pk}</div>`;
        }).join('');
        return `<div style="font-weight:bold;border-bottom:1px solid rgba(255,255,255,.5);padding-bottom:2px;margin-bottom:2px">${escapeHtml(entity.name)}</div>`
            + `<div style="font-size:11px;text-align:left">${rows}</div>`;
    };

    const renderEntities = () => {
        const entities = $scope.model.entities.filter(e => e && e.name);
        if (!entities.length) return;
        const graph = newSection('Entities');
        const container = graph.container;
        const parent = graph.getDefaultParent();
        graph.getModel().beginUpdate();
        try {
            const byName = {};
            for (const entity of entities) {
                const height = 30 + 18 * (entity.fields || []).filter(f => f && f.name).length;
                byName[entity.name] = graph.insertVertex(parent, null, entityLabel(entity), 0, 0, 200, Math.max(height, 48), nodeStyle(COLOR.entity));
            }
            for (const entity of entities) {
                for (const relation of (entity.relations || [])) {
                    if (!relation || !relation.to || !byName[relation.to]) continue;
                    // Composition (required to-one) is a solid edge, association is dashed - matching the EDM semantics.
                    graph.insertEdge(parent, null, relation.name || '', byName[entity.name], byName[relation.to], edgeStyle(!relation.required));
                }
            }
            const layout = new mxFastOrganicLayout(graph);
            layout.forceConstant = 180;
            layout.disableEdgeStyle = false;
            layout.execute(parent);
        } finally {
            graph.getModel().endUpdate();
        }
        sizeToContent(graph, container);
    };

    // Mirrors BpmnIntentGenerator: a linear chain through the declared steps; a decision emits a
    // labelled conditioned edge to `then` and routes its default edge to `else` (falling back to the
    // next step in the chain); `end`-kind steps collapse into the single end node.
    const renderProcess = (process) => {
        const steps = (process.steps || []).filter(s => s && s.name);
        const graph = newSection('Process: ' + process.name);
        const container = graph.container;
        const parent = graph.getDefaultParent();
        graph.getModel().beginUpdate();
        try {
            const start = graph.insertVertex(parent, null, 'start', 0, 0, 60, 40, shapeStyle('ellipse', COLOR.terminal));
            const end = graph.insertVertex(parent, null, 'end', 0, 0, 60, 40, shapeStyle('ellipse', COLOR.terminal));
            const byName = {};
            for (const step of steps) {
                if (String(step.kind).toLowerCase() === 'end') { byName[step.name] = end; continue; }
                if (step.kind === 'decision') byName[step.name] = graph.insertVertex(parent, null, step.name, 0, 0, 120, 70, shapeStyle('rhombus', COLOR.decision));
                else if (step.kind === 'serviceTask' || step.kind === 'script') byName[step.name] = graph.insertVertex(parent, null, step.name, 0, 0, 140, 44, nodeStyle(COLOR.service));
                else byName[step.name] = graph.insertVertex(parent, null, step.name, 0, 0, 140, 44, nodeStyle(COLOR.entity));
            }
            const vertexFor = (name) => {
                if (String(name).toLowerCase() === 'end') return end;
                return byName[name] || end;
            };

            const chain = [start];
            for (const step of steps) {
                const v = String(step.kind).toLowerCase() === 'end' ? end : byName[step.name];
                if (chain[chain.length - 1] !== v) chain.push(v);
            }
            if (chain[chain.length - 1] !== end) chain.push(end);

            for (let i = 0; i < chain.length - 1; i++) {
                const source = chain[i];
                let target = chain[i + 1];
                const decision = steps.find(s => byName[s.name] === source && s.kind === 'decision');
                if (decision) {
                    const args = decision.args || {};
                    if (args['else']) target = vertexFor(args['else']);
                    graph.insertEdge(parent, null, '', source, target, edgeStyle(true));
                    if (args['if'] && args['then']) {
                        graph.insertEdge(parent, null, String(args['if']), source, vertexFor(args['then']), edgeStyle(false));
                    }
                } else {
                    graph.insertEdge(parent, null, '', source, target, edgeStyle(false));
                }
            }

            const layout = new mxHierarchicalLayout(graph, mxConstants.DIRECTION_NORTH);
            layout.intraCellSpacing = 30;
            layout.interRankCellSpacing = 60;
            layout.execute(parent);
        } finally {
            graph.getModel().endUpdate();
        }
        sizeToContent(graph, container);
    };

    const render = () => {
        const host = document.getElementById('intent-diagrams');
        if (!host || typeof mxGraph === 'undefined') return;
        teardown();
        renderEntities();
        for (const process of $scope.model.processes) {
            if (process && process.name) renderProcess(process);
        }
        if (!diagrams.length) {
            const empty = document.createElement('div');
            empty.className = 'intent-diagram-empty';
            empty.textContent = 'Nothing to diagram yet - declare entities or processes.';
            host.appendChild(empty);
        }
    };

    // ----- Editor lifecycle wiring -----------------------------------------------

    layoutHub.onFocusEditor((data) => {
        if (data.path && data.path === $scope.dataParameters.filePath) statusBarHub.showLabel('');
    });

    layoutHub.onReloadEditorParams((data) => {
        if (data.path === $scope.dataParameters.filePath) {
            $scope.$evalAsync(() => {
                $scope.dataParameters = ViewParameters.get();
            });
        };
    });

    workspaceHub.onSaveAll(() => {
        if ($scope.changed && !$scope.state.error) {
            $scope.save();
        }
    });

    workspaceHub.onSaveFile((data) => {
        if (data.path && data.path === $scope.dataParameters.filePath) {
            if ($scope.changed && !$scope.state.error) {
                $scope.save();
            }
        }
    });

    $scope.$on('$destroy', teardown);

    $scope.dataParameters = ViewParameters.get();
    if (!$scope.dataParameters.hasOwnProperty('filePath')) {
        $scope.state.error = true;
        $scope.errorMessage = 'The \'filePath\' data parameter is missing.';
        $scope.state.isBusy = false;
    } else loadFileContents();
});
