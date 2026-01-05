/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.api.log;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.ArrayType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.oracle.truffle.js.runtime.GraalJSException;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.builtins.JSErrorObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Optional;

/**
 * The Class LogFacade.
 */
@Component
public class LogFacade {

    /** The Constant LOGGER. */
    private static final Logger LOGGER = LoggerFactory.getLogger(LogFacade.class);

    private static final String APP_LOGGER_NAME_PREFIX = "app";

    /** The Constant APP_LOGGER_NAME_SEPARATOR. */
    private static final String APP_LOGGER_NAME_SEPARATOR = ".";

    /** The Constant om. */
    private static final ObjectMapper om = new ObjectMapper();

    /** The Constant objectArrayType. */
    private static final ArrayType objectArrayType = TypeFactory.defaultInstance()
                                                                .constructArrayType(Object.class);

    /**
     * Sets the logging level.
     *
     * @param loggerName the logger name
     * @param level the level
     */
    public static void setLevel(String loggerName, String level) {

        final org.slf4j.Logger logger = getLogger(loggerName);

        if (!(logger instanceof ch.qos.logback.classic.Logger logbackLogger)) {
            LOGGER.error("Logger with name {} is not of type {}", loggerName, ch.qos.logback.classic.Logger.class.getName());
            return;
        }

        logbackLogger.setLevel(ch.qos.logback.classic.Level.valueOf(level));
    }

    /**
     * Gets the logger.
     *
     * @param loggerName the logger name
     * @return Logger
     */
    public static Logger getLogger(final String loggerName) {
        /*
         * logger names are implicitly prefixed with 'app.' to derive from the applications root logger
         * configuration for severity and appenders. Null arguments for logger name will be treated as
         * reference to the 'app' logger
         */
        String appLoggerName = loggerName;
        if (appLoggerName == null) {
            appLoggerName = APP_LOGGER_NAME_PREFIX;
        } else {
            appLoggerName = APP_LOGGER_NAME_PREFIX + APP_LOGGER_NAME_SEPARATOR + appLoggerName;
        }

        return LoggerFactory.getLogger(appLoggerName);
    }

    public static boolean isDebugEnabled(String loggerName) throws IOException {
        Logger logger = getLogger(loggerName);
        return logger.isDebugEnabled();
    }

    public static boolean isErrorEnabled(String loggerName) throws IOException {
        Logger logger = getLogger(loggerName);
        return logger.isErrorEnabled();
    }

    public static boolean isWarnEnabled(String loggerName) throws IOException {
        Logger logger = getLogger(loggerName);
        return logger.isWarnEnabled();
    }

    public static boolean isInfoEnabled(String loggerName) throws IOException {
        Logger logger = getLogger(loggerName);
        return logger.isInfoEnabled();
    }

    public static boolean isTraceEnabled(String loggerName) throws IOException {
        Logger logger = getLogger(loggerName);
        return logger.isTraceEnabled();
    }

    /**
     * Log.
     *
     * @param loggerName the logger name
     * @param level the level
     * @param message the message
     * @param logArguments the log arguments
     * @param rawError the error json
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public static void log(String loggerName, String level, String message, String logArguments, Object rawError) throws IOException {

        final Logger logger = getLogger(loggerName);

        if (ch.qos.logback.classic.Level.DEBUG.toString()
                                              .equalsIgnoreCase(level)
                && logger.isDebugEnabled()) {
            Object[] args = getMessageArgs(loggerName, level, message, logArguments, rawError);
            logger.debug(message, args);
        } else if (ch.qos.logback.classic.Level.TRACE.toString()
                                                     .equalsIgnoreCase(level)
                && logger.isTraceEnabled()) {
            Object[] args = getMessageArgs(loggerName, level, message, logArguments, rawError);
            logger.trace(message, args);
        } else if (ch.qos.logback.classic.Level.INFO.toString()
                                                    .equalsIgnoreCase(level)
                && logger.isInfoEnabled()) {
            Object[] args = getMessageArgs(loggerName, level, message, logArguments, rawError);
            logger.info(message, args);
        } else if (ch.qos.logback.classic.Level.WARN.toString()
                                                    .equalsIgnoreCase(level)
                && logger.isWarnEnabled()) {
            Object[] args = getMessageArgs(loggerName, level, message, logArguments, rawError);
            logger.warn(message, args);
        } else if (ch.qos.logback.classic.Level.ERROR.toString()
                                                     .equalsIgnoreCase(level)
                && logger.isErrorEnabled()) {
            Object[] args = getMessageArgs(loggerName, level, message, logArguments, rawError);
            logger.error(message, args);
        } else {
            LOGGER.debug("Logging using logger [{}] for message [{}] will be skipped. Log level [{}].", loggerName, message, level);
        }
    }

    private static Object[] getMessageArgs(String loggerName, String level, String message, String logArguments, Object rawError) {
        Object[] args = null;
        if (logArguments != null) {
            try {
                args = om.readValue(logArguments, objectArrayType);
                if (args.length < 1) {
                    args = null;
                }
            } catch (IOException e) {
                LOGGER.error("Cannot parse log arguments for logger [{}] for message [{}] at level [{}]", loggerName, message, level, e);
            }
        }

        Throwable error = extractError(rawError);
        if (error != null) {
            if (args == null) {
                args = new Object[] {error};
            } else {
                args = Arrays.copyOf(args, args.length + 1);
                args[args.length - 1] = error;
            }
        }

        return args;
    }

    private static Throwable extractError(Object rawError) {
        if (null == rawError) {
            return null;
        }

        // in case the error is thrown in a java code (called by script code)
        if (rawError instanceof Throwable throwable) {
            return throwable;
        }

        // in case the error is thrown in a script code
        Optional<Method> guestObjectMethod = getPublicMethod(rawError, "getGuestObject");
        if (guestObjectMethod.isPresent()) {
            Object guestObject = invokeMethod(guestObjectMethod.get(), rawError);
            if (guestObject instanceof JSErrorObject jsErrorObject) {
                if (jsErrorObject.isException()) {
                    GraalJSException graalJSException = jsErrorObject.getException();
                    GraalJSException.JSStackTraceElement[] jsStackTrace = graalJSException.getJSStackTrace();
                    StackTraceElement[] jsStackTraceInJavaFormat = toJavaStackTrace(jsStackTrace);

                    LOGGER.debug("A script error occurred (before changing the trace)", graalJSException);
                    // set js stack trace to be printed when logged
                    graalJSException.setStackTrace(jsStackTraceInJavaFormat);

                    return graalJSException;
                }
            }
        }

        LOGGER.warn("An error cannot be extracted from [{}] of class [{}]", rawError, rawError.getClass());

        return null;
    }

    private static StackTraceElement[] toJavaStackTrace(GraalJSException.JSStackTraceElement[] jsStackTrace) {
        if (null == jsStackTrace) {
            return null;
        }

        return Arrays.stream(jsStackTrace)
                     .map(LogFacade::toJavaStackTrace)
                     .toArray(StackTraceElement[]::new);
    }

    private static StackTraceElement toJavaStackTrace(GraalJSException.JSStackTraceElement jsStackTrace) {
        String declaringClass = "<js>";
        String methodName = jsStackTrace.getMethodName();
        String fileName = Strings.toJavaString(jsStackTrace.getFileName());
        int lineNumber = jsStackTrace.getLineNumber();

        return new StackTraceElement(declaringClass, methodName, fileName, lineNumber);
    }

    private static Object invokeMethod(Method method, Object object) {
        try {
            method.trySetAccessible();
            return method.invoke(object);
        } catch (IllegalAccessException | InvocationTargetException ex) {
            throw new IllegalStateException("Failed to call method " + method + " in object " + object, ex);
        }
    }

    private static Optional<Method> getPublicMethod(Object obj, String methodName) {
        try {
            return Optional.of(obj.getClass()
                                  .getMethod(methodName));
        } catch (NoSuchMethodException e) {
            LOGGER.debug("Missing method [{}] in object [{}]", methodName, obj);
            return Optional.empty();
        }
    }

}
