/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.sdk.db;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a foreign-key field as a to-one reference and names where the referenced record is read
 * from. The field itself stays the plain key column it always was - the store does not read this
 * annotation and nothing about persistence changes.
 *
 * <p>
 * It exists so that code which holds a record of an entity it does <b>not</b> own can follow the
 * record's references without knowing its schema: the entity declares them on its own class, and a
 * reader discovers them there at runtime. A print feeder of another model reaches
 * <code>&#123;&#123;document.Customer.City.Name&#125;&#125;</code> this way (see
 * {@link org.eclipse.dirigible.sdk.print.PrintRecords}) without a single field of the customer's
 * model being named in its own source.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Reference {

    /**
     * The fully qualified class name of the repository the referenced record is loaded through. A name
     * rather than a class, because the referenced entity may belong to another model whose generated
     * code is not a compile-time dependency of this one.
     *
     * @return the repository class name
     */
    String repository();

    /**
     * The property of the referenced record that names it to a person.
     *
     * @return the label property, empty when the referenced entity has none
     */
    String label() default "";
}
