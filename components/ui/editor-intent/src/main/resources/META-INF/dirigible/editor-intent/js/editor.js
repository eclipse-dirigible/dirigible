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
editorView.controller('IntentEditorController', ($scope, $http, $sce, ViewParameters, WorkspaceService) => {
    const statusBarHub = new StatusBarHub();
    const workspaceHub = new WorkspaceHub();
    const layoutHub = new LayoutHub();
    const dialogHub = new DialogHub();
    const themeHub = new ThemingHub();
    const PARSE_URL = '/services/ide/intent/parse';
    const GENERATE_URL = '/services/ide/intent/generate';

    $scope.state = { isBusy: true, error: false };
    $scope.errorMessage = '';
    $scope.changed = false;
    $scope.text = '';
    $scope.model = { entities: [], processes: [], forms: [], reports: [], permissions: [], seeds: [] };
    $scope.issues = [];
    $scope.diagramSvg = '';
    let savedText = '';
    let parseTimer = null;
    let renderCounter = 0;

    // ----- Mermaid theming -----------------------------------------------------
    // Drive Mermaid's colors from the live BlimpKit theme so the diagram tracks the IDE light/dark
    // switch. The two anchors that guarantee contrast are the theme foreground and background; lines,
    // borders and text all use the foreground (the faint divider/surface tokens are deliberately
    // low-contrast and made the diagram nearly invisible), and node fills are the foreground blended
    // into the background at a low ratio so panels read as distinct without washing out their text.

    // Resolve a CSS color expression (incl. nested var()/named colors) to a concrete "rgb(...)" via a
    // probe element - getComputedStyle on a raw custom property returns the unresolved authored value.
    const probe = document.createElement('span');
    probe.style.display = 'none';
    document.body.appendChild(probe);
    const resolveColor = (expr) => {
        probe.style.color = '';
        probe.style.color = expr;
        return getComputedStyle(probe).color || 'rgb(0, 0, 0)';
    };

    const rgbOf = (color) => {
        const m = color.match(/\d+(\.\d+)?/g);
        return m && m.length >= 3 ? [Number(m[0]), Number(m[1]), Number(m[2])] : [0, 0, 0];
    };

    const luminance = (rgb) => (0.299 * rgb[0] + 0.587 * rgb[1] + 0.114 * rgb[2]) / 255;

    const mix = (a, b, ratio) => {
        const r = Math.round(a[0] * ratio + b[0] * (1 - ratio));
        const g = Math.round(a[1] * ratio + b[1] * (1 - ratio));
        const bl = Math.round(a[2] * ratio + b[2] * (1 - ratio));
        return `rgb(${r}, ${g}, ${bl})`;
    };

    const applyMermaidTheme = () => {
        if (!window.mermaid || typeof window.mermaid.initialize !== 'function') return;
        const fg = rgbOf(resolveColor('var(--sapTextColor, var(--foreground, #1d2d3e))'));
        const bg = rgbOf(resolveColor('var(--sapBackgroundColor, var(--background, #ffffff))'));
        const fgCss = `rgb(${fg[0]}, ${fg[1]}, ${fg[2]})`;
        const bgCss = `rgb(${bg[0]}, ${bg[1]}, ${bg[2]})`;
        const nodeFill = mix(fg, bg, 0.10); // faint foreground tint - distinct panel, still light/dark-correct
        const altRow = mix(fg, bg, 0.05);
        window.mermaid.initialize({
            startOnLoad: false,
            securityLevel: 'strict',
            theme: 'base',
            themeVariables: {
                darkMode: luminance(bg) < 0.5,
                background: bgCss,
                // High-contrast structure: lines / borders / text all use the theme foreground.
                lineColor: fgCss,
                textColor: fgCss,
                primaryTextColor: fgCss,
                secondaryTextColor: fgCss,
                tertiaryTextColor: fgCss,
                nodeTextColor: fgCss,
                titleColor: fgCss,
                primaryBorderColor: fgCss,
                secondaryBorderColor: fgCss,
                tertiaryBorderColor: fgCss,
                nodeBorder: fgCss,
                clusterBorder: fgCss,
                // Surfaces: foreground tinted into the background so panels are visible in both themes.
                primaryColor: nodeFill,
                secondaryColor: altRow,
                tertiaryColor: bgCss,
                mainBkg: nodeFill,
                clusterBkg: bgCss,
                edgeLabelBackground: bgCss,
                attributeBackgroundColorOdd: nodeFill,
                attributeBackgroundColorEven: bgCss
            },
            // themeVariables alone do not reliably reach the ER relationship lines and flowchart edge
            // strokes/arrowheads (mermaid derives those from internal CSS), so force them to the theme
            // foreground - this is what makes the connector lines visible in dark mode.
            themeCSS: `
                .edgePath .path, .flowchart-link, path.relation, .relationshipLine, line.relationshipLine { stroke: ${fgCss} !important; }
                .arrowheadPath, marker path, defs marker path, #arrowhead path { fill: ${fgCss} !important; stroke: ${fgCss} !important; }
                .node rect, .node circle, .node ellipse, .node polygon, .node path, .er.entityBox, .cluster rect, .statediagram-state rect { stroke: ${fgCss} !important; }
                .nodeLabel, .edgeLabel, .label, .er.entityLabel, .er.relationshipLabel, .titleText, text { fill: ${fgCss} !important; color: ${fgCss} !important; }
                .edgeLabel rect, .edgeLabel .label-container { background-color: ${bgCss} !important; fill: ${bgCss} !important; }
            `
        });
    };

    applyMermaidTheme();

    themeHub.onThemeChange(() => {
        // The theme swaps its <link> stylesheets asynchronously; recompute and re-render once the new
        // CSS variables have taken effect.
        setTimeout(() => {
            applyMermaidTheme();
            render();
        }, 300);
    });

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

    // ----- Diagram rendering -----------------------------------------------------

    const render = async () => {
        if (!window.mermaid || typeof window.mermaid.render !== 'function') {
            $scope.diagramSvg = $sce.trustAsHtml('<em>Mermaid not loaded.</em>');
            return;
        }
        const sections = [];
        const erSpec = toErDiagram($scope.model);
        if (erSpec) sections.push({ title: 'Entities', spec: erSpec });
        for (const process of $scope.model.processes) {
            if (!process || !process.name) continue;
            sections.push({ title: 'Process: ' + process.name, spec: toFlowchart(process) });
        }
        if (!sections.length) {
            $scope.diagramSvg = $sce.trustAsHtml('<em>Nothing to diagram yet - declare entities or processes.</em>');
            return;
        }
        const generation = ++renderCounter;
        // Render sequentially: mermaid.render shares global parser/DOM state and is not safe to call
        // concurrently (Promise.all here produced "Syntax error in text" diagrams, especially right
        // after a theme-change re-initialize).
        const rendered = [];
        for (let index = 0; index < sections.length; index++) {
            const section = sections[index];
            const header = `<h4 class="intent-section-title">${escapeHtml(section.title)}</h4>`;
            try {
                const result = await window.mermaid.render(`intent-editor-svg-${generation}-${index}`, section.spec);
                rendered.push(header + result.svg);
            } catch (err) {
                rendered.push(`${header}<em>Render failed: ${escapeHtml(err && err.message ? err.message : String(err))}</em>`);
            }
            if (generation !== renderCounter) return; // a newer render superseded this one
        }
        $scope.diagramSvg = $sce.trustAsHtml(rendered.join(''));
        $scope.$applyAsync();
    };

    const escapeHtml = (s) => String(s).replace(/[&<>'"]/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', "'": '&#39;', '"': '&quot;' }[c]));

    const safeName = (s) => String(s || 'UNNAMED').replace(/[^A-Za-z0-9_]/g, '_');

    // Composition (required to-one) renders as an identifying (solid) relationship,
    // association as non-identifying (dashed) - mirroring the EDM generator semantics.
    const cardinality = (kind, required) => {
        const line = required ? '--' : '..';
        switch (kind) {
            case 'oneToMany': return '||' + line + 'o{';
            case 'manyToOne': return '}o' + line + '||';
            case 'oneToOne': return '||' + line + '||';
            case 'manyToMany': return '}o' + line + 'o{';
            default: return '||' + line + 'o{';
        }
    };

    const toErDiagram = (model) => {
        const entities = model.entities.filter(e => e && e.name);
        if (!entities.length) return null;
        const lines = ['erDiagram'];
        for (const entity of entities) {
            lines.push('    ' + safeName(entity.name) + ' {');
            for (const field of (entity.fields || [])) {
                if (!field || !field.name) continue;
                const type = (field.type || 'string').replace(/[^A-Za-z0-9_]/g, '_');
                const flag = field.primaryKey ? ' PK' : '';
                lines.push('        ' + type + ' ' + safeName(field.name) + flag);
            }
            lines.push('    }');
        }
        for (const entity of entities) {
            for (const relation of (entity.relations || [])) {
                if (!relation || !relation.to || !relation.name) continue;
                lines.push('    ' + safeName(entity.name) + ' ' + cardinality(relation.kind, relation.required)
                    + ' ' + safeName(relation.to) + ' : "' + relation.name + '"');
            }
        }
        return lines.join('\n');
    };

    // Mirrors BpmnIntentGenerator: a linear chain through the declared steps; decisions
    // emit a labeled conditioned edge to `then` and route their default edge to `else`
    // (falling back to the next step); `end`-kind steps collapse into the end node.
    const toFlowchart = (process) => {
        const steps = (process.steps || []).filter(s => s && s.name);
        const effectiveId = (name) => {
            if (String(name).toLowerCase() === 'end') return 'END';
            const step = steps.find(s => s.name === name);
            return (step && String(step.kind).toLowerCase() === 'end') ? 'END' : safeName(name);
        };
        const lines = ['flowchart TD', '    START((start))', '    END((end))'];
        for (const step of steps) {
            if (String(step.kind).toLowerCase() === 'end') continue;
            const id = safeName(step.name);
            const text = '"' + step.name.replace(/"/g, "'") + '"';
            if (step.kind === 'decision') lines.push('    ' + id + '{' + text + '}');
            else if (step.kind === 'serviceTask' || step.kind === 'script') lines.push('    ' + id + '[[' + text + ']]');
            else lines.push('    ' + id + '[' + text + ']');
        }
        const chain = ['START'];
        for (const step of steps) {
            const id = String(step.kind).toLowerCase() === 'end' ? 'END' : safeName(step.name);
            if (chain[chain.length - 1] !== id) chain.push(id);
        }
        if (chain[chain.length - 1] !== 'END') chain.push('END');
        for (let i = 0; i < chain.length - 1; i++) {
            const source = chain[i];
            let target = chain[i + 1];
            const decision = steps.find(s => safeName(s.name) === source && s.kind === 'decision');
            if (decision) {
                const args = decision.args || {};
                if (args['else']) target = effectiveId(args['else']);
                lines.push('    ' + source + ' -.-> ' + target);
                if (args['if'] && args['then']) {
                    lines.push('    ' + source + ' -- "' + String(args['if']).replace(/"/g, "'") + '" --> ' + effectiveId(args['then']));
                }
            } else {
                lines.push('    ' + source + ' --> ' + target);
            }
        }
        return lines.join('\n');
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

    $scope.dataParameters = ViewParameters.get();
    if (!$scope.dataParameters.hasOwnProperty('filePath')) {
        $scope.state.error = true;
        $scope.errorMessage = 'The \'filePath\' data parameter is missing.';
        $scope.state.isBusy = false;
    } else loadFileContents();
});
