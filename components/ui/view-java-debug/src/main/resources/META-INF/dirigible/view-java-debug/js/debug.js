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
const javaDebugApp = angular.module('javaDebug', ['blimpKit', 'platformView']);
javaDebugApp.constant('Layout', new LayoutHub());
javaDebugApp.controller('JavaDebugController', ($scope, $timeout, $interval, Layout) => {
    const themingHub = new ThemingHub();

    $scope.sections = {
        callStack: true,
        variables: true,
        breakpoints: true,
    };

    $scope.status = 'disconnected'; // disconnected | connecting | connected | error
    $scope.callStack = [];
    $scope.variables = [];
    $scope.selectedFrame = null;

    // -------------------------------------------------------------------------
    // Workspace — read from platform localStorage, kept in sync via hub event
    // -------------------------------------------------------------------------

    let currentWorkspace = (function () {
        try {
            const prefix = getBrandingInfo().prefix;
            return localStorage.getItem(`${prefix}.workspace.selected`) || 'workspace';
        } catch (e) {
            return 'workspace';
        }
    })();

    themingHub.addMessageListener({
        topic: 'platform.workspace.changed',
        handler: (msg) => {
            if (msg && msg.workspace) {
                currentWorkspace = msg.workspace;
            }
        },
    });

    // -------------------------------------------------------------------------
    // Port — fetched from backend (DirigibleConfig.JAVA_DEBUG_JDWP_PORT)
    // -------------------------------------------------------------------------

    let jdwpPort = 8000;
    fetch('/services/ide/java-debug/config')
        .then(r => r.json())
        .then(data => { if (data.jdwpPort) jdwpPort = data.jdwpPort; })
        .catch(() => { /* keep default 8000 */ });

    // -------------------------------------------------------------------------
    // Connecting animation
    // -------------------------------------------------------------------------

    const CONNECTING_MESSAGES = [
        'Connecting...',
        'Bootstrapping JDT Language Server...',
        'Resolving workspace classpath...',
        'Parsing project sources...',
        'Negotiating DAP handshake...',
        'Attaching to JVM...',
        'Opening JDWP tunnel...',
        'Loading debug symbols...',
        'Resolving source maps...',
        'Scanning annotations...',
        'Compiling hot paths...',
        'Inspecting bytecode...',
        'Generating AST cache...',
        'Waiting for debugger transport...',
        'Poking the JDWP port...',
        'Spinning up debug adapter...',
        'Injecting breakpoints...',
        'Evaluating launch configuration...',
    ];
    let connectingMsgIdx = 0;
    let connectingInterval = null;

    function startConnectingAnimation() {
        connectingMsgIdx = 0;
        if (connectingInterval) $interval.cancel(connectingInterval);
        connectingInterval = $interval(() => {
            connectingMsgIdx = (connectingMsgIdx + 1) % CONNECTING_MESSAGES.length;
        }, 3000);
    }

    function stopConnectingAnimation() {
        if (connectingInterval) {
            $interval.cancel(connectingInterval);
            connectingInterval = null;
        }
    }

    $scope.$on('$destroy', stopConnectingAnimation);

    $scope.statusLabel = () => {
        switch ($scope.status) {
            case 'connected': return 'Connected';
            case 'connecting': return CONNECTING_MESSAGES[connectingMsgIdx];
            case 'error': return 'Error';
            default: return 'Disconnected';
        }
    };

    $scope.getStatusType = () => {
        switch ($scope.status) {
            case 'connected': return 'positive';
            case 'connecting': return 'informative';
            case 'error': return 'negative';
            default: return '';
        }
    };

    $scope.getStatusGlyph = () => {
        switch ($scope.status) {
            case 'connected': return 'sap-icon--message-success';
            case 'error': return 'sap-icon--message-error';
            default: return '';
        }
    };

    // -------------------------------------------------------------------------
    // Breakpoints — persist to / restore from localStorage
    // -------------------------------------------------------------------------

    const breakpoints = {};
    $scope.bpList = [];
    let ws = null;
    let dapSeq = 1;

    const STORAGE_KEY = 'dirigible.java.debug.breakpoints';

    function saveBreakpoints() {
        try {
            localStorage.setItem(STORAGE_KEY, JSON.stringify(breakpoints));
        } catch (e) { /* storage unavailable */ }
    }

    (function loadBreakpoints() {
        try {
            const stored = localStorage.getItem(STORAGE_KEY);
            if (stored) {
                const parsed = JSON.parse(stored);
                Object.assign(breakpoints, parsed);
                refreshBpList();
            }
        } catch (e) { /* corrupted storage — start fresh */ }
    })();

    function refreshBpList() {
        const list = [];
        for (const [file, lines] of Object.entries(breakpoints)) {
            for (const line of lines) {
                list.push({ file: file.split('/').pop(), line, fullPath: file });
            }
        }
        $scope.bpList = list;
    }

    $scope.isPaused = () => $scope.callStack.length > 0;

    $scope.removeBreakpoint = (bp) => {
        if (breakpoints[bp.fullPath]) {
            breakpoints[bp.fullPath] = breakpoints[bp.fullPath].filter(l => l !== bp.line);
            if (breakpoints[bp.fullPath].length === 0) {
                delete breakpoints[bp.fullPath];
            }
            refreshBpList();
            saveBreakpoints();
            syncBreakpointsForFile(bp.fullPath);
            broadcastBreakpoints();
        }
    };

    $scope.clearAllBreakpoints = () => {
        const paths = Object.keys(breakpoints);
        paths.forEach(path => {
            delete breakpoints[path];
            if ($scope.status === 'connected') {
                syncBreakpointsForFile(path, []);
            }
        });
        refreshBpList();
        saveBreakpoints();
        broadcastBreakpoints();
    };

    $scope.attach = () => {
        if (ws) ws.close();
        $scope.status = 'connecting';
        startConnectingAnimation();
        const wsUrl = `ws://${location.host}/websockets/ide/java-debug?workspace=${encodeURIComponent(currentWorkspace)}`;
        ws = new WebSocket(wsUrl);
        ws.onopen = () => {
            sendDap('initialize', {
                clientID: 'dirigible',
                clientName: 'Eclipse Dirigible',
                adapterID: 'java',
                locale: 'en-US',
                linesStartAt1: true,
                columnsStartAt1: true,
                pathFormat: 'path',
            });
        };
        ws.onmessage = (event) => {
            try {
                handleDapMessage(JSON.parse(event.data));
            } catch (e) {
                console.error('[java-debug] Failed to parse DAP message', e, event.data);
            }
        };
        ws.onerror = () => {
            stopConnectingAnimation();
            $scope.$evalAsync(() => { $scope.status = 'error'; });
        };
        ws.onclose = () => {
            stopConnectingAnimation();
            $scope.$evalAsync(() => {
                if ($scope.status !== 'disconnected') {
                    $scope.status = 'disconnected';
                }
                $scope.callStack = [];
                $scope.variables = [];
                highlightLine(null, 0);
            });
        };
    };

    $scope.disconnect = () => {
        if (ws) {
            sendDap('disconnect', { restart: false });
            ws.close();
            ws = null;
        }
        stopConnectingAnimation();
        $scope.status = 'disconnected';
        $scope.callStack = [];
        $scope.variables = [];
        highlightLine(null, 0);
    };

    function clearPausedState() {
        $scope.callStack = [];
        $scope.variables = [];
        $scope.selectedFrame = null;
        highlightLine(null, 0);
    }

    $scope.dapContinue = () => {
        clearPausedState();
        sendDap('continue', { threadId: currentThreadId() });
    };
    $scope.stepOver = () => sendDap('next', { threadId: currentThreadId() });
    $scope.stepIn = () => sendDap('stepIn', { threadId: currentThreadId() });
    $scope.stepOut = () => sendDap('stepOut', { threadId: currentThreadId() });
    $scope.dapStop = () => sendDap('terminate', {});

    $scope.selectFrame = (frame) => {
        $scope.selectedFrame = frame;
        loadVariablesForFrame(frame);
        if (frame.source && frame.source.path) {
            highlightLine(frame.source.path, frame.line);
        }
    };

    // -------------------------------------------------------------------------
    // DAP message handling
    // -------------------------------------------------------------------------

    let _stoppedThreadId = null;

    function currentThreadId() {
        return _stoppedThreadId || 1;
    }

    function handleDapMessage(msg) {
        if (msg.type === 'response') {
            handleResponse(msg);
        } else if (msg.type === 'event') {
            handleEvent(msg);
        }
    }

    function handleResponse(msg) {
        if (!msg.success) {
            console.warn('[java-debug] DAP error response for', msg.command, msg.message);
        }
        switch (msg.command) {
            case 'initialize':
                stopConnectingAnimation();
                $scope.$evalAsync(() => { $scope.status = 'connected'; });
                sendDap('attach', { hostName: 'localhost', port: jdwpPort });
                sendAllBreakpoints();
                break;
            case 'stackTrace':
                if (msg.body && msg.body.stackFrames) {
                    $scope.$evalAsync(() => {
                        $scope.callStack = msg.body.stackFrames;
                        if ($scope.callStack.length > 0) {
                            $scope.selectFrame($scope.callStack[0]);
                        }
                    });
                }
                break;
            case 'scopes':
                if (msg.body && msg.body.scopes && msg.body.scopes.length > 0) {
                    sendDap('variables', { variablesReference: msg.body.scopes[0].variablesReference });
                }
                break;
            case 'variables':
                if (msg.body && msg.body.variables) {
                    $scope.$evalAsync(() => { $scope.variables = msg.body.variables; });
                }
                break;
        }
    }

    function handleEvent(msg) {
        switch (msg.event) {
            case 'initialized':
                sendAllBreakpoints();
                sendDap('configurationDone', {});
                break;
            case 'stopped':
                _stoppedThreadId = msg.body && msg.body.threadId;
                $scope.$evalAsync(() => { $scope.callStack = []; $scope.variables = []; });
                sendDap('stackTrace', { threadId: _stoppedThreadId, startFrame: 0, levels: 20 });
                break;
            case 'continued':
                $scope.$evalAsync(() => { $scope.callStack = []; $scope.variables = []; });
                highlightLine(null, 0);
                break;
            case 'terminated':
            case 'exited':
                stopConnectingAnimation();
                $scope.$evalAsync(() => {
                    $scope.status = 'disconnected';
                    $scope.callStack = [];
                    $scope.variables = [];
                });
                highlightLine(null, 0);
                break;
        }
    }

    // -------------------------------------------------------------------------
    // DAP helpers
    // -------------------------------------------------------------------------

    function sendDap(command, args) {
        if (!ws || ws.readyState !== WebSocket.OPEN) return;
        const msg = { type: 'request', seq: dapSeq++, command, arguments: args };
        ws.send(JSON.stringify(msg));
    }

    function loadVariablesForFrame(frame) {
        sendDap('scopes', { frameId: frame.id });
    }

    function sendAllBreakpoints() {
        for (const [path, lines] of Object.entries(breakpoints)) {
            syncBreakpointsForFile(path, lines);
        }
    }

    function syncBreakpointsForFile(path, lines) {
        const bpLines = lines || breakpoints[path] || [];
        sendDap('setBreakpoints', {
            source: { path },
            breakpoints: bpLines.map(line => ({ line })),
        });
    }

    // -------------------------------------------------------------------------
    // Editor integration — highlight current line and receive breakpoint changes
    // -------------------------------------------------------------------------

    function highlightLine(filePath, lineNumber) {
        if (filePath && lineNumber > 0) {
            const virtualPath = realToVirtualPath(filePath);
            if (virtualPath) {
                Layout.openEditor({ path: virtualPath, contentType: 'text/x-java' });
            }
        }
        themingHub.postMessage({
            topic: 'java.debug.highlight',
            data: { filePath, lineNumber },
        });
        if (filePath && lineNumber > 0) {
            $timeout(() => {
                themingHub.postMessage({
                    topic: 'java.debug.highlight',
                    data: { filePath, lineNumber },
                });
            }, 1500);
        }
    }

    function realToVirtualPath(realPath) {
        const marker = '/' + currentWorkspace + '/';
        const idx = realPath.lastIndexOf(marker);
        return idx >= 0 ? realPath.substring(idx) : null;
    }

    function broadcastBreakpoints() {
        themingHub.postMessage({
            topic: 'java.debug.breakpoints',
            data: { breakpoints },
        });
    }

    // Restore glyph decorations in any already-open editors after loading from storage.
    $timeout(broadcastBreakpoints);

    // Re-broadcast whenever an editor tab signals it has finished initialising.
    // This handles the case where the debug view's startup broadcast fired before
    // the editor's iframe registered its listener (e.g. after a full browser refresh).
    themingHub.addMessageListener({
        topic: 'java.debug.breakpoints.request',
        handler: () => broadcastBreakpoints(),
    });

    themingHub.addMessageListener({
        topic: 'java.debug.breakpoints.changed',
        handler: (msg) => {
            const { filePath, lines } = msg;
            if (lines && lines.length > 0) {
                breakpoints[filePath] = lines;
            } else {
                delete breakpoints[filePath];
            }
            saveBreakpoints();
            if ($scope.status === 'connected') {
                syncBreakpointsForFile(filePath);
            }
            $scope.$evalAsync(refreshBpList);
        },
    });
});
