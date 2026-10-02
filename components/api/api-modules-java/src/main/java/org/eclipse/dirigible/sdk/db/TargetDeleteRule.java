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

/**
 * Extension point through which the records referencing another record decide what its delete does
 * to them (intent {@code whenTargetDeleted: restrict | nullify | cascade} on a to-one relation).
 *
 * <p>
 * The referenced entity's repository cannot know which entities point at it - another model may
 * reference it, generated later or by another team - so the REFERENCING side contributes: its
 * generated repository implements this interface, and every repository asks all contributions when
 * it deletes. First every contribution is asked whether it restricts the delete, before anything is
 * written; a restricting reference refuses it with {@link DeleteRestrictionException}. Only then,
 * in the delete's own transaction, does each contribution release its references - clearing the
 * foreign key or deleting the referencing record - and the record itself is deleted. A contribution
 * is a {@code @Component} bean like any other extension, found through
 * {@link org.eclipse.dirigible.sdk.extensions.Extensions#find(Class)}.
 */
public interface TargetDeleteRule {

    /**
     * How many of this contribution's records reference the given record through a relation that
     * restricts its delete.
     *
     * @param targetEntity the fully qualified class name of the entity being deleted
     * @param targetId the primary key of the record being deleted
     * @return the number of restricting references; {@code 0} when there are none, or when this
     *         contribution does not reference {@code targetEntity} at all
     */
    int countRestricting(String targetEntity, Object targetId);

    /**
     * Releases this contribution's references to a record that is being deleted: clears the foreign key
     * of each referencing record whose relation nullifies, and deletes each referencing record whose
     * relation cascades. Runs inside the delete's own transaction, after no contribution restricted it.
     *
     * @param targetEntity the fully qualified class name of the entity being deleted
     * @param targetId the primary key of the record being deleted
     */
    void release(String targetEntity, Object targetId);

    /**
     * How a refusal names the referencing records, as a person reading the page knows them.
     *
     * @return the referencing entity's label, e.g. {@code Expense Claim}
     */
    String referencingLabel();
}
