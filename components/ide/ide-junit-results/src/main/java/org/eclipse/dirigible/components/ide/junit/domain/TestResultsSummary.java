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
 * Represents a summary of test execution results.
 */
public class TestResultsSummary {

    private int total;
    private int passed;
    private int failed;
    private int skipped;
    private long duration;

    /**
     * Default constructor.
     */
    public TestResultsSummary() {}

    /**
     * Constructor with all fields.
     */
    public TestResultsSummary(int total, int passed, int failed, int skipped, long duration) {
        this.total = total;
        this.passed = passed;
        this.failed = failed;
        this.skipped = skipped;
        this.duration = duration;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public int getPassed() {
        return passed;
    }

    public void setPassed(int passed) {
        this.passed = passed;
    }

    public int getFailed() {
        return failed;
    }

    public void setFailed(int failed) {
        this.failed = failed;
    }

    public int getSkipped() {
        return skipped;
    }

    public void setSkipped(int skipped) {
        this.skipped = skipped;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    @Override
    public String toString() {
        return "TestResultsSummary{" + "total=" + total + ", passed=" + passed + ", failed=" + failed + ", skipped=" + skipped
                + ", duration=" + duration + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        TestResultsSummary that = (TestResultsSummary) o;

        if (total != that.total)
            return false;
        if (passed != that.passed)
            return false;
        if (failed != that.failed)
            return false;
        if (skipped != that.skipped)
            return false;
        return duration == that.duration;
    }

    @Override
    public int hashCode() {
        int result = total;
        result = 31 * result + passed;
        result = 31 * result + failed;
        result = 31 * result + skipped;
        result = 31 * result + (int) (duration ^ (duration >>> 32));
        return result;
    }

}
