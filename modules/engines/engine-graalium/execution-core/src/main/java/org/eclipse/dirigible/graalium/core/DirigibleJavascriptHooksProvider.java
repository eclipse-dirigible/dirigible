/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.graalium.core;

import java.util.function.Consumer;

import org.graalvm.polyglot.Context;

/**
 * The Interface DirigibleJavascriptHooksProvider.
 */
public interface DirigibleJavascriptHooksProvider {

    /**
     * Gets the on before context created listener.
     *
     * @return the on before context created listener
     */
    Consumer<Context.Builder> getOnBeforeContextCreatedListener();

    /**
     * Gets the on after context created listener.
     *
     * @return the on after context created listener
     */
    Consumer<Context> getOnAfterContextCreatedListener();


}
