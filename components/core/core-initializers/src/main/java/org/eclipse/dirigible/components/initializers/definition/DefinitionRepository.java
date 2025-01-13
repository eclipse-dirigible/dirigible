/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.initializers.definition;

import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * The Interface DefinitionRepository.
 */
@Repository("definitionRepository")
public interface DefinitionRepository extends JpaRepository<Definition, Long> {

    /**
     * Update checksums.
     *
     * @param checksum the checksum
     * @param types the types
     */
    @Modifying
    @Transactional
    @Query("update Definition d set d.checksum = :checksum where d.type in :types")
    void updateChecksums(String checksum, Set<String> types);

}
