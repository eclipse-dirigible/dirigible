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
const editorView = angular.module('intentEditor', ['blimpKit', 'platformView', 'platformShortcuts', 'platformSplit', 'WorkspaceService']);
editorView.controller('IntentEditorController', ($scope, $http, ViewParameters, WorkspaceService) => {
    const statusBarHub = new StatusBarHub();
    const workspaceHub = new WorkspaceHub();
    const layoutHub = new LayoutHub();
    const dialogHub = new DialogHub();
    const PARSE_URL = '/services/ide/intent/parse';
    const GENERATE_URL = '/services/ide/intent/generate';
    const AGENT_URL = '/services/ide/intent/agent';

    $scope.state = { isBusy: true, error: false };
    $scope.errorMessage = '';
    $scope.changed = false;
    $scope.text = '';
    $scope.model = { entities: [], processes: [], forms: [], reports: [], permissions: [], seeds: [], notifications: [], schedules: [], integrations: [], inbound: [], rollups: [] };
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
        output: '#9141ac',   // purple - authoring outputs (forms, reports)
        glue: '#c64600',     // rust   - declarative glue (notifications, schedules, integrations, inbound, rollups)
        label: '#ffffff'     // white  - on-shape text
    };

    // SAP-icon glyphs that badge each non-entity artifact in the "Glue & Outputs" diagram, so forms,
    // reports and the event-driven glue are recognizable at a glance. These are the platform's icon
    // font (the same family the toolbar buttons use, loaded via the ng-editor platform-links category);
    // the diagram renders HTML labels, so an <i class="sap-icon--..."> draws the glyph and inherits the
    // label colour - monochrome and theme-consistent, not decorative emoji.
    const ICON = {
        form: 'sap-icon--form',
        report: 'sap-icon--bar-chart',
        notification: 'sap-icon--email',
        schedule: 'sap-icon--date-time',
        integration: 'sap-icon--chain-link',
        inbound: 'sap-icon--inbox',
        rollup: 'sap-icon--sum'
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
                mountEditor();
            });
        }, (response) => {
            console.error(response);
            if (response && response.status === 404) {
                // The file no longer exists (e.g. the workspace was cleaned by a rebuild) - close the stale editor.
                layoutHub.closeEditor({ path: $scope.dataParameters.filePath });
                return;
            }
            $scope.$evalAsync(() => {
                $scope.state.error = true;
                $scope.errorMessage = 'Error while loading the intent file. Please look at the console for more information.';
                $scope.state.isBusy = false;
            });
        });
    };

    $scope.save = (keySet = 'ctrl+s', event) => {
        event?.preventDefault();
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

    // ----- Monaco source editor ------------------------------------------------
    // The left pane is a Monaco editor (the same engine as the platform's main code editor) with YAML
    // highlighting; $scope.text stays the single source the parse / save / diagram code reads, kept in
    // sync from Monaco's change event. The theme follows the IDE via ThemingHub, mirroring editor-monaco.
    const themingHub = new ThemingHub();
    let monacoEditor = null;
    let monacoApi = null; // the loaded monaco module, reused by the AI assistant's diff editor

    const monacoThemeFor = (theme) => {
        if (!theme) theme = themingHub.getSavedTheme();
        if (theme && theme.type === 'light') return 'vs-light';
        const classic = theme && typeof theme.id === 'string' && theme.id.startsWith('classic');
        if (theme && theme.type === 'dark') return classic ? 'classic-dark' : 'blimpkit-dark';
        const prefersDark = window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches;
        if (!prefersDark) return 'vs-light';
        return classic ? 'classic-dark' : 'blimpkit-dark';
    };

    // The platform's dark editor themes - editor-monaco defines these too; redefining is idempotent.
    const defineMonacoThemes = (monaco) => {
        monaco.editor.defineTheme('blimpkit-dark', {
            base: 'vs-dark', inherit: true, rules: [{ background: '1d1d1d' }],
            colors: { 'editor.background': '#1d1d1d', 'minimap.background': '#1d1d1d', 'editorGutter.background': '#1d1d1d' }
        });
        monaco.editor.defineTheme('classic-dark', {
            base: 'vs-dark', inherit: true, rules: [{ background: '1c2228' }],
            colors: { 'editor.background': '#1c2228', 'minimap.background': '#1c2228', 'editorGutter.background': '#1c2228' }
        });
    };

    const mountEditor = () => {
        if (monacoEditor || typeof require === 'undefined') return;
        require.config({ paths: { vs: '/webjars/monaco-editor/min/vs' } });
        require(['vs/editor/editor.main'], (monaco) => {
            monacoApi = monaco;
            const container = document.getElementById('intent-monaco');
            if (!container) return;
            defineMonacoThemes(monaco);
            monacoEditor = monaco.editor.create(container, {
                value: $scope.text || '',
                language: 'yaml',
                theme: monacoThemeFor(),
                automaticLayout: true,
                fontSize: 13,
                tabSize: 2,
                insertSpaces: true,
                minimap: { enabled: false },
                scrollBeyondLastLine: false,
                renderWhitespace: 'selection',
            });
            // Monaco owns the text now; push every edit back into $scope.text and run the same
            // dirty-tracking + debounced re-parse the textarea's ng-change used to drive.
            monacoEditor.onDidChangeModelContent(() => {
                $scope.$evalAsync(() => {
                    $scope.text = monacoEditor.getValue();
                    handleTextChanged();
                });
            });
            themingHub.onThemeChange((theme) => monaco.editor.setTheme(monacoThemeFor(theme)));
            recoverWhenRevealed(container);
        });
    };

    // After a browser refresh every open editor iframe bootstraps at once, but only the focused tab
    // is visible - the rest sit in `display:none` tab panels (the layout toggles them with ng-show).
    // Monaco created inside a hidden, zero-size container measures a zero-width font and lays out to
    // nothing, and unlike the geometry-driven mxGraph diagrams it does not repaint on its own when the
    // tab is finally revealed: the text pane shows up blank. `automaticLayout` re-runs layout on the
    // size change but keeps the stale zero font metrics, so the glyphs stay invisible. Watch for the
    // container gaining real size (the tab being shown) and, once, remeasure the fonts and relayout so
    // the source appears. A no-op for the focused editor, which is already sized when Monaco is created.
    let revealObserver = null;
    const recoverWhenRevealed = (container) => {
        if (typeof ResizeObserver === 'undefined') return;
        if (container.clientWidth > 0 && container.clientHeight > 0) return; // already visible at creation
        revealObserver = new ResizeObserver(() => {
            if (container.clientWidth > 0 && container.clientHeight > 0) {
                revealObserver.disconnect();
                revealObserver = null;
                if (!monacoEditor) return;
                if (monacoApi) monacoApi.editor.remeasureFonts();
                monacoEditor.layout();
            }
        });
        revealObserver.observe(container);
    };

    // ----- Live preview --------------------------------------------------------

    const handleTextChanged = () => {
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
        return $http.post(PARSE_URL, $scope.text || '', { headers: { 'Content-Type': 'text/plain' } }).then((response) => {
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

    // Re-parse and re-validate the current buffer on demand. Generate resolves cross-model
    // dependencies against other projects' already-generated .model files; when one is missing it
    // fails with issues that stay pinned (blocking Generate) until the buffer is re-parsed. After
    // generating the dependency project, Refresh clears those stale issues and re-renders — no browser
    // reload needed.
    $scope.refresh = () => {
        refreshPreview().then(() => {
            statusBarHub.showMessage('Re-validated the intent');
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
        model.notifications = model.notifications || [];
        model.schedules = model.schedules || [];
        model.integrations = model.integrations || [];
        model.inbound = model.inbound || [];
        model.rollups = model.rollups || [];
        return model;
    };

    // ----- Generate ------------------------------------------------------------

    const finishGenerate = (location, written, scrubbed, codeCount) => {
        dialogHub.closeBusyDialog();
        const code = codeCount ? `, generated code from ${codeCount} model(s)` : '';
        const stale = scrubbed ? `, removed ${scrubbed} stale` : '';
        statusBarHub.showMessage(`Generated ${written} model file(s)${stale}${code} in '${location.project}'`);
        dialogHub.postMessage({ topic: 'projects.tree.refresh', data: { partial: true, project: location.project, workspace: location.workspace } });
        $scope.$evalAsync(() => { $scope.state.isBusy = false; });
    };

    // Run the model-to-code plan from the response (the templates + parameters registered in the
    // <intent>.settings), one generate-from-template call per generated model, sequentially.
    const runCodeGenerations = (location, plan, written, scrubbed) => {
        let index = 0;
        const next = () => {
            if (index >= plan.length) {
                finishGenerate(location, written, scrubbed, plan.length);
                return;
            }
            const entry = plan[index++];
            dialogHub.showBusyDialog(`Generating code (${index}/${plan.length}): ${entry.path}`);
            const url = `/services/js/service-generate/generate.mjs/model/${encodeURIComponent(location.workspace)}/${encodeURIComponent(location.project)}?path=${encodeURIComponent(entry.path)}`;
            $http.post(url, { template: entry.templateId, parameters: entry.parameters || {} })
                 .then(next, (response) => {
                     console.error(response);
                     dialogHub.closeBusyDialog();
                     $scope.$evalAsync(() => { $scope.state.isBusy = false; });
                     dialogHub.postMessage({ topic: 'projects.tree.refresh', data: { partial: true, project: location.project, workspace: location.workspace } });
                     dialogHub.showAlert({
                         title: 'Failed to generate code',
                         message: `Models were generated, but generating code from '${entry.path}' failed. See the console for details.`,
                         type: AlertTypes.Error,
                         preformatted: false,
                     });
                 });
        };
        next();
    };

    $scope.generate = () => {
        const location = fileLocation();
        $scope.state.isBusy = true;
        dialogHub.showBusyDialog('Generating model files');
        $http.post(`${GENERATE_URL}?workspace=${encodeURIComponent(location.workspace)}&project=${encodeURIComponent(location.project)}&path=${encodeURIComponent(location.path)}`)
             .then((response) => {
                 const written = (response.data.written || []).length;
                 const scrubbed = (response.data.scrubbed || []).length;
                 const plan = response.data.codeGenerations || [];
                 if (plan.length) {
                     // Models are written; now chain the model-to-code generation per the .settings recipe.
                     runCodeGenerations(location, plan, written, scrubbed);
                 } else {
                     finishGenerate(location, written, scrubbed, 0);
                 }
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

    // Bring the laid-out graph into view: layouts place cells at arbitrary (often negative)
    // coordinates, so translate the view to seat the content's top-left at the border (otherwise
    // cells fall off the left/top edge and get clipped), then size the container to the content
    // height so it shows at natural size and the pane scrolls.
    const fitIntoView = (graph, container) => {
        const cells = graph.getChildCells(graph.getDefaultParent(), true, true);
        const bbox = graph.getBoundingBoxFromGeometry(cells, true);
        if (!bbox) return;
        graph.view.setTranslate(graph.border - bbox.x, graph.border - bbox.y);
        container.style.height = `${Math.ceil(bbox.height) + 2 * graph.border}px`;
        container.scrollLeft = 0;
        container.scrollTop = 0;
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
                    // A required relation is drawn solid, an optional one dashed.
                    graph.insertEdge(parent, null, relation.name || '', byName[entity.name], byName[relation.to], edgeStyle(!relation.required));
                }
            }
            // Hierarchical (left-to-right), the same layout the processes use: it assigns layers so
            // the entity cards never overlap - unlike the organic layout, which collapsed every card
            // onto the same spot because they all start at the origin.
            const layout = new mxHierarchicalLayout(graph, mxConstants.DIRECTION_WEST);
            layout.intraCellSpacing = 40;
            layout.interRankCellSpacing = 80;
            layout.execute(parent);
        } finally {
            graph.getModel().endUpdate();
        }
        fitIntoView(graph, container);
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
                    // A non-decision step with an explicit `next` routes to that step (or `end`) instead of
                    // the next in the linear chain - mirrors BpmnIntentGenerator, so e.g. `send: { next: end }`
                    // draws send -> end, not send -> the following declared step.
                    const step = steps.find(s => byName[s.name] === source);
                    const nextArg = step && step.args && step.args['next'];
                    if (nextArg) target = vertexFor(nextArg);
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
        fitIntoView(graph, container);
    };

    // The single lifecycle event a glue binding reacts to: { kind: 'onCreate'|'onUpdate'|'onDelete',
    // entity: <Entity> }. Returns null when none is declared.
    const eventOf = (ev) => {
        if (!ev) return null;
        for (const kind of ['onCreate', 'onUpdate', 'onDelete']) {
            if (ev[kind]) return { kind, entity: ev[kind] };
        }
        return null;
    };
    const eventVerb = (kind) => ({ onCreate: 'on create', onUpdate: 'on update', onDelete: 'on delete' }[kind] || '');

    // The roll-up's parent entity is the target of its `via` to-one relation on the counted child entity.
    const rollupParent = (rollup) => {
        const child = $scope.model.entities.find(e => e && e.name === rollup.entity);
        const relation = child && (child.relations || []).find(r => r && r.name === rollup.via);
        return relation ? relation.to : null;
    };

    // Card label: an icon-badged name over a one-line binding detail (escaped; the detail is optional).
    const cardLabel = (icon, name, detail) =>
        `<div style="font-weight:bold"><i class="${icon}" style="margin-right:6px"></i>${escapeHtml(name)}</div>`
        + (detail ? `<div style="font-size:11px;opacity:.9">${escapeHtml(detail)}</div>` : '');

    // One section diagramming the artifacts that hang off the entities - authoring outputs (forms,
    // reports) and the declarative glue (notifications, schedules, integrations, inbound webhooks,
    // roll-ups). Each is an icon card edged to the entity it binds to, so the "express the integration
    // as intent" story is visible alongside the ER and process diagrams.
    const renderGlue = () => {
        const categories = [
            { list: $scope.model.forms, icon: ICON.form, color: COLOR.output, entity: f => f.forEntity, detail: () => 'form' },
            { list: $scope.model.reports, icon: ICON.report, color: COLOR.output, entity: r => r.source, detail: r => r.widget ? 'report • KPI ' + (r.widget.kind || (r.widget.value ? 'value' : 'count')) : 'report' },
            { list: $scope.model.notifications, icon: ICON.notification, color: COLOR.glue, entity: n => (eventOf(n.event) || {}).entity, detail: n => eventVerb((eventOf(n.event) || {}).kind) + ' → email' },
            { list: $scope.model.schedules, icon: ICON.schedule, color: COLOR.glue, entity: s => s.entity, detail: s => s.cron || 'scheduled' },
            { list: $scope.model.integrations, icon: ICON.integration, color: COLOR.glue, entity: i => (eventOf(i.event) || {}).entity, detail: i => (i.method || 'POST') + ' ' + eventVerb((eventOf(i.event) || {}).kind) },
            { list: $scope.model.inbound, icon: ICON.inbound, color: COLOR.glue, entity: w => w.create, detail: w => 'POST ' + (w.path || '') },
            { list: $scope.model.rollups, icon: ICON.rollup, color: COLOR.glue, entity: r => r.entity, detail: r => '→ ' + (rollupParent(r) || '?') + '.' + (r.field || '') }
        ];

        const items = [];
        for (const category of categories) {
            for (const item of (category.list || [])) {
                if (item && item.name) items.push({ category, item });
            }
        }
        if (!items.length) return;

        const graph = newSection('Glue & Outputs');
        const container = graph.container;
        const parent = graph.getDefaultParent();
        graph.getModel().beginUpdate();
        try {
            // Entity anchor nodes are created on demand so only entities actually referenced appear.
            const anchors = {};
            const anchor = (name) => {
                if (!name) return null;
                if (!anchors[name]) {
                    anchors[name] = graph.insertVertex(parent, null, `<div style="font-weight:bold">${escapeHtml(name)}</div>`, 0, 0, 130, 34, nodeStyle(COLOR.entity));
                }
                return anchors[name];
            };
            for (const { category, item } of items) {
                const card = graph.insertVertex(parent, null, cardLabel(category.icon, item.name, category.detail(item)), 0, 0, 190, 48, nodeStyle(category.color));
                const entity = anchor(category.entity(item));
                if (entity) graph.insertEdge(parent, null, '', entity, card, edgeStyle(false));
            }
            const layout = new mxHierarchicalLayout(graph, mxConstants.DIRECTION_WEST);
            layout.intraCellSpacing = 30;
            layout.interRankCellSpacing = 80;
            layout.execute(parent);
        } finally {
            graph.getModel().endUpdate();
        }
        fitIntoView(graph, container);
    };

    const render = () => {
        const host = document.getElementById('intent-diagrams');
        if (!host || typeof mxGraph === 'undefined') return;
        teardown();
        renderEntities();
        for (const process of $scope.model.processes) {
            if (process && process.name) renderProcess(process);
        }
        renderGlue();
        if (!diagrams.length) {
            const empty = document.createElement('div');
            empty.className = 'intent-diagram-empty';
            empty.textContent = 'Nothing to diagram yet - declare entities, processes or glue.';
            host.appendChild(empty);
        }
    };

    // ----- AI assistant (chat + patch preview) -----------------------------------
    // A right-hand pane where the developer asks for changes in natural language. The assistant
    // returns the COMPLETE proposed app.intent (never a re-emitted model file); we show it as a
    // Monaco diff against the current buffer and, on Accept, replace the buffer (still unsaved) so
    // the normal Save + Generate flow stays in the developer's hands - the agent never writes disk.

    // `messages` is the display list (may hold UI-only notes and errors); `turns` is the clean
    // user/assistant transcript sent upstream - the model API requires strictly alternating roles.
    $scope.chat = { open: false, busy: false, input: '', messages: [], turns: [], proposalPending: false };
    let proposedYaml = null;
    let diffEditor = null;

    $scope.toggleChat = () => { $scope.chat.open = !$scope.chat.open; };

    const scrollChatToBottom = () => {
        setTimeout(() => {
            const list = document.getElementById('intent-chat-messages');
            if (list) list.scrollTop = list.scrollHeight;
        }, 0);
    };

    const disposeDiff = () => {
        if (diffEditor) {
            const model = diffEditor.getModel();
            if (model) {
                if (model.original) model.original.dispose();
                if (model.modified) model.modified.dispose();
            }
            diffEditor.dispose();
            diffEditor = null;
        }
    };

    const showDiff = () => {
        if (!monacoApi) return;
        disposeDiff();
        const container = document.getElementById('intent-chat-diff');
        if (!container) return;
        diffEditor = monacoApi.editor.createDiffEditor(container, {
            theme: monacoThemeFor(),
            automaticLayout: true,
            readOnly: true,
            renderSideBySide: false,
            fontSize: 12,
            minimap: { enabled: false },
            scrollBeyondLastLine: false,
        });
        diffEditor.setModel({
            original: monacoApi.editor.createModel($scope.text || '', 'yaml'),
            modified: monacoApi.editor.createModel(proposedYaml || '', 'yaml'),
        });
    };

    $scope.sendChat = () => {
        const message = ($scope.chat.input || '').trim();
        if (!message || $scope.chat.busy) return;
        // Discard any still-open proposal when a new turn starts.
        $scope.rejectProposal();
        const history = $scope.chat.turns.slice();
        $scope.chat.messages.push({ role: 'user', text: message });
        $scope.chat.turns.push({ role: 'user', content: message });
        $scope.chat.input = '';
        $scope.chat.busy = true;
        scrollChatToBottom();
        $http.post(AGENT_URL, { yaml: $scope.text || '', message: message, history: history })
             .then((response) => {
                 $scope.chat.busy = false;
                 const reply = (response.data && response.data.reply) || '';
                 if (reply) {
                     $scope.chat.messages.push({ role: 'assistant', text: reply });
                     $scope.chat.turns.push({ role: 'assistant', content: reply });
                 }
                 if (response.data && response.data.proposedYaml) {
                     proposedYaml = response.data.proposedYaml;
                     $scope.chat.proposalPending = true;
                     setTimeout(showDiff, 0); // defer until ng-if renders the diff container
                 }
                 scrollChatToBottom();
             }, (response) => {
                 $scope.chat.busy = false;
                 // The turn never completed - drop its unanswered user turn so the transcript stays alternating.
                 $scope.chat.turns.pop();
                 let text = 'The AI assistant request failed. Please look at the console for more information.';
                 if (response.status === 412) {
                     text = (response.data && response.data.message) || 'The AI assistant is not configured. Set DIRIGIBLE_INTENT_AI_API_KEY.';
                 } else console.error(response);
                 $scope.chat.messages.push({ role: 'error', text: text });
                 scrollChatToBottom();
             });
    };

    $scope.onChatKey = (event) => {
        // Enter sends; Shift+Enter inserts a newline.
        if (event.key === 'Enter' && !event.shiftKey) {
            event.preventDefault();
            $scope.sendChat();
        }
    };

    $scope.acceptProposal = () => {
        if (!$scope.chat.proposalPending || proposedYaml === null) return;
        if (monacoEditor) monacoEditor.setValue(proposedYaml); // fires onDidChangeModelContent -> $scope.text, dirty, re-parse
        else { $scope.text = proposedYaml; handleTextChanged(); }
        $scope.chat.messages.push({ role: 'assistant', text: 'Applied to the editor. Review, then Save and Generate.' });
        $scope.rejectProposal();
        scrollChatToBottom();
    };

    $scope.rejectProposal = () => {
        proposedYaml = null;
        $scope.chat.proposalPending = false;
        disposeDiff();
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

    $scope.$on('$destroy', () => {
        teardown();
        disposeDiff();
        if (revealObserver) {
            revealObserver.disconnect();
            revealObserver = null;
        }
        if (monacoEditor) {
            monacoEditor.dispose();
            monacoEditor = null;
        }
    });

    $scope.dataParameters = ViewParameters.get();
    if (!$scope.dataParameters.hasOwnProperty('filePath')) {
        $scope.state.error = true;
        $scope.errorMessage = 'The \'filePath\' data parameter is missing.';
        $scope.state.isBusy = false;
    } else loadFileContents();
});
