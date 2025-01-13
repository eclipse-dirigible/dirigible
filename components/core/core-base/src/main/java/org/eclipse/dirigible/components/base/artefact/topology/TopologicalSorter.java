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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

/**
 * The Class TopologicalSorter.
 *
 * @param <T> the generic type
 */
public class TopologicalSorter<T extends TopologicallySortable> {


    /** The stack. */
    private Stack<TopologicalSorterNode<T>> stack;

    /**
     * Instantiates a new topological sorter.
     */
    public TopologicalSorter() {
        this.stack = new Stack<>();
    }

    /**
     * Sort.
     *
     * @param list the list
     * @return the list
     */
    public List<T> sort(List<T> list) {
        Map<String, TopologicalSorterNode<T>> nodes = new HashMap<>();
        for (TopologicallySortable sortable : list) {
            TopologicalSorterNode<T> node = new TopologicalSorterNode(sortable, nodes);
            nodes.put(sortable.getId(), node);
        }

        for (TopologicalSorterNode<T> node : nodes.values()) {
            topologicalSort(node);
        }

        List<T> results = new ArrayList<>();
        for (TopologicalSorterNode<T> node : stack) {
            results.add(node.getData());
        }

        return results;
    }

    /**
     * Topological sort.
     *
     * @param node the node
     */
    private void topologicalSort(TopologicalSorterNode<T> node) {
        List<TopologicalSorterNode<T>> dependencies = node.getDependencies();
        for (int i = 0; i < dependencies.size(); i++) {
            TopologicalSorterNode<T> n = dependencies.get(i);
            if (n != null && !n.isVisited()) {
                n.setVisited(true);
                topologicalSort(n);
            }
        }
        if (!stack.contains(node)) {
            stack.push(node);
        }
    }

}
