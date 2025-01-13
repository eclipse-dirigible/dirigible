/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.engine.odata2.sql.clause;

import org.apache.olingo.odata2.api.commons.HttpStatusCodes;
import org.apache.olingo.odata2.api.edm.*;
import org.apache.olingo.odata2.api.exception.ODataNotImplementedException;
import org.apache.olingo.odata2.api.uri.expression.*;
import org.apache.olingo.odata2.core.edm.EdmNull;
import org.eclipse.dirigible.engine.odata2.sql.api.OData2Exception;
import org.eclipse.dirigible.engine.odata2.sql.api.SQLStatementParam;
import org.eclipse.dirigible.engine.odata2.sql.binding.EdmTableBinding.ColumnInfo;
import org.eclipse.dirigible.engine.odata2.sql.builder.SQLSelectBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static org.apache.olingo.odata2.api.commons.HttpStatusCodes.INTERNAL_SERVER_ERROR;
import static org.eclipse.dirigible.engine.odata2.sql.builder.EdmUtils.evaluateDateTimeExpressions;

/**
 * The Class SQLWhereClauseVisitor.
 */
public class SQLWhereClauseVisitor implements ExpressionVisitor {

    /** The Constant LOG. */
    private static final Logger LOG = LoggerFactory.getLogger(SQLWhereClauseVisitor.class);

    /** The binary operator priorities. */
    private static Map<BinaryOperator, Integer> binaryOperatorPriorities;

    static {
        binaryOperatorPriorities = new HashMap<>();
        binaryOperatorPriorities.put(BinaryOperator.PROPERTY_ACCESS, 100);
        binaryOperatorPriorities.put(BinaryOperator.MUL, 60);
        binaryOperatorPriorities.put(BinaryOperator.DIV, 60);
        binaryOperatorPriorities.put(BinaryOperator.MODULO, 60);
        binaryOperatorPriorities.put(BinaryOperator.ADD, 50);
        binaryOperatorPriorities.put(BinaryOperator.SUB, 50);
        binaryOperatorPriorities.put(BinaryOperator.LT, 40);
        binaryOperatorPriorities.put(BinaryOperator.GT, 40);
        binaryOperatorPriorities.put(BinaryOperator.GE, 40);
        binaryOperatorPriorities.put(BinaryOperator.LE, 40);
        binaryOperatorPriorities.put(BinaryOperator.EQ, 30);
        binaryOperatorPriorities.put(BinaryOperator.NE, 30);
        binaryOperatorPriorities.put(BinaryOperator.AND, 20);
        binaryOperatorPriorities.put(BinaryOperator.OR, 10);
    }

    /** The where clause params. */
    private final List<SQLStatementParam> whereClauseParams;

    /** The query. */
    private SQLSelectBuilder query;

    /** The target type. */
    private EdmStructuralType targetType;

    /** The is in complex type. */
    private boolean isInComplexType = false;


    /**
     * Instantiates a new SQL where clause visitor.
     *
     * @param query the query
     * @param targetType the target type
     */
    public SQLWhereClauseVisitor(SQLSelectBuilder query, EdmStructuralType targetType) {
        this.whereClauseParams = new ArrayList<>();
        this.targetType = targetType;
        this.query = query;
    }

    /**
     * Visit filter expression.
     *
     * @param filterExpression the filter expression
     * @param expressionString the expression string
     * @param expression the expression
     * @return the object
     */
    @Override
    public Object visitFilterExpression(FilterExpression filterExpression, String expressionString, Object expression) {
        return new SQLWhereClause((String) expression, whereClauseParams.toArray(new SQLStatementParam[0]));
    }

    /**
     * Visit binary.
     *
     * @param binaryExpression the binary expression
     * @param operator the operator
     * @param leftSide the left side
     * @param rightSide the right side
     * @return the object
     */
    @Override
    public Object visitBinary(BinaryExpression binaryExpression, BinaryOperator operator, Object leftSide, Object rightSide) {
        String leftSideString;
        String rightSideString;

        if (leftSide instanceof ColumnInfo) {
            leftSideString = ((ColumnInfo) leftSide).getColumnName();
        } else {
            leftSideString = (String) leftSide;
        }

        if (rightSide instanceof ColumnInfo) {
            rightSideString = ((ColumnInfo) rightSide).getColumnName();
        } else {
            rightSideString = (String) rightSide;
        }

        switch (operator) {
            case AND:
            case OR:
            case EQ:
            case NE:
            case LT:
            case LE:
            case GT:
            case GE: {

                boolean isNullPredicate = binaryExpression.getRightOperand()
                                                          .getEdmType() instanceof EdmNull;
                Object res = escapeOperatorPrecedence(binaryExpression, binaryExpression.getLeftOperand(), //
                        writeParam(binaryExpression.getLeftOperand(), leftSideString, null)) + //
                        " " + translateToSQL(operator, isNullPredicate) + " " + //
                        escapeOperatorPrecedence(binaryExpression, binaryExpression.getRightOperand(), //
                                writeParam(binaryExpression.getRightOperand(), rightSideString, null));
                return res;
                // TODO Add support for ADD SUB MUL DIV MOD (Relevant for SQL ?)
            }
            case PROPERTY_ACCESS:
                // TODO this is when we have property access (need to use different table). what
                LOG.debug("Property access not implemented for {}", binaryExpression.getUriLiteral());
                throw new RuntimeException(new ODataNotImplementedException());
            default:
                LOG.debug("Binary clause not implemented for {}", binaryExpression.getUriLiteral());
                throw new RuntimeException(new ODataNotImplementedException());
        }
    }

    /**
     * Visit literal.
     *
     * @param literal the literal
     * @param edmLiteral the edm literal
     * @return the object
     */
    @Override
    public Object visitLiteral(LiteralExpression literal, EdmLiteral edmLiteral) {
        return edmLiteral.getLiteral();
    }

    /**
     * Visit method.
     *
     * @param methodExpression the method expression
     * @param method the method
     * @param parameters the parameters
     * @return the object
     */
    @Override
    public Object visitMethod(MethodExpression methodExpression, MethodOperator method, List<Object> parameters) {

        String firstParameterAsString = null;
        Object firstParameter = parameters.get(0);
        if (firstParameter instanceof ColumnInfo) {
            firstParameterAsString = ((ColumnInfo) firstParameter).getColumnName();
        } else {
            firstParameterAsString = (String) firstParameter;
        }

        String secondParameterAsString = null;
        if (parameters.size() > 1) {
            Object secondParameter = parameters.get(1);
            if (secondParameter instanceof ColumnInfo) {
                secondParameterAsString = ((ColumnInfo) secondParameter).getColumnName();
            } else {
                secondParameterAsString = (String) secondParameter;
            }
        }

        switch (methodExpression.getMethod()) {
            case ENDSWITH:
                String param = writeParam(methodExpression.getParameters()
                                                          .get(1),
                        secondParameterAsString, "%%%s");
                if (!(firstParameter instanceof ColumnInfo)) {
                    throw new OData2Exception("Invalid like syntax", HttpStatusCodes.BAD_REQUEST);
                }
                return String.format("%s LIKE %s", //
                        firstParameterAsString, //
                        param);
            case LENGTH:
                writeParam(methodExpression.getParameters()
                                           .get(0),
                        firstParameterAsString, "%s");
                return String.format("LENGTH(%s)", //
                        firstParameterAsString);
            case CONCAT:
                // concat(FIELD, '') and concat('',FIELD)
                String param1 = writeParam(methodExpression.getParameters()
                                                           .get(0),
                        firstParameterAsString, "%s");
                String param2 = writeParam(methodExpression.getParameters()
                                                           .get(1),
                        secondParameterAsString, "%s");
                return String.format("CONCAT(%s,%s)", param1, param2);
            case STARTSWITH:
                if (!(firstParameter instanceof ColumnInfo)) {
                    throw new OData2Exception("Invalid startswith usage", HttpStatusCodes.BAD_REQUEST);
                }
                return String.format("%s LIKE %s", //
                        firstParameterAsString, //
                        writeParam(methodExpression.getParameters()
                                                   .get(1),
                                secondParameterAsString, "%s%%"));
            case SUBSTRINGOF:
                return String.format("%s LIKE %s", //
                        secondParameterAsString, //
                        writeParam(methodExpression.getParameters()
                                                   .get(0),
                                firstParameterAsString, "%%%s%%"));
            case TOLOWER:
                return String.format("LOWER(%s)", firstParameterAsString);
            case TOUPPER:
                return String.format("UPPER(%s)", firstParameterAsString);
            default:
                throw new RuntimeException(new ODataNotImplementedException());
        }
    }

    /**
     * Visit member.
     *
     * @param memberExpression the member expression
     * @param path the path
     * @param property the property
     * @return the object
     */
    @Override
    public Object visitMember(MemberExpression memberExpression, Object path, Object property) {
        final EdmTypeKind propertyKind = memberExpression.getProperty()
                                                         .getEdmType()
                                                         .getKind();
        final CommonExpression pathEdmType = memberExpression.getPath();
        try {
            switch (propertyKind) {
                case SIMPLE:
                    if (memberExpression.getPath()
                                        .getKind() == ExpressionKind.MEMBER) {
                        final MemberExpression pathMemberExpression = (MemberExpression) memberExpression.getPath();
                        query.join((EdmStructuralType) pathMemberExpression.getProperty()
                                                                           .getEdmType(),
                                (EdmStructuralType) pathMemberExpression.getPath()
                                                                        .getEdmType());
                    } else {
                        query.join((EdmStructuralType) memberExpression.getPath()
                                                                       .getEdmType(),
                                targetType);
                    }
                    return query.getSQLTableColumnInfo((EdmStructuralType) memberExpression.getPath()
                                                                                           .getEdmType(),
                            (EdmProperty) property);
                case ENTITY:
                    if (memberExpression.getPath()
                                        .getKind() == ExpressionKind.MEMBER) {
                        final MemberExpression pathMemberExpression = (MemberExpression) memberExpression.getPath();
                        query.join((EdmStructuralType) pathMemberExpression.getProperty()
                                                                           .getEdmType(),
                                (EdmStructuralType) pathMemberExpression.getPath()
                                                                        .getEdmType());
                        return query.getSQLTableColumnInfo((EdmStructuralType) memberExpression.getPath()
                                                                                               .getEdmType(),
                                (EdmProperty) property);
                    } else {
                        query.join((EdmStructuralType) memberExpression.getPath()
                                                                       .getEdmType(),
                                targetType);
                    }
                    return property; // Return property here to be used as path for next level Member
                default:
                    throw new OData2Exception(String.format(
                            "Error during processing member clause between path %s and property %s: Unsupported property kind %s",
                            pathEdmType.getEdmType(), memberExpression.getProperty()
                                                                      .getEdmType(),
                            propertyKind), INTERNAL_SERVER_ERROR);
            }
        } catch (EdmException e) {
            throw new OData2Exception(String.format("Error during processing member clause between path %s and property %s",
                    pathEdmType.getEdmType(), memberExpression.getProperty()
                                                              .getEdmType()),
                    INTERNAL_SERVER_ERROR, e);
        }
    }

    /**
     * Visit unary.
     *
     * @param unaryExpression the unary expression
     * @param operator the operator
     * @param operand the operand
     * @return the object
     */
    @Override
    public Object visitUnary(UnaryExpression unaryExpression, UnaryOperator operator, Object operand) {
        final ExpressionKind operandExpressionKind = unaryExpression.getOperand()
                                                                    .getKind();
        String format = "%s %s";
        if (operandExpressionKind == ExpressionKind.UNARY || operandExpressionKind == ExpressionKind.BINARY) {
            // Brackets are only required in case Unary clause operates on another unary or binary clause
            format = "%s(%s)";
        }

        final String operandString = String.valueOf(operand);
        String operatorString = operator.name();
        switch (operator) {
            case NOT:
                operatorString = "NOT";
                break;
            case MINUS:
                operatorString = "-";
                break;
        }
        return String.format(format, operatorString, operandString);
    }

    /**
     * Visit order by expression.
     *
     * @param orderByExpression the order by expression
     * @param expressionString the expression string
     * @param orders the orders
     * @return the object
     */
    @Override
    public Object visitOrderByExpression(OrderByExpression orderByExpression, String expressionString, List<Object> orders) {
        // TODO implement me
        throw new RuntimeException(new ODataNotImplementedException());
    }

    /**
     * Visit order.
     *
     * @param orderExpression the order expression
     * @param filterResult the filter result
     * @param sortOrder the sort order
     * @return the object
     */
    @Override
    public Object visitOrder(OrderExpression orderExpression, Object filterResult, SortOrder sortOrder) {
        // TODO implement me
        throw new RuntimeException(new ODataNotImplementedException());
    }

    /**
     * Visit property.
     *
     * @param propertyExpression the property expression
     * @param uriLiteral the uri literal
     * @param edmProperty the edm property
     * @return the object
     */
    @Override
    public Object visitProperty(PropertyExpression propertyExpression, String uriLiteral, EdmTyped edmProperty) {
        try {
            final EdmTypeKind propertyKind = edmProperty.getType()
                                                        .getKind();

            switch (propertyKind) {
                case COMPLEX:
                    this.isInComplexType = true;
                    EdmStructuralType complexType = (EdmStructuralType) edmProperty.getType();
                    return complexType;
                case ENTITY:
                    isInComplexType = true; // ???
                    return propertyExpression.getEdmType();
                case SIMPLE:
                    if (isInComplexType) {
                        this.isInComplexType = false;
                        return edmProperty;
                    }
                    try {
                        return query.getSQLTableColumnInfo(targetType, (EdmProperty) edmProperty);
                    } catch (EdmException e) {
                        throw new OData2Exception(
                                String.format("Unable to find binding for type %s and property %s", targetType, edmProperty),
                                INTERNAL_SERVER_ERROR, e);
                    }
                default:
                    throw new OData2Exception(String.format("Unable to handle PropertyKind %s for EdmType %s and PropertyProvider %s",
                            propertyKind, targetType, edmProperty.getClass()
                                                                 .getName()),
                            INTERNAL_SERVER_ERROR);

            }
        } catch (EdmException e) {
            throw new OData2Exception(String.format("Unable to find binding for type %s and property %s", targetType, edmProperty.getClass()
                                                                                                                                 .getName()),
                    INTERNAL_SERVER_ERROR, e);
        }
    }

    /**
     * Translate to SQL.
     *
     * @param operator the operator
     * @param isNullPredicate the is null predicate
     * @return the string
     */
    private String translateToSQL(final BinaryOperator operator, boolean isNullPredicate) {
        switch (operator) {
            case EQ:
                // TODO CLEARIFY: Why not using operator.name() also here ???
                return isNullPredicate ? "IS" : "=";
            case NE:
                return isNullPredicate ? "IS NOT" : "<>";
            case LT:
                return "<";
            case LE:
                return "<=";
            case GT:
                return ">";
            case GE:
                return ">=";
            default:
                return operator.name();
        }
    }

    /**
     * Write param.
     *
     * @param expression the expression
     * @param literal the literal
     * @param format the format
     * @return the string
     */
    private String writeParam(final CommonExpression expression, final String literal, final String format) {

        if (expression.getKind() == ExpressionKind.LITERAL) {
            if (expression.getEdmType() instanceof EdmNull) {
                return "NULL";
            }
            final String value = (format != null ? String.format(format, literal) : literal);
            Object literalValue = evaluateDateTimeExpressions(value, (EdmSimpleType) expression.getEdmType());
            List<ColumnInfo> columnInfo = getColumnInfo(expression);
            if (columnInfo != null && columnInfo.size() > 1) {
                throw new IllegalArgumentException(String.format("Unable to parse expression %s", expression));
            }
            whereClauseParams.add(SQLWhereClause.param(literalValue, (EdmSimpleType) expression.getEdmType(),
                    columnInfo == null ? null : columnInfo.get(0)));
            // we should be using a prepared statement, so therefore always return the question marks here,
            // since we have added the param to the query
            return "?";
        }
        return literal;
    }

    /**
     * Gets the column info.
     *
     * @param expression the expression
     * @return the column info
     */
    List<ColumnInfo> getColumnInfo(CommonExpression expression) {
        try {
            if (expression instanceof BinaryExpression) {
                BinaryExpression be = (BinaryExpression) expression;
                CommonExpression left = be.getLeftOperand();
                CommonExpression right = be.getRightOperand();
                List<ColumnInfo> leftColumnInfo = getColumnInfo(left);
                List<ColumnInfo> rightColumnInfo = getColumnInfo(right);

                List<ColumnInfo> res = new ArrayList<>();
                if (leftColumnInfo != null)
                    res.addAll(leftColumnInfo);
                if (rightColumnInfo != null)
                    res.addAll(rightColumnInfo);
                return res;
            } else if (expression instanceof UnaryExpression) {
                CommonExpression operand = ((UnaryExpression) expression).getOperand();
                return getColumnInfo(operand);
            } else if (expression instanceof MethodExpression) {
                List<CommonExpression> params = ((MethodExpression) expression).getParameters();
                List<ColumnInfo> result = new ArrayList<>();
                for (CommonExpression ce : params) {
                    List<ColumnInfo> cis = getColumnInfo(ce);
                    if (cis != null) {
                        result.addAll(cis);
                    }
                }
                return result;
            } else if (expression instanceof PropertyExpression) {
                PropertyExpression propExpression = (PropertyExpression) expression;
                EdmTyped property = targetType.getProperty(propExpression.getPropertyName());
                if (property instanceof EdmProperty) {
                    ColumnInfo ci = query.getSQLTableColumnInfo(targetType, (EdmProperty) property);
                    return Collections.singletonList(ci);
                } else {
                    return null;
                }
            } else if (expression instanceof LiteralExpression) {
                return null;
            }
        } catch (EdmException e) {
            throw new RuntimeException("Unable to parse where expression", e);
        }
        return null;
    }

    /**
     * Write param.
     *
     * @param expression the expression
     * @param literal the literal
     * @param format the format
     * @param info the info
     * @return the string
     */
    private String writeParam(final CommonExpression expression, final String literal, final String format, final ColumnInfo info) {
        if (expression.getKind() == ExpressionKind.LITERAL) {
            final String value = (format != null ? String.format(format, literal) : literal);
            EdmSimpleType edmType = (EdmSimpleType) expression.getEdmType();
            Object literalValue = evaluateDateTimeExpressions(value, edmType);
            whereClauseParams.add(SQLWhereClause.param(literalValue, edmType, info));
            // we should be using a prepared statement, so therefore always return the question marks here,
            // since we have added the param to the query
            return "?";
        }
        return literal;
    }

    /**
     * Escape operator precedence.
     *
     * @param expression the expression
     * @param subExpression the sub expression
     * @param subExpressionString the sub expression string
     * @return the object
     */
    private Object escapeOperatorPrecedence(BinaryExpression expression, CommonExpression subExpression, String subExpressionString) {

        if (subExpression.getKind() == ExpressionKind.BINARY) {
            final Integer expressionPriority = binaryOperatorPriorities.get(expression.getOperator());
            final Integer subExpressionPriority = binaryOperatorPriorities.get(((BinaryExpression) subExpression).getOperator());
            if (expressionPriority > subExpressionPriority) {
                // If subExpression has lower implicit priority use brackets to invert precedence
                return "(" + subExpressionString + ")";
            }
        }

        // Simply return clause as is
        return subExpressionString;
    }

}
