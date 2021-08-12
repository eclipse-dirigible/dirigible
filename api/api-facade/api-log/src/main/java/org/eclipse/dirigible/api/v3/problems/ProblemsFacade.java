/*
 * Copyright (c) 2021 SAP SE or an SAP affiliate company and Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: 2021 SAP SE or an SAP affiliate company and Eclipse Dirigible contributors
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.api.v3.problems;

import org.eclipse.dirigible.core.problems.exceptions.ProblemsException;
import org.eclipse.dirigible.core.problems.model.ProblemsModel;
import org.eclipse.dirigible.core.problems.service.ProblemsCoreService;
import org.eclipse.dirigible.commons.api.scripting.IScriptingFacade;
import java.util.List;

public class ProblemsFacade implements IScriptingFacade {

    public static final void save(String location, String type, String line, String column,
                                            String category, String module, String source, String program) throws ProblemsException {

        new ProblemsCoreService().save(location, type, line, column, category, module, source, program);
    }

    public static final String findProblem(Long id) throws ProblemsException {
        return new ProblemsCoreService().getProblemById(id).toJson();
    }

    public static final List<ProblemsModel> fetchAllProblems() throws ProblemsException {
        return new ProblemsCoreService().getAllProblems();
    }

    public static final void deleteProblem(Long id) throws ProblemsException {
        new ProblemsCoreService().deleteProblemById(id);
    }

    public static final void deleteAllByStatus(String status) throws ProblemsException {
        new ProblemsCoreService().deleteProblemsByStatus(status);
    }

    public static final void clearAllProblems() throws ProblemsException {
        new ProblemsCoreService().deleteAll();
    }

    public static final void updateStatus(Long id, String status) throws ProblemsException {
        new ProblemsCoreService().updateProblemStatusById(id, status);
    }

    public static final void updateStatusMultiple(List<Long> ids, String status) throws ProblemsException {
        new ProblemsCoreService().updateStatusMultipleProblems(ids, status);
    }
}
