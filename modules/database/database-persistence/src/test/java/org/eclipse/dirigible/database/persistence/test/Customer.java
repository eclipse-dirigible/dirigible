/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.database.persistence.test;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * The Class Customer.
 */
@Table(name = "CUSTOMERS", schema = "FACTORY")
public class Customer {

    /** The id. */
    @Id
    @Column(name = "CUSTOMER_ID", columnDefinition = "INTEGER", nullable = false)
    private int id;

    /** The first name. */
    @Column(name = "CUSTOMER_FIRST_NAME", columnDefinition = "VARCHAR", nullable = false, length = 512)
    private String firstName;

    /** The last name. */
    @Column(name = "CUSTOMER_LAST_NAME", columnDefinition = "VARCHAR", nullable = false, length = 512)
    private String lastName;

    /** The age. */
    @Column(name = "CUSTOMER_AGE", columnDefinition = "INTEGER", nullable = false)
    private int age;

    /**
     * Gets the id.
     *
     * @return the id
     */
    public int getId() {
        return id;
    }

    /**
     * Sets the id.
     *
     * @param id the new id
     */
    public void setId(int id) {
        this.id = id;
    }

    /**
     * Gets the first name.
     *
     * @return the first name
     */
    public String getFirstName() {
        return firstName;
    }

    /**
     * Sets the first name.
     *
     * @param firstName the new first name
     */
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    /**
     * Gets the last name.
     *
     * @return the last name
     */
    public String getLastName() {
        return lastName;
    }

    /**
     * Sets the last name.
     *
     * @param lastName the new last name
     */
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    /**
     * Gets the age.
     *
     * @return the age
     */
    public int getAge() {
        return age;
    }

    /**
     * Sets the age.
     *
     * @param age the new age
     */
    public void setAge(int age) {
        this.age = age;
    }

}
