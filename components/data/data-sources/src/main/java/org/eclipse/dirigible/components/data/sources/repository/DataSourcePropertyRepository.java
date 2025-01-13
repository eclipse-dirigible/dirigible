/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.data.sources.repository;

import org.eclipse.dirigible.components.data.sources.domain.DataSourceProperty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * The Interface DataSourcePropertyRepository.
 */
@Repository("datasourcePropertyRepository")
public interface DataSourcePropertyRepository extends JpaRepository<DataSourceProperty, Long> {

}

