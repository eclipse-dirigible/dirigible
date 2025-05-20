package org.eclipse.dirigible.components.api.db.params;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

class IndexedParamJsonObject {

    private final JsonElement valueElement;

    IndexedParamJsonObject(JsonElement valueElement) {
        this.valueElement = valueElement;
    }

    public JsonElement getValueElement() {
        return valueElement;
    }

    static IndexedParamJsonObject fromJsonElement(JsonElement parameterElement) throws IllegalArgumentException {
        JsonObject jsonObject = parameterElement.getAsJsonObject();

        JsonElement valueElement = jsonObject.get("value");

        return new IndexedParamJsonObject(valueElement);
    }

}
