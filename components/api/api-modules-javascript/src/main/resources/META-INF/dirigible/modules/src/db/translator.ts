import { sql, query } from "sdk/db";

export class Translator {

	public static translateList(list: any[], language: string | undefined, basetTable: string): any[] {
        if (list && language) {
            try {
                let script = sql.getDialect().select().column("*").from('"' + basetTable + '_LANG"').where('Language = ?').build();
                const resultSet = query.execute(script, [language]);
                if (resultSet !== null && resultSet[0] !== null) {
                    let translatedProperties = Object.getOwnPropertyNames(resultSet[0]);
                    let maps: any[] = [];
                    for (let i = 0; i < translatedProperties.length - 2; i++) {
                        maps[i] = {};
                    }
                    resultSet.forEach((r) => {
                        for (let i = 0; i < translatedProperties.length - 2; i++) {
                            maps[i][r[translatedProperties[0]]] = r[translatedProperties[i + 1]];
                        }
                    });
                    list.forEach((r) => {
                        for (let i = 0; i < translatedProperties.length - 2; i++) {
                            if (maps[i][r[translatedProperties[0]]]) {
                                r[translatedProperties[i + 1]] = maps[i][r[translatedProperties[0]]];
                            }
                        }

                    });
                }
            } catch (Error) {
                console.error("Entity is marked as language dependent, but no language table present: " + basetTable);
            }
        }
        return list;
    }
	
	public static translateEntity(entity: any, id: string | number, language: string | undefined, basetTable: string): any[] {
        if (entity && language) {
            try {
                let script = sql.getDialect().select().column("*").from('"' + basetTable + '_LANG"').where('Language = ?').where('Id = ?').build();
                const resultSet = query.execute(script, [language, id]);
                let translatedProperties = Object.getOwnPropertyNames(resultSet[0]);
                let maps: any[] = [];
                for (let i = 0; i < translatedProperties.length - 2; i++) {
                    maps[i] = {};
                }
                resultSet.forEach((r) => {
                    for (let i = 0; i < translatedProperties.length - 2; i++) {
                        maps[i][r[translatedProperties[0]]] = r[translatedProperties[i + 1]];
                    }
                });
                for (let i = 0; i < translatedProperties.length - 2; i++) {
                    if (maps[i][entity[translatedProperties[0]]]) {
                        entity[translatedProperties[i + 1]] = maps[i][entity[translatedProperties[0]]];
                    }
                }
            } catch (Error) {
                console.error("Entity is marked as language dependent, but no language table present: " + basetTable);
            }
        }
        return entity;
    }
		
}