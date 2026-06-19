/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.sdk.extensions;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.eclipse.dirigible.sdk.component.Component;

/**
 * Registers a client Java class as a contribution to a typed Dirigible extension point. The class
 * must implement the {@link #target()} interface; the runtime validates this at registration time
 * and rejects any class that does not, so consumers can cast results from
 * {@link Extensions#find(Class)} in a type-safe manner without reflection.
 *
 * <p>
 * {@code @Extension} is meta-annotated with {@link Component @Component}, so every contribution is
 * also a managed bean. A consumer can therefore receive all contributions by collection injection
 * (a {@code List<TargetInterface>} constructor parameter or {@code @Inject} field) in addition to
 * the programmatic {@link Extensions#find(Class)} lookup.
 *
 * <p>
 * The {@code target} interface should be marked with {@link ExtensionPoint @ExtensionPoint} and
 * defines the contract the consumer relies on. Its fully qualified name is used as the extension
 * point identifier in the {@code DIRIGIBLE_EXTENSIONS} table — renaming the interface invalidates
 * every persisted reference, so treat the interface FQN as part of the contract.
 *
 * <p>
 * Example:
 *
 * <pre>
 * {@literal @}ExtensionPoint("Order processors")
 * public interface OrderProcessor {
 *     void process(Order order);
 * }
 *
 * {@literal @}Extension(target = OrderProcessor.class, name = "fast-processor")
 * public class FastOrderProcessor implements OrderProcessor {
 *     public void process(Order order) { ... }
 * }
 * </pre>
 *
 * <p>
 * Cross-runtime extension points (where TypeScript / JavaScript modules also contribute to the same
 * logical point) are not expressible in the typed Java surface — a JS module cannot safely satisfy
 * a Java interface contract. Use the TypeScript {@code @Extension} decorator for those.
 */
@Component
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Extension {

    /** The extension point interface this class implements. Must carry {@link ExtensionPoint}. */
    Class<?> target();

    /** Logical name of this contribution. Surfaced in the Extensions UI; not used for lookup. */
    String name() default "";

}
