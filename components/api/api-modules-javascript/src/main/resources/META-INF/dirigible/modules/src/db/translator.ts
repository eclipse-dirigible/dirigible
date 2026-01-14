import { sql, query } from "../db";

/**
 * Provides static methods for translating entity properties based on a dedicated language table.
 * Translation is achieved by querying a separate table (e.g., 'BASE_TABLE_LANG') and merging
 * the translated fields back into the original data.
 */
export class Translator {

	/**
	 * Translates properties for a list of entities by querying the corresponding language table.
	 *
	 * @param list The array of entities to be translated.
	 * @param language The target language code (e.g., 'en', 'de'). If undefined, no translation occurs.
	 * @param basetTable The name of the base entity table (used to derive the language table name).
	 * @returns The translated array of entities.
	 */
	public static translateList(list: any[], language: string | undefined, basetTable: string): any[] {
        if (list && language) {
            try {
				// Construct the query for all translated columns in the target language.
				// Assumes the language table is named 'BASE_TABLE_LANG'.
                const script = sql.getDialect()
                    .select()
                    .column("*")
                    .from('"' + basetTable + '_LANG"')
                    .where('Language = ?')
                    .build();

                const resultSet: any[] = query.execute(script, [language]);

				// Check if any translations were found
                if (resultSet && resultSet.length > 0) {
                    const translatedProperties = Object.getOwnPropertyNames(resultSet[0]);
                    
					// Assumption: translatedProperties[0] is the entity ID property name.
					// Assumption: The last two properties are the ID property and the Language property, leaving N-2 for translated data.
                    const ID_PROPERTY_NAME = translatedProperties[0];
                    const NUM_TRANSLATED_PROPERTIES = translatedProperties.length - 2;
                    
					// Create an array of maps, one map per translated property name: { ID_value -> translated_value }
                    const translationMaps: Record<any, any>[] = Array(NUM_TRANSLATED_PROPERTIES).fill(null).map(() => ({}));
                    
                    // Populate the translation maps from the result set
                    resultSet.forEach((r) => {
                        const idValue = r[ID_PROPERTY_NAME];
                        for (let i = 0; i < NUM_TRANSLATED_PROPERTIES; i++) {
							// translatedProperties[i + 1] holds the actual translated field name
                            translationMaps[i][idValue] = r[translatedProperties[i + 1]];
                        }
                    });

					// Apply the translations to the original list
                    list.forEach((r) => {
                        const idValue = r[ID_PROPERTY_NAME];
                        for (let i = 0; i < NUM_TRANSLATED_PROPERTIES; i++) {
                            const translatedKey = translatedProperties[i + 1];
                            const translatedValue = translationMaps[i][idValue];
                            
                            if (translatedValue !== undefined && translatedValue !== null) {
								// Overwrite the base property with the translated value
                                r[translatedKey] = translatedValue;
                            }
                        }
                    });
                }
            } catch (error) {
                console.error("Entity is marked as language dependent, but no language table present for: " + basetTable, error);
            }
        }
        return list;
    }
	
	/**
	 * Translates properties for a single entity by querying the corresponding language table.
	 *
	 * @param entity The entity object to be translated.
	 * @param id The ID of the entity.
	 * @param language The target language code (e.g., 'en', 'de'). If undefined, no translation occurs.
	 * @param basetTable The name of the base entity table.
	 * @returns The translated entity object.
	 */
	public static translateEntity(entity: any, id: string | number, language: string | undefined, basetTable: string): any {
        if (entity && language) {
            try {
				// Construct the query for a specific ID and Language
                const script = sql.getDialect()
                    .select()
                    .column("*")
                    .from('"' + basetTable + '_LANG"')
                    .where('Language = ?')
                    .where('Id = ?')
                    .build();

                const resultSet: any[] = query.execute(script, [language, id]);

				// Since we query by Id and Language, we expect at most one row.
                if (resultSet && resultSet.length > 0) {
                    const translationRow = resultSet[0];
                    
					// Iterate through the properties of the translation row and merge them into the entity.
					// We must assume that the base table columns (except ID and Language keys) align with the translation table columns.
                    for (const key in translationRow) {
                        if (translationRow.hasOwnProperty(key)) {
							// Avoid overwriting the entity's ID and Language key if they exist, 
							// although typically the language key is not present in the base entity.
                            if (key !== 'Id' && key !== 'Language') {
                                entity[key] = translationRow[key];
                            }
                        }
                    }
                }
            } catch (error) {
                console.error("Entity is marked as language dependent, but no language table present for: " + basetTable, error);
            }
        }
        return entity;
    }
		
}

// @ts-ignore
if (typeof module !== 'undefined') {
	// @ts-ignore
	module.exports = Translator;
}
