/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.sdk.capture;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Instant;
import org.eclipse.dirigible.sdk.db.Database;
import org.eclipse.dirigible.sdk.security.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Appends one row per capture attempt to the entity's sibling <code>&lt;TABLE&gt;_CAPTURES</code>
 * table, on its own connection exactly as {@code ImportLedger} does: a failure to append is logged
 * at ERROR and never undoes the record that was created.
 */
public final class CaptureLedger {

    private static final Logger LOGGER = LoggerFactory.getLogger(CaptureLedger.class);
    private static final Gson GSON = new Gson();

    private static final String LEDGER_TABLE_SUFFIX = "_CAPTURES";
    private static final int FILE_NAME_LENGTH = 255;
    private static final int MESSAGE_LENGTH = 2000;
    private static final int RECORD_ID_LENGTH = 64;
    private static final int PROVIDER_LENGTH = 64;
    private static final int MODEL_LENGTH = 128;

    private CaptureLedger() {}

    /**
     * Record a capture attempt.
     *
     * @param table the BASE table name of the entity (the ledger is {@code <table>_CAPTURES})
     * @param result the attempt's outcome
     */
    public static void record(String table, CaptureResult result) {
        String ledgerTable = table + LEDGER_TABLE_SUFFIX;
        String script = "INSERT INTO \"" + ledgerTable
                + "\" (\"CaptureName\", \"RecordId\", \"FileName\", \"Checksum\", \"Provider\", \"Model\", \"InputTokens\", \"OutputTokens\","
                + " \"Status\", \"Message\", \"Flags\", \"Answer\", \"CapturedAt\", \"CapturedBy\")"
                + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = Database.getConnection(); PreparedStatement statement = connection.prepareStatement(script)) {
            statement.setString(1, result.getCaptureName());
            statement.setString(2, truncate(result.getRecordId(), RECORD_ID_LENGTH));
            statement.setString(3, truncate(result.getFileName(), FILE_NAME_LENGTH));
            statement.setString(4, result.getChecksum());
            statement.setString(5, truncate(result.getProvider(), PROVIDER_LENGTH));
            statement.setString(6, truncate(result.getModel(), MODEL_LENGTH));
            statement.setInt(7, result.getInputTokens());
            statement.setInt(8, result.getOutputTokens());
            statement.setString(9, result.getStatus()
                                         .name());
            statement.setString(10, truncate(result.getMessage(), MESSAGE_LENGTH));
            statement.setString(11, GSON.toJson(result.getFlags()));
            statement.setString(12, result.answer());
            statement.setTimestamp(13, Timestamp.from(Instant.now()));
            statement.setString(14, currentUser());
            statement.executeUpdate();
        } catch (Throwable e) {
            LOGGER.error("Cannot record the {} capture of [{}] by [{}] in [{}]", result.getStatus(), result.getFileName(),
                    result.getCaptureName(), ledgerTable, e);
        }
    }

    /**
     * The newest successful capture of a record.
     *
     * @param table the BASE table name of the entity
     * @param recordId the record's id
     * @return a JSON object {@code {captureName, fileName, status, provider, model, capturedAt,
     *         capturedBy, flags}}, or null when the record was not captured
     */
    public static String latest(String table, Object recordId) {
        if (recordId == null) {
            return null;
        }
        String script =
                "SELECT \"CaptureName\", \"FileName\", \"Status\", \"Provider\", \"Model\", \"CapturedAt\", \"CapturedBy\", \"Flags\" FROM \""
                        + table + LEDGER_TABLE_SUFFIX + "\" WHERE \"RecordId\" = ? AND \"Status\" = 'CAPTURED' ORDER BY \"Id\" DESC";
        try (Connection connection = Database.getConnection(); PreparedStatement statement = connection.prepareStatement(script)) {
            statement.setString(1, String.valueOf(recordId));
            try (ResultSet rows = statement.executeQuery()) {
                if (!rows.next()) {
                    return null;
                }
                JsonObject json = new JsonObject();
                json.addProperty("captureName", rows.getString(1));
                json.addProperty("fileName", rows.getString(2));
                json.addProperty("status", rows.getString(3));
                json.addProperty("provider", rows.getString(4));
                json.addProperty("model", rows.getString(5));
                Timestamp at = rows.getTimestamp(6);
                json.addProperty("capturedAt", at == null ? null
                        : at.toInstant()
                            .toString());
                json.addProperty("capturedBy", rows.getString(7));
                json.add("flags", flags(rows.getString(8)));
                return GSON.toJson(json);
            }
        } catch (Throwable e) {
            LOGGER.error("Cannot read the latest capture of [{}] in [{}{}]", recordId, table, LEDGER_TABLE_SUFFIX, e);
            return null;
        }
    }

    private static JsonElement flags(String text) {
        try {
            if (text != null && !text.isBlank()) {
                JsonElement parsed = JsonParser.parseString(text);
                if (parsed.isJsonArray()) {
                    return parsed;
                }
            }
        } catch (RuntimeException e) {
            LOGGER.warn("Unreadable capture flags in the ledger", e);
        }
        return new com.google.gson.JsonArray();
    }

    private static String truncate(String value, int length) {
        return value == null || value.length() <= length ? value : value.substring(0, length);
    }

    private static String currentUser() {
        try {
            return User.getName();
        } catch (Throwable e) {
            LOGGER.warn("Cannot resolve the acting user for a capture ledger entry", e);
            return null;
        }
    }
}
