/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.parsers.document.binding;

import java.math.BigDecimal;
import java.util.Locale;

/**
 * Spells a money amount out in one language - the {@code :words(<currency>)} placeholder format.
 * Pure text, no storage: the language comes from the folder the template was read from.
 */
interface AmountInWords {

    /** The strategy of a language with none: every amount renders empty. */
    AmountInWords NONE = (amount, currency) -> null;

    /**
     * The amount in words.
     *
     * @param amount the amount, any scale
     * @param currency the ISO 4217 code of the currency the amount is in
     * @return the text, or {@code null} when this language cannot name the currency or the amount
     */
    String render(BigDecimal amount, String currency);

    /**
     * The strategy of a template language.
     *
     * @param language the language code of the template folder ({@code bg}, {@code en}, ...), may be
     *        {@code null}
     * @return the language's strategy, or {@link #NONE}
     */
    static AmountInWords forLanguage(String language) {
        if (language == null) {
            return NONE;
        }
        return switch (language.trim()
                               .toLowerCase(Locale.ROOT)) {
            case "bg" -> new BulgarianAmountInWords();
            case "en" -> new EnglishAmountInWords();
            default -> NONE;
        };
    }
}
