const DataStoreFacade = Java.type("org.eclipse.dirigible.components.api.db.DataStoreFacade");

/**
 * Defines the available comparison operators for query conditions.
 */
export enum Operator {
	EQ = "=", // Equals
	NE = "<>", // Not Equals
	GT = ">", // Greater Than
	LT = "<", // Less Than
	GE = ">=", // Greater Than or Equals
	LE = "<=", // Less Than or Equals
	LIKE = "LIKE", // SQL LIKE operator
	BETWEEN = "BETWEEN", // SQL BETWEEN operator (requires two values)
	IN = "IN" // SQL IN operator (requires a List or Array of values)
}

/**
 * Defines the direction for sorting.
 */
export enum Direction {
	ASC = "ASC", // Ascending
	DESC = "DESC" // Descending
}

/**
 * Represents a single condition for filtering data.
 */
export interface Condition {
	propertyName: string,
	operator: Operator,
	value: any | any[]
}

/**
 * Represents a single sorting instruction.
 */
export interface Sort {
	propertyName: string,
	direction: Direction
}

/**
 * Defines optional parameters for list and count operations.
 */
export interface Options {
	conditions?: Condition[],
	sorts?: Sort[],
	limit?: number,
	offset?: number,
	language?: string
}

import { TypedQueryParameter, NamedQueryParameter } from './query';

/**
 * Facade class for interacting with the underlying Dirigible Data Store.
 * All methods serialize/deserialize JavaScript objects to/from JSON strings
 * before interacting with the native Java facade.
 */
export class Store {

	/**
	 * Saves a new entry to the data store.
	 * @param name The entity/table name.
	 * @param entry The JavaScript object to save.
	 * @returns The ID of the newly created entry (string or number).
	 */
	public static save(name: string, entry: any): string | number {
		return DataStoreFacade.save(name, JSON.stringify(entry));
	}
	
	/**
	 * Inserts a new entry or updates an existing one if the ID is present.
	 * @param name The entity/table name.
	 * @param entry The JavaScript object to insert/update.
	 */
	public static upsert(name: string, entry: any): void {
		DataStoreFacade.upsert(name, JSON.stringify(entry));
	}
	
	/**
	 * Updates an existing entry.
	 * @param name The entity/table name.
	 * @param entry The JavaScript object with the ID and updated data.
	 */
	public static update(name: string, entry: any): void {
		DataStoreFacade.update(name, JSON.stringify(entry));
	}
	
	/**
	 * Lists entries based on optional filtering, sorting, and pagination options.
	 * @param name The entity/table name.
	 * @param options Optional {@link Options} for query execution.
	 * @returns An array of JavaScript objects.
	 */
	public static list(name: string, options?: Options): any[] {
		const result = DataStoreFacade.list(name, options ? JSON.stringify(options) : null);
		return JSON.parse(result);
	}
	
	/**
	 * Counts the number of entries based on optional filtering options.
	 * @param name The entity/table name.
	 * @param options Optional {@link Options} for query execution.
	 * @returns The count of matching entries.
	 */
	public static count(name: string, options?: Options): number {
		const optionsString = options ? JSON.stringify(options) : null;
		const result = DataStoreFacade.count(name, optionsString);
		return result;
	}

	/**
	 * Retrieves a single entry by its ID.
	 * @param name The entity/table name.
	 * @param id The ID of the entry.
	 * @returns The entry object, or undefined if not found.
	 */
	public static get(name: string, id: any): any | undefined {
		const result = DataStoreFacade.get(name, id);
		// Assuming the native API returns null/undefined or an empty JSON string if not found, 
		// otherwise JSON.parse handles the conversion.
		if (result === null || result === undefined || result === "") {
			return undefined;
		}
		return JSON.parse(result);
	};

	/**
	 * Deletes an entry by its ID.
	 * @param name The entity/table name.
	 * @param id The ID of the entry to remove.
	 */
	public static remove(name: string, id: any): void {
		DataStoreFacade.deleteEntry(name, id);
	}
	
	/**
	 * Finds entries matching an example object (query-by-example).
	 * @param name The entity/table name.
	 * @param example An object containing properties to match.
	 * @param limit Maximum number of results to return.
	 * @param offset Number of results to skip.
	 * @returns An array of matching JavaScript objects.
	 */
	public static find(name: string, example: any, limit: number = 100, offset: number = 0): any[] {
		const result = DataStoreFacade.find(name, JSON.stringify(example), limit, offset);
		return JSON.parse(result);
	}
	
	/**
	 * Queries all entries for a given script with pagination.
	 * @param query The query script.
	 * @param limit Maximum number of results to return.
	 * @param offset Number of results to skip.
	 * @returns An array of JavaScript objects.
	 */
	public static query(query: string, parameters?: (string | number | boolean | Date | TypedQueryParameter | NamedQueryParameter)[], limit: number = 100, offset: number = 0): any[] {
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
		  const result = DataStoreFacade.query(query, null, limit, offset);
		  return JSON.parse(result);
	    }

	    const first = arr[0];

	    // NamedQueryParameter (has name + type)
	    if (first && typeof first === "object" && "name" in first && "type" in first) {
		  const result = DataStoreFacade.queryNamed(query, JSON.stringify(arr), limit, offset);
		  return JSON.parse(result);
	    }

	    // TypedQueryParameter (has type, no name)
	    if (first && typeof first === "object" && "type" in first && !("name" in first)) {
		  const result = DataStoreFacade.query(query, JSON.stringify(arr), limit, offset);
		  return JSON.parse(result);
	    }

	    // Primitive array
	    if (
	      arr.every(
	        (v) =>
	          typeof v === "string" ||
	          typeof v === "number" ||
	          typeof v === "boolean" ||
	          v instanceof Date	||
		  	  Array.isArray(v)
	      )
	    ) {
		  const result = DataStoreFacade.query(query, JSON.stringify(arr), limit, offset);
		  return JSON.parse(result);
	    }

	    throw new Error("Unsupported parameter format: " + JSON.stringify(parameters));
	}
	
	/**
	 * Queries all entries for a given entity name without pagination.
	 * @param query The entity/table name.
	 * @returns An array of all JavaScript objects.
	 */
	public static queryNative(query: string, parameters?: (string | number | boolean | Date | TypedQueryParameter | NamedQueryParameter)[], limit: number = 100, offset: number = 0): any[] {
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
		  const result = DataStoreFacade.queryNative(query, null, limit, offset);
		  return JSON.parse(result);
	    }

	    const first = arr[0];

	    // NamedQueryParameter (has name + type)
	    if (first && typeof first === "object" && "name" in first && "type" in first) {
		  const result = DataStoreFacade.queryNativeNamed(query, JSON.stringify(arr), limit, offset);
		  return JSON.parse(result);
	    }

	    // TypedQueryParameter (has type, no name)
	    if (first && typeof first === "object" && "type" in first && !("name" in first)) {
		  const result = DataStoreFacade.queryNative(query, JSON.stringify(arr), limit, offset);
		  return JSON.parse(result);
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
		  const result = DataStoreFacade.queryNative(query, JSON.stringify(arr), limit, offset);
		  return JSON.parse(result);
	    }

	    throw new Error("Unsupported parameter format: " + JSON.stringify(parameters));
	}
	
	// --- Metadata Getters ---

	/**
	 * Gets the name of the entity associated with the store name.
	 */
	public static getEntityName(name: string): string {
		return DataStoreFacade.getEntityName(name);
	}
	
	/**
	 * Gets the underlying database table name for the entity.
	 */
	public static getTableName(name: string): string {
		return DataStoreFacade.getTableName(name);
	}
		
	/**
	 * Gets the property name used as the ID field in the entity object.
	 */
	public static getIdName(name: string): string {
		return DataStoreFacade.getIdName(name);
	}
			
	/**
	 * Gets the underlying database column name used for the ID field.
	 */
	public static getIdColumn(name: string): string {
		return DataStoreFacade.getIdColumn(name);
	}

}

// @ts-ignore
if (typeof module !== 'undefined') {
	// @ts-ignore
	module.exports = Store;
}