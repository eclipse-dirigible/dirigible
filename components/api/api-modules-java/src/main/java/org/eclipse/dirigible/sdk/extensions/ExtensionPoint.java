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

/**
 * Marks an interface as a Dirigible extension point. Implementations declare their contribution via
 * {@link Extension @Extension(target = MyExtensionPoint.class)} and a consumer retrieves them with
 * {@link Extensions#find(Class)} — the call returns typed instances that can be invoked directly
 * without reflection.
 *
 * <p>
 * The annotation itself is metadata only — the contract is the interface's methods. Apply it to
 * give the Extensions UI a human-readable label and to document intent at the declaration site.
 *
 * <p>
 * The interface's fully qualified name is the persisted extension-point identifier; renaming it
 * invalidates every {@code DIRIGIBLE_EXTENSIONS} row pointing at the old FQN, so treat it as part
 * of the contract.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ExtensionPoint {

    /** Human-readable label shown in the Extensions UI. Defaults to the interface's simple name. */
    String value() default "";

}
