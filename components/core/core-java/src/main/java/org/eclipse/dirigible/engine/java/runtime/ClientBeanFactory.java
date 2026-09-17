/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.engine.java.runtime;

import java.util.Optional;

/**
 * The client bean container as seen by platform code that has to instantiate a client class the
 * container does <em>not</em> own - today a client {@code JavaDelegate}, which Flowable creates
 * itself and which therefore never becomes a bean.
 *
 * <p>
 * Extends the read view {@link ClientBeanResolver} rather than widening it, so the three methods
 * the client-facing SDK facade {@code org.eclipse.dirigible.sdk.component.Beans} exposes stay
 * exactly what they are: handing client code a factory for container-wired instances nobody owns
 * would invite lifecycles the container cannot manage.
 *
 * <p>
 * Implemented by the engine's component container and published through {@link ClientBeansHolder}.
 * It lives in {@code core-java} because {@code engine-bpm-flowable} - the one consumer - depends on
 * this module and deliberately not on {@code engine-java} (that would close the cycle
 * {@code engine-bpm-flowable -> engine-java -> api-modules-java -> api-bpm -> engine-bpm-flowable}).
 */
public interface ClientBeanFactory extends ClientBeanResolver {

    /**
     * Create an instance the container does <b>not</b> own and will not cache: the selected
     * constructor's parameters and every {@code @Inject} field are resolved against the live
     * generation's singletons (never by constructing a new bean), then {@code @PostConstruct} runs.
     *
     * <p>
     * The resolution rules are the container's own - single constructor, else the {@code @Inject} one,
     * else the no-arg one; by type, with the parameter / field name disambiguating several candidates;
     * a {@code Collection} injection point receiving every assignable bean in registration order - so a
     * delegate wires exactly like a {@code @Component}. An unsatisfiable or ambiguous dependency is
     * refused rather than guessed, by throwing.
     *
     * <p>
     * {@code @PreDestroy} is never invoked: nothing owns this instance's lifecycle.
     *
     * @param <T> the instance type
     * @param type the class to construct
     * @return the wired instance, or empty when the class declares nothing the container would do - no
     *         constructor parameters, no {@code @Inject} field and no {@code @PostConstruct} method -
     *         in which case the caller keeps its own plain instantiation, which is equivalent by
     *         construction
     * @throws RuntimeException if the class cannot be constructed, or a dependency cannot be satisfied
     *         unambiguously
     */
    <T> Optional<T> createUnmanaged(Class<T> type);

}
