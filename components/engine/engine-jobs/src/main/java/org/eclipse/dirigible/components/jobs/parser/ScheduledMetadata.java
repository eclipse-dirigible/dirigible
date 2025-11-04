package org.eclipse.dirigible.components.jobs.parser;

/**
 * Represents metadata extracted from @Scheduled decorators.
 */
public class ScheduledMetadata {

    private String className;
    private String expression;
    private String group;

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getExpression() {
        return expression;
    }

    public void setExpression(String expression) {
        this.expression = expression;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    @Override
    public String toString() {
        return "ScheduledMetadata{" + "className='" + className + '\'' + ", expression='" + expression + '\'' + ", group='" + group + '\''
                + '}';
    }
}
