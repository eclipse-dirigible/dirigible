package org.eclipse.dirigible.components.tracing;

import java.util.Map;
import java.util.TreeMap;

public class TaskStateUtil {

    /**
     * Gets the variables.
     *
     * @param context the context
     * @return the variables
     */
    public static final Map<String, String> getVariables(Map<String, Object> map) {
        Map<String, String> result = new TreeMap<String, String>();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (entry.getValue() instanceof String) {
                result.put(entry.getKey(), (String) entry.getValue());
            }
        }
        return result;
    }

}
