/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.intent.model;

import java.util.List;

/**
 * An entity's {@code share:} (issue #8613): the declaration that the ISSUED COPY of its records may
 * be handed to a reader without an account, through a link a user creates on the record.
 *
 * <pre>
 * share:
 *   when: "Status == ISSUED"     # required: only a record in this state can get a link
 *   copy: SalesInvoiceCopy       # required: the function: Snapshot child whose stored file is served
 *   roles: [SalesInvoiceShare]   # required: who may create and revoke links; [] admits nobody
 *   expiresAfterDays: 0          # optional, default 0 = permanent; a tenant can only shorten it
 *   expiry:                      # optional: the first entry whose when: the record meets sets the days
 *     - { when: "Customer.Kind == 2", days: 30 }
 * </pre>
 *
 * Without the block no link can exist for the entity - there is no switch anywhere that makes an
 * undeclared entity shareable. With it, a link still serves only the stored copy: never the live
 * record, never a re-render, never another file.
 */
public class ShareIntent {

    /** The longest life a declaration may give a link - ten years, beyond which it is permanent. */
    public static final int MAX_EXPIRES_AFTER_DAYS = 3650;

    /** The record condition a link can be created in - a comparison, or a list of them (ANDed). */
    private Object when;

    /** The {@code function: Snapshot} child whose stored file a link serves. */
    private String copy;

    /** The link's life in days - {@code 0} or absent for a permanent link. */
    private Object expiresAfterDays;

    /** The roles that may create and revoke links; {@code null} when the key is not declared. */
    private List<String> roles;

    /** The life by record (#8620): the first entry whose condition holds, else expiresAfterDays. */
    private List<Expiry> expiry;

    public Object getWhen() {
        return when;
    }

    public void setWhen(Object when) {
        this.when = when;
    }

    public String getCopy() {
        return copy;
    }

    public void setCopy(String copy) {
        this.copy = copy;
    }

    public Object getExpiresAfterDays() {
        return expiresAfterDays;
    }

    public void setExpiresAfterDays(Object expiresAfterDays) {
        this.expiresAfterDays = expiresAfterDays;
    }

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }

    public List<Expiry> getExpiry() {
        return expiry;
    }

    public void setExpiry(List<Expiry> expiry) {
        this.expiry = expiry;
    }

    /**
     * The declared life as whole days.
     *
     * @return the days ({@code 0} when the key is absent), or {@code null} when the value is not a
     *         whole number - the parser reports it
     */
    public Integer expiresAfterDaysOrNull() {
        return expiresAfterDays == null ? Integer.valueOf(0) : wholeDays(expiresAfterDays);
    }

    /** A value as whole days, or {@code null} when it is none. */
    private static Integer wholeDays(Object value) {
        if (value instanceof Number number && number.doubleValue() == Math.rint(number.doubleValue())
                && Math.abs(number.doubleValue()) <= Integer.MAX_VALUE) {
            return number.intValue();
        }
        return null;
    }

    /**
     * One entry of {@code expiry:} (#8620): a link created while the record meets {@code when} lives
     * {@code days} - read when the link is created, so the link keeps the life it was given.
     */
    public static class Expiry {

        /**
         * The record condition - its own property or a one-hop {@code Relation.field}, or a list (ANDed).
         */
        private Object when;

        /** The life in days, {@code 0} for a permanent link. */
        private Object days;

        public Object getWhen() {
            return when;
        }

        public void setWhen(Object when) {
            this.when = when;
        }

        public Object getDays() {
            return days;
        }

        public void setDays(Object days) {
            this.days = days;
        }

        /**
         * The declared life as whole days.
         *
         * @return the days, or {@code null} when absent or not a whole number - the parser reports it
         */
        public Integer daysOrNull() {
            return wholeDays(days);
        }
    }
}
