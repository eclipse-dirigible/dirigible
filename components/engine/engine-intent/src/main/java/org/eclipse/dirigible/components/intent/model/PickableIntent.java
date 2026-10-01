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

import com.google.gson.annotations.SerializedName;

/**
 * A to-one relation's {@code pickable:} declaration (issue #7496): the rule a TARGET row must meet
 * to be offered by the relation's picker - a customer is picked onto an invoice only once its
 * registration number and address are filled in.
 *
 * <p>
 * Authored as {@code pickable: { when: [registrationNumber != null, address != null], else: mark,
 * message: "..." }}. It governs the picker only: a row failing the rule is shown disabled with the
 * message under its name ({@code mark}, the default - the user learns why it is not offered) or
 * left out ({@code hide}). A stored value that fails the rule still shows its label. The server is
 * not gated by it - a REST client bypasses any picker, so the write-side rule is a {@code checks:}
 * declaration of its own.
 */
public class PickableIntent {

    /**
     * The condition a target row must meet: one comparison, or a list of them (their AND). Each is
     * {@code <target property> ==|!= <literal>}, or {@code <target property> ==|!= null} for presence.
     */
    private Object when;

    /**
     * What happens to a row failing {@link #when}: {@code mark} (the default - listed, disabled, the
     * message shown under its name) or {@code hide} (not listed). {@code else} is a Java keyword, so
     * the field is bound to the YAML key.
     */
    @SerializedName("else")
    private String otherwise;

    /**
     * The text a marked row carries - why it cannot be picked. Optional: absent, the rule itself is
     * shown. A plain string or a map of {@code languages:} codes to the text in that language (issue
     * #7611).
     */
    private Object message;

    public Object getWhen() {
        return when;
    }

    public void setWhen(Object when) {
        this.when = when;
    }

    public String getOtherwise() {
        return otherwise;
    }

    public void setOtherwise(String otherwise) {
        this.otherwise = otherwise;
    }

    /**
     * The text in the module's default language.
     *
     * @return the default-language text, or {@code null} when none is authored
     */
    public String getMessage() {
        return LocalizedText.defaultText(message);
    }

    /**
     * The authored per-language texts - empty for a plain string.
     *
     * @return language code to text
     */
    public java.util.Map<String, String> getMessageTranslations() {
        return LocalizedText.translations(message);
    }

    /**
     * The text exactly as authored - a string or a language map.
     *
     * @return the authored value
     */
    public Object getAuthoredMessage() {
        return message;
    }

    public void setMessage(Object message) {
        this.message = message;
    }
}
