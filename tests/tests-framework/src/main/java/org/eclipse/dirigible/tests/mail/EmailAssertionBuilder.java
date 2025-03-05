/*
 * Copyright (c) 2010-2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.tests.mail;

public class EmailAssertionBuilder {

    private String from;
    private String to;
    private String subject;
    private String toContainExactBody;
    private String bodyRegex;

    public EmailAssertion build() {
        return new EmailAssertion(from, to, subject, toContainExactBody, bodyRegex);
    }

    public EmailAssertionBuilder expectedFrom(String from) {
        this.from = from;
        return this;
    }

    public EmailAssertionBuilder expectedTo(String to) {
        this.to = to;
        return this;
    }

    public EmailAssertionBuilder expectedSubject(String subject) {
        this.subject = subject;
        return this;
    }

    public EmailAssertionBuilder expectedToContainBody(String body) {
        this.toContainExactBody = body;
        return this;
    }

    public EmailAssertionBuilder expectedBodyRegex(String bodyRegex) {
        this.bodyRegex = bodyRegex;
        return this;
    }
}
