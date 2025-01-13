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

import org.eclipse.dirigible.components.base.artefact.ArtefactPhase;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The Class TopologicalDepleterTest.
 */
public class TopologicalDepleterTest {

    /**
     * The Class DepletableNode.
     */
    public static class DepletableNode implements TopologicallyDepletable {

        /** The id. */
        public String id;

        /** The completable. */
        int completable;

        /**
         * Instantiates a new depletable node.
         *
         * @param id the id
         * @param completable the completable
         */
        public DepletableNode(String id, int completable) {
            this.id = id;
            this.completable = completable;
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
         * Complete.
         *
         * @param flow the flow
         * @return true, if successful
         */
        @Override
        public boolean complete(ArtefactPhase flow) {
            if (completable == 0) {
                System.out.println(this.id);
                return true;
            }
            --completable;
            return false;
        }

    }

    /**
     * Test depleted.
     */
    @Test
    public void testDepleted() {
        System.out.println("Depleted:");

        DepletableNode nodeA = new DepletableNode("A", 0);
        DepletableNode nodeB = new DepletableNode("B", 3);
        DepletableNode nodeC = new DepletableNode("C", 2);
        DepletableNode nodeD = new DepletableNode("D", 0);
        DepletableNode nodeE = new DepletableNode("E", 4);
        DepletableNode nodeF = new DepletableNode("F", 3);
        DepletableNode nodeG = new DepletableNode("G", 5);
        DepletableNode nodeH = new DepletableNode("H", 1);
        DepletableNode nodeI = new DepletableNode("I", 1);

        Set<DepletableNode> list = new HashSet<>();
        list.add(nodeG);
        list.add(nodeB);
        list.add(nodeH);
        list.add(nodeA);
        list.add(nodeD);
        list.add(nodeE);
        list.add(nodeF);
        list.add(nodeI);
        list.add(nodeC);

        // A
        // D
        // H
        // I
        // C
        // B
        // F
        // E
        // G

        TopologicalDepleter<DepletableNode> depleter = new TopologicalDepleter<>();
        Set<DepletableNode> results = depleter.deplete(list, ArtefactPhase.CREATE);
        for (TopologicallyDepletable depletable : results) {
            System.out.println(depletable.getId());
        }

        assertEquals(results.size(), 0);

    }

    /**
     * Test not depleted.
     */
    @Test
    public void testNotDepleted() {
        System.out.println("Not depleted:");

        DepletableNode nodeA = new DepletableNode("A", 0);
        DepletableNode nodeB = new DepletableNode("B", 3);
        DepletableNode nodeC = new DepletableNode("C", 2);
        DepletableNode nodeD = new DepletableNode("D", 8);
        DepletableNode nodeE = new DepletableNode("E", 4);
        DepletableNode nodeF = new DepletableNode("F", 3);
        DepletableNode nodeG = new DepletableNode("G", 5);
        DepletableNode nodeH = new DepletableNode("H", 1);
        DepletableNode nodeI = new DepletableNode("I", 1);

        Set<DepletableNode> list = new HashSet<>();
        list.add(nodeG);
        list.add(nodeB);
        list.add(nodeH);
        list.add(nodeA);
        list.add(nodeD);
        list.add(nodeE);
        list.add(nodeF);
        list.add(nodeI);
        list.add(nodeC);

        // A
        // H
        // I
        // C
        // B
        // F
        // E
        // G
        // D

        TopologicalDepleter<DepletableNode> depleter = new TopologicalDepleter<>();
        Set<DepletableNode> results = depleter.deplete(list, ArtefactPhase.CREATE);
        for (TopologicallyDepletable depletable : results) {
            System.out.println(depletable.getId() + " remained");
        }

        assertEquals(results.size(), 1);

    }

}
