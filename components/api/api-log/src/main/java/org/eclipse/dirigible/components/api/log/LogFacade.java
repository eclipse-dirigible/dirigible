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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.ArrayType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Arrays;

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
     * The Class ErrorObject.
     */
    static class ErrorObject {

        /** The error message. */
        @JsonProperty("message")
        public String message;

        /** The error stack. */
        @JsonProperty("stack")
        public StackTraceEl[] stack;
    }


    /**
     * The Class StackTraceEl.
     */
    static class StackTraceEl {

        /** The file name. */
        @JsonProperty("fileName")
        public String fileName;

        /** The line number. */
        @JsonProperty("lineNumber")
        public int lineNumber;

        /** The declaring class. */
        @JsonProperty("declaringClass")
        public String declaringClass;

        /** The method name. */
        @JsonProperty("methodName")
        public String methodName;
    }

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
     * @param errorJson the error json
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public static void log(String loggerName, String level, String message, String logArguments, String errorJson) throws IOException {

        final Logger logger = getLogger(loggerName);

        if (ch.qos.logback.classic.Level.DEBUG.toString()
                                              .equalsIgnoreCase(level)
                && logger.isDebugEnabled()) {
            Object[] args = getMessageArgs(loggerName, level, message, logArguments, errorJson);
            logger.debug(message, args);
        } else if (ch.qos.logback.classic.Level.TRACE.toString()
                                                     .equalsIgnoreCase(level)
                && logger.isTraceEnabled()) {
            Object[] args = getMessageArgs(loggerName, level, message, logArguments, errorJson);
            logger.trace(message, args);
        } else if (ch.qos.logback.classic.Level.INFO.toString()
                                                    .equalsIgnoreCase(level)
                && logger.isInfoEnabled()) {
            Object[] args = getMessageArgs(loggerName, level, message, logArguments, errorJson);
            logger.info(message, args);
        } else if (ch.qos.logback.classic.Level.WARN.toString()
                                                    .equalsIgnoreCase(level)
                && logger.isWarnEnabled()) {
            Object[] args = getMessageArgs(loggerName, level, message, logArguments, errorJson);
            logger.warn(message, args);
        } else if (ch.qos.logback.classic.Level.ERROR.toString()
                                                     .equalsIgnoreCase(level)
                && logger.isErrorEnabled()) {
            Object[] args = getMessageArgs(loggerName, level, message, logArguments, errorJson);
            logger.error(message, args);
        } else {
            LOGGER.debug("Logging using logger [{}] for message [{}] will be skipped. Log level level [{}].", loggerName, message, level);
        }
    }

    private static Object[] getMessageArgs(String loggerName, String level, String message, String logArguments, String errorJson)
            throws IOException {
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
        // https://www.slf4j.org/faq.html#paramException
        if (errorJson != null) {
            Exception ex = toException(errorJson);
            if (args == null) {
                args = new Object[] {ex};
            } else {
                args = Arrays.copyOf(args, args.length + 1);
                args[args.length - 1] = ex;
            }
        }
        return args;
    }

    /**
     * Creates a JSServiceException from JSON.
     *
     * @param errorJson the error json
     * @return the JS service exception
     * @throws JsonMappingException the json mapping exception
     * @throws IOException Signals that an I/O exception has occurred.
     */
    private static Exception toException(String errorJson) throws JsonMappingException, IOException {

        Exception ex;

        if (errorJson != null) {

            ErrorObject errObj = om.readValue(errorJson, ErrorObject.class);

            if (errObj.message == null) {
                ex = new Exception();
            } else {
                ex = new Exception(errObj.message);
            }

            if (errObj.stack != null) {
                StackTraceElement[] stackTraceElementArray = new StackTraceElement[errObj.stack.length];
                for (int i = 0; i < errObj.stack.length; i++) {
                    StackTraceEl customStackTraceElement = errObj.stack[i];
                    StackTraceElement stackTraceElement = new StackTraceElement(customStackTraceElement.declaringClass,
                            customStackTraceElement.methodName, customStackTraceElement.fileName, customStackTraceElement.lineNumber);
                    stackTraceElementArray[i] = stackTraceElement;
                }
                ex.setStackTrace(stackTraceElementArray);
            } else {
                ex.setStackTrace(new StackTraceElement[] {new StackTraceElement("", "", null, 1)});
            }

        } else {
            ex = new Exception();
            ex.setStackTrace(new StackTraceElement[0]);
        }

        return ex;
    }

}
