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
import java.util.List;
import java.util.Map;

/**
 * The Class TopologicalSorterNode.
 *
 * @param <T> the generic type
 */
public class TopologicalSorterNode<T extends TopologicallySortable> {

    /** The data. */
    private T data;

    /** The nodes. */
    private Map<String, TopologicalSorterNode<T>> nodes;

    /** The visited. */
    private boolean visited;

    /** The dependencies. */
    private List<TopologicalSorterNode<T>> dependencies;

    /**
     * Instantiates a new topological sorter node.
     *
     * @param data the data
     * @param nodes the nodes
     */
    public TopologicalSorterNode(T data, Map<String, TopologicalSorterNode<T>> nodes) {
        this.data = data;
        this.nodes = nodes;
    }

    /**
     * Gets the data.
     *
     * @return the data
     */
    public T getData() {
        return data;
    }

    /**
     * Checks if is visited.
     *
     * @return true, if is visited
     */
    public boolean isVisited() {
        return visited;
    }

    /**
     * Sets the visited.
     *
     * @param visited the new visited
     */
    public void setVisited(boolean visited) {
        this.visited = visited;
    }

    /**
     * Gets the dependencies.
     *
     * @return the dependencies
     */
    public List<TopologicalSorterNode<T>> getDependencies() {
        if (this.dependencies == null) {
            this.dependencies = new ArrayList<>();
            for (TopologicallySortable sortable : this.data.getDependencies()) {
                this.dependencies.add(nodes.get(sortable.getId()));
            }
        }
        return this.dependencies;
    }

    /**
     * Sets the dependencies.
     *
     * @param dependencies the new dependencies
     */
    public void setDependencies(List<TopologicalSorterNode<T>> dependencies) {
        this.dependencies = dependencies;
    }

    /**
     * To string.
     *
     * @return the string
     */
    public String toString() {
        return data.getId();
    }

}
