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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestSource;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.descriptor.AbstractTestDescriptor;
import org.junit.platform.engine.support.descriptor.ClassSource;
import org.junit.platform.engine.support.descriptor.MethodSource;

/**
 * The partitions CI splits a shard into must be disjoint and together complete - a class in none of
 * them would silently not run, which is the failure mode a tag- or name-based split invites.
 */
class ClassPartitionFilterTest {

    private static final List<String> CLASS_NAMES = IntStream.range(0, 200)
                                                             .mapToObj(i -> "org.eclipse.dirigible.integration.tests.api.Sample" + i + "IT")
                                                             .toList();

    @Test
    void withoutAPartitionEveryClassIsIncluded() {
        ClassPartitionFilter filter = new ClassPartitionFilter(null);

        CLASS_NAMES.forEach(name -> assertTrue(included(filter, classDescriptor(name)), name));
    }

    @Test
    void aBlankPartitionIncludesEveryClass() {
        ClassPartitionFilter filter = new ClassPartitionFilter(" ");

        CLASS_NAMES.forEach(name -> assertTrue(included(filter, classDescriptor(name)), name));
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3})
    void everyClassIsInExactlyOnePartition(int count) {
        List<ClassPartitionFilter> partitions = IntStream.rangeClosed(1, count)
                                                         .mapToObj(index -> new ClassPartitionFilter(index + "/" + count))
                                                         .toList();

        for (String name : CLASS_NAMES) {
            long including = partitions.stream()
                                       .filter(partition -> included(partition, classDescriptor(name)))
                                       .count();
            assertEquals(1, including, name);
        }
    }

    @Test
    void aNestedClassAndTheMethodsOfAClassFollowTheirTopLevelClass() {
        for (String name : CLASS_NAMES) {
            ClassPartitionFilter filter = new ClassPartitionFilter("1/2");
            boolean topLevel = included(filter, classDescriptor(name));

            assertEquals(topLevel, included(filter, classDescriptor(name + "$QueueTest")), name);
            assertEquals(topLevel, included(filter, descriptor(MethodSource.from(name + "$QueueTest", "sends"))), name);
            assertEquals(topLevel, included(filter, descriptor(MethodSource.from(name, "runs"))), name);
        }
    }

    @Test
    void aDescriptorWithoutAClassIsIncluded() {
        assertTrue(included(new ClassPartitionFilter("2/2"), descriptor(null)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"1", "0/2", "3/2", "1/0", "a/2", "1/2/3", "-1/2"})
    void aMalformedPartitionIsRefused(String partition) {
        assertThrows(IllegalArgumentException.class, () -> new ClassPartitionFilter(partition));
    }

    private static boolean included(ClassPartitionFilter filter, TestDescriptor descriptor) {
        return filter.apply(descriptor)
                     .included();
    }

    private static TestDescriptor classDescriptor(String className) {
        return descriptor(ClassSource.from(className));
    }

    private static TestDescriptor descriptor(TestSource source) {
        return new AbstractTestDescriptor(UniqueId.forEngine("test"), "test", source) {
            @Override
            public Type getType() {
                return Type.CONTAINER;
            }
        };
    }

}
