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
javaDebugApp.controller('JavaDebugController', ($scope, $timeout, Layout) => {
    const themingHub = new ThemingHub();

    $scope.config = {
        workspace: 'workspace',
        host: 'localhost',
        port: '8000',
    };

    $scope.status = 'disconnected'; // disconnected | connecting | connected | error
    $scope.callStack = [];
    $scope.variables = [];
    $scope.selectedFrame = null;

    // Map of virtualFilePath -> int[]  (1-based line numbers)
    const breakpoints = {};
    $scope.bpList = [];
    let ws = null;
    let dapSeq = 1;

    function refreshBpList() {
        const list = [];
        for (const [file, lines] of Object.entries(breakpoints)) {
            for (const line of lines) {
                list.push({ file: file.split('/').pop(), line, fullPath: file });
            }
        }
        $scope.bpList = list;
    }

    $scope.statusLabel = () => {
        switch ($scope.status) {
            case 'connected': return 'Connected';
            case 'connecting': return 'Connecting...';
            case 'error': return 'Error';
            default: return 'Disconnected';
        }
    };

    $scope.isPaused = () => $scope.callStack.length > 0;

    $scope.removeBreakpoint = (bp) => {
        if (breakpoints[bp.fullPath]) {
            breakpoints[bp.fullPath] = breakpoints[bp.fullPath].filter(l => l !== bp.line);
            if (breakpoints[bp.fullPath].length === 0) {
                delete breakpoints[bp.fullPath];
            }
            refreshBpList();
            syncBreakpointsForFile(bp.fullPath);
            broadcastBreakpoints();
        }
    };

    $scope.attach = () => {
        if (ws) {
            ws.close();
        }
        $scope.status = 'connecting';
        const workspace = $scope.config.workspace || 'workspace';
        const wsUrl = `ws://${location.host}/websockets/ide/java-debug?workspace=${encodeURIComponent(workspace)}`;
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
            $timeout(() => { $scope.status = 'error'; });
        };
        ws.onclose = () => {
            $timeout(() => {
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
        $scope.status = 'disconnected';
        $scope.callStack = [];
        $scope.variables = [];
        highlightLine(null, 0);
    };

    $scope.dapContinue = () => sendDap('continue', { threadId: currentThreadId() });
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
                $timeout(() => { $scope.status = 'connected'; });
                sendDap('attach', {
                    hostName: $scope.config.host || 'localhost',
                    port: parseInt($scope.config.port, 10) || 8000,
                });
                sendAllBreakpoints();
                break;
            case 'stackTrace':
                if (msg.body && msg.body.stackFrames) {
                    $timeout(() => {
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
                    $timeout(() => { $scope.variables = msg.body.variables; });
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
                $timeout(() => { $scope.callStack = []; $scope.variables = []; });
                sendDap('stackTrace', { threadId: _stoppedThreadId, startFrame: 0, levels: 20 });
                break;
            case 'continued':
                $timeout(() => { $scope.callStack = []; $scope.variables = []; });
                highlightLine(null, 0);
                break;
            case 'terminated':
            case 'exited':
                $timeout(() => {
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
            // DAP returns real filesystem paths; convert to the virtual workspace path
            // that Layout.openEditor and the editor's resourcePath use.
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
            // Re-broadcast after a short delay so newly opened editor tabs receive it
            // (they register their listener after their iframe initialises).
            $timeout(() => {
                themingHub.postMessage({
                    topic: 'java.debug.highlight',
                    data: { filePath, lineNumber },
                });
            }, 1500);
        }
    }

    /**
     * Converts a real server-side filesystem path returned by the DAP stack trace
     * (e.g. /repo/root/users/admin/workspace/proj/Foo.java) back to the virtual
     * workspace path the IDE uses (e.g. /workspace/proj/Foo.java) by finding the
     * last occurrence of /{workspaceName}/ in the path.
     */
    function realToVirtualPath(realPath) {
        const ws = $scope.config.workspace || 'workspace';
        const marker = '/' + ws + '/';
        const idx = realPath.lastIndexOf(marker);
        return idx >= 0 ? realPath.substring(idx) : null;
    }

    function broadcastBreakpoints() {
        themingHub.postMessage({
            topic: 'java.debug.breakpoints',
            data: { breakpoints },
        });
    }

    // Listen for breakpoint changes from the editor (glyph margin clicks)
    themingHub.addMessageListener({
        topic: 'java.debug.breakpoints.changed',
        handler: (msg) => {
            const { filePath, lines } = msg;
            if (lines && lines.length > 0) {
                breakpoints[filePath] = lines;
            } else {
                delete breakpoints[filePath];
            }
            if ($scope.status === 'connected') {
                syncBreakpointsForFile(filePath);
            }
            $timeout(refreshBpList);
        },
    });
});
