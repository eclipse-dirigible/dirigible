## Native applications (`engine-native-apps`)

User projects can declare a `*.nativeapp` JSON file that turns an external web server — local OS process or remote HTTP(S) endpoint — into a first-class Dirigible artefact, reverse-proxied under `/services/native-apps-proxy/v1/<basePath>/...` with optional Dirigible-managed authentication and role-based access. The whole feature lives in `components/engine/engine-native-apps`.

**Detailed guide:** [`components/engine/engine-native-apps/CLAUDE.md`](components/engine/engine-native-apps/CLAUDE.md). Read it before changing anything under that module — it covers the synchronizer model, kinds + start modes, port resolution, the stop / teardown contract (three layered guarantees), the dual-DELETE rehydrate requirement, PID + port logging, the proxy filter chain, placeholder expansion, the credentials field naming, the management endpoint's role policy, integration-test patterns including the no-manual-cleanup rule, and the macOS gotchas (Python 3.14 bind regression, wildcard-vs-loopback `ServerSocket` probe, `PollingWatchService` deadlock).

