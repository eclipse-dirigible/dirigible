/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.graalium.core.javascript;

import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.eclipse.dirigible.components.open.telemetry.OpenTelemetryProvider;
import org.eclipse.dirigible.graalium.core.CodeRunner;
import org.eclipse.dirigible.graalium.core.graal.ContextCreator;
import org.eclipse.dirigible.graalium.core.graal.EngineCreator;
import org.eclipse.dirigible.graalium.core.graal.ValueTransformer;
import org.eclipse.dirigible.graalium.core.graal.globals.GlobalFunction;
import org.eclipse.dirigible.graalium.core.graal.globals.GlobalObject;
import org.eclipse.dirigible.graalium.core.javascript.modules.ModuleResolver;
import org.eclipse.dirigible.graalium.core.javascript.modules.ModuleType;
import org.eclipse.dirigible.graalium.core.javascript.modules.downloadable.DownloadableModuleResolver;
import org.eclipse.dirigible.graalium.core.javascript.polyfills.JavascriptPolyfill;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.slf4j.LoggerFactory;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;

/**
 * The Class GraalJSCodeRunner.
 */
public class GraalJSCodeRunner implements CodeRunner<Source, Value> {

    /**
     * The current working directory path.
     */
    private final Path currentWorkingDirectoryPath;

    /**
     * The graal context.
     */
    private final Context graalContext;

    /**
     * The graal JS source creator.
     */
    private final GraalJSSourceCreator graalJSSourceCreator;

    /**
     * The graal JS interceptor.
     */
    private final GraalJSInterceptor graalJSInterceptor;

    /**
     * Instantiates a new graal JS code runner.
     *
     * @param builder the builder
     */
    private GraalJSCodeRunner(Builder builder) {
        this.currentWorkingDirectoryPath = builder.workingDirectoryPath;

        Consumer<Context.Builder> onBeforeContextCreatedHook = provideOnBeforeContextCreatedHook(builder.onBeforeContextCreatedListeners);
        Consumer<Context> onAfterContextCreatedHook = provideOnAfterContextCreatedHook(builder.onAfterContextCreatedListener);

        Engine graalEngine = builder.waitForDebugger ? EngineCreator.getOrCreateDebuggableEngine() : EngineCreator.getOrCreateEngine();
        DownloadableModuleResolver downloadableModuleResolver = new DownloadableModuleResolver(builder.dependenciesCachePath);

        GraalJSFileSystem graalJSFileSystem = new GraalJSFileSystem(currentWorkingDirectoryPath, builder.moduleResolvers,
                downloadableModuleResolver, builder.onRealPathNotFound, builder.delegateFileSystem);

        graalJSSourceCreator = new GraalJSSourceCreator(builder.jsModuleType);
        graalJSInterceptor = builder.interceptor;
        graalContext = new ContextCreator(graalEngine, currentWorkingDirectoryPath, currentWorkingDirectoryPath, null,
                onBeforeContextCreatedHook, onAfterContextCreatedHook, graalJSFileSystem).createContext();

        addGlobalObjects(builder.globalObjects);
        registerPolyfills(graalContext, builder.jsPolyfills);
    }

    /**
     * Provide on before context created hook.
     *
     * @param onBeforeContextCreatedListeners the on before context created listeners
     * @return the consumer
     */
    private static Consumer<Context.Builder> provideOnBeforeContextCreatedHook(
            List<Consumer<Context.Builder>> onBeforeContextCreatedListeners) {
        return contextBuilder -> onBeforeContextCreatedListeners.forEach(x -> x.accept(contextBuilder));
    }

    /**
     * Provide on after context created hook.
     *
     * @param onAfterContextCreatedListeners the on after context created listeners
     * @return the consumer
     */
    private static Consumer<Context> provideOnAfterContextCreatedHook(List<Consumer<Context>> onAfterContextCreatedListeners) {
        return context -> onAfterContextCreatedListeners.forEach(x -> x.accept(context));
    }

    /**
     * Register polyfills.
     *
     * @param context the context
     * @param jsPolyfills the js polyfills
     */
    private void registerPolyfills(Context context, List<JavascriptPolyfill> jsPolyfills) {
        jsPolyfills.stream()
                   .map(polyfill -> graalJSSourceCreator.createInternalSource(polyfill.getSource(), polyfill.getFileName()))
                   .forEach(context::eval);
    }

    /**
     * Adds the global objects.
     *
     * @param globalObjects the js global objects
     */
    public void addGlobalObjects(List<GlobalObject> globalObjects) {
        Value contextBindings = graalContext.getBindings("js");
        globalObjects.forEach(global -> contextBindings.putMember(global.getName(), global.getValue()));
    }

    /**
     * The Class Builder.
     */
    public static class Builder {

        /**
         * The working directory path.
         */
        private final Path workingDirectoryPath;

        /**
         * The dependencies cache path.
         */
        private final Path dependenciesCachePath;
        /**
         * The js polyfills.
         */
        private final List<JavascriptPolyfill> jsPolyfills = new ArrayList<>();
        /**
         * The global objects.
         */
        private final List<GlobalObject> globalObjects = new ArrayList<>();
        /**
         * The on before context created listeners.
         */
        private final List<Consumer<Context.Builder>> onBeforeContextCreatedListeners = new ArrayList<>();
        /**
         * The on after context created listener.
         */
        private final List<Consumer<Context>> onAfterContextCreatedListener = new ArrayList<>();
        /**
         * The module resolvers.
         */
        private final List<ModuleResolver> moduleResolvers = new ArrayList<>();
        /**
         * The wait for debugger.
         */
        private boolean waitForDebugger = false;
        /**
         * The js module type.
         */
        private ModuleType jsModuleType = ModuleType.BASED_ON_FILE_EXTENSION;
        /**
         * The interceptor *.
         */
        private GraalJSInterceptor interceptor;

        /**
         * The callback to invoke in GraalJS' FileSystem when a Path's toRealPath fails.
         */
        private Function<Path, Path> onRealPathNotFound;

        /**
         * The file system to delegate to in GraalJS' file system
         */
        private FileSystem delegateFileSystem = FileSystems.getDefault();

        /**
         * Instantiates a new builder.
         *
         * @param workingDirectoryPath the working directory path
         * @param cachesPath the caches path
         */
        public Builder(Path workingDirectoryPath, Path cachesPath) {
            this.workingDirectoryPath = workingDirectoryPath;
            this.dependenciesCachePath = cachesPath.resolve("dependencies-cache");
        }

        /**
         * With JS module type.
         *
         * @param jsModuleType the js module type
         * @return the builder
         */
        public Builder withJSModuleType(ModuleType jsModuleType) {
            this.jsModuleType = jsModuleType;
            return this;
        }

        /**
         * Wait for debugger.
         *
         * @param shouldWaitForDebugger the should wait for debugger
         * @return the builder
         */
        public Builder waitForDebugger(boolean shouldWaitForDebugger) {
            waitForDebugger = shouldWaitForDebugger;
            return this;
        }

        /**
         * Wait for debugger.
         *
         * @param shouldWaitForDebugger the should wait for debugger
         * @return the builder
         */
        public Builder waitForDebugger(Supplier<Boolean> shouldWaitForDebugger) {
            waitForDebugger = shouldWaitForDebugger.get();
            return this;
        }

        /**
         * Adds the JS polyfill.
         *
         * @param jsPolyfill the js polyfill
         * @return the builder
         */
        public Builder addJSPolyfill(JavascriptPolyfill jsPolyfill) {
            jsPolyfills.add(jsPolyfill);
            return this;
        }

        /**
         * Adds the global object.
         *
         * @param globalObject the js global object
         * @return the builder
         */
        public Builder addGlobalObject(GlobalObject globalObject) {
            globalObjects.add(globalObject);
            return this;
        }

        /**
         * Adds the module resolver.
         *
         * @param moduleResolver the module resolver
         * @return the builder
         */
        public Builder addModuleResolver(ModuleResolver moduleResolver) {
            moduleResolvers.add(moduleResolver);
            return this;
        }

        /**
         * Adds the on before context created listener.
         *
         * @param onBeforeContextCreatedListener the on before context created listener
         * @return the builder
         */
        public Builder addOnBeforeContextCreatedListener(Consumer<Context.Builder> onBeforeContextCreatedListener) {
            if (onBeforeContextCreatedListener != null) {
                onBeforeContextCreatedListeners.add(onBeforeContextCreatedListener);
            }
            return this;
        }

        /**
         * Adds the on after context created listener.
         *
         * @param onAfterContextCreatedListener the on after context created listener
         * @return the builder
         */
        public Builder addOnAfterContextCreatedListener(Consumer<Context> onAfterContextCreatedListener) {
            if (onAfterContextCreatedListener != null) {
                this.onAfterContextCreatedListener.add(onAfterContextCreatedListener);
            }
            return this;
        }

        /**
         * Sets a callback to invoke when a Path's toRealPath fails in GraalJS' FileSystem. This callback
         * should return another Path if it could be constructed or throw an exception
         *
         * @param onRealPathNotFound the callback to invoke
         * @return the builder
         */
        public Builder setOnRealPathNotFound(Function<Path, Path> onRealPathNotFound) {
            this.onRealPathNotFound = onRealPathNotFound;
            return this;
        }

        /**
         * Sets a file system to use in GraalJS' FileSystem. Defaults to the system one.
         *
         * @param fileSystem the file system to use
         * @return
         */
        public Builder setDelegateFileSystem(FileSystem fileSystem) {
            this.delegateFileSystem = fileSystem;
            return this;
        }

        /**
         * Adds the on after context created listener.
         *
         * @param interceptor the interceptor
         * @return the builder
         */
        public Builder setInterceptor(GraalJSInterceptor interceptor) {
            if (interceptor != null) {
                this.interceptor = interceptor;
            }
            return this;
        }

        /**
         * Builds the.
         *
         * @return the graal JS code runner
         * @throws IllegalStateException the illegal state exception
         */
        public GraalJSCodeRunner build() throws IllegalStateException {
            if (workingDirectoryPath == null || dependenciesCachePath == null) {
                throw new RuntimeException("Please, provide all folder paths!");
            }

            return new GraalJSCodeRunner(this);
        }

    }

    /**
     * Gets the graal context.
     *
     * @return the graal context
     */
    public Context getGraalContext() {
        return graalContext;
    }

    /**
     * Run.
     *
     * @param codeFilePath the code file path
     * @return the source
     */
    @Override
    public Source prepareSource(Path codeFilePath) {
        Path relativeCodeFilePath = currentWorkingDirectoryPath.resolve(codeFilePath);
        return graalJSSourceCreator.createSource(relativeCodeFilePath);
    }

    /**
     * Run.
     *
     * @param codeSource the code source
     * @return the value
     */

    @Override
    public Value run(Source codeSource) {
        Tracer tracer = OpenTelemetryProvider.get()
                                             .getTracer("eclipse-dirigible");
        Span span = tracer.spanBuilder("script_execution")
                          .startSpan();
        try (Scope scope = span.makeCurrent()) {
            span.addEvent("Executing [" + codeSource.getPath() + "]");
            span.setAttribute("source", codeSource.toString());
            span.setAttribute("source.name", codeSource.getName());

            Value result = graalContext.eval(codeSource);

            Object convertedResult = ValueTransformer.transformValue(result);
            String resultAttribute = null == convertedResult ? null : convertedResult.toString();
            span.setAttribute("result", resultAttribute);

            rethrowIfError(result);
            span.addEvent("Successfully executed [" + codeSource.getPath() + "]");

            return result;
        } catch (PolyglotException e) {
            // Try to add the script stacktrace as cause
            if (e.isGuestException()) {
                Value guestObject = e.getGuestObject();
                if (guestObject.isException()) {
                    String exMessage = getExceptionMessage(guestObject);
                    if (exMessage == null) {
                        exMessage = e.getMessage();
                    }
                    String exClassName = getExceptionClass(guestObject);
                    Throwable exCause = getExceptionCause(guestObject);
                    StackTraceElement[] stackTrace = getExceptionStackTrace(guestObject);
                    if (exMessage != null || exClassName != null || (null != stackTrace && stackTrace.length > 0)) {
                        GuestLanguageException guestLanguageException =
                                new GuestLanguageException(codeSource.getPath(), exMessage, exClassName, exCause);
                        guestLanguageException.setStackTrace(stackTrace);
                        e.addSuppressed(guestLanguageException);
                    }
                }
            }

            span.recordException(e);
            span.setStatus(io.opentelemetry.api.trace.StatusCode.ERROR, "Exception occurred");

            throw e;
        } catch (RuntimeException e) {
            span.recordException(e);
            span.setStatus(io.opentelemetry.api.trace.StatusCode.ERROR, "Exception occurred");

            throw e;
        } finally {
            span.end();
        }
    }

    private String getExceptionMessage(Value value) {

        String getMessageMethodName = "getMessage";
        if (value.isException() && value.canInvokeMember(getMessageMethodName)) {
            return value.invokeMember(getMessageMethodName)
                        .asString();
        }
        return null;
    }

    private String getExceptionClass(Value value) {

        String getClassMethodName = "getClass";
        if (value.canInvokeMember(getClassMethodName)) {
            Value classValue = value.invokeMember(getClassMethodName);

            String getNameMethodName = "getName";
            if (classValue.canInvokeMember(getNameMethodName)) {
                return classValue.invokeMember(getNameMethodName)
                                 .asString();
            }
        }
        return null;
    }

    private Throwable getExceptionCause(Value value) {

        String getCauseMethodName = "getCause";
        if (value.isException() && value.canInvokeMember(getCauseMethodName)) {
            Value causeValue = value.invokeMember(getCauseMethodName);
            if (null != causeValue && causeValue.isException()) {
                return causeValue.as(Throwable.class);
            }
        }
        return null;
    }

    private StackTraceElement[] getExceptionStackTrace(Value value) {

        String getStackTraceMethodName = "getStackTrace";
        if (value.isException() && value.canInvokeMember(getStackTraceMethodName)) {
            Value stackTrace = value.invokeMember(getStackTraceMethodName);
            return stackTrace.as(StackTraceElement[].class);
        }

        return new StackTraceElement[0];
    }

    /**
     * Rethrow if error.
     *
     * @param maybeError the maybe error
     */
    private static void rethrowIfError(Value maybeError) {
        if (maybeError.isException()) {
            throw maybeError.throwException();
        }
    }

    /**
     * Gets the graal JS interceptor.
     *
     * @return the graal JS interceptor
     */
    public GraalJSInterceptor getGraalJSInterceptor() {
        return graalJSInterceptor;
    }

    /**
     * Gets the current working directory path.
     *
     * @return the current working directory path
     */
    public Path getCurrentWorkingDirectoryPath() {
        return currentWorkingDirectoryPath;
    }

    /**
     * Adds the global functions.
     *
     * @param globalFunctions the js global functions
     */
    public void addGlobalFunctions(List<GlobalFunction> globalFunctions) {
        Value contextBindings = graalContext.getBindings("js");
        globalFunctions.forEach(global -> contextBindings.putMember(global.getName(), global));
    }

    /**
     * Parses the.
     *
     * @param codeFilePath the code file path
     * @return the value
     */
    public Value parse(Path codeFilePath) {
        Path relativeCodeFilePath = currentWorkingDirectoryPath.resolve(codeFilePath);
        Source codeSource = graalJSSourceCreator.createSource(relativeCodeFilePath);
        return parse(codeSource);
    }

    /**
     * Parses the.
     *
     * @param codeSource the code source
     * @return the value
     */
    public Value parse(Source codeSource) {
        return graalContext.parse(codeSource);
    }

    /**
     * Leave.
     */
    public void leave() {
        graalContext.leave();
    }

    /**
     * New builder.
     *
     * @param currentWorkingDirectoryPath the current working directory path
     * @param cachesPath the caches path
     * @return the builder
     */
    public static Builder newBuilder(Path currentWorkingDirectoryPath, Path cachesPath) {
        return new Builder(currentWorkingDirectoryPath, cachesPath);
    }

    /**
     * Close.
     */
    @Override
    public void close() {
        if (graalContext != null) {
            graalContext.close(false);
        }
    }
}
