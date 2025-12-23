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
		return Store.parseResult(result);
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
		return Store.parseResult(result);
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
		return Store.parseResult(result);
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
			return Store.parseResult(result);
		}

		const first = arr[0];

		// NamedQueryParameter (has name + type)
		if (first && typeof first === "object" && "name" in first && "type" in first) {
			const result = DataStoreFacade.queryNamed(query, JSON.stringify(arr), limit, offset);
			return Store.parseResult(result);
		}

		// TypedQueryParameter (has type, no name)
		if (first && typeof first === "object" && "type" in first && !("name" in first)) {
			const result = DataStoreFacade.query(query, JSON.stringify(arr), limit, offset);
			return Store.parseResult(result);
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
			const result = DataStoreFacade.query(query, JSON.stringify(arr), limit, offset);
			return Store.parseResult(result);
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
			return Store.parseResult(result);
		}

		const first = arr[0];

		// NamedQueryParameter (has name + type)
		if (first && typeof first === "object" && "name" in first && "type" in first) {
			const result = DataStoreFacade.queryNativeNamed(query, JSON.stringify(arr), limit, offset);
			return Store.parseResult(result);
		}

		// TypedQueryParameter (has type, no name)
		if (first && typeof first === "object" && "type" in first && !("name" in first)) {
			const result = DataStoreFacade.queryNative(query, JSON.stringify(arr), limit, offset);
			return Store.parseResult(result);
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
			return Store.parseResult(result);
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

	/**
	 * Parse a JSON string and revive ISO date strings into JS Date objects.
	 * It handles both full ISO timestamps (with timezone) and date-only strings (YYYY-MM-DD).
	 * Returns undefined for null/empty inputs.
	 */
	private static parseResult(result: any): any {
		if (result === null || result === undefined || result === '') {
			return undefined;
		}
		if (typeof result !== 'string') {
			// already an object/array
			return result;
		}

		// Accept timezone offsets with or without colon (e.g. +00:00 or +0000)
		const ISO_DATETIME = /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(?:\.\d+)?(?:Z|[+\-]\d{2}:?\d{2})$/;
		const DATE_ONLY = /^\d{4}-\d{2}-\d{2}$/;
		const TZ_NO_COLON = /([+\-]\d{2})(\d{2})$/;
		// Space-separated datetime: "YYYY-MM-DD HH:MM:SS" (optionally with fractional seconds and timezone)
		const SPACE_DATETIME = /^\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}(?:\.\d+)?(?:Z|[+\-]\d{2}:?\d{2})?$/;
		// Time-only strings like "11:29:33" or "11:29:33.123"
		const TIME_ONLY = /^\d{2}:\d{2}:\d{2}(?:\.\d+)?$/;
		// Epoch strings: 13-digit ms or 10-digit seconds
		const EPOCH_MS = /^[+\-]?\d{13}$/;
		const EPOCH_S = /^[+\-]?\d{10}$/;

		return JSON.parse(result, (key, value) => {
			if (typeof value === 'string') {
				const s = value.trim();

				// Epoch millisecond/second strings
				if (EPOCH_MS.test(s)) {
					const ms = parseInt(s, 10);
					return new Date(ms);
				}
				if (EPOCH_S.test(s)) {
					const ms = parseInt(s, 10) * 1000;
					return new Date(ms);
				}

				// Space-separated datetimes: normalize to ISO and parse
				if (SPACE_DATETIME.test(s)) {
					let v = s.replace(' ', 'T');
					if (TZ_NO_COLON.test(v)) {
						v = v.replace(TZ_NO_COLON, '$1:$2');
					}
					const d = new Date(v);
					if (!isNaN(d.getTime())) {
						return d;
					}
				}

				// Time-only strings: attach current UTC date and parse as UTC
				if (TIME_ONLY.test(s)) {
					const m = s.match(/(\d{2}):(\d{2}):(\d{2})(?:\.(\d+))?/);
					if (m) {
						const hh = parseInt(m[1], 10);
						const mm = parseInt(m[2], 10);
						const ss = parseInt(m[3], 10);
						const frac = m[4] ? (m[4] + '000').substring(0, 3) : '000';
						const ms = parseInt(frac, 10);
						const now = new Date();
						const d = new Date(Date.UTC(now.getUTCFullYear(), now.getUTCMonth(), now.getUTCDate(), hh, mm, ss, ms));
						return d;
					}
				}

				if (ISO_DATETIME.test(s) || DATE_ONLY.test(s)) {
					// normalize timezone without colon (e.g. +0000 -> +00:00) because Date parsing
					// prefers the colon-separated offset in many JS engines
					let v = s;
					if (TZ_NO_COLON.test(v)) {
						v = v.replace(TZ_NO_COLON, '$1:$2');
					}
					const d = new Date(v);
					if (!isNaN(d.getTime())) {
						return d;
					}
				}
			}
			return value;
		});
	}

}

// @ts-ignore
if (typeof module !== 'undefined') {
	// @ts-ignore
	module.exports = Store;
}