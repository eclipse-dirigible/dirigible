/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.intent.conversation;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * The Interface IntentConversationRepository.
 */
@Repository("intentConversationRepository")
public interface IntentConversationRepository extends JpaRepository<IntentConversation, Long> {

    /**
     * Finds the conversation of one application in one surface. The tenant is part of the lookup, so a
     * tenant can never reach another tenant's conversation.
     *
     * @param tenantId the owning tenant
     * @param project the workspace project
     * @param surface the authoring surface
     * @param path the intent file path within the project
     * @return the conversation, when there is one
     */
    Optional<IntentConversation> findByTenantIdAndProjectAndSurfaceAndPath(String tenantId, String project, String surface, String path);
}
