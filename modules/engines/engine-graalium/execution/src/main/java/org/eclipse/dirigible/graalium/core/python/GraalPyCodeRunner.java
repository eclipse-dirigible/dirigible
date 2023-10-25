package org.eclipse.dirigible.graalium.core.python;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;

import org.eclipse.dirigible.graalium.core.CodeRunner;
import org.eclipse.dirigible.graalium.core.graal.ContextCreator;
import org.eclipse.dirigible.graalium.core.graal.EngineCreator;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;

public class GraalPyCodeRunner implements CodeRunner<Source, Value> {
    private final Context context;

    public GraalPyCodeRunner(
            Path workingDirectoryPath,
            Path projectDirectoryPath,
            Path pythonModulesPath,
            boolean debug
    ) {
        var engine = debug ? EngineCreator.getOrCreateDebuggableEngine() : EngineCreator.getOrCreateEngine();
        var fs = new GraalPyFileSystem(workingDirectoryPath, FileSystems.getDefault());
        context = new ContextCreator(engine, workingDirectoryPath, projectDirectoryPath, pythonModulesPath, fs).createContext();
    }

    @Override
    public Source prepareSource(Path codeFilePath) {
        try {
            return Source.newBuilder("python", codeFilePath.toFile()).build();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Value run(Source codeSource) {
        return context.eval(codeSource);
    }

    @Override
    public void close() {
        context.close();
    }
}