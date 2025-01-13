/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.base.converters;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.persistence.AttributeConverter;

/**
 * The Class ListOfStringsToCsvConverter.
 */
public class ListOfStringsToCsvConverter implements AttributeConverter<List<String>, String> {

    /**
     * Convert to database column.
     *
     * @param attribute the attribute
     * @return the string
     */
    @Override
    public String convertToDatabaseColumn(List<String> attribute) {
        return attribute == null ? null
                : attribute.stream()
                           .map(String::trim)
                           .collect(Collectors.joining(","));
    }

    /**
     * Convert to entity attribute.
     *
     * @param data the data
     * @return the list
     */
    @Override
    public List<String> convertToEntityAttribute(String data) {
        return data == null ? null
                : Arrays.stream(data.split(","))
                        .map(String::trim)
                        .collect(Collectors.toList());
    }

}
