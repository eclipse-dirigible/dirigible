/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.ftp.repository;

import java.util.List;

import org.eclipse.dirigible.components.engine.ftp.domain.FtpUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * The Interface FtpUserRepository.
 */
@Repository("ftpUserRepository")
public interface FtpUserRepository extends JpaRepository<FtpUser, Long> {

    /**
     * Find all by username.
     *
     * @param username the username
     * @return the list
     */
    List<FtpUser> findAllByUsername(String username);

    /**
     * Delete by username.
     *
     * @param username the username
     */
    void deleteByUsername(String username);

}
