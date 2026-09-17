## Java Debugger (`ide-java-debug` + `view-java-debug`)

Implemented in PR [#5948](https://github.com/eclipse-dirigible/dirigible/pull/5948). Bridges the browser IDE to a JDWP-enabled JVM via the Debug Adapter Protocol (DAP).

### Architecture

```
Browser (debug.js) ←WebSocket→ JavaDebugWebSocketHandler
                                  ↓ JavaDebugManager
                                  ↓ JavaDebugBridge (per workspace)
                                  ↓ DAP TCP socket
                                  JDT.LS (vscode.java.startDebugSession)
                                  ↓ JDWP TCP (port 8000 by default)
                                  Target JVM
```

**Key classes:**
- `JavaDebugManager` (`components/ide/ide-java-debug`) — singleton Spring bean; one `JavaDebugBridge` per `username/workspace` key. `getOrStart()` is the entry point: asks JDT.LS for a DAP TCP port via `workspace/executeCommand → vscode.java.startDebugSession`, then creates the bridge. `removeSession()` tears down the bridge when the last WebSocket session leaves.
- `JavaDebugBridge` — reads Content-Length-framed DAP messages from the TCP socket and broadcasts them as raw JSON to all WebSocket sessions. Translates `setBreakpoints` source paths from virtual workspace paths (`/workspace/proj/Foo.java`) to real filesystem paths before forwarding to the DAP server. **`destroy()` sends `disconnect` with `terminateDebuggee:false` before closing the socket** — without this the DAP adapter calls `VM.exit()` and kills the JVM, making the JDWP port refuse the next attach.
- `JavaDebugWebSocketHandler` — Spring WebSocket handler at `/websockets/ide/java-debug?workspace=<name>`. On open: `manager.getOrStart(username, workspace).addSession(session)`. On message: `bridge.sendToDap(json)`. On close: `manager.removeSession(sessionId)`.
- `JavaDebugConfigEndpoint` — `GET /services/ide/java-debug/config` → `{"jdwpPort": N}`. The default port is `DirigibleConfig.JAVA_DEBUG_JDWP_PORT` (`DIRIGIBLE_JAVA_DEBUG_JDWP_PORT`, default `8000`).

**OSGi requirement:** `com.microsoft.java.debug.plugin-*.jar` must be in the JDT.LS `plugins/` directory **and** listed in every `config_<platform>/config.ini` `osgi.bundles` entry. `JdtLsManager.installDebugPlugin()` handles both — it reads `Bundle-Version` from the jar's manifest and appends the entry. Without this, `vscode.java.startDebugSession` returns "unknown command".

**Path translation:** the browser sends virtual paths like `/workspace/proj/Foo.java`; the DAP server needs real paths like `/home/user/.dirigible/repository/root/users/admin/workspace/proj/Foo.java`. `JavaDebugBridge.translateSourcePaths()` rewrites `setBreakpoints` requests. In the reverse direction, `realToVirtualPath()` in `debug.js` uses `lastIndexOf('/' + workspaceName + '/')` to strip the server prefix from stack-frame source paths.

**Server-side LSP init:** `JdtLsInstance.ensureInitialized()` sends `initialize` + `initialized` to JDT.LS without a browser editor session so that `workspace/executeCommand` is not queued forever waiting for a client. Timeout: 60 s for `ensureInitialized`, 90 s for `startDebugSession` (CI runners are slow).

### Frontend (`components/ui/view-java-debug`)

- **Region:** `left` (sits alongside Projects/Import/Search in the left sidebar)
- **Config:** `configs/debug-view.js` — `region: 'left'`, `lazyLoad: true`
- **Layout:** compact toolbar (Attach, Disconnect, step controls) + status strip + three collapsible sections (Call Stack, Variables, Breakpoints)
- **Workspace:** read from `localStorage[${getBrandingInfo().prefix}.workspace.selected]` on load; kept in sync via `platform.workspace.changed` hub topic (same source as the Projects view)
- **Port:** fetched from `GET /services/ide/java-debug/config` on startup; falls back to 8000
- **Host:** always `localhost` — not configurable from the UI
- **Connecting animation:** `$interval` cycles through 18 descriptive phrases every 3 s while `status === 'connecting'`; cancelled on connected / error / disconnect / terminate / `$destroy`
- **Breakpoints persistence:** stored in `localStorage['dirigible.java.debug.breakpoints']` as `{ virtualPath → int[] }`. Restored on load; `refreshBpList()` must be called after restoring to populate `$scope.bpList`.

### Editor integration (`components/ui/editor-monaco`)

- **`java.debug.highlight` topic:** `debug.js` posts `{ filePath: realPath, lineNumber }` when paused; editors match via `filePath.endsWith(myPath)`. Post is repeated after 1500 ms for newly opened tabs. `clearPausedState()` in `dapContinue()` posts `{ filePath: null, lineNumber: 0 }` immediately (the DAP `continued` event is optional and often skipped by the Java adapter for long-running JVMs).
- **`java.debug.breakpoints` topic:** `debug.js` broadcasts the full `breakpoints` map; each editor restores glyph decorations for its own path. After registering its listener the editor immediately posts `java.debug.breakpoints.request` so the debug view re-broadcasts — this handles the race where the debug view's startup broadcast fires before the editor iframe is ready.
- **Glyph CSS** (`editor.css`): `.debug-breakpoint-glyph` — red circle (14 × 14 px, `border-radius:50%`). `.debug-current-line-glyph` — right-pointing yellow triangle (CSS border trick, `#ffcc00`, 12 × 14 px). `.debug-current-line` — faint yellow line background.
- **Glyph margin clicks:** `onMouseDown` with `isGlyphMarginLineNumber` toggles breakpoints and posts `java.debug.breakpoints.changed` with the updated lines array for that file.

