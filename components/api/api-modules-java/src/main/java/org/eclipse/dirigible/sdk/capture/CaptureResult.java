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

import java.util.List;

/**
 * The outcome of one capture attempt - a draft record created, an answer that could not become a
 * record, or a provider call that failed.
 */
public final class CaptureResult {

    /** How a capture ended. */
    public enum Status {
        /** A draft record was created. */
        CAPTURED,
        /** The answer could not become a record; nothing was saved. */
        REJECTED,
        /** The provider call failed; nothing was saved. */
        FAILED
    }

    private final String captureName;
    private final String fileName;
    private final String checksum;
    private final Status status;
    private final String message;
    private final String recordId;
    private final List<CaptureFlag> flags;
    private final String provider;
    private final String model;
    private final int inputTokens;
    private final int outputTokens;
    // transient: Json.stringify(result) must not echo the raw provider answer to the browser
    private final transient String answer;

    private CaptureResult(String captureName, String fileName, String checksum, Status status, String message, String recordId,
            List<CaptureFlag> flags, CaptureReading reading) {
        this.captureName = captureName;
        this.fileName = fileName;
        this.checksum = checksum;
        this.status = status;
        this.message = message;
        this.recordId = recordId;
        this.flags = flags == null ? List.of() : List.copyOf(flags);
        this.provider = reading == null ? null : reading.getProvider();
        this.model = reading == null ? null : reading.getModel();
        this.inputTokens = reading == null ? 0 : reading.getInputTokens();
        this.outputTokens = reading == null ? 0 : reading.getOutputTokens();
        this.answer = reading == null ? null : reading.getAnswer();
    }

    /**
     * A document that became a draft record.
     *
     * @param captureName the declared capture
     * @param fileName the file's name
     * @param checksum the file's checksum
     * @param reading what the provider read
     * @param recordId the created record's id
     * @param flags what a person must look at
     * @return the result
     */
    public static CaptureResult captured(String captureName, String fileName, String checksum, CaptureReading reading, Object recordId,
            List<CaptureFlag> flags) {
        return new CaptureResult(captureName, fileName, checksum, Status.CAPTURED, null, recordId == null ? null : String.valueOf(recordId),
                flags, reading);
    }

    /**
     * An answer that could not become a record.
     *
     * @param captureName the declared capture
     * @param fileName the file's name
     * @param checksum the file's checksum
     * @param reading what the provider read, may be null
     * @param message why it was rejected
     * @return the result
     */
    public static CaptureResult rejected(String captureName, String fileName, String checksum, CaptureReading reading, String message) {
        return new CaptureResult(captureName, fileName, checksum, Status.REJECTED, message, null, null, reading);
    }

    /**
     * A provider call that failed.
     *
     * @param captureName the declared capture
     * @param fileName the file's name
     * @param checksum the file's checksum
     * @param message why it failed
     * @return the result
     */
    public static CaptureResult failed(String captureName, String fileName, String checksum, String message) {
        return new CaptureResult(captureName, fileName, checksum, Status.FAILED, message, null, null, null);
    }

    /**
     * The declared capture.
     *
     * @return the capture name
     */
    public String getCaptureName() {
        return captureName;
    }

    /**
     * The file's name.
     *
     * @return the file name
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * The file's checksum.
     *
     * @return the checksum
     */
    public String getChecksum() {
        return checksum;
    }

    /**
     * How the capture ended.
     *
     * @return the status
     */
    public Status getStatus() {
        return status;
    }

    /**
     * Why the capture was rejected or failed.
     *
     * @return the message, or null
     */
    public String getMessage() {
        return message;
    }

    /**
     * The created record's id.
     *
     * @return the id as text, or null
     */
    public String getRecordId() {
        return recordId;
    }

    /**
     * What a person must look at.
     *
     * @return the flags, never null
     */
    public List<CaptureFlag> getFlags() {
        return flags;
    }

    /**
     * The provider that read the document.
     *
     * @return the provider name, or null
     */
    public String getProvider() {
        return provider;
    }

    /**
     * The model that read the document.
     *
     * @return the model, or null
     */
    public String getModel() {
        return model;
    }

    /**
     * Tokens read.
     *
     * @return the input token count
     */
    public int getInputTokens() {
        return inputTokens;
    }

    /**
     * Tokens written.
     *
     * @return the output token count
     */
    public int getOutputTokens() {
        return outputTokens;
    }

    /**
     * The raw JSON the provider returned, for the ledger. Deliberately not a getter so the result can
     * be serialised to the browser without it.
     *
     * @return the answer, or null
     */
    public String answer() {
        return answer;
    }
}
