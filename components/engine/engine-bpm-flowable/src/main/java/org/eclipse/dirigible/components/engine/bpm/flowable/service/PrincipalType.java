package org.eclipse.dirigible.components.engine.bpm.flowable.service;

public enum PrincipalType {

    /** The assignee. */
    ASSIGNEE("assignee"),
    /** The candidate groups. */
    CANDIDATE_GROUPS("groups");

    /** The type. */
    private final String type;

    /**
     * Instantiates a new type.
     *
     * @param type the type
     */
    PrincipalType(String type) {
        this.type = type;
    }

    /**
     * From string.
     *
     * @param type the type
     * @return the type
     */
    public static PrincipalType fromString(String type) {
        for (PrincipalType enumValue : values()) {
            if (enumValue.type.equals(type)) {
                return enumValue;
            }
        }
        throw new IllegalArgumentException("Unknown enum type: " + type);
    }
}
