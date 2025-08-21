/**
 * API Values
 */

export class Values {

	public static parseValue(value: any): any {
		try {
			return JSON.parse(value);
		} catch (e) {
			// Do nothing
		}
		return value;
	}

	public static parseValuesMap(variables: Map<string, any>): Map<string, any> {
		for (const [key, value] of variables) {
			variables.set(key, Values.parseValue(value));
		}
		return variables;
	}

	public static stringifyValue(value: any): any {
		if (Array.isArray(value)) {
			// @ts-ignore
			return java.util.Arrays.asList(value.map(e => JSON.stringify(e)));
		} else if (typeof value === 'object') {
			return JSON.stringify(value);
		}
		return value;
	}

	public static stringifyValuesMap(variables: Map<string, any>): Map<string, any> {
		for (const [key, value] of variables) {
			variables.set(key, Values.stringifyValue(value));
		}
		return variables;
	}
}

// @ts-ignore
if (typeof module !== 'undefined') {
	// @ts-ignore
	module.exports = Values;
}