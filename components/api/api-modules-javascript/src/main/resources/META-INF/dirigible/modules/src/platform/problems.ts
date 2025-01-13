/*
 * Copyright (c) 2023 SAP SE or an SAP affiliate company and Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: 2023 SAP SE or an SAP affiliate company and Eclipse Dirigible contributors
 * SPDX-License-Identifier: EPL-2.0
 */

const ProblemsFacade = Java.type("org.eclipse.dirigible.components.api.platform.ProblemsFacade");

export interface Problem {
   id: number;
   location: string;
   type: string;
   line: string;
   column: string;
   cause: string;
   expected: string;
   createdAt: Date;
   createdBy: string;
   category: string;
   module: string;
   source: string;
   program: string;
   status: string;
}

export class Problems {

   public static readonly ACTIVE = "ACTIVE";
   public static readonly SOLVED = "SOLVED";
   public static readonly IGNORED = "IGNORED";

   public static save(location: string, type: string, line: string, column: string, cause: string, expected: string, category: string, module: string, source: string, program: string): void {
      ProblemsFacade.save(location, type, line, column, cause, expected, category, module, source, program);
   }

   public static findProblem(id: number): Problem {
      return JSON.parse(ProblemsFacade.findProblem(id));
   }

   public static fetchAllProblems(): Problem[] {
      return JSON.parse(ProblemsFacade.fetchAllProblems());
   }

   public static fetchProblemsBatch(condition: string, limit: number): Problem[] {
      return ProblemsFacade.fetchProblemsBatch(condition, limit);
   }

   public static deleteProblem(id: number): void {
      ProblemsFacade.deleteProblem(id);
   }

   public static deleteAllByStatus(status: string): void {
      ProblemsFacade.deleteAllByStatus(status);
   }

   public static clearAllProblems(): void {
      ProblemsFacade.clearAllProblems();
   }

   public static updateStatus(id: number, status: string): void {
      ProblemsFacade.updateStatus(id, status);
   }

   public static updateStatusMultiple(ids: number[], status: string): void {
      ProblemsFacade.updateStatusMultiple(ids, status);
   }
}

// @ts-ignore
if (typeof module !== 'undefined') {
	// @ts-ignore
	module.exports = Problems;
}
