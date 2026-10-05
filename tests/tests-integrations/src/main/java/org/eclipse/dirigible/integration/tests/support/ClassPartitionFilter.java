/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.integration.tests.support;

import java.util.Optional;

import org.junit.platform.engine.FilterResult;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestSource;
import org.junit.platform.engine.support.descriptor.ClassSource;
import org.junit.platform.engine.support.descriptor.MethodSource;
import org.junit.platform.launcher.PostDiscoveryFilter;

/**
 * Splits the selected test classes into {@code n} disjoint partitions and keeps only partition
 * {@code k}, given as the system property {@value #PARTITION_PROPERTY} in the form {@code k/n} (for
 * example {@code 1/2}). Without the property every test is kept.
 *
 * <p>
 * The partition of a class is a hash of its top-level class name, so the partitions are disjoint
 * and together complete by construction: a new IT lands in exactly one of them without anyone
 * having to tag it, and a nested class always runs with its enclosing one. CI uses it to split a
 * tag-selected shard whose classes cost about the same each - the HTTP-level "api" shard, which has
 * no long pole a tag could move out.
 *
 * <p>
 * Registered through {@code META-INF/services/org.junit.platform.launcher.PostDiscoveryFilter}, so
 * the JUnit Platform launcher applies it after the tag filter of {@code -Dit.groups}.
 */
public final class ClassPartitionFilter implements PostDiscoveryFilter {

    /** The system property that selects the partition, as {@code k/n}. */
    public static final String PARTITION_PROPERTY = "dirigible.it.partition";

    private final Optional<Partition> partition;

    /**
     * Reads the partition from {@value #PARTITION_PROPERTY}; called by the launcher's service loader.
     */
    public ClassPartitionFilter() {
        this(System.getProperty(PARTITION_PROPERTY));
    }

    ClassPartitionFilter(String partition) {
        this.partition = Partition.parse(partition);
    }

    @Override
    public FilterResult apply(TestDescriptor descriptor) {
        if (partition.isEmpty()) {
            return FilterResult.included("no partition requested");
        }
        Optional<String> className = topLevelClassName(descriptor);
        if (className.isEmpty()) {
            return FilterResult.included("not a class or method");
        }
        return partition.get()
                        .contains(className.get()) ? FilterResult.included("in partition " + partition.get())
                                : FilterResult.excluded("not in partition " + partition.get());
    }

    private static Optional<String> topLevelClassName(TestDescriptor descriptor) {
        Optional<String> className = descriptor.getSource()
                                               .flatMap(ClassPartitionFilter::className);
        return className.map(name -> {
            int nested = name.indexOf('$');
            return nested < 0 ? name : name.substring(0, nested);
        });
    }

    private static Optional<String> className(TestSource source) {
        if (source instanceof ClassSource classSource) {
            return Optional.of(classSource.getClassName());
        }
        if (source instanceof MethodSource methodSource) {
            return Optional.of(methodSource.getClassName());
        }
        return Optional.empty();
    }

    record Partition(int index, int count) {

        static Optional<Partition> parse(String value) {
            if (value == null || value.isBlank()) {
                return Optional.empty();
            }
            String[] parts = value.trim()
                                  .split("/");
            if (parts.length != 2) {
                throw invalid(value);
            }
            int index;
            int count;
            try {
                index = Integer.parseInt(parts[0].trim());
                count = Integer.parseInt(parts[1].trim());
            } catch (NumberFormatException ex) {
                throw invalid(value);
            }
            if (count < 1 || index < 1 || index > count) {
                throw invalid(value);
            }
            return Optional.of(new Partition(index, count));
        }

        private static IllegalArgumentException invalid(String value) {
            return new IllegalArgumentException(
                    "Invalid " + PARTITION_PROPERTY + " [" + value + "]: expected k/n with 1 <= k <= n, e.g. 1/2");
        }

        boolean contains(String className) {
            return Math.floorMod(className.hashCode(), count) == index - 1;
        }

        @Override
        public String toString() {
            return index + "/" + count;
        }
    }

}
