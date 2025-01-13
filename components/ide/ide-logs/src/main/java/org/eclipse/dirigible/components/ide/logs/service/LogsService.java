/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.ide.logs.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.eclipse.dirigible.commons.api.helpers.GsonHelper;
import org.eclipse.dirigible.commons.config.Configuration;
import org.eclipse.dirigible.components.ide.logs.dto.LogInfo;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;

/**
 * The Class LogsProcessor.
 */
@Service
public class LogsService {

    /** The Constant DIRIGIBLE_OPERATIONS_LOGS_ROOT_FOLDER_DEFAULT. */
    private static final String DIRIGIBLE_OPERATIONS_LOGS_ROOT_FOLDER_DEFAULT = "DIRIGIBLE_OPERATIONS_LOGS_ROOT_FOLDER_DEFAULT";

    /** The Constant CATALINA_BASE. */
    private static final String CATALINA_BASE = "CATALINA_BASE";

    /** The Constant CATALINA_HOME. */
    private static final String CATALINA_HOME = "CATALINA_HOME";

    /** The Constant DEFAULT_LOGS_FOLDER. */
    private static final String DEFAULT_LOGS_FOLDER = "logs";

    /** The Constant DEFAULT_LOGS_LOCATION. */
    private static final String DEFAULT_LOGS_LOCATION = ".." + File.separator + DEFAULT_LOGS_FOLDER;

    /**
     * List.
     *
     * @return the string
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public List<String> list() throws IOException {
        String logsFolder = getLogsLocation();
        List<String> fileNames = new ArrayList<>();
        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(Paths.get(logsFolder))) {
            for (Path path : directoryStream) {
                String name = path.toString();
                fileNames.add(name.substring(name.lastIndexOf(File.separator) + 1));
            }
        } catch (IOException e) {
            throw e;
        }
        return fileNames;
    }

    /**
     * Gets the.
     *
     * @param file the file
     * @return the string
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public String get(String file) throws IOException {
        String logsFolder = getLogsLocation();
        Path path = Paths.get(logsFolder, file);
        FileInputStream input = null;
        try {
            input = new FileInputStream(path.toFile());
            String content = new String(IOUtils.toByteArray(input), StandardCharsets.UTF_8);
            return content;
        } finally {
            if (input != null) {
                input.close();
            }
        }
    }

    /**
     * Gets the logs location.
     *
     * @return the logs location
     */
    private String getLogsLocation() {
        String logsFolder = Configuration.get(DIRIGIBLE_OPERATIONS_LOGS_ROOT_FOLDER_DEFAULT);
        if (logsFolder != null && !logsFolder.equals("")) {
            return logsFolder;
        }
        logsFolder = Configuration.get(CATALINA_BASE);
        if (logsFolder != null && !logsFolder.equals("")) {
            return logsFolder + File.separator + DEFAULT_LOGS_FOLDER;
        }
        logsFolder = Configuration.get(CATALINA_HOME);
        if (logsFolder != null && !logsFolder.equals("")) {
            return logsFolder + File.separator + DEFAULT_LOGS_FOLDER;
        }
        return DEFAULT_LOGS_LOCATION;
    }

    /**
     * List loggers.
     *
     * @return the list
     */
    public List<LogInfo> listLoggers() {
        List<LogInfo> result = new ArrayList<LogInfo>();
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        List<Logger> loggers = loggerContext.getLoggerList();
        for (Logger logger : loggers) {
            LogInfo logInfo = new LogInfo(logger.getName(), logger.getLevel() == null ? "-"
                    : logger.getLevel()
                            .toString());
            result.add(logInfo);
        }
        return result;
    }

    /**
     * Gets the severity.
     *
     * @param loggerName the logger name
     * @return the severity
     */
    public String getSeverity(String loggerName) {
        org.slf4j.Logger logger = LoggerFactory.getLogger(loggerName);
        if (logger.isTraceEnabled()) {
            return Level.TRACE.toString();
        } else if (logger.isDebugEnabled()) {
            return Level.DEBUG.toString();
        } else if (logger.isInfoEnabled()) {
            return Level.INFO.toString();
        } else if (logger.isWarnEnabled()) {
            return Level.WARN.toString();
        } else if (logger.isErrorEnabled()) {
            return Level.ERROR.toString();
        }

        return "Unknown logger: " + loggerName;
    }

    /**
     * Sets the severity.
     *
     * @param loggerName the logger name
     * @param logLevel the log level
     * @return the object
     */
    public String setSeverity(String loggerName, String logLevel) {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        Logger logger = loggerContext.getLogger(loggerName);
        Level level = Level.toLevel(logLevel);
        if (!level.equals(logger.getLevel())) {
            logger.setLevel(level);
        } else {
            logger.setLevel(null);
        }
        return getSeverity(loggerName);
    }

}
