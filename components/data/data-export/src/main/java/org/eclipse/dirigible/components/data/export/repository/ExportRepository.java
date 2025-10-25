/*
 * Copyright (c) 2010-2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.data.export.repository;

import java.util.Optional;

import org.eclipse.dirigible.components.data.export.domain.Export;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * The Interface ExportRepository.
 */
@Repository("exportsRepository")
public interface ExportRepository extends JpaRepository<Export, Long> {

    Optional<Export> findByName(String name);

}
