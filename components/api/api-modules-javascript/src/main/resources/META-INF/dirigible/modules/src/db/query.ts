/**
 * API Query
 *
 */

const DatabaseFacade = Java.type(
  "org.eclipse.dirigible.components.api.db.DatabaseFacade",
);

/**
 * Interface used to wrap complex or specific values for non-named queries.
 */
export type TypedQueryParameter = {
  readonly type: string;
  readonly value: unknown;
};

/**
 * Interface defining a parameter for a named query (using placeholders like :paramName).
 */
export interface NamedQueryParameter {
  readonly name: string;
  readonly type: string;
  readonly value: any;
}

/**
 * Interface to specify formatting options for the query result set.
 */
export interface FormattingParameter {
  readonly dateFormat: string;
}

/**
 * Provides static methods for executing parameterized SQL SELECT statements.
 */
export class Query {
  /**
   * Executes a standard SQL query with positional parameters. Parameters array supports primitives e.g. `[1, 'John', 34.56]` or objects in format either `{'type':'[DATA_TYPE]', 'value':[VALUE]}` or `{'name':'[string]', 'type':'[DATA_TYPE]', 'value':[VALUE]}` e.g. `[{'type':'CHAR', 'value':'ISBN19202323322'}]` or `[{'name': 'order_number', 'type':'CHAR', 'value':'ISBN19202323322'}]`
   *
   * @param sql The SQL query to execute.
   * @param parameters An optional array of values (primitives, TypedQueryParameter or NamedQueryParameter objects) to replace '?' or :paramName placeholders.
   * @param datasourceName The name of the database connection to use (optional).
   * @param formatting Optional formatting parameters for the result set (e.g., date format).
   * @returns An array of records representing the query results.
   */
  public static execute(
    sql: string,
    parameters?: (string | number | boolean | Date | TypedQueryParameter | NamedQueryParameter)[] | string,
    datasourceName?: string,
    formatting?: FormattingParameter,
  ): any[] {
    const formattingJson = formatting ? JSON.stringify(formatting) : undefined;

    let arr: any[] = [];

    if (parameters == null) {
      arr = [];
    } else if (typeof parameters === "string") {
      try {
        const parsed = JSON.parse(parameters);
        if (!Array.isArray(parsed)) {
          throw new Error("Input parameter string must represent a JSON array");
        }
        arr = parsed;
      } catch (e) {
        throw new Error("Invalid JSON parameters: " + e);
      }
    } else if (Array.isArray(parameters)) {
      arr = parameters;
    } else {
      throw new Error("Parameters must be either an array or a JSON string");
    }

    if (arr.length === 0) {
      const resultset = DatabaseFacade.query(sql, null, datasourceName, formattingJson);
      return JSON.parse(resultset);
    }

    const first = arr[0];

    // NamedQueryParameter (has name + type)
    if (first && typeof first === "object" && "name" in first && "type" in first) {
      const resultset = DatabaseFacade.queryNamed(
        sql,
        JSON.stringify(arr),
        datasourceName
      );
      return JSON.parse(resultset);
    }

    // TypedQueryParameter (has type, no name)
    if (first && typeof first === "object" && "type" in first && !("name" in first)) {
      const resultset = DatabaseFacade.query(
        sql,
        JSON.stringify(arr),
        datasourceName,
        formattingJson
      );
      return JSON.parse(resultset);
    }

    // Primitive array
    if (
      arr.every(
        (v) =>
          typeof v === "string" ||
          typeof v === "number" ||
          typeof v === "boolean" ||
          v instanceof Date ||
		  Array.isArray(v)
      )
    ) {
      const resultset = DatabaseFacade.query(
        sql,
        JSON.stringify(arr),
        datasourceName,
        formattingJson
      );
      return JSON.parse(resultset);
    }

    throw new Error("Unsupported parameter format: " + JSON.stringify(parameters));
  }


  /**
   * Executes a SQL query with named parameters (e.g., ":name", ":id").
   *
   * @param sql The SQL query to execute.
   * @param parameters An optional array of NamedQueryParameter objects.
   * @param datasourceName The name of the database connection to use (optional).
   * @returns An array of records representing the query results.
   */
  public static executeNamed(
    sql: string,
    parameters?: NamedQueryParameter[],
    datasourceName?: string,
  ): any[] {
    // Serialize the array of named parameters for the Java facade
    const paramsJson = parameters ? JSON.stringify(parameters) : undefined;

    // The DatabaseFacade returns a JSON string representation of the result set
    const resultset = DatabaseFacade.queryNamed(
      sql,
      paramsJson,
      datasourceName,
    );

    // Parse the JSON string back into a JavaScript array of objects
    return JSON.parse(resultset);
  }
}

// @ts-ignore
if (typeof module !== "undefined") {
  // @ts-ignore
  module.exports = Query;
}
