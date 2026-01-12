/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.database.helpers;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.util.Optional;

import org.eclipse.dirigible.components.database.format.ResultSetCsvWriter;
import org.eclipse.dirigible.components.database.format.ResultSetJsonWriter;
import org.eclipse.dirigible.components.database.format.ResultSetMonospacedWriter;

/**
 * The Database Result SetHelper.
 */
public class DatabaseResultSetHelper {

    /**
     * Prints the provided ResultSet to the {@link ResultSetMonospacedWriter} writer.
     *
     * @param resultSet the result set
     * @param limited the limited
     * @param output the output
     * @throws Exception the exception
     */
    public static void print(ResultSet resultSet, boolean limited, OutputStream output) throws Exception {
        ResultSetMonospacedWriter writer = new ResultSetMonospacedWriter();
        writer.setLimited(limited);
        writer.write(resultSet, output);
        output.flush();
    }

    /**
     * Prints the provided ResultSet to the {@link ResultSetJsonWriter} writer.
     *
     * @param resultSet the result set
     * @param limited the limited
     * @param stringify the stringified flag
     * @param output the output
     * @throws Exception the exception
     */
    public static void toJson(ResultSet resultSet, boolean limited, boolean stringify, OutputStream output) throws Exception {
        toJson(resultSet, limited, stringify, output, Optional.empty());
    }

    /**
     * Prints the provided ResultSet to the {@link ResultSetJsonWriter} writer.
     *
     * @param resultSet the result set
     * @param limited the limited
     * @param stringify the stringified flag
     * @param output the output
     * @param formattingParameters formatting parameters
     * @throws Exception the exception
     */
    public static void toJson(ResultSet resultSet, boolean limited, boolean stringify, OutputStream output,
            Optional<FormattingParameters> formattingParameters) throws Exception {
        ResultSetJsonWriter writer = new ResultSetJsonWriter();
        writer.setLimited(limited);
        writer.setStringified(stringify);
        writer.write(resultSet, output, formattingParameters);
        output.flush();
    }

    /**
     * used in ABAP functionality - DirigibleDatabaseClient
     *
     * @param resultSet
     * @param limit
     * @param stringify
     * @return
     * @throws Exception
     */
    public static String toJson(ResultSet resultSet, int limit, boolean stringify) throws Exception {
        ResultSetJsonWriter writer = new ResultSetJsonWriter();

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        writer.setLimited(true);
        writer.setLimit(limit);
        writer.setStringified(stringify);
        writer.write(resultSet, outputStream);
        outputStream.flush();

        return outputStream.toString(StandardCharsets.UTF_8);
    }

    /**
     * Prints the provided ResultSet to the {@link ResultSetCsvWriter} writer.
     *
     * @param resultSet the result set
     * @param limited the limited
     * @param stringify the stringified flag
     * @param output the output
     * @throws Exception the exception
     */
    public static void toCsv(ResultSet resultSet, boolean limited, boolean stringify, OutputStream output) throws Exception {
        ResultSetCsvWriter writer = new ResultSetCsvWriter();
        writer.setLimited(limited);
        writer.setStringified(stringify);
        writer.write(resultSet, output);
        output.flush();
    }

}
