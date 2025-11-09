/**
 * Provides a wrapper for the platform's ProblemsFacade to manage system issues,
 * including saving new problems, fetching existing ones, and updating their status.
 */
const ProblemsFacade = Java.type("org.eclipse.dirigible.components.api.platform.ProblemsFacade");

/**
 * @interface Problem
 * @description Represents a single problem or issue recorded in the system.
 */
export interface Problem {
   /** The unique identifier of the problem. */
   id: number;
   /** The resource path or file location where the problem was detected. */
   location: string;
   /** The severity or nature of the problem (e.g., "Error", "Warning"). */
   type: string;
   /** The line number in the source where the problem occurred. */
   line: string;
   /** The column number in the source where the problem occurred. */
   column: string;
   /** A brief description of the cause of the problem. */
   cause: string;
   /** A description of the expected state or value. */
   expected: string;
   /** The timestamp when the problem was created. */
   createdAt: Date;
   /** The user who created or triggered the problem record. */
   createdBy: string;
   /** The category of the problem (e.g., "Syntax", "Deployment"). */
   category: string;
   /** The module or component where the problem was found. */
   module: string;
   /** The original source code or content related to the problem. */
   source: string;
   /** The program or file name associated with the problem. */
   program: string;
   /** The current status of the problem (e.g., ACTIVE, SOLVED, IGNORED). */
   status: string;
}

/**
 * @class Problems
 * @description Static utility class for managing system problems via the ProblemsFacade.
 */
export class Problems {

   /** Status indicating a newly reported or unresolved problem. */
   public static readonly ACTIVE = "ACTIVE";
   /** Status indicating a problem that has been fixed or is no longer relevant. */
   public static readonly SOLVED = "SOLVED";
   /** Status indicating a problem that is known but deliberately being disregarded. */
   public static readonly IGNORED = "IGNORED";

   /**
    * Saves a new problem entry to the system's problem log.
    *
    * @param {string} location The resource path or file location.
    * @param {string} type The severity or nature of the problem.
    * @param {string} line The line number.
    * @param {string} column The column number.
    * @param {string} cause The cause description.
    * @param {string} expected The expected state/value description.
    * @param {string} category The problem category.
    * @param {string} module The module/component name.
    * @param {string} source The original source content.
    * @param {string} program The program or file name.
    */
   public static save(location: string, type: string, line: string, column: string, cause: string, expected: string, category: string, module: string, source: string, program: string): void {
      ProblemsFacade.save(location, type, line, column, cause, expected, category, module, source, program);
   }

   /**
    * Finds a specific problem by its unique ID.
    * Note: The underlying facade returns a JSON string which is parsed here.
    *
    * @param {number} id The unique problem ID.
    * @returns {Problem} The found Problem object.
    */
   public static findProblem(id: number): Problem {
      return JSON.parse(ProblemsFacade.findProblem(id));
   }

   /**
    * Fetches all recorded problems in the system.
    * Note: The underlying facade returns a JSON string which is parsed here.
    *
    * @returns {Problem[]} An array of all Problem objects.
    */
   public static fetchAllProblems(): Problem[] {
      return JSON.parse(ProblemsFacade.fetchAllProblems());
   }

   /**
    * Fetches a batch of problems based on a custom condition and limit.
    *
    * @param {string} condition A SQL-like condition string (e.g., "CATEGORY='Syntax'").
    * @param {number} limit The maximum number of problems to retrieve.
    * @returns {Problem[]} An array of Problem objects matching the condition.
    */
   public static fetchProblemsBatch(condition: string, limit: number): Problem[] {
      // Assuming ProblemsFacade.fetchProblemsBatch returns a parsed JSON array based on the original structure.
      // If it returns a string, it should be wrapped in JSON.parse() like the others.
      // Keeping it as is based on the original provided code.
      return ProblemsFacade.fetchProblemsBatch(condition, limit);
   }

   /**
    * Deletes a problem record by its unique ID.
    *
    * @param {number} id The unique problem ID to delete.
    */
   public static deleteProblem(id: number): void {
      ProblemsFacade.deleteProblem(id);
   }

   /**
    * Deletes all problem records that currently have the specified status.
    *
    * @param {string} status The status (e.g., Problems.SOLVED or Problems.IGNORED).
    */
   public static deleteAllByStatus(status: string): void {
      ProblemsFacade.deleteAllByStatus(status);
   }

   /**
    * Clears (deletes) all problem records in the system, regardless of status.
    */
   public static clearAllProblems(): void {
      ProblemsFacade.clearAllProblems();
   }

   /**
    * Updates the status of a single problem by its ID.
    *
    * @param {number} id The unique problem ID.
    * @param {string} status The new status (e.g., Problems.SOLVED).
    */
   public static updateStatus(id: number, status: string): void {
      ProblemsFacade.updateStatus(id, status);
   }

   /**
    * Updates the status of multiple problems identified by an array of IDs.
    *
    * @param {number[]} ids An array of unique problem IDs.
    * @param {string} status The new status to apply to all problems.
    */
   public static updateStatusMultiple(ids: number[], status: string): void {
      ProblemsFacade.updateStatusMultiple(ids, status);
   }
}

// @ts-ignore
if (typeof module !== 'undefined') {
	// @ts-ignore
	module.exports = Problems;
}