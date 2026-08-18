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
 * Intent diagrams - the read-only mxGraph rendering of a parsed intent model.
 *
 * Framework-free on purpose: it takes a parsed model (the `/services/ide/intent/parse` response)
 * and a host element, and owns nothing else. The AngularJS Intent Editor and the Harmonia Builder
 * shell both render the same diagrams through it - the editor by relative path, the shell by the
 * absolute `/services/web/editor-intent/js/intent-diagrams.js` URL (the established shared-runtime
 * pattern).
 *
 * Requires mxGraph (`mxClient.js`) to be loaded; `render` is a no-op without it. The host is
 * expected to carry the `.intent-diagram*` styles - both consumers define them.
 *
 *   IntentDiagrams.render(model, hostElement);   // teardown + entities ER + a flowchart per
 *                                                // process + the Glue & Outputs section
 *   IntentDiagrams.dispose(hostElement);         // explicit teardown (destroy the live graphs)
 */
window.IntentDiagrams = (() => {

    // ----- Diagram palette -----------------------------------------------------
    // The diagram is drawn with mxGraph (the same engine the EDM and BPMN modelers use), so it inherits
    // their robust rendering instead of Mermaid's brittle theming. Colours are fixed brand tones that
    // read equally well on the light and dark themes - solid fills with white labels on a transparent
    // canvas, exactly like the schema/entity modelers - so the diagram looks identical in either theme
    // and needs no recolour on a theme switch. Values mirror editor-entity/css/styles.css.
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
    // font; the diagram renders HTML labels, so an <i class="sap-icon--..."> draws the glyph and
    // inherits the label colour - monochrome and theme-consistent, not decorative emoji. The font
    // comes from platform-core's fonts.css, which every consumer must load.
    const ICON = {
        form: 'sap-icon--form',
        report: 'sap-icon--bar-chart',
        notification: 'sap-icon--email',
        schedule: 'sap-icon--date-time',
        integration: 'sap-icon--chain-link',
        inbound: 'sap-icon--inbox',
        rollup: 'sap-icon--sum'
    };

    const escapeHtml = (s) => String(s == null ? '' : s).replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));

    // The live mxGraph instances per host element, destroyed before each re-render. Keyed off the
    // host (rather than a module-level array) so several hosts can render independently.
    const live = new Map();

    const dispose = (host) => {
        const graphs = live.get(host);
        if (graphs) {
            while (graphs.length) graphs.pop().destroy();
            live.delete(host);
        }
        if (host) host.innerHTML = '';
    };

    // A read-only graph wired into a freshly created, titled container appended to the diagram host.
    const sectionFactory = (host, graphs) => (title) => {
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
        graphs.push(graph);
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

    const renderEntities = (model, newSection) => {
        const entities = model.entities.filter(e => e && e.name);
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
    const renderProcess = (process, newSection) => {
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
                // A wait parks the process on an entity event (a message catch event in the BPMN) - an
                // ellipse like the terminals, but glue-rust so the event-driven resume stands out.
                else if (step.kind === 'wait') byName[step.name] = graph.insertVertex(parent, null, step.name, 0, 0, 130, 44, shapeStyle('ellipse', COLOR.glue));
                // A parallel fork is a gateway (diamond) that fans to its branches and joins before `next`.
                else if (step.kind === 'parallel') byName[step.name] = graph.insertVertex(parent, null, step.name, 0, 0, 90, 60, shapeStyle('rhombus', COLOR.decision));
                else byName[step.name] = graph.insertVertex(parent, null, step.name, 0, 0, 140, 44, nodeStyle(COLOR.entity));
            }
            // Parallel fork/join (mirrors BpmnIntentGenerator + ProcessParallelSupport): a branch is a
            // CHAIN - everything reachable from the branch step through its own routing, a nested fork
            // included - and the whole branch region is OFF the linear chain. Inside a region a step
            // routes explicitly or, declaring no routing, joins; the literal `join` converges on the
            // enclosing join gateway.
            const forks = steps.filter(s => s.kind === 'parallel').map(s => ({
                id: s.name, branches: ((s.args || {}).branches) || [], next: (s.args || {}).next, joinId: s.name + 'Join'
            }));
            const joinVertex = {};
            for (const f of forks) {
                joinVertex[f.joinId] = graph.insertVertex(parent, null, '', 0, 0, 50, 50, shapeStyle('rhombus', COLOR.decision));
            }
            const stepByName = {};
            for (const s of steps) stepByName[s.name] = s;
            const routingTargets = (step) => {
                const args = step.args || {};
                const targets = step.kind === 'decision' ? [args['then'], args['else']] : [args['next']];
                for (const timer of ['timeout', 'expire']) {
                    const t = args[timer];
                    if (t && typeof t === 'object' && t.then) targets.push(t.then);
                }
                if (args['onError']) targets.push(args['onError']);
                return targets.filter(t => t !== undefined && t !== null && String(t) !== '');
            };
            // Step name -> the innermost enclosing join id. A nested fork is claimed by the enclosing
            // branch and the walk resumes at its `next`; its own branches are claimed by its own walk.
            const joinOf = {};
            for (const f of forks) {
                for (const head of f.branches) {
                    const pending = [head];
                    while (pending.length) {
                        const name = String(pending.shift());
                        const step = stepByName[name];
                        if (!step || joinOf[name]) continue;
                        joinOf[name] = f.joinId;
                        pending.push(...routingTargets(step));
                    }
                }
            }
            const vertexFor = (name, enclosingJoin) => {
                const id = name === undefined || name === null ? '' : String(name);
                if (id === '' || id.toLowerCase() === 'join') return enclosingJoin ? joinVertex[enclosingJoin] : end;
                if (id.toLowerCase() === 'end') return end;
                return byName[id] || end;
            };

            const chain = [start];
            for (const step of steps) {
                if (joinOf[step.name]) continue; // a branch region is off the linear chain
                const v = String(step.kind).toLowerCase() === 'end' ? end : byName[step.name];
                if (chain[chain.length - 1] !== v) chain.push(v);
            }
            if (chain[chain.length - 1] !== end) chain.push(end);

            for (let i = 0; i < chain.length - 1; i++) {
                const source = chain[i];
                let target = chain[i + 1];
                // A parallel fork fans to its branches and joins before `next` - no linear fall-through
                // (the fork/join edges are drawn for every fork below, nested ones included).
                if (forks.some(f => byName[f.id] === source)) continue;
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

            // Every fork fans to its branch chains; its join flows on to `next` (a nested fork with no
            // `next` joins into its own enclosing join).
            for (const f of forks) {
                for (const b of f.branches) {
                    graph.insertEdge(parent, null, '', byName[f.id] || start, vertexFor(b), edgeStyle(false));
                }
                graph.insertEdge(parent, null, '', joinVertex[f.joinId], vertexFor(f.next, joinOf[f.id]), edgeStyle(false));
            }
            // The routing inside the branch regions - explicit, or into the join.
            for (const name of Object.keys(joinOf)) {
                const step = stepByName[name];
                const source = byName[name];
                // A nested fork's outgoing edge belongs to its join, drawn above.
                if (!step || !source || step.kind === 'parallel') continue;
                const join = joinOf[name];
                const args = step.args || {};
                if (step.kind === 'decision') {
                    graph.insertEdge(parent, null, '', source, vertexFor(args['else'], join), edgeStyle(true));
                    if (args['if'] && args['then']) {
                        graph.insertEdge(parent, null, String(args['if']), source, vertexFor(args['then'], join), edgeStyle(false));
                    }
                } else {
                    graph.insertEdge(parent, null, '', source, vertexFor(args['next'], join), edgeStyle(false));
                }
            }

            // Boundary timers on a user task (timeout: non-cancelling reminder, expire: cancelling
            // date-driven expiry) draw as dashed labelled edges from the task to their `then` branch -
            // mirrors the boundaryEvent + timerEventDefinition BpmnIntentGenerator emits.
            for (const step of steps) {
                if (step.kind !== 'userTask' || !step.args || !byName[step.name]) continue;
                const timeout = step.args['timeout'];
                if (timeout && typeof timeout === 'object' && timeout.then) {
                    graph.insertEdge(parent, null, 'timeout ' + String(timeout.after || ''), byName[step.name], vertexFor(timeout.then, joinOf[step.name]), edgeStyle(true));
                }
                const expire = step.args['expire'];
                if (expire && typeof expire === 'object' && expire.then) {
                    graph.insertEdge(parent, null, 'expires ' + String(expire.until || ''), byName[step.name], vertexFor(expire.then, joinOf[step.name]), edgeStyle(true));
                }
            }

            // onError on a delegate service task draws as a dashed labelled edge to its error route -
            // mirrors the error boundary event BpmnIntentGenerator emits; a declared retry cycle
            // annotates the label so the diagram says how many attempts precede the route.
            for (const step of steps) {
                if (!step.args || !step.args['onError'] || !byName[step.name]) continue;
                const retry = step.args['retry'];
                const label = retry && typeof retry === 'object' && retry.count !== undefined && retry.count !== null
                    ? 'on error (after ' + String(retry.count) + ' retries)' : 'on error';
                graph.insertEdge(parent, null, label, byName[step.name], vertexFor(step.args['onError'], joinOf[step.name]), edgeStyle(true));
            }

            // abortOn: a transition into a listed status cancels the whole in-flight process (an
            // interrupting event subprocess in the BPMN). Draw a glue-rust "abort" node with a dashed
            // edge from start (the whole flow is under the abort's watch) to a terminate marker or the
            // optional cleanup step - mirrors the emitted event subprocess.
            const abortOn = process.abortOn;
            if (abortOn && (abortOn.status !== undefined && abortOn.status !== null)) {
                const statuses = Array.isArray(abortOn.status) ? abortOn.status : [abortOn.status];
                const abort = graph.insertVertex(parent, null, 'abort on status ' + statuses.join(', '), 0, 0, 150, 44, shapeStyle('ellipse', COLOR.glue));
                graph.insertEdge(parent, null, 'transitioned', start, abort, edgeStyle(true));
                const then = abortOn.then && String(abortOn.then).toLowerCase() !== 'end' ? abortOn.then : null;
                graph.insertEdge(parent, null, then ? 'cleanup' : 'terminate', abort, then ? vertexFor(then) : end, edgeStyle(true));
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

    // The single event a glue binding reacts to, on either axis: an entity lifecycle event
    // ({ kind: 'onCreate'|'onUpdate'|'onDelete', entity: <Entity> }) or a process step event
    // ({ kind: 'onStepReached'|'onStepCompleted', process, step }). Returns null when none is declared.
    const eventOf = (ev) => {
        if (!ev) return null;
        for (const kind of ['onCreate', 'onUpdate', 'onDelete']) {
            if (ev[kind]) return { kind, entity: ev[kind] };
        }
        for (const kind of ['onStepReached', 'onStepCompleted']) {
            const at = ev[kind];
            if (at && at.process && at.step) return { kind, process: at.process, step: at.step };
        }
        return null;
    };
    const eventVerb = (ev) => {
        const event = eventOf(ev);
        if (!event) return '';
        if (event.process) return 'on ' + event.step + (event.kind === 'onStepCompleted' ? ' completed' : ' reached');
        return { onCreate: 'on create', onUpdate: 'on update', onDelete: 'on delete' }[event.kind] || '';
    };

    // The entity a glue binding is about: the one a lifecycle event names, or the trigger entity of the
    // process a step event names - the record that process runs on, which is what the action addresses.
    const eventEntity = (model, ev) => {
        const event = eventOf(ev);
        if (!event) return null;
        if (event.entity) return event.entity;
        const process = (model.processes || []).find(p => p && p.name === event.process);
        const trigger = (process && process.trigger) || {};
        return trigger.onCreate || trigger.onUpdate || trigger.onDelete || null;
    };

    // Where an inbound ingest arrives from: its HTTP path, or its message/file source.
    const inboundDetail = (ingest) => {
        const source = ingest.source || {};
        if (source.queue) return 'queue ' + source.queue;
        if (source.topic) return 'topic ' + source.topic;
        if (source.folder) return 'folder ' + source.folder;
        return 'POST ' + (ingest.path || '');
    };

    // A declared payload is a contract, so the card says so - the alternative (the record as stored)
    // is a different promise entirely and must not look the same on the diagram.
    const integrationDetail = (integration) => {
        const declared = Object.keys(integration.payload || {}).length;
        return (integration.method || 'POST') + ' ' + eventVerb(integration.event) + (declared ? ' • payload' : '');
    };

    // The roll-up's parent entity is the target of its `via` to-one relation on the counted child entity.
    const rollupParent = (model, rollup) => {
        const child = model.entities.find(e => e && e.name === rollup.entity);
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
    const renderGlue = (model, newSection) => {
        const categories = [
            { list: model.forms, icon: ICON.form, color: COLOR.output, entity: f => f.forEntity, detail: () => 'form' },
            { list: model.reports, icon: ICON.report, color: COLOR.output, entity: r => r.source, detail: r => r.widget ? 'report • KPI ' + (r.widget.kind || (r.widget.value ? 'value' : 'count')) : 'report' },
            { list: model.notifications, icon: ICON.notification, color: COLOR.glue, entity: n => eventEntity(model, n.event), detail: n => eventVerb(n.event) + ' → email' },
            { list: model.schedules, icon: ICON.schedule, color: COLOR.glue, entity: s => s.model ? null : s.entity, detail: s => (s.model ? s.model + '.' + s.entity + ' • ' : '') + (s.cron || 'scheduled') },
            { list: model.integrations, icon: ICON.integration, color: COLOR.glue, entity: i => eventEntity(model, i.event), detail: integrationDetail },
            { list: model.inbound, icon: ICON.inbound, color: COLOR.glue, entity: w => w.create, detail: inboundDetail },
            { list: model.rollups, icon: ICON.rollup, color: COLOR.glue, entity: r => r.entity, detail: r => '→ ' + (rollupParent(model, r) || '?') + '.' + (r.field || '') }
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

    /**
     * Fill every collection the renderers walk, so a partial model (or one whose empty collections
     * were omitted) renders instead of throwing.
     */
    const normalize = (model) => {
        model = model || {};
        for (const key of ['entities', 'processes', 'forms', 'reports', 'permissions', 'seeds',
            'notifications', 'schedules', 'integrations', 'inbound', 'rollups']) {
            model[key] = model[key] || [];
        }
        return model;
    };

    /**
     * Render the whole intent as diagrams into `host`, replacing whatever was there before.
     * A model with nothing to diagram leaves an explanatory placeholder.
     */
    const render = (model, host) => {
        if (!host || typeof mxGraph === 'undefined') return;
        dispose(host);
        const graphs = [];
        live.set(host, graphs);
        const newSection = sectionFactory(host, graphs);
        const normalized = normalize(model);
        renderEntities(normalized, newSection);
        for (const process of normalized.processes) {
            if (process && process.name) renderProcess(process, newSection);
        }
        renderGlue(normalized, newSection);
        if (!graphs.length) {
            live.delete(host);
            const empty = document.createElement('div');
            empty.className = 'intent-diagram-empty';
            empty.textContent = 'Nothing to diagram yet - declare entities, processes or glue.';
            host.appendChild(empty);
        }
    };

    return { render, dispose, normalize };
})();
