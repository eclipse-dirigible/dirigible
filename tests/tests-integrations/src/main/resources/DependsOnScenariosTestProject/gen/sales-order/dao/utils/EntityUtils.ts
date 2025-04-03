export class EntityUtils {

	public static setDate(obj: any, property: string): void {
		if (obj && obj[property]) {
			obj[property] = new Date(obj[property]).getTime();
		}
	}

	public static setLocalDate(obj: any, property: string): void {
		if (obj && obj[property]) {
			obj[property] = new Date(new Date(obj[property]).setHours(-(new Date().getTimezoneOffset() / 60), 0, 0, 0)).toISOString();
		}
	}

	public static setBoolean(obj: any, property: string): void {
		if (obj && obj[property] !== undefined) {
			obj[property] = obj[property] ? true : false;
		}
	}
}