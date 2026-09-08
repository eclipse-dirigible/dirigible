/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.bpm.flowable.delegate;

import java.util.Optional;

import org.eclipse.dirigible.components.base.spring.BeanProvider;
import org.eclipse.dirigible.engine.java.runtime.ClientBeanFactory;
import org.eclipse.dirigible.engine.java.runtime.ClientBeansHolder;
import org.flowable.engine.delegate.JavaDelegate;

/**
 * Wires a client {@link JavaDelegate} through the client bean container, for the two paths that
 * instantiate one: {@code flowable:class} ({@link ResilientClassDelegate}) and
 * {@code flowable:delegateExpression="${JavaTask}"} ({@link DirigibleJavaCallDelegate}).
 *
 * <p>
 * A delegate is created by Flowable, so it is never a container-owned bean and {@code @Inject}
 * could not reach it; {@link ClientBeanFactory#createUnmanaged(Class)} constructs it with the
 * container's own injection rules without registering it. Empty means the class declares no
 * injection point, and the caller keeps its own plain instantiation.
 *
 * <p>
 * The container is reached through {@link BeanProvider} rather than injected, because both callers
 * are created by Flowable too: a {@code ClassDelegate} is declared {@code Serializable} and cached
 * in the process-definition cache, so it must not hold a Spring bean reference. The module already
 * does this in {@link BPMTask}.
 */
final class ClientDelegateBeans {

    private ClientDelegateBeans() {}

    /**
     * The container-wired instance of {@code type}, or empty when there is nothing to wire.
     *
     * @param <T> the delegate type
     * @param type the client class Flowable is about to instantiate
     * @return the wired instance, or empty when the class declares no injection point, Spring is not
     *         initialized (a standalone engine), or no client generation has been built yet
     * @throws RuntimeException if a declared dependency cannot be satisfied unambiguously — the failure
     *         belongs to the step being executed, exactly as a failing {@code Beans.get} does
     */
    static <T> Optional<T> createUnmanaged(Class<T> type) {
        if (!BeanProvider.isInitialzed()) {
            return Optional.empty();
        }
        ClientBeanFactory container = BeanProvider.getOptionalBean(ClientBeansHolder.class)
                                                  .map(ClientBeansHolder::current)
                                                  .orElse(null);
        return container == null ? Optional.empty() : container.createUnmanaged(type);
    }

}
