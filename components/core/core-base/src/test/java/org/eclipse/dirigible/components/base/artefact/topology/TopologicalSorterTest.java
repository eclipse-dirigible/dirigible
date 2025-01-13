/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.base.artefact.topology;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * The Class TopologicalSorterTest.
 */
public class TopologicalSorterTest {

    /**
     * Test.
     */
    @Test
    public void test() {
        SortableNode nodeA = new SortableNode("A");
        SortableNode nodeB = new SortableNode("B_A", nodeA);
        SortableNode nodeC = new SortableNode("C_A", nodeA);
        SortableNode nodeD = new SortableNode("D_AB", nodeA, nodeB);
        SortableNode nodeE = new SortableNode("E_CD", nodeC, nodeD);
        SortableNode nodeF = new SortableNode("F");
        SortableNode nodeG = new SortableNode("G_F", nodeF);
        SortableNode nodeH = new SortableNode("H_FA", nodeF, nodeA);
        SortableNode nodeI = new SortableNode("I_FC", nodeF, nodeC);

        List<TopologicallySortable> list = new ArrayList<>();
        // list.add(nodeG);
        // list.add(nodeB);
        // list.add(nodeH);
        // list.add(nodeA);
        // list.add(nodeD);
        // list.add(nodeE);
        // list.add(nodeF);
        // list.add(nodeI);
        // list.add(nodeC);

        list.add(nodeC);
        list.add(nodeD);
        list.add(nodeG);
        list.add(nodeB);
        list.add(nodeI);
        list.add(nodeH);
        list.add(nodeA);
        list.add(nodeE);
        list.add(nodeF);



        TopologicalSorter sorter = new TopologicalSorter();
        List<TopologicallySortable> results = sorter.sort(list);
        for (TopologicallySortable sortable : results) {
            System.out.println(sortable.getId());
        }

        assertEquals(results.get(0)
                            .getId(),
                "F");

        // F
        // A
        // H_FA
        // C_A
        // I_FC
        // B_A
        // D_AB
        // E_CD
        // G_F



    }

    /**
     * The Class SortableNode.
     */
    public static class SortableNode implements TopologicallySortable {

        /** The id. */
        public String id;

        /** The dependencies. */
        public List<TopologicallySortable> dependencies;

        /**
         * Instantiates a new sortable node.
         *
         * @param id the id
         * @param dependencies the dependencies
         */
        public SortableNode(String id, TopologicallySortable... dependencies) {
            this.id = id;
            this.dependencies = Arrays.asList(dependencies);
        }

        /**
         * Gets the id.
         *
         * @return the id
         */
        @Override
        public String getId() {
            return this.id;
        }

        /**
         * Gets the dependencies.
         *
         * @return the dependencies
         */
        @Override
        public List<TopologicallySortable> getDependencies() {
            return this.dependencies;
        }

    }

}
