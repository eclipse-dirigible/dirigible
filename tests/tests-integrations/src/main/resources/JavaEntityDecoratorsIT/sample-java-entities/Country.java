/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package demo;

import org.eclipse.dirigible.engine.java.annotations.Column;
import org.eclipse.dirigible.engine.java.annotations.Documentation;
import org.eclipse.dirigible.engine.java.annotations.Entity;
import org.eclipse.dirigible.engine.java.annotations.GeneratedValue;
import org.eclipse.dirigible.engine.java.annotations.GenerationType;
import org.eclipse.dirigible.engine.java.annotations.Id;
import org.eclipse.dirigible.engine.java.annotations.Table;

@Entity
@Table(name = "COUNTRIES")
@Documentation("Sample Country Entity used by JavaEntityDecoratorsIT")
public class Country {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Documentation("Auto-generated primary key")
    public Long id;

    @Column(name = "CODE2", length = 2)
    public String code2;

    @Column(name = "CODE3", length = 3)
    public String code3;

    @Column(name = "NUMERIC", length = 3)
    public String numericCode;

    @Column(name = "NAME", length = 128)
    @Documentation("Official short name")
    public String name;

}
