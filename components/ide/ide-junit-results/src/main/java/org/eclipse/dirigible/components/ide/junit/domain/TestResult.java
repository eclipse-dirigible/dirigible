/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.ide.junit.domain;

/**
 * Represents a single test result.
 */
public class TestResult {

    private String name;
    private String status; // passed, failed, skipped
    private long duration;
    private String error;
    private String stackTrace;
    private long timestamp;

    /**
     * Default constructor.
     */
    public TestResult() {}

    /**
     * Constructor with all fields.
     */
    public TestResult(String name, String status, long duration, String error, String stackTrace, long timestamp) {
        this.name = name;
        this.status = status;
        this.duration = duration;
        this.error = error;
        this.stackTrace = stackTrace;
        this.timestamp = timestamp;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getStackTrace() {
        return stackTrace;
    }

    public void setStackTrace(String stackTrace) {
        this.stackTrace = stackTrace;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "TestResult{" + "name='" + name + '\'' + ", status='" + status + '\'' + ", duration=" + duration + ", error='" + error + '\''
                + ", stackTrace='" + stackTrace + '\'' + ", timestamp=" + timestamp + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        TestResult that = (TestResult) o;

        if (duration != that.duration)
            return false;
        if (timestamp != that.timestamp)
            return false;
        if (name != null ? !name.equals(that.name) : that.name != null)
            return false;
        if (status != null ? !status.equals(that.status) : that.status != null)
            return false;
        if (error != null ? !error.equals(that.error) : that.error != null)
            return false;
        return stackTrace != null ? stackTrace.equals(that.stackTrace) : that.stackTrace == null;
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (status != null ? status.hashCode() : 0);
        result = 31 * result + (int) (duration ^ (duration >>> 32));
        result = 31 * result + (error != null ? error.hashCode() : 0);
        result = 31 * result + (stackTrace != null ? stackTrace.hashCode() : 0);
        result = 31 * result + (int) (timestamp ^ (timestamp >>> 32));
        return result;
    }

}
