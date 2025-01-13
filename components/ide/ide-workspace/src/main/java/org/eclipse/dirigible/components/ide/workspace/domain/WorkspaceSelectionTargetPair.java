/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.ide.workspace.domain;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

/**
 * The Workspace Selection Target Pair including conflict resolution rules.
 */
public class WorkspaceSelectionTargetPair {

    /** The source selection. */
    private ArrayList<SelectedNode> sourceSelection;

    /** The source workspace. */
    private String sourceWorkspace;

    /** The target workspace. */
    private String targetWorkspace;

    /** The target. */
    private String target;

    /**
     * Gets the source workspace.
     *
     * @return the sourceWorkspace
     */
    public String getSourceWorkspace() {
        return sourceWorkspace;
    }

    /**
     * Sets the source.
     *
     * @param sourceWorkspace the new source workspace
     */
    public void setSourceWorkspace(String sourceWorkspace) {
        this.sourceWorkspace = sourceWorkspace;
    }

    /**
     * Gets the source.
     *
     * @return the source
     */
    public ArrayList<SelectedNode> getSource() {
        return sourceSelection;
    }

    /**
     * Sets the source.
     *
     * @param source the new source
     */
    public void setSource(ArrayList<SelectedNode> source) {
        this.sourceSelection = source;
    }

    /**
     * Gets the target workspace.
     *
     * @return the targetWorkspace
     */
    public String getTargetWorkspace() {
        return targetWorkspace;
    }

    /**
     * Sets the target workspace.
     *
     * @param targetWorkspace the new target workspace
     */
    public void setTargetWorkspace(String targetWorkspace) {
        this.targetWorkspace = targetWorkspace;
    }

    /**
     * Gets the target.
     *
     * @return the target
     */
    public String getTarget() {
        return target;
    }

    /**
     * Sets the target.
     *
     * @param target the new target
     */
    public void setTarget(String target) {
        this.target = target;
    }

    /**
     * Skip by path.
     *
     * @param skipPath the path to skip all inside
     */
    public void skipByPath(String skipPath) {
        for (int i = 0; i < sourceSelection.size(); i++) {
            String pathOfNode = sourceSelection.get(i)
                                               .getPath();
            String pathToCompare =
                    pathOfNode.substring(0, pathOfNode.length() >= skipPath.length() ? skipPath.length() : pathOfNode.length());
            System.out.println(pathToCompare + ", " + pathOfNode);
            if (pathToCompare.equals(skipPath)) {
                sourceSelection.set(i, sourceSelection.get(i)
                                                      .setResolution("skip"));
            }
        }
    }


    /**
     * The Class SelectedNode.
     */
    public class SelectedNode {

        /** The id. */
        private String id;

        /** The path. */
        private String path;

        /** The type. */
        private String type;

        /** The norootpath. */
        private String norootpath;

        /** The resolution. */
        private String resolution;

        /**
         * Gets the internal path.
         *
         * @return the internal path
         */
        public String getInternalPath() {
            return norootpath;
        };

        /**
         * Gets the node type.
         *
         * @return the node type
         */
        public String getNodeType() {
            return type;
        }

        /**
         * Gets the path.
         *
         * @return the path
         */
        public String getPath() {
            return path;
        }

        /**
         * Sets the path.
         *
         * @param newPath the new path
         */
        public void setPath(String newPath) {
            path = newPath;
        }

        /**
         * Gets the relative path.
         *
         * @return the relative path
         */
        public String getRelativePath() {
            Path fullpath = Paths.get(path);
            return fullpath.subpath(2, fullpath.getNameCount())
                           .toString()
                           .replaceAll("^/+", "");
        }

        /**
         * Gets the resolution.
         *
         * @return the resolution
         */
        public String getResolution() {
            String defaultResolution = "rename";
            if (resolution != null)
                return resolution;
            return defaultResolution;
        }

        /**
         * Sets the resolution.
         *
         * @param resolution the resolution
         * @return the selected node
         */
        public SelectedNode setResolution(String resolution) {
            this.resolution = resolution;
            return this;
        }
    }
}
