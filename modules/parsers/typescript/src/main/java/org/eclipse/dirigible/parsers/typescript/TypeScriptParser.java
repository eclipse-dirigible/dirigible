/*
 * Copyright (c) 2010-2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
// Generated from org/eclipse/dirigible/parsers/typescript/TypeScriptParser.g4 by ANTLR 4.13.2
package org.eclipse.dirigible.parsers.typescript;

import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.*;
import org.antlr.v4.runtime.tree.*;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast", "CheckReturnValue", "this-escape"})
public class TypeScriptParser extends TypeScriptParserBase {
    static {
        RuntimeMetaData.checkVersion("4.13.2", RuntimeMetaData.VERSION);
    }

    protected static final DFA[] _decisionToDFA;
    protected static final PredictionContextCache _sharedContextCache = new PredictionContextCache();
    public static final int MultiLineComment = 1, SingleLineComment = 2, RegularExpressionLiteral = 3, OpenBracket = 4, CloseBracket = 5,
            OpenParen = 6, CloseParen = 7, OpenBrace = 8, TemplateCloseBrace = 9, CloseBrace = 10, SemiColon = 11, Comma = 12, Assign = 13,
            QuestionMark = 14, QuestionMarkDot = 15, Colon = 16, Ellipsis = 17, Dot = 18, PlusPlus = 19, MinusMinus = 20, Plus = 21,
            Minus = 22, BitNot = 23, Not = 24, Multiply = 25, Divide = 26, Modulus = 27, Power = 28, NullCoalesce = 29, Hashtag = 30,
            LeftShiftArithmetic = 31, LessThan = 32, MoreThan = 33, LessThanEquals = 34, GreaterThanEquals = 35, Equals_ = 36,
            NotEquals = 37, IdentityEquals = 38, IdentityNotEquals = 39, BitAnd = 40, BitXOr = 41, BitOr = 42, And = 43, Or = 44,
            MultiplyAssign = 45, DivideAssign = 46, ModulusAssign = 47, PlusAssign = 48, MinusAssign = 49, LeftShiftArithmeticAssign = 50,
            RightShiftArithmeticAssign = 51, RightShiftLogicalAssign = 52, BitAndAssign = 53, BitXorAssign = 54, BitOrAssign = 55,
            PowerAssign = 56, NullishCoalescingAssign = 57, ARROW = 58, NullLiteral = 59, BooleanLiteral = 60, DecimalLiteral = 61,
            HexIntegerLiteral = 62, OctalIntegerLiteral = 63, OctalIntegerLiteral2 = 64, BinaryIntegerLiteral = 65,
            BigHexIntegerLiteral = 66, BigOctalIntegerLiteral = 67, BigBinaryIntegerLiteral = 68, BigDecimalIntegerLiteral = 69, Break = 70,
            Do = 71, Instanceof = 72, Typeof = 73, Case = 74, Else = 75, New = 76, Var = 77, Catch = 78, Finally = 79, Return = 80,
            Void = 81, Continue = 82, For = 83, Switch = 84, While = 85, Debugger = 86, Function_ = 87, This = 88, With = 89, Default = 90,
            If = 91, Throw = 92, Delete = 93, In = 94, Try = 95, As = 96, From = 97, ReadOnly = 98, Async = 99, Await = 100, Yield = 101,
            YieldStar = 102, Class = 103, Enum = 104, Extends = 105, Super = 106, Const = 107, Export = 108, Import = 109, Implements = 110,
            Let = 111, Private = 112, Public = 113, Interface = 114, Package = 115, Protected = 116, Static = 117, Any = 118, Number = 119,
            Never = 120, Boolean = 121, String = 122, Unique = 123, Symbol = 124, Undefined = 125, Object = 126, Of = 127, KeyOf = 128,
            TypeAlias = 129, Constructor = 130, Namespace = 131, Require = 132, Module = 133, Declare = 134, Abstract = 135, Is = 136,
            At = 137, Identifier = 138, StringLiteral = 139, BackTick = 140, WhiteSpaces = 141, LineTerminator = 142, HtmlComment = 143,
            CDataComment = 144, UnexpectedCharacter = 145, TemplateStringEscapeAtom = 146, TemplateStringStartExpression = 147,
            TemplateStringAtom = 148;
    public static final int RULE_initializer = 0, RULE_bindingPattern = 1, RULE_typeParameters = 2, RULE_typeParameterList = 3,
            RULE_typeParameter = 4, RULE_constraint = 5, RULE_typeArguments = 6, RULE_typeArgumentList = 7, RULE_typeArgument = 8,
            RULE_type_ = 9, RULE_unionOrIntersectionOrPrimaryType = 10, RULE_primaryType = 11, RULE_predefinedType = 12,
            RULE_typeReference = 13, RULE_typeGeneric = 14, RULE_typeName = 15, RULE_objectType = 16, RULE_typeBody = 17,
            RULE_typeMemberList = 18, RULE_typeMember = 19, RULE_arrayType = 20, RULE_tupleType = 21, RULE_tupleElementTypes = 22,
            RULE_functionType = 23, RULE_constructorType = 24, RULE_typeQuery = 25, RULE_typeQueryExpression = 26,
            RULE_propertySignatur = 27, RULE_typeAnnotation = 28, RULE_callSignature = 29, RULE_parameterList = 30,
            RULE_requiredParameterList = 31, RULE_parameter = 32, RULE_optionalParameter = 33, RULE_restParameter = 34,
            RULE_requiredParameter = 35, RULE_accessibilityModifier = 36, RULE_identifierOrPattern = 37, RULE_constructSignature = 38,
            RULE_indexSignature = 39, RULE_methodSignature = 40, RULE_typeAliasDeclaration = 41, RULE_constructorDeclaration = 42,
            RULE_interfaceDeclaration = 43, RULE_interfaceExtendsClause = 44, RULE_classOrInterfaceTypeList = 45, RULE_enumDeclaration = 46,
            RULE_enumBody = 47, RULE_enumMemberList = 48, RULE_enumMember = 49, RULE_namespaceDeclaration = 50, RULE_namespaceName = 51,
            RULE_importAliasDeclaration = 52, RULE_decoratorList = 53, RULE_decorator = 54, RULE_decoratorMemberExpression = 55,
            RULE_decoratorCallExpression = 56, RULE_program = 57, RULE_sourceElement = 58, RULE_statement = 59, RULE_block = 60,
            RULE_statementList = 61, RULE_abstractDeclaration = 62, RULE_importStatement = 63, RULE_importFromBlock = 64,
            RULE_importModuleItems = 65, RULE_importAliasName = 66, RULE_moduleExportName = 67, RULE_importedBinding = 68,
            RULE_importDefault = 69, RULE_importNamespace = 70, RULE_importFrom = 71, RULE_aliasName = 72, RULE_exportStatement = 73,
            RULE_exportFromBlock = 74, RULE_exportModuleItems = 75, RULE_exportAliasName = 76, RULE_declaration = 77,
            RULE_variableStatement = 78, RULE_variableDeclarationList = 79, RULE_variableDeclaration = 80, RULE_emptyStatement_ = 81,
            RULE_expressionStatement = 82, RULE_ifStatement = 83, RULE_iterationStatement = 84, RULE_varModifier = 85,
            RULE_continueStatement = 86, RULE_breakStatement = 87, RULE_returnStatement = 88, RULE_yieldStatement = 89,
            RULE_withStatement = 90, RULE_switchStatement = 91, RULE_caseBlock = 92, RULE_caseClauses = 93, RULE_caseClause = 94,
            RULE_defaultClause = 95, RULE_labelledStatement = 96, RULE_throwStatement = 97, RULE_tryStatement = 98,
            RULE_catchProduction = 99, RULE_finallyProduction = 100, RULE_debuggerStatement = 101, RULE_functionDeclaration = 102,
            RULE_classDeclaration = 103, RULE_classHeritage = 104, RULE_classTail = 105, RULE_classExtendsClause = 106,
            RULE_implementsClause = 107, RULE_classElement = 108, RULE_propertyMemberDeclaration = 109, RULE_propertyMemberBase = 110,
            RULE_indexMemberDeclaration = 111, RULE_generatorMethod = 112, RULE_generatorFunctionDeclaration = 113,
            RULE_generatorBlock = 114, RULE_generatorDefinition = 115, RULE_iteratorBlock = 116, RULE_iteratorDefinition = 117,
            RULE_classElementName = 118, RULE_privateIdentifier = 119, RULE_formalParameterList = 120, RULE_formalParameterArg = 121,
            RULE_lastFormalParameterArg = 122, RULE_functionBody = 123, RULE_sourceElements = 124, RULE_arrayLiteral = 125,
            RULE_elementList = 126, RULE_arrayElement = 127, RULE_objectLiteral = 128, RULE_propertyAssignment = 129,
            RULE_getAccessor = 130, RULE_setAccessor = 131, RULE_propertyName = 132, RULE_arguments = 133, RULE_argumentList = 134,
            RULE_argument = 135, RULE_expressionSequence = 136, RULE_singleExpression = 137, RULE_asExpression = 138, RULE_assignable = 139,
            RULE_anonymousFunction = 140, RULE_arrowFunctionDeclaration = 141, RULE_arrowFunctionParameters = 142,
            RULE_arrowFunctionBody = 143, RULE_assignmentOperator = 144, RULE_literal = 145, RULE_templateStringLiteral = 146,
            RULE_templateStringAtom = 147, RULE_numericLiteral = 148, RULE_bigintLiteral = 149, RULE_getter = 150, RULE_setter = 151,
            RULE_identifierName = 152, RULE_identifier = 153, RULE_identifierOrKeyWord = 154, RULE_reservedWord = 155, RULE_keyword = 156,
            RULE_eos = 157;

    private static String[] makeRuleNames() {
        return new String[] {"initializer", "bindingPattern", "typeParameters", "typeParameterList", "typeParameter", "constraint",
                "typeArguments", "typeArgumentList", "typeArgument", "type_", "unionOrIntersectionOrPrimaryType", "primaryType",
                "predefinedType", "typeReference", "typeGeneric", "typeName", "objectType", "typeBody", "typeMemberList", "typeMember",
                "arrayType", "tupleType", "tupleElementTypes", "functionType", "constructorType", "typeQuery", "typeQueryExpression",
                "propertySignatur", "typeAnnotation", "callSignature", "parameterList", "requiredParameterList", "parameter",
                "optionalParameter", "restParameter", "requiredParameter", "accessibilityModifier", "identifierOrPattern",
                "constructSignature", "indexSignature", "methodSignature", "typeAliasDeclaration", "constructorDeclaration",
                "interfaceDeclaration", "interfaceExtendsClause", "classOrInterfaceTypeList", "enumDeclaration", "enumBody",
                "enumMemberList", "enumMember", "namespaceDeclaration", "namespaceName", "importAliasDeclaration", "decoratorList",
                "decorator", "decoratorMemberExpression", "decoratorCallExpression", "program", "sourceElement", "statement", "block",
                "statementList", "abstractDeclaration", "importStatement", "importFromBlock", "importModuleItems", "importAliasName",
                "moduleExportName", "importedBinding", "importDefault", "importNamespace", "importFrom", "aliasName", "exportStatement",
                "exportFromBlock", "exportModuleItems", "exportAliasName", "declaration", "variableStatement", "variableDeclarationList",
                "variableDeclaration", "emptyStatement_", "expressionStatement", "ifStatement", "iterationStatement", "varModifier",
                "continueStatement", "breakStatement", "returnStatement", "yieldStatement", "withStatement", "switchStatement", "caseBlock",
                "caseClauses", "caseClause", "defaultClause", "labelledStatement", "throwStatement", "tryStatement", "catchProduction",
                "finallyProduction", "debuggerStatement", "functionDeclaration", "classDeclaration", "classHeritage", "classTail",
                "classExtendsClause", "implementsClause", "classElement", "propertyMemberDeclaration", "propertyMemberBase",
                "indexMemberDeclaration", "generatorMethod", "generatorFunctionDeclaration", "generatorBlock", "generatorDefinition",
                "iteratorBlock", "iteratorDefinition", "classElementName", "privateIdentifier", "formalParameterList", "formalParameterArg",
                "lastFormalParameterArg", "functionBody", "sourceElements", "arrayLiteral", "elementList", "arrayElement", "objectLiteral",
                "propertyAssignment", "getAccessor", "setAccessor", "propertyName", "arguments", "argumentList", "argument",
                "expressionSequence", "singleExpression", "asExpression", "assignable", "anonymousFunction", "arrowFunctionDeclaration",
                "arrowFunctionParameters", "arrowFunctionBody", "assignmentOperator", "literal", "templateStringLiteral",
                "templateStringAtom", "numericLiteral", "bigintLiteral", "getter", "setter", "identifierName", "identifier",
                "identifierOrKeyWord", "reservedWord", "keyword", "eos"};
    }

    public static final String[] ruleNames = makeRuleNames();

    private static String[] makeLiteralNames() {
        return new String[] {null, null, null, null, "'['", "']'", "'('", "')'", "'{'", null, "'}'", "';'", "','", "'='", "'?'", "'?.'",
                "':'", "'...'", "'.'", "'++'", "'--'", "'+'", "'-'", "'~'", "'!'", "'*'", "'/'", "'%'", "'**'", "'??'", "'#'", "'<<'",
                "'<'", "'>'", "'<='", "'>='", "'=='", "'!='", "'==='", "'!=='", "'&'", "'^'", "'|'", "'&&'", "'||'", "'*='", "'/='", "'%='",
                "'+='", "'-='", "'<<='", "'>>='", "'>>>='", "'&='", "'^='", "'|='", "'**='", "'??='", "'=>'", "'null'", null, null, null,
                null, null, null, null, null, null, null, "'break'", "'do'", "'instanceof'", "'typeof'", "'case'", "'else'", "'new'",
                "'var'", "'catch'", "'finally'", "'return'", "'void'", "'continue'", "'for'", "'switch'", "'while'", "'debugger'",
                "'function'", "'this'", "'with'", "'default'", "'if'", "'throw'", "'delete'", "'in'", "'try'", "'as'", "'from'",
                "'readonly'", "'async'", "'await'", "'yield'", "'yield*'", "'class'", "'enum'", "'extends'", "'super'", "'const'",
                "'export'", "'import'", "'implements'", "'let'", "'private'", "'public'", "'interface'", "'package'", "'protected'",
                "'static'", "'any'", "'number'", "'never'", "'boolean'", "'string'", "'unique'", "'symbol'", "'undefined'", "'object'",
                "'of'", "'keyof'", "'type'", "'constructor'", "'namespace'", "'require'", "'module'", "'declare'", "'abstract'", "'is'",
                "'@'"};
    }

    private static final String[] _LITERAL_NAMES = makeLiteralNames();

    private static String[] makeSymbolicNames() {
        return new String[] {null, "MultiLineComment", "SingleLineComment", "RegularExpressionLiteral", "OpenBracket", "CloseBracket",
                "OpenParen", "CloseParen", "OpenBrace", "TemplateCloseBrace", "CloseBrace", "SemiColon", "Comma", "Assign", "QuestionMark",
                "QuestionMarkDot", "Colon", "Ellipsis", "Dot", "PlusPlus", "MinusMinus", "Plus", "Minus", "BitNot", "Not", "Multiply",
                "Divide", "Modulus", "Power", "NullCoalesce", "Hashtag", "LeftShiftArithmetic", "LessThan", "MoreThan", "LessThanEquals",
                "GreaterThanEquals", "Equals_", "NotEquals", "IdentityEquals", "IdentityNotEquals", "BitAnd", "BitXOr", "BitOr", "And",
                "Or", "MultiplyAssign", "DivideAssign", "ModulusAssign", "PlusAssign", "MinusAssign", "LeftShiftArithmeticAssign",
                "RightShiftArithmeticAssign", "RightShiftLogicalAssign", "BitAndAssign", "BitXorAssign", "BitOrAssign", "PowerAssign",
                "NullishCoalescingAssign", "ARROW", "NullLiteral", "BooleanLiteral", "DecimalLiteral", "HexIntegerLiteral",
                "OctalIntegerLiteral", "OctalIntegerLiteral2", "BinaryIntegerLiteral", "BigHexIntegerLiteral", "BigOctalIntegerLiteral",
                "BigBinaryIntegerLiteral", "BigDecimalIntegerLiteral", "Break", "Do", "Instanceof", "Typeof", "Case", "Else", "New", "Var",
                "Catch", "Finally", "Return", "Void", "Continue", "For", "Switch", "While", "Debugger", "Function_", "This", "With",
                "Default", "If", "Throw", "Delete", "In", "Try", "As", "From", "ReadOnly", "Async", "Await", "Yield", "YieldStar", "Class",
                "Enum", "Extends", "Super", "Const", "Export", "Import", "Implements", "Let", "Private", "Public", "Interface", "Package",
                "Protected", "Static", "Any", "Number", "Never", "Boolean", "String", "Unique", "Symbol", "Undefined", "Object", "Of",
                "KeyOf", "TypeAlias", "Constructor", "Namespace", "Require", "Module", "Declare", "Abstract", "Is", "At", "Identifier",
                "StringLiteral", "BackTick", "WhiteSpaces", "LineTerminator", "HtmlComment", "CDataComment", "UnexpectedCharacter",
                "TemplateStringEscapeAtom", "TemplateStringStartExpression", "TemplateStringAtom"};
    }

    private static final String[] _SYMBOLIC_NAMES = makeSymbolicNames();
    public static final Vocabulary VOCABULARY = new VocabularyImpl(_LITERAL_NAMES, _SYMBOLIC_NAMES);

    /**
     * @deprecated Use {@link #VOCABULARY} instead.
     */
    @Deprecated
    public static final String[] tokenNames;
    static {
        tokenNames = new String[_SYMBOLIC_NAMES.length];
        for (int i = 0; i < tokenNames.length; i++) {
            tokenNames[i] = VOCABULARY.getLiteralName(i);
            if (tokenNames[i] == null) {
                tokenNames[i] = VOCABULARY.getSymbolicName(i);
            }

            if (tokenNames[i] == null) {
                tokenNames[i] = "<INVALID>";
            }
        }
    }

    @Override
    @Deprecated
    public String[] getTokenNames() {
        return tokenNames;
    }

    @Override

    public Vocabulary getVocabulary() {
        return VOCABULARY;
    }

    @Override
    public String getGrammarFileName() {
        return "TypeScriptParser.g4";
    }

    @Override
    public String[] getRuleNames() {
        return ruleNames;
    }

    @Override
    public String getSerializedATN() {
        return _serializedATN;
    }

    @Override
    public ATN getATN() {
        return _ATN;
    }

    public TypeScriptParser(TokenStream input) {
        super(input);
        _interp = new ParserATNSimulator(this, _ATN, _decisionToDFA, _sharedContextCache);
    }

    @SuppressWarnings("CheckReturnValue")
    public static class InitializerContext extends ParserRuleContext {
        public TerminalNode Assign() {
            return getToken(TypeScriptParser.Assign, 0);
        }

        public SingleExpressionContext singleExpression() {
            return getRuleContext(SingleExpressionContext.class, 0);
        }

        public InitializerContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_initializer;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterInitializer(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitInitializer(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitInitializer(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final InitializerContext initializer() throws RecognitionException {
        InitializerContext _localctx = new InitializerContext(_ctx, getState());
        enterRule(_localctx, 0, RULE_initializer);
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(316);
                match(Assign);
                setState(317);
                singleExpression(0);
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class BindingPatternContext extends ParserRuleContext {
        public ArrayLiteralContext arrayLiteral() {
            return getRuleContext(ArrayLiteralContext.class, 0);
        }

        public ObjectLiteralContext objectLiteral() {
            return getRuleContext(ObjectLiteralContext.class, 0);
        }

        public BindingPatternContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_bindingPattern;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterBindingPattern(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitBindingPattern(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitBindingPattern(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final BindingPatternContext bindingPattern() throws RecognitionException {
        BindingPatternContext _localctx = new BindingPatternContext(_ctx, getState());
        enterRule(_localctx, 2, RULE_bindingPattern);
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(321);
                _errHandler.sync(this);
                switch (_input.LA(1)) {
                    case OpenBracket: {
                        setState(319);
                        arrayLiteral();
                    }
                        break;
                    case OpenBrace: {
                        setState(320);
                        objectLiteral();
                    }
                        break;
                    default:
                        throw new NoViableAltException(this);
                }
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class TypeParametersContext extends ParserRuleContext {
        public TerminalNode LessThan() {
            return getToken(TypeScriptParser.LessThan, 0);
        }

        public TerminalNode MoreThan() {
            return getToken(TypeScriptParser.MoreThan, 0);
        }

        public TypeParameterListContext typeParameterList() {
            return getRuleContext(TypeParameterListContext.class, 0);
        }

        public TypeParametersContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_typeParameters;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterTypeParameters(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitTypeParameters(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitTypeParameters(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final TypeParametersContext typeParameters() throws RecognitionException {
        TypeParametersContext _localctx = new TypeParametersContext(_ctx, getState());
        enterRule(_localctx, 4, RULE_typeParameters);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(323);
                match(LessThan);
                setState(325);
                _errHandler.sync(this);
                _la = _input.LA(1);
                if (_la == LessThan || ((((_la - 96)) & ~0x3f) == 0 && ((1L << (_la - 96)) & 5016517607467L) != 0)) {
                    {
                        setState(324);
                        typeParameterList();
                    }
                }

                setState(327);
                match(MoreThan);
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class TypeParameterListContext extends ParserRuleContext {
        public List<TypeParameterContext> typeParameter() {
            return getRuleContexts(TypeParameterContext.class);
        }

        public TypeParameterContext typeParameter(int i) {
            return getRuleContext(TypeParameterContext.class, i);
        }

        public List<TerminalNode> Comma() {
            return getTokens(TypeScriptParser.Comma);
        }

        public TerminalNode Comma(int i) {
            return getToken(TypeScriptParser.Comma, i);
        }

        public TypeParameterListContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_typeParameterList;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterTypeParameterList(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitTypeParameterList(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitTypeParameterList(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final TypeParameterListContext typeParameterList() throws RecognitionException {
        TypeParameterListContext _localctx = new TypeParameterListContext(_ctx, getState());
        enterRule(_localctx, 6, RULE_typeParameterList);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(329);
                typeParameter();
                setState(334);
                _errHandler.sync(this);
                _la = _input.LA(1);
                while (_la == Comma) {
                    {
                        {
                            setState(330);
                            match(Comma);
                            setState(331);
                            typeParameter();
                        }
                    }
                    setState(336);
                    _errHandler.sync(this);
                    _la = _input.LA(1);
                }
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class TypeParameterContext extends ParserRuleContext {
        public IdentifierContext identifier() {
            return getRuleContext(IdentifierContext.class, 0);
        }

        public ConstraintContext constraint() {
            return getRuleContext(ConstraintContext.class, 0);
        }

        public TerminalNode Assign() {
            return getToken(TypeScriptParser.Assign, 0);
        }

        public TypeArgumentContext typeArgument() {
            return getRuleContext(TypeArgumentContext.class, 0);
        }

        public TypeParametersContext typeParameters() {
            return getRuleContext(TypeParametersContext.class, 0);
        }

        public TypeParameterContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_typeParameter;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterTypeParameter(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitTypeParameter(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitTypeParameter(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final TypeParameterContext typeParameter() throws RecognitionException {
        TypeParameterContext _localctx = new TypeParameterContext(_ctx, getState());
        enterRule(_localctx, 8, RULE_typeParameter);
        int _la;
        try {
            setState(346);
            _errHandler.sync(this);
            switch (getInterpreter().adaptivePredict(_input, 4, _ctx)) {
                case 1:
                    enterOuterAlt(_localctx, 1); {
                    setState(337);
                    identifier();
                    setState(339);
                    _errHandler.sync(this);
                    _la = _input.LA(1);
                    if (_la == Extends) {
                        {
                            setState(338);
                            constraint();
                        }
                    }

                }
                    break;
                case 2:
                    enterOuterAlt(_localctx, 2); {
                    setState(341);
                    identifier();
                    setState(342);
                    match(Assign);
                    setState(343);
                    typeArgument();
                }
                    break;
                case 3:
                    enterOuterAlt(_localctx, 3); {
                    setState(345);
                    typeParameters();
                }
                    break;
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class ConstraintContext extends ParserRuleContext {
        public TerminalNode Extends() {
            return getToken(TypeScriptParser.Extends, 0);
        }

        public Type_Context type_() {
            return getRuleContext(Type_Context.class, 0);
        }

        public ConstraintContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_constraint;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterConstraint(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitConstraint(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitConstraint(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final ConstraintContext constraint() throws RecognitionException {
        ConstraintContext _localctx = new ConstraintContext(_ctx, getState());
        enterRule(_localctx, 10, RULE_constraint);
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(348);
                match(Extends);
                setState(349);
                type_();
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class TypeArgumentsContext extends ParserRuleContext {
        public TerminalNode LessThan() {
            return getToken(TypeScriptParser.LessThan, 0);
        }

        public TerminalNode MoreThan() {
            return getToken(TypeScriptParser.MoreThan, 0);
        }

        public TypeArgumentListContext typeArgumentList() {
            return getRuleContext(TypeArgumentListContext.class, 0);
        }

        public TypeArgumentsContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_typeArguments;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterTypeArguments(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitTypeArguments(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitTypeArguments(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final TypeArgumentsContext typeArguments() throws RecognitionException {
        TypeArgumentsContext _localctx = new TypeArgumentsContext(_ctx, getState());
        enterRule(_localctx, 12, RULE_typeArguments);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(351);
                match(LessThan);
                setState(353);
                _errHandler.sync(this);
                _la = _input.LA(1);
                if ((((_la) & ~0x3f) == 0 && ((1L << _la) & 4035230767977070928L) != 0)
                        || ((((_la - 73)) & ~0x3f) == 0 && ((1L << (_la - 73)) & 5188111586719465737L) != 0) || _la == Identifier
                        || _la == StringLiteral) {
                    {
                        setState(352);
                        typeArgumentList();
                    }
                }

                setState(355);
                match(MoreThan);
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class TypeArgumentListContext extends ParserRuleContext {
        public List<TypeArgumentContext> typeArgument() {
            return getRuleContexts(TypeArgumentContext.class);
        }

        public TypeArgumentContext typeArgument(int i) {
            return getRuleContext(TypeArgumentContext.class, i);
        }

        public List<TerminalNode> Comma() {
            return getTokens(TypeScriptParser.Comma);
        }

        public TerminalNode Comma(int i) {
            return getToken(TypeScriptParser.Comma, i);
        }

        public TypeArgumentListContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_typeArgumentList;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterTypeArgumentList(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitTypeArgumentList(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitTypeArgumentList(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final TypeArgumentListContext typeArgumentList() throws RecognitionException {
        TypeArgumentListContext _localctx = new TypeArgumentListContext(_ctx, getState());
        enterRule(_localctx, 14, RULE_typeArgumentList);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(357);
                typeArgument();
                setState(362);
                _errHandler.sync(this);
                _la = _input.LA(1);
                while (_la == Comma) {
                    {
                        {
                            setState(358);
                            match(Comma);
                            setState(359);
                            typeArgument();
                        }
                    }
                    setState(364);
                    _errHandler.sync(this);
                    _la = _input.LA(1);
                }
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class TypeArgumentContext extends ParserRuleContext {
        public Type_Context type_() {
            return getRuleContext(Type_Context.class, 0);
        }

        public TypeArgumentContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_typeArgument;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterTypeArgument(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitTypeArgument(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitTypeArgument(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final TypeArgumentContext typeArgument() throws RecognitionException {
        TypeArgumentContext _localctx = new TypeArgumentContext(_ctx, getState());
        enterRule(_localctx, 16, RULE_typeArgument);
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(365);
                type_();
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class Type_Context extends ParserRuleContext {
        public UnionOrIntersectionOrPrimaryTypeContext unionOrIntersectionOrPrimaryType() {
            return getRuleContext(UnionOrIntersectionOrPrimaryTypeContext.class, 0);
        }

        public TerminalNode BitOr() {
            return getToken(TypeScriptParser.BitOr, 0);
        }

        public TerminalNode BitAnd() {
            return getToken(TypeScriptParser.BitAnd, 0);
        }

        public FunctionTypeContext functionType() {
            return getRuleContext(FunctionTypeContext.class, 0);
        }

        public ConstructorTypeContext constructorType() {
            return getRuleContext(ConstructorTypeContext.class, 0);
        }

        public TypeGenericContext typeGeneric() {
            return getRuleContext(TypeGenericContext.class, 0);
        }

        public Type_Context(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_type_;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterType_(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitType_(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitType_(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final Type_Context type_() throws RecognitionException {
        Type_Context _localctx = new Type_Context(_ctx, getState());
        enterRule(_localctx, 18, RULE_type_);
        int _la;
        try {
            setState(374);
            _errHandler.sync(this);
            switch (getInterpreter().adaptivePredict(_input, 8, _ctx)) {
                case 1:
                    enterOuterAlt(_localctx, 1); {
                    setState(368);
                    _errHandler.sync(this);
                    _la = _input.LA(1);
                    if (_la == BitAnd || _la == BitOr) {
                        {
                            setState(367);
                            _la = _input.LA(1);
                            if (!(_la == BitAnd || _la == BitOr)) {
                                _errHandler.recoverInline(this);
                            } else {
                                if (_input.LA(1) == Token.EOF)
                                    matchedEOF = true;
                                _errHandler.reportMatch(this);
                                consume();
                            }
                        }
                    }

                    setState(370);
                    unionOrIntersectionOrPrimaryType(0);
                }
                    break;
                case 2:
                    enterOuterAlt(_localctx, 2); {
                    setState(371);
                    functionType();
                }
                    break;
                case 3:
                    enterOuterAlt(_localctx, 3); {
                    setState(372);
                    constructorType();
                }
                    break;
                case 4:
                    enterOuterAlt(_localctx, 4); {
                    setState(373);
                    typeGeneric();
                }
                    break;
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class UnionOrIntersectionOrPrimaryTypeContext extends ParserRuleContext {
        public UnionOrIntersectionOrPrimaryTypeContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_unionOrIntersectionOrPrimaryType;
        }

        public UnionOrIntersectionOrPrimaryTypeContext() {}

        public void copyFrom(UnionOrIntersectionOrPrimaryTypeContext ctx) {
            super.copyFrom(ctx);
        }
    }
    @SuppressWarnings("CheckReturnValue")
    public static class IntersectionContext extends UnionOrIntersectionOrPrimaryTypeContext {
        public List<UnionOrIntersectionOrPrimaryTypeContext> unionOrIntersectionOrPrimaryType() {
            return getRuleContexts(UnionOrIntersectionOrPrimaryTypeContext.class);
        }

        public UnionOrIntersectionOrPrimaryTypeContext unionOrIntersectionOrPrimaryType(int i) {
            return getRuleContext(UnionOrIntersectionOrPrimaryTypeContext.class, i);
        }

        public TerminalNode BitAnd() {
            return getToken(TypeScriptParser.BitAnd, 0);
        }

        public IntersectionContext(UnionOrIntersectionOrPrimaryTypeContext ctx) {
            copyFrom(ctx);
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterIntersection(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitIntersection(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitIntersection(this);
            else
                return visitor.visitChildren(this);
        }
    }
    @SuppressWarnings("CheckReturnValue")
    public static class PrimaryContext extends UnionOrIntersectionOrPrimaryTypeContext {
        public PrimaryTypeContext primaryType() {
            return getRuleContext(PrimaryTypeContext.class, 0);
        }

        public PrimaryContext(UnionOrIntersectionOrPrimaryTypeContext ctx) {
            copyFrom(ctx);
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterPrimary(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitPrimary(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitPrimary(this);
            else
                return visitor.visitChildren(this);
        }
    }
    @SuppressWarnings("CheckReturnValue")
    public static class UnionContext extends UnionOrIntersectionOrPrimaryTypeContext {
        public List<UnionOrIntersectionOrPrimaryTypeContext> unionOrIntersectionOrPrimaryType() {
            return getRuleContexts(UnionOrIntersectionOrPrimaryTypeContext.class);
        }

        public UnionOrIntersectionOrPrimaryTypeContext unionOrIntersectionOrPrimaryType(int i) {
            return getRuleContext(UnionOrIntersectionOrPrimaryTypeContext.class, i);
        }

        public TerminalNode BitOr() {
            return getToken(TypeScriptParser.BitOr, 0);
        }

        public UnionContext(UnionOrIntersectionOrPrimaryTypeContext ctx) {
            copyFrom(ctx);
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterUnion(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitUnion(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitUnion(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final UnionOrIntersectionOrPrimaryTypeContext unionOrIntersectionOrPrimaryType() throws RecognitionException {
        return unionOrIntersectionOrPrimaryType(0);
    }

    private UnionOrIntersectionOrPrimaryTypeContext unionOrIntersectionOrPrimaryType(int _p) throws RecognitionException {
        ParserRuleContext _parentctx = _ctx;
        int _parentState = getState();
        UnionOrIntersectionOrPrimaryTypeContext _localctx = new UnionOrIntersectionOrPrimaryTypeContext(_ctx, _parentState);
        UnionOrIntersectionOrPrimaryTypeContext _prevctx = _localctx;
        int _startState = 20;
        enterRecursionRule(_localctx, 20, RULE_unionOrIntersectionOrPrimaryType, _p);
        try {
            int _alt;
            enterOuterAlt(_localctx, 1);
            {
                {
                    _localctx = new PrimaryContext(_localctx);
                    _ctx = _localctx;
                    _prevctx = _localctx;

                    setState(377);
                    primaryType(0);
                }
                _ctx.stop = _input.LT(-1);
                setState(387);
                _errHandler.sync(this);
                _alt = getInterpreter().adaptivePredict(_input, 10, _ctx);
                while (_alt != 2 && _alt != org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER) {
                    if (_alt == 1) {
                        if (_parseListeners != null)
                            triggerExitRuleEvent();
                        _prevctx = _localctx;
                        {
                            setState(385);
                            _errHandler.sync(this);
                            switch (getInterpreter().adaptivePredict(_input, 9, _ctx)) {
                                case 1: {
                                    _localctx = new UnionContext(new UnionOrIntersectionOrPrimaryTypeContext(_parentctx, _parentState));
                                    pushNewRecursionContext(_localctx, _startState, RULE_unionOrIntersectionOrPrimaryType);
                                    setState(379);
                                    if (!(precpred(_ctx, 3)))
                                        throw new FailedPredicateException(this, "precpred(_ctx, 3)");
                                    setState(380);
                                    match(BitOr);
                                    setState(381);
                                    unionOrIntersectionOrPrimaryType(4);
                                }
                                    break;
                                case 2: {
                                    _localctx =
                                            new IntersectionContext(new UnionOrIntersectionOrPrimaryTypeContext(_parentctx, _parentState));
                                    pushNewRecursionContext(_localctx, _startState, RULE_unionOrIntersectionOrPrimaryType);
                                    setState(382);
                                    if (!(precpred(_ctx, 2)))
                                        throw new FailedPredicateException(this, "precpred(_ctx, 2)");
                                    setState(383);
                                    match(BitAnd);
                                    setState(384);
                                    unionOrIntersectionOrPrimaryType(3);
                                }
                                    break;
                            }
                        }
                    }
                    setState(389);
                    _errHandler.sync(this);
                    _alt = getInterpreter().adaptivePredict(_input, 10, _ctx);
                }
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            unrollRecursionContexts(_parentctx);
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class PrimaryTypeContext extends ParserRuleContext {
        public PrimaryTypeContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_primaryType;
        }

        public PrimaryTypeContext() {}

        public void copyFrom(PrimaryTypeContext ctx) {
            super.copyFrom(ctx);
        }
    }
    @SuppressWarnings("CheckReturnValue")
    public static class RedefinitionOfTypeContext extends PrimaryTypeContext {
        public TypeReferenceContext typeReference() {
            return getRuleContext(TypeReferenceContext.class, 0);
        }

        public TerminalNode Is() {
            return getToken(TypeScriptParser.Is, 0);
        }

        public PrimaryTypeContext primaryType() {
            return getRuleContext(PrimaryTypeContext.class, 0);
        }

        public RedefinitionOfTypeContext(PrimaryTypeContext ctx) {
            copyFrom(ctx);
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterRedefinitionOfType(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitRedefinitionOfType(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitRedefinitionOfType(this);
            else
                return visitor.visitChildren(this);
        }
    }
    @SuppressWarnings("CheckReturnValue")
    public static class PredefinedPrimTypeContext extends PrimaryTypeContext {
        public PredefinedTypeContext predefinedType() {
            return getRuleContext(PredefinedTypeContext.class, 0);
        }

        public PredefinedPrimTypeContext(PrimaryTypeContext ctx) {
            copyFrom(ctx);
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterPredefinedPrimType(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitPredefinedPrimType(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitPredefinedPrimType(this);
            else
                return visitor.visitChildren(this);
        }
    }
    @SuppressWarnings("CheckReturnValue")
    public static class ArrayPrimTypeContext extends PrimaryTypeContext {
        public List<PrimaryTypeContext> primaryType() {
            return getRuleContexts(PrimaryTypeContext.class);
        }

        public PrimaryTypeContext primaryType(int i) {
            return getRuleContext(PrimaryTypeContext.class, i);
        }

        public TerminalNode OpenBracket() {
            return getToken(TypeScriptParser.OpenBracket, 0);
        }

        public TerminalNode CloseBracket() {
            return getToken(TypeScriptParser.CloseBracket, 0);
        }

        public ArrayPrimTypeContext(PrimaryTypeContext ctx) {
            copyFrom(ctx);
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterArrayPrimType(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitArrayPrimType(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitArrayPrimType(this);
            else
                return visitor.visitChildren(this);
        }
    }
    @SuppressWarnings("CheckReturnValue")
    public static class ParenthesizedPrimTypeContext extends PrimaryTypeContext {
        public TerminalNode OpenParen() {
            return getToken(TypeScriptParser.OpenParen, 0);
        }

        public Type_Context type_() {
            return getRuleContext(Type_Context.class, 0);
        }

        public TerminalNode CloseParen() {
            return getToken(TypeScriptParser.CloseParen, 0);
        }

        public ParenthesizedPrimTypeContext(PrimaryTypeContext ctx) {
            copyFrom(ctx);
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterParenthesizedPrimType(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitParenthesizedPrimType(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitParenthesizedPrimType(this);
            else
                return visitor.visitChildren(this);
        }
    }
    @SuppressWarnings("CheckReturnValue")
    public static class ThisPrimTypeContext extends PrimaryTypeContext {
        public TerminalNode This() {
            return getToken(TypeScriptParser.This, 0);
        }

        public ThisPrimTypeContext(PrimaryTypeContext ctx) {
            copyFrom(ctx);
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterThisPrimType(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitThisPrimType(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitThisPrimType(this);
            else
                return visitor.visitChildren(this);
        }
    }
    @SuppressWarnings("CheckReturnValue")
    public static class TuplePrimTypeContext extends PrimaryTypeContext {
        public TerminalNode OpenBracket() {
            return getToken(TypeScriptParser.OpenBracket, 0);
        }

        public TupleElementTypesContext tupleElementTypes() {
            return getRuleContext(TupleElementTypesContext.class, 0);
        }

        public TerminalNode CloseBracket() {
            return getToken(TypeScriptParser.CloseBracket, 0);
        }

        public TuplePrimTypeContext(PrimaryTypeContext ctx) {
            copyFrom(ctx);
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterTuplePrimType(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitTuplePrimType(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitTuplePrimType(this);
            else
                return visitor.visitChildren(this);
        }
    }
    @SuppressWarnings("CheckReturnValue")
    public static class KeyOfTypeContext extends PrimaryTypeContext {
        public TerminalNode KeyOf() {
            return getToken(TypeScriptParser.KeyOf, 0);
        }

        public PrimaryTypeContext primaryType() {
            return getRuleContext(PrimaryTypeContext.class, 0);
        }

        public KeyOfTypeContext(PrimaryTypeContext ctx) {
            copyFrom(ctx);
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterKeyOfType(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitKeyOfType(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitKeyOfType(this);
            else
                return visitor.visitChildren(this);
        }
    }
    @SuppressWarnings("CheckReturnValue")
    public static class ObjectPrimTypeContext extends PrimaryTypeContext {
        public ObjectTypeContext objectType() {
            return getRuleContext(ObjectTypeContext.class, 0);
        }

        public ObjectPrimTypeContext(PrimaryTypeContext ctx) {
            copyFrom(ctx);
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterObjectPrimType(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitObjectPrimType(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitObjectPrimType(this);
            else
                return visitor.visitChildren(this);
        }
    }
    @SuppressWarnings("CheckReturnValue")
    public static class ReferencePrimTypeContext extends PrimaryTypeContext {
        public TypeReferenceContext typeReference() {
            return getRuleContext(TypeReferenceContext.class, 0);
        }

        public ReferencePrimTypeContext(PrimaryTypeContext ctx) {
            copyFrom(ctx);
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterReferencePrimType(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitReferencePrimType(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitReferencePrimType(this);
            else
                return visitor.visitChildren(this);
        }
    }
    @SuppressWarnings("CheckReturnValue")
    public static class QueryPrimTypeContext extends PrimaryTypeContext {
        public TypeQueryContext typeQuery() {
            return getRuleContext(TypeQueryContext.class, 0);
        }

        public QueryPrimTypeContext(PrimaryTypeContext ctx) {
            copyFrom(ctx);
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterQueryPrimType(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitQueryPrimType(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitQueryPrimType(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final PrimaryTypeContext primaryType() throws RecognitionException {
        return primaryType(0);
    }

    private PrimaryTypeContext primaryType(int _p) throws RecognitionException {
        ParserRuleContext _parentctx = _ctx;
        int _parentState = getState();
        PrimaryTypeContext _localctx = new PrimaryTypeContext(_ctx, _parentState);
        PrimaryTypeContext _prevctx = _localctx;
        int _startState = 22;
        enterRecursionRule(_localctx, 22, RULE_primaryType, _p);
        int _la;
        try {
            int _alt;
            enterOuterAlt(_localctx, 1);
            {
                setState(410);
                _errHandler.sync(this);
                switch (getInterpreter().adaptivePredict(_input, 11, _ctx)) {
                    case 1: {
                        _localctx = new ParenthesizedPrimTypeContext(_localctx);
                        _ctx = _localctx;
                        _prevctx = _localctx;

                        setState(391);
                        match(OpenParen);
                        setState(392);
                        type_();
                        setState(393);
                        match(CloseParen);
                    }
                        break;
                    case 2: {
                        _localctx = new PredefinedPrimTypeContext(_localctx);
                        _ctx = _localctx;
                        _prevctx = _localctx;
                        setState(395);
                        predefinedType();
                    }
                        break;
                    case 3: {
                        _localctx = new ReferencePrimTypeContext(_localctx);
                        _ctx = _localctx;
                        _prevctx = _localctx;
                        setState(396);
                        typeReference();
                    }
                        break;
                    case 4: {
                        _localctx = new ObjectPrimTypeContext(_localctx);
                        _ctx = _localctx;
                        _prevctx = _localctx;
                        setState(397);
                        objectType();
                    }
                        break;
                    case 5: {
                        _localctx = new TuplePrimTypeContext(_localctx);
                        _ctx = _localctx;
                        _prevctx = _localctx;
                        setState(398);
                        match(OpenBracket);
                        setState(399);
                        tupleElementTypes();
                        setState(400);
                        match(CloseBracket);
                    }
                        break;
                    case 6: {
                        _localctx = new QueryPrimTypeContext(_localctx);
                        _ctx = _localctx;
                        _prevctx = _localctx;
                        setState(402);
                        typeQuery();
                    }
                        break;
                    case 7: {
                        _localctx = new ThisPrimTypeContext(_localctx);
                        _ctx = _localctx;
                        _prevctx = _localctx;
                        setState(403);
                        match(This);
                    }
                        break;
                    case 8: {
                        _localctx = new RedefinitionOfTypeContext(_localctx);
                        _ctx = _localctx;
                        _prevctx = _localctx;
                        setState(404);
                        typeReference();
                        setState(405);
                        match(Is);
                        setState(406);
                        primaryType(2);
                    }
                        break;
                    case 9: {
                        _localctx = new KeyOfTypeContext(_localctx);
                        _ctx = _localctx;
                        _prevctx = _localctx;
                        setState(408);
                        match(KeyOf);
                        setState(409);
                        primaryType(1);
                    }
                        break;
                }
                _ctx.stop = _input.LT(-1);
                setState(421);
                _errHandler.sync(this);
                _alt = getInterpreter().adaptivePredict(_input, 13, _ctx);
                while (_alt != 2 && _alt != org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER) {
                    if (_alt == 1) {
                        if (_parseListeners != null)
                            triggerExitRuleEvent();
                        _prevctx = _localctx;
                        {
                            {
                                _localctx = new ArrayPrimTypeContext(new PrimaryTypeContext(_parentctx, _parentState));
                                pushNewRecursionContext(_localctx, _startState, RULE_primaryType);
                                setState(412);
                                if (!(precpred(_ctx, 6)))
                                    throw new FailedPredicateException(this, "precpred(_ctx, 6)");
                                setState(413);
                                if (!(this.notLineTerminator()))
                                    throw new FailedPredicateException(this, "this.notLineTerminator()");
                                setState(414);
                                match(OpenBracket);
                                setState(416);
                                _errHandler.sync(this);
                                _la = _input.LA(1);
                                if ((((_la) & ~0x3f) == 0 && ((1L << _la) & 4035225266123964752L) != 0)
                                        || ((((_la - 73)) & ~0x3f) == 0 && ((1L << (_la - 73)) & 5188111586719465729L) != 0)
                                        || _la == Identifier || _la == StringLiteral) {
                                    {
                                        setState(415);
                                        primaryType(0);
                                    }
                                }

                                setState(418);
                                match(CloseBracket);
                            }
                        }
                    }
                    setState(423);
                    _errHandler.sync(this);
                    _alt = getInterpreter().adaptivePredict(_input, 13, _ctx);
                }
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            unrollRecursionContexts(_parentctx);
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class PredefinedTypeContext extends ParserRuleContext {
        public TerminalNode Any() {
            return getToken(TypeScriptParser.Any, 0);
        }

        public TerminalNode NullLiteral() {
            return getToken(TypeScriptParser.NullLiteral, 0);
        }

        public TerminalNode Number() {
            return getToken(TypeScriptParser.Number, 0);
        }

        public TerminalNode DecimalLiteral() {
            return getToken(TypeScriptParser.DecimalLiteral, 0);
        }

        public TerminalNode Boolean() {
            return getToken(TypeScriptParser.Boolean, 0);
        }

        public TerminalNode BooleanLiteral() {
            return getToken(TypeScriptParser.BooleanLiteral, 0);
        }

        public TerminalNode String() {
            return getToken(TypeScriptParser.String, 0);
        }

        public TerminalNode StringLiteral() {
            return getToken(TypeScriptParser.StringLiteral, 0);
        }

        public TerminalNode Symbol() {
            return getToken(TypeScriptParser.Symbol, 0);
        }

        public TerminalNode Unique() {
            return getToken(TypeScriptParser.Unique, 0);
        }

        public TerminalNode Never() {
            return getToken(TypeScriptParser.Never, 0);
        }

        public TerminalNode Undefined() {
            return getToken(TypeScriptParser.Undefined, 0);
        }

        public TerminalNode Object() {
            return getToken(TypeScriptParser.Object, 0);
        }

        public TerminalNode Void() {
            return getToken(TypeScriptParser.Void, 0);
        }

        public PredefinedTypeContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_predefinedType;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterPredefinedType(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitPredefinedType(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitPredefinedType(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final PredefinedTypeContext predefinedType() throws RecognitionException {
        PredefinedTypeContext _localctx = new PredefinedTypeContext(_ctx, getState());
        enterRule(_localctx, 24, RULE_predefinedType);
        int _la;
        try {
            setState(440);
            _errHandler.sync(this);
            switch (_input.LA(1)) {
                case Any:
                    enterOuterAlt(_localctx, 1); {
                    setState(424);
                    match(Any);
                }
                    break;
                case NullLiteral:
                    enterOuterAlt(_localctx, 2); {
                    setState(425);
                    match(NullLiteral);
                }
                    break;
                case Number:
                    enterOuterAlt(_localctx, 3); {
                    setState(426);
                    match(Number);
                }
                    break;
                case DecimalLiteral:
                    enterOuterAlt(_localctx, 4); {
                    setState(427);
                    match(DecimalLiteral);
                }
                    break;
                case Boolean:
                    enterOuterAlt(_localctx, 5); {
                    setState(428);
                    match(Boolean);
                }
                    break;
                case BooleanLiteral:
                    enterOuterAlt(_localctx, 6); {
                    setState(429);
                    match(BooleanLiteral);
                }
                    break;
                case String:
                    enterOuterAlt(_localctx, 7); {
                    setState(430);
                    match(String);
                }
                    break;
                case StringLiteral:
                    enterOuterAlt(_localctx, 8); {
                    setState(431);
                    match(StringLiteral);
                }
                    break;
                case Unique:
                case Symbol:
                    enterOuterAlt(_localctx, 9); {
                    setState(433);
                    _errHandler.sync(this);
                    _la = _input.LA(1);
                    if (_la == Unique) {
                        {
                            setState(432);
                            match(Unique);
                        }
                    }

                    setState(435);
                    match(Symbol);
                }
                    break;
                case Never:
                    enterOuterAlt(_localctx, 10); {
                    setState(436);
                    match(Never);
                }
                    break;
                case Undefined:
                    enterOuterAlt(_localctx, 11); {
                    setState(437);
                    match(Undefined);
                }
                    break;
                case Object:
                    enterOuterAlt(_localctx, 12); {
                    setState(438);
                    match(Object);
                }
                    break;
                case Void:
                    enterOuterAlt(_localctx, 13); {
                    setState(439);
                    match(Void);
                }
                    break;
                default:
                    throw new NoViableAltException(this);
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class TypeReferenceContext extends ParserRuleContext {
        public TypeNameContext typeName() {
            return getRuleContext(TypeNameContext.class, 0);
        }

        public TypeGenericContext typeGeneric() {
            return getRuleContext(TypeGenericContext.class, 0);
        }

        public TypeReferenceContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_typeReference;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterTypeReference(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitTypeReference(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitTypeReference(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final TypeReferenceContext typeReference() throws RecognitionException {
        TypeReferenceContext _localctx = new TypeReferenceContext(_ctx, getState());
        enterRule(_localctx, 26, RULE_typeReference);
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(442);
                typeName();
                setState(444);
                _errHandler.sync(this);
                switch (getInterpreter().adaptivePredict(_input, 16, _ctx)) {
                    case 1: {
                        setState(443);
                        typeGeneric();
                    }
                        break;
                }
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class TypeGenericContext extends ParserRuleContext {
        public TerminalNode LessThan() {
            return getToken(TypeScriptParser.LessThan, 0);
        }

        public TypeArgumentListContext typeArgumentList() {
            return getRuleContext(TypeArgumentListContext.class, 0);
        }

        public TerminalNode MoreThan() {
            return getToken(TypeScriptParser.MoreThan, 0);
        }

        public TypeGenericContext typeGeneric() {
            return getRuleContext(TypeGenericContext.class, 0);
        }

        public TypeGenericContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_typeGeneric;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterTypeGeneric(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitTypeGeneric(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitTypeGeneric(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final TypeGenericContext typeGeneric() throws RecognitionException {
        TypeGenericContext _localctx = new TypeGenericContext(_ctx, getState());
        enterRule(_localctx, 28, RULE_typeGeneric);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(446);
                match(LessThan);
                setState(447);
                typeArgumentList();
                setState(449);
                _errHandler.sync(this);
                _la = _input.LA(1);
                if (_la == LessThan) {
                    {
                        setState(448);
                        typeGeneric();
                    }
                }

                setState(451);
                match(MoreThan);
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class TypeNameContext extends ParserRuleContext {
        public IdentifierContext identifier() {
            return getRuleContext(IdentifierContext.class, 0);
        }

        public NamespaceNameContext namespaceName() {
            return getRuleContext(NamespaceNameContext.class, 0);
        }

        public TypeNameContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_typeName;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterTypeName(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitTypeName(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitTypeName(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final TypeNameContext typeName() throws RecognitionException {
        TypeNameContext _localctx = new TypeNameContext(_ctx, getState());
        enterRule(_localctx, 30, RULE_typeName);
        try {
            setState(455);
            _errHandler.sync(this);
            switch (getInterpreter().adaptivePredict(_input, 18, _ctx)) {
                case 1:
                    enterOuterAlt(_localctx, 1); {
                    setState(453);
                    identifier();
                }
                    break;
                case 2:
                    enterOuterAlt(_localctx, 2); {
                    setState(454);
                    namespaceName();
                }
                    break;
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class ObjectTypeContext extends ParserRuleContext {
        public TerminalNode OpenBrace() {
            return getToken(TypeScriptParser.OpenBrace, 0);
        }

        public TerminalNode CloseBrace() {
            return getToken(TypeScriptParser.CloseBrace, 0);
        }

        public TypeBodyContext typeBody() {
            return getRuleContext(TypeBodyContext.class, 0);
        }

        public ObjectTypeContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_objectType;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterObjectType(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitObjectType(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitObjectType(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final ObjectTypeContext objectType() throws RecognitionException {
        ObjectTypeContext _localctx = new ObjectTypeContext(_ctx, getState());
        enterRule(_localctx, 32, RULE_objectType);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(457);
                match(OpenBrace);
                setState(459);
                _errHandler.sync(this);
                _la = _input.LA(1);
                if ((((_la) & ~0x3f) == 0 && ((1L << _la) & -576460748008456112L) != 0)
                        || ((((_la - 64)) & ~0x3f) == 0 && ((1L << (_la - 64)) & -274877907005L) != 0)
                        || ((((_la - 128)) & ~0x3f) == 0 && ((1L << (_la - 128)) & 3263L) != 0)) {
                    {
                        setState(458);
                        typeBody();
                    }
                }

                setState(461);
                match(CloseBrace);
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class TypeBodyContext extends ParserRuleContext {
        public TypeMemberListContext typeMemberList() {
            return getRuleContext(TypeMemberListContext.class, 0);
        }

        public TerminalNode SemiColon() {
            return getToken(TypeScriptParser.SemiColon, 0);
        }

        public TerminalNode Comma() {
            return getToken(TypeScriptParser.Comma, 0);
        }

        public TypeBodyContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_typeBody;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterTypeBody(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitTypeBody(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitTypeBody(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final TypeBodyContext typeBody() throws RecognitionException {
        TypeBodyContext _localctx = new TypeBodyContext(_ctx, getState());
        enterRule(_localctx, 34, RULE_typeBody);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(463);
                typeMemberList();
                setState(465);
                _errHandler.sync(this);
                _la = _input.LA(1);
                if (_la == SemiColon || _la == Comma) {
                    {
                        setState(464);
                        _la = _input.LA(1);
                        if (!(_la == SemiColon || _la == Comma)) {
                            _errHandler.recoverInline(this);
                        } else {
                            if (_input.LA(1) == Token.EOF)
                                matchedEOF = true;
                            _errHandler.reportMatch(this);
                            consume();
                        }
                    }
                }

            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class TypeMemberListContext extends ParserRuleContext {
        public List<TypeMemberContext> typeMember() {
            return getRuleContexts(TypeMemberContext.class);
        }

        public TypeMemberContext typeMember(int i) {
            return getRuleContext(TypeMemberContext.class, i);
        }

        public List<TerminalNode> SemiColon() {
            return getTokens(TypeScriptParser.SemiColon);
        }

        public TerminalNode SemiColon(int i) {
            return getToken(TypeScriptParser.SemiColon, i);
        }

        public List<TerminalNode> Comma() {
            return getTokens(TypeScriptParser.Comma);
        }

        public TerminalNode Comma(int i) {
            return getToken(TypeScriptParser.Comma, i);
        }

        public TypeMemberListContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_typeMemberList;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterTypeMemberList(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitTypeMemberList(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitTypeMemberList(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final TypeMemberListContext typeMemberList() throws RecognitionException {
        TypeMemberListContext _localctx = new TypeMemberListContext(_ctx, getState());
        enterRule(_localctx, 36, RULE_typeMemberList);
        int _la;
        try {
            int _alt;
            enterOuterAlt(_localctx, 1);
            {
                setState(467);
                typeMember();
                setState(472);
                _errHandler.sync(this);
                _alt = getInterpreter().adaptivePredict(_input, 21, _ctx);
                while (_alt != 2 && _alt != org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER) {
                    if (_alt == 1) {
                        {
                            {
                                setState(468);
                                _la = _input.LA(1);
                                if (!(_la == SemiColon || _la == Comma)) {
                                    _errHandler.recoverInline(this);
                                } else {
                                    if (_input.LA(1) == Token.EOF)
                                        matchedEOF = true;
                                    _errHandler.reportMatch(this);
                                    consume();
                                }
                                setState(469);
                                typeMember();
                            }
                        }
                    }
                    setState(474);
                    _errHandler.sync(this);
                    _alt = getInterpreter().adaptivePredict(_input, 21, _ctx);
                }
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class TypeMemberContext extends ParserRuleContext {
        public PropertySignaturContext propertySignatur() {
            return getRuleContext(PropertySignaturContext.class, 0);
        }

        public CallSignatureContext callSignature() {
            return getRuleContext(CallSignatureContext.class, 0);
        }

        public ConstructSignatureContext constructSignature() {
            return getRuleContext(ConstructSignatureContext.class, 0);
        }

        public IndexSignatureContext indexSignature() {
            return getRuleContext(IndexSignatureContext.class, 0);
        }

        public MethodSignatureContext methodSignature() {
            return getRuleContext(MethodSignatureContext.class, 0);
        }

        public TerminalNode ARROW() {
            return getToken(TypeScriptParser.ARROW, 0);
        }

        public Type_Context type_() {
            return getRuleContext(Type_Context.class, 0);
        }

        public TypeMemberContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_typeMember;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterTypeMember(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitTypeMember(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitTypeMember(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final TypeMemberContext typeMember() throws RecognitionException {
        TypeMemberContext _localctx = new TypeMemberContext(_ctx, getState());
        enterRule(_localctx, 38, RULE_typeMember);
        int _la;
        try {
            setState(484);
            _errHandler.sync(this);
            switch (getInterpreter().adaptivePredict(_input, 23, _ctx)) {
                case 1:
                    enterOuterAlt(_localctx, 1); {
                    setState(475);
                    propertySignatur();
                }
                    break;
                case 2:
                    enterOuterAlt(_localctx, 2); {
                    setState(476);
                    callSignature();
                }
                    break;
                case 3:
                    enterOuterAlt(_localctx, 3); {
                    setState(477);
                    constructSignature();
                }
                    break;
                case 4:
                    enterOuterAlt(_localctx, 4); {
                    setState(478);
                    indexSignature();
                }
                    break;
                case 5:
                    enterOuterAlt(_localctx, 5); {
                    setState(479);
                    methodSignature();
                    setState(482);
                    _errHandler.sync(this);
                    _la = _input.LA(1);
                    if (_la == ARROW) {
                        {
                            setState(480);
                            match(ARROW);
                            setState(481);
                            type_();
                        }
                    }

                }
                    break;
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class ArrayTypeContext extends ParserRuleContext {
        public PrimaryTypeContext primaryType() {
            return getRuleContext(PrimaryTypeContext.class, 0);
        }

        public TerminalNode OpenBracket() {
            return getToken(TypeScriptParser.OpenBracket, 0);
        }

        public TerminalNode CloseBracket() {
            return getToken(TypeScriptParser.CloseBracket, 0);
        }

        public ArrayTypeContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_arrayType;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterArrayType(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitArrayType(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitArrayType(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final ArrayTypeContext arrayType() throws RecognitionException {
        ArrayTypeContext _localctx = new ArrayTypeContext(_ctx, getState());
        enterRule(_localctx, 40, RULE_arrayType);
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(486);
                primaryType(0);
                setState(487);
                if (!(this.notLineTerminator()))
                    throw new FailedPredicateException(this, "this.notLineTerminator()");
                setState(488);
                match(OpenBracket);
                setState(489);
                match(CloseBracket);
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class TupleTypeContext extends ParserRuleContext {
        public TerminalNode OpenBracket() {
            return getToken(TypeScriptParser.OpenBracket, 0);
        }

        public TupleElementTypesContext tupleElementTypes() {
            return getRuleContext(TupleElementTypesContext.class, 0);
        }

        public TerminalNode CloseBracket() {
            return getToken(TypeScriptParser.CloseBracket, 0);
        }

        public TupleTypeContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_tupleType;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterTupleType(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitTupleType(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitTupleType(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final TupleTypeContext tupleType() throws RecognitionException {
        TupleTypeContext _localctx = new TupleTypeContext(_ctx, getState());
        enterRule(_localctx, 42, RULE_tupleType);
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(491);
                match(OpenBracket);
                setState(492);
                tupleElementTypes();
                setState(493);
                match(CloseBracket);
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class TupleElementTypesContext extends ParserRuleContext {
        public List<Type_Context> type_() {
            return getRuleContexts(Type_Context.class);
        }

        public Type_Context type_(int i) {
            return getRuleContext(Type_Context.class, i);
        }

        public List<TerminalNode> Comma() {
            return getTokens(TypeScriptParser.Comma);
        }

        public TerminalNode Comma(int i) {
            return getToken(TypeScriptParser.Comma, i);
        }

        public TupleElementTypesContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_tupleElementTypes;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterTupleElementTypes(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitTupleElementTypes(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitTupleElementTypes(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final TupleElementTypesContext tupleElementTypes() throws RecognitionException {
        TupleElementTypesContext _localctx = new TupleElementTypesContext(_ctx, getState());
        enterRule(_localctx, 44, RULE_tupleElementTypes);
        int _la;
        try {
            int _alt;
            enterOuterAlt(_localctx, 1);
            {
                setState(495);
                type_();
                setState(500);
                _errHandler.sync(this);
                _alt = getInterpreter().adaptivePredict(_input, 24, _ctx);
                while (_alt != 2 && _alt != org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER) {
                    if (_alt == 1) {
                        {
                            {
                                setState(496);
                                match(Comma);
                                setState(497);
                                type_();
                            }
                        }
                    }
                    setState(502);
                    _errHandler.sync(this);
                    _alt = getInterpreter().adaptivePredict(_input, 24, _ctx);
                }
                setState(504);
                _errHandler.sync(this);
                _la = _input.LA(1);
                if (_la == Comma) {
                    {
                        setState(503);
                        match(Comma);
                    }
                }

            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class FunctionTypeContext extends ParserRuleContext {
        public TerminalNode OpenParen() {
            return getToken(TypeScriptParser.OpenParen, 0);
        }

        public TerminalNode CloseParen() {
            return getToken(TypeScriptParser.CloseParen, 0);
        }

        public TerminalNode ARROW() {
            return getToken(TypeScriptParser.ARROW, 0);
        }

        public Type_Context type_() {
            return getRuleContext(Type_Context.class, 0);
        }

        public TypeParametersContext typeParameters() {
            return getRuleContext(TypeParametersContext.class, 0);
        }

        public ParameterListContext parameterList() {
            return getRuleContext(ParameterListContext.class, 0);
        }

        public FunctionTypeContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_functionType;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterFunctionType(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitFunctionType(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitFunctionType(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final FunctionTypeContext functionType() throws RecognitionException {
        FunctionTypeContext _localctx = new FunctionTypeContext(_ctx, getState());
        enterRule(_localctx, 46, RULE_functionType);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(507);
                _errHandler.sync(this);
                _la = _input.LA(1);
                if (_la == LessThan) {
                    {
                        setState(506);
                        typeParameters();
                    }
                }

                setState(509);
                match(OpenParen);
                setState(511);
                _errHandler.sync(this);
                _la = _input.LA(1);
                if ((((_la) & ~0x3f) == 0 && ((1L << _la) & 1729382256910401808L) != 0)
                        || ((((_la - 70)) & ~0x3f) == 0 && ((1L << (_la - 70)) & -4294967297L) != 0)
                        || ((((_la - 135)) & ~0x3f) == 0 && ((1L << (_la - 135)) & 13L) != 0)) {
                    {
                        setState(510);
                        parameterList();
                    }
                }

                setState(513);
                match(CloseParen);
                setState(514);
                match(ARROW);
                setState(515);
                type_();
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class ConstructorTypeContext extends ParserRuleContext {
        public TerminalNode New() {
            return getToken(TypeScriptParser.New, 0);
        }

        public TerminalNode OpenParen() {
            return getToken(TypeScriptParser.OpenParen, 0);
        }

        public TerminalNode CloseParen() {
            return getToken(TypeScriptParser.CloseParen, 0);
        }

        public TerminalNode ARROW() {
            return getToken(TypeScriptParser.ARROW, 0);
        }

        public Type_Context type_() {
            return getRuleContext(Type_Context.class, 0);
        }

        public TypeParametersContext typeParameters() {
            return getRuleContext(TypeParametersContext.class, 0);
        }

        public ParameterListContext parameterList() {
            return getRuleContext(ParameterListContext.class, 0);
        }

        public ConstructorTypeContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_constructorType;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterConstructorType(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitConstructorType(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitConstructorType(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final ConstructorTypeContext constructorType() throws RecognitionException {
        ConstructorTypeContext _localctx = new ConstructorTypeContext(_ctx, getState());
        enterRule(_localctx, 48, RULE_constructorType);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(517);
                match(New);
                setState(519);
                _errHandler.sync(this);
                _la = _input.LA(1);
                if (_la == LessThan) {
                    {
                        setState(518);
                        typeParameters();
                    }
                }

                setState(521);
                match(OpenParen);
                setState(523);
                _errHandler.sync(this);
                _la = _input.LA(1);
                if ((((_la) & ~0x3f) == 0 && ((1L << _la) & 1729382256910401808L) != 0)
                        || ((((_la - 70)) & ~0x3f) == 0 && ((1L << (_la - 70)) & -4294967297L) != 0)
                        || ((((_la - 135)) & ~0x3f) == 0 && ((1L << (_la - 135)) & 13L) != 0)) {
                    {
                        setState(522);
                        parameterList();
                    }
                }

                setState(525);
                match(CloseParen);
                setState(526);
                match(ARROW);
                setState(527);
                type_();
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class TypeQueryContext extends ParserRuleContext {
        public TerminalNode Typeof() {
            return getToken(TypeScriptParser.Typeof, 0);
        }

        public TypeQueryExpressionContext typeQueryExpression() {
            return getRuleContext(TypeQueryExpressionContext.class, 0);
        }

        public TypeQueryContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_typeQuery;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterTypeQuery(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitTypeQuery(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitTypeQuery(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final TypeQueryContext typeQuery() throws RecognitionException {
        TypeQueryContext _localctx = new TypeQueryContext(_ctx, getState());
        enterRule(_localctx, 50, RULE_typeQuery);
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(529);
                match(Typeof);
                setState(530);
                typeQueryExpression();
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class TypeQueryExpressionContext extends ParserRuleContext {
        public IdentifierContext identifier() {
            return getRuleContext(IdentifierContext.class, 0);
        }

        public List<IdentifierNameContext> identifierName() {
            return getRuleContexts(IdentifierNameContext.class);
        }

        public IdentifierNameContext identifierName(int i) {
            return getRuleContext(IdentifierNameContext.class, i);
        }

        public List<TerminalNode> Dot() {
            return getTokens(TypeScriptParser.Dot);
        }

        public TerminalNode Dot(int i) {
            return getToken(TypeScriptParser.Dot, i);
        }

        public TypeQueryExpressionContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_typeQueryExpression;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterTypeQueryExpression(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitTypeQueryExpression(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitTypeQueryExpression(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final TypeQueryExpressionContext typeQueryExpression() throws RecognitionException {
        TypeQueryExpressionContext _localctx = new TypeQueryExpressionContext(_ctx, getState());
        enterRule(_localctx, 52, RULE_typeQueryExpression);
        try {
            int _alt;
            setState(542);
            _errHandler.sync(this);
            switch (getInterpreter().adaptivePredict(_input, 31, _ctx)) {
                case 1:
                    enterOuterAlt(_localctx, 1); {
                    setState(532);
                    identifier();
                }
                    break;
                case 2:
                    enterOuterAlt(_localctx, 2); {
                    setState(536);
                    _errHandler.sync(this);
                    _alt = 1;
                    do {
                        switch (_alt) {
                            case 1: {
                                {
                                    setState(533);
                                    identifierName();
                                    setState(534);
                                    match(Dot);
                                }
                            }
                                break;
                            default:
                                throw new NoViableAltException(this);
                        }
                        setState(538);
                        _errHandler.sync(this);
                        _alt = getInterpreter().adaptivePredict(_input, 30, _ctx);
                    } while (_alt != 2 && _alt != org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER);
                    setState(540);
                    identifierName();
                }
                    break;
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class PropertySignaturContext extends ParserRuleContext {
        public PropertyNameContext propertyName() {
            return getRuleContext(PropertyNameContext.class, 0);
        }

        public TerminalNode ReadOnly() {
            return getToken(TypeScriptParser.ReadOnly, 0);
        }

        public TerminalNode QuestionMark() {
            return getToken(TypeScriptParser.QuestionMark, 0);
        }

        public TypeAnnotationContext typeAnnotation() {
            return getRuleContext(TypeAnnotationContext.class, 0);
        }

        public TerminalNode ARROW() {
            return getToken(TypeScriptParser.ARROW, 0);
        }

        public Type_Context type_() {
            return getRuleContext(Type_Context.class, 0);
        }

        public PropertySignaturContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_propertySignatur;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterPropertySignatur(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitPropertySignatur(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitPropertySignatur(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final PropertySignaturContext propertySignatur() throws RecognitionException {
        PropertySignaturContext _localctx = new PropertySignaturContext(_ctx, getState());
        enterRule(_localctx, 54, RULE_propertySignatur);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(545);
                _errHandler.sync(this);
                switch (getInterpreter().adaptivePredict(_input, 32, _ctx)) {
                    case 1: {
                        setState(544);
                        match(ReadOnly);
                    }
                        break;
                }
                setState(547);
                propertyName();
                setState(549);
                _errHandler.sync(this);
                _la = _input.LA(1);
                if (_la == QuestionMark) {
                    {
                        setState(548);
                        match(QuestionMark);
                    }
                }

                setState(552);
                _errHandler.sync(this);
                _la = _input.LA(1);
                if (_la == Colon) {
                    {
                        setState(551);
                        typeAnnotation();
                    }
                }

                setState(556);
                _errHandler.sync(this);
                _la = _input.LA(1);
                if (_la == ARROW) {
                    {
                        setState(554);
                        match(ARROW);
                        setState(555);
                        type_();
                    }
                }

            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class TypeAnnotationContext extends ParserRuleContext {
        public TerminalNode Colon() {
            return getToken(TypeScriptParser.Colon, 0);
        }

        public Type_Context type_() {
            return getRuleContext(Type_Context.class, 0);
        }

        public TypeAnnotationContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_typeAnnotation;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterTypeAnnotation(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitTypeAnnotation(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitTypeAnnotation(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final TypeAnnotationContext typeAnnotation() throws RecognitionException {
        TypeAnnotationContext _localctx = new TypeAnnotationContext(_ctx, getState());
        enterRule(_localctx, 56, RULE_typeAnnotation);
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(558);
                match(Colon);
                setState(559);
                type_();
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class CallSignatureContext extends ParserRuleContext {
        public TerminalNode OpenParen() {
            return getToken(TypeScriptParser.OpenParen, 0);
        }

        public TerminalNode CloseParen() {
            return getToken(TypeScriptParser.CloseParen, 0);
        }

        public TypeParametersContext typeParameters() {
            return getRuleContext(TypeParametersContext.class, 0);
        }

        public ParameterListContext parameterList() {
            return getRuleContext(ParameterListContext.class, 0);
        }

        public TypeAnnotationContext typeAnnotation() {
            return getRuleContext(TypeAnnotationContext.class, 0);
        }

        public CallSignatureContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_callSignature;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterCallSignature(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitCallSignature(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitCallSignature(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final CallSignatureContext callSignature() throws RecognitionException {
        CallSignatureContext _localctx = new CallSignatureContext(_ctx, getState());
        enterRule(_localctx, 58, RULE_callSignature);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(562);
                _errHandler.sync(this);
                _la = _input.LA(1);
                if (_la == LessThan) {
                    {
                        setState(561);
                        typeParameters();
                    }
                }

                setState(564);
                match(OpenParen);
                setState(566);
                _errHandler.sync(this);
                _la = _input.LA(1);
                if ((((_la) & ~0x3f) == 0 && ((1L << _la) & 1729382256910401808L) != 0)
                        || ((((_la - 70)) & ~0x3f) == 0 && ((1L << (_la - 70)) & -4294967297L) != 0)
                        || ((((_la - 135)) & ~0x3f) == 0 && ((1L << (_la - 135)) & 13L) != 0)) {
                    {
                        setState(565);
                        parameterList();
                    }
                }

                setState(568);
                match(CloseParen);
                setState(570);
                _errHandler.sync(this);
                switch (getInterpreter().adaptivePredict(_input, 38, _ctx)) {
                    case 1: {
                        setState(569);
                        typeAnnotation();
                    }
                        break;
                }
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class ParameterListContext extends ParserRuleContext {
        public RestParameterContext restParameter() {
            return getRuleContext(RestParameterContext.class, 0);
        }

        public List<ParameterContext> parameter() {
            return getRuleContexts(ParameterContext.class);
        }

        public ParameterContext parameter(int i) {
            return getRuleContext(ParameterContext.class, i);
        }

        public List<TerminalNode> Comma() {
            return getTokens(TypeScriptParser.Comma);
        }

        public TerminalNode Comma(int i) {
            return getToken(TypeScriptParser.Comma, i);
        }

        public ParameterListContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_parameterList;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterParameterList(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitParameterList(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitParameterList(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final ParameterListContext parameterList() throws RecognitionException {
        ParameterListContext _localctx = new ParameterListContext(_ctx, getState());
        enterRule(_localctx, 60, RULE_parameterList);
        int _la;
        try {
            int _alt;
            setState(588);
            _errHandler.sync(this);
            switch (_input.LA(1)) {
                case Ellipsis:
                    enterOuterAlt(_localctx, 1); {
                    setState(572);
                    restParameter();
                }
                    break;
                case OpenBracket:
                case OpenBrace:
                case NullLiteral:
                case BooleanLiteral:
                case Break:
                case Do:
                case Instanceof:
                case Typeof:
                case Case:
                case Else:
                case New:
                case Var:
                case Catch:
                case Finally:
                case Return:
                case Void:
                case Continue:
                case For:
                case Switch:
                case While:
                case Debugger:
                case Function_:
                case This:
                case With:
                case Default:
                case If:
                case Throw:
                case Delete:
                case In:
                case Try:
                case As:
                case From:
                case ReadOnly:
                case Async:
                case Await:
                case Yield:
                case Class:
                case Enum:
                case Extends:
                case Super:
                case Const:
                case Export:
                case Import:
                case Implements:
                case Let:
                case Private:
                case Public:
                case Interface:
                case Package:
                case Protected:
                case Static:
                case Any:
                case Number:
                case Never:
                case Boolean:
                case String:
                case Unique:
                case Symbol:
                case Undefined:
                case Object:
                case Of:
                case KeyOf:
                case TypeAlias:
                case Constructor:
                case Namespace:
                case Require:
                case Module:
                case Abstract:
                case At:
                case Identifier:
                    enterOuterAlt(_localctx, 2); {
                    setState(573);
                    parameter();
                    setState(578);
                    _errHandler.sync(this);
                    _alt = getInterpreter().adaptivePredict(_input, 39, _ctx);
                    while (_alt != 2 && _alt != org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER) {
                        if (_alt == 1) {
                            {
                                {
                                    setState(574);
                                    match(Comma);
                                    setState(575);
                                    parameter();
                                }
                            }
                        }
                        setState(580);
                        _errHandler.sync(this);
                        _alt = getInterpreter().adaptivePredict(_input, 39, _ctx);
                    }
                    setState(583);
                    _errHandler.sync(this);
                    switch (getInterpreter().adaptivePredict(_input, 40, _ctx)) {
                        case 1: {
                            setState(581);
                            match(Comma);
                            setState(582);
                            restParameter();
                        }
                            break;
                    }
                    setState(586);
                    _errHandler.sync(this);
                    _la = _input.LA(1);
                    if (_la == Comma) {
                        {
                            setState(585);
                            match(Comma);
                        }
                    }

                }
                    break;
                default:
                    throw new NoViableAltException(this);
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class RequiredParameterListContext extends ParserRuleContext {
        public List<RequiredParameterContext> requiredParameter() {
            return getRuleContexts(RequiredParameterContext.class);
        }

        public RequiredParameterContext requiredParameter(int i) {
            return getRuleContext(RequiredParameterContext.class, i);
        }

        public List<TerminalNode> Comma() {
            return getTokens(TypeScriptParser.Comma);
        }

        public TerminalNode Comma(int i) {
            return getToken(TypeScriptParser.Comma, i);
        }

        public RequiredParameterListContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_requiredParameterList;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterRequiredParameterList(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitRequiredParameterList(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitRequiredParameterList(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final RequiredParameterListContext requiredParameterList() throws RecognitionException {
        RequiredParameterListContext _localctx = new RequiredParameterListContext(_ctx, getState());
        enterRule(_localctx, 62, RULE_requiredParameterList);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(590);
                requiredParameter();
                setState(595);
                _errHandler.sync(this);
                _la = _input.LA(1);
                while (_la == Comma) {
                    {
                        {
                            setState(591);
                            match(Comma);
                            setState(592);
                            requiredParameter();
                        }
                    }
                    setState(597);
                    _errHandler.sync(this);
                    _la = _input.LA(1);
                }
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class ParameterContext extends ParserRuleContext {
        public RequiredParameterContext requiredParameter() {
            return getRuleContext(RequiredParameterContext.class, 0);
        }

        public OptionalParameterContext optionalParameter() {
            return getRuleContext(OptionalParameterContext.class, 0);
        }

        public ParameterContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_parameter;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterParameter(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitParameter(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitParameter(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final ParameterContext parameter() throws RecognitionException {
        ParameterContext _localctx = new ParameterContext(_ctx, getState());
        enterRule(_localctx, 64, RULE_parameter);
        try {
            setState(600);
            _errHandler.sync(this);
            switch (getInterpreter().adaptivePredict(_input, 44, _ctx)) {
                case 1:
                    enterOuterAlt(_localctx, 1); {
                    setState(598);
                    requiredParameter();
                }
                    break;
                case 2:
                    enterOuterAlt(_localctx, 2); {
                    setState(599);
                    optionalParameter();
                }
                    break;
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class OptionalParameterContext extends ParserRuleContext {
        public IdentifierOrPatternContext identifierOrPattern() {
            return getRuleContext(IdentifierOrPatternContext.class, 0);
        }

        public DecoratorListContext decoratorList() {
            return getRuleContext(DecoratorListContext.class, 0);
        }

        public TerminalNode QuestionMark() {
            return getToken(TypeScriptParser.QuestionMark, 0);
        }

        public InitializerContext initializer() {
            return getRuleContext(InitializerContext.class, 0);
        }

        public AccessibilityModifierContext accessibilityModifier() {
            return getRuleContext(AccessibilityModifierContext.class, 0);
        }

        public TypeAnnotationContext typeAnnotation() {
            return getRuleContext(TypeAnnotationContext.class, 0);
        }

        public OptionalParameterContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_optionalParameter;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterOptionalParameter(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitOptionalParameter(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitOptionalParameter(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final OptionalParameterContext optionalParameter() throws RecognitionException {
        OptionalParameterContext _localctx = new OptionalParameterContext(_ctx, getState());
        enterRule(_localctx, 66, RULE_optionalParameter);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(603);
                _errHandler.sync(this);
                _la = _input.LA(1);
                if (_la == At) {
                    {
                        setState(602);
                        decoratorList();
                    }
                }

                {
                    setState(606);
                    _errHandler.sync(this);
                    switch (getInterpreter().adaptivePredict(_input, 46, _ctx)) {
                        case 1: {
                            setState(605);
                            accessibilityModifier();
                        }
                            break;
                    }
                    setState(608);
                    identifierOrPattern();
                    setState(617);
                    _errHandler.sync(this);
                    switch (_input.LA(1)) {
                        case QuestionMark: {
                            setState(609);
                            match(QuestionMark);
                            setState(611);
                            _errHandler.sync(this);
                            _la = _input.LA(1);
                            if (_la == Colon) {
                                {
                                    setState(610);
                                    typeAnnotation();
                                }
                            }

                        }
                            break;
                        case Assign:
                        case Colon: {
                            setState(614);
                            _errHandler.sync(this);
                            _la = _input.LA(1);
                            if (_la == Colon) {
                                {
                                    setState(613);
                                    typeAnnotation();
                                }
                            }

                            setState(616);
                            initializer();
                        }
                            break;
                        default:
                            throw new NoViableAltException(this);
                    }
                }
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class RestParameterContext extends ParserRuleContext {
        public TerminalNode Ellipsis() {
            return getToken(TypeScriptParser.Ellipsis, 0);
        }

        public SingleExpressionContext singleExpression() {
            return getRuleContext(SingleExpressionContext.class, 0);
        }

        public TypeAnnotationContext typeAnnotation() {
            return getRuleContext(TypeAnnotationContext.class, 0);
        }

        public RestParameterContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_restParameter;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterRestParameter(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitRestParameter(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitRestParameter(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final RestParameterContext restParameter() throws RecognitionException {
        RestParameterContext _localctx = new RestParameterContext(_ctx, getState());
        enterRule(_localctx, 68, RULE_restParameter);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(619);
                match(Ellipsis);
                setState(620);
                singleExpression(0);
                setState(622);
                _errHandler.sync(this);
                _la = _input.LA(1);
                if (_la == Colon) {
                    {
                        setState(621);
                        typeAnnotation();
                    }
                }

            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class RequiredParameterContext extends ParserRuleContext {
        public IdentifierOrPatternContext identifierOrPattern() {
            return getRuleContext(IdentifierOrPatternContext.class, 0);
        }

        public DecoratorListContext decoratorList() {
            return getRuleContext(DecoratorListContext.class, 0);
        }

        public AccessibilityModifierContext accessibilityModifier() {
            return getRuleContext(AccessibilityModifierContext.class, 0);
        }

        public TypeAnnotationContext typeAnnotation() {
            return getRuleContext(TypeAnnotationContext.class, 0);
        }

        public RequiredParameterContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_requiredParameter;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterRequiredParameter(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitRequiredParameter(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitRequiredParameter(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final RequiredParameterContext requiredParameter() throws RecognitionException {
        RequiredParameterContext _localctx = new RequiredParameterContext(_ctx, getState());
        enterRule(_localctx, 70, RULE_requiredParameter);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(625);
                _errHandler.sync(this);
                _la = _input.LA(1);
                if (_la == At) {
                    {
                        setState(624);
                        decoratorList();
                    }
                }

                setState(628);
                _errHandler.sync(this);
                switch (getInterpreter().adaptivePredict(_input, 52, _ctx)) {
                    case 1: {
                        setState(627);
                        accessibilityModifier();
                    }
                        break;
                }
                setState(630);
                identifierOrPattern();
                setState(632);
                _errHandler.sync(this);
                _la = _input.LA(1);
                if (_la == Colon) {
                    {
                        setState(631);
                        typeAnnotation();
                    }
                }

            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class AccessibilityModifierContext extends ParserRuleContext {
        public TerminalNode Public() {
            return getToken(TypeScriptParser.Public, 0);
        }

        public TerminalNode Private() {
            return getToken(TypeScriptParser.Private, 0);
        }

        public TerminalNode Protected() {
            return getToken(TypeScriptParser.Protected, 0);
        }

        public AccessibilityModifierContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_accessibilityModifier;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterAccessibilityModifier(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitAccessibilityModifier(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitAccessibilityModifier(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final AccessibilityModifierContext accessibilityModifier() throws RecognitionException {
        AccessibilityModifierContext _localctx = new AccessibilityModifierContext(_ctx, getState());
        enterRule(_localctx, 72, RULE_accessibilityModifier);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(634);
                _la = _input.LA(1);
                if (!(((((_la - 112)) & ~0x3f) == 0 && ((1L << (_la - 112)) & 19L) != 0))) {
                    _errHandler.recoverInline(this);
                } else {
                    if (_input.LA(1) == Token.EOF)
                        matchedEOF = true;
                    _errHandler.reportMatch(this);
                    consume();
                }
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class IdentifierOrPatternContext extends ParserRuleContext {
        public IdentifierNameContext identifierName() {
            return getRuleContext(IdentifierNameContext.class, 0);
        }

        public BindingPatternContext bindingPattern() {
            return getRuleContext(BindingPatternContext.class, 0);
        }

        public IdentifierOrPatternContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_identifierOrPattern;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterIdentifierOrPattern(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitIdentifierOrPattern(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitIdentifierOrPattern(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final IdentifierOrPatternContext identifierOrPattern() throws RecognitionException {
        IdentifierOrPatternContext _localctx = new IdentifierOrPatternContext(_ctx, getState());
        enterRule(_localctx, 74, RULE_identifierOrPattern);
        try {
            setState(638);
            _errHandler.sync(this);
            switch (_input.LA(1)) {
                case NullLiteral:
                case BooleanLiteral:
                case Break:
                case Do:
                case Instanceof:
                case Typeof:
                case Case:
                case Else:
                case New:
                case Var:
                case Catch:
                case Finally:
                case Return:
                case Void:
                case Continue:
                case For:
                case Switch:
                case While:
                case Debugger:
                case Function_:
                case This:
                case With:
                case Default:
                case If:
                case Throw:
                case Delete:
                case In:
                case Try:
                case As:
                case From:
                case ReadOnly:
                case Async:
                case Await:
                case Yield:
                case Class:
                case Enum:
                case Extends:
                case Super:
                case Const:
                case Export:
                case Import:
                case Implements:
                case Let:
                case Private:
                case Public:
                case Interface:
                case Package:
                case Protected:
                case Static:
                case Any:
                case Number:
                case Never:
                case Boolean:
                case String:
                case Unique:
                case Symbol:
                case Undefined:
                case Object:
                case Of:
                case KeyOf:
                case TypeAlias:
                case Constructor:
                case Namespace:
                case Require:
                case Module:
                case Abstract:
                case Identifier:
                    enterOuterAlt(_localctx, 1); {
                    setState(636);
                    identifierName();
                }
                    break;
                case OpenBracket:
                case OpenBrace:
                    enterOuterAlt(_localctx, 2); {
                    setState(637);
                    bindingPattern();
                }
                    break;
                default:
                    throw new NoViableAltException(this);
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class ConstructSignatureContext extends ParserRuleContext {
        public TerminalNode New() {
            return getToken(TypeScriptParser.New, 0);
        }

        public TerminalNode OpenParen() {
            return getToken(TypeScriptParser.OpenParen, 0);
        }

        public TerminalNode CloseParen() {
            return getToken(TypeScriptParser.CloseParen, 0);
        }

        public TypeParametersContext typeParameters() {
            return getRuleContext(TypeParametersContext.class, 0);
        }

        public ParameterListContext parameterList() {
            return getRuleContext(ParameterListContext.class, 0);
        }

        public TypeAnnotationContext typeAnnotation() {
            return getRuleContext(TypeAnnotationContext.class, 0);
        }

        public ConstructSignatureContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_constructSignature;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterConstructSignature(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitConstructSignature(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitConstructSignature(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final ConstructSignatureContext constructSignature() throws RecognitionException {
        ConstructSignatureContext _localctx = new ConstructSignatureContext(_ctx, getState());
        enterRule(_localctx, 76, RULE_constructSignature);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(640);
                match(New);
                setState(642);
                _errHandler.sync(this);
                _la = _input.LA(1);
                if (_la == LessThan) {
                    {
                        setState(641);
                        typeParameters();
                    }
                }

                setState(644);
                match(OpenParen);
                setState(646);
                _errHandler.sync(this);
                _la = _input.LA(1);
                if ((((_la) & ~0x3f) == 0 && ((1L << _la) & 1729382256910401808L) != 0)
                        || ((((_la - 70)) & ~0x3f) == 0 && ((1L << (_la - 70)) & -4294967297L) != 0)
                        || ((((_la - 135)) & ~0x3f) == 0 && ((1L << (_la - 135)) & 13L) != 0)) {
                    {
                        setState(645);
                        parameterList();
                    }
                }

                setState(648);
                match(CloseParen);
                setState(650);
                _errHandler.sync(this);
                _la = _input.LA(1);
                if (_la == Colon) {
                    {
                        setState(649);
                        typeAnnotation();
                    }
                }

            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class IndexSignatureContext extends ParserRuleContext {
        public TerminalNode OpenBracket() {
            return getToken(TypeScriptParser.OpenBracket, 0);
        }

        public IdentifierContext identifier() {
            return getRuleContext(IdentifierContext.class, 0);
        }

        public TerminalNode Colon() {
            return getToken(TypeScriptParser.Colon, 0);
        }

        public TerminalNode CloseBracket() {
            return getToken(TypeScriptParser.CloseBracket, 0);
        }

        public TypeAnnotationContext typeAnnotation() {
            return getRuleContext(TypeAnnotationContext.class, 0);
        }

        public TerminalNode Number() {
            return getToken(TypeScriptParser.Number, 0);
        }

        public TerminalNode String() {
            return getToken(TypeScriptParser.String, 0);
        }

        public IndexSignatureContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_indexSignature;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterIndexSignature(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitIndexSignature(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitIndexSignature(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final IndexSignatureContext indexSignature() throws RecognitionException {
        IndexSignatureContext _localctx = new IndexSignatureContext(_ctx, getState());
        enterRule(_localctx, 78, RULE_indexSignature);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(652);
                match(OpenBracket);
                setState(653);
                identifier();
                setState(654);
                match(Colon);
                setState(655);
                _la = _input.LA(1);
                if (!(_la == Number || _la == String)) {
                    _errHandler.recoverInline(this);
                } else {
                    if (_input.LA(1) == Token.EOF)
                        matchedEOF = true;
                    _errHandler.reportMatch(this);
                    consume();
                }
                setState(656);
                match(CloseBracket);
                setState(657);
                typeAnnotation();
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class MethodSignatureContext extends ParserRuleContext {
        public PropertyNameContext propertyName() {
            return getRuleContext(PropertyNameContext.class, 0);
        }

        public CallSignatureContext callSignature() {
            return getRuleContext(CallSignatureContext.class, 0);
        }

        public TerminalNode QuestionMark() {
            return getToken(TypeScriptParser.QuestionMark, 0);
        }

        public MethodSignatureContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_methodSignature;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterMethodSignature(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitMethodSignature(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitMethodSignature(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final MethodSignatureContext methodSignature() throws RecognitionException {
        MethodSignatureContext _localctx = new MethodSignatureContext(_ctx, getState());
        enterRule(_localctx, 80, RULE_methodSignature);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(659);
                propertyName();
                setState(661);
                _errHandler.sync(this);
                _la = _input.LA(1);
                if (_la == QuestionMark) {
                    {
                        setState(660);
                        match(QuestionMark);
                    }
                }

                setState(663);
                callSignature();
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class TypeAliasDeclarationContext extends ParserRuleContext {
        public TerminalNode TypeAlias() {
            return getToken(TypeScriptParser.TypeAlias, 0);
        }

        public IdentifierContext identifier() {
            return getRuleContext(IdentifierContext.class, 0);
        }

        public TerminalNode Assign() {
            return getToken(TypeScriptParser.Assign, 0);
        }

        public Type_Context type_() {
            return getRuleContext(Type_Context.class, 0);
        }

        public EosContext eos() {
            return getRuleContext(EosContext.class, 0);
        }

        public TerminalNode Export() {
            return getToken(TypeScriptParser.Export, 0);
        }

        public TypeParametersContext typeParameters() {
            return getRuleContext(TypeParametersContext.class, 0);
        }

        public TypeAliasDeclarationContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_typeAliasDeclaration;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterTypeAliasDeclaration(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitTypeAliasDeclaration(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitTypeAliasDeclaration(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final TypeAliasDeclarationContext typeAliasDeclaration() throws RecognitionException {
        TypeAliasDeclarationContext _localctx = new TypeAliasDeclarationContext(_ctx, getState());
        enterRule(_localctx, 82, RULE_typeAliasDeclaration);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(666);
                _errHandler.sync(this);
                _la = _input.LA(1);
                if (_la == Export) {
                    {
                        setState(665);
                        match(Export);
                    }
                }

                setState(668);
                match(TypeAlias);
                setState(669);
                identifier();
                setState(671);
                _errHandler.sync(this);
                _la = _input.LA(1);
                if (_la == LessThan) {
                    {
                        setState(670);
                        typeParameters();
                    }
                }

                setState(673);
                match(Assign);
                setState(674);
                type_();
                setState(675);
                eos();
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class ConstructorDeclarationContext extends ParserRuleContext {
        public TerminalNode Constructor() {
            return getToken(TypeScriptParser.Constructor, 0);
        }

        public TerminalNode OpenParen() {
            return getToken(TypeScriptParser.OpenParen, 0);
        }

        public TerminalNode CloseParen() {
            return getToken(TypeScriptParser.CloseParen, 0);
        }

        public AccessibilityModifierContext accessibilityModifier() {
            return getRuleContext(AccessibilityModifierContext.class, 0);
        }

        public FormalParameterListContext formalParameterList() {
            return getRuleContext(FormalParameterListContext.class, 0);
        }

        public TerminalNode SemiColon() {
            return getToken(TypeScriptParser.SemiColon, 0);
        }

        public TerminalNode OpenBrace() {
            return getToken(TypeScriptParser.OpenBrace, 0);
        }

        public FunctionBodyContext functionBody() {
            return getRuleContext(FunctionBodyContext.class, 0);
        }

        public TerminalNode CloseBrace() {
            return getToken(TypeScriptParser.CloseBrace, 0);
        }

        public ConstructorDeclarationContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_constructorDeclaration;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterConstructorDeclaration(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitConstructorDeclaration(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitConstructorDeclaration(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final ConstructorDeclarationContext constructorDeclaration() throws RecognitionException {
        ConstructorDeclarationContext _localctx = new ConstructorDeclarationContext(_ctx, getState());
        enterRule(_localctx, 84, RULE_constructorDeclaration);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(678);
                _errHandler.sync(this);
                _la = _input.LA(1);
                if (((((_la - 112)) & ~0x3f) == 0 && ((1L << (_la - 112)) & 19L) != 0)) {
                    {
                        setState(677);
                        accessibilityModifier();
                    }
                }

                setState(680);
                match(Constructor);
                setState(681);
                match(OpenParen);
                setState(683);
                _errHandler.sync(this);
                _la = _input.LA(1);
                if ((((_la) & ~0x3f) == 0 && ((1L << _la) & 131344L) != 0)
                        || ((((_la - 70)) & ~0x3f) == 0 && ((1L << (_la - 70)) & -4294967297L) != 0)
                        || ((((_la - 135)) & ~0x3f) == 0 && ((1L << (_la - 135)) & 13L) != 0)) {
                    {
                        setState(682);
                        formalParameterList();
                    }
                }

                setState(685);
                match(CloseParen);
                setState(691);
                _errHandler.sync(this);
                switch (getInterpreter().adaptivePredict(_input, 63, _ctx)) {
                    case 1: {
                        {
                            setState(686);
                            match(OpenBrace);
                            setState(687);
                            functionBody();
                            setState(688);
                            match(CloseBrace);
                        }
                    }
                        break;
                    case 2: {
                        setState(690);
                        match(SemiColon);
                    }
                        break;
                }
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class InterfaceDeclarationContext extends ParserRuleContext {
        public TerminalNode Interface() {
            return getToken(TypeScriptParser.Interface, 0);
        }

        public IdentifierContext identifier() {
            return getRuleContext(IdentifierContext.class, 0);
        }

        public ObjectTypeContext objectType() {
            return getRuleContext(ObjectTypeContext.class, 0);
        }

        public TerminalNode Export() {
            return getToken(TypeScriptParser.Export, 0);
        }

        public TerminalNode Declare() {
            return getToken(TypeScriptParser.Declare, 0);
        }

        public TypeParametersContext typeParameters() {
            return getRuleContext(TypeParametersContext.class, 0);
        }

        public InterfaceExtendsClauseContext interfaceExtendsClause() {
            return getRuleContext(InterfaceExtendsClauseContext.class, 0);
        }

        public TerminalNode SemiColon() {
            return getToken(TypeScriptParser.SemiColon, 0);
        }

        public InterfaceDeclarationContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_interfaceDeclaration;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterInterfaceDeclaration(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitInterfaceDeclaration(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitInterfaceDeclaration(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final InterfaceDeclarationContext interfaceDeclaration() throws RecognitionException {
        InterfaceDeclarationContext _localctx = new InterfaceDeclarationContext(_ctx, getState());
        enterRule(_localctx, 86, RULE_interfaceDeclaration);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(694);
                _errHandler.sync(this);
                _la = _input.LA(1);
                if (_la == Export) {
                    {
                        setState(693);
                        match(Export);
                    }
                }

                setState(697);
                _errHandler.sync(this);
                _la = _input.LA(1);
                if (_la == Declare) {
                    {
                        setState(696);
                        match(Declare);
                    }
                }

                setState(699);
                match(Interface);
                setState(700);
                identifier();
                setState(702);
                _errHandler.sync(this);
                _la = _input.LA(1);
                if (_la == LessThan) {
                    {
                        setState(701);
                        typeParameters();
                    }
                }

                setState(705);
                _errHandler.sync(this);
                _la = _input.LA(1);
                if (_la == Extends) {
                    {
                        setState(704);
                        interfaceExtendsClause();
                    }
                }

                setState(707);
                objectType();
                setState(709);
                _errHandler.sync(this);
                switch (getInterpreter().adaptivePredict(_input, 68, _ctx)) {
                    case 1: {
                        setState(708);
                        match(SemiColon);
                    }
                        break;
                }
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class InterfaceExtendsClauseContext extends ParserRuleContext {
        public TerminalNode Extends() {
            return getToken(TypeScriptParser.Extends, 0);
        }

        public ClassOrInterfaceTypeListContext classOrInterfaceTypeList() {
            return getRuleContext(ClassOrInterfaceTypeListContext.class, 0);
        }

        public InterfaceExtendsClauseContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_interfaceExtendsClause;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterInterfaceExtendsClause(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitInterfaceExtendsClause(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitInterfaceExtendsClause(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final InterfaceExtendsClauseContext interfaceExtendsClause() throws RecognitionException {
        InterfaceExtendsClauseContext _localctx = new InterfaceExtendsClauseContext(_ctx, getState());
        enterRule(_localctx, 88, RULE_interfaceExtendsClause);
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(711);
                match(Extends);
                setState(712);
                classOrInterfaceTypeList();
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class ClassOrInterfaceTypeListContext extends ParserRuleContext {
        public List<TypeReferenceContext> typeReference() {
            return getRuleContexts(TypeReferenceContext.class);
        }

        public TypeReferenceContext typeReference(int i) {
            return getRuleContext(TypeReferenceContext.class, i);
        }

        public List<TerminalNode> Comma() {
            return getTokens(TypeScriptParser.Comma);
        }

        public TerminalNode Comma(int i) {
            return getToken(TypeScriptParser.Comma, i);
        }

        public ClassOrInterfaceTypeListContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_classOrInterfaceTypeList;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterClassOrInterfaceTypeList(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitClassOrInterfaceTypeList(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitClassOrInterfaceTypeList(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final ClassOrInterfaceTypeListContext classOrInterfaceTypeList() throws RecognitionException {
        ClassOrInterfaceTypeListContext _localctx = new ClassOrInterfaceTypeListContext(_ctx, getState());
        enterRule(_localctx, 90, RULE_classOrInterfaceTypeList);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(714);
                typeReference();
                setState(719);
                _errHandler.sync(this);
                _la = _input.LA(1);
                while (_la == Comma) {
                    {
                        {
                            setState(715);
                            match(Comma);
                            setState(716);
                            typeReference();
                        }
                    }
                    setState(721);
                    _errHandler.sync(this);
                    _la = _input.LA(1);
                }
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class EnumDeclarationContext extends ParserRuleContext {
        public TerminalNode Enum() {
            return getToken(TypeScriptParser.Enum, 0);
        }

        public IdentifierContext identifier() {
            return getRuleContext(IdentifierContext.class, 0);
        }

        public TerminalNode OpenBrace() {
            return getToken(TypeScriptParser.OpenBrace, 0);
        }

        public TerminalNode CloseBrace() {
            return getToken(TypeScriptParser.CloseBrace, 0);
        }

        public TerminalNode Const() {
            return getToken(TypeScriptParser.Const, 0);
        }

        public EnumBodyContext enumBody() {
            return getRuleContext(EnumBodyContext.class, 0);
        }

        public EnumDeclarationContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_enumDeclaration;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterEnumDeclaration(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitEnumDeclaration(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitEnumDeclaration(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final EnumDeclarationContext enumDeclaration() throws RecognitionException {
        EnumDeclarationContext _localctx = new EnumDeclarationContext(_ctx, getState());
        enterRule(_localctx, 92, RULE_enumDeclaration);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(723);
                _errHandler.sync(this);
                _la = _input.LA(1);
                if (_la == Const) {
                    {
                        setState(722);
                        match(Const);
                    }
                }

                setState(725);
                match(Enum);
                setState(726);
                identifier();
                setState(727);
                match(OpenBrace);
                setState(729);
                _errHandler.sync(this);
                _la = _input.LA(1);
                if ((((_la) & ~0x3f) == 0 && ((1L << _la) & -576460752303423472L) != 0)
                        || ((((_la - 64)) & ~0x3f) == 0 && ((1L << (_la - 64)) & -274877907005L) != 0)
                        || ((((_la - 128)) & ~0x3f) == 0 && ((1L << (_la - 128)) & 3263L) != 0)) {
                    {
                        setState(728);
                        enumBody();
                    }
                }

                setState(731);
                match(CloseBrace);
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class EnumBodyContext extends ParserRuleContext {
        public EnumMemberListContext enumMemberList() {
            return getRuleContext(EnumMemberListContext.class, 0);
        }

        public TerminalNode Comma() {
            return getToken(TypeScriptParser.Comma, 0);
        }

        public EnumBodyContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_enumBody;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterEnumBody(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitEnumBody(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitEnumBody(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final EnumBodyContext enumBody() throws RecognitionException {
        EnumBodyContext _localctx = new EnumBodyContext(_ctx, getState());
        enterRule(_localctx, 94, RULE_enumBody);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(733);
                enumMemberList();
                setState(735);
                _errHandler.sync(this);
                _la = _input.LA(1);
                if (_la == Comma) {
                    {
                        setState(734);
                        match(Comma);
                    }
                }

            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class EnumMemberListContext extends ParserRuleContext {
        public List<EnumMemberContext> enumMember() {
            return getRuleContexts(EnumMemberContext.class);
        }

        public EnumMemberContext enumMember(int i) {
            return getRuleContext(EnumMemberContext.class, i);
        }

        public List<TerminalNode> Comma() {
            return getTokens(TypeScriptParser.Comma);
        }

        public TerminalNode Comma(int i) {
            return getToken(TypeScriptParser.Comma, i);
        }

        public EnumMemberListContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_enumMemberList;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterEnumMemberList(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitEnumMemberList(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitEnumMemberList(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final EnumMemberListContext enumMemberList() throws RecognitionException {
        EnumMemberListContext _localctx = new EnumMemberListContext(_ctx, getState());
        enterRule(_localctx, 96, RULE_enumMemberList);
        try {
            int _alt;
            enterOuterAlt(_localctx, 1);
            {
                setState(737);
                enumMember();
                setState(742);
                _errHandler.sync(this);
                _alt = getInterpreter().adaptivePredict(_input, 73, _ctx);
                while (_alt != 2 && _alt != org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER) {
                    if (_alt == 1) {
                        {
                            {
                                setState(738);
                                match(Comma);
                                setState(739);
                                enumMember();
                            }
                        }
                    }
                    setState(744);
                    _errHandler.sync(this);
                    _alt = getInterpreter().adaptivePredict(_input, 73, _ctx);
                }
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class EnumMemberContext extends ParserRuleContext {
        public PropertyNameContext propertyName() {
            return getRuleContext(PropertyNameContext.class, 0);
        }

        public TerminalNode Assign() {
            return getToken(TypeScriptParser.Assign, 0);
        }

        public SingleExpressionContext singleExpression() {
            return getRuleContext(SingleExpressionContext.class, 0);
        }

        public EnumMemberContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_enumMember;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterEnumMember(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitEnumMember(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitEnumMember(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final EnumMemberContext enumMember() throws RecognitionException {
        EnumMemberContext _localctx = new EnumMemberContext(_ctx, getState());
        enterRule(_localctx, 98, RULE_enumMember);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(745);
                propertyName();
                setState(748);
                _errHandler.sync(this);
                _la = _input.LA(1);
                if (_la == Assign) {
                    {
                        setState(746);
                        match(Assign);
                        setState(747);
                        singleExpression(0);
                    }
                }

            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class NamespaceDeclarationContext extends ParserRuleContext {
        public TerminalNode Namespace() {
            return getToken(TypeScriptParser.Namespace, 0);
        }

        public NamespaceNameContext namespaceName() {
            return getRuleContext(NamespaceNameContext.class, 0);
        }

        public TerminalNode OpenBrace() {
            return getToken(TypeScriptParser.OpenBrace, 0);
        }

        public TerminalNode CloseBrace() {
            return getToken(TypeScriptParser.CloseBrace, 0);
        }

        public TerminalNode Declare() {
            return getToken(TypeScriptParser.Declare, 0);
        }

        public StatementListContext statementList() {
            return getRuleContext(StatementListContext.class, 0);
        }

        public NamespaceDeclarationContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_namespaceDeclaration;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterNamespaceDeclaration(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitNamespaceDeclaration(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitNamespaceDeclaration(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final NamespaceDeclarationContext namespaceDeclaration() throws RecognitionException {
        NamespaceDeclarationContext _localctx = new NamespaceDeclarationContext(_ctx, getState());
        enterRule(_localctx, 100, RULE_namespaceDeclaration);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(751);
                _errHandler.sync(this);
                _la = _input.LA(1);
                if (_la == Declare) {
                    {
                        setState(750);
                        match(Declare);
                    }
                }

                setState(753);
                match(Namespace);
                setState(754);
                namespaceName();
                setState(755);
                match(OpenBrace);
                setState(757);
                _errHandler.sync(this);
                switch (getInterpreter().adaptivePredict(_input, 76, _ctx)) {
                    case 1: {
                        setState(756);
                        statementList();
                    }
                        break;
                }
                setState(759);
                match(CloseBrace);
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class NamespaceNameContext extends ParserRuleContext {
        public List<IdentifierContext> identifier() {
            return getRuleContexts(IdentifierContext.class);
        }

        public IdentifierContext identifier(int i) {
            return getRuleContext(IdentifierContext.class, i);
        }

        public List<TerminalNode> Dot() {
            return getTokens(TypeScriptParser.Dot);
        }

        public TerminalNode Dot(int i) {
            return getToken(TypeScriptParser.Dot, i);
        }

        public NamespaceNameContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_namespaceName;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterNamespaceName(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitNamespaceName(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitNamespaceName(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final NamespaceNameContext namespaceName() throws RecognitionException {
        NamespaceNameContext _localctx = new NamespaceNameContext(_ctx, getState());
        enterRule(_localctx, 102, RULE_namespaceName);
        int _la;
        try {
            int _alt;
            enterOuterAlt(_localctx, 1);
            {
                setState(761);
                identifier();
                setState(770);
                _errHandler.sync(this);
                _alt = getInterpreter().adaptivePredict(_input, 78, _ctx);
                while (_alt != 2 && _alt != org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER) {
                    if (_alt == 1) {
                        {
                            {
                                setState(763);
                                _errHandler.sync(this);
                                _la = _input.LA(1);
                                do {
                                    {
                                        {
                                            setState(762);
                                            match(Dot);
                                        }
                                    }
                                    setState(765);
                                    _errHandler.sync(this);
                                    _la = _input.LA(1);
                                } while (_la == Dot);
                                setState(767);
                                identifier();
                            }
                        }
                    }
                    setState(772);
                    _errHandler.sync(this);
                    _alt = getInterpreter().adaptivePredict(_input, 78, _ctx);
                }
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class ImportAliasDeclarationContext extends ParserRuleContext {
        public IdentifierContext identifier() {
            return getRuleContext(IdentifierContext.class, 0);
        }

        public TerminalNode Assign() {
            return getToken(TypeScriptParser.Assign, 0);
        }

        public NamespaceNameContext namespaceName() {
            return getRuleContext(NamespaceNameContext.class, 0);
        }

        public TerminalNode SemiColon() {
            return getToken(TypeScriptParser.SemiColon, 0);
        }

        public ImportAliasDeclarationContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_importAliasDeclaration;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterImportAliasDeclaration(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitImportAliasDeclaration(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitImportAliasDeclaration(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final ImportAliasDeclarationContext importAliasDeclaration() throws RecognitionException {
        ImportAliasDeclarationContext _localctx = new ImportAliasDeclarationContext(_ctx, getState());
        enterRule(_localctx, 104, RULE_importAliasDeclaration);
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(773);
                identifier();
                setState(774);
                match(Assign);
                setState(775);
                namespaceName();
                setState(776);
                match(SemiColon);
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class DecoratorListContext extends ParserRuleContext {
        public List<DecoratorContext> decorator() {
            return getRuleContexts(DecoratorContext.class);
        }

        public DecoratorContext decorator(int i) {
            return getRuleContext(DecoratorContext.class, i);
        }

        public DecoratorListContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_decoratorList;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterDecoratorList(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitDecoratorList(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitDecoratorList(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final DecoratorListContext decoratorList() throws RecognitionException {
        DecoratorListContext _localctx = new DecoratorListContext(_ctx, getState());
        enterRule(_localctx, 106, RULE_decoratorList);
        try {
            int _alt;
            enterOuterAlt(_localctx, 1);
            {
                setState(779);
                _errHandler.sync(this);
                _alt = 1;
                do {
                    switch (_alt) {
                        case 1: {
                            {
                                setState(778);
                                decorator();
                            }
                        }
                            break;
                        default:
                            throw new NoViableAltException(this);
                    }
                    setState(781);
                    _errHandler.sync(this);
                    _alt = getInterpreter().adaptivePredict(_input, 79, _ctx);
                } while (_alt != 2 && _alt != org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER);
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class DecoratorContext extends ParserRuleContext {
        public TerminalNode At() {
            return getToken(TypeScriptParser.At, 0);
        }

        public DecoratorMemberExpressionContext decoratorMemberExpression() {
            return getRuleContext(DecoratorMemberExpressionContext.class, 0);
        }

        public DecoratorCallExpressionContext decoratorCallExpression() {
            return getRuleContext(DecoratorCallExpressionContext.class, 0);
        }

        public DecoratorContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_decorator;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterDecorator(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitDecorator(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitDecorator(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final DecoratorContext decorator() throws RecognitionException {
        DecoratorContext _localctx = new DecoratorContext(_ctx, getState());
        enterRule(_localctx, 108, RULE_decorator);
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(783);
                match(At);
                setState(786);
                _errHandler.sync(this);
                switch (getInterpreter().adaptivePredict(_input, 80, _ctx)) {
                    case 1: {
                        setState(784);
                        decoratorMemberExpression(0);
                    }
                        break;
                    case 2: {
                        setState(785);
                        decoratorCallExpression();
                    }
                        break;
                }
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class DecoratorMemberExpressionContext extends ParserRuleContext {
        public IdentifierContext identifier() {
            return getRuleContext(IdentifierContext.class, 0);
        }

        public TerminalNode OpenParen() {
            return getToken(TypeScriptParser.OpenParen, 0);
        }

        public SingleExpressionContext singleExpression() {
            return getRuleContext(SingleExpressionContext.class, 0);
        }

        public TerminalNode CloseParen() {
            return getToken(TypeScriptParser.CloseParen, 0);
        }

        public DecoratorMemberExpressionContext decoratorMemberExpression() {
            return getRuleContext(DecoratorMemberExpressionContext.class, 0);
        }

        public TerminalNode Dot() {
            return getToken(TypeScriptParser.Dot, 0);
        }

        public IdentifierNameContext identifierName() {
            return getRuleContext(IdentifierNameContext.class, 0);
        }

        public DecoratorMemberExpressionContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_decoratorMemberExpression;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterDecoratorMemberExpression(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitDecoratorMemberExpression(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitDecoratorMemberExpression(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final DecoratorMemberExpressionContext decoratorMemberExpression() throws RecognitionException {
        return decoratorMemberExpression(0);
    }

    private DecoratorMemberExpressionContext decoratorMemberExpression(int _p) throws RecognitionException {
        ParserRuleContext _parentctx = _ctx;
        int _parentState = getState();
        DecoratorMemberExpressionContext _localctx = new DecoratorMemberExpressionContext(_ctx, _parentState);
        DecoratorMemberExpressionContext _prevctx = _localctx;
        int _startState = 110;
        enterRecursionRule(_localctx, 110, RULE_decoratorMemberExpression, _p);
        try {
            int _alt;
            enterOuterAlt(_localctx, 1);
            {
                setState(794);
                _errHandler.sync(this);
                switch (_input.LA(1)) {
                    case As:
                    case From:
                    case Async:
                    case Yield:
                    case Any:
                    case Number:
                    case Never:
                    case Boolean:
                    case String:
                    case Unique:
                    case Symbol:
                    case Undefined:
                    case Object:
                    case Of:
                    case KeyOf:
                    case TypeAlias:
                    case Constructor:
                    case Namespace:
                    case Abstract:
                    case Identifier: {
                        setState(789);
                        identifier();
                    }
                        break;
                    case OpenParen: {
                        setState(790);
                        match(OpenParen);
                        setState(791);
                        singleExpression(0);
                        setState(792);
                        match(CloseParen);
                    }
                        break;
                    default:
                        throw new NoViableAltException(this);
                }
                _ctx.stop = _input.LT(-1);
                setState(801);
                _errHandler.sync(this);
                _alt = getInterpreter().adaptivePredict(_input, 82, _ctx);
                while (_alt != 2 && _alt != org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER) {
                    if (_alt == 1) {
                        if (_parseListeners != null)
                            triggerExitRuleEvent();
                        _prevctx = _localctx;
                        {
                            {
                                _localctx = new DecoratorMemberExpressionContext(_parentctx, _parentState);
                                pushNewRecursionContext(_localctx, _startState, RULE_decoratorMemberExpression);
                                setState(796);
                                if (!(precpred(_ctx, 2)))
                                    throw new FailedPredicateException(this, "precpred(_ctx, 2)");
                                setState(797);
                                match(Dot);
                                setState(798);
                                identifierName();
                            }
                        }
                    }
                    setState(803);
                    _errHandler.sync(this);
                    _alt = getInterpreter().adaptivePredict(_input, 82, _ctx);
                }
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            unrollRecursionContexts(_parentctx);
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class DecoratorCallExpressionContext extends ParserRuleContext {
        public DecoratorMemberExpressionContext decoratorMemberExpression() {
            return getRuleContext(DecoratorMemberExpressionContext.class, 0);
        }

        public ArgumentsContext arguments() {
            return getRuleContext(ArgumentsContext.class, 0);
        }

        public DecoratorCallExpressionContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_decoratorCallExpression;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterDecoratorCallExpression(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitDecoratorCallExpression(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitDecoratorCallExpression(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final DecoratorCallExpressionContext decoratorCallExpression() throws RecognitionException {
        DecoratorCallExpressionContext _localctx = new DecoratorCallExpressionContext(_ctx, getState());
        enterRule(_localctx, 112, RULE_decoratorCallExpression);
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(804);
                decoratorMemberExpression(0);
                setState(805);
                arguments();
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class ProgramContext extends ParserRuleContext {
        public TerminalNode EOF() {
            return getToken(TypeScriptParser.EOF, 0);
        }

        public SourceElementsContext sourceElements() {
            return getRuleContext(SourceElementsContext.class, 0);
        }

        public ProgramContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_program;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterProgram(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitProgram(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitProgram(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final ProgramContext program() throws RecognitionException {
        ProgramContext _localctx = new ProgramContext(_ctx, getState());
        enterRule(_localctx, 114, RULE_program);
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(808);
                _errHandler.sync(this);
                switch (getInterpreter().adaptivePredict(_input, 83, _ctx)) {
                    case 1: {
                        setState(807);
                        sourceElements();
                    }
                        break;
                }
                setState(810);
                match(EOF);
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class SourceElementContext extends ParserRuleContext {
        public StatementContext statement() {
            return getRuleContext(StatementContext.class, 0);
        }

        public TerminalNode Export() {
            return getToken(TypeScriptParser.Export, 0);
        }

        public SourceElementContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_sourceElement;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterSourceElement(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitSourceElement(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitSourceElement(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final SourceElementContext sourceElement() throws RecognitionException {
        SourceElementContext _localctx = new SourceElementContext(_ctx, getState());
        enterRule(_localctx, 116, RULE_sourceElement);
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(813);
                _errHandler.sync(this);
                switch (getInterpreter().adaptivePredict(_input, 84, _ctx)) {
                    case 1: {
                        setState(812);
                        match(Export);
                    }
                        break;
                }
                setState(815);
                statement();
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class StatementContext extends ParserRuleContext {
        public BlockContext block() {
            return getRuleContext(BlockContext.class, 0);
        }

        public VariableStatementContext variableStatement() {
            return getRuleContext(VariableStatementContext.class, 0);
        }

        public ImportStatementContext importStatement() {
            return getRuleContext(ImportStatementContext.class, 0);
        }

        public ExportStatementContext exportStatement() {
            return getRuleContext(ExportStatementContext.class, 0);
        }

        public EmptyStatement_Context emptyStatement_() {
            return getRuleContext(EmptyStatement_Context.class, 0);
        }

        public AbstractDeclarationContext abstractDeclaration() {
            return getRuleContext(AbstractDeclarationContext.class, 0);
        }

        public ClassDeclarationContext classDeclaration() {
            return getRuleContext(ClassDeclarationContext.class, 0);
        }

        public FunctionDeclarationContext functionDeclaration() {
            return getRuleContext(FunctionDeclarationContext.class, 0);
        }

        public ExpressionStatementContext expressionStatement() {
            return getRuleContext(ExpressionStatementContext.class, 0);
        }

        public InterfaceDeclarationContext interfaceDeclaration() {
            return getRuleContext(InterfaceDeclarationContext.class, 0);
        }

        public NamespaceDeclarationContext namespaceDeclaration() {
            return getRuleContext(NamespaceDeclarationContext.class, 0);
        }

        public IfStatementContext ifStatement() {
            return getRuleContext(IfStatementContext.class, 0);
        }

        public IterationStatementContext iterationStatement() {
            return getRuleContext(IterationStatementContext.class, 0);
        }

        public ContinueStatementContext continueStatement() {
            return getRuleContext(ContinueStatementContext.class, 0);
        }

        public BreakStatementContext breakStatement() {
            return getRuleContext(BreakStatementContext.class, 0);
        }

        public ReturnStatementContext returnStatement() {
            return getRuleContext(ReturnStatementContext.class, 0);
        }

        public YieldStatementContext yieldStatement() {
            return getRuleContext(YieldStatementContext.class, 0);
        }

        public WithStatementContext withStatement() {
            return getRuleContext(WithStatementContext.class, 0);
        }

        public LabelledStatementContext labelledStatement() {
            return getRuleContext(LabelledStatementContext.class, 0);
        }

        public SwitchStatementContext switchStatement() {
            return getRuleContext(SwitchStatementContext.class, 0);
        }

        public ThrowStatementContext throwStatement() {
            return getRuleContext(ThrowStatementContext.class, 0);
        }

        public TryStatementContext tryStatement() {
            return getRuleContext(TryStatementContext.class, 0);
        }

        public DebuggerStatementContext debuggerStatement() {
            return getRuleContext(DebuggerStatementContext.class, 0);
        }

        public ArrowFunctionDeclarationContext arrowFunctionDeclaration() {
            return getRuleContext(ArrowFunctionDeclarationContext.class, 0);
        }

        public GeneratorFunctionDeclarationContext generatorFunctionDeclaration() {
            return getRuleContext(GeneratorFunctionDeclarationContext.class, 0);
        }

        public TypeAliasDeclarationContext typeAliasDeclaration() {
            return getRuleContext(TypeAliasDeclarationContext.class, 0);
        }

        public EnumDeclarationContext enumDeclaration() {
            return getRuleContext(EnumDeclarationContext.class, 0);
        }

        public TerminalNode Export() {
            return getToken(TypeScriptParser.Export, 0);
        }

        public StatementContext statement() {
            return getRuleContext(StatementContext.class, 0);
        }

        public StatementContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_statement;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterStatement(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitStatement(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitStatement(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final StatementContext statement() throws RecognitionException {
        StatementContext _localctx = new StatementContext(_ctx, getState());
        enterRule(_localctx, 118, RULE_statement);
        try {
            setState(846);
            _errHandler.sync(this);
            switch (getInterpreter().adaptivePredict(_input, 85, _ctx)) {
                case 1:
                    enterOuterAlt(_localctx, 1); {
                    setState(817);
                    block();
                }
                    break;
                case 2:
                    enterOuterAlt(_localctx, 2); {
                    setState(818);
                    variableStatement();
                }
                    break;
                case 3:
                    enterOuterAlt(_localctx, 3); {
                    setState(819);
                    importStatement();
                }
                    break;
                case 4:
                    enterOuterAlt(_localctx, 4); {
                    setState(820);
                    exportStatement();
                }
                    break;
                case 5:
                    enterOuterAlt(_localctx, 5); {
                    setState(821);
                    emptyStatement_();
                }
                    break;
                case 6:
                    enterOuterAlt(_localctx, 6); {
                    setState(822);
                    abstractDeclaration();
                }
                    break;
                case 7:
                    enterOuterAlt(_localctx, 7); {
                    setState(823);
                    classDeclaration();
                }
                    break;
                case 8:
                    enterOuterAlt(_localctx, 8); {
                    setState(824);
                    functionDeclaration();
                }
                    break;
                case 9:
                    enterOuterAlt(_localctx, 9); {
                    setState(825);
                    expressionStatement();
                }
                    break;
                case 10:
                    enterOuterAlt(_localctx, 10); {
                    setState(826);
                    interfaceDeclaration();
                }
                    break;
                case 11:
                    enterOuterAlt(_localctx, 11); {
                    setState(827);
                    namespaceDeclaration();
                }
                    break;
                case 12:
                    enterOuterAlt(_localctx, 12); {
                    setState(828);
                    ifStatement();
                }
                    break;
                case 13:
                    enterOuterAlt(_localctx, 13); {
                    setState(829);
                    iterationStatement();
                }
                    break;
                case 14:
                    enterOuterAlt(_localctx, 14); {
                    setState(830);
                    continueStatement();
                }
                    break;
                case 15:
                    enterOuterAlt(_localctx, 15); {
                    setState(831);
                    breakStatement();
                }
                    break;
                case 16:
                    enterOuterAlt(_localctx, 16); {
                    setState(832);
                    returnStatement();
                }
                    break;
                case 17:
                    enterOuterAlt(_localctx, 17); {
                    setState(833);
                    yieldStatement();
                }
                    break;
                case 18:
                    enterOuterAlt(_localctx, 18); {
                    setState(834);
                    withStatement();
                }
                    break;
                case 19:
                    enterOuterAlt(_localctx, 19); {
                    setState(835);
                    labelledStatement();
                }
                    break;
                case 20:
                    enterOuterAlt(_localctx, 20); {
                    setState(836);
                    switchStatement();
                }
                    break;
                case 21:
                    enterOuterAlt(_localctx, 21); {
                    setState(837);
                    throwStatement();
                }
                    break;
                case 22:
                    enterOuterAlt(_localctx, 22); {
                    setState(838);
                    tryStatement();
                }
                    break;
                case 23:
                    enterOuterAlt(_localctx, 23); {
                    setState(839);
                    debuggerStatement();
                }
                    break;
                case 24:
                    enterOuterAlt(_localctx, 24); {
                    setState(840);
                    arrowFunctionDeclaration();
                }
                    break;
                case 25:
                    enterOuterAlt(_localctx, 25); {
                    setState(841);
                    generatorFunctionDeclaration();
                }
                    break;
                case 26:
                    enterOuterAlt(_localctx, 26); {
                    setState(842);
                    typeAliasDeclaration();
                }
                    break;
                case 27:
                    enterOuterAlt(_localctx, 27); {
                    setState(843);
                    enumDeclaration();
                }
                    break;
                case 28:
                    enterOuterAlt(_localctx, 28); {
                    setState(844);
                    match(Export);
                    setState(845);
                    statement();
                }
                    break;
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class BlockContext extends ParserRuleContext {
        public TerminalNode OpenBrace() {
            return getToken(TypeScriptParser.OpenBrace, 0);
        }

        public TerminalNode CloseBrace() {
            return getToken(TypeScriptParser.CloseBrace, 0);
        }

        public StatementListContext statementList() {
            return getRuleContext(StatementListContext.class, 0);
        }

        public BlockContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_block;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterBlock(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitBlock(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitBlock(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final BlockContext block() throws RecognitionException {
        BlockContext _localctx = new BlockContext(_ctx, getState());
        enterRule(_localctx, 120, RULE_block);
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(848);
                match(OpenBrace);
                setState(850);
                _errHandler.sync(this);
                switch (getInterpreter().adaptivePredict(_input, 86, _ctx)) {
                    case 1: {
                        setState(849);
                        statementList();
                    }
                        break;
                }
                setState(852);
                match(CloseBrace);
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class StatementListContext extends ParserRuleContext {
        public List<StatementContext> statement() {
            return getRuleContexts(StatementContext.class);
        }

        public StatementContext statement(int i) {
            return getRuleContext(StatementContext.class, i);
        }

        public StatementListContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_statementList;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterStatementList(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitStatementList(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitStatementList(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final StatementListContext statementList() throws RecognitionException {
        StatementListContext _localctx = new StatementListContext(_ctx, getState());
        enterRule(_localctx, 122, RULE_statementList);
        try {
            int _alt;
            enterOuterAlt(_localctx, 1);
            {
                setState(855);
                _errHandler.sync(this);
                _alt = 1;
                do {
                    switch (_alt) {
                        case 1: {
                            {
                                setState(854);
                                statement();
                            }
                        }
                            break;
                        default:
                            throw new NoViableAltException(this);
                    }
                    setState(857);
                    _errHandler.sync(this);
                    _alt = getInterpreter().adaptivePredict(_input, 87, _ctx);
                } while (_alt != 2 && _alt != org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER);
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class AbstractDeclarationContext extends ParserRuleContext {
        public TerminalNode Abstract() {
            return getToken(TypeScriptParser.Abstract, 0);
        }

        public EosContext eos() {
            return getRuleContext(EosContext.class, 0);
        }

        public IdentifierContext identifier() {
            return getRuleContext(IdentifierContext.class, 0);
        }

        public CallSignatureContext callSignature() {
            return getRuleContext(CallSignatureContext.class, 0);
        }

        public VariableStatementContext variableStatement() {
            return getRuleContext(VariableStatementContext.class, 0);
        }

        public AbstractDeclarationContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_abstractDeclaration;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterAbstractDeclaration(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitAbstractDeclaration(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitAbstractDeclaration(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final AbstractDeclarationContext abstractDeclaration() throws RecognitionException {
        AbstractDeclarationContext _localctx = new AbstractDeclarationContext(_ctx, getState());
        enterRule(_localctx, 124, RULE_abstractDeclaration);
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(859);
                match(Abstract);
                setState(864);
                _errHandler.sync(this);
                switch (getInterpreter().adaptivePredict(_input, 88, _ctx)) {
                    case 1: {
                        setState(860);
                        identifier();
                        setState(861);
                        callSignature();
                    }
                        break;
                    case 2: {
                        setState(863);
                        variableStatement();
                    }
                        break;
                }
                setState(866);
                eos();
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class ImportStatementContext extends ParserRuleContext {
        public TerminalNode Import() {
            return getToken(TypeScriptParser.Import, 0);
        }

        public ImportFromBlockContext importFromBlock() {
            return getRuleContext(ImportFromBlockContext.class, 0);
        }

        public ImportStatementContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_importStatement;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterImportStatement(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitImportStatement(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitImportStatement(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final ImportStatementContext importStatement() throws RecognitionException {
        ImportStatementContext _localctx = new ImportStatementContext(_ctx, getState());
        enterRule(_localctx, 126, RULE_importStatement);
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(868);
                match(Import);
                setState(869);
                importFromBlock();
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class ImportFromBlockContext extends ParserRuleContext {
        public ImportFromContext importFrom() {
            return getRuleContext(ImportFromContext.class, 0);
        }

        public EosContext eos() {
            return getRuleContext(EosContext.class, 0);
        }

        public ImportNamespaceContext importNamespace() {
            return getRuleContext(ImportNamespaceContext.class, 0);
        }

        public ImportModuleItemsContext importModuleItems() {
            return getRuleContext(ImportModuleItemsContext.class, 0);
        }

        public ImportDefaultContext importDefault() {
            return getRuleContext(ImportDefaultContext.class, 0);
        }

        public TerminalNode StringLiteral() {
            return getToken(TypeScriptParser.StringLiteral, 0);
        }

        public ImportFromBlockContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_importFromBlock;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterImportFromBlock(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitImportFromBlock(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitImportFromBlock(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final ImportFromBlockContext importFromBlock() throws RecognitionException {
        ImportFromBlockContext _localctx = new ImportFromBlockContext(_ctx, getState());
        enterRule(_localctx, 128, RULE_importFromBlock);
        try {
            setState(883);
            _errHandler.sync(this);
            switch (_input.LA(1)) {
                case OpenBrace:
                case Multiply:
                case NullLiteral:
                case BooleanLiteral:
                case Break:
                case Do:
                case Instanceof:
                case Typeof:
                case Case:
                case Else:
                case New:
                case Var:
                case Catch:
                case Finally:
                case Return:
                case Void:
                case Continue:
                case For:
                case Switch:
                case While:
                case Debugger:
                case Function_:
                case This:
                case With:
                case Default:
                case If:
                case Throw:
                case Delete:
                case In:
                case Try:
                case As:
                case From:
                case ReadOnly:
                case Async:
                case Await:
                case Yield:
                case Class:
                case Enum:
                case Extends:
                case Super:
                case Const:
                case Export:
                case Import:
                case Implements:
                case Let:
                case Private:
                case Public:
                case Interface:
                case Package:
                case Protected:
                case Static:
                case Any:
                case Number:
                case Never:
                case Boolean:
                case String:
                case Unique:
                case Symbol:
                case Undefined:
                case Object:
                case Of:
                case KeyOf:
                case TypeAlias:
                case Constructor:
                case Namespace:
                case Require:
                case Module:
                case Abstract:
                case Identifier:
                    enterOuterAlt(_localctx, 1); {
                    setState(872);
                    _errHandler.sync(this);
                    switch (getInterpreter().adaptivePredict(_input, 89, _ctx)) {
                        case 1: {
                            setState(871);
                            importDefault();
                        }
                            break;
                    }
                    setState(876);
                    _errHandler.sync(this);
                    switch (_input.LA(1)) {
                        case Multiply:
                        case NullLiteral:
                        case BooleanLiteral:
                        case Break:
                        case Do:
                        case Instanceof:
                        case Typeof:
                        case Case:
                        case Else:
                        case New:
                        case Var:
                        case Catch:
                        case Finally:
                        case Return:
                        case Void:
                        case Continue:
                        case For:
                        case Switch:
                        case While:
                        case Debugger:
                        case Function_:
                        case This:
                        case With:
                        case Default:
                        case If:
                        case Throw:
                        case Delete:
                        case In:
                        case Try:
                        case As:
                        case From:
                        case ReadOnly:
                        case Async:
                        case Await:
                        case Yield:
                        case Class:
                        case Enum:
                        case Extends:
                        case Super:
                        case Const:
                        case Export:
                        case Import:
                        case Implements:
                        case Let:
                        case Private:
                        case Public:
                        case Interface:
                        case Package:
                        case Protected:
                        case Static:
                        case Any:
                        case Number:
                        case Never:
                        case Boolean:
                        case String:
                        case Unique:
                        case Symbol:
                        case Undefined:
                        case Object:
                        case Of:
                        case KeyOf:
                        case TypeAlias:
                        case Constructor:
                        case Namespace:
                        case Require:
                        case Module:
                        case Abstract:
                        case Identifier: {
                            setState(874);
                            importNamespace();
                        }
                            break;
                        case OpenBrace: {
                            setState(875);
                            importModuleItems();
                        }
                            break;
                        default:
                            throw new NoViableAltException(this);
                    }
                    setState(878);
                    importFrom();
                    setState(879);
                    eos();
                }
                    break;
                case StringLiteral:
                    enterOuterAlt(_localctx, 2); {
                    setState(881);
                    match(StringLiteral);
                    setState(882);
                    eos();
                }
                    break;
                default:
                    throw new NoViableAltException(this);
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class ImportModuleItemsContext extends ParserRuleContext {
        public TerminalNode OpenBrace() {
            return getToken(TypeScriptParser.OpenBrace, 0);
        }

        public TerminalNode CloseBrace() {
            return getToken(TypeScriptParser.CloseBrace, 0);
        }

        public List<ImportAliasNameContext> importAliasName() {
            return getRuleContexts(ImportAliasNameContext.class);
        }

        public ImportAliasNameContext importAliasName(int i) {
            return getRuleContext(ImportAliasNameContext.class, i);
        }

        public List<TerminalNode> Comma() {
            return getTokens(TypeScriptParser.Comma);
        }

        public TerminalNode Comma(int i) {
            return getToken(TypeScriptParser.Comma, i);
        }

        public ImportModuleItemsContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_importModuleItems;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterImportModuleItems(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitImportModuleItems(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitImportModuleItems(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final ImportModuleItemsContext importModuleItems() throws RecognitionException {
        ImportModuleItemsContext _localctx = new ImportModuleItemsContext(_ctx, getState());
        enterRule(_localctx, 130, RULE_importModuleItems);
        int _la;
        try {
            int _alt;
            enterOuterAlt(_localctx, 1);
            {
                setState(885);
                match(OpenBrace);
                setState(891);
                _errHandler.sync(this);
                _alt = getInterpreter().adaptivePredict(_input, 92, _ctx);
                while (_alt != 2 && _alt != org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER) {
                    if (_alt == 1) {
                        {
                            {
                                setState(886);
                                importAliasName();
                                setState(887);
                                match(Comma);
                            }
                        }
                    }
                    setState(893);
                    _errHandler.sync(this);
                    _alt = getInterpreter().adaptivePredict(_input, 92, _ctx);
                }
                setState(898);
                _errHandler.sync(this);
                _la = _input.LA(1);
                if (((((_la - 59)) & ~0x3f) == 0 && ((1L << (_la - 59)) & -8796093024253L) != 0)
                        || ((((_la - 123)) & ~0x3f) == 0 && ((1L << (_la - 123)) & 104447L) != 0)) {
                    {
                        setState(894);
                        importAliasName();
                        setState(896);
                        _errHandler.sync(this);
                        _la = _input.LA(1);
                        if (_la == Comma) {
                            {
                                setState(895);
                                match(Comma);
                            }
                        }

                    }
                }

                setState(900);
                match(CloseBrace);
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class ImportAliasNameContext extends ParserRuleContext {
        public ModuleExportNameContext moduleExportName() {
            return getRuleContext(ModuleExportNameContext.class, 0);
        }

        public TerminalNode As() {
            return getToken(TypeScriptParser.As, 0);
        }

        public ImportedBindingContext importedBinding() {
            return getRuleContext(ImportedBindingContext.class, 0);
        }

        public ImportAliasNameContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_importAliasName;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterImportAliasName(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitImportAliasName(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitImportAliasName(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final ImportAliasNameContext importAliasName() throws RecognitionException {
        ImportAliasNameContext _localctx = new ImportAliasNameContext(_ctx, getState());
        enterRule(_localctx, 132, RULE_importAliasName);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(902);
                moduleExportName();
                setState(905);
                _errHandler.sync(this);
                _la = _input.LA(1);
                if (_la == As) {
                    {
                        setState(903);
                        match(As);
                        setState(904);
                        importedBinding();
                    }
                }

            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class ModuleExportNameContext extends ParserRuleContext {
        public IdentifierNameContext identifierName() {
            return getRuleContext(IdentifierNameContext.class, 0);
        }

        public TerminalNode StringLiteral() {
            return getToken(TypeScriptParser.StringLiteral, 0);
        }

        public ModuleExportNameContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_moduleExportName;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterModuleExportName(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitModuleExportName(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitModuleExportName(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final ModuleExportNameContext moduleExportName() throws RecognitionException {
        ModuleExportNameContext _localctx = new ModuleExportNameContext(_ctx, getState());
        enterRule(_localctx, 134, RULE_moduleExportName);
        try {
            setState(909);
            _errHandler.sync(this);
            switch (_input.LA(1)) {
                case NullLiteral:
                case BooleanLiteral:
                case Break:
                case Do:
                case Instanceof:
                case Typeof:
                case Case:
                case Else:
                case New:
                case Var:
                case Catch:
                case Finally:
                case Return:
                case Void:
                case Continue:
                case For:
                case Switch:
                case While:
                case Debugger:
                case Function_:
                case This:
                case With:
                case Default:
                case If:
                case Throw:
                case Delete:
                case In:
                case Try:
                case As:
                case From:
                case ReadOnly:
                case Async:
                case Await:
                case Yield:
                case Class:
                case Enum:
                case Extends:
                case Super:
                case Const:
                case Export:
                case Import:
                case Implements:
                case Let:
                case Private:
                case Public:
                case Interface:
                case Package:
                case Protected:
                case Static:
                case Any:
                case Number:
                case Never:
                case Boolean:
                case String:
                case Unique:
                case Symbol:
                case Undefined:
                case Object:
                case Of:
                case KeyOf:
                case TypeAlias:
                case Constructor:
                case Namespace:
                case Require:
                case Module:
                case Abstract:
                case Identifier:
                    enterOuterAlt(_localctx, 1); {
                    setState(907);
                    identifierName();
                }
                    break;
                case StringLiteral:
                    enterOuterAlt(_localctx, 2); {
                    setState(908);
                    match(StringLiteral);
                }
                    break;
                default:
                    throw new NoViableAltException(this);
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class ImportedBindingContext extends ParserRuleContext {
        public TerminalNode Identifier() {
            return getToken(TypeScriptParser.Identifier, 0);
        }

        public TerminalNode Yield() {
            return getToken(TypeScriptParser.Yield, 0);
        }

        public TerminalNode Await() {
            return getToken(TypeScriptParser.Await, 0);
        }

        public ImportedBindingContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_importedBinding;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterImportedBinding(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitImportedBinding(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitImportedBinding(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final ImportedBindingContext importedBinding() throws RecognitionException {
        ImportedBindingContext _localctx = new ImportedBindingContext(_ctx, getState());
        enterRule(_localctx, 136, RULE_importedBinding);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(911);
                _la = _input.LA(1);
                if (!(((((_la - 100)) & ~0x3f) == 0 && ((1L << (_la - 100)) & 274877906947L) != 0))) {
                    _errHandler.recoverInline(this);
                } else {
                    if (_input.LA(1) == Token.EOF)
                        matchedEOF = true;
                    _errHandler.reportMatch(this);
                    consume();
                }
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class ImportDefaultContext extends ParserRuleContext {
        public AliasNameContext aliasName() {
            return getRuleContext(AliasNameContext.class, 0);
        }

        public TerminalNode Comma() {
            return getToken(TypeScriptParser.Comma, 0);
        }

        public ImportDefaultContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_importDefault;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterImportDefault(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitImportDefault(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitImportDefault(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final ImportDefaultContext importDefault() throws RecognitionException {
        ImportDefaultContext _localctx = new ImportDefaultContext(_ctx, getState());
        enterRule(_localctx, 138, RULE_importDefault);
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(913);
                aliasName();
                setState(914);
                match(Comma);
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class ImportNamespaceContext extends ParserRuleContext {
        public TerminalNode Multiply() {
            return getToken(TypeScriptParser.Multiply, 0);
        }

        public List<IdentifierNameContext> identifierName() {
            return getRuleContexts(IdentifierNameContext.class);
        }

        public IdentifierNameContext identifierName(int i) {
            return getRuleContext(IdentifierNameContext.class, i);
        }

        public TerminalNode As() {
            return getToken(TypeScriptParser.As, 0);
        }

        public ImportNamespaceContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_importNamespace;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterImportNamespace(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitImportNamespace(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitImportNamespace(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final ImportNamespaceContext importNamespace() throws RecognitionException {
        ImportNamespaceContext _localctx = new ImportNamespaceContext(_ctx, getState());
        enterRule(_localctx, 140, RULE_importNamespace);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(918);
                _errHandler.sync(this);
                switch (_input.LA(1)) {
                    case Multiply: {
                        setState(916);
                        match(Multiply);
                    }
                        break;
                    case NullLiteral:
                    case BooleanLiteral:
                    case Break:
                    case Do:
                    case Instanceof:
                    case Typeof:
                    case Case:
                    case Else:
                    case New:
                    case Var:
                    case Catch:
                    case Finally:
                    case Return:
                    case Void:
                    case Continue:
                    case For:
                    case Switch:
                    case While:
                    case Debugger:
                    case Function_:
                    case This:
                    case With:
                    case Default:
                    case If:
                    case Throw:
                    case Delete:
                    case In:
                    case Try:
                    case As:
                    case From:
                    case ReadOnly:
                    case Async:
                    case Await:
                    case Yield:
                    case Class:
                    case Enum:
                    case Extends:
                    case Super:
                    case Const:
                    case Export:
                    case Import:
                    case Implements:
                    case Let:
                    case Private:
                    case Public:
                    case Interface:
                    case Package:
                    case Protected:
                    case Static:
                    case Any:
                    case Number:
                    case Never:
                    case Boolean:
                    case String:
                    case Unique:
                    case Symbol:
                    case Undefined:
                    case Object:
                    case Of:
                    case KeyOf:
                    case TypeAlias:
                    case Constructor:
                    case Namespace:
                    case Require:
                    case Module:
                    case Abstract:
                    case Identifier: {
                        setState(917);
                        identifierName();
                    }
                        break;
                    default:
                        throw new NoViableAltException(this);
                }
                setState(922);
                _errHandler.sync(this);
                _la = _input.LA(1);
                if (_la == As) {
                    {
                        setState(920);
                        match(As);
                        setState(921);
                        identifierName();
                    }
                }

            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class ImportFromContext extends ParserRuleContext {
        public TerminalNode From() {
            return getToken(TypeScriptParser.From, 0);
        }

        public TerminalNode StringLiteral() {
            return getToken(TypeScriptParser.StringLiteral, 0);
        }

        public ImportFromContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_importFrom;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterImportFrom(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitImportFrom(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitImportFrom(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final ImportFromContext importFrom() throws RecognitionException {
        ImportFromContext _localctx = new ImportFromContext(_ctx, getState());
        enterRule(_localctx, 142, RULE_importFrom);
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(924);
                match(From);
                setState(925);
                match(StringLiteral);
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class AliasNameContext extends ParserRuleContext {
        public List<IdentifierNameContext> identifierName() {
            return getRuleContexts(IdentifierNameContext.class);
        }

        public IdentifierNameContext identifierName(int i) {
            return getRuleContext(IdentifierNameContext.class, i);
        }

        public TerminalNode As() {
            return getToken(TypeScriptParser.As, 0);
        }

        public AliasNameContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_aliasName;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterAliasName(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitAliasName(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitAliasName(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final AliasNameContext aliasName() throws RecognitionException {
        AliasNameContext _localctx = new AliasNameContext(_ctx, getState());
        enterRule(_localctx, 144, RULE_aliasName);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(927);
                identifierName();
                setState(930);
                _errHandler.sync(this);
                _la = _input.LA(1);
                if (_la == As) {
                    {
                        setState(928);
                        match(As);
                        setState(929);
                        identifierName();
                    }
                }

            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class ExportStatementContext extends ParserRuleContext {
        public ExportStatementContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_exportStatement;
        }

        public ExportStatementContext() {}

        public void copyFrom(ExportStatementContext ctx) {
            super.copyFrom(ctx);
        }
    }
    @SuppressWarnings("CheckReturnValue")
    public static class ExportDefaultDeclarationContext extends ExportStatementContext {
        public TerminalNode Export() {
            return getToken(TypeScriptParser.Export, 0);
        }

        public TerminalNode Default() {
            return getToken(TypeScriptParser.Default, 0);
        }

        public SingleExpressionContext singleExpression() {
            return getRuleContext(SingleExpressionContext.class, 0);
        }

        public EosContext eos() {
            return getRuleContext(EosContext.class, 0);
        }

        public ExportDefaultDeclarationContext(ExportStatementContext ctx) {
            copyFrom(ctx);
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterExportDefaultDeclaration(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitExportDefaultDeclaration(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitExportDefaultDeclaration(this);
            else
                return visitor.visitChildren(this);
        }
    }
    @SuppressWarnings("CheckReturnValue")
    public static class ExportDeclarationContext extends ExportStatementContext {
        public TerminalNode Export() {
            return getToken(TypeScriptParser.Export, 0);
        }

        public EosContext eos() {
            return getRuleContext(EosContext.class, 0);
        }

        public ExportFromBlockContext exportFromBlock() {
            return getRuleContext(ExportFromBlockContext.class, 0);
        }

        public DeclarationContext declaration() {
            return getRuleContext(DeclarationContext.class, 0);
        }

        public TerminalNode Default() {
            return getToken(TypeScriptParser.Default, 0);
        }

        public ExportDeclarationContext(ExportStatementContext ctx) {
            copyFrom(ctx);
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterExportDeclaration(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitExportDeclaration(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitExportDeclaration(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final ExportStatementContext exportStatement() throws RecognitionException {
        ExportStatementContext _localctx = new ExportStatementContext(_ctx, getState());
        enterRule(_localctx, 146, RULE_exportStatement);
        try {
            setState(947);
            _errHandler.sync(this);
            switch (getInterpreter().adaptivePredict(_input, 102, _ctx)) {
                case 1:
                    _localctx = new ExportDeclarationContext(_localctx);
                    enterOuterAlt(_localctx, 1); {
                    setState(932);
                    match(Export);
                    setState(934);
                    _errHandler.sync(this);
                    switch (getInterpreter().adaptivePredict(_input, 100, _ctx)) {
                        case 1: {
                            setState(933);
                            match(Default);
                        }
                            break;
                    }
                    setState(938);
                    _errHandler.sync(this);
                    switch (getInterpreter().adaptivePredict(_input, 101, _ctx)) {
                        case 1: {
                            setState(936);
                            exportFromBlock();
                        }
                            break;
                        case 2: {
                            setState(937);
                            declaration();
                        }
                            break;
                    }
                    setState(940);
                    eos();
                }
                    break;
                case 2:
                    _localctx = new ExportDefaultDeclarationContext(_localctx);
                    enterOuterAlt(_localctx, 2); {
                    setState(942);
                    match(Export);
                    setState(943);
                    match(Default);
                    setState(944);
                    singleExpression(0);
                    setState(945);
                    eos();
                }
                    break;
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class ExportFromBlockContext extends ParserRuleContext {
        public ImportNamespaceContext importNamespace() {
            return getRuleContext(ImportNamespaceContext.class, 0);
        }

        public ImportFromContext importFrom() {
            return getRuleContext(ImportFromContext.class, 0);
        }

        public EosContext eos() {
            return getRuleContext(EosContext.class, 0);
        }

        public ExportModuleItemsContext exportModuleItems() {
            return getRuleContext(ExportModuleItemsContext.class, 0);
        }

        public ExportFromBlockContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_exportFromBlock;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterExportFromBlock(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitExportFromBlock(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitExportFromBlock(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final ExportFromBlockContext exportFromBlock() throws RecognitionException {
        ExportFromBlockContext _localctx = new ExportFromBlockContext(_ctx, getState());
        enterRule(_localctx, 148, RULE_exportFromBlock);
        try {
            setState(959);
            _errHandler.sync(this);
            switch (_input.LA(1)) {
                case Multiply:
                case NullLiteral:
                case BooleanLiteral:
                case Break:
                case Do:
                case Instanceof:
                case Typeof:
                case Case:
                case Else:
                case New:
                case Var:
                case Catch:
                case Finally:
                case Return:
                case Void:
                case Continue:
                case For:
                case Switch:
                case While:
                case Debugger:
                case Function_:
                case This:
                case With:
                case Default:
                case If:
                case Throw:
                case Delete:
                case In:
                case Try:
                case As:
                case From:
                case ReadOnly:
                case Async:
                case Await:
                case Yield:
                case Class:
                case Enum:
                case Extends:
                case Super:
                case Const:
                case Export:
                case Import:
                case Implements:
                case Let:
                case Private:
                case Public:
                case Interface:
                case Package:
                case Protected:
                case Static:
                case Any:
                case Number:
                case Never:
                case Boolean:
                case String:
                case Unique:
                case Symbol:
                case Undefined:
                case Object:
                case Of:
                case KeyOf:
                case TypeAlias:
                case Constructor:
                case Namespace:
                case Require:
                case Module:
                case Abstract:
                case Identifier:
                    enterOuterAlt(_localctx, 1); {
                    setState(949);
                    importNamespace();
                    setState(950);
                    importFrom();
                    setState(951);
                    eos();
                }
                    break;
                case OpenBrace:
                    enterOuterAlt(_localctx, 2); {
                    setState(953);
                    exportModuleItems();
                    setState(955);
                    _errHandler.sync(this);
                    switch (getInterpreter().adaptivePredict(_input, 103, _ctx)) {
                        case 1: {
                            setState(954);
                            importFrom();
                        }
                            break;
                    }
                    setState(957);
                    eos();
                }
                    break;
                default:
                    throw new NoViableAltException(this);
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class ExportModuleItemsContext extends ParserRuleContext {
        public TerminalNode OpenBrace() {
            return getToken(TypeScriptParser.OpenBrace, 0);
        }

        public TerminalNode CloseBrace() {
            return getToken(TypeScriptParser.CloseBrace, 0);
        }

        public List<ExportAliasNameContext> exportAliasName() {
            return getRuleContexts(ExportAliasNameContext.class);
        }

        public ExportAliasNameContext exportAliasName(int i) {
            return getRuleContext(ExportAliasNameContext.class, i);
        }

        public List<TerminalNode> Comma() {
            return getTokens(TypeScriptParser.Comma);
        }

        public TerminalNode Comma(int i) {
            return getToken(TypeScriptParser.Comma, i);
        }

        public ExportModuleItemsContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_exportModuleItems;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterExportModuleItems(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitExportModuleItems(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitExportModuleItems(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final ExportModuleItemsContext exportModuleItems() throws RecognitionException {
        ExportModuleItemsContext _localctx = new ExportModuleItemsContext(_ctx, getState());
        enterRule(_localctx, 150, RULE_exportModuleItems);
        int _la;
        try {
            int _alt;
            enterOuterAlt(_localctx, 1);
            {
                setState(961);
                match(OpenBrace);
                setState(967);
                _errHandler.sync(this);
                _alt = getInterpreter().adaptivePredict(_input, 105, _ctx);
                while (_alt != 2 && _alt != org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER) {
                    if (_alt == 1) {
                        {
                            {
                                setState(962);
                                exportAliasName();
                                setState(963);
                                match(Comma);
                            }
                        }
                    }
                    setState(969);
                    _errHandler.sync(this);
                    _alt = getInterpreter().adaptivePredict(_input, 105, _ctx);
                }
                setState(974);
                _errHandler.sync(this);
                _la = _input.LA(1);
                if (((((_la - 59)) & ~0x3f) == 0 && ((1L << (_la - 59)) & -8796093024253L) != 0)
                        || ((((_la - 123)) & ~0x3f) == 0 && ((1L << (_la - 123)) & 104447L) != 0)) {
                    {
                        setState(970);
                        exportAliasName();
                        setState(972);
                        _errHandler.sync(this);
                        _la = _input.LA(1);
                        if (_la == Comma) {
                            {
                                setState(971);
                                match(Comma);
                            }
                        }

                    }
                }

                setState(976);
                match(CloseBrace);
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class ExportAliasNameContext extends ParserRuleContext {
        public List<ModuleExportNameContext> moduleExportName() {
            return getRuleContexts(ModuleExportNameContext.class);
        }

        public ModuleExportNameContext moduleExportName(int i) {
            return getRuleContext(ModuleExportNameContext.class, i);
        }

        public TerminalNode As() {
            return getToken(TypeScriptParser.As, 0);
        }

        public ExportAliasNameContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_exportAliasName;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterExportAliasName(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitExportAliasName(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitExportAliasName(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final ExportAliasNameContext exportAliasName() throws RecognitionException {
        ExportAliasNameContext _localctx = new ExportAliasNameContext(_ctx, getState());
        enterRule(_localctx, 152, RULE_exportAliasName);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(978);
                moduleExportName();
                setState(981);
                _errHandler.sync(this);
                _la = _input.LA(1);
                if (_la == As) {
                    {
                        setState(979);
                        match(As);
                        setState(980);
                        moduleExportName();
                    }
                }

            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class DeclarationContext extends ParserRuleContext {
        public VariableStatementContext variableStatement() {
            return getRuleContext(VariableStatementContext.class, 0);
        }

        public ClassDeclarationContext classDeclaration() {
            return getRuleContext(ClassDeclarationContext.class, 0);
        }

        public FunctionDeclarationContext functionDeclaration() {
            return getRuleContext(FunctionDeclarationContext.class, 0);
        }

        public DeclarationContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_declaration;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterDeclaration(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitDeclaration(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitDeclaration(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final DeclarationContext declaration() throws RecognitionException {
        DeclarationContext _localctx = new DeclarationContext(_ctx, getState());
        enterRule(_localctx, 154, RULE_declaration);
        try {
            setState(986);
            _errHandler.sync(this);
            switch (getInterpreter().adaptivePredict(_input, 109, _ctx)) {
                case 1:
                    enterOuterAlt(_localctx, 1); {
                    setState(983);
                    variableStatement();
                }
                    break;
                case 2:
                    enterOuterAlt(_localctx, 2); {
                    setState(984);
                    classDeclaration();
                }
                    break;
                case 3:
                    enterOuterAlt(_localctx, 3); {
                    setState(985);
                    functionDeclaration();
                }
                    break;
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class VariableStatementContext extends ParserRuleContext {
        public BindingPatternContext bindingPattern() {
            return getRuleContext(BindingPatternContext.class, 0);
        }

        public InitializerContext initializer() {
            return getRuleContext(InitializerContext.class, 0);
        }

        public TypeAnnotationContext typeAnnotation() {
            return getRuleContext(TypeAnnotationContext.class, 0);
        }

        public TerminalNode SemiColon() {
            return getToken(TypeScriptParser.SemiColon, 0);
        }

        public VariableDeclarationListContext variableDeclarationList() {
            return getRuleContext(VariableDeclarationListContext.class, 0);
        }

        public AccessibilityModifierContext accessibilityModifier() {
            return getRuleContext(AccessibilityModifierContext.class, 0);
        }

        public VarModifierContext varModifier() {
            return getRuleContext(VarModifierContext.class, 0);
        }

        public TerminalNode ReadOnly() {
            return getToken(TypeScriptParser.ReadOnly, 0);
        }

        public TerminalNode Declare() {
            return getToken(TypeScriptParser.Declare, 0);
        }

        public VariableStatementContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_variableStatement;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterVariableStatement(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitVariableStatement(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitVariableStatement(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final VariableStatementContext variableStatement() throws RecognitionException {
        VariableStatementContext _localctx = new VariableStatementContext(_ctx, getState());
        enterRule(_localctx, 156, RULE_variableStatement);
        int _la;
        try {
            setState(1017);
            _errHandler.sync(this);
            switch (getInterpreter().adaptivePredict(_input, 118, _ctx)) {
                case 1:
                    enterOuterAlt(_localctx, 1); {
                    setState(988);
                    bindingPattern();
                    setState(990);
                    _errHandler.sync(this);
                    _la = _input.LA(1);
                    if (_la == Colon) {
                        {
                            setState(989);
                            typeAnnotation();
                        }
                    }

                    setState(992);
                    initializer();
                    setState(994);
                    _errHandler.sync(this);
                    switch (getInterpreter().adaptivePredict(_input, 111, _ctx)) {
                        case 1: {
                            setState(993);
                            match(SemiColon);
                        }
                            break;
                    }
                }
                    break;
                case 2:
                    enterOuterAlt(_localctx, 2); {
                    setState(997);
                    _errHandler.sync(this);
                    _la = _input.LA(1);
                    if (((((_la - 112)) & ~0x3f) == 0 && ((1L << (_la - 112)) & 19L) != 0)) {
                        {
                            setState(996);
                            accessibilityModifier();
                        }
                    }

                    setState(1000);
                    _errHandler.sync(this);
                    _la = _input.LA(1);
                    if (((((_la - 77)) & ~0x3f) == 0 && ((1L << (_la - 77)) & 18253611009L) != 0)) {
                        {
                            setState(999);
                            varModifier();
                        }
                    }

                    setState(1003);
                    _errHandler.sync(this);
                    _la = _input.LA(1);
                    if (_la == ReadOnly) {
                        {
                            setState(1002);
                            match(ReadOnly);
                        }
                    }

                    setState(1005);
                    variableDeclarationList();
                    setState(1007);
                    _errHandler.sync(this);
                    switch (getInterpreter().adaptivePredict(_input, 115, _ctx)) {
                        case 1: {
                            setState(1006);
                            match(SemiColon);
                        }
                            break;
                    }
                }
                    break;
                case 3:
                    enterOuterAlt(_localctx, 3); {
                    setState(1009);
                    match(Declare);
                    setState(1011);
                    _errHandler.sync(this);
                    _la = _input.LA(1);
                    if (((((_la - 77)) & ~0x3f) == 0 && ((1L << (_la - 77)) & 18253611009L) != 0)) {
                        {
                            setState(1010);
                            varModifier();
                        }
                    }

                    setState(1013);
                    variableDeclarationList();
                    setState(1015);
                    _errHandler.sync(this);
                    switch (getInterpreter().adaptivePredict(_input, 117, _ctx)) {
                        case 1: {
                            setState(1014);
                            match(SemiColon);
                        }
                            break;
                    }
                }
                    break;
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class VariableDeclarationListContext extends ParserRuleContext {
        public List<VariableDeclarationContext> variableDeclaration() {
            return getRuleContexts(VariableDeclarationContext.class);
        }

        public VariableDeclarationContext variableDeclaration(int i) {
            return getRuleContext(VariableDeclarationContext.class, i);
        }

        public List<TerminalNode> Comma() {
            return getTokens(TypeScriptParser.Comma);
        }

        public TerminalNode Comma(int i) {
            return getToken(TypeScriptParser.Comma, i);
        }

        public VariableDeclarationListContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_variableDeclarationList;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterVariableDeclarationList(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitVariableDeclarationList(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitVariableDeclarationList(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final VariableDeclarationListContext variableDeclarationList() throws RecognitionException {
        VariableDeclarationListContext _localctx = new VariableDeclarationListContext(_ctx, getState());
        enterRule(_localctx, 158, RULE_variableDeclarationList);
        try {
            int _alt;
            enterOuterAlt(_localctx, 1);
            {
                setState(1019);
                variableDeclaration();
                setState(1024);
                _errHandler.sync(this);
                _alt = getInterpreter().adaptivePredict(_input, 119, _ctx);
                while (_alt != 2 && _alt != org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER) {
                    if (_alt == 1) {
                        {
                            {
                                setState(1020);
                                match(Comma);
                                setState(1021);
                                variableDeclaration();
                            }
                        }
                    }
                    setState(1026);
                    _errHandler.sync(this);
                    _alt = getInterpreter().adaptivePredict(_input, 119, _ctx);
                }
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class VariableDeclarationContext extends ParserRuleContext {
        public IdentifierOrKeyWordContext identifierOrKeyWord() {
            return getRuleContext(IdentifierOrKeyWordContext.class, 0);
        }

        public ArrayLiteralContext arrayLiteral() {
            return getRuleContext(ArrayLiteralContext.class, 0);
        }

        public ObjectLiteralContext objectLiteral() {
            return getRuleContext(ObjectLiteralContext.class, 0);
        }

        public TypeAnnotationContext typeAnnotation() {
            return getRuleContext(TypeAnnotationContext.class, 0);
        }

        public List<SingleExpressionContext> singleExpression() {
            return getRuleContexts(SingleExpressionContext.class);
        }

        public SingleExpressionContext singleExpression(int i) {
            return getRuleContext(SingleExpressionContext.class, i);
        }

        public TerminalNode Assign() {
            return getToken(TypeScriptParser.Assign, 0);
        }

        public TypeParametersContext typeParameters() {
            return getRuleContext(TypeParametersContext.class, 0);
        }

        public VariableDeclarationContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_variableDeclaration;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterVariableDeclaration(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitVariableDeclaration(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitVariableDeclaration(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final VariableDeclarationContext variableDeclaration() throws RecognitionException {
        VariableDeclarationContext _localctx = new VariableDeclarationContext(_ctx, getState());
        enterRule(_localctx, 160, RULE_variableDeclaration);
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(1030);
                _errHandler.sync(this);
                switch (_input.LA(1)) {
                    case As:
                    case From:
                    case Async:
                    case Yield:
                    case Any:
                    case Number:
                    case Never:
                    case Boolean:
                    case String:
                    case Unique:
                    case Symbol:
                    case Undefined:
                    case Object:
                    case Of:
                    case KeyOf:
                    case TypeAlias:
                    case Constructor:
                    case Namespace:
                    case Require:
                    case Abstract:
                    case Identifier: {
                        setState(1027);
                        identifierOrKeyWord();
                    }
                        break;
                    case OpenBracket: {
                        setState(1028);
                        arrayLiteral();
                    }
                        break;
                    case OpenBrace: {
                        setState(1029);
                        objectLiteral();
                    }
                        break;
                    default:
                        throw new NoViableAltException(this);
                }
                setState(1033);
                _errHandler.sync(this);
                switch (getInterpreter().adaptivePredict(_input, 121, _ctx)) {
                    case 1: {
                        setState(1032);
                        typeAnnotation();
                    }
                        break;
                }
                setState(1036);
                _errHandler.sync(this);
                switch (getInterpreter().adaptivePredict(_input, 122, _ctx)) {
                    case 1: {
                        setState(1035);
                        singleExpression(0);
                    }
                        break;
                }
                setState(1043);
                _errHandler.sync(this);
                switch (getInterpreter().adaptivePredict(_input, 124, _ctx)) {
                    case 1: {
                        setState(1038);
                        match(Assign);
                        setState(1040);
                        _errHandler.sync(this);
                        switch (getInterpreter().adaptivePredict(_input, 123, _ctx)) {
                            case 1: {
                                setState(1039);
                                typeParameters();
                            }
                                break;
                        }
                        setState(1042);
                        singleExpression(0);
                    }
                        break;
                }
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class EmptyStatement_Context extends ParserRuleContext {
        public TerminalNode SemiColon() {
            return getToken(TypeScriptParser.SemiColon, 0);
        }

        public EmptyStatement_Context(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_emptyStatement_;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterEmptyStatement_(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitEmptyStatement_(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitEmptyStatement_(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final EmptyStatement_Context emptyStatement_() throws RecognitionException {
        EmptyStatement_Context _localctx = new EmptyStatement_Context(_ctx, getState());
        enterRule(_localctx, 162, RULE_emptyStatement_);
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(1045);
                match(SemiColon);
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class ExpressionStatementContext extends ParserRuleContext {
        public ExpressionSequenceContext expressionSequence() {
            return getRuleContext(ExpressionSequenceContext.class, 0);
        }

        public TerminalNode SemiColon() {
            return getToken(TypeScriptParser.SemiColon, 0);
        }

        public ExpressionStatementContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_expressionStatement;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterExpressionStatement(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitExpressionStatement(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitExpressionStatement(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final ExpressionStatementContext expressionStatement() throws RecognitionException {
        ExpressionStatementContext _localctx = new ExpressionStatementContext(_ctx, getState());
        enterRule(_localctx, 164, RULE_expressionStatement);
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(1047);
                if (!(this.notOpenBraceAndNotFunctionAndNotInterface()))
                    throw new FailedPredicateException(this, "this.notOpenBraceAndNotFunctionAndNotInterface()");
                setState(1048);
                expressionSequence();
                setState(1050);
                _errHandler.sync(this);
                switch (getInterpreter().adaptivePredict(_input, 125, _ctx)) {
                    case 1: {
                        setState(1049);
                        match(SemiColon);
                    }
                        break;
                }
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class IfStatementContext extends ParserRuleContext {
        public TerminalNode If() {
            return getToken(TypeScriptParser.If, 0);
        }

        public TerminalNode OpenParen() {
            return getToken(TypeScriptParser.OpenParen, 0);
        }

        public ExpressionSequenceContext expressionSequence() {
            return getRuleContext(ExpressionSequenceContext.class, 0);
        }

        public TerminalNode CloseParen() {
            return getToken(TypeScriptParser.CloseParen, 0);
        }

        public List<StatementContext> statement() {
            return getRuleContexts(StatementContext.class);
        }

        public StatementContext statement(int i) {
            return getRuleContext(StatementContext.class, i);
        }

        public TerminalNode Else() {
            return getToken(TypeScriptParser.Else, 0);
        }

        public IfStatementContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_ifStatement;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterIfStatement(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitIfStatement(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitIfStatement(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final IfStatementContext ifStatement() throws RecognitionException {
        IfStatementContext _localctx = new IfStatementContext(_ctx, getState());
        enterRule(_localctx, 166, RULE_ifStatement);
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(1052);
                match(If);
                setState(1053);
                match(OpenParen);
                setState(1054);
                expressionSequence();
                setState(1055);
                match(CloseParen);
                setState(1056);
                statement();
                setState(1059);
                _errHandler.sync(this);
                switch (getInterpreter().adaptivePredict(_input, 126, _ctx)) {
                    case 1: {
                        setState(1057);
                        match(Else);
                        setState(1058);
                        statement();
                    }
                        break;
                }
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class IterationStatementContext extends ParserRuleContext {
        public IterationStatementContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_iterationStatement;
        }

        public IterationStatementContext() {}

        public void copyFrom(IterationStatementContext ctx) {
            super.copyFrom(ctx);
        }
    }
    @SuppressWarnings("CheckReturnValue")
    public static class ForVarOfStatementContext extends IterationStatementContext {
        public TerminalNode For() {
            return getToken(TypeScriptParser.For, 0);
        }

        public TerminalNode OpenParen() {
            return getToken(TypeScriptParser.OpenParen, 0);
        }

        public VarModifierContext varModifier() {
            return getRuleContext(VarModifierContext.class, 0);
        }

        public VariableDeclarationContext variableDeclaration() {
            return getRuleContext(VariableDeclarationContext.class, 0);
        }

        public IdentifierContext identifier() {
            return getRuleContext(IdentifierContext.class, 0);
        }

        public ExpressionSequenceContext expressionSequence() {
            return getRuleContext(ExpressionSequenceContext.class, 0);
        }

        public TerminalNode CloseParen() {
            return getToken(TypeScriptParser.CloseParen, 0);
        }

        public StatementContext statement() {
            return getRuleContext(StatementContext.class, 0);
        }

        public TerminalNode Await() {
            return getToken(TypeScriptParser.Await, 0);
        }

        public TerminalNode As() {
            return getToken(TypeScriptParser.As, 0);
        }

        public Type_Context type_() {
            return getRuleContext(Type_Context.class, 0);
        }

        public ForVarOfStatementContext(IterationStatementContext ctx) {
            copyFrom(ctx);
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterForVarOfStatement(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitForVarOfStatement(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitForVarOfStatement(this);
            else
                return visitor.visitChildren(this);
        }
    }
    @SuppressWarnings("CheckReturnValue")
    public static class DoStatementContext extends IterationStatementContext {
        public TerminalNode Do() {
            return getToken(TypeScriptParser.Do, 0);
        }

        public StatementContext statement() {
            return getRuleContext(StatementContext.class, 0);
        }

        public TerminalNode While() {
            return getToken(TypeScriptParser.While, 0);
        }

        public TerminalNode OpenParen() {
            return getToken(TypeScriptParser.OpenParen, 0);
        }

        public ExpressionSequenceContext expressionSequence() {
            return getRuleContext(ExpressionSequenceContext.class, 0);
        }

        public TerminalNode CloseParen() {
            return getToken(TypeScriptParser.CloseParen, 0);
        }

        public EosContext eos() {
            return getRuleContext(EosContext.class, 0);
        }

        public DoStatementContext(IterationStatementContext ctx) {
            copyFrom(ctx);
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterDoStatement(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitDoStatement(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitDoStatement(this);
            else
                return visitor.visitChildren(this);
        }
    }
    @SuppressWarnings("CheckReturnValue")
    public static class ForVarStatementContext extends IterationStatementContext {
        public TerminalNode For() {
            return getToken(TypeScriptParser.For, 0);
        }

        public TerminalNode OpenParen() {
            return getToken(TypeScriptParser.OpenParen, 0);
        }

        public VarModifierContext varModifier() {
            return getRuleContext(VarModifierContext.class, 0);
        }

        public VariableDeclarationListContext variableDeclarationList() {
            return getRuleContext(VariableDeclarationListContext.class, 0);
        }

        public List<TerminalNode> SemiColon() {
            return getTokens(TypeScriptParser.SemiColon);
        }

        public TerminalNode SemiColon(int i) {
            return getToken(TypeScriptParser.SemiColon, i);
        }

        public TerminalNode CloseParen() {
            return getToken(TypeScriptParser.CloseParen, 0);
        }

        public StatementContext statement() {
            return getRuleContext(StatementContext.class, 0);
        }

        public List<ExpressionSequenceContext> expressionSequence() {
            return getRuleContexts(ExpressionSequenceContext.class);
        }

        public ExpressionSequenceContext expressionSequence(int i) {
            return getRuleContext(ExpressionSequenceContext.class, i);
        }

        public ForVarStatementContext(IterationStatementContext ctx) {
            copyFrom(ctx);
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterForVarStatement(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitForVarStatement(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitForVarStatement(this);
            else
                return visitor.visitChildren(this);
        }
    }
    @SuppressWarnings("CheckReturnValue")
    public static class ForVarInStatementContext extends IterationStatementContext {
        public TerminalNode For() {
            return getToken(TypeScriptParser.For, 0);
        }

        public TerminalNode OpenParen() {
            return getToken(TypeScriptParser.OpenParen, 0);
        }

        public VarModifierContext varModifier() {
            return getRuleContext(VarModifierContext.class, 0);
        }

        public VariableDeclarationContext variableDeclaration() {
            return getRuleContext(VariableDeclarationContext.class, 0);
        }

        public TerminalNode In() {
            return getToken(TypeScriptParser.In, 0);
        }

        public ExpressionSequenceContext expressionSequence() {
            return getRuleContext(ExpressionSequenceContext.class, 0);
        }

        public TerminalNode CloseParen() {
            return getToken(TypeScriptParser.CloseParen, 0);
        }

        public StatementContext statement() {
            return getRuleContext(StatementContext.class, 0);
        }

        public ForVarInStatementContext(IterationStatementContext ctx) {
            copyFrom(ctx);
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterForVarInStatement(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitForVarInStatement(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitForVarInStatement(this);
            else
                return visitor.visitChildren(this);
        }
    }
    @SuppressWarnings("CheckReturnValue")
    public static class WhileStatementContext extends IterationStatementContext {
        public TerminalNode While() {
            return getToken(TypeScriptParser.While, 0);
        }

        public TerminalNode OpenParen() {
            return getToken(TypeScriptParser.OpenParen, 0);
        }

        public ExpressionSequenceContext expressionSequence() {
            return getRuleContext(ExpressionSequenceContext.class, 0);
        }

        public TerminalNode CloseParen() {
            return getToken(TypeScriptParser.CloseParen, 0);
        }

        public StatementContext statement() {
            return getRuleContext(StatementContext.class, 0);
        }

        public WhileStatementContext(IterationStatementContext ctx) {
            copyFrom(ctx);
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterWhileStatement(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitWhileStatement(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitWhileStatement(this);
            else
                return visitor.visitChildren(this);
        }
    }
    @SuppressWarnings("CheckReturnValue")
    public static class ForStatementContext extends IterationStatementContext {
        public TerminalNode For() {
            return getToken(TypeScriptParser.For, 0);
        }

        public TerminalNode OpenParen() {
            return getToken(TypeScriptParser.OpenParen, 0);
        }

        public List<TerminalNode> SemiColon() {
            return getTokens(TypeScriptParser.SemiColon);
        }

        public TerminalNode SemiColon(int i) {
            return getToken(TypeScriptParser.SemiColon, i);
        }

        public TerminalNode CloseParen() {
            return getToken(TypeScriptParser.CloseParen, 0);
        }

        public StatementContext statement() {
            return getRuleContext(StatementContext.class, 0);
        }

        public List<ExpressionSequenceContext> expressionSequence() {
            return getRuleContexts(ExpressionSequenceContext.class);
        }

        public ExpressionSequenceContext expressionSequence(int i) {
            return getRuleContext(ExpressionSequenceContext.class, i);
        }

        public ForStatementContext(IterationStatementContext ctx) {
            copyFrom(ctx);
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterForStatement(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitForStatement(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitForStatement(this);
            else
                return visitor.visitChildren(this);
        }
    }
    @SuppressWarnings("CheckReturnValue")
    public static class ForInStatementContext extends IterationStatementContext {
        public TerminalNode For() {
            return getToken(TypeScriptParser.For, 0);
        }

        public TerminalNode OpenParen() {
            return getToken(TypeScriptParser.OpenParen, 0);
        }

        public SingleExpressionContext singleExpression() {
            return getRuleContext(SingleExpressionContext.class, 0);
        }

        public TerminalNode In() {
            return getToken(TypeScriptParser.In, 0);
        }

        public ExpressionSequenceContext expressionSequence() {
            return getRuleContext(ExpressionSequenceContext.class, 0);
        }

        public TerminalNode CloseParen() {
            return getToken(TypeScriptParser.CloseParen, 0);
        }

        public StatementContext statement() {
            return getRuleContext(StatementContext.class, 0);
        }

        public ForInStatementContext(IterationStatementContext ctx) {
            copyFrom(ctx);
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterForInStatement(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitForInStatement(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitForInStatement(this);
            else
                return visitor.visitChildren(this);
        }
    }
    @SuppressWarnings("CheckReturnValue")
    public static class ForOfStatementContext extends IterationStatementContext {
        public TerminalNode For() {
            return getToken(TypeScriptParser.For, 0);
        }

        public TerminalNode OpenParen() {
            return getToken(TypeScriptParser.OpenParen, 0);
        }

        public SingleExpressionContext singleExpression() {
            return getRuleContext(SingleExpressionContext.class, 0);
        }

        public IdentifierContext identifier() {
            return getRuleContext(IdentifierContext.class, 0);
        }

        public ExpressionSequenceContext expressionSequence() {
            return getRuleContext(ExpressionSequenceContext.class, 0);
        }

        public TerminalNode CloseParen() {
            return getToken(TypeScriptParser.CloseParen, 0);
        }

        public StatementContext statement() {
            return getRuleContext(StatementContext.class, 0);
        }

        public TerminalNode Await() {
            return getToken(TypeScriptParser.Await, 0);
        }

        public TerminalNode As() {
            return getToken(TypeScriptParser.As, 0);
        }

        public Type_Context type_() {
            return getRuleContext(Type_Context.class, 0);
        }

        public ForOfStatementContext(IterationStatementContext ctx) {
            copyFrom(ctx);
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterForOfStatement(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitForOfStatement(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitForOfStatement(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final IterationStatementContext iterationStatement() throws RecognitionException {
        IterationStatementContext _localctx = new IterationStatementContext(_ctx, getState());
        enterRule(_localctx, 168, RULE_iterationStatement);
        int _la;
        try {
            setState(1155);
            _errHandler.sync(this);
            switch (getInterpreter().adaptivePredict(_input, 136, _ctx)) {
                case 1:
                    _localctx = new DoStatementContext(_localctx);
                    enterOuterAlt(_localctx, 1); {
                    setState(1061);
                    match(Do);
                    setState(1062);
                    statement();
                    setState(1063);
                    match(While);
                    setState(1064);
                    match(OpenParen);
                    setState(1065);
                    expressionSequence();
                    setState(1066);
                    match(CloseParen);
                    setState(1067);
                    eos();
                }
                    break;
                case 2:
                    _localctx = new WhileStatementContext(_localctx);
                    enterOuterAlt(_localctx, 2); {
                    setState(1069);
                    match(While);
                    setState(1070);
                    match(OpenParen);
                    setState(1071);
                    expressionSequence();
                    setState(1072);
                    match(CloseParen);
                    setState(1073);
                    statement();
                }
                    break;
                case 3:
                    _localctx = new ForStatementContext(_localctx);
                    enterOuterAlt(_localctx, 3); {
                    setState(1075);
                    match(For);
                    setState(1076);
                    match(OpenParen);
                    setState(1078);
                    _errHandler.sync(this);
                    _la = _input.LA(1);
                    if ((((_la) & ~0x3f) == 0 && ((1L << _la) & -576460747975425704L) != 0)
                            || ((((_la - 64)) & ~0x3f) == 0 && ((1L << (_la - 64)) & -1L) != 0)
                            || ((((_la - 128)) & ~0x3f) == 0 && ((1L << (_la - 128)) & 7359L) != 0)) {
                        {
                            setState(1077);
                            expressionSequence();
                        }
                    }

                    setState(1080);
                    match(SemiColon);
                    setState(1082);
                    _errHandler.sync(this);
                    _la = _input.LA(1);
                    if ((((_la) & ~0x3f) == 0 && ((1L << _la) & -576460747975425704L) != 0)
                            || ((((_la - 64)) & ~0x3f) == 0 && ((1L << (_la - 64)) & -1L) != 0)
                            || ((((_la - 128)) & ~0x3f) == 0 && ((1L << (_la - 128)) & 7359L) != 0)) {
                        {
                            setState(1081);
                            expressionSequence();
                        }
                    }

                    setState(1084);
                    match(SemiColon);
                    setState(1086);
                    _errHandler.sync(this);
                    _la = _input.LA(1);
                    if ((((_la) & ~0x3f) == 0 && ((1L << _la) & -576460747975425704L) != 0)
                            || ((((_la - 64)) & ~0x3f) == 0 && ((1L << (_la - 64)) & -1L) != 0)
                            || ((((_la - 128)) & ~0x3f) == 0 && ((1L << (_la - 128)) & 7359L) != 0)) {
                        {
                            setState(1085);
                            expressionSequence();
                        }
                    }

                    setState(1088);
                    match(CloseParen);
                    setState(1089);
                    statement();
                }
                    break;
                case 4:
                    _localctx = new ForVarStatementContext(_localctx);
                    enterOuterAlt(_localctx, 4); {
                    setState(1090);
                    match(For);
                    setState(1091);
                    match(OpenParen);
                    setState(1092);
                    varModifier();
                    setState(1093);
                    variableDeclarationList();
                    setState(1094);
                    match(SemiColon);
                    setState(1096);
                    _errHandler.sync(this);
                    _la = _input.LA(1);
                    if ((((_la) & ~0x3f) == 0 && ((1L << _la) & -576460747975425704L) != 0)
                            || ((((_la - 64)) & ~0x3f) == 0 && ((1L << (_la - 64)) & -1L) != 0)
                            || ((((_la - 128)) & ~0x3f) == 0 && ((1L << (_la - 128)) & 7359L) != 0)) {
                        {
                            setState(1095);
                            expressionSequence();
                        }
                    }

                    setState(1098);
                    match(SemiColon);
                    setState(1100);
                    _errHandler.sync(this);
                    _la = _input.LA(1);
                    if ((((_la) & ~0x3f) == 0 && ((1L << _la) & -576460747975425704L) != 0)
                            || ((((_la - 64)) & ~0x3f) == 0 && ((1L << (_la - 64)) & -1L) != 0)
                            || ((((_la - 128)) & ~0x3f) == 0 && ((1L << (_la - 128)) & 7359L) != 0)) {
                        {
                            setState(1099);
                            expressionSequence();
                        }
                    }

                    setState(1102);
                    match(CloseParen);
                    setState(1103);
                    statement();
                }
                    break;
                case 5:
                    _localctx = new ForInStatementContext(_localctx);
                    enterOuterAlt(_localctx, 5); {
                    setState(1105);
                    match(For);
                    setState(1106);
                    match(OpenParen);
                    setState(1107);
                    singleExpression(0);
                    setState(1108);
                    match(In);
                    setState(1109);
                    expressionSequence();
                    setState(1110);
                    match(CloseParen);
                    setState(1111);
                    statement();
                }
                    break;
                case 6:
                    _localctx = new ForVarInStatementContext(_localctx);
                    enterOuterAlt(_localctx, 6); {
                    setState(1113);
                    match(For);
                    setState(1114);
                    match(OpenParen);
                    setState(1115);
                    varModifier();
                    setState(1116);
                    variableDeclaration();
                    setState(1117);
                    match(In);
                    setState(1118);
                    expressionSequence();
                    setState(1119);
                    match(CloseParen);
                    setState(1120);
                    statement();
                }
                    break;
                case 7:
                    _localctx = new ForOfStatementContext(_localctx);
                    enterOuterAlt(_localctx, 7); {
                    setState(1122);
                    match(For);
                    setState(1124);
                    _errHandler.sync(this);
                    _la = _input.LA(1);
                    if (_la == Await) {
                        {
                            setState(1123);
                            match(Await);
                        }
                    }

                    setState(1126);
                    match(OpenParen);
                    setState(1127);
                    singleExpression(0);
                    setState(1128);
                    identifier();
                    setState(1129);
                    if (!(this.p("of")))
                        throw new FailedPredicateException(this, "this.p(\"of\")");
                    setState(1130);
                    expressionSequence();
                    setState(1133);
                    _errHandler.sync(this);
                    _la = _input.LA(1);
                    if (_la == As) {
                        {
                            setState(1131);
                            match(As);
                            setState(1132);
                            type_();
                        }
                    }

                    setState(1135);
                    match(CloseParen);
                    setState(1136);
                    statement();
                }
                    break;
                case 8:
                    _localctx = new ForVarOfStatementContext(_localctx);
                    enterOuterAlt(_localctx, 8); {
                    setState(1138);
                    match(For);
                    setState(1140);
                    _errHandler.sync(this);
                    _la = _input.LA(1);
                    if (_la == Await) {
                        {
                            setState(1139);
                            match(Await);
                        }
                    }

                    setState(1142);
                    match(OpenParen);
                    setState(1143);
                    varModifier();
                    setState(1144);
                    variableDeclaration();
                    setState(1145);
                    identifier();
                    setState(1146);
                    if (!(this.p("of")))
                        throw new FailedPredicateException(this, "this.p(\"of\")");
                    setState(1147);
                    expressionSequence();
                    setState(1150);
                    _errHandler.sync(this);
                    _la = _input.LA(1);
                    if (_la == As) {
                        {
                            setState(1148);
                            match(As);
                            setState(1149);
                            type_();
                        }
                    }

                    setState(1152);
                    match(CloseParen);
                    setState(1153);
                    statement();
                }
                    break;
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class VarModifierContext extends ParserRuleContext {
        public TerminalNode Var() {
            return getToken(TypeScriptParser.Var, 0);
        }

        public TerminalNode Let() {
            return getToken(TypeScriptParser.Let, 0);
        }

        public TerminalNode Const() {
            return getToken(TypeScriptParser.Const, 0);
        }

        public VarModifierContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_varModifier;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterVarModifier(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitVarModifier(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitVarModifier(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final VarModifierContext varModifier() throws RecognitionException {
        VarModifierContext _localctx = new VarModifierContext(_ctx, getState());
        enterRule(_localctx, 170, RULE_varModifier);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(1157);
                _la = _input.LA(1);
                if (!(((((_la - 77)) & ~0x3f) == 0 && ((1L << (_la - 77)) & 18253611009L) != 0))) {
                    _errHandler.recoverInline(this);
                } else {
                    if (_input.LA(1) == Token.EOF)
                        matchedEOF = true;
                    _errHandler.reportMatch(this);
                    consume();
                }
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class ContinueStatementContext extends ParserRuleContext {
        public TerminalNode Continue() {
            return getToken(TypeScriptParser.Continue, 0);
        }

        public EosContext eos() {
            return getRuleContext(EosContext.class, 0);
        }

        public IdentifierContext identifier() {
            return getRuleContext(IdentifierContext.class, 0);
        }

        public ContinueStatementContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_continueStatement;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterContinueStatement(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitContinueStatement(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitContinueStatement(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final ContinueStatementContext continueStatement() throws RecognitionException {
        ContinueStatementContext _localctx = new ContinueStatementContext(_ctx, getState());
        enterRule(_localctx, 172, RULE_continueStatement);
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(1159);
                match(Continue);
                setState(1162);
                _errHandler.sync(this);
                switch (getInterpreter().adaptivePredict(_input, 137, _ctx)) {
                    case 1: {
                        setState(1160);
                        if (!(this.notLineTerminator()))
                            throw new FailedPredicateException(this, "this.notLineTerminator()");
                        setState(1161);
                        identifier();
                    }
                        break;
                }
                setState(1164);
                eos();
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class BreakStatementContext extends ParserRuleContext {
        public TerminalNode Break() {
            return getToken(TypeScriptParser.Break, 0);
        }

        public EosContext eos() {
            return getRuleContext(EosContext.class, 0);
        }

        public IdentifierContext identifier() {
            return getRuleContext(IdentifierContext.class, 0);
        }

        public BreakStatementContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_breakStatement;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterBreakStatement(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitBreakStatement(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitBreakStatement(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final BreakStatementContext breakStatement() throws RecognitionException {
        BreakStatementContext _localctx = new BreakStatementContext(_ctx, getState());
        enterRule(_localctx, 174, RULE_breakStatement);
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(1166);
                match(Break);
                setState(1169);
                _errHandler.sync(this);
                switch (getInterpreter().adaptivePredict(_input, 138, _ctx)) {
                    case 1: {
                        setState(1167);
                        if (!(this.notLineTerminator()))
                            throw new FailedPredicateException(this, "this.notLineTerminator()");
                        setState(1168);
                        identifier();
                    }
                        break;
                }
                setState(1171);
                eos();
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class ReturnStatementContext extends ParserRuleContext {
        public TerminalNode Return() {
            return getToken(TypeScriptParser.Return, 0);
        }

        public EosContext eos() {
            return getRuleContext(EosContext.class, 0);
        }

        public ExpressionSequenceContext expressionSequence() {
            return getRuleContext(ExpressionSequenceContext.class, 0);
        }

        public ReturnStatementContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_returnStatement;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterReturnStatement(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitReturnStatement(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitReturnStatement(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final ReturnStatementContext returnStatement() throws RecognitionException {
        ReturnStatementContext _localctx = new ReturnStatementContext(_ctx, getState());
        enterRule(_localctx, 176, RULE_returnStatement);
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(1173);
                match(Return);
                setState(1176);
                _errHandler.sync(this);
                switch (getInterpreter().adaptivePredict(_input, 139, _ctx)) {
                    case 1: {
                        setState(1174);
                        if (!(this.notLineTerminator()))
                            throw new FailedPredicateException(this, "this.notLineTerminator()");
                        setState(1175);
                        expressionSequence();
                    }
                        break;
                }
                setState(1178);
                eos();
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class YieldStatementContext extends ParserRuleContext {
        public EosContext eos() {
            return getRuleContext(EosContext.class, 0);
        }

        public TerminalNode Yield() {
            return getToken(TypeScriptParser.Yield, 0);
        }

        public TerminalNode YieldStar() {
            return getToken(TypeScriptParser.YieldStar, 0);
        }

        public ExpressionSequenceContext expressionSequence() {
            return getRuleContext(ExpressionSequenceContext.class, 0);
        }

        public YieldStatementContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_yieldStatement;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterYieldStatement(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitYieldStatement(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitYieldStatement(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final YieldStatementContext yieldStatement() throws RecognitionException {
        YieldStatementContext _localctx = new YieldStatementContext(_ctx, getState());
        enterRule(_localctx, 178, RULE_yieldStatement);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(1180);
                _la = _input.LA(1);
                if (!(_la == Yield || _la == YieldStar)) {
                    _errHandler.recoverInline(this);
                } else {
                    if (_input.LA(1) == Token.EOF)
                        matchedEOF = true;
                    _errHandler.reportMatch(this);
                    consume();
                }
                setState(1183);
                _errHandler.sync(this);
                switch (getInterpreter().adaptivePredict(_input, 140, _ctx)) {
                    case 1: {
                        setState(1181);
                        if (!(this.notLineTerminator()))
                            throw new FailedPredicateException(this, "this.notLineTerminator()");
                        setState(1182);
                        expressionSequence();
                    }
                        break;
                }
                setState(1185);
                eos();
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class WithStatementContext extends ParserRuleContext {
        public TerminalNode With() {
            return getToken(TypeScriptParser.With, 0);
        }

        public TerminalNode OpenParen() {
            return getToken(TypeScriptParser.OpenParen, 0);
        }

        public ExpressionSequenceContext expressionSequence() {
            return getRuleContext(ExpressionSequenceContext.class, 0);
        }

        public TerminalNode CloseParen() {
            return getToken(TypeScriptParser.CloseParen, 0);
        }

        public StatementContext statement() {
            return getRuleContext(StatementContext.class, 0);
        }

        public WithStatementContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_withStatement;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterWithStatement(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitWithStatement(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitWithStatement(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final WithStatementContext withStatement() throws RecognitionException {
        WithStatementContext _localctx = new WithStatementContext(_ctx, getState());
        enterRule(_localctx, 180, RULE_withStatement);
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(1187);
                match(With);
                setState(1188);
                match(OpenParen);
                setState(1189);
                expressionSequence();
                setState(1190);
                match(CloseParen);
                setState(1191);
                statement();
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class SwitchStatementContext extends ParserRuleContext {
        public TerminalNode Switch() {
            return getToken(TypeScriptParser.Switch, 0);
        }

        public TerminalNode OpenParen() {
            return getToken(TypeScriptParser.OpenParen, 0);
        }

        public ExpressionSequenceContext expressionSequence() {
            return getRuleContext(ExpressionSequenceContext.class, 0);
        }

        public TerminalNode CloseParen() {
            return getToken(TypeScriptParser.CloseParen, 0);
        }

        public CaseBlockContext caseBlock() {
            return getRuleContext(CaseBlockContext.class, 0);
        }

        public SwitchStatementContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_switchStatement;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterSwitchStatement(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitSwitchStatement(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitSwitchStatement(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final SwitchStatementContext switchStatement() throws RecognitionException {
        SwitchStatementContext _localctx = new SwitchStatementContext(_ctx, getState());
        enterRule(_localctx, 182, RULE_switchStatement);
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(1193);
                match(Switch);
                setState(1194);
                match(OpenParen);
                setState(1195);
                expressionSequence();
                setState(1196);
                match(CloseParen);
                setState(1197);
                caseBlock();
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class CaseBlockContext extends ParserRuleContext {
        public TerminalNode OpenBrace() {
            return getToken(TypeScriptParser.OpenBrace, 0);
        }

        public TerminalNode CloseBrace() {
            return getToken(TypeScriptParser.CloseBrace, 0);
        }

        public List<CaseClausesContext> caseClauses() {
            return getRuleContexts(CaseClausesContext.class);
        }

        public CaseClausesContext caseClauses(int i) {
            return getRuleContext(CaseClausesContext.class, i);
        }

        public DefaultClauseContext defaultClause() {
            return getRuleContext(DefaultClauseContext.class, 0);
        }

        public CaseBlockContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_caseBlock;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterCaseBlock(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitCaseBlock(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitCaseBlock(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final CaseBlockContext caseBlock() throws RecognitionException {
        CaseBlockContext _localctx = new CaseBlockContext(_ctx, getState());
        enterRule(_localctx, 184, RULE_caseBlock);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(1199);
                match(OpenBrace);
                setState(1201);
                _errHandler.sync(this);
                _la = _input.LA(1);
                if (_la == Case) {
                    {
                        setState(1200);
                        caseClauses();
                    }
                }

                setState(1207);
                _errHandler.sync(this);
                _la = _input.LA(1);
                if (_la == Default) {
                    {
                        setState(1203);
                        defaultClause();
                        setState(1205);
                        _errHandler.sync(this);
                        _la = _input.LA(1);
                        if (_la == Case) {
                            {
                                setState(1204);
                                caseClauses();
                            }
                        }

                    }
                }

                setState(1209);
                match(CloseBrace);
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class CaseClausesContext extends ParserRuleContext {
        public List<CaseClauseContext> caseClause() {
            return getRuleContexts(CaseClauseContext.class);
        }

        public CaseClauseContext caseClause(int i) {
            return getRuleContext(CaseClauseContext.class, i);
        }

        public CaseClausesContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_caseClauses;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterCaseClauses(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitCaseClauses(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitCaseClauses(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final CaseClausesContext caseClauses() throws RecognitionException {
        CaseClausesContext _localctx = new CaseClausesContext(_ctx, getState());
        enterRule(_localctx, 186, RULE_caseClauses);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(1212);
                _errHandler.sync(this);
                _la = _input.LA(1);
                do {
                    {
                        {
                            setState(1211);
                            caseClause();
                        }
                    }
                    setState(1214);
                    _errHandler.sync(this);
                    _la = _input.LA(1);
                } while (_la == Case);
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class CaseClauseContext extends ParserRuleContext {
        public TerminalNode Case() {
            return getToken(TypeScriptParser.Case, 0);
        }

        public ExpressionSequenceContext expressionSequence() {
            return getRuleContext(ExpressionSequenceContext.class, 0);
        }

        public TerminalNode Colon() {
            return getToken(TypeScriptParser.Colon, 0);
        }

        public StatementListContext statementList() {
            return getRuleContext(StatementListContext.class, 0);
        }

        public CaseClauseContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_caseClause;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterCaseClause(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitCaseClause(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitCaseClause(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final CaseClauseContext caseClause() throws RecognitionException {
        CaseClauseContext _localctx = new CaseClauseContext(_ctx, getState());
        enterRule(_localctx, 188, RULE_caseClause);
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(1216);
                match(Case);
                setState(1217);
                expressionSequence();
                setState(1218);
                match(Colon);
                setState(1220);
                _errHandler.sync(this);
                switch (getInterpreter().adaptivePredict(_input, 145, _ctx)) {
                    case 1: {
                        setState(1219);
                        statementList();
                    }
                        break;
                }
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class DefaultClauseContext extends ParserRuleContext {
        public TerminalNode Default() {
            return getToken(TypeScriptParser.Default, 0);
        }

        public TerminalNode Colon() {
            return getToken(TypeScriptParser.Colon, 0);
        }

        public StatementListContext statementList() {
            return getRuleContext(StatementListContext.class, 0);
        }

        public DefaultClauseContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_defaultClause;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterDefaultClause(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitDefaultClause(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitDefaultClause(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final DefaultClauseContext defaultClause() throws RecognitionException {
        DefaultClauseContext _localctx = new DefaultClauseContext(_ctx, getState());
        enterRule(_localctx, 190, RULE_defaultClause);
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(1222);
                match(Default);
                setState(1223);
                match(Colon);
                setState(1225);
                _errHandler.sync(this);
                switch (getInterpreter().adaptivePredict(_input, 146, _ctx)) {
                    case 1: {
                        setState(1224);
                        statementList();
                    }
                        break;
                }
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class LabelledStatementContext extends ParserRuleContext {
        public IdentifierContext identifier() {
            return getRuleContext(IdentifierContext.class, 0);
        }

        public TerminalNode Colon() {
            return getToken(TypeScriptParser.Colon, 0);
        }

        public StatementContext statement() {
            return getRuleContext(StatementContext.class, 0);
        }

        public LabelledStatementContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_labelledStatement;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterLabelledStatement(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitLabelledStatement(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitLabelledStatement(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final LabelledStatementContext labelledStatement() throws RecognitionException {
        LabelledStatementContext _localctx = new LabelledStatementContext(_ctx, getState());
        enterRule(_localctx, 192, RULE_labelledStatement);
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(1227);
                identifier();
                setState(1228);
                match(Colon);
                setState(1229);
                statement();
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class ThrowStatementContext extends ParserRuleContext {
        public TerminalNode Throw() {
            return getToken(TypeScriptParser.Throw, 0);
        }

        public ExpressionSequenceContext expressionSequence() {
            return getRuleContext(ExpressionSequenceContext.class, 0);
        }

        public EosContext eos() {
            return getRuleContext(EosContext.class, 0);
        }

        public ThrowStatementContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_throwStatement;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterThrowStatement(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitThrowStatement(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitThrowStatement(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final ThrowStatementContext throwStatement() throws RecognitionException {
        ThrowStatementContext _localctx = new ThrowStatementContext(_ctx, getState());
        enterRule(_localctx, 194, RULE_throwStatement);
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(1231);
                match(Throw);
                setState(1232);
                if (!(this.notLineTerminator()))
                    throw new FailedPredicateException(this, "this.notLineTerminator()");
                setState(1233);
                expressionSequence();
                setState(1234);
                eos();
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class TryStatementContext extends ParserRuleContext {
        public TerminalNode Try() {
            return getToken(TypeScriptParser.Try, 0);
        }

        public BlockContext block() {
            return getRuleContext(BlockContext.class, 0);
        }

        public CatchProductionContext catchProduction() {
            return getRuleContext(CatchProductionContext.class, 0);
        }

        public FinallyProductionContext finallyProduction() {
            return getRuleContext(FinallyProductionContext.class, 0);
        }

        public TryStatementContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_tryStatement;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterTryStatement(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitTryStatement(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitTryStatement(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final TryStatementContext tryStatement() throws RecognitionException {
        TryStatementContext _localctx = new TryStatementContext(_ctx, getState());
        enterRule(_localctx, 196, RULE_tryStatement);
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(1236);
                match(Try);
                setState(1237);
                block();
                setState(1243);
                _errHandler.sync(this);
                switch (_input.LA(1)) {
                    case Catch: {
                        setState(1238);
                        catchProduction();
                        setState(1240);
                        _errHandler.sync(this);
                        switch (getInterpreter().adaptivePredict(_input, 147, _ctx)) {
                            case 1: {
                                setState(1239);
                                finallyProduction();
                            }
                                break;
                        }
                    }
                        break;
                    case Finally: {
                        setState(1242);
                        finallyProduction();
                    }
                        break;
                    default:
                        throw new NoViableAltException(this);
                }
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class CatchProductionContext extends ParserRuleContext {
        public TerminalNode Catch() {
            return getToken(TypeScriptParser.Catch, 0);
        }

        public BlockContext block() {
            return getRuleContext(BlockContext.class, 0);
        }

        public TerminalNode OpenParen() {
            return getToken(TypeScriptParser.OpenParen, 0);
        }

        public IdentifierContext identifier() {
            return getRuleContext(IdentifierContext.class, 0);
        }

        public TerminalNode CloseParen() {
            return getToken(TypeScriptParser.CloseParen, 0);
        }

        public TypeAnnotationContext typeAnnotation() {
            return getRuleContext(TypeAnnotationContext.class, 0);
        }

        public CatchProductionContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_catchProduction;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterCatchProduction(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitCatchProduction(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitCatchProduction(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final CatchProductionContext catchProduction() throws RecognitionException {
        CatchProductionContext _localctx = new CatchProductionContext(_ctx, getState());
        enterRule(_localctx, 198, RULE_catchProduction);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(1245);
                match(Catch);
                setState(1253);
                _errHandler.sync(this);
                _la = _input.LA(1);
                if (_la == OpenParen) {
                    {
                        setState(1246);
                        match(OpenParen);
                        setState(1247);
                        identifier();
                        setState(1249);
                        _errHandler.sync(this);
                        _la = _input.LA(1);
                        if (_la == Colon) {
                            {
                                setState(1248);
                                typeAnnotation();
                            }
                        }

                        setState(1251);
                        match(CloseParen);
                    }
                }

                setState(1255);
                block();
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class FinallyProductionContext extends ParserRuleContext {
        public TerminalNode Finally() {
            return getToken(TypeScriptParser.Finally, 0);
        }

        public BlockContext block() {
            return getRuleContext(BlockContext.class, 0);
        }

        public FinallyProductionContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_finallyProduction;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterFinallyProduction(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitFinallyProduction(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitFinallyProduction(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final FinallyProductionContext finallyProduction() throws RecognitionException {
        FinallyProductionContext _localctx = new FinallyProductionContext(_ctx, getState());
        enterRule(_localctx, 200, RULE_finallyProduction);
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(1257);
                match(Finally);
                setState(1258);
                block();
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class DebuggerStatementContext extends ParserRuleContext {
        public TerminalNode Debugger() {
            return getToken(TypeScriptParser.Debugger, 0);
        }

        public EosContext eos() {
            return getRuleContext(EosContext.class, 0);
        }

        public DebuggerStatementContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_debuggerStatement;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterDebuggerStatement(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitDebuggerStatement(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitDebuggerStatement(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final DebuggerStatementContext debuggerStatement() throws RecognitionException {
        DebuggerStatementContext _localctx = new DebuggerStatementContext(_ctx, getState());
        enterRule(_localctx, 202, RULE_debuggerStatement);
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(1260);
                match(Debugger);
                setState(1261);
                eos();
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class FunctionDeclarationContext extends ParserRuleContext {
        public TerminalNode Function_() {
            return getToken(TypeScriptParser.Function_, 0);
        }

        public IdentifierContext identifier() {
            return getRuleContext(IdentifierContext.class, 0);
        }

        public CallSignatureContext callSignature() {
            return getRuleContext(CallSignatureContext.class, 0);
        }

        public TerminalNode SemiColon() {
            return getToken(TypeScriptParser.SemiColon, 0);
        }

        public TerminalNode Async() {
            return getToken(TypeScriptParser.Async, 0);
        }

        public TerminalNode Multiply() {
            return getToken(TypeScriptParser.Multiply, 0);
        }

        public TerminalNode OpenBrace() {
            return getToken(TypeScriptParser.OpenBrace, 0);
        }

        public FunctionBodyContext functionBody() {
            return getRuleContext(FunctionBodyContext.class, 0);
        }

        public TerminalNode CloseBrace() {
            return getToken(TypeScriptParser.CloseBrace, 0);
        }

        public FunctionDeclarationContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_functionDeclaration;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterFunctionDeclaration(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitFunctionDeclaration(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitFunctionDeclaration(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final FunctionDeclarationContext functionDeclaration() throws RecognitionException {
        FunctionDeclarationContext _localctx = new FunctionDeclarationContext(_ctx, getState());
        enterRule(_localctx, 204, RULE_functionDeclaration);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(1264);
                _errHandler.sync(this);
                _la = _input.LA(1);
                if (_la == Async) {
                    {
                        setState(1263);
                        match(Async);
                    }
                }

                setState(1266);
                match(Function_);
                setState(1268);
                _errHandler.sync(this);
                _la = _input.LA(1);
                if (_la == Multiply) {
                    {
                        setState(1267);
                        match(Multiply);
                    }
                }

                setState(1270);
                identifier();
                setState(1271);
                callSignature();
                setState(1277);
                _errHandler.sync(this);
                switch (_input.LA(1)) {
                    case OpenBrace: {
                        {
                            setState(1272);
                            match(OpenBrace);
                            setState(1273);
                            functionBody();
                            setState(1274);
                            match(CloseBrace);
                        }
                    }
                        break;
                    case SemiColon: {
                        setState(1276);
                        match(SemiColon);
                    }
                        break;
                    default:
                        throw new NoViableAltException(this);
                }
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class ClassDeclarationContext extends ParserRuleContext {
        public TerminalNode Class() {
            return getToken(TypeScriptParser.Class, 0);
        }

        public IdentifierContext identifier() {
            return getRuleContext(IdentifierContext.class, 0);
        }

        public ClassHeritageContext classHeritage() {
            return getRuleContext(ClassHeritageContext.class, 0);
        }

        public ClassTailContext classTail() {
            return getRuleContext(ClassTailContext.class, 0);
        }

        public DecoratorListContext decoratorList() {
            return getRuleContext(DecoratorListContext.class, 0);
        }

        public TerminalNode Export() {
            return getToken(TypeScriptParser.Export, 0);
        }

        public TerminalNode Abstract() {
            return getToken(TypeScriptParser.Abstract, 0);
        }

        public TypeParametersContext typeParameters() {
            return getRuleContext(TypeParametersContext.class, 0);
        }

        public TerminalNode Default() {
            return getToken(TypeScriptParser.Default, 0);
        }

        public ClassDeclarationContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_classDeclaration;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterClassDeclaration(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitClassDeclaration(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitClassDeclaration(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final ClassDeclarationContext classDeclaration() throws RecognitionException {
        ClassDeclarationContext _localctx = new ClassDeclarationContext(_ctx, getState());
        enterRule(_localctx, 206, RULE_classDeclaration);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(1280);
                _errHandler.sync(this);
                _la = _input.LA(1);
                if (_la == At) {
                    {
                        setState(1279);
                        decoratorList();
                    }
                }

                setState(1286);
                _errHandler.sync(this);
                _la = _input.LA(1);
                if (_la == Export) {
                    {
                        setState(1282);
                        match(Export);
                        setState(1284);
                        _errHandler.sync(this);
                        _la = _input.LA(1);
                        if (_la == Default) {
                            {
                                setState(1283);
                                match(Default);
                            }
                        }

                    }
                }

                setState(1289);
                _errHandler.sync(this);
                _la = _input.LA(1);
                if (_la == Abstract) {
                    {
                        setState(1288);
                        match(Abstract);
                    }
                }

                setState(1291);
                match(Class);
                setState(1292);
                identifier();
                setState(1294);
                _errHandler.sync(this);
                _la = _input.LA(1);
                if (_la == LessThan) {
                    {
                        setState(1293);
                        typeParameters();
                    }
                }

                setState(1296);
                classHeritage();
                setState(1297);
                classTail();
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class ClassHeritageContext extends ParserRuleContext {
        public ClassExtendsClauseContext classExtendsClause() {
            return getRuleContext(ClassExtendsClauseContext.class, 0);
        }

        public ImplementsClauseContext implementsClause() {
            return getRuleContext(ImplementsClauseContext.class, 0);
        }

        public ClassHeritageContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_classHeritage;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterClassHeritage(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitClassHeritage(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitClassHeritage(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final ClassHeritageContext classHeritage() throws RecognitionException {
        ClassHeritageContext _localctx = new ClassHeritageContext(_ctx, getState());
        enterRule(_localctx, 208, RULE_classHeritage);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(1300);
                _errHandler.sync(this);
                _la = _input.LA(1);
                if (_la == Extends) {
                    {
                        setState(1299);
                        classExtendsClause();
                    }
                }

                setState(1303);
                _errHandler.sync(this);
                _la = _input.LA(1);
                if (_la == Implements) {
                    {
                        setState(1302);
                        implementsClause();
                    }
                }

            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class ClassTailContext extends ParserRuleContext {
        public TerminalNode OpenBrace() {
            return getToken(TypeScriptParser.OpenBrace, 0);
        }

        public TerminalNode CloseBrace() {
            return getToken(TypeScriptParser.CloseBrace, 0);
        }

        public List<ClassElementContext> classElement() {
            return getRuleContexts(ClassElementContext.class);
        }

        public ClassElementContext classElement(int i) {
            return getRuleContext(ClassElementContext.class, i);
        }

        public ClassTailContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_classTail;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterClassTail(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitClassTail(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitClassTail(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final ClassTailContext classTail() throws RecognitionException {
        ClassTailContext _localctx = new ClassTailContext(_ctx, getState());
        enterRule(_localctx, 210, RULE_classTail);
        try {
            int _alt;
            enterOuterAlt(_localctx, 1);
            {
                setState(1305);
                match(OpenBrace);
                setState(1309);
                _errHandler.sync(this);
                _alt = getInterpreter().adaptivePredict(_input, 161, _ctx);
                while (_alt != 2 && _alt != org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER) {
                    if (_alt == 1) {
                        {
                            {
                                setState(1306);
                                classElement();
                            }
                        }
                    }
                    setState(1311);
                    _errHandler.sync(this);
                    _alt = getInterpreter().adaptivePredict(_input, 161, _ctx);
                }
                setState(1312);
                match(CloseBrace);
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class ClassExtendsClauseContext extends ParserRuleContext {
        public TerminalNode Extends() {
            return getToken(TypeScriptParser.Extends, 0);
        }

        public TypeReferenceContext typeReference() {
            return getRuleContext(TypeReferenceContext.class, 0);
        }

        public ClassExtendsClauseContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_classExtendsClause;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterClassExtendsClause(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitClassExtendsClause(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitClassExtendsClause(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final ClassExtendsClauseContext classExtendsClause() throws RecognitionException {
        ClassExtendsClauseContext _localctx = new ClassExtendsClauseContext(_ctx, getState());
        enterRule(_localctx, 212, RULE_classExtendsClause);
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(1314);
                match(Extends);
                setState(1315);
                typeReference();
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class ImplementsClauseContext extends ParserRuleContext {
        public TerminalNode Implements() {
            return getToken(TypeScriptParser.Implements, 0);
        }

        public ClassOrInterfaceTypeListContext classOrInterfaceTypeList() {
            return getRuleContext(ClassOrInterfaceTypeListContext.class, 0);
        }

        public ImplementsClauseContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_implementsClause;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterImplementsClause(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitImplementsClause(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitImplementsClause(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final ImplementsClauseContext implementsClause() throws RecognitionException {
        ImplementsClauseContext _localctx = new ImplementsClauseContext(_ctx, getState());
        enterRule(_localctx, 214, RULE_implementsClause);
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(1317);
                match(Implements);
                setState(1318);
                classOrInterfaceTypeList();
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class ClassElementContext extends ParserRuleContext {
        public ConstructorDeclarationContext constructorDeclaration() {
            return getRuleContext(ConstructorDeclarationContext.class, 0);
        }

        public PropertyMemberDeclarationContext propertyMemberDeclaration() {
            return getRuleContext(PropertyMemberDeclarationContext.class, 0);
        }

        public DecoratorListContext decoratorList() {
            return getRuleContext(DecoratorListContext.class, 0);
        }

        public IndexMemberDeclarationContext indexMemberDeclaration() {
            return getRuleContext(IndexMemberDeclarationContext.class, 0);
        }

        public StatementContext statement() {
            return getRuleContext(StatementContext.class, 0);
        }

        public ClassElementContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_classElement;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterClassElement(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitClassElement(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitClassElement(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final ClassElementContext classElement() throws RecognitionException {
        ClassElementContext _localctx = new ClassElementContext(_ctx, getState());
        enterRule(_localctx, 216, RULE_classElement);
        try {
            setState(1327);
            _errHandler.sync(this);
            switch (getInterpreter().adaptivePredict(_input, 163, _ctx)) {
                case 1:
                    enterOuterAlt(_localctx, 1); {
                    setState(1320);
                    constructorDeclaration();
                }
                    break;
                case 2:
                    enterOuterAlt(_localctx, 2); {
                    setState(1322);
                    _errHandler.sync(this);
                    switch (getInterpreter().adaptivePredict(_input, 162, _ctx)) {
                        case 1: {
                            setState(1321);
                            decoratorList();
                        }
                            break;
                    }
                    setState(1324);
                    propertyMemberDeclaration();
                }
                    break;
                case 3:
                    enterOuterAlt(_localctx, 3); {
                    setState(1325);
                    indexMemberDeclaration();
                }
                    break;
                case 4:
                    enterOuterAlt(_localctx, 4); {
                    setState(1326);
                    statement();
                }
                    break;
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class PropertyMemberDeclarationContext extends ParserRuleContext {
        public PropertyMemberDeclarationContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_propertyMemberDeclaration;
        }

        public PropertyMemberDeclarationContext() {}

        public void copyFrom(PropertyMemberDeclarationContext ctx) {
            super.copyFrom(ctx);
        }
    }
    @SuppressWarnings("CheckReturnValue")
    public static class PropertyDeclarationExpressionContext extends PropertyMemberDeclarationContext {
        public PropertyMemberBaseContext propertyMemberBase() {
            return getRuleContext(PropertyMemberBaseContext.class, 0);
        }

        public PropertyNameContext propertyName() {
            return getRuleContext(PropertyNameContext.class, 0);
        }

        public TerminalNode SemiColon() {
            return getToken(TypeScriptParser.SemiColon, 0);
        }

        public TerminalNode QuestionMark() {
            return getToken(TypeScriptParser.QuestionMark, 0);
        }

        public TypeAnnotationContext typeAnnotation() {
            return getRuleContext(TypeAnnotationContext.class, 0);
        }

        public InitializerContext initializer() {
            return getRuleContext(InitializerContext.class, 0);
        }

        public PropertyDeclarationExpressionContext(PropertyMemberDeclarationContext ctx) {
            copyFrom(ctx);
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterPropertyDeclarationExpression(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitPropertyDeclarationExpression(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitPropertyDeclarationExpression(this);
            else
                return visitor.visitChildren(this);
        }
    }
    @SuppressWarnings("CheckReturnValue")
    public static class MethodDeclarationExpressionContext extends PropertyMemberDeclarationContext {
        public PropertyMemberBaseContext propertyMemberBase() {
            return getRuleContext(PropertyMemberBaseContext.class, 0);
        }

        public PropertyNameContext propertyName() {
            return getRuleContext(PropertyNameContext.class, 0);
        }

        public CallSignatureContext callSignature() {
            return getRuleContext(CallSignatureContext.class, 0);
        }

        public TerminalNode SemiColon() {
            return getToken(TypeScriptParser.SemiColon, 0);
        }

        public TerminalNode OpenBrace() {
            return getToken(TypeScriptParser.OpenBrace, 0);
        }

        public FunctionBodyContext functionBody() {
            return getRuleContext(FunctionBodyContext.class, 0);
        }

        public TerminalNode CloseBrace() {
            return getToken(TypeScriptParser.CloseBrace, 0);
        }

        public MethodDeclarationExpressionContext(PropertyMemberDeclarationContext ctx) {
            copyFrom(ctx);
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterMethodDeclarationExpression(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitMethodDeclarationExpression(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitMethodDeclarationExpression(this);
            else
                return visitor.visitChildren(this);
        }
    }
    @SuppressWarnings("CheckReturnValue")
    public static class GetterSetterDeclarationExpressionContext extends PropertyMemberDeclarationContext {
        public PropertyMemberBaseContext propertyMemberBase() {
            return getRuleContext(PropertyMemberBaseContext.class, 0);
        }

        public GetAccessorContext getAccessor() {
            return getRuleContext(GetAccessorContext.class, 0);
        }

        public SetAccessorContext setAccessor() {
            return getRuleContext(SetAccessorContext.class, 0);
        }

        public GetterSetterDeclarationExpressionContext(PropertyMemberDeclarationContext ctx) {
            copyFrom(ctx);
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterGetterSetterDeclarationExpression(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitGetterSetterDeclarationExpression(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitGetterSetterDeclarationExpression(this);
            else
                return visitor.visitChildren(this);
        }
    }
    @SuppressWarnings("CheckReturnValue")
    public static class AbstractMemberDeclarationContext extends PropertyMemberDeclarationContext {
        public AbstractDeclarationContext abstractDeclaration() {
            return getRuleContext(AbstractDeclarationContext.class, 0);
        }

        public AbstractMemberDeclarationContext(PropertyMemberDeclarationContext ctx) {
            copyFrom(ctx);
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterAbstractMemberDeclaration(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitAbstractMemberDeclaration(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitAbstractMemberDeclaration(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final PropertyMemberDeclarationContext propertyMemberDeclaration() throws RecognitionException {
        PropertyMemberDeclarationContext _localctx = new PropertyMemberDeclarationContext(_ctx, getState());
        enterRule(_localctx, 218, RULE_propertyMemberDeclaration);
        int _la;
        try {
            setState(1358);
            _errHandler.sync(this);
            switch (getInterpreter().adaptivePredict(_input, 169, _ctx)) {
                case 1:
                    _localctx = new PropertyDeclarationExpressionContext(_localctx);
                    enterOuterAlt(_localctx, 1); {
                    setState(1329);
                    propertyMemberBase();
                    setState(1330);
                    propertyName();
                    setState(1332);
                    _errHandler.sync(this);
                    _la = _input.LA(1);
                    if (_la == QuestionMark) {
                        {
                            setState(1331);
                            match(QuestionMark);
                        }
                    }

                    setState(1335);
                    _errHandler.sync(this);
                    _la = _input.LA(1);
                    if (_la == Colon) {
                        {
                            setState(1334);
                            typeAnnotation();
                        }
                    }

                    setState(1338);
                    _errHandler.sync(this);
                    _la = _input.LA(1);
                    if (_la == Assign) {
                        {
                            setState(1337);
                            initializer();
                        }
                    }

                    setState(1340);
                    match(SemiColon);
                }
                    break;
                case 2:
                    _localctx = new MethodDeclarationExpressionContext(_localctx);
                    enterOuterAlt(_localctx, 2); {
                    setState(1342);
                    propertyMemberBase();
                    setState(1343);
                    propertyName();
                    setState(1344);
                    callSignature();
                    setState(1350);
                    _errHandler.sync(this);
                    switch (_input.LA(1)) {
                        case OpenBrace: {
                            {
                                setState(1345);
                                match(OpenBrace);
                                setState(1346);
                                functionBody();
                                setState(1347);
                                match(CloseBrace);
                            }
                        }
                            break;
                        case SemiColon: {
                            setState(1349);
                            match(SemiColon);
                        }
                            break;
                        default:
                            throw new NoViableAltException(this);
                    }
                }
                    break;
                case 3:
                    _localctx = new GetterSetterDeclarationExpressionContext(_localctx);
                    enterOuterAlt(_localctx, 3); {
                    setState(1352);
                    propertyMemberBase();
                    setState(1355);
                    _errHandler.sync(this);
                    switch (getInterpreter().adaptivePredict(_input, 168, _ctx)) {
                        case 1: {
                            setState(1353);
                            getAccessor();
                        }
                            break;
                        case 2: {
                            setState(1354);
                            setAccessor();
                        }
                            break;
                    }
                }
                    break;
                case 4:
                    _localctx = new AbstractMemberDeclarationContext(_localctx);
                    enterOuterAlt(_localctx, 4); {
                    setState(1357);
                    abstractDeclaration();
                }
                    break;
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class PropertyMemberBaseContext extends ParserRuleContext {
        public AccessibilityModifierContext accessibilityModifier() {
            return getRuleContext(AccessibilityModifierContext.class, 0);
        }

        public TerminalNode Async() {
            return getToken(TypeScriptParser.Async, 0);
        }

        public TerminalNode Static() {
            return getToken(TypeScriptParser.Static, 0);
        }

        public TerminalNode ReadOnly() {
            return getToken(TypeScriptParser.ReadOnly, 0);
        }

        public PropertyMemberBaseContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_propertyMemberBase;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterPropertyMemberBase(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitPropertyMemberBase(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitPropertyMemberBase(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final PropertyMemberBaseContext propertyMemberBase() throws RecognitionException {
        PropertyMemberBaseContext _localctx = new PropertyMemberBaseContext(_ctx, getState());
        enterRule(_localctx, 220, RULE_propertyMemberBase);
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(1361);
                _errHandler.sync(this);
                switch (getInterpreter().adaptivePredict(_input, 170, _ctx)) {
                    case 1: {
                        setState(1360);
                        accessibilityModifier();
                    }
                        break;
                }
                setState(1364);
                _errHandler.sync(this);
                switch (getInterpreter().adaptivePredict(_input, 171, _ctx)) {
                    case 1: {
                        setState(1363);
                        match(Async);
                    }
                        break;
                }
                setState(1367);
                _errHandler.sync(this);
                switch (getInterpreter().adaptivePredict(_input, 172, _ctx)) {
                    case 1: {
                        setState(1366);
                        match(Static);
                    }
                        break;
                }
                setState(1370);
                _errHandler.sync(this);
                switch (getInterpreter().adaptivePredict(_input, 173, _ctx)) {
                    case 1: {
                        setState(1369);
                        match(ReadOnly);
                    }
                        break;
                }
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class IndexMemberDeclarationContext extends ParserRuleContext {
        public IndexSignatureContext indexSignature() {
            return getRuleContext(IndexSignatureContext.class, 0);
        }

        public TerminalNode SemiColon() {
            return getToken(TypeScriptParser.SemiColon, 0);
        }

        public IndexMemberDeclarationContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_indexMemberDeclaration;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterIndexMemberDeclaration(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitIndexMemberDeclaration(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitIndexMemberDeclaration(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final IndexMemberDeclarationContext indexMemberDeclaration() throws RecognitionException {
        IndexMemberDeclarationContext _localctx = new IndexMemberDeclarationContext(_ctx, getState());
        enterRule(_localctx, 222, RULE_indexMemberDeclaration);
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(1372);
                indexSignature();
                setState(1373);
                match(SemiColon);
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class GeneratorMethodContext extends ParserRuleContext {
        public PropertyNameContext propertyName() {
            return getRuleContext(PropertyNameContext.class, 0);
        }

        public TerminalNode OpenParen() {
            return getToken(TypeScriptParser.OpenParen, 0);
        }

        public TerminalNode CloseParen() {
            return getToken(TypeScriptParser.CloseParen, 0);
        }

        public TerminalNode OpenBrace() {
            return getToken(TypeScriptParser.OpenBrace, 0);
        }

        public FunctionBodyContext functionBody() {
            return getRuleContext(FunctionBodyContext.class, 0);
        }

        public TerminalNode CloseBrace() {
            return getToken(TypeScriptParser.CloseBrace, 0);
        }

        public TerminalNode Async() {
            return getToken(TypeScriptParser.Async, 0);
        }

        public TerminalNode Multiply() {
            return getToken(TypeScriptParser.Multiply, 0);
        }

        public FormalParameterListContext formalParameterList() {
            return getRuleContext(FormalParameterListContext.class, 0);
        }

        public GeneratorMethodContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_generatorMethod;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterGeneratorMethod(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitGeneratorMethod(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitGeneratorMethod(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final GeneratorMethodContext generatorMethod() throws RecognitionException {
        GeneratorMethodContext _localctx = new GeneratorMethodContext(_ctx, getState());
        enterRule(_localctx, 224, RULE_generatorMethod);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(1377);
                _errHandler.sync(this);
                switch (getInterpreter().adaptivePredict(_input, 174, _ctx)) {
                    case 1: {
                        setState(1375);
                        match(Async);
                        setState(1376);
                        if (!(this.notLineTerminator()))
                            throw new FailedPredicateException(this, "this.notLineTerminator()");
                    }
                        break;
                }
                setState(1380);
                _errHandler.sync(this);
                _la = _input.LA(1);
                if (_la == Multiply) {
                    {
                        setState(1379);
                        match(Multiply);
                    }
                }

                setState(1382);
                propertyName();
                setState(1383);
                match(OpenParen);
                setState(1385);
                _errHandler.sync(this);
                _la = _input.LA(1);
                if ((((_la) & ~0x3f) == 0 && ((1L << _la) & 131344L) != 0)
                        || ((((_la - 70)) & ~0x3f) == 0 && ((1L << (_la - 70)) & -4294967297L) != 0)
                        || ((((_la - 135)) & ~0x3f) == 0 && ((1L << (_la - 135)) & 13L) != 0)) {
                    {
                        setState(1384);
                        formalParameterList();
                    }
                }

                setState(1387);
                match(CloseParen);
                setState(1388);
                match(OpenBrace);
                setState(1389);
                functionBody();
                setState(1390);
                match(CloseBrace);
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class GeneratorFunctionDeclarationContext extends ParserRuleContext {
        public TerminalNode Function_() {
            return getToken(TypeScriptParser.Function_, 0);
        }

        public TerminalNode Multiply() {
            return getToken(TypeScriptParser.Multiply, 0);
        }

        public TerminalNode OpenParen() {
            return getToken(TypeScriptParser.OpenParen, 0);
        }

        public TerminalNode CloseParen() {
            return getToken(TypeScriptParser.CloseParen, 0);
        }

        public TerminalNode OpenBrace() {
            return getToken(TypeScriptParser.OpenBrace, 0);
        }

        public FunctionBodyContext functionBody() {
            return getRuleContext(FunctionBodyContext.class, 0);
        }

        public TerminalNode CloseBrace() {
            return getToken(TypeScriptParser.CloseBrace, 0);
        }

        public TerminalNode Async() {
            return getToken(TypeScriptParser.Async, 0);
        }

        public IdentifierContext identifier() {
            return getRuleContext(IdentifierContext.class, 0);
        }

        public FormalParameterListContext formalParameterList() {
            return getRuleContext(FormalParameterListContext.class, 0);
        }

        public GeneratorFunctionDeclarationContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_generatorFunctionDeclaration;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterGeneratorFunctionDeclaration(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitGeneratorFunctionDeclaration(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitGeneratorFunctionDeclaration(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final GeneratorFunctionDeclarationContext generatorFunctionDeclaration() throws RecognitionException {
        GeneratorFunctionDeclarationContext _localctx = new GeneratorFunctionDeclarationContext(_ctx, getState());
        enterRule(_localctx, 226, RULE_generatorFunctionDeclaration);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(1393);
                _errHandler.sync(this);
                _la = _input.LA(1);
                if (_la == Async) {
                    {
                        setState(1392);
                        match(Async);
                    }
                }

                setState(1395);
                match(Function_);
                setState(1396);
                match(Multiply);
                setState(1398);
                _errHandler.sync(this);
                _la = _input.LA(1);
                if (((((_la - 96)) & ~0x3f) == 0 && ((1L << (_la - 96)) & 5016517607467L) != 0)) {
                    {
                        setState(1397);
                        identifier();
                    }
                }

                setState(1400);
                match(OpenParen);
                setState(1402);
                _errHandler.sync(this);
                _la = _input.LA(1);
                if ((((_la) & ~0x3f) == 0 && ((1L << _la) & 131344L) != 0)
                        || ((((_la - 70)) & ~0x3f) == 0 && ((1L << (_la - 70)) & -4294967297L) != 0)
                        || ((((_la - 135)) & ~0x3f) == 0 && ((1L << (_la - 135)) & 13L) != 0)) {
                    {
                        setState(1401);
                        formalParameterList();
                    }
                }

                setState(1404);
                match(CloseParen);
                setState(1405);
                match(OpenBrace);
                setState(1406);
                functionBody();
                setState(1407);
                match(CloseBrace);
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class GeneratorBlockContext extends ParserRuleContext {
        public TerminalNode OpenBrace() {
            return getToken(TypeScriptParser.OpenBrace, 0);
        }

        public List<GeneratorDefinitionContext> generatorDefinition() {
            return getRuleContexts(GeneratorDefinitionContext.class);
        }

        public GeneratorDefinitionContext generatorDefinition(int i) {
            return getRuleContext(GeneratorDefinitionContext.class, i);
        }

        public TerminalNode CloseBrace() {
            return getToken(TypeScriptParser.CloseBrace, 0);
        }

        public List<TerminalNode> Comma() {
            return getTokens(TypeScriptParser.Comma);
        }

        public TerminalNode Comma(int i) {
            return getToken(TypeScriptParser.Comma, i);
        }

        public GeneratorBlockContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_generatorBlock;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterGeneratorBlock(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitGeneratorBlock(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitGeneratorBlock(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final GeneratorBlockContext generatorBlock() throws RecognitionException {
        GeneratorBlockContext _localctx = new GeneratorBlockContext(_ctx, getState());
        enterRule(_localctx, 228, RULE_generatorBlock);
        int _la;
        try {
            int _alt;
            enterOuterAlt(_localctx, 1);
            {
                setState(1409);
                match(OpenBrace);
                setState(1410);
                generatorDefinition();
                setState(1415);
                _errHandler.sync(this);
                _alt = getInterpreter().adaptivePredict(_input, 180, _ctx);
                while (_alt != 2 && _alt != org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER) {
                    if (_alt == 1) {
                        {
                            {
                                setState(1411);
                                match(Comma);
                                setState(1412);
                                generatorDefinition();
                            }
                        }
                    }
                    setState(1417);
                    _errHandler.sync(this);
                    _alt = getInterpreter().adaptivePredict(_input, 180, _ctx);
                }
                setState(1419);
                _errHandler.sync(this);
                _la = _input.LA(1);
                if (_la == Comma) {
                    {
                        setState(1418);
                        match(Comma);
                    }
                }

                setState(1421);
                match(CloseBrace);
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class GeneratorDefinitionContext extends ParserRuleContext {
        public TerminalNode Multiply() {
            return getToken(TypeScriptParser.Multiply, 0);
        }

        public IteratorDefinitionContext iteratorDefinition() {
            return getRuleContext(IteratorDefinitionContext.class, 0);
        }

        public GeneratorDefinitionContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_generatorDefinition;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterGeneratorDefinition(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitGeneratorDefinition(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitGeneratorDefinition(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final GeneratorDefinitionContext generatorDefinition() throws RecognitionException {
        GeneratorDefinitionContext _localctx = new GeneratorDefinitionContext(_ctx, getState());
        enterRule(_localctx, 230, RULE_generatorDefinition);
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(1423);
                match(Multiply);
                setState(1424);
                iteratorDefinition();
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class IteratorBlockContext extends ParserRuleContext {
        public TerminalNode OpenBrace() {
            return getToken(TypeScriptParser.OpenBrace, 0);
        }

        public List<IteratorDefinitionContext> iteratorDefinition() {
            return getRuleContexts(IteratorDefinitionContext.class);
        }

        public IteratorDefinitionContext iteratorDefinition(int i) {
            return getRuleContext(IteratorDefinitionContext.class, i);
        }

        public TerminalNode CloseBrace() {
            return getToken(TypeScriptParser.CloseBrace, 0);
        }

        public List<TerminalNode> Comma() {
            return getTokens(TypeScriptParser.Comma);
        }

        public TerminalNode Comma(int i) {
            return getToken(TypeScriptParser.Comma, i);
        }

        public IteratorBlockContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_iteratorBlock;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterIteratorBlock(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitIteratorBlock(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitIteratorBlock(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final IteratorBlockContext iteratorBlock() throws RecognitionException {
        IteratorBlockContext _localctx = new IteratorBlockContext(_ctx, getState());
        enterRule(_localctx, 232, RULE_iteratorBlock);
        int _la;
        try {
            int _alt;
            enterOuterAlt(_localctx, 1);
            {
                setState(1426);
                match(OpenBrace);
                setState(1427);
                iteratorDefinition();
                setState(1432);
                _errHandler.sync(this);
                _alt = getInterpreter().adaptivePredict(_input, 182, _ctx);
                while (_alt != 2 && _alt != org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER) {
                    if (_alt == 1) {
                        {
                            {
                                setState(1428);
                                match(Comma);
                                setState(1429);
                                iteratorDefinition();
                            }
                        }
                    }
                    setState(1434);
                    _errHandler.sync(this);
                    _alt = getInterpreter().adaptivePredict(_input, 182, _ctx);
                }
                setState(1436);
                _errHandler.sync(this);
                _la = _input.LA(1);
                if (_la == Comma) {
                    {
                        setState(1435);
                        match(Comma);
                    }
                }

                setState(1438);
                match(CloseBrace);
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class IteratorDefinitionContext extends ParserRuleContext {
        public TerminalNode OpenBracket() {
            return getToken(TypeScriptParser.OpenBracket, 0);
        }

        public SingleExpressionContext singleExpression() {
            return getRuleContext(SingleExpressionContext.class, 0);
        }

        public TerminalNode CloseBracket() {
            return getToken(TypeScriptParser.CloseBracket, 0);
        }

        public TerminalNode OpenParen() {
            return getToken(TypeScriptParser.OpenParen, 0);
        }

        public TerminalNode CloseParen() {
            return getToken(TypeScriptParser.CloseParen, 0);
        }

        public TerminalNode OpenBrace() {
            return getToken(TypeScriptParser.OpenBrace, 0);
        }

        public FunctionBodyContext functionBody() {
            return getRuleContext(FunctionBodyContext.class, 0);
        }

        public TerminalNode CloseBrace() {
            return getToken(TypeScriptParser.CloseBrace, 0);
        }

        public FormalParameterListContext formalParameterList() {
            return getRuleContext(FormalParameterListContext.class, 0);
        }

        public IteratorDefinitionContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_iteratorDefinition;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterIteratorDefinition(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitIteratorDefinition(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitIteratorDefinition(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final IteratorDefinitionContext iteratorDefinition() throws RecognitionException {
        IteratorDefinitionContext _localctx = new IteratorDefinitionContext(_ctx, getState());
        enterRule(_localctx, 234, RULE_iteratorDefinition);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(1440);
                match(OpenBracket);
                setState(1441);
                singleExpression(0);
                setState(1442);
                match(CloseBracket);
                setState(1443);
                match(OpenParen);
                setState(1445);
                _errHandler.sync(this);
                _la = _input.LA(1);
                if ((((_la) & ~0x3f) == 0 && ((1L << _la) & 131344L) != 0)
                        || ((((_la - 70)) & ~0x3f) == 0 && ((1L << (_la - 70)) & -4294967297L) != 0)
                        || ((((_la - 135)) & ~0x3f) == 0 && ((1L << (_la - 135)) & 13L) != 0)) {
                    {
                        setState(1444);
                        formalParameterList();
                    }
                }

                setState(1447);
                match(CloseParen);
                setState(1448);
                match(OpenBrace);
                setState(1449);
                functionBody();
                setState(1450);
                match(CloseBrace);
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class ClassElementNameContext extends ParserRuleContext {
        public PropertyNameContext propertyName() {
            return getRuleContext(PropertyNameContext.class, 0);
        }

        public PrivateIdentifierContext privateIdentifier() {
            return getRuleContext(PrivateIdentifierContext.class, 0);
        }

        public ClassElementNameContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_classElementName;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterClassElementName(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitClassElementName(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitClassElementName(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final ClassElementNameContext classElementName() throws RecognitionException {
        ClassElementNameContext _localctx = new ClassElementNameContext(_ctx, getState());
        enterRule(_localctx, 236, RULE_classElementName);
        try {
            setState(1454);
            _errHandler.sync(this);
            switch (_input.LA(1)) {
                case OpenBracket:
                case NullLiteral:
                case BooleanLiteral:
                case DecimalLiteral:
                case HexIntegerLiteral:
                case OctalIntegerLiteral:
                case OctalIntegerLiteral2:
                case BinaryIntegerLiteral:
                case Break:
                case Do:
                case Instanceof:
                case Typeof:
                case Case:
                case Else:
                case New:
                case Var:
                case Catch:
                case Finally:
                case Return:
                case Void:
                case Continue:
                case For:
                case Switch:
                case While:
                case Debugger:
                case Function_:
                case This:
                case With:
                case Default:
                case If:
                case Throw:
                case Delete:
                case In:
                case Try:
                case As:
                case From:
                case ReadOnly:
                case Async:
                case Await:
                case Yield:
                case Class:
                case Enum:
                case Extends:
                case Super:
                case Const:
                case Export:
                case Import:
                case Implements:
                case Let:
                case Private:
                case Public:
                case Interface:
                case Package:
                case Protected:
                case Static:
                case Any:
                case Number:
                case Never:
                case Boolean:
                case String:
                case Unique:
                case Symbol:
                case Undefined:
                case Object:
                case Of:
                case KeyOf:
                case TypeAlias:
                case Constructor:
                case Namespace:
                case Require:
                case Module:
                case Abstract:
                case Identifier:
                case StringLiteral:
                    enterOuterAlt(_localctx, 1); {
                    setState(1452);
                    propertyName();
                }
                    break;
                case Hashtag:
                    enterOuterAlt(_localctx, 2); {
                    setState(1453);
                    privateIdentifier();
                }
                    break;
                default:
                    throw new NoViableAltException(this);
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class PrivateIdentifierContext extends ParserRuleContext {
        public TerminalNode Hashtag() {
            return getToken(TypeScriptParser.Hashtag, 0);
        }

        public IdentifierNameContext identifierName() {
            return getRuleContext(IdentifierNameContext.class, 0);
        }

        public PrivateIdentifierContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_privateIdentifier;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterPrivateIdentifier(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitPrivateIdentifier(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitPrivateIdentifier(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final PrivateIdentifierContext privateIdentifier() throws RecognitionException {
        PrivateIdentifierContext _localctx = new PrivateIdentifierContext(_ctx, getState());
        enterRule(_localctx, 238, RULE_privateIdentifier);
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(1456);
                match(Hashtag);
                setState(1457);
                identifierName();
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class FormalParameterListContext extends ParserRuleContext {
        public List<FormalParameterArgContext> formalParameterArg() {
            return getRuleContexts(FormalParameterArgContext.class);
        }

        public FormalParameterArgContext formalParameterArg(int i) {
            return getRuleContext(FormalParameterArgContext.class, i);
        }

        public List<TerminalNode> Comma() {
            return getTokens(TypeScriptParser.Comma);
        }

        public TerminalNode Comma(int i) {
            return getToken(TypeScriptParser.Comma, i);
        }

        public LastFormalParameterArgContext lastFormalParameterArg() {
            return getRuleContext(LastFormalParameterArgContext.class, 0);
        }

        public ArrayLiteralContext arrayLiteral() {
            return getRuleContext(ArrayLiteralContext.class, 0);
        }

        public ObjectLiteralContext objectLiteral() {
            return getRuleContext(ObjectLiteralContext.class, 0);
        }

        public TerminalNode Colon() {
            return getToken(TypeScriptParser.Colon, 0);
        }

        public FormalParameterListContext formalParameterList() {
            return getRuleContext(FormalParameterListContext.class, 0);
        }

        public FormalParameterListContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_formalParameterList;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterFormalParameterList(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitFormalParameterList(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitFormalParameterList(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final FormalParameterListContext formalParameterList() throws RecognitionException {
        FormalParameterListContext _localctx = new FormalParameterListContext(_ctx, getState());
        enterRule(_localctx, 240, RULE_formalParameterList);
        int _la;
        try {
            int _alt;
            setState(1481);
            _errHandler.sync(this);
            switch (getInterpreter().adaptivePredict(_input, 190, _ctx)) {
                case 1:
                    enterOuterAlt(_localctx, 1); {
                    setState(1459);
                    formalParameterArg();
                    setState(1464);
                    _errHandler.sync(this);
                    _alt = getInterpreter().adaptivePredict(_input, 186, _ctx);
                    while (_alt != 2 && _alt != org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER) {
                        if (_alt == 1) {
                            {
                                {
                                    setState(1460);
                                    match(Comma);
                                    setState(1461);
                                    formalParameterArg();
                                }
                            }
                        }
                        setState(1466);
                        _errHandler.sync(this);
                        _alt = getInterpreter().adaptivePredict(_input, 186, _ctx);
                    }
                    setState(1469);
                    _errHandler.sync(this);
                    switch (getInterpreter().adaptivePredict(_input, 187, _ctx)) {
                        case 1: {
                            setState(1467);
                            match(Comma);
                            setState(1468);
                            lastFormalParameterArg();
                        }
                            break;
                    }
                    setState(1472);
                    _errHandler.sync(this);
                    _la = _input.LA(1);
                    if (_la == Comma) {
                        {
                            setState(1471);
                            match(Comma);
                        }
                    }

                }
                    break;
                case 2:
                    enterOuterAlt(_localctx, 2); {
                    setState(1474);
                    lastFormalParameterArg();
                }
                    break;
                case 3:
                    enterOuterAlt(_localctx, 3); {
                    setState(1475);
                    arrayLiteral();
                }
                    break;
                case 4:
                    enterOuterAlt(_localctx, 4); {
                    setState(1476);
                    objectLiteral();
                    setState(1479);
                    _errHandler.sync(this);
                    _la = _input.LA(1);
                    if (_la == Colon) {
                        {
                            setState(1477);
                            match(Colon);
                            setState(1478);
                            formalParameterList();
                        }
                    }

                }
                    break;
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class FormalParameterArgContext extends ParserRuleContext {
        public AssignableContext assignable() {
            return getRuleContext(AssignableContext.class, 0);
        }

        public DecoratorContext decorator() {
            return getRuleContext(DecoratorContext.class, 0);
        }

        public AccessibilityModifierContext accessibilityModifier() {
            return getRuleContext(AccessibilityModifierContext.class, 0);
        }

        public TerminalNode QuestionMark() {
            return getToken(TypeScriptParser.QuestionMark, 0);
        }

        public TypeAnnotationContext typeAnnotation() {
            return getRuleContext(TypeAnnotationContext.class, 0);
        }

        public TerminalNode Assign() {
            return getToken(TypeScriptParser.Assign, 0);
        }

        public SingleExpressionContext singleExpression() {
            return getRuleContext(SingleExpressionContext.class, 0);
        }

        public FormalParameterArgContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_formalParameterArg;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterFormalParameterArg(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitFormalParameterArg(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitFormalParameterArg(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final FormalParameterArgContext formalParameterArg() throws RecognitionException {
        FormalParameterArgContext _localctx = new FormalParameterArgContext(_ctx, getState());
        enterRule(_localctx, 242, RULE_formalParameterArg);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(1484);
                _errHandler.sync(this);
                _la = _input.LA(1);
                if (_la == At) {
                    {
                        setState(1483);
                        decorator();
                    }
                }

                setState(1487);
                _errHandler.sync(this);
                switch (getInterpreter().adaptivePredict(_input, 192, _ctx)) {
                    case 1: {
                        setState(1486);
                        accessibilityModifier();
                    }
                        break;
                }
                setState(1489);
                assignable();
                setState(1491);
                _errHandler.sync(this);
                _la = _input.LA(1);
                if (_la == QuestionMark) {
                    {
                        setState(1490);
                        match(QuestionMark);
                    }
                }

                setState(1494);
                _errHandler.sync(this);
                _la = _input.LA(1);
                if (_la == Colon) {
                    {
                        setState(1493);
                        typeAnnotation();
                    }
                }

                setState(1498);
                _errHandler.sync(this);
                _la = _input.LA(1);
                if (_la == Assign) {
                    {
                        setState(1496);
                        match(Assign);
                        setState(1497);
                        singleExpression(0);
                    }
                }

            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class LastFormalParameterArgContext extends ParserRuleContext {
        public TerminalNode Ellipsis() {
            return getToken(TypeScriptParser.Ellipsis, 0);
        }

        public IdentifierContext identifier() {
            return getRuleContext(IdentifierContext.class, 0);
        }

        public TypeAnnotationContext typeAnnotation() {
            return getRuleContext(TypeAnnotationContext.class, 0);
        }

        public LastFormalParameterArgContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_lastFormalParameterArg;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterLastFormalParameterArg(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitLastFormalParameterArg(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitLastFormalParameterArg(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final LastFormalParameterArgContext lastFormalParameterArg() throws RecognitionException {
        LastFormalParameterArgContext _localctx = new LastFormalParameterArgContext(_ctx, getState());
        enterRule(_localctx, 244, RULE_lastFormalParameterArg);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(1500);
                match(Ellipsis);
                setState(1501);
                identifier();
                setState(1503);
                _errHandler.sync(this);
                _la = _input.LA(1);
                if (_la == Colon) {
                    {
                        setState(1502);
                        typeAnnotation();
                    }
                }

            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class FunctionBodyContext extends ParserRuleContext {
        public SourceElementsContext sourceElements() {
            return getRuleContext(SourceElementsContext.class, 0);
        }

        public FunctionBodyContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_functionBody;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterFunctionBody(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitFunctionBody(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitFunctionBody(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final FunctionBodyContext functionBody() throws RecognitionException {
        FunctionBodyContext _localctx = new FunctionBodyContext(_ctx, getState());
        enterRule(_localctx, 246, RULE_functionBody);
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(1506);
                _errHandler.sync(this);
                switch (getInterpreter().adaptivePredict(_input, 197, _ctx)) {
                    case 1: {
                        setState(1505);
                        sourceElements();
                    }
                        break;
                }
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class SourceElementsContext extends ParserRuleContext {
        public List<SourceElementContext> sourceElement() {
            return getRuleContexts(SourceElementContext.class);
        }

        public SourceElementContext sourceElement(int i) {
            return getRuleContext(SourceElementContext.class, i);
        }

        public SourceElementsContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_sourceElements;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterSourceElements(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitSourceElements(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitSourceElements(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final SourceElementsContext sourceElements() throws RecognitionException {
        SourceElementsContext _localctx = new SourceElementsContext(_ctx, getState());
        enterRule(_localctx, 248, RULE_sourceElements);
        try {
            int _alt;
            enterOuterAlt(_localctx, 1);
            {
                setState(1509);
                _errHandler.sync(this);
                _alt = 1;
                do {
                    switch (_alt) {
                        case 1: {
                            {
                                setState(1508);
                                sourceElement();
                            }
                        }
                            break;
                        default:
                            throw new NoViableAltException(this);
                    }
                    setState(1511);
                    _errHandler.sync(this);
                    _alt = getInterpreter().adaptivePredict(_input, 198, _ctx);
                } while (_alt != 2 && _alt != org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER);
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class ArrayLiteralContext extends ParserRuleContext {
        public TerminalNode OpenBracket() {
            return getToken(TypeScriptParser.OpenBracket, 0);
        }

        public ElementListContext elementList() {
            return getRuleContext(ElementListContext.class, 0);
        }

        public TerminalNode CloseBracket() {
            return getToken(TypeScriptParser.CloseBracket, 0);
        }

        public ArrayLiteralContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_arrayLiteral;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterArrayLiteral(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitArrayLiteral(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitArrayLiteral(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final ArrayLiteralContext arrayLiteral() throws RecognitionException {
        ArrayLiteralContext _localctx = new ArrayLiteralContext(_ctx, getState());
        enterRule(_localctx, 250, RULE_arrayLiteral);
        try {
            enterOuterAlt(_localctx, 1);
            {
                {
                    setState(1513);
                    match(OpenBracket);
                    setState(1514);
                    elementList();
                    setState(1515);
                    match(CloseBracket);
                }
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class ElementListContext extends ParserRuleContext {
        public List<TerminalNode> Comma() {
            return getTokens(TypeScriptParser.Comma);
        }

        public TerminalNode Comma(int i) {
            return getToken(TypeScriptParser.Comma, i);
        }

        public List<ArrayElementContext> arrayElement() {
            return getRuleContexts(ArrayElementContext.class);
        }

        public ArrayElementContext arrayElement(int i) {
            return getRuleContext(ArrayElementContext.class, i);
        }

        public ElementListContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_elementList;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterElementList(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitElementList(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitElementList(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final ElementListContext elementList() throws RecognitionException {
        ElementListContext _localctx = new ElementListContext(_ctx, getState());
        enterRule(_localctx, 252, RULE_elementList);
        int _la;
        try {
            int _alt;
            enterOuterAlt(_localctx, 1);
            {
                setState(1520);
                _errHandler.sync(this);
                _alt = getInterpreter().adaptivePredict(_input, 199, _ctx);
                while (_alt != 2 && _alt != org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER) {
                    if (_alt == 1) {
                        {
                            {
                                setState(1517);
                                match(Comma);
                            }
                        }
                    }
                    setState(1522);
                    _errHandler.sync(this);
                    _alt = getInterpreter().adaptivePredict(_input, 199, _ctx);
                }
                setState(1524);
                _errHandler.sync(this);
                _la = _input.LA(1);
                if ((((_la) & ~0x3f) == 0 && ((1L << _la) & -576460747975294632L) != 0)
                        || ((((_la - 64)) & ~0x3f) == 0 && ((1L << (_la - 64)) & -1L) != 0)
                        || ((((_la - 128)) & ~0x3f) == 0 && ((1L << (_la - 128)) & 7359L) != 0)) {
                    {
                        setState(1523);
                        arrayElement();
                    }
                }

                setState(1534);
                _errHandler.sync(this);
                _alt = getInterpreter().adaptivePredict(_input, 202, _ctx);
                while (_alt != 2 && _alt != org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER) {
                    if (_alt == 1) {
                        {
                            {
                                setState(1527);
                                _errHandler.sync(this);
                                _la = _input.LA(1);
                                do {
                                    {
                                        {
                                            setState(1526);
                                            match(Comma);
                                        }
                                    }
                                    setState(1529);
                                    _errHandler.sync(this);
                                    _la = _input.LA(1);
                                } while (_la == Comma);
                                setState(1531);
                                arrayElement();
                            }
                        }
                    }
                    setState(1536);
                    _errHandler.sync(this);
                    _alt = getInterpreter().adaptivePredict(_input, 202, _ctx);
                }
                setState(1540);
                _errHandler.sync(this);
                _la = _input.LA(1);
                while (_la == Comma) {
                    {
                        {
                            setState(1537);
                            match(Comma);
                        }
                    }
                    setState(1542);
                    _errHandler.sync(this);
                    _la = _input.LA(1);
                }
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class ArrayElementContext extends ParserRuleContext {
        public SingleExpressionContext singleExpression() {
            return getRuleContext(SingleExpressionContext.class, 0);
        }

        public IdentifierContext identifier() {
            return getRuleContext(IdentifierContext.class, 0);
        }

        public TerminalNode Ellipsis() {
            return getToken(TypeScriptParser.Ellipsis, 0);
        }

        public TerminalNode Comma() {
            return getToken(TypeScriptParser.Comma, 0);
        }

        public ArrayElementContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_arrayElement;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterArrayElement(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitArrayElement(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitArrayElement(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final ArrayElementContext arrayElement() throws RecognitionException {
        ArrayElementContext _localctx = new ArrayElementContext(_ctx, getState());
        enterRule(_localctx, 254, RULE_arrayElement);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(1544);
                _errHandler.sync(this);
                _la = _input.LA(1);
                if (_la == Ellipsis) {
                    {
                        setState(1543);
                        match(Ellipsis);
                    }
                }

                setState(1548);
                _errHandler.sync(this);
                switch (getInterpreter().adaptivePredict(_input, 205, _ctx)) {
                    case 1: {
                        setState(1546);
                        singleExpression(0);
                    }
                        break;
                    case 2: {
                        setState(1547);
                        identifier();
                    }
                        break;
                }
                setState(1551);
                _errHandler.sync(this);
                switch (getInterpreter().adaptivePredict(_input, 206, _ctx)) {
                    case 1: {
                        setState(1550);
                        match(Comma);
                    }
                        break;
                }
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class ObjectLiteralContext extends ParserRuleContext {
        public TerminalNode OpenBrace() {
            return getToken(TypeScriptParser.OpenBrace, 0);
        }

        public TerminalNode CloseBrace() {
            return getToken(TypeScriptParser.CloseBrace, 0);
        }

        public List<PropertyAssignmentContext> propertyAssignment() {
            return getRuleContexts(PropertyAssignmentContext.class);
        }

        public PropertyAssignmentContext propertyAssignment(int i) {
            return getRuleContext(PropertyAssignmentContext.class, i);
        }

        public List<TerminalNode> Comma() {
            return getTokens(TypeScriptParser.Comma);
        }

        public TerminalNode Comma(int i) {
            return getToken(TypeScriptParser.Comma, i);
        }

        public ObjectLiteralContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_objectLiteral;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterObjectLiteral(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitObjectLiteral(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitObjectLiteral(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final ObjectLiteralContext objectLiteral() throws RecognitionException {
        ObjectLiteralContext _localctx = new ObjectLiteralContext(_ctx, getState());
        enterRule(_localctx, 256, RULE_objectLiteral);
        int _la;
        try {
            int _alt;
            enterOuterAlt(_localctx, 1);
            {
                setState(1553);
                match(OpenBrace);
                setState(1565);
                _errHandler.sync(this);
                switch (getInterpreter().adaptivePredict(_input, 209, _ctx)) {
                    case 1: {
                        setState(1554);
                        propertyAssignment();
                        setState(1559);
                        _errHandler.sync(this);
                        _alt = getInterpreter().adaptivePredict(_input, 207, _ctx);
                        while (_alt != 2 && _alt != org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER) {
                            if (_alt == 1) {
                                {
                                    {
                                        setState(1555);
                                        match(Comma);
                                        setState(1556);
                                        propertyAssignment();
                                    }
                                }
                            }
                            setState(1561);
                            _errHandler.sync(this);
                            _alt = getInterpreter().adaptivePredict(_input, 207, _ctx);
                        }
                        setState(1563);
                        _errHandler.sync(this);
                        _la = _input.LA(1);
                        if (_la == Comma) {
                            {
                                setState(1562);
                                match(Comma);
                            }
                        }

                    }
                        break;
                }
                setState(1567);
                match(CloseBrace);
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class PropertyAssignmentContext extends ParserRuleContext {
        public PropertyAssignmentContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_propertyAssignment;
        }

        public PropertyAssignmentContext() {}

        public void copyFrom(PropertyAssignmentContext ctx) {
            super.copyFrom(ctx);
        }
    }
    @SuppressWarnings("CheckReturnValue")
    public static class PropertyExpressionAssignmentContext extends PropertyAssignmentContext {
        public PropertyNameContext propertyName() {
            return getRuleContext(PropertyNameContext.class, 0);
        }

        public SingleExpressionContext singleExpression() {
            return getRuleContext(SingleExpressionContext.class, 0);
        }

        public TerminalNode Colon() {
            return getToken(TypeScriptParser.Colon, 0);
        }

        public TerminalNode Assign() {
            return getToken(TypeScriptParser.Assign, 0);
        }

        public PropertyExpressionAssignmentContext(PropertyAssignmentContext ctx) {
            copyFrom(ctx);
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterPropertyExpressionAssignment(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitPropertyExpressionAssignment(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitPropertyExpressionAssignment(this);
            else
                return visitor.visitChildren(this);
        }
    }
    @SuppressWarnings("CheckReturnValue")
    public static class ComputedPropertyExpressionAssignmentContext extends PropertyAssignmentContext {
        public TerminalNode OpenBracket() {
            return getToken(TypeScriptParser.OpenBracket, 0);
        }

        public List<SingleExpressionContext> singleExpression() {
            return getRuleContexts(SingleExpressionContext.class);
        }

        public SingleExpressionContext singleExpression(int i) {
            return getRuleContext(SingleExpressionContext.class, i);
        }

        public TerminalNode CloseBracket() {
            return getToken(TypeScriptParser.CloseBracket, 0);
        }

        public TerminalNode Colon() {
            return getToken(TypeScriptParser.Colon, 0);
        }

        public ComputedPropertyExpressionAssignmentContext(PropertyAssignmentContext ctx) {
            copyFrom(ctx);
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterComputedPropertyExpressionAssignment(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitComputedPropertyExpressionAssignment(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitComputedPropertyExpressionAssignment(this);
            else
                return visitor.visitChildren(this);
        }
    }
    @SuppressWarnings("CheckReturnValue")
    public static class SpreadOperatorContext extends PropertyAssignmentContext {
        public SingleExpressionContext singleExpression() {
            return getRuleContext(SingleExpressionContext.class, 0);
        }

        public TerminalNode Ellipsis() {
            return getToken(TypeScriptParser.Ellipsis, 0);
        }

        public SpreadOperatorContext(PropertyAssignmentContext ctx) {
            copyFrom(ctx);
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterSpreadOperator(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitSpreadOperator(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitSpreadOperator(this);
            else
                return visitor.visitChildren(this);
        }
    }
    @SuppressWarnings("CheckReturnValue")
    public static class PropertyShorthandContext extends PropertyAssignmentContext {
        public IdentifierOrKeyWordContext identifierOrKeyWord() {
            return getRuleContext(IdentifierOrKeyWordContext.class, 0);
        }

        public PropertyShorthandContext(PropertyAssignmentContext ctx) {
            copyFrom(ctx);
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterPropertyShorthand(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitPropertyShorthand(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitPropertyShorthand(this);
            else
                return visitor.visitChildren(this);
        }
    }
    @SuppressWarnings("CheckReturnValue")
    public static class PropertySetterContext extends PropertyAssignmentContext {
        public SetAccessorContext setAccessor() {
            return getRuleContext(SetAccessorContext.class, 0);
        }

        public PropertySetterContext(PropertyAssignmentContext ctx) {
            copyFrom(ctx);
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterPropertySetter(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitPropertySetter(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitPropertySetter(this);
            else
                return visitor.visitChildren(this);
        }
    }
    @SuppressWarnings("CheckReturnValue")
    public static class PropertyGetterContext extends PropertyAssignmentContext {
        public GetAccessorContext getAccessor() {
            return getRuleContext(GetAccessorContext.class, 0);
        }

        public PropertyGetterContext(PropertyAssignmentContext ctx) {
            copyFrom(ctx);
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterPropertyGetter(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitPropertyGetter(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitPropertyGetter(this);
            else
                return visitor.visitChildren(this);
        }
    }
    @SuppressWarnings("CheckReturnValue")
    public static class RestParameterInObjectContext extends PropertyAssignmentContext {
        public RestParameterContext restParameter() {
            return getRuleContext(RestParameterContext.class, 0);
        }

        public RestParameterInObjectContext(PropertyAssignmentContext ctx) {
            copyFrom(ctx);
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterRestParameterInObject(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitRestParameterInObject(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitRestParameterInObject(this);
            else
                return visitor.visitChildren(this);
        }
    }
    @SuppressWarnings("CheckReturnValue")
    public static class MethodPropertyContext extends PropertyAssignmentContext {
        public GeneratorMethodContext generatorMethod() {
            return getRuleContext(GeneratorMethodContext.class, 0);
        }

        public MethodPropertyContext(PropertyAssignmentContext ctx) {
            copyFrom(ctx);
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterMethodProperty(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitMethodProperty(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitMethodProperty(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final PropertyAssignmentContext propertyAssignment() throws RecognitionException {
        PropertyAssignmentContext _localctx = new PropertyAssignmentContext(_ctx, getState());
        enterRule(_localctx, 258, RULE_propertyAssignment);
        int _la;
        try {
            setState(1588);
            _errHandler.sync(this);
            switch (getInterpreter().adaptivePredict(_input, 211, _ctx)) {
                case 1:
                    _localctx = new PropertyExpressionAssignmentContext(_localctx);
                    enterOuterAlt(_localctx, 1); {
                    setState(1569);
                    propertyName();
                    setState(1570);
                    _la = _input.LA(1);
                    if (!(_la == Assign || _la == Colon)) {
                        _errHandler.recoverInline(this);
                    } else {
                        if (_input.LA(1) == Token.EOF)
                            matchedEOF = true;
                        _errHandler.reportMatch(this);
                        consume();
                    }
                    setState(1571);
                    singleExpression(0);
                }
                    break;
                case 2:
                    _localctx = new ComputedPropertyExpressionAssignmentContext(_localctx);
                    enterOuterAlt(_localctx, 2); {
                    setState(1573);
                    match(OpenBracket);
                    setState(1574);
                    singleExpression(0);
                    setState(1575);
                    match(CloseBracket);
                    setState(1576);
                    match(Colon);
                    setState(1577);
                    singleExpression(0);
                }
                    break;
                case 3:
                    _localctx = new PropertyGetterContext(_localctx);
                    enterOuterAlt(_localctx, 3); {
                    setState(1579);
                    getAccessor();
                }
                    break;
                case 4:
                    _localctx = new PropertySetterContext(_localctx);
                    enterOuterAlt(_localctx, 4); {
                    setState(1580);
                    setAccessor();
                }
                    break;
                case 5:
                    _localctx = new MethodPropertyContext(_localctx);
                    enterOuterAlt(_localctx, 5); {
                    setState(1581);
                    generatorMethod();
                }
                    break;
                case 6:
                    _localctx = new PropertyShorthandContext(_localctx);
                    enterOuterAlt(_localctx, 6); {
                    setState(1582);
                    identifierOrKeyWord();
                }
                    break;
                case 7:
                    _localctx = new SpreadOperatorContext(_localctx);
                    enterOuterAlt(_localctx, 7); {
                    setState(1584);
                    _errHandler.sync(this);
                    _la = _input.LA(1);
                    if (_la == Ellipsis) {
                        {
                            setState(1583);
                            match(Ellipsis);
                        }
                    }

                    setState(1586);
                    singleExpression(0);
                }
                    break;
                case 8:
                    _localctx = new RestParameterInObjectContext(_localctx);
                    enterOuterAlt(_localctx, 8); {
                    setState(1587);
                    restParameter();
                }
                    break;
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class GetAccessorContext extends ParserRuleContext {
        public GetterContext getter() {
            return getRuleContext(GetterContext.class, 0);
        }

        public TerminalNode OpenParen() {
            return getToken(TypeScriptParser.OpenParen, 0);
        }

        public TerminalNode CloseParen() {
            return getToken(TypeScriptParser.CloseParen, 0);
        }

        public TerminalNode OpenBrace() {
            return getToken(TypeScriptParser.OpenBrace, 0);
        }

        public FunctionBodyContext functionBody() {
            return getRuleContext(FunctionBodyContext.class, 0);
        }

        public TerminalNode CloseBrace() {
            return getToken(TypeScriptParser.CloseBrace, 0);
        }

        public TypeAnnotationContext typeAnnotation() {
            return getRuleContext(TypeAnnotationContext.class, 0);
        }

        public GetAccessorContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_getAccessor;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterGetAccessor(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitGetAccessor(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitGetAccessor(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final GetAccessorContext getAccessor() throws RecognitionException {
        GetAccessorContext _localctx = new GetAccessorContext(_ctx, getState());
        enterRule(_localctx, 260, RULE_getAccessor);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(1590);
                getter();
                setState(1591);
                match(OpenParen);
                setState(1592);
                match(CloseParen);
                setState(1594);
                _errHandler.sync(this);
                _la = _input.LA(1);
                if (_la == Colon) {
                    {
                        setState(1593);
                        typeAnnotation();
                    }
                }

                setState(1596);
                match(OpenBrace);
                setState(1597);
                functionBody();
                setState(1598);
                match(CloseBrace);
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class SetAccessorContext extends ParserRuleContext {
        public SetterContext setter() {
            return getRuleContext(SetterContext.class, 0);
        }

        public TerminalNode OpenParen() {
            return getToken(TypeScriptParser.OpenParen, 0);
        }

        public TerminalNode CloseParen() {
            return getToken(TypeScriptParser.CloseParen, 0);
        }

        public TerminalNode OpenBrace() {
            return getToken(TypeScriptParser.OpenBrace, 0);
        }

        public FunctionBodyContext functionBody() {
            return getRuleContext(FunctionBodyContext.class, 0);
        }

        public TerminalNode CloseBrace() {
            return getToken(TypeScriptParser.CloseBrace, 0);
        }

        public FormalParameterListContext formalParameterList() {
            return getRuleContext(FormalParameterListContext.class, 0);
        }

        public SetAccessorContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_setAccessor;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterSetAccessor(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitSetAccessor(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitSetAccessor(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final SetAccessorContext setAccessor() throws RecognitionException {
        SetAccessorContext _localctx = new SetAccessorContext(_ctx, getState());
        enterRule(_localctx, 262, RULE_setAccessor);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(1600);
                setter();
                setState(1601);
                match(OpenParen);
                setState(1603);
                _errHandler.sync(this);
                _la = _input.LA(1);
                if ((((_la) & ~0x3f) == 0 && ((1L << _la) & 131344L) != 0)
                        || ((((_la - 70)) & ~0x3f) == 0 && ((1L << (_la - 70)) & -4294967297L) != 0)
                        || ((((_la - 135)) & ~0x3f) == 0 && ((1L << (_la - 135)) & 13L) != 0)) {
                    {
                        setState(1602);
                        formalParameterList();
                    }
                }

                setState(1605);
                match(CloseParen);
                setState(1606);
                match(OpenBrace);
                setState(1607);
                functionBody();
                setState(1608);
                match(CloseBrace);
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class PropertyNameContext extends ParserRuleContext {
        public IdentifierNameContext identifierName() {
            return getRuleContext(IdentifierNameContext.class, 0);
        }

        public TerminalNode StringLiteral() {
            return getToken(TypeScriptParser.StringLiteral, 0);
        }

        public NumericLiteralContext numericLiteral() {
            return getRuleContext(NumericLiteralContext.class, 0);
        }

        public TerminalNode OpenBracket() {
            return getToken(TypeScriptParser.OpenBracket, 0);
        }

        public SingleExpressionContext singleExpression() {
            return getRuleContext(SingleExpressionContext.class, 0);
        }

        public TerminalNode CloseBracket() {
            return getToken(TypeScriptParser.CloseBracket, 0);
        }

        public PropertyNameContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_propertyName;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterPropertyName(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitPropertyName(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitPropertyName(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final PropertyNameContext propertyName() throws RecognitionException {
        PropertyNameContext _localctx = new PropertyNameContext(_ctx, getState());
        enterRule(_localctx, 264, RULE_propertyName);
        try {
            setState(1617);
            _errHandler.sync(this);
            switch (_input.LA(1)) {
                case NullLiteral:
                case BooleanLiteral:
                case Break:
                case Do:
                case Instanceof:
                case Typeof:
                case Case:
                case Else:
                case New:
                case Var:
                case Catch:
                case Finally:
                case Return:
                case Void:
                case Continue:
                case For:
                case Switch:
                case While:
                case Debugger:
                case Function_:
                case This:
                case With:
                case Default:
                case If:
                case Throw:
                case Delete:
                case In:
                case Try:
                case As:
                case From:
                case ReadOnly:
                case Async:
                case Await:
                case Yield:
                case Class:
                case Enum:
                case Extends:
                case Super:
                case Const:
                case Export:
                case Import:
                case Implements:
                case Let:
                case Private:
                case Public:
                case Interface:
                case Package:
                case Protected:
                case Static:
                case Any:
                case Number:
                case Never:
                case Boolean:
                case String:
                case Unique:
                case Symbol:
                case Undefined:
                case Object:
                case Of:
                case KeyOf:
                case TypeAlias:
                case Constructor:
                case Namespace:
                case Require:
                case Module:
                case Abstract:
                case Identifier:
                    enterOuterAlt(_localctx, 1); {
                    setState(1610);
                    identifierName();
                }
                    break;
                case StringLiteral:
                    enterOuterAlt(_localctx, 2); {
                    setState(1611);
                    match(StringLiteral);
                }
                    break;
                case DecimalLiteral:
                case HexIntegerLiteral:
                case OctalIntegerLiteral:
                case OctalIntegerLiteral2:
                case BinaryIntegerLiteral:
                    enterOuterAlt(_localctx, 3); {
                    setState(1612);
                    numericLiteral();
                }
                    break;
                case OpenBracket:
                    enterOuterAlt(_localctx, 4); {
                    setState(1613);
                    match(OpenBracket);
                    setState(1614);
                    singleExpression(0);
                    setState(1615);
                    match(CloseBracket);
                }
                    break;
                default:
                    throw new NoViableAltException(this);
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class ArgumentsContext extends ParserRuleContext {
        public TerminalNode OpenParen() {
            return getToken(TypeScriptParser.OpenParen, 0);
        }

        public TerminalNode CloseParen() {
            return getToken(TypeScriptParser.CloseParen, 0);
        }

        public ArgumentListContext argumentList() {
            return getRuleContext(ArgumentListContext.class, 0);
        }

        public TerminalNode Comma() {
            return getToken(TypeScriptParser.Comma, 0);
        }

        public ArgumentsContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_arguments;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterArguments(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitArguments(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitArguments(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final ArgumentsContext arguments() throws RecognitionException {
        ArgumentsContext _localctx = new ArgumentsContext(_ctx, getState());
        enterRule(_localctx, 266, RULE_arguments);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(1619);
                match(OpenParen);
                setState(1624);
                _errHandler.sync(this);
                _la = _input.LA(1);
                if ((((_la) & ~0x3f) == 0 && ((1L << _la) & -576460747975294632L) != 0)
                        || ((((_la - 64)) & ~0x3f) == 0 && ((1L << (_la - 64)) & -1L) != 0)
                        || ((((_la - 128)) & ~0x3f) == 0 && ((1L << (_la - 128)) & 7359L) != 0)) {
                    {
                        setState(1620);
                        argumentList();
                        setState(1622);
                        _errHandler.sync(this);
                        _la = _input.LA(1);
                        if (_la == Comma) {
                            {
                                setState(1621);
                                match(Comma);
                            }
                        }

                    }
                }

                setState(1626);
                match(CloseParen);
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class ArgumentListContext extends ParserRuleContext {
        public List<ArgumentContext> argument() {
            return getRuleContexts(ArgumentContext.class);
        }

        public ArgumentContext argument(int i) {
            return getRuleContext(ArgumentContext.class, i);
        }

        public List<TerminalNode> Comma() {
            return getTokens(TypeScriptParser.Comma);
        }

        public TerminalNode Comma(int i) {
            return getToken(TypeScriptParser.Comma, i);
        }

        public ArgumentListContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_argumentList;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterArgumentList(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitArgumentList(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitArgumentList(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final ArgumentListContext argumentList() throws RecognitionException {
        ArgumentListContext _localctx = new ArgumentListContext(_ctx, getState());
        enterRule(_localctx, 268, RULE_argumentList);
        try {
            int _alt;
            enterOuterAlt(_localctx, 1);
            {
                setState(1628);
                argument();
                setState(1633);
                _errHandler.sync(this);
                _alt = getInterpreter().adaptivePredict(_input, 217, _ctx);
                while (_alt != 2 && _alt != org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER) {
                    if (_alt == 1) {
                        {
                            {
                                setState(1629);
                                match(Comma);
                                setState(1630);
                                argument();
                            }
                        }
                    }
                    setState(1635);
                    _errHandler.sync(this);
                    _alt = getInterpreter().adaptivePredict(_input, 217, _ctx);
                }
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class ArgumentContext extends ParserRuleContext {
        public SingleExpressionContext singleExpression() {
            return getRuleContext(SingleExpressionContext.class, 0);
        }

        public IdentifierContext identifier() {
            return getRuleContext(IdentifierContext.class, 0);
        }

        public TerminalNode Ellipsis() {
            return getToken(TypeScriptParser.Ellipsis, 0);
        }

        public ArgumentContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_argument;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterArgument(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitArgument(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitArgument(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final ArgumentContext argument() throws RecognitionException {
        ArgumentContext _localctx = new ArgumentContext(_ctx, getState());
        enterRule(_localctx, 270, RULE_argument);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(1637);
                _errHandler.sync(this);
                _la = _input.LA(1);
                if (_la == Ellipsis) {
                    {
                        setState(1636);
                        match(Ellipsis);
                    }
                }

                setState(1641);
                _errHandler.sync(this);
                switch (getInterpreter().adaptivePredict(_input, 219, _ctx)) {
                    case 1: {
                        setState(1639);
                        singleExpression(0);
                    }
                        break;
                    case 2: {
                        setState(1640);
                        identifier();
                    }
                        break;
                }
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class ExpressionSequenceContext extends ParserRuleContext {
        public List<SingleExpressionContext> singleExpression() {
            return getRuleContexts(SingleExpressionContext.class);
        }

        public SingleExpressionContext singleExpression(int i) {
            return getRuleContext(SingleExpressionContext.class, i);
        }

        public List<TerminalNode> Comma() {
            return getTokens(TypeScriptParser.Comma);
        }

        public TerminalNode Comma(int i) {
            return getToken(TypeScriptParser.Comma, i);
        }

        public ExpressionSequenceContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_expressionSequence;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterExpressionSequence(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitExpressionSequence(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitExpressionSequence(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final ExpressionSequenceContext expressionSequence() throws RecognitionException {
        ExpressionSequenceContext _localctx = new ExpressionSequenceContext(_ctx, getState());
        enterRule(_localctx, 272, RULE_expressionSequence);
        try {
            int _alt;
            enterOuterAlt(_localctx, 1);
            {
                setState(1643);
                singleExpression(0);
                setState(1648);
                _errHandler.sync(this);
                _alt = getInterpreter().adaptivePredict(_input, 220, _ctx);
                while (_alt != 2 && _alt != org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER) {
                    if (_alt == 1) {
                        {
                            {
                                setState(1644);
                                match(Comma);
                                setState(1645);
                                singleExpression(0);
                            }
                        }
                    }
                    setState(1650);
                    _errHandler.sync(this);
                    _alt = getInterpreter().adaptivePredict(_input, 220, _ctx);
                }
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class SingleExpressionContext extends ParserRuleContext {
        public SingleExpressionContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_singleExpression;
        }

        public SingleExpressionContext() {}

        public void copyFrom(SingleExpressionContext ctx) {
            super.copyFrom(ctx);
        }
    }
    @SuppressWarnings("CheckReturnValue")
    public static class TemplateStringExpressionContext extends SingleExpressionContext {
        public SingleExpressionContext singleExpression() {
            return getRuleContext(SingleExpressionContext.class, 0);
        }

        public TemplateStringLiteralContext templateStringLiteral() {
            return getRuleContext(TemplateStringLiteralContext.class, 0);
        }

        public TemplateStringExpressionContext(SingleExpressionContext ctx) {
            copyFrom(ctx);
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterTemplateStringExpression(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitTemplateStringExpression(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitTemplateStringExpression(this);
            else
                return visitor.visitChildren(this);
        }
    }
    @SuppressWarnings("CheckReturnValue")
    public static class GeneratorsExpressionContext extends SingleExpressionContext {
        public GeneratorBlockContext generatorBlock() {
            return getRuleContext(GeneratorBlockContext.class, 0);
        }

        public GeneratorsExpressionContext(SingleExpressionContext ctx) {
            copyFrom(ctx);
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterGeneratorsExpression(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitGeneratorsExpression(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitGeneratorsExpression(this);
            else
                return visitor.visitChildren(this);
        }
    }
    @SuppressWarnings("CheckReturnValue")
    public static class PowerExpressionContext extends SingleExpressionContext {
        public List<SingleExpressionContext> singleExpression() {
            return getRuleContexts(SingleExpressionContext.class);
        }

        public SingleExpressionContext singleExpression(int i) {
            return getRuleContext(SingleExpressionContext.class, i);
        }

        public TerminalNode Power() {
            return getToken(TypeScriptParser.Power, 0);
        }

        public PowerExpressionContext(SingleExpressionContext ctx) {
            copyFrom(ctx);
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterPowerExpression(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitPowerExpression(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitPowerExpression(this);
            else
                return visitor.visitChildren(this);
        }
    }
    @SuppressWarnings("CheckReturnValue")
    public static class InExpressionContext extends SingleExpressionContext {
        public List<SingleExpressionContext> singleExpression() {
            return getRuleContexts(SingleExpressionContext.class);
        }

        public SingleExpressionContext singleExpression(int i) {
            return getRuleContext(SingleExpressionContext.class, i);
        }

        public TerminalNode In() {
            return getToken(TypeScriptParser.In, 0);
        }

        public InExpressionContext(SingleExpressionContext ctx) {
            copyFrom(ctx);
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterInExpression(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitInExpression(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitInExpression(this);
            else
                return visitor.visitChildren(this);
        }
    }
    @SuppressWarnings("CheckReturnValue")
    public static class GenericTypesContext extends SingleExpressionContext {
        public TypeArgumentsContext typeArguments() {
            return getRuleContext(TypeArgumentsContext.class, 0);
        }

        public ExpressionSequenceContext expressionSequence() {
            return getRuleContext(ExpressionSequenceContext.class, 0);
        }

        public GenericTypesContext(SingleExpressionContext ctx) {
            copyFrom(ctx);
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterGenericTypes(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitGenericTypes(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitGenericTypes(this);
            else
                return visitor.visitChildren(this);
        }
    }
    @SuppressWarnings("CheckReturnValue")
    public static class OptionalChainExpressionContext extends SingleExpressionContext {
        public List<SingleExpressionContext> singleExpression() {
            return getRuleContexts(SingleExpressionContext.class);
        }

        public SingleExpressionContext singleExpression(int i) {
            return getRuleContext(SingleExpressionContext.class, i);
        }

        public TerminalNode QuestionMarkDot() {
            return getToken(TypeScriptParser.QuestionMarkDot, 0);
        }

        public OptionalChainExpressionContext(SingleExpressionContext ctx) {
            copyFrom(ctx);
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterOptionalChainExpression(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitOptionalChainExpression(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitOptionalChainExpression(this);
            else
                return visitor.visitChildren(this);
        }
    }
    @SuppressWarnings("CheckReturnValue")
    public static class ArgumentsExpressionContext extends SingleExpressionContext {
        public SingleExpressionContext singleExpression() {
            return getRuleContext(SingleExpressionContext.class, 0);
        }

        public ArgumentsContext arguments() {
            return getRuleContext(ArgumentsContext.class, 0);
        }

        public ArgumentsExpressionContext(SingleExpressionContext ctx) {
            copyFrom(ctx);
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterArgumentsExpression(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitArgumentsExpression(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitArgumentsExpression(this);
            else
                return visitor.visitChildren(this);
        }
    }
    @SuppressWarnings("CheckReturnValue")
    public static class ThisExpressionContext extends SingleExpressionContext {
        public TerminalNode This() {
            return getToken(TypeScriptParser.This, 0);
        }

        public ThisExpressionContext(SingleExpressionContext ctx) {
            copyFrom(ctx);
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterThisExpression(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitThisExpression(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitThisExpression(this);
            else
                return visitor.visitChildren(this);
        }
    }
    @SuppressWarnings("CheckReturnValue")
    public static class TypeofExpressionContext extends SingleExpressionContext {
        public TerminalNode Typeof() {
            return getToken(TypeScriptParser.Typeof, 0);
        }

        public SingleExpressionContext singleExpression() {
            return getRuleContext(SingleExpressionContext.class, 0);
        }

        public TypeofExpressionContext(SingleExpressionContext ctx) {
            copyFrom(ctx);
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterTypeofExpression(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitTypeofExpression(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitTypeofExpression(this);
            else
                return visitor.visitChildren(this);
        }
    }
    @SuppressWarnings("CheckReturnValue")
    public static class GeneratorsFunctionExpressionContext extends SingleExpressionContext {
        public GeneratorFunctionDeclarationContext generatorFunctionDeclaration() {
            return getRuleContext(GeneratorFunctionDeclarationContext.class, 0);
        }

        public GeneratorsFunctionExpressionContext(SingleExpressionContext ctx) {
            copyFrom(ctx);
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterGeneratorsFunctionExpression(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitGeneratorsFunctionExpression(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitGeneratorsFunctionExpression(this);
            else
                return visitor.visitChildren(this);
        }
    }
    @SuppressWarnings("CheckReturnValue")
    public static class EqualityExpressionContext extends SingleExpressionContext {
        public List<SingleExpressionContext> singleExpression() {
            return getRuleContexts(SingleExpressionContext.class);
        }

        public SingleExpressionContext singleExpression(int i) {
            return getRuleContext(SingleExpressionContext.class, i);
        }

        public TerminalNode Equals_() {
            return getToken(TypeScriptParser.Equals_, 0);
        }

        public TerminalNode NotEquals() {
            return getToken(TypeScriptParser.NotEquals, 0);
        }

        public TerminalNode IdentityEquals() {
            return getToken(TypeScriptParser.IdentityEquals, 0);
        }

        public TerminalNode IdentityNotEquals() {
            return getToken(TypeScriptParser.IdentityNotEquals, 0);
        }

        public EqualityExpressionContext(SingleExpressionContext ctx) {
            copyFrom(ctx);
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterEqualityExpression(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitEqualityExpression(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitEqualityExpression(this);
            else
                return visitor.visitChildren(this);
        }
    }
    @SuppressWarnings("CheckReturnValue")
    public static class BitXOrExpressionContext extends SingleExpressionContext {
        public List<SingleExpressionContext> singleExpression() {
            return getRuleContexts(SingleExpressionContext.class);
        }

        public SingleExpressionContext singleExpression(int i) {
            return getRuleContext(SingleExpressionContext.class, i);
        }

        public TerminalNode BitXOr() {
            return getToken(TypeScriptParser.BitXOr, 0);
        }

        public BitXOrExpressionContext(SingleExpressionContext ctx) {
            copyFrom(ctx);
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterBitXOrExpression(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitBitXOrExpression(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitBitXOrExpression(this);
            else
                return visitor.visitChildren(this);
        }
    }
    @SuppressWarnings("CheckReturnValue")
    public static class CastAsExpressionContext extends SingleExpressionContext {
        public SingleExpressionContext singleExpression() {
            return getRuleContext(SingleExpressionContext.class, 0);
        }

        public TerminalNode As() {
            return getToken(TypeScriptParser.As, 0);
        }

        public AsExpressionContext asExpression() {
            return getRuleContext(AsExpressionContext.class, 0);
        }

        public CastAsExpressionContext(SingleExpressionContext ctx) {
            copyFrom(ctx);
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterCastAsExpression(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitCastAsExpression(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitCastAsExpression(this);
            else
                return visitor.visitChildren(this);
        }
    }
    @SuppressWarnings("CheckReturnValue")
    public static class MultiplicativeExpressionContext extends SingleExpressionContext {
        public List<SingleExpressionContext> singleExpression() {
            return getRuleContexts(SingleExpressionContext.class);
        }

        public SingleExpressionContext singleExpression(int i) {
            return getRuleContext(SingleExpressionContext.class, i);
        }

        public TerminalNode Multiply() {
            return getToken(TypeScriptParser.Multiply, 0);
        }

        public TerminalNode Divide() {
            return getToken(TypeScriptParser.Divide, 0);
        }

        public TerminalNode Modulus() {
            return getToken(TypeScriptParser.Modulus, 0);
        }

        public MultiplicativeExpressionContext(SingleExpressionContext ctx) {
            copyFrom(ctx);
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterMultiplicativeExpression(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitMultiplicativeExpression(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitMultiplicativeExpression(this);
            else
                return visitor.visitChildren(this);
        }
    }
    @SuppressWarnings("CheckReturnValue")
    public static class BitShiftExpressionContext extends SingleExpressionContext {
        public List<SingleExpressionContext> singleExpression() {
            return getRuleContexts(SingleExpressionContext.class);
        }

        public SingleExpressionContext singleExpression(int i) {
            return getRuleContext(SingleExpressionContext.class, i);
        }

        public TerminalNode LeftShiftArithmetic() {
            return getToken(TypeScriptParser.LeftShiftArithmetic, 0);
        }

        public List<TerminalNode> MoreThan() {
            return getTokens(TypeScriptParser.MoreThan);
        }

        public TerminalNode MoreThan(int i) {
            return getToken(TypeScriptParser.MoreThan, i);
        }

        public BitShiftExpressionContext(SingleExpressionContext ctx) {
            copyFrom(ctx);
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterBitShiftExpression(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitBitShiftExpression(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitBitShiftExpression(this);
            else
                return visitor.visitChildren(this);
        }
    }
    @SuppressWarnings("CheckReturnValue")
    public static class AdditiveExpressionContext extends SingleExpressionContext {
        public List<SingleExpressionContext> singleExpression() {
            return getRuleContexts(SingleExpressionContext.class);
        }

        public SingleExpressionContext singleExpression(int i) {
            return getRuleContext(SingleExpressionContext.class, i);
        }

        public TerminalNode Plus() {
            return getToken(TypeScriptParser.Plus, 0);
        }

        public TerminalNode Minus() {
            return getToken(TypeScriptParser.Minus, 0);
        }

        public AdditiveExpressionContext(SingleExpressionContext ctx) {
            copyFrom(ctx);
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterAdditiveExpression(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitAdditiveExpression(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitAdditiveExpression(this);
            else
                return visitor.visitChildren(this);
        }
    }
    @SuppressWarnings("CheckReturnValue")
    public static class RelationalExpressionContext extends SingleExpressionContext {
        public List<SingleExpressionContext> singleExpression() {
            return getRuleContexts(SingleExpressionContext.class);
        }

        public SingleExpressionContext singleExpression(int i) {
            return getRuleContext(SingleExpressionContext.class, i);
        }

        public TerminalNode LessThan() {
            return getToken(TypeScriptParser.LessThan, 0);
        }

        public TerminalNode MoreThan() {
            return getToken(TypeScriptParser.MoreThan, 0);
        }

        public TerminalNode LessThanEquals() {
            return getToken(TypeScriptParser.LessThanEquals, 0);
        }

        public TerminalNode GreaterThanEquals() {
            return getToken(TypeScriptParser.GreaterThanEquals, 0);
        }

        public RelationalExpressionContext(SingleExpressionContext ctx) {
            copyFrom(ctx);
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterRelationalExpression(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitRelationalExpression(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitRelationalExpression(this);
            else
                return visitor.visitChildren(this);
        }
    }
    @SuppressWarnings("CheckReturnValue")
    public static class BitNotExpressionContext extends SingleExpressionContext {
        public TerminalNode BitNot() {
            return getToken(TypeScriptParser.BitNot, 0);
        }

        public SingleExpressionContext singleExpression() {
            return getRuleContext(SingleExpressionContext.class, 0);
        }

        public BitNotExpressionContext(SingleExpressionContext ctx) {
            copyFrom(ctx);
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterBitNotExpression(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitBitNotExpression(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitBitNotExpression(this);
            else
                return visitor.visitChildren(this);
        }
    }
    @SuppressWarnings("CheckReturnValue")
    public static class NewExpressionContext extends SingleExpressionContext {
        public TerminalNode New() {
            return getToken(TypeScriptParser.New, 0);
        }

        public SingleExpressionContext singleExpression() {
            return getRuleContext(SingleExpressionContext.class, 0);
        }

        public ArgumentsContext arguments() {
            return getRuleContext(ArgumentsContext.class, 0);
        }

        public TypeArgumentsContext typeArguments() {
            return getRuleContext(TypeArgumentsContext.class, 0);
        }

        public NewExpressionContext(SingleExpressionContext ctx) {
            copyFrom(ctx);
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterNewExpression(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitNewExpression(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitNewExpression(this);
            else
                return visitor.visitChildren(this);
        }
    }
    @SuppressWarnings("CheckReturnValue")
    public static class LiteralExpressionContext extends SingleExpressionContext {
        public LiteralContext literal() {
            return getRuleContext(LiteralContext.class, 0);
        }

        public LiteralExpressionContext(SingleExpressionContext ctx) {
            copyFrom(ctx);
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterLiteralExpression(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitLiteralExpression(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitLiteralExpression(this);
            else
                return visitor.visitChildren(this);
        }
    }
    @SuppressWarnings("CheckReturnValue")
    public static class ArrayLiteralExpressionContext extends SingleExpressionContext {
        public ArrayLiteralContext arrayLiteral() {
            return getRuleContext(ArrayLiteralContext.class, 0);
        }

        public ArrayLiteralExpressionContext(SingleExpressionContext ctx) {
            copyFrom(ctx);
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterArrayLiteralExpression(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitArrayLiteralExpression(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitArrayLiteralExpression(this);
            else
                return visitor.visitChildren(this);
        }
    }
    @SuppressWarnings("CheckReturnValue")
    public static class MemberDotExpressionContext extends SingleExpressionContext {
        public SingleExpressionContext singleExpression() {
            return getRuleContext(SingleExpressionContext.class, 0);
        }

        public TerminalNode Dot() {
            return getToken(TypeScriptParser.Dot, 0);
        }

        public IdentifierNameContext identifierName() {
            return getRuleContext(IdentifierNameContext.class, 0);
        }

        public TerminalNode Not() {
            return getToken(TypeScriptParser.Not, 0);
        }

        public TerminalNode Hashtag() {
            return getToken(TypeScriptParser.Hashtag, 0);
        }

        public TypeGenericContext typeGeneric() {
            return getRuleContext(TypeGenericContext.class, 0);
        }

        public TerminalNode QuestionMark() {
            return getToken(TypeScriptParser.QuestionMark, 0);
        }

        public MemberDotExpressionContext(SingleExpressionContext ctx) {
            copyFrom(ctx);
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterMemberDotExpression(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitMemberDotExpression(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitMemberDotExpression(this);
            else
                return visitor.visitChildren(this);
        }
    }
    @SuppressWarnings("CheckReturnValue")
    public static class MemberIndexExpressionContext extends SingleExpressionContext {
        public SingleExpressionContext singleExpression() {
            return getRuleContext(SingleExpressionContext.class, 0);
        }

        public TerminalNode OpenBracket() {
            return getToken(TypeScriptParser.OpenBracket, 0);
        }

        public ExpressionSequenceContext expressionSequence() {
            return getRuleContext(ExpressionSequenceContext.class, 0);
        }

        public TerminalNode CloseBracket() {
            return getToken(TypeScriptParser.CloseBracket, 0);
        }

        public TerminalNode QuestionMarkDot() {
            return getToken(TypeScriptParser.QuestionMarkDot, 0);
        }

        public MemberIndexExpressionContext(SingleExpressionContext ctx) {
            copyFrom(ctx);
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterMemberIndexExpression(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitMemberIndexExpression(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitMemberIndexExpression(this);
            else
                return visitor.visitChildren(this);
        }
    }
    @SuppressWarnings("CheckReturnValue")
    public static class BitAndExpressionContext extends SingleExpressionContext {
        public List<SingleExpressionContext> singleExpression() {
            return getRuleContexts(SingleExpressionContext.class);
        }

        public SingleExpressionContext singleExpression(int i) {
            return getRuleContext(SingleExpressionContext.class, i);
        }

        public TerminalNode BitAnd() {
            return getToken(TypeScriptParser.BitAnd, 0);
        }

        public BitAndExpressionContext(SingleExpressionContext ctx) {
            copyFrom(ctx);
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterBitAndExpression(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitBitAndExpression(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitBitAndExpression(this);
            else
                return visitor.visitChildren(this);
        }
    }
    @SuppressWarnings("CheckReturnValue")
    public static class BitOrExpressionContext extends SingleExpressionContext {
        public List<SingleExpressionContext> singleExpression() {
            return getRuleContexts(SingleExpressionContext.class);
        }

        public SingleExpressionContext singleExpression(int i) {
            return getRuleContext(SingleExpressionContext.class, i);
        }

        public TerminalNode BitOr() {
            return getToken(TypeScriptParser.BitOr, 0);
        }

        public BitOrExpressionContext(SingleExpressionContext ctx) {
            copyFrom(ctx);
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterBitOrExpression(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitBitOrExpression(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitBitOrExpression(this);
            else
                return visitor.visitChildren(this);
        }
    }
    @SuppressWarnings("CheckReturnValue")
    public static class AssignmentOperatorExpressionContext extends SingleExpressionContext {
        public List<SingleExpressionContext> singleExpression() {
            return getRuleContexts(SingleExpressionContext.class);
        }

        public SingleExpressionContext singleExpression(int i) {
            return getRuleContext(SingleExpressionContext.class, i);
        }

        public AssignmentOperatorContext assignmentOperator() {
            return getRuleContext(AssignmentOperatorContext.class, 0);
        }

        public AssignmentOperatorExpressionContext(SingleExpressionContext ctx) {
            copyFrom(ctx);
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterAssignmentOperatorExpression(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitAssignmentOperatorExpression(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitAssignmentOperatorExpression(this);
            else
                return visitor.visitChildren(this);
        }
    }
    @SuppressWarnings("CheckReturnValue")
    public static class VoidExpressionContext extends SingleExpressionContext {
        public TerminalNode Void() {
            return getToken(TypeScriptParser.Void, 0);
        }

        public SingleExpressionContext singleExpression() {
            return getRuleContext(SingleExpressionContext.class, 0);
        }

        public VoidExpressionContext(SingleExpressionContext ctx) {
            copyFrom(ctx);
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterVoidExpression(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitVoidExpression(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitVoidExpression(this);
            else
                return visitor.visitChildren(this);
        }
    }
    @SuppressWarnings("CheckReturnValue")
    public static class TernaryExpressionContext extends SingleExpressionContext {
        public List<SingleExpressionContext> singleExpression() {
            return getRuleContexts(SingleExpressionContext.class);
        }

        public SingleExpressionContext singleExpression(int i) {
            return getRuleContext(SingleExpressionContext.class, i);
        }

        public TerminalNode QuestionMark() {
            return getToken(TypeScriptParser.QuestionMark, 0);
        }

        public TerminalNode Colon() {
            return getToken(TypeScriptParser.Colon, 0);
        }

        public TernaryExpressionContext(SingleExpressionContext ctx) {
            copyFrom(ctx);
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterTernaryExpression(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitTernaryExpression(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitTernaryExpression(this);
            else
                return visitor.visitChildren(this);
        }
    }
    @SuppressWarnings("CheckReturnValue")
    public static class LogicalAndExpressionContext extends SingleExpressionContext {
        public List<SingleExpressionContext> singleExpression() {
            return getRuleContexts(SingleExpressionContext.class);
        }

        public SingleExpressionContext singleExpression(int i) {
            return getRuleContext(SingleExpressionContext.class, i);
        }

        public TerminalNode And() {
            return getToken(TypeScriptParser.And, 0);
        }

        public LogicalAndExpressionContext(SingleExpressionContext ctx) {
            copyFrom(ctx);
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterLogicalAndExpression(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitLogicalAndExpression(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitLogicalAndExpression(this);
            else
                return visitor.visitChildren(this);
        }
    }
    @SuppressWarnings("CheckReturnValue")
    public static class PreIncrementExpressionContext extends SingleExpressionContext {
        public TerminalNode PlusPlus() {
            return getToken(TypeScriptParser.PlusPlus, 0);
        }

        public SingleExpressionContext singleExpression() {
            return getRuleContext(SingleExpressionContext.class, 0);
        }

        public PreIncrementExpressionContext(SingleExpressionContext ctx) {
            copyFrom(ctx);
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterPreIncrementExpression(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitPreIncrementExpression(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitPreIncrementExpression(this);
            else
                return visitor.visitChildren(this);
        }
    }
    @SuppressWarnings("CheckReturnValue")
    public static class ObjectLiteralExpressionContext extends SingleExpressionContext {
        public ObjectLiteralContext objectLiteral() {
            return getRuleContext(ObjectLiteralContext.class, 0);
        }

        public ObjectLiteralExpressionContext(SingleExpressionContext ctx) {
            copyFrom(ctx);
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterObjectLiteralExpression(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitObjectLiteralExpression(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitObjectLiteralExpression(this);
            else
                return visitor.visitChildren(this);
        }
    }
    @SuppressWarnings("CheckReturnValue")
    public static class LogicalOrExpressionContext extends SingleExpressionContext {
        public List<SingleExpressionContext> singleExpression() {
            return getRuleContexts(SingleExpressionContext.class);
        }

        public SingleExpressionContext singleExpression(int i) {
            return getRuleContext(SingleExpressionContext.class, i);
        }

        public TerminalNode Or() {
            return getToken(TypeScriptParser.Or, 0);
        }

        public LogicalOrExpressionContext(SingleExpressionContext ctx) {
            copyFrom(ctx);
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterLogicalOrExpression(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitLogicalOrExpression(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitLogicalOrExpression(this);
            else
                return visitor.visitChildren(this);
        }
    }
    @SuppressWarnings("CheckReturnValue")
    public static class NonNullAssertionExpressionContext extends SingleExpressionContext {
        public SingleExpressionContext singleExpression() {
            return getRuleContext(SingleExpressionContext.class, 0);
        }

        public TerminalNode Not() {
            return getToken(TypeScriptParser.Not, 0);
        }

        public NonNullAssertionExpressionContext(SingleExpressionContext ctx) {
            copyFrom(ctx);
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterNonNullAssertionExpression(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitNonNullAssertionExpression(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitNonNullAssertionExpression(this);
            else
                return visitor.visitChildren(this);
        }
    }
    @SuppressWarnings("CheckReturnValue")
    public static class NotExpressionContext extends SingleExpressionContext {
        public TerminalNode Not() {
            return getToken(TypeScriptParser.Not, 0);
        }

        public SingleExpressionContext singleExpression() {
            return getRuleContext(SingleExpressionContext.class, 0);
        }

        public NotExpressionContext(SingleExpressionContext ctx) {
            copyFrom(ctx);
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterNotExpression(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitNotExpression(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitNotExpression(this);
            else
                return visitor.visitChildren(this);
        }
    }
    @SuppressWarnings("CheckReturnValue")
    public static class PreDecreaseExpressionContext extends SingleExpressionContext {
        public TerminalNode MinusMinus() {
            return getToken(TypeScriptParser.MinusMinus, 0);
        }

        public SingleExpressionContext singleExpression() {
            return getRuleContext(SingleExpressionContext.class, 0);
        }

        public PreDecreaseExpressionContext(SingleExpressionContext ctx) {
            copyFrom(ctx);
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterPreDecreaseExpression(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitPreDecreaseExpression(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitPreDecreaseExpression(this);
            else
                return visitor.visitChildren(this);
        }
    }
    @SuppressWarnings("CheckReturnValue")
    public static class AwaitExpressionContext extends SingleExpressionContext {
        public TerminalNode Await() {
            return getToken(TypeScriptParser.Await, 0);
        }

        public SingleExpressionContext singleExpression() {
            return getRuleContext(SingleExpressionContext.class, 0);
        }

        public AwaitExpressionContext(SingleExpressionContext ctx) {
            copyFrom(ctx);
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterAwaitExpression(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitAwaitExpression(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitAwaitExpression(this);
            else
                return visitor.visitChildren(this);
        }
    }
    @SuppressWarnings("CheckReturnValue")
    public static class FunctionExpressionContext extends SingleExpressionContext {
        public AnonymousFunctionContext anonymousFunction() {
            return getRuleContext(AnonymousFunctionContext.class, 0);
        }

        public FunctionExpressionContext(SingleExpressionContext ctx) {
            copyFrom(ctx);
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterFunctionExpression(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitFunctionExpression(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitFunctionExpression(this);
            else
                return visitor.visitChildren(this);
        }
    }
    @SuppressWarnings("CheckReturnValue")
    public static class UnaryMinusExpressionContext extends SingleExpressionContext {
        public TerminalNode Minus() {
            return getToken(TypeScriptParser.Minus, 0);
        }

        public SingleExpressionContext singleExpression() {
            return getRuleContext(SingleExpressionContext.class, 0);
        }

        public UnaryMinusExpressionContext(SingleExpressionContext ctx) {
            copyFrom(ctx);
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterUnaryMinusExpression(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitUnaryMinusExpression(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitUnaryMinusExpression(this);
            else
                return visitor.visitChildren(this);
        }
    }
    @SuppressWarnings("CheckReturnValue")
    public static class AssignmentExpressionContext extends SingleExpressionContext {
        public List<SingleExpressionContext> singleExpression() {
            return getRuleContexts(SingleExpressionContext.class);
        }

        public SingleExpressionContext singleExpression(int i) {
            return getRuleContext(SingleExpressionContext.class, i);
        }

        public TerminalNode Assign() {
            return getToken(TypeScriptParser.Assign, 0);
        }

        public AssignmentExpressionContext(SingleExpressionContext ctx) {
            copyFrom(ctx);
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterAssignmentExpression(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitAssignmentExpression(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitAssignmentExpression(this);
            else
                return visitor.visitChildren(this);
        }
    }
    @SuppressWarnings("CheckReturnValue")
    public static class PostDecreaseExpressionContext extends SingleExpressionContext {
        public SingleExpressionContext singleExpression() {
            return getRuleContext(SingleExpressionContext.class, 0);
        }

        public TerminalNode MinusMinus() {
            return getToken(TypeScriptParser.MinusMinus, 0);
        }

        public PostDecreaseExpressionContext(SingleExpressionContext ctx) {
            copyFrom(ctx);
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterPostDecreaseExpression(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitPostDecreaseExpression(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitPostDecreaseExpression(this);
            else
                return visitor.visitChildren(this);
        }
    }
    @SuppressWarnings("CheckReturnValue")
    public static class InstanceofExpressionContext extends SingleExpressionContext {
        public List<SingleExpressionContext> singleExpression() {
            return getRuleContexts(SingleExpressionContext.class);
        }

        public SingleExpressionContext singleExpression(int i) {
            return getRuleContext(SingleExpressionContext.class, i);
        }

        public TerminalNode Instanceof() {
            return getToken(TypeScriptParser.Instanceof, 0);
        }

        public InstanceofExpressionContext(SingleExpressionContext ctx) {
            copyFrom(ctx);
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterInstanceofExpression(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitInstanceofExpression(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitInstanceofExpression(this);
            else
                return visitor.visitChildren(this);
        }
    }
    @SuppressWarnings("CheckReturnValue")
    public static class UnaryPlusExpressionContext extends SingleExpressionContext {
        public TerminalNode Plus() {
            return getToken(TypeScriptParser.Plus, 0);
        }

        public SingleExpressionContext singleExpression() {
            return getRuleContext(SingleExpressionContext.class, 0);
        }

        public UnaryPlusExpressionContext(SingleExpressionContext ctx) {
            copyFrom(ctx);
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterUnaryPlusExpression(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitUnaryPlusExpression(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitUnaryPlusExpression(this);
            else
                return visitor.visitChildren(this);
        }
    }
    @SuppressWarnings("CheckReturnValue")
    public static class DeleteExpressionContext extends SingleExpressionContext {
        public TerminalNode Delete() {
            return getToken(TypeScriptParser.Delete, 0);
        }

        public SingleExpressionContext singleExpression() {
            return getRuleContext(SingleExpressionContext.class, 0);
        }

        public DeleteExpressionContext(SingleExpressionContext ctx) {
            copyFrom(ctx);
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterDeleteExpression(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitDeleteExpression(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitDeleteExpression(this);
            else
                return visitor.visitChildren(this);
        }
    }
    @SuppressWarnings("CheckReturnValue")
    public static class IteratorsExpressionContext extends SingleExpressionContext {
        public IteratorBlockContext iteratorBlock() {
            return getRuleContext(IteratorBlockContext.class, 0);
        }

        public IteratorsExpressionContext(SingleExpressionContext ctx) {
            copyFrom(ctx);
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterIteratorsExpression(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitIteratorsExpression(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitIteratorsExpression(this);
            else
                return visitor.visitChildren(this);
        }
    }
    @SuppressWarnings("CheckReturnValue")
    public static class SuperExpressionContext extends SingleExpressionContext {
        public TerminalNode Super() {
            return getToken(TypeScriptParser.Super, 0);
        }

        public SuperExpressionContext(SingleExpressionContext ctx) {
            copyFrom(ctx);
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterSuperExpression(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitSuperExpression(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitSuperExpression(this);
            else
                return visitor.visitChildren(this);
        }
    }
    @SuppressWarnings("CheckReturnValue")
    public static class ParenthesizedExpressionContext extends SingleExpressionContext {
        public TerminalNode OpenParen() {
            return getToken(TypeScriptParser.OpenParen, 0);
        }

        public ExpressionSequenceContext expressionSequence() {
            return getRuleContext(ExpressionSequenceContext.class, 0);
        }

        public TerminalNode CloseParen() {
            return getToken(TypeScriptParser.CloseParen, 0);
        }

        public ParenthesizedExpressionContext(SingleExpressionContext ctx) {
            copyFrom(ctx);
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterParenthesizedExpression(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitParenthesizedExpression(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitParenthesizedExpression(this);
            else
                return visitor.visitChildren(this);
        }
    }
    @SuppressWarnings("CheckReturnValue")
    public static class PostIncrementExpressionContext extends SingleExpressionContext {
        public SingleExpressionContext singleExpression() {
            return getRuleContext(SingleExpressionContext.class, 0);
        }

        public TerminalNode PlusPlus() {
            return getToken(TypeScriptParser.PlusPlus, 0);
        }

        public PostIncrementExpressionContext(SingleExpressionContext ctx) {
            copyFrom(ctx);
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterPostIncrementExpression(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitPostIncrementExpression(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitPostIncrementExpression(this);
            else
                return visitor.visitChildren(this);
        }
    }
    @SuppressWarnings("CheckReturnValue")
    public static class YieldExpressionContext extends SingleExpressionContext {
        public YieldStatementContext yieldStatement() {
            return getRuleContext(YieldStatementContext.class, 0);
        }

        public YieldExpressionContext(SingleExpressionContext ctx) {
            copyFrom(ctx);
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterYieldExpression(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitYieldExpression(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitYieldExpression(this);
            else
                return visitor.visitChildren(this);
        }
    }
    @SuppressWarnings("CheckReturnValue")
    public static class ClassExpressionContext extends SingleExpressionContext {
        public TerminalNode Class() {
            return getToken(TypeScriptParser.Class, 0);
        }

        public ClassHeritageContext classHeritage() {
            return getRuleContext(ClassHeritageContext.class, 0);
        }

        public ClassTailContext classTail() {
            return getRuleContext(ClassTailContext.class, 0);
        }

        public IdentifierContext identifier() {
            return getRuleContext(IdentifierContext.class, 0);
        }

        public TypeParametersContext typeParameters() {
            return getRuleContext(TypeParametersContext.class, 0);
        }

        public ClassExpressionContext(SingleExpressionContext ctx) {
            copyFrom(ctx);
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterClassExpression(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitClassExpression(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitClassExpression(this);
            else
                return visitor.visitChildren(this);
        }
    }
    @SuppressWarnings("CheckReturnValue")
    public static class IdentifierExpressionContext extends SingleExpressionContext {
        public IdentifierNameContext identifierName() {
            return getRuleContext(IdentifierNameContext.class, 0);
        }

        public SingleExpressionContext singleExpression() {
            return getRuleContext(SingleExpressionContext.class, 0);
        }

        public IdentifierExpressionContext(SingleExpressionContext ctx) {
            copyFrom(ctx);
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterIdentifierExpression(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitIdentifierExpression(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitIdentifierExpression(this);
            else
                return visitor.visitChildren(this);
        }
    }
    @SuppressWarnings("CheckReturnValue")
    public static class CoalesceExpressionContext extends SingleExpressionContext {
        public List<SingleExpressionContext> singleExpression() {
            return getRuleContexts(SingleExpressionContext.class);
        }

        public SingleExpressionContext singleExpression(int i) {
            return getRuleContext(SingleExpressionContext.class, i);
        }

        public TerminalNode NullCoalesce() {
            return getToken(TypeScriptParser.NullCoalesce, 0);
        }

        public CoalesceExpressionContext(SingleExpressionContext ctx) {
            copyFrom(ctx);
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterCoalesceExpression(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitCoalesceExpression(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitCoalesceExpression(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final SingleExpressionContext singleExpression() throws RecognitionException {
        return singleExpression(0);
    }

    private SingleExpressionContext singleExpression(int _p) throws RecognitionException {
        ParserRuleContext _parentctx = _ctx;
        int _parentState = getState();
        SingleExpressionContext _localctx = new SingleExpressionContext(_ctx, _parentState);
        SingleExpressionContext _prevctx = _localctx;
        int _startState = 274;
        enterRecursionRule(_localctx, 274, RULE_singleExpression, _p);
        int _la;
        try {
            int _alt;
            enterOuterAlt(_localctx, 1);
            {
                setState(1716);
                _errHandler.sync(this);
                switch (getInterpreter().adaptivePredict(_input, 227, _ctx)) {
                    case 1: {
                        _localctx = new FunctionExpressionContext(_localctx);
                        _ctx = _localctx;
                        _prevctx = _localctx;

                        setState(1652);
                        anonymousFunction();
                    }
                        break;
                    case 2: {
                        _localctx = new ClassExpressionContext(_localctx);
                        _ctx = _localctx;
                        _prevctx = _localctx;
                        setState(1653);
                        match(Class);
                        setState(1655);
                        _errHandler.sync(this);
                        _la = _input.LA(1);
                        if (((((_la - 96)) & ~0x3f) == 0 && ((1L << (_la - 96)) & 5016517607467L) != 0)) {
                            {
                                setState(1654);
                                identifier();
                            }
                        }

                        setState(1658);
                        _errHandler.sync(this);
                        _la = _input.LA(1);
                        if (_la == LessThan) {
                            {
                                setState(1657);
                                typeParameters();
                            }
                        }

                        setState(1660);
                        classHeritage();
                        setState(1661);
                        classTail();
                    }
                        break;
                    case 3: {
                        _localctx = new NewExpressionContext(_localctx);
                        _ctx = _localctx;
                        _prevctx = _localctx;
                        setState(1663);
                        match(New);
                        setState(1664);
                        singleExpression(0);
                        setState(1666);
                        _errHandler.sync(this);
                        _la = _input.LA(1);
                        if (_la == LessThan) {
                            {
                                setState(1665);
                                typeArguments();
                            }
                        }

                        setState(1668);
                        arguments();
                    }
                        break;
                    case 4: {
                        _localctx = new NewExpressionContext(_localctx);
                        _ctx = _localctx;
                        _prevctx = _localctx;
                        setState(1670);
                        match(New);
                        setState(1671);
                        singleExpression(0);
                        setState(1673);
                        _errHandler.sync(this);
                        switch (getInterpreter().adaptivePredict(_input, 224, _ctx)) {
                            case 1: {
                                setState(1672);
                                typeArguments();
                            }
                                break;
                        }
                    }
                        break;
                    case 5: {
                        _localctx = new DeleteExpressionContext(_localctx);
                        _ctx = _localctx;
                        _prevctx = _localctx;
                        setState(1675);
                        match(Delete);
                        setState(1676);
                        singleExpression(42);
                    }
                        break;
                    case 6: {
                        _localctx = new VoidExpressionContext(_localctx);
                        _ctx = _localctx;
                        _prevctx = _localctx;
                        setState(1677);
                        match(Void);
                        setState(1678);
                        singleExpression(41);
                    }
                        break;
                    case 7: {
                        _localctx = new TypeofExpressionContext(_localctx);
                        _ctx = _localctx;
                        _prevctx = _localctx;
                        setState(1679);
                        match(Typeof);
                        setState(1680);
                        singleExpression(40);
                    }
                        break;
                    case 8: {
                        _localctx = new PreIncrementExpressionContext(_localctx);
                        _ctx = _localctx;
                        _prevctx = _localctx;
                        setState(1681);
                        match(PlusPlus);
                        setState(1682);
                        singleExpression(39);
                    }
                        break;
                    case 9: {
                        _localctx = new PreDecreaseExpressionContext(_localctx);
                        _ctx = _localctx;
                        _prevctx = _localctx;
                        setState(1683);
                        match(MinusMinus);
                        setState(1684);
                        singleExpression(38);
                    }
                        break;
                    case 10: {
                        _localctx = new UnaryPlusExpressionContext(_localctx);
                        _ctx = _localctx;
                        _prevctx = _localctx;
                        setState(1685);
                        match(Plus);
                        setState(1686);
                        singleExpression(37);
                    }
                        break;
                    case 11: {
                        _localctx = new UnaryMinusExpressionContext(_localctx);
                        _ctx = _localctx;
                        _prevctx = _localctx;
                        setState(1687);
                        match(Minus);
                        setState(1688);
                        singleExpression(36);
                    }
                        break;
                    case 12: {
                        _localctx = new BitNotExpressionContext(_localctx);
                        _ctx = _localctx;
                        _prevctx = _localctx;
                        setState(1689);
                        match(BitNot);
                        setState(1690);
                        singleExpression(35);
                    }
                        break;
                    case 13: {
                        _localctx = new NotExpressionContext(_localctx);
                        _ctx = _localctx;
                        _prevctx = _localctx;
                        setState(1691);
                        match(Not);
                        setState(1692);
                        singleExpression(34);
                    }
                        break;
                    case 14: {
                        _localctx = new AwaitExpressionContext(_localctx);
                        _ctx = _localctx;
                        _prevctx = _localctx;
                        setState(1693);
                        match(Await);
                        setState(1694);
                        singleExpression(33);
                    }
                        break;
                    case 15: {
                        _localctx = new IteratorsExpressionContext(_localctx);
                        _ctx = _localctx;
                        _prevctx = _localctx;
                        setState(1695);
                        iteratorBlock();
                    }
                        break;
                    case 16: {
                        _localctx = new GeneratorsExpressionContext(_localctx);
                        _ctx = _localctx;
                        _prevctx = _localctx;
                        setState(1696);
                        generatorBlock();
                    }
                        break;
                    case 17: {
                        _localctx = new GeneratorsFunctionExpressionContext(_localctx);
                        _ctx = _localctx;
                        _prevctx = _localctx;
                        setState(1697);
                        generatorFunctionDeclaration();
                    }
                        break;
                    case 18: {
                        _localctx = new YieldExpressionContext(_localctx);
                        _ctx = _localctx;
                        _prevctx = _localctx;
                        setState(1698);
                        yieldStatement();
                    }
                        break;
                    case 19: {
                        _localctx = new ThisExpressionContext(_localctx);
                        _ctx = _localctx;
                        _prevctx = _localctx;
                        setState(1699);
                        match(This);
                    }
                        break;
                    case 20: {
                        _localctx = new IdentifierExpressionContext(_localctx);
                        _ctx = _localctx;
                        _prevctx = _localctx;
                        setState(1700);
                        identifierName();
                        setState(1702);
                        _errHandler.sync(this);
                        switch (getInterpreter().adaptivePredict(_input, 225, _ctx)) {
                            case 1: {
                                setState(1701);
                                singleExpression(0);
                            }
                                break;
                        }
                    }
                        break;
                    case 21: {
                        _localctx = new SuperExpressionContext(_localctx);
                        _ctx = _localctx;
                        _prevctx = _localctx;
                        setState(1704);
                        match(Super);
                    }
                        break;
                    case 22: {
                        _localctx = new LiteralExpressionContext(_localctx);
                        _ctx = _localctx;
                        _prevctx = _localctx;
                        setState(1705);
                        literal();
                    }
                        break;
                    case 23: {
                        _localctx = new ArrayLiteralExpressionContext(_localctx);
                        _ctx = _localctx;
                        _prevctx = _localctx;
                        setState(1706);
                        arrayLiteral();
                    }
                        break;
                    case 24: {
                        _localctx = new ObjectLiteralExpressionContext(_localctx);
                        _ctx = _localctx;
                        _prevctx = _localctx;
                        setState(1707);
                        objectLiteral();
                    }
                        break;
                    case 25: {
                        _localctx = new ParenthesizedExpressionContext(_localctx);
                        _ctx = _localctx;
                        _prevctx = _localctx;
                        setState(1708);
                        match(OpenParen);
                        setState(1709);
                        expressionSequence();
                        setState(1710);
                        match(CloseParen);
                    }
                        break;
                    case 26: {
                        _localctx = new GenericTypesContext(_localctx);
                        _ctx = _localctx;
                        _prevctx = _localctx;
                        setState(1712);
                        typeArguments();
                        setState(1714);
                        _errHandler.sync(this);
                        switch (getInterpreter().adaptivePredict(_input, 226, _ctx)) {
                            case 1: {
                                setState(1713);
                                expressionSequence();
                            }
                                break;
                        }
                    }
                        break;
                }
                _ctx.stop = _input.LT(-1);
                setState(1832);
                _errHandler.sync(this);
                _alt = getInterpreter().adaptivePredict(_input, 237, _ctx);
                while (_alt != 2 && _alt != org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER) {
                    if (_alt == 1) {
                        if (_parseListeners != null)
                            triggerExitRuleEvent();
                        _prevctx = _localctx;
                        {
                            setState(1830);
                            _errHandler.sync(this);
                            switch (getInterpreter().adaptivePredict(_input, 236, _ctx)) {
                                case 1: {
                                    _localctx = new OptionalChainExpressionContext(new SingleExpressionContext(_parentctx, _parentState));
                                    pushNewRecursionContext(_localctx, _startState, RULE_singleExpression);
                                    setState(1718);
                                    if (!(precpred(_ctx, 50)))
                                        throw new FailedPredicateException(this, "precpred(_ctx, 50)");
                                    setState(1719);
                                    match(QuestionMarkDot);
                                    setState(1720);
                                    singleExpression(51);
                                }
                                    break;
                                case 2: {
                                    _localctx = new PowerExpressionContext(new SingleExpressionContext(_parentctx, _parentState));
                                    pushNewRecursionContext(_localctx, _startState, RULE_singleExpression);
                                    setState(1721);
                                    if (!(precpred(_ctx, 32)))
                                        throw new FailedPredicateException(this, "precpred(_ctx, 32)");
                                    setState(1722);
                                    match(Power);
                                    setState(1723);
                                    singleExpression(32);
                                }
                                    break;
                                case 3: {
                                    _localctx = new MultiplicativeExpressionContext(new SingleExpressionContext(_parentctx, _parentState));
                                    pushNewRecursionContext(_localctx, _startState, RULE_singleExpression);
                                    setState(1724);
                                    if (!(precpred(_ctx, 31)))
                                        throw new FailedPredicateException(this, "precpred(_ctx, 31)");
                                    setState(1725);
                                    _la = _input.LA(1);
                                    if (!((((_la) & ~0x3f) == 0 && ((1L << _la) & 234881024L) != 0))) {
                                        _errHandler.recoverInline(this);
                                    } else {
                                        if (_input.LA(1) == Token.EOF)
                                            matchedEOF = true;
                                        _errHandler.reportMatch(this);
                                        consume();
                                    }
                                    setState(1726);
                                    singleExpression(32);
                                }
                                    break;
                                case 4: {
                                    _localctx = new AdditiveExpressionContext(new SingleExpressionContext(_parentctx, _parentState));
                                    pushNewRecursionContext(_localctx, _startState, RULE_singleExpression);
                                    setState(1727);
                                    if (!(precpred(_ctx, 30)))
                                        throw new FailedPredicateException(this, "precpred(_ctx, 30)");
                                    setState(1728);
                                    _la = _input.LA(1);
                                    if (!(_la == Plus || _la == Minus)) {
                                        _errHandler.recoverInline(this);
                                    } else {
                                        if (_input.LA(1) == Token.EOF)
                                            matchedEOF = true;
                                        _errHandler.reportMatch(this);
                                        consume();
                                    }
                                    setState(1729);
                                    singleExpression(31);
                                }
                                    break;
                                case 5: {
                                    _localctx = new CoalesceExpressionContext(new SingleExpressionContext(_parentctx, _parentState));
                                    pushNewRecursionContext(_localctx, _startState, RULE_singleExpression);
                                    setState(1730);
                                    if (!(precpred(_ctx, 29)))
                                        throw new FailedPredicateException(this, "precpred(_ctx, 29)");
                                    setState(1731);
                                    match(NullCoalesce);
                                    setState(1732);
                                    singleExpression(30);
                                }
                                    break;
                                case 6: {
                                    _localctx = new BitShiftExpressionContext(new SingleExpressionContext(_parentctx, _parentState));
                                    pushNewRecursionContext(_localctx, _startState, RULE_singleExpression);
                                    setState(1733);
                                    if (!(precpred(_ctx, 28)))
                                        throw new FailedPredicateException(this, "precpred(_ctx, 28)");
                                    setState(1740);
                                    _errHandler.sync(this);
                                    switch (getInterpreter().adaptivePredict(_input, 228, _ctx)) {
                                        case 1: {
                                            setState(1734);
                                            match(LeftShiftArithmetic);
                                        }
                                            break;
                                        case 2: {
                                            setState(1735);
                                            match(MoreThan);
                                            setState(1736);
                                            match(MoreThan);
                                        }
                                            break;
                                        case 3: {
                                            setState(1737);
                                            match(MoreThan);
                                            setState(1738);
                                            match(MoreThan);
                                            setState(1739);
                                            match(MoreThan);
                                        }
                                            break;
                                    }
                                    setState(1742);
                                    singleExpression(29);
                                }
                                    break;
                                case 7: {
                                    _localctx = new RelationalExpressionContext(new SingleExpressionContext(_parentctx, _parentState));
                                    pushNewRecursionContext(_localctx, _startState, RULE_singleExpression);
                                    setState(1743);
                                    if (!(precpred(_ctx, 27)))
                                        throw new FailedPredicateException(this, "precpred(_ctx, 27)");
                                    setState(1744);
                                    _la = _input.LA(1);
                                    if (!((((_la) & ~0x3f) == 0 && ((1L << _la) & 64424509440L) != 0))) {
                                        _errHandler.recoverInline(this);
                                    } else {
                                        if (_input.LA(1) == Token.EOF)
                                            matchedEOF = true;
                                        _errHandler.reportMatch(this);
                                        consume();
                                    }
                                    setState(1745);
                                    singleExpression(28);
                                }
                                    break;
                                case 8: {
                                    _localctx = new InstanceofExpressionContext(new SingleExpressionContext(_parentctx, _parentState));
                                    pushNewRecursionContext(_localctx, _startState, RULE_singleExpression);
                                    setState(1746);
                                    if (!(precpred(_ctx, 26)))
                                        throw new FailedPredicateException(this, "precpred(_ctx, 26)");
                                    setState(1747);
                                    match(Instanceof);
                                    setState(1748);
                                    singleExpression(27);
                                }
                                    break;
                                case 9: {
                                    _localctx = new InExpressionContext(new SingleExpressionContext(_parentctx, _parentState));
                                    pushNewRecursionContext(_localctx, _startState, RULE_singleExpression);
                                    setState(1749);
                                    if (!(precpred(_ctx, 25)))
                                        throw new FailedPredicateException(this, "precpred(_ctx, 25)");
                                    setState(1750);
                                    match(In);
                                    setState(1751);
                                    singleExpression(26);
                                }
                                    break;
                                case 10: {
                                    _localctx = new EqualityExpressionContext(new SingleExpressionContext(_parentctx, _parentState));
                                    pushNewRecursionContext(_localctx, _startState, RULE_singleExpression);
                                    setState(1752);
                                    if (!(precpred(_ctx, 24)))
                                        throw new FailedPredicateException(this, "precpred(_ctx, 24)");
                                    setState(1753);
                                    _la = _input.LA(1);
                                    if (!((((_la) & ~0x3f) == 0 && ((1L << _la) & 1030792151040L) != 0))) {
                                        _errHandler.recoverInline(this);
                                    } else {
                                        if (_input.LA(1) == Token.EOF)
                                            matchedEOF = true;
                                        _errHandler.reportMatch(this);
                                        consume();
                                    }
                                    setState(1754);
                                    singleExpression(25);
                                }
                                    break;
                                case 11: {
                                    _localctx = new BitAndExpressionContext(new SingleExpressionContext(_parentctx, _parentState));
                                    pushNewRecursionContext(_localctx, _startState, RULE_singleExpression);
                                    setState(1755);
                                    if (!(precpred(_ctx, 23)))
                                        throw new FailedPredicateException(this, "precpred(_ctx, 23)");
                                    setState(1756);
                                    match(BitAnd);
                                    setState(1757);
                                    singleExpression(24);
                                }
                                    break;
                                case 12: {
                                    _localctx = new BitXOrExpressionContext(new SingleExpressionContext(_parentctx, _parentState));
                                    pushNewRecursionContext(_localctx, _startState, RULE_singleExpression);
                                    setState(1758);
                                    if (!(precpred(_ctx, 22)))
                                        throw new FailedPredicateException(this, "precpred(_ctx, 22)");
                                    setState(1759);
                                    match(BitXOr);
                                    setState(1760);
                                    singleExpression(23);
                                }
                                    break;
                                case 13: {
                                    _localctx = new BitOrExpressionContext(new SingleExpressionContext(_parentctx, _parentState));
                                    pushNewRecursionContext(_localctx, _startState, RULE_singleExpression);
                                    setState(1761);
                                    if (!(precpred(_ctx, 21)))
                                        throw new FailedPredicateException(this, "precpred(_ctx, 21)");
                                    setState(1762);
                                    match(BitOr);
                                    setState(1763);
                                    singleExpression(22);
                                }
                                    break;
                                case 14: {
                                    _localctx = new LogicalAndExpressionContext(new SingleExpressionContext(_parentctx, _parentState));
                                    pushNewRecursionContext(_localctx, _startState, RULE_singleExpression);
                                    setState(1764);
                                    if (!(precpred(_ctx, 20)))
                                        throw new FailedPredicateException(this, "precpred(_ctx, 20)");
                                    setState(1765);
                                    match(And);
                                    setState(1766);
                                    singleExpression(21);
                                }
                                    break;
                                case 15: {
                                    _localctx = new LogicalOrExpressionContext(new SingleExpressionContext(_parentctx, _parentState));
                                    pushNewRecursionContext(_localctx, _startState, RULE_singleExpression);
                                    setState(1767);
                                    if (!(precpred(_ctx, 19)))
                                        throw new FailedPredicateException(this, "precpred(_ctx, 19)");
                                    setState(1768);
                                    match(Or);
                                    setState(1769);
                                    singleExpression(20);
                                }
                                    break;
                                case 16: {
                                    _localctx = new TernaryExpressionContext(new SingleExpressionContext(_parentctx, _parentState));
                                    pushNewRecursionContext(_localctx, _startState, RULE_singleExpression);
                                    setState(1770);
                                    if (!(precpred(_ctx, 18)))
                                        throw new FailedPredicateException(this, "precpred(_ctx, 18)");
                                    setState(1771);
                                    match(QuestionMark);
                                    setState(1772);
                                    singleExpression(0);
                                    setState(1773);
                                    match(Colon);
                                    setState(1774);
                                    singleExpression(19);
                                }
                                    break;
                                case 17: {
                                    _localctx = new AssignmentExpressionContext(new SingleExpressionContext(_parentctx, _parentState));
                                    pushNewRecursionContext(_localctx, _startState, RULE_singleExpression);
                                    setState(1776);
                                    if (!(precpred(_ctx, 17)))
                                        throw new FailedPredicateException(this, "precpred(_ctx, 17)");
                                    setState(1777);
                                    match(Assign);
                                    setState(1778);
                                    singleExpression(18);
                                }
                                    break;
                                case 18: {
                                    _localctx =
                                            new AssignmentOperatorExpressionContext(new SingleExpressionContext(_parentctx, _parentState));
                                    pushNewRecursionContext(_localctx, _startState, RULE_singleExpression);
                                    setState(1779);
                                    if (!(precpred(_ctx, 16)))
                                        throw new FailedPredicateException(this, "precpred(_ctx, 16)");
                                    setState(1780);
                                    assignmentOperator();
                                    setState(1781);
                                    singleExpression(17);
                                }
                                    break;
                                case 19: {
                                    _localctx = new MemberIndexExpressionContext(new SingleExpressionContext(_parentctx, _parentState));
                                    pushNewRecursionContext(_localctx, _startState, RULE_singleExpression);
                                    setState(1783);
                                    if (!(precpred(_ctx, 51)))
                                        throw new FailedPredicateException(this, "precpred(_ctx, 51)");
                                    setState(1785);
                                    _errHandler.sync(this);
                                    _la = _input.LA(1);
                                    if (_la == QuestionMarkDot) {
                                        {
                                            setState(1784);
                                            match(QuestionMarkDot);
                                        }
                                    }

                                    setState(1787);
                                    match(OpenBracket);
                                    setState(1788);
                                    expressionSequence();
                                    setState(1789);
                                    match(CloseBracket);
                                }
                                    break;
                                case 20: {
                                    _localctx = new MemberDotExpressionContext(new SingleExpressionContext(_parentctx, _parentState));
                                    pushNewRecursionContext(_localctx, _startState, RULE_singleExpression);
                                    setState(1791);
                                    if (!(precpred(_ctx, 49)))
                                        throw new FailedPredicateException(this, "precpred(_ctx, 49)");
                                    setState(1793);
                                    _errHandler.sync(this);
                                    _la = _input.LA(1);
                                    if (_la == Not) {
                                        {
                                            setState(1792);
                                            match(Not);
                                        }
                                    }

                                    setState(1795);
                                    match(Dot);
                                    setState(1797);
                                    _errHandler.sync(this);
                                    _la = _input.LA(1);
                                    if (_la == Hashtag) {
                                        {
                                            setState(1796);
                                            match(Hashtag);
                                        }
                                    }

                                    setState(1799);
                                    identifierName();
                                    setState(1801);
                                    _errHandler.sync(this);
                                    switch (getInterpreter().adaptivePredict(_input, 232, _ctx)) {
                                        case 1: {
                                            setState(1800);
                                            typeGeneric();
                                        }
                                            break;
                                    }
                                }
                                    break;
                                case 21: {
                                    _localctx = new MemberDotExpressionContext(new SingleExpressionContext(_parentctx, _parentState));
                                    pushNewRecursionContext(_localctx, _startState, RULE_singleExpression);
                                    setState(1803);
                                    if (!(precpred(_ctx, 48)))
                                        throw new FailedPredicateException(this, "precpred(_ctx, 48)");
                                    setState(1805);
                                    _errHandler.sync(this);
                                    _la = _input.LA(1);
                                    if (_la == QuestionMark) {
                                        {
                                            setState(1804);
                                            match(QuestionMark);
                                        }
                                    }

                                    setState(1807);
                                    match(Dot);
                                    setState(1809);
                                    _errHandler.sync(this);
                                    _la = _input.LA(1);
                                    if (_la == Hashtag) {
                                        {
                                            setState(1808);
                                            match(Hashtag);
                                        }
                                    }

                                    setState(1811);
                                    identifierName();
                                    setState(1813);
                                    _errHandler.sync(this);
                                    switch (getInterpreter().adaptivePredict(_input, 235, _ctx)) {
                                        case 1: {
                                            setState(1812);
                                            typeGeneric();
                                        }
                                            break;
                                    }
                                }
                                    break;
                                case 22: {
                                    _localctx = new ArgumentsExpressionContext(new SingleExpressionContext(_parentctx, _parentState));
                                    pushNewRecursionContext(_localctx, _startState, RULE_singleExpression);
                                    setState(1815);
                                    if (!(precpred(_ctx, 45)))
                                        throw new FailedPredicateException(this, "precpred(_ctx, 45)");
                                    setState(1816);
                                    arguments();
                                }
                                    break;
                                case 23: {
                                    _localctx = new PostIncrementExpressionContext(new SingleExpressionContext(_parentctx, _parentState));
                                    pushNewRecursionContext(_localctx, _startState, RULE_singleExpression);
                                    setState(1817);
                                    if (!(precpred(_ctx, 44)))
                                        throw new FailedPredicateException(this, "precpred(_ctx, 44)");
                                    setState(1818);
                                    if (!(this.notLineTerminator()))
                                        throw new FailedPredicateException(this, "this.notLineTerminator()");
                                    setState(1819);
                                    match(PlusPlus);
                                }
                                    break;
                                case 24: {
                                    _localctx = new PostDecreaseExpressionContext(new SingleExpressionContext(_parentctx, _parentState));
                                    pushNewRecursionContext(_localctx, _startState, RULE_singleExpression);
                                    setState(1820);
                                    if (!(precpred(_ctx, 43)))
                                        throw new FailedPredicateException(this, "precpred(_ctx, 43)");
                                    setState(1821);
                                    if (!(this.notLineTerminator()))
                                        throw new FailedPredicateException(this, "this.notLineTerminator()");
                                    setState(1822);
                                    match(MinusMinus);
                                }
                                    break;
                                case 25: {
                                    _localctx = new TemplateStringExpressionContext(new SingleExpressionContext(_parentctx, _parentState));
                                    pushNewRecursionContext(_localctx, _startState, RULE_singleExpression);
                                    setState(1823);
                                    if (!(precpred(_ctx, 15)))
                                        throw new FailedPredicateException(this, "precpred(_ctx, 15)");
                                    setState(1824);
                                    templateStringLiteral();
                                }
                                    break;
                                case 26: {
                                    _localctx = new CastAsExpressionContext(new SingleExpressionContext(_parentctx, _parentState));
                                    pushNewRecursionContext(_localctx, _startState, RULE_singleExpression);
                                    setState(1825);
                                    if (!(precpred(_ctx, 2)))
                                        throw new FailedPredicateException(this, "precpred(_ctx, 2)");
                                    setState(1826);
                                    match(As);
                                    setState(1827);
                                    asExpression();
                                }
                                    break;
                                case 27: {
                                    _localctx =
                                            new NonNullAssertionExpressionContext(new SingleExpressionContext(_parentctx, _parentState));
                                    pushNewRecursionContext(_localctx, _startState, RULE_singleExpression);
                                    setState(1828);
                                    if (!(precpred(_ctx, 1)))
                                        throw new FailedPredicateException(this, "precpred(_ctx, 1)");
                                    setState(1829);
                                    match(Not);
                                }
                                    break;
                            }
                        }
                    }
                    setState(1834);
                    _errHandler.sync(this);
                    _alt = getInterpreter().adaptivePredict(_input, 237, _ctx);
                }
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            unrollRecursionContexts(_parentctx);
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class AsExpressionContext extends ParserRuleContext {
        public PredefinedTypeContext predefinedType() {
            return getRuleContext(PredefinedTypeContext.class, 0);
        }

        public TerminalNode OpenBracket() {
            return getToken(TypeScriptParser.OpenBracket, 0);
        }

        public TerminalNode CloseBracket() {
            return getToken(TypeScriptParser.CloseBracket, 0);
        }

        public SingleExpressionContext singleExpression() {
            return getRuleContext(SingleExpressionContext.class, 0);
        }

        public AsExpressionContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_asExpression;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterAsExpression(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitAsExpression(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitAsExpression(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final AsExpressionContext asExpression() throws RecognitionException {
        AsExpressionContext _localctx = new AsExpressionContext(_ctx, getState());
        enterRule(_localctx, 276, RULE_asExpression);
        try {
            setState(1841);
            _errHandler.sync(this);
            switch (getInterpreter().adaptivePredict(_input, 239, _ctx)) {
                case 1:
                    enterOuterAlt(_localctx, 1); {
                    setState(1835);
                    predefinedType();
                    setState(1838);
                    _errHandler.sync(this);
                    switch (getInterpreter().adaptivePredict(_input, 238, _ctx)) {
                        case 1: {
                            setState(1836);
                            match(OpenBracket);
                            setState(1837);
                            match(CloseBracket);
                        }
                            break;
                    }
                }
                    break;
                case 2:
                    enterOuterAlt(_localctx, 2); {
                    setState(1840);
                    singleExpression(0);
                }
                    break;
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class AssignableContext extends ParserRuleContext {
        public IdentifierContext identifier() {
            return getRuleContext(IdentifierContext.class, 0);
        }

        public KeywordContext keyword() {
            return getRuleContext(KeywordContext.class, 0);
        }

        public ArrayLiteralContext arrayLiteral() {
            return getRuleContext(ArrayLiteralContext.class, 0);
        }

        public ObjectLiteralContext objectLiteral() {
            return getRuleContext(ObjectLiteralContext.class, 0);
        }

        public AssignableContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_assignable;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterAssignable(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitAssignable(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitAssignable(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final AssignableContext assignable() throws RecognitionException {
        AssignableContext _localctx = new AssignableContext(_ctx, getState());
        enterRule(_localctx, 278, RULE_assignable);
        try {
            setState(1847);
            _errHandler.sync(this);
            switch (getInterpreter().adaptivePredict(_input, 240, _ctx)) {
                case 1:
                    enterOuterAlt(_localctx, 1); {
                    setState(1843);
                    identifier();
                }
                    break;
                case 2:
                    enterOuterAlt(_localctx, 2); {
                    setState(1844);
                    keyword();
                }
                    break;
                case 3:
                    enterOuterAlt(_localctx, 3); {
                    setState(1845);
                    arrayLiteral();
                }
                    break;
                case 4:
                    enterOuterAlt(_localctx, 4); {
                    setState(1846);
                    objectLiteral();
                }
                    break;
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class AnonymousFunctionContext extends ParserRuleContext {
        public FunctionDeclarationContext functionDeclaration() {
            return getRuleContext(FunctionDeclarationContext.class, 0);
        }

        public TerminalNode Function_() {
            return getToken(TypeScriptParser.Function_, 0);
        }

        public TerminalNode OpenParen() {
            return getToken(TypeScriptParser.OpenParen, 0);
        }

        public TerminalNode CloseParen() {
            return getToken(TypeScriptParser.CloseParen, 0);
        }

        public TerminalNode OpenBrace() {
            return getToken(TypeScriptParser.OpenBrace, 0);
        }

        public FunctionBodyContext functionBody() {
            return getRuleContext(FunctionBodyContext.class, 0);
        }

        public TerminalNode CloseBrace() {
            return getToken(TypeScriptParser.CloseBrace, 0);
        }

        public TerminalNode Async() {
            return getToken(TypeScriptParser.Async, 0);
        }

        public TerminalNode Multiply() {
            return getToken(TypeScriptParser.Multiply, 0);
        }

        public FormalParameterListContext formalParameterList() {
            return getRuleContext(FormalParameterListContext.class, 0);
        }

        public TypeAnnotationContext typeAnnotation() {
            return getRuleContext(TypeAnnotationContext.class, 0);
        }

        public ArrowFunctionDeclarationContext arrowFunctionDeclaration() {
            return getRuleContext(ArrowFunctionDeclarationContext.class, 0);
        }

        public AnonymousFunctionContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_anonymousFunction;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterAnonymousFunction(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitAnonymousFunction(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitAnonymousFunction(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final AnonymousFunctionContext anonymousFunction() throws RecognitionException {
        AnonymousFunctionContext _localctx = new AnonymousFunctionContext(_ctx, getState());
        enterRule(_localctx, 280, RULE_anonymousFunction);
        int _la;
        try {
            setState(1870);
            _errHandler.sync(this);
            switch (getInterpreter().adaptivePredict(_input, 245, _ctx)) {
                case 1:
                    enterOuterAlt(_localctx, 1); {
                    setState(1849);
                    functionDeclaration();
                }
                    break;
                case 2:
                    enterOuterAlt(_localctx, 2); {
                    setState(1851);
                    _errHandler.sync(this);
                    _la = _input.LA(1);
                    if (_la == Async) {
                        {
                            setState(1850);
                            match(Async);
                        }
                    }

                    setState(1853);
                    match(Function_);
                    setState(1855);
                    _errHandler.sync(this);
                    _la = _input.LA(1);
                    if (_la == Multiply) {
                        {
                            setState(1854);
                            match(Multiply);
                        }
                    }

                    setState(1857);
                    match(OpenParen);
                    setState(1859);
                    _errHandler.sync(this);
                    _la = _input.LA(1);
                    if ((((_la) & ~0x3f) == 0 && ((1L << _la) & 131344L) != 0)
                            || ((((_la - 70)) & ~0x3f) == 0 && ((1L << (_la - 70)) & -4294967297L) != 0)
                            || ((((_la - 135)) & ~0x3f) == 0 && ((1L << (_la - 135)) & 13L) != 0)) {
                        {
                            setState(1858);
                            formalParameterList();
                        }
                    }

                    setState(1861);
                    match(CloseParen);
                    setState(1863);
                    _errHandler.sync(this);
                    _la = _input.LA(1);
                    if (_la == Colon) {
                        {
                            setState(1862);
                            typeAnnotation();
                        }
                    }

                    setState(1865);
                    match(OpenBrace);
                    setState(1866);
                    functionBody();
                    setState(1867);
                    match(CloseBrace);
                }
                    break;
                case 3:
                    enterOuterAlt(_localctx, 3); {
                    setState(1869);
                    arrowFunctionDeclaration();
                }
                    break;
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class ArrowFunctionDeclarationContext extends ParserRuleContext {
        public ArrowFunctionParametersContext arrowFunctionParameters() {
            return getRuleContext(ArrowFunctionParametersContext.class, 0);
        }

        public TerminalNode ARROW() {
            return getToken(TypeScriptParser.ARROW, 0);
        }

        public ArrowFunctionBodyContext arrowFunctionBody() {
            return getRuleContext(ArrowFunctionBodyContext.class, 0);
        }

        public TerminalNode Async() {
            return getToken(TypeScriptParser.Async, 0);
        }

        public TypeAnnotationContext typeAnnotation() {
            return getRuleContext(TypeAnnotationContext.class, 0);
        }

        public ArrowFunctionDeclarationContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_arrowFunctionDeclaration;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterArrowFunctionDeclaration(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitArrowFunctionDeclaration(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitArrowFunctionDeclaration(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final ArrowFunctionDeclarationContext arrowFunctionDeclaration() throws RecognitionException {
        ArrowFunctionDeclarationContext _localctx = new ArrowFunctionDeclarationContext(_ctx, getState());
        enterRule(_localctx, 282, RULE_arrowFunctionDeclaration);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(1873);
                _errHandler.sync(this);
                switch (getInterpreter().adaptivePredict(_input, 246, _ctx)) {
                    case 1: {
                        setState(1872);
                        match(Async);
                    }
                        break;
                }
                setState(1875);
                arrowFunctionParameters();
                setState(1877);
                _errHandler.sync(this);
                _la = _input.LA(1);
                if (_la == Colon) {
                    {
                        setState(1876);
                        typeAnnotation();
                    }
                }

                setState(1879);
                match(ARROW);
                setState(1880);
                arrowFunctionBody();
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class ArrowFunctionParametersContext extends ParserRuleContext {
        public PropertyNameContext propertyName() {
            return getRuleContext(PropertyNameContext.class, 0);
        }

        public TerminalNode OpenParen() {
            return getToken(TypeScriptParser.OpenParen, 0);
        }

        public TerminalNode CloseParen() {
            return getToken(TypeScriptParser.CloseParen, 0);
        }

        public FormalParameterListContext formalParameterList() {
            return getRuleContext(FormalParameterListContext.class, 0);
        }

        public ArrowFunctionParametersContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_arrowFunctionParameters;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterArrowFunctionParameters(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitArrowFunctionParameters(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitArrowFunctionParameters(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final ArrowFunctionParametersContext arrowFunctionParameters() throws RecognitionException {
        ArrowFunctionParametersContext _localctx = new ArrowFunctionParametersContext(_ctx, getState());
        enterRule(_localctx, 284, RULE_arrowFunctionParameters);
        int _la;
        try {
            setState(1888);
            _errHandler.sync(this);
            switch (_input.LA(1)) {
                case OpenBracket:
                case NullLiteral:
                case BooleanLiteral:
                case DecimalLiteral:
                case HexIntegerLiteral:
                case OctalIntegerLiteral:
                case OctalIntegerLiteral2:
                case BinaryIntegerLiteral:
                case Break:
                case Do:
                case Instanceof:
                case Typeof:
                case Case:
                case Else:
                case New:
                case Var:
                case Catch:
                case Finally:
                case Return:
                case Void:
                case Continue:
                case For:
                case Switch:
                case While:
                case Debugger:
                case Function_:
                case This:
                case With:
                case Default:
                case If:
                case Throw:
                case Delete:
                case In:
                case Try:
                case As:
                case From:
                case ReadOnly:
                case Async:
                case Await:
                case Yield:
                case Class:
                case Enum:
                case Extends:
                case Super:
                case Const:
                case Export:
                case Import:
                case Implements:
                case Let:
                case Private:
                case Public:
                case Interface:
                case Package:
                case Protected:
                case Static:
                case Any:
                case Number:
                case Never:
                case Boolean:
                case String:
                case Unique:
                case Symbol:
                case Undefined:
                case Object:
                case Of:
                case KeyOf:
                case TypeAlias:
                case Constructor:
                case Namespace:
                case Require:
                case Module:
                case Abstract:
                case Identifier:
                case StringLiteral:
                    enterOuterAlt(_localctx, 1); {
                    setState(1882);
                    propertyName();
                }
                    break;
                case OpenParen:
                    enterOuterAlt(_localctx, 2); {
                    setState(1883);
                    match(OpenParen);
                    setState(1885);
                    _errHandler.sync(this);
                    _la = _input.LA(1);
                    if ((((_la) & ~0x3f) == 0 && ((1L << _la) & 131344L) != 0)
                            || ((((_la - 70)) & ~0x3f) == 0 && ((1L << (_la - 70)) & -4294967297L) != 0)
                            || ((((_la - 135)) & ~0x3f) == 0 && ((1L << (_la - 135)) & 13L) != 0)) {
                        {
                            setState(1884);
                            formalParameterList();
                        }
                    }

                    setState(1887);
                    match(CloseParen);
                }
                    break;
                default:
                    throw new NoViableAltException(this);
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class ArrowFunctionBodyContext extends ParserRuleContext {
        public SingleExpressionContext singleExpression() {
            return getRuleContext(SingleExpressionContext.class, 0);
        }

        public TerminalNode OpenBrace() {
            return getToken(TypeScriptParser.OpenBrace, 0);
        }

        public FunctionBodyContext functionBody() {
            return getRuleContext(FunctionBodyContext.class, 0);
        }

        public TerminalNode CloseBrace() {
            return getToken(TypeScriptParser.CloseBrace, 0);
        }

        public ArrowFunctionBodyContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_arrowFunctionBody;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterArrowFunctionBody(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitArrowFunctionBody(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitArrowFunctionBody(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final ArrowFunctionBodyContext arrowFunctionBody() throws RecognitionException {
        ArrowFunctionBodyContext _localctx = new ArrowFunctionBodyContext(_ctx, getState());
        enterRule(_localctx, 286, RULE_arrowFunctionBody);
        try {
            setState(1895);
            _errHandler.sync(this);
            switch (getInterpreter().adaptivePredict(_input, 250, _ctx)) {
                case 1:
                    enterOuterAlt(_localctx, 1); {
                    setState(1890);
                    singleExpression(0);
                }
                    break;
                case 2:
                    enterOuterAlt(_localctx, 2); {
                    setState(1891);
                    match(OpenBrace);
                    setState(1892);
                    functionBody();
                    setState(1893);
                    match(CloseBrace);
                }
                    break;
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class AssignmentOperatorContext extends ParserRuleContext {
        public TerminalNode MultiplyAssign() {
            return getToken(TypeScriptParser.MultiplyAssign, 0);
        }

        public TerminalNode DivideAssign() {
            return getToken(TypeScriptParser.DivideAssign, 0);
        }

        public TerminalNode ModulusAssign() {
            return getToken(TypeScriptParser.ModulusAssign, 0);
        }

        public TerminalNode PlusAssign() {
            return getToken(TypeScriptParser.PlusAssign, 0);
        }

        public TerminalNode MinusAssign() {
            return getToken(TypeScriptParser.MinusAssign, 0);
        }

        public TerminalNode LeftShiftArithmeticAssign() {
            return getToken(TypeScriptParser.LeftShiftArithmeticAssign, 0);
        }

        public TerminalNode RightShiftArithmeticAssign() {
            return getToken(TypeScriptParser.RightShiftArithmeticAssign, 0);
        }

        public TerminalNode RightShiftLogicalAssign() {
            return getToken(TypeScriptParser.RightShiftLogicalAssign, 0);
        }

        public TerminalNode BitAndAssign() {
            return getToken(TypeScriptParser.BitAndAssign, 0);
        }

        public TerminalNode BitXorAssign() {
            return getToken(TypeScriptParser.BitXorAssign, 0);
        }

        public TerminalNode BitOrAssign() {
            return getToken(TypeScriptParser.BitOrAssign, 0);
        }

        public TerminalNode PowerAssign() {
            return getToken(TypeScriptParser.PowerAssign, 0);
        }

        public TerminalNode NullishCoalescingAssign() {
            return getToken(TypeScriptParser.NullishCoalescingAssign, 0);
        }

        public AssignmentOperatorContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_assignmentOperator;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterAssignmentOperator(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitAssignmentOperator(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitAssignmentOperator(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final AssignmentOperatorContext assignmentOperator() throws RecognitionException {
        AssignmentOperatorContext _localctx = new AssignmentOperatorContext(_ctx, getState());
        enterRule(_localctx, 288, RULE_assignmentOperator);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(1897);
                _la = _input.LA(1);
                if (!((((_la) & ~0x3f) == 0 && ((1L << _la) & 288195191779622912L) != 0))) {
                    _errHandler.recoverInline(this);
                } else {
                    if (_input.LA(1) == Token.EOF)
                        matchedEOF = true;
                    _errHandler.reportMatch(this);
                    consume();
                }
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class LiteralContext extends ParserRuleContext {
        public TerminalNode NullLiteral() {
            return getToken(TypeScriptParser.NullLiteral, 0);
        }

        public TerminalNode BooleanLiteral() {
            return getToken(TypeScriptParser.BooleanLiteral, 0);
        }

        public TerminalNode StringLiteral() {
            return getToken(TypeScriptParser.StringLiteral, 0);
        }

        public TemplateStringLiteralContext templateStringLiteral() {
            return getRuleContext(TemplateStringLiteralContext.class, 0);
        }

        public TerminalNode RegularExpressionLiteral() {
            return getToken(TypeScriptParser.RegularExpressionLiteral, 0);
        }

        public NumericLiteralContext numericLiteral() {
            return getRuleContext(NumericLiteralContext.class, 0);
        }

        public BigintLiteralContext bigintLiteral() {
            return getRuleContext(BigintLiteralContext.class, 0);
        }

        public LiteralContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_literal;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterLiteral(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitLiteral(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitLiteral(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final LiteralContext literal() throws RecognitionException {
        LiteralContext _localctx = new LiteralContext(_ctx, getState());
        enterRule(_localctx, 290, RULE_literal);
        try {
            setState(1906);
            _errHandler.sync(this);
            switch (_input.LA(1)) {
                case NullLiteral:
                    enterOuterAlt(_localctx, 1); {
                    setState(1899);
                    match(NullLiteral);
                }
                    break;
                case BooleanLiteral:
                    enterOuterAlt(_localctx, 2); {
                    setState(1900);
                    match(BooleanLiteral);
                }
                    break;
                case StringLiteral:
                    enterOuterAlt(_localctx, 3); {
                    setState(1901);
                    match(StringLiteral);
                }
                    break;
                case BackTick:
                    enterOuterAlt(_localctx, 4); {
                    setState(1902);
                    templateStringLiteral();
                }
                    break;
                case RegularExpressionLiteral:
                    enterOuterAlt(_localctx, 5); {
                    setState(1903);
                    match(RegularExpressionLiteral);
                }
                    break;
                case DecimalLiteral:
                case HexIntegerLiteral:
                case OctalIntegerLiteral:
                case OctalIntegerLiteral2:
                case BinaryIntegerLiteral:
                    enterOuterAlt(_localctx, 6); {
                    setState(1904);
                    numericLiteral();
                }
                    break;
                case BigHexIntegerLiteral:
                case BigOctalIntegerLiteral:
                case BigBinaryIntegerLiteral:
                case BigDecimalIntegerLiteral:
                    enterOuterAlt(_localctx, 7); {
                    setState(1905);
                    bigintLiteral();
                }
                    break;
                default:
                    throw new NoViableAltException(this);
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class TemplateStringLiteralContext extends ParserRuleContext {
        public List<TerminalNode> BackTick() {
            return getTokens(TypeScriptParser.BackTick);
        }

        public TerminalNode BackTick(int i) {
            return getToken(TypeScriptParser.BackTick, i);
        }

        public List<TemplateStringAtomContext> templateStringAtom() {
            return getRuleContexts(TemplateStringAtomContext.class);
        }

        public TemplateStringAtomContext templateStringAtom(int i) {
            return getRuleContext(TemplateStringAtomContext.class, i);
        }

        public TemplateStringLiteralContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_templateStringLiteral;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterTemplateStringLiteral(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitTemplateStringLiteral(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitTemplateStringLiteral(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final TemplateStringLiteralContext templateStringLiteral() throws RecognitionException {
        TemplateStringLiteralContext _localctx = new TemplateStringLiteralContext(_ctx, getState());
        enterRule(_localctx, 292, RULE_templateStringLiteral);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(1908);
                match(BackTick);
                setState(1912);
                _errHandler.sync(this);
                _la = _input.LA(1);
                while (((((_la - 146)) & ~0x3f) == 0 && ((1L << (_la - 146)) & 7L) != 0)) {
                    {
                        {
                            setState(1909);
                            templateStringAtom();
                        }
                    }
                    setState(1914);
                    _errHandler.sync(this);
                    _la = _input.LA(1);
                }
                setState(1915);
                match(BackTick);
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class TemplateStringAtomContext extends ParserRuleContext {
        public TerminalNode TemplateStringAtom() {
            return getToken(TypeScriptParser.TemplateStringAtom, 0);
        }

        public TerminalNode TemplateStringStartExpression() {
            return getToken(TypeScriptParser.TemplateStringStartExpression, 0);
        }

        public SingleExpressionContext singleExpression() {
            return getRuleContext(SingleExpressionContext.class, 0);
        }

        public TerminalNode TemplateCloseBrace() {
            return getToken(TypeScriptParser.TemplateCloseBrace, 0);
        }

        public TerminalNode TemplateStringEscapeAtom() {
            return getToken(TypeScriptParser.TemplateStringEscapeAtom, 0);
        }

        public TemplateStringAtomContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_templateStringAtom;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterTemplateStringAtom(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitTemplateStringAtom(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitTemplateStringAtom(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final TemplateStringAtomContext templateStringAtom() throws RecognitionException {
        TemplateStringAtomContext _localctx = new TemplateStringAtomContext(_ctx, getState());
        enterRule(_localctx, 294, RULE_templateStringAtom);
        try {
            setState(1923);
            _errHandler.sync(this);
            switch (_input.LA(1)) {
                case TemplateStringAtom:
                    enterOuterAlt(_localctx, 1); {
                    setState(1917);
                    match(TemplateStringAtom);
                }
                    break;
                case TemplateStringStartExpression:
                    enterOuterAlt(_localctx, 2); {
                    setState(1918);
                    match(TemplateStringStartExpression);
                    setState(1919);
                    singleExpression(0);
                    setState(1920);
                    match(TemplateCloseBrace);
                }
                    break;
                case TemplateStringEscapeAtom:
                    enterOuterAlt(_localctx, 3); {
                    setState(1922);
                    match(TemplateStringEscapeAtom);
                }
                    break;
                default:
                    throw new NoViableAltException(this);
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class NumericLiteralContext extends ParserRuleContext {
        public TerminalNode DecimalLiteral() {
            return getToken(TypeScriptParser.DecimalLiteral, 0);
        }

        public TerminalNode HexIntegerLiteral() {
            return getToken(TypeScriptParser.HexIntegerLiteral, 0);
        }

        public TerminalNode OctalIntegerLiteral() {
            return getToken(TypeScriptParser.OctalIntegerLiteral, 0);
        }

        public TerminalNode OctalIntegerLiteral2() {
            return getToken(TypeScriptParser.OctalIntegerLiteral2, 0);
        }

        public TerminalNode BinaryIntegerLiteral() {
            return getToken(TypeScriptParser.BinaryIntegerLiteral, 0);
        }

        public NumericLiteralContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_numericLiteral;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterNumericLiteral(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitNumericLiteral(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitNumericLiteral(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final NumericLiteralContext numericLiteral() throws RecognitionException {
        NumericLiteralContext _localctx = new NumericLiteralContext(_ctx, getState());
        enterRule(_localctx, 296, RULE_numericLiteral);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(1925);
                _la = _input.LA(1);
                if (!(((((_la - 61)) & ~0x3f) == 0 && ((1L << (_la - 61)) & 31L) != 0))) {
                    _errHandler.recoverInline(this);
                } else {
                    if (_input.LA(1) == Token.EOF)
                        matchedEOF = true;
                    _errHandler.reportMatch(this);
                    consume();
                }
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class BigintLiteralContext extends ParserRuleContext {
        public TerminalNode BigDecimalIntegerLiteral() {
            return getToken(TypeScriptParser.BigDecimalIntegerLiteral, 0);
        }

        public TerminalNode BigHexIntegerLiteral() {
            return getToken(TypeScriptParser.BigHexIntegerLiteral, 0);
        }

        public TerminalNode BigOctalIntegerLiteral() {
            return getToken(TypeScriptParser.BigOctalIntegerLiteral, 0);
        }

        public TerminalNode BigBinaryIntegerLiteral() {
            return getToken(TypeScriptParser.BigBinaryIntegerLiteral, 0);
        }

        public BigintLiteralContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_bigintLiteral;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterBigintLiteral(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitBigintLiteral(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitBigintLiteral(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final BigintLiteralContext bigintLiteral() throws RecognitionException {
        BigintLiteralContext _localctx = new BigintLiteralContext(_ctx, getState());
        enterRule(_localctx, 298, RULE_bigintLiteral);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(1927);
                _la = _input.LA(1);
                if (!(((((_la - 66)) & ~0x3f) == 0 && ((1L << (_la - 66)) & 15L) != 0))) {
                    _errHandler.recoverInline(this);
                } else {
                    if (_input.LA(1) == Token.EOF)
                        matchedEOF = true;
                    _errHandler.reportMatch(this);
                    consume();
                }
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class GetterContext extends ParserRuleContext {
        public IdentifierContext identifier() {
            return getRuleContext(IdentifierContext.class, 0);
        }

        public ClassElementNameContext classElementName() {
            return getRuleContext(ClassElementNameContext.class, 0);
        }

        public GetterContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_getter;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterGetter(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitGetter(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitGetter(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final GetterContext getter() throws RecognitionException {
        GetterContext _localctx = new GetterContext(_ctx, getState());
        enterRule(_localctx, 300, RULE_getter);
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(1929);
                if (!(this.n("get")))
                    throw new FailedPredicateException(this, "this.n(\"get\")");
                setState(1930);
                identifier();
                setState(1931);
                classElementName();
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class SetterContext extends ParserRuleContext {
        public IdentifierContext identifier() {
            return getRuleContext(IdentifierContext.class, 0);
        }

        public ClassElementNameContext classElementName() {
            return getRuleContext(ClassElementNameContext.class, 0);
        }

        public SetterContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_setter;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterSetter(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitSetter(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitSetter(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final SetterContext setter() throws RecognitionException {
        SetterContext _localctx = new SetterContext(_ctx, getState());
        enterRule(_localctx, 302, RULE_setter);
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(1933);
                if (!(this.n("set")))
                    throw new FailedPredicateException(this, "this.n(\"set\")");
                setState(1934);
                identifier();
                setState(1935);
                classElementName();
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class IdentifierNameContext extends ParserRuleContext {
        public IdentifierContext identifier() {
            return getRuleContext(IdentifierContext.class, 0);
        }

        public ReservedWordContext reservedWord() {
            return getRuleContext(ReservedWordContext.class, 0);
        }

        public IdentifierNameContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_identifierName;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterIdentifierName(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitIdentifierName(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitIdentifierName(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final IdentifierNameContext identifierName() throws RecognitionException {
        IdentifierNameContext _localctx = new IdentifierNameContext(_ctx, getState());
        enterRule(_localctx, 304, RULE_identifierName);
        try {
            setState(1939);
            _errHandler.sync(this);
            switch (getInterpreter().adaptivePredict(_input, 254, _ctx)) {
                case 1:
                    enterOuterAlt(_localctx, 1); {
                    setState(1937);
                    identifier();
                }
                    break;
                case 2:
                    enterOuterAlt(_localctx, 2); {
                    setState(1938);
                    reservedWord();
                }
                    break;
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class IdentifierContext extends ParserRuleContext {
        public TerminalNode Identifier() {
            return getToken(TypeScriptParser.Identifier, 0);
        }

        public TerminalNode Async() {
            return getToken(TypeScriptParser.Async, 0);
        }

        public TerminalNode As() {
            return getToken(TypeScriptParser.As, 0);
        }

        public TerminalNode From() {
            return getToken(TypeScriptParser.From, 0);
        }

        public TerminalNode Yield() {
            return getToken(TypeScriptParser.Yield, 0);
        }

        public TerminalNode Of() {
            return getToken(TypeScriptParser.Of, 0);
        }

        public TerminalNode Any() {
            return getToken(TypeScriptParser.Any, 0);
        }

        public TerminalNode Number() {
            return getToken(TypeScriptParser.Number, 0);
        }

        public TerminalNode Boolean() {
            return getToken(TypeScriptParser.Boolean, 0);
        }

        public TerminalNode String() {
            return getToken(TypeScriptParser.String, 0);
        }

        public TerminalNode Unique() {
            return getToken(TypeScriptParser.Unique, 0);
        }

        public TerminalNode Symbol() {
            return getToken(TypeScriptParser.Symbol, 0);
        }

        public TerminalNode Never() {
            return getToken(TypeScriptParser.Never, 0);
        }

        public TerminalNode Undefined() {
            return getToken(TypeScriptParser.Undefined, 0);
        }

        public TerminalNode Object() {
            return getToken(TypeScriptParser.Object, 0);
        }

        public TerminalNode KeyOf() {
            return getToken(TypeScriptParser.KeyOf, 0);
        }

        public TerminalNode TypeAlias() {
            return getToken(TypeScriptParser.TypeAlias, 0);
        }

        public TerminalNode Constructor() {
            return getToken(TypeScriptParser.Constructor, 0);
        }

        public TerminalNode Namespace() {
            return getToken(TypeScriptParser.Namespace, 0);
        }

        public TerminalNode Abstract() {
            return getToken(TypeScriptParser.Abstract, 0);
        }

        public IdentifierContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_identifier;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterIdentifier(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitIdentifier(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitIdentifier(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final IdentifierContext identifier() throws RecognitionException {
        IdentifierContext _localctx = new IdentifierContext(_ctx, getState());
        enterRule(_localctx, 306, RULE_identifier);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(1941);
                _la = _input.LA(1);
                if (!(((((_la - 96)) & ~0x3f) == 0 && ((1L << (_la - 96)) & 5016517607467L) != 0))) {
                    _errHandler.recoverInline(this);
                } else {
                    if (_input.LA(1) == Token.EOF)
                        matchedEOF = true;
                    _errHandler.reportMatch(this);
                    consume();
                }
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class IdentifierOrKeyWordContext extends ParserRuleContext {
        public IdentifierContext identifier() {
            return getRuleContext(IdentifierContext.class, 0);
        }

        public TerminalNode TypeAlias() {
            return getToken(TypeScriptParser.TypeAlias, 0);
        }

        public TerminalNode Require() {
            return getToken(TypeScriptParser.Require, 0);
        }

        public IdentifierOrKeyWordContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_identifierOrKeyWord;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterIdentifierOrKeyWord(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitIdentifierOrKeyWord(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitIdentifierOrKeyWord(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final IdentifierOrKeyWordContext identifierOrKeyWord() throws RecognitionException {
        IdentifierOrKeyWordContext _localctx = new IdentifierOrKeyWordContext(_ctx, getState());
        enterRule(_localctx, 308, RULE_identifierOrKeyWord);
        try {
            setState(1946);
            _errHandler.sync(this);
            switch (getInterpreter().adaptivePredict(_input, 255, _ctx)) {
                case 1:
                    enterOuterAlt(_localctx, 1); {
                    setState(1943);
                    identifier();
                }
                    break;
                case 2:
                    enterOuterAlt(_localctx, 2); {
                    setState(1944);
                    match(TypeAlias);
                }
                    break;
                case 3:
                    enterOuterAlt(_localctx, 3); {
                    setState(1945);
                    match(Require);
                }
                    break;
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class ReservedWordContext extends ParserRuleContext {
        public KeywordContext keyword() {
            return getRuleContext(KeywordContext.class, 0);
        }

        public TerminalNode NullLiteral() {
            return getToken(TypeScriptParser.NullLiteral, 0);
        }

        public TerminalNode BooleanLiteral() {
            return getToken(TypeScriptParser.BooleanLiteral, 0);
        }

        public ReservedWordContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_reservedWord;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterReservedWord(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitReservedWord(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitReservedWord(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final ReservedWordContext reservedWord() throws RecognitionException {
        ReservedWordContext _localctx = new ReservedWordContext(_ctx, getState());
        enterRule(_localctx, 310, RULE_reservedWord);
        try {
            setState(1951);
            _errHandler.sync(this);
            switch (_input.LA(1)) {
                case Break:
                case Do:
                case Instanceof:
                case Typeof:
                case Case:
                case Else:
                case New:
                case Var:
                case Catch:
                case Finally:
                case Return:
                case Void:
                case Continue:
                case For:
                case Switch:
                case While:
                case Debugger:
                case Function_:
                case This:
                case With:
                case Default:
                case If:
                case Throw:
                case Delete:
                case In:
                case Try:
                case As:
                case From:
                case ReadOnly:
                case Async:
                case Await:
                case Yield:
                case Class:
                case Enum:
                case Extends:
                case Super:
                case Const:
                case Export:
                case Import:
                case Implements:
                case Let:
                case Private:
                case Public:
                case Interface:
                case Package:
                case Protected:
                case Static:
                case Number:
                case Boolean:
                case String:
                case TypeAlias:
                case Require:
                case Module:
                    enterOuterAlt(_localctx, 1); {
                    setState(1948);
                    keyword();
                }
                    break;
                case NullLiteral:
                    enterOuterAlt(_localctx, 2); {
                    setState(1949);
                    match(NullLiteral);
                }
                    break;
                case BooleanLiteral:
                    enterOuterAlt(_localctx, 3); {
                    setState(1950);
                    match(BooleanLiteral);
                }
                    break;
                default:
                    throw new NoViableAltException(this);
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class KeywordContext extends ParserRuleContext {
        public TerminalNode Break() {
            return getToken(TypeScriptParser.Break, 0);
        }

        public TerminalNode Do() {
            return getToken(TypeScriptParser.Do, 0);
        }

        public TerminalNode Instanceof() {
            return getToken(TypeScriptParser.Instanceof, 0);
        }

        public TerminalNode Typeof() {
            return getToken(TypeScriptParser.Typeof, 0);
        }

        public TerminalNode Case() {
            return getToken(TypeScriptParser.Case, 0);
        }

        public TerminalNode Else() {
            return getToken(TypeScriptParser.Else, 0);
        }

        public TerminalNode New() {
            return getToken(TypeScriptParser.New, 0);
        }

        public TerminalNode Var() {
            return getToken(TypeScriptParser.Var, 0);
        }

        public TerminalNode Catch() {
            return getToken(TypeScriptParser.Catch, 0);
        }

        public TerminalNode Finally() {
            return getToken(TypeScriptParser.Finally, 0);
        }

        public TerminalNode Return() {
            return getToken(TypeScriptParser.Return, 0);
        }

        public TerminalNode Void() {
            return getToken(TypeScriptParser.Void, 0);
        }

        public TerminalNode Continue() {
            return getToken(TypeScriptParser.Continue, 0);
        }

        public TerminalNode For() {
            return getToken(TypeScriptParser.For, 0);
        }

        public TerminalNode Switch() {
            return getToken(TypeScriptParser.Switch, 0);
        }

        public TerminalNode While() {
            return getToken(TypeScriptParser.While, 0);
        }

        public TerminalNode Debugger() {
            return getToken(TypeScriptParser.Debugger, 0);
        }

        public TerminalNode Function_() {
            return getToken(TypeScriptParser.Function_, 0);
        }

        public TerminalNode This() {
            return getToken(TypeScriptParser.This, 0);
        }

        public TerminalNode With() {
            return getToken(TypeScriptParser.With, 0);
        }

        public TerminalNode Default() {
            return getToken(TypeScriptParser.Default, 0);
        }

        public TerminalNode If() {
            return getToken(TypeScriptParser.If, 0);
        }

        public TerminalNode Throw() {
            return getToken(TypeScriptParser.Throw, 0);
        }

        public TerminalNode Delete() {
            return getToken(TypeScriptParser.Delete, 0);
        }

        public TerminalNode In() {
            return getToken(TypeScriptParser.In, 0);
        }

        public TerminalNode Try() {
            return getToken(TypeScriptParser.Try, 0);
        }

        public TerminalNode Class() {
            return getToken(TypeScriptParser.Class, 0);
        }

        public TerminalNode Enum() {
            return getToken(TypeScriptParser.Enum, 0);
        }

        public TerminalNode Extends() {
            return getToken(TypeScriptParser.Extends, 0);
        }

        public TerminalNode Super() {
            return getToken(TypeScriptParser.Super, 0);
        }

        public TerminalNode Const() {
            return getToken(TypeScriptParser.Const, 0);
        }

        public TerminalNode Export() {
            return getToken(TypeScriptParser.Export, 0);
        }

        public TerminalNode Import() {
            return getToken(TypeScriptParser.Import, 0);
        }

        public TerminalNode Implements() {
            return getToken(TypeScriptParser.Implements, 0);
        }

        public TerminalNode Let() {
            return getToken(TypeScriptParser.Let, 0);
        }

        public TerminalNode Private() {
            return getToken(TypeScriptParser.Private, 0);
        }

        public TerminalNode Public() {
            return getToken(TypeScriptParser.Public, 0);
        }

        public TerminalNode Interface() {
            return getToken(TypeScriptParser.Interface, 0);
        }

        public TerminalNode Package() {
            return getToken(TypeScriptParser.Package, 0);
        }

        public TerminalNode Protected() {
            return getToken(TypeScriptParser.Protected, 0);
        }

        public TerminalNode Static() {
            return getToken(TypeScriptParser.Static, 0);
        }

        public TerminalNode Yield() {
            return getToken(TypeScriptParser.Yield, 0);
        }

        public TerminalNode Async() {
            return getToken(TypeScriptParser.Async, 0);
        }

        public TerminalNode Await() {
            return getToken(TypeScriptParser.Await, 0);
        }

        public TerminalNode ReadOnly() {
            return getToken(TypeScriptParser.ReadOnly, 0);
        }

        public TerminalNode From() {
            return getToken(TypeScriptParser.From, 0);
        }

        public TerminalNode As() {
            return getToken(TypeScriptParser.As, 0);
        }

        public TerminalNode Require() {
            return getToken(TypeScriptParser.Require, 0);
        }

        public TerminalNode TypeAlias() {
            return getToken(TypeScriptParser.TypeAlias, 0);
        }

        public TerminalNode String() {
            return getToken(TypeScriptParser.String, 0);
        }

        public TerminalNode Boolean() {
            return getToken(TypeScriptParser.Boolean, 0);
        }

        public TerminalNode Number() {
            return getToken(TypeScriptParser.Number, 0);
        }

        public TerminalNode Module() {
            return getToken(TypeScriptParser.Module, 0);
        }

        public KeywordContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_keyword;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterKeyword(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitKeyword(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitKeyword(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final KeywordContext keyword() throws RecognitionException {
        KeywordContext _localctx = new KeywordContext(_ctx, getState());
        enterRule(_localctx, 312, RULE_keyword);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(1953);
                _la = _input.LA(1);
                if (!(((((_la - 70)) & ~0x3f) == 0 && ((1L << (_la - 70)) & -4027625446047744001L) != 0))) {
                    _errHandler.recoverInline(this);
                } else {
                    if (_input.LA(1) == Token.EOF)
                        matchedEOF = true;
                    _errHandler.reportMatch(this);
                    consume();
                }
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class EosContext extends ParserRuleContext {
        public TerminalNode SemiColon() {
            return getToken(TypeScriptParser.SemiColon, 0);
        }

        public TerminalNode EOF() {
            return getToken(TypeScriptParser.EOF, 0);
        }

        public EosContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_eos;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).enterEos(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof TypeScriptParserListener)
                ((TypeScriptParserListener) listener).exitEos(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof TypeScriptParserVisitor)
                return ((TypeScriptParserVisitor<? extends T>) visitor).visitEos(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final EosContext eos() throws RecognitionException {
        EosContext _localctx = new EosContext(_ctx, getState());
        enterRule(_localctx, 314, RULE_eos);
        try {
            setState(1959);
            _errHandler.sync(this);
            switch (getInterpreter().adaptivePredict(_input, 257, _ctx)) {
                case 1:
                    enterOuterAlt(_localctx, 1); {
                    setState(1955);
                    match(SemiColon);
                }
                    break;
                case 2:
                    enterOuterAlt(_localctx, 2); {
                    setState(1956);
                    match(EOF);
                }
                    break;
                case 3:
                    enterOuterAlt(_localctx, 3); {
                    setState(1957);
                    if (!(this.lineTerminatorAhead()))
                        throw new FailedPredicateException(this, "this.lineTerminatorAhead()");
                }
                    break;
                case 4:
                    enterOuterAlt(_localctx, 4); {
                    setState(1958);
                    if (!(this.closeBrace()))
                        throw new FailedPredicateException(this, "this.closeBrace()");
                }
                    break;
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    public boolean sempred(RuleContext _localctx, int ruleIndex, int predIndex) {
        switch (ruleIndex) {
            case 10:
                return unionOrIntersectionOrPrimaryType_sempred((UnionOrIntersectionOrPrimaryTypeContext) _localctx, predIndex);
            case 11:
                return primaryType_sempred((PrimaryTypeContext) _localctx, predIndex);
            case 20:
                return arrayType_sempred((ArrayTypeContext) _localctx, predIndex);
            case 55:
                return decoratorMemberExpression_sempred((DecoratorMemberExpressionContext) _localctx, predIndex);
            case 82:
                return expressionStatement_sempred((ExpressionStatementContext) _localctx, predIndex);
            case 84:
                return iterationStatement_sempred((IterationStatementContext) _localctx, predIndex);
            case 86:
                return continueStatement_sempred((ContinueStatementContext) _localctx, predIndex);
            case 87:
                return breakStatement_sempred((BreakStatementContext) _localctx, predIndex);
            case 88:
                return returnStatement_sempred((ReturnStatementContext) _localctx, predIndex);
            case 89:
                return yieldStatement_sempred((YieldStatementContext) _localctx, predIndex);
            case 97:
                return throwStatement_sempred((ThrowStatementContext) _localctx, predIndex);
            case 112:
                return generatorMethod_sempred((GeneratorMethodContext) _localctx, predIndex);
            case 137:
                return singleExpression_sempred((SingleExpressionContext) _localctx, predIndex);
            case 150:
                return getter_sempred((GetterContext) _localctx, predIndex);
            case 151:
                return setter_sempred((SetterContext) _localctx, predIndex);
            case 157:
                return eos_sempred((EosContext) _localctx, predIndex);
        }
        return true;
    }

    private boolean unionOrIntersectionOrPrimaryType_sempred(UnionOrIntersectionOrPrimaryTypeContext _localctx, int predIndex) {
        switch (predIndex) {
            case 0:
                return precpred(_ctx, 3);
            case 1:
                return precpred(_ctx, 2);
        }
        return true;
    }

    private boolean primaryType_sempred(PrimaryTypeContext _localctx, int predIndex) {
        switch (predIndex) {
            case 2:
                return precpred(_ctx, 6);
            case 3:
                return this.notLineTerminator();
        }
        return true;
    }

    private boolean arrayType_sempred(ArrayTypeContext _localctx, int predIndex) {
        switch (predIndex) {
            case 4:
                return this.notLineTerminator();
        }
        return true;
    }

    private boolean decoratorMemberExpression_sempred(DecoratorMemberExpressionContext _localctx, int predIndex) {
        switch (predIndex) {
            case 5:
                return precpred(_ctx, 2);
        }
        return true;
    }

    private boolean expressionStatement_sempred(ExpressionStatementContext _localctx, int predIndex) {
        switch (predIndex) {
            case 6:
                return this.notOpenBraceAndNotFunctionAndNotInterface();
        }
        return true;
    }

    private boolean iterationStatement_sempred(IterationStatementContext _localctx, int predIndex) {
        switch (predIndex) {
            case 7:
                return this.p("of");
            case 8:
                return this.p("of");
        }
        return true;
    }

    private boolean continueStatement_sempred(ContinueStatementContext _localctx, int predIndex) {
        switch (predIndex) {
            case 9:
                return this.notLineTerminator();
        }
        return true;
    }

    private boolean breakStatement_sempred(BreakStatementContext _localctx, int predIndex) {
        switch (predIndex) {
            case 10:
                return this.notLineTerminator();
        }
        return true;
    }

    private boolean returnStatement_sempred(ReturnStatementContext _localctx, int predIndex) {
        switch (predIndex) {
            case 11:
                return this.notLineTerminator();
        }
        return true;
    }

    private boolean yieldStatement_sempred(YieldStatementContext _localctx, int predIndex) {
        switch (predIndex) {
            case 12:
                return this.notLineTerminator();
        }
        return true;
    }

    private boolean throwStatement_sempred(ThrowStatementContext _localctx, int predIndex) {
        switch (predIndex) {
            case 13:
                return this.notLineTerminator();
        }
        return true;
    }

    private boolean generatorMethod_sempred(GeneratorMethodContext _localctx, int predIndex) {
        switch (predIndex) {
            case 14:
                return this.notLineTerminator();
        }
        return true;
    }

    private boolean singleExpression_sempred(SingleExpressionContext _localctx, int predIndex) {
        switch (predIndex) {
            case 15:
                return precpred(_ctx, 50);
            case 16:
                return precpred(_ctx, 32);
            case 17:
                return precpred(_ctx, 31);
            case 18:
                return precpred(_ctx, 30);
            case 19:
                return precpred(_ctx, 29);
            case 20:
                return precpred(_ctx, 28);
            case 21:
                return precpred(_ctx, 27);
            case 22:
                return precpred(_ctx, 26);
            case 23:
                return precpred(_ctx, 25);
            case 24:
                return precpred(_ctx, 24);
            case 25:
                return precpred(_ctx, 23);
            case 26:
                return precpred(_ctx, 22);
            case 27:
                return precpred(_ctx, 21);
            case 28:
                return precpred(_ctx, 20);
            case 29:
                return precpred(_ctx, 19);
            case 30:
                return precpred(_ctx, 18);
            case 31:
                return precpred(_ctx, 17);
            case 32:
                return precpred(_ctx, 16);
            case 33:
                return precpred(_ctx, 51);
            case 34:
                return precpred(_ctx, 49);
            case 35:
                return precpred(_ctx, 48);
            case 36:
                return precpred(_ctx, 45);
            case 37:
                return precpred(_ctx, 44);
            case 38:
                return this.notLineTerminator();
            case 39:
                return precpred(_ctx, 43);
            case 40:
                return this.notLineTerminator();
            case 41:
                return precpred(_ctx, 15);
            case 42:
                return precpred(_ctx, 2);
            case 43:
                return precpred(_ctx, 1);
        }
        return true;
    }

    private boolean getter_sempred(GetterContext _localctx, int predIndex) {
        switch (predIndex) {
            case 44:
                return this.n("get");
        }
        return true;
    }

    private boolean setter_sempred(SetterContext _localctx, int predIndex) {
        switch (predIndex) {
            case 45:
                return this.n("set");
        }
        return true;
    }

    private boolean eos_sempred(EosContext _localctx, int predIndex) {
        switch (predIndex) {
            case 46:
                return this.lineTerminatorAhead();
            case 47:
                return this.closeBrace();
        }
        return true;
    }

    public static final String _serializedATN = "\u0004\u0001\u0094\u07aa\u0002\u0000\u0007\u0000\u0002\u0001\u0007\u0001"
            + "\u0002\u0002\u0007\u0002\u0002\u0003\u0007\u0003\u0002\u0004\u0007\u0004"
            + "\u0002\u0005\u0007\u0005\u0002\u0006\u0007\u0006\u0002\u0007\u0007\u0007"
            + "\u0002\b\u0007\b\u0002\t\u0007\t\u0002\n\u0007\n\u0002\u000b\u0007\u000b"
            + "\u0002\f\u0007\f\u0002\r\u0007\r\u0002\u000e\u0007\u000e\u0002\u000f\u0007"
            + "\u000f\u0002\u0010\u0007\u0010\u0002\u0011\u0007\u0011\u0002\u0012\u0007"
            + "\u0012\u0002\u0013\u0007\u0013\u0002\u0014\u0007\u0014\u0002\u0015\u0007"
            + "\u0015\u0002\u0016\u0007\u0016\u0002\u0017\u0007\u0017\u0002\u0018\u0007"
            + "\u0018\u0002\u0019\u0007\u0019\u0002\u001a\u0007\u001a\u0002\u001b\u0007"
            + "\u001b\u0002\u001c\u0007\u001c\u0002\u001d\u0007\u001d\u0002\u001e\u0007"
            + "\u001e\u0002\u001f\u0007\u001f\u0002 \u0007 \u0002!\u0007!\u0002\"\u0007"
            + "\"\u0002#\u0007#\u0002$\u0007$\u0002%\u0007%\u0002&\u0007&\u0002\'\u0007"
            + "\'\u0002(\u0007(\u0002)\u0007)\u0002*\u0007*\u0002+\u0007+\u0002,\u0007"
            + ",\u0002-\u0007-\u0002.\u0007.\u0002/\u0007/\u00020\u00070\u00021\u0007"
            + "1\u00022\u00072\u00023\u00073\u00024\u00074\u00025\u00075\u00026\u0007"
            + "6\u00027\u00077\u00028\u00078\u00029\u00079\u0002:\u0007:\u0002;\u0007"
            + ";\u0002<\u0007<\u0002=\u0007=\u0002>\u0007>\u0002?\u0007?\u0002@\u0007"
            + "@\u0002A\u0007A\u0002B\u0007B\u0002C\u0007C\u0002D\u0007D\u0002E\u0007"
            + "E\u0002F\u0007F\u0002G\u0007G\u0002H\u0007H\u0002I\u0007I\u0002J\u0007"
            + "J\u0002K\u0007K\u0002L\u0007L\u0002M\u0007M\u0002N\u0007N\u0002O\u0007"
            + "O\u0002P\u0007P\u0002Q\u0007Q\u0002R\u0007R\u0002S\u0007S\u0002T\u0007"
            + "T\u0002U\u0007U\u0002V\u0007V\u0002W\u0007W\u0002X\u0007X\u0002Y\u0007"
            + "Y\u0002Z\u0007Z\u0002[\u0007[\u0002\\\u0007\\\u0002]\u0007]\u0002^\u0007"
            + "^\u0002_\u0007_\u0002`\u0007`\u0002a\u0007a\u0002b\u0007b\u0002c\u0007"
            + "c\u0002d\u0007d\u0002e\u0007e\u0002f\u0007f\u0002g\u0007g\u0002h\u0007"
            + "h\u0002i\u0007i\u0002j\u0007j\u0002k\u0007k\u0002l\u0007l\u0002m\u0007"
            + "m\u0002n\u0007n\u0002o\u0007o\u0002p\u0007p\u0002q\u0007q\u0002r\u0007"
            + "r\u0002s\u0007s\u0002t\u0007t\u0002u\u0007u\u0002v\u0007v\u0002w\u0007"
            + "w\u0002x\u0007x\u0002y\u0007y\u0002z\u0007z\u0002{\u0007{\u0002|\u0007"
            + "|\u0002}\u0007}\u0002~\u0007~\u0002\u007f\u0007\u007f\u0002\u0080\u0007"
            + "\u0080\u0002\u0081\u0007\u0081\u0002\u0082\u0007\u0082\u0002\u0083\u0007"
            + "\u0083\u0002\u0084\u0007\u0084\u0002\u0085\u0007\u0085\u0002\u0086\u0007"
            + "\u0086\u0002\u0087\u0007\u0087\u0002\u0088\u0007\u0088\u0002\u0089\u0007"
            + "\u0089\u0002\u008a\u0007\u008a\u0002\u008b\u0007\u008b\u0002\u008c\u0007"
            + "\u008c\u0002\u008d\u0007\u008d\u0002\u008e\u0007\u008e\u0002\u008f\u0007"
            + "\u008f\u0002\u0090\u0007\u0090\u0002\u0091\u0007\u0091\u0002\u0092\u0007"
            + "\u0092\u0002\u0093\u0007\u0093\u0002\u0094\u0007\u0094\u0002\u0095\u0007"
            + "\u0095\u0002\u0096\u0007\u0096\u0002\u0097\u0007\u0097\u0002\u0098\u0007"
            + "\u0098\u0002\u0099\u0007\u0099\u0002\u009a\u0007\u009a\u0002\u009b\u0007"
            + "\u009b\u0002\u009c\u0007\u009c\u0002\u009d\u0007\u009d\u0001\u0000\u0001"
            + "\u0000\u0001\u0000\u0001\u0001\u0001\u0001\u0003\u0001\u0142\b\u0001\u0001"
            + "\u0002\u0001\u0002\u0003\u0002\u0146\b\u0002\u0001\u0002\u0001\u0002\u0001"
            + "\u0003\u0001\u0003\u0001\u0003\u0005\u0003\u014d\b\u0003\n\u0003\f\u0003"
            + "\u0150\t\u0003\u0001\u0004\u0001\u0004\u0003\u0004\u0154\b\u0004\u0001"
            + "\u0004\u0001\u0004\u0001\u0004\u0001\u0004\u0001\u0004\u0003\u0004\u015b"
            + "\b\u0004\u0001\u0005\u0001\u0005\u0001\u0005\u0001\u0006\u0001\u0006\u0003"
            + "\u0006\u0162\b\u0006\u0001\u0006\u0001\u0006\u0001\u0007\u0001\u0007\u0001"
            + "\u0007\u0005\u0007\u0169\b\u0007\n\u0007\f\u0007\u016c\t\u0007\u0001\b"
            + "\u0001\b\u0001\t\u0003\t\u0171\b\t\u0001\t\u0001\t\u0001\t\u0001\t\u0003"
            + "\t\u0177\b\t\u0001\n\u0001\n\u0001\n\u0001\n\u0001\n\u0001\n\u0001\n\u0001"
            + "\n\u0001\n\u0005\n\u0182\b\n\n\n\f\n\u0185\t\n\u0001\u000b\u0001\u000b"
            + "\u0001\u000b\u0001\u000b\u0001\u000b\u0001\u000b\u0001\u000b\u0001\u000b"
            + "\u0001\u000b\u0001\u000b\u0001\u000b\u0001\u000b\u0001\u000b\u0001\u000b"
            + "\u0001\u000b\u0001\u000b\u0001\u000b\u0001\u000b\u0001\u000b\u0001\u000b"
            + "\u0003\u000b\u019b\b\u000b\u0001\u000b\u0001\u000b\u0001\u000b\u0001\u000b"
            + "\u0003\u000b\u01a1\b\u000b\u0001\u000b\u0005\u000b\u01a4\b\u000b\n\u000b"
            + "\f\u000b\u01a7\t\u000b\u0001\f\u0001\f\u0001\f\u0001\f\u0001\f\u0001\f"
            + "\u0001\f\u0001\f\u0001\f\u0003\f\u01b2\b\f\u0001\f\u0001\f\u0001\f\u0001"
            + "\f\u0001\f\u0003\f\u01b9\b\f\u0001\r\u0001\r\u0003\r\u01bd\b\r\u0001\u000e"
            + "\u0001\u000e\u0001\u000e\u0003\u000e\u01c2\b\u000e\u0001\u000e\u0001\u000e"
            + "\u0001\u000f\u0001\u000f\u0003\u000f\u01c8\b\u000f\u0001\u0010\u0001\u0010"
            + "\u0003\u0010\u01cc\b\u0010\u0001\u0010\u0001\u0010\u0001\u0011\u0001\u0011"
            + "\u0003\u0011\u01d2\b\u0011\u0001\u0012\u0001\u0012\u0001\u0012\u0005\u0012"
            + "\u01d7\b\u0012\n\u0012\f\u0012\u01da\t\u0012\u0001\u0013\u0001\u0013\u0001"
            + "\u0013\u0001\u0013\u0001\u0013\u0001\u0013\u0001\u0013\u0003\u0013\u01e3"
            + "\b\u0013\u0003\u0013\u01e5\b\u0013\u0001\u0014\u0001\u0014\u0001\u0014"
            + "\u0001\u0014\u0001\u0014\u0001\u0015\u0001\u0015\u0001\u0015\u0001\u0015"
            + "\u0001\u0016\u0001\u0016\u0001\u0016\u0005\u0016\u01f3\b\u0016\n\u0016"
            + "\f\u0016\u01f6\t\u0016\u0001\u0016\u0003\u0016\u01f9\b\u0016\u0001\u0017"
            + "\u0003\u0017\u01fc\b\u0017\u0001\u0017\u0001\u0017\u0003\u0017\u0200\b"
            + "\u0017\u0001\u0017\u0001\u0017\u0001\u0017\u0001\u0017\u0001\u0018\u0001"
            + "\u0018\u0003\u0018\u0208\b\u0018\u0001\u0018\u0001\u0018\u0003\u0018\u020c"
            + "\b\u0018\u0001\u0018\u0001\u0018\u0001\u0018\u0001\u0018\u0001\u0019\u0001"
            + "\u0019\u0001\u0019\u0001\u001a\u0001\u001a\u0001\u001a\u0001\u001a\u0004"
            + "\u001a\u0219\b\u001a\u000b\u001a\f\u001a\u021a\u0001\u001a\u0001\u001a"
            + "\u0003\u001a\u021f\b\u001a\u0001\u001b\u0003\u001b\u0222\b\u001b\u0001"
            + "\u001b\u0001\u001b\u0003\u001b\u0226\b\u001b\u0001\u001b\u0003\u001b\u0229"
            + "\b\u001b\u0001\u001b\u0001\u001b\u0003\u001b\u022d\b\u001b\u0001\u001c"
            + "\u0001\u001c\u0001\u001c\u0001\u001d\u0003\u001d\u0233\b\u001d\u0001\u001d"
            + "\u0001\u001d\u0003\u001d\u0237\b\u001d\u0001\u001d\u0001\u001d\u0003\u001d"
            + "\u023b\b\u001d\u0001\u001e\u0001\u001e\u0001\u001e\u0001\u001e\u0005\u001e"
            + "\u0241\b\u001e\n\u001e\f\u001e\u0244\t\u001e\u0001\u001e\u0001\u001e\u0003"
            + "\u001e\u0248\b\u001e\u0001\u001e\u0003\u001e\u024b\b\u001e\u0003\u001e"
            + "\u024d\b\u001e\u0001\u001f\u0001\u001f\u0001\u001f\u0005\u001f\u0252\b"
            + "\u001f\n\u001f\f\u001f\u0255\t\u001f\u0001 \u0001 \u0003 \u0259\b \u0001"
            + "!\u0003!\u025c\b!\u0001!\u0003!\u025f\b!\u0001!\u0001!\u0001!\u0003!\u0264"
            + "\b!\u0001!\u0003!\u0267\b!\u0001!\u0003!\u026a\b!\u0001\"\u0001\"\u0001"
            + "\"\u0003\"\u026f\b\"\u0001#\u0003#\u0272\b#\u0001#\u0003#\u0275\b#\u0001"
            + "#\u0001#\u0003#\u0279\b#\u0001$\u0001$\u0001%\u0001%\u0003%\u027f\b%\u0001"
            + "&\u0001&\u0003&\u0283\b&\u0001&\u0001&\u0003&\u0287\b&\u0001&\u0001&\u0003"
            + "&\u028b\b&\u0001\'\u0001\'\u0001\'\u0001\'\u0001\'\u0001\'\u0001\'\u0001"
            + "(\u0001(\u0003(\u0296\b(\u0001(\u0001(\u0001)\u0003)\u029b\b)\u0001)\u0001"
            + ")\u0001)\u0003)\u02a0\b)\u0001)\u0001)\u0001)\u0001)\u0001*\u0003*\u02a7"
            + "\b*\u0001*\u0001*\u0001*\u0003*\u02ac\b*\u0001*\u0001*\u0001*\u0001*\u0001"
            + "*\u0001*\u0003*\u02b4\b*\u0001+\u0003+\u02b7\b+\u0001+\u0003+\u02ba\b"
            + "+\u0001+\u0001+\u0001+\u0003+\u02bf\b+\u0001+\u0003+\u02c2\b+\u0001+\u0001"
            + "+\u0003+\u02c6\b+\u0001,\u0001,\u0001,\u0001-\u0001-\u0001-\u0005-\u02ce"
            + "\b-\n-\f-\u02d1\t-\u0001.\u0003.\u02d4\b.\u0001.\u0001.\u0001.\u0001."
            + "\u0003.\u02da\b.\u0001.\u0001.\u0001/\u0001/\u0003/\u02e0\b/\u00010\u0001"
            + "0\u00010\u00050\u02e5\b0\n0\f0\u02e8\t0\u00011\u00011\u00011\u00031\u02ed"
            + "\b1\u00012\u00032\u02f0\b2\u00012\u00012\u00012\u00012\u00032\u02f6\b"
            + "2\u00012\u00012\u00013\u00013\u00043\u02fc\b3\u000b3\f3\u02fd\u00013\u0005"
            + "3\u0301\b3\n3\f3\u0304\t3\u00014\u00014\u00014\u00014\u00014\u00015\u0004"
            + "5\u030c\b5\u000b5\f5\u030d\u00016\u00016\u00016\u00036\u0313\b6\u0001"
            + "7\u00017\u00017\u00017\u00017\u00017\u00037\u031b\b7\u00017\u00017\u0001"
            + "7\u00057\u0320\b7\n7\f7\u0323\t7\u00018\u00018\u00018\u00019\u00039\u0329"
            + "\b9\u00019\u00019\u0001:\u0003:\u032e\b:\u0001:\u0001:\u0001;\u0001;\u0001"
            + ";\u0001;\u0001;\u0001;\u0001;\u0001;\u0001;\u0001;\u0001;\u0001;\u0001"
            + ";\u0001;\u0001;\u0001;\u0001;\u0001;\u0001;\u0001;\u0001;\u0001;\u0001"
            + ";\u0001;\u0001;\u0001;\u0001;\u0001;\u0001;\u0003;\u034f\b;\u0001<\u0001"
            + "<\u0003<\u0353\b<\u0001<\u0001<\u0001=\u0004=\u0358\b=\u000b=\f=\u0359"
            + "\u0001>\u0001>\u0001>\u0001>\u0001>\u0003>\u0361\b>\u0001>\u0001>\u0001"
            + "?\u0001?\u0001?\u0001@\u0003@\u0369\b@\u0001@\u0001@\u0003@\u036d\b@\u0001"
            + "@\u0001@\u0001@\u0001@\u0001@\u0003@\u0374\b@\u0001A\u0001A\u0001A\u0001"
            + "A\u0005A\u037a\bA\nA\fA\u037d\tA\u0001A\u0001A\u0003A\u0381\bA\u0003A"
            + "\u0383\bA\u0001A\u0001A\u0001B\u0001B\u0001B\u0003B\u038a\bB\u0001C\u0001"
            + "C\u0003C\u038e\bC\u0001D\u0001D\u0001E\u0001E\u0001E\u0001F\u0001F\u0003"
            + "F\u0397\bF\u0001F\u0001F\u0003F\u039b\bF\u0001G\u0001G\u0001G\u0001H\u0001"
            + "H\u0001H\u0003H\u03a3\bH\u0001I\u0001I\u0003I\u03a7\bI\u0001I\u0001I\u0003"
            + "I\u03ab\bI\u0001I\u0001I\u0001I\u0001I\u0001I\u0001I\u0001I\u0003I\u03b4"
            + "\bI\u0001J\u0001J\u0001J\u0001J\u0001J\u0001J\u0003J\u03bc\bJ\u0001J\u0001"
            + "J\u0003J\u03c0\bJ\u0001K\u0001K\u0001K\u0001K\u0005K\u03c6\bK\nK\fK\u03c9"
            + "\tK\u0001K\u0001K\u0003K\u03cd\bK\u0003K\u03cf\bK\u0001K\u0001K\u0001"
            + "L\u0001L\u0001L\u0003L\u03d6\bL\u0001M\u0001M\u0001M\u0003M\u03db\bM\u0001"
            + "N\u0001N\u0003N\u03df\bN\u0001N\u0001N\u0003N\u03e3\bN\u0001N\u0003N\u03e6"
            + "\bN\u0001N\u0003N\u03e9\bN\u0001N\u0003N\u03ec\bN\u0001N\u0001N\u0003"
            + "N\u03f0\bN\u0001N\u0001N\u0003N\u03f4\bN\u0001N\u0001N\u0003N\u03f8\b"
            + "N\u0003N\u03fa\bN\u0001O\u0001O\u0001O\u0005O\u03ff\bO\nO\fO\u0402\tO"
            + "\u0001P\u0001P\u0001P\u0003P\u0407\bP\u0001P\u0003P\u040a\bP\u0001P\u0003"
            + "P\u040d\bP\u0001P\u0001P\u0003P\u0411\bP\u0001P\u0003P\u0414\bP\u0001"
            + "Q\u0001Q\u0001R\u0001R\u0001R\u0003R\u041b\bR\u0001S\u0001S\u0001S\u0001"
            + "S\u0001S\u0001S\u0001S\u0003S\u0424\bS\u0001T\u0001T\u0001T\u0001T\u0001"
            + "T\u0001T\u0001T\u0001T\u0001T\u0001T\u0001T\u0001T\u0001T\u0001T\u0001"
            + "T\u0001T\u0001T\u0003T\u0437\bT\u0001T\u0001T\u0003T\u043b\bT\u0001T\u0001"
            + "T\u0003T\u043f\bT\u0001T\u0001T\u0001T\u0001T\u0001T\u0001T\u0001T\u0001"
            + "T\u0003T\u0449\bT\u0001T\u0001T\u0003T\u044d\bT\u0001T\u0001T\u0001T\u0001"
            + "T\u0001T\u0001T\u0001T\u0001T\u0001T\u0001T\u0001T\u0001T\u0001T\u0001"
            + "T\u0001T\u0001T\u0001T\u0001T\u0001T\u0001T\u0001T\u0001T\u0003T\u0465"
            + "\bT\u0001T\u0001T\u0001T\u0001T\u0001T\u0001T\u0001T\u0003T\u046e\bT\u0001"
            + "T\u0001T\u0001T\u0001T\u0001T\u0003T\u0475\bT\u0001T\u0001T\u0001T\u0001"
            + "T\u0001T\u0001T\u0001T\u0001T\u0003T\u047f\bT\u0001T\u0001T\u0001T\u0003"
            + "T\u0484\bT\u0001U\u0001U\u0001V\u0001V\u0001V\u0003V\u048b\bV\u0001V\u0001"
            + "V\u0001W\u0001W\u0001W\u0003W\u0492\bW\u0001W\u0001W\u0001X\u0001X\u0001"
            + "X\u0003X\u0499\bX\u0001X\u0001X\u0001Y\u0001Y\u0001Y\u0003Y\u04a0\bY\u0001"
            + "Y\u0001Y\u0001Z\u0001Z\u0001Z\u0001Z\u0001Z\u0001Z\u0001[\u0001[\u0001"
            + "[\u0001[\u0001[\u0001[\u0001\\\u0001\\\u0003\\\u04b2\b\\\u0001\\\u0001"
            + "\\\u0003\\\u04b6\b\\\u0003\\\u04b8\b\\\u0001\\\u0001\\\u0001]\u0004]\u04bd"
            + "\b]\u000b]\f]\u04be\u0001^\u0001^\u0001^\u0001^\u0003^\u04c5\b^\u0001"
            + "_\u0001_\u0001_\u0003_\u04ca\b_\u0001`\u0001`\u0001`\u0001`\u0001a\u0001"
            + "a\u0001a\u0001a\u0001a\u0001b\u0001b\u0001b\u0001b\u0003b\u04d9\bb\u0001"
            + "b\u0003b\u04dc\bb\u0001c\u0001c\u0001c\u0001c\u0003c\u04e2\bc\u0001c\u0001"
            + "c\u0003c\u04e6\bc\u0001c\u0001c\u0001d\u0001d\u0001d\u0001e\u0001e\u0001"
            + "e\u0001f\u0003f\u04f1\bf\u0001f\u0001f\u0003f\u04f5\bf\u0001f\u0001f\u0001"
            + "f\u0001f\u0001f\u0001f\u0001f\u0003f\u04fe\bf\u0001g\u0003g\u0501\bg\u0001"
            + "g\u0001g\u0003g\u0505\bg\u0003g\u0507\bg\u0001g\u0003g\u050a\bg\u0001"
            + "g\u0001g\u0001g\u0003g\u050f\bg\u0001g\u0001g\u0001g\u0001h\u0003h\u0515"
            + "\bh\u0001h\u0003h\u0518\bh\u0001i\u0001i\u0005i\u051c\bi\ni\fi\u051f\t"
            + "i\u0001i\u0001i\u0001j\u0001j\u0001j\u0001k\u0001k\u0001k\u0001l\u0001"
            + "l\u0003l\u052b\bl\u0001l\u0001l\u0001l\u0003l\u0530\bl\u0001m\u0001m\u0001"
            + "m\u0003m\u0535\bm\u0001m\u0003m\u0538\bm\u0001m\u0003m\u053b\bm\u0001"
            + "m\u0001m\u0001m\u0001m\u0001m\u0001m\u0001m\u0001m\u0001m\u0001m\u0003"
            + "m\u0547\bm\u0001m\u0001m\u0001m\u0003m\u054c\bm\u0001m\u0003m\u054f\b"
            + "m\u0001n\u0003n\u0552\bn\u0001n\u0003n\u0555\bn\u0001n\u0003n\u0558\b"
            + "n\u0001n\u0003n\u055b\bn\u0001o\u0001o\u0001o\u0001p\u0001p\u0003p\u0562"
            + "\bp\u0001p\u0003p\u0565\bp\u0001p\u0001p\u0001p\u0003p\u056a\bp\u0001"
            + "p\u0001p\u0001p\u0001p\u0001p\u0001q\u0003q\u0572\bq\u0001q\u0001q\u0001"
            + "q\u0003q\u0577\bq\u0001q\u0001q\u0003q\u057b\bq\u0001q\u0001q\u0001q\u0001"
            + "q\u0001q\u0001r\u0001r\u0001r\u0001r\u0005r\u0586\br\nr\fr\u0589\tr\u0001"
            + "r\u0003r\u058c\br\u0001r\u0001r\u0001s\u0001s\u0001s\u0001t\u0001t\u0001"
            + "t\u0001t\u0005t\u0597\bt\nt\ft\u059a\tt\u0001t\u0003t\u059d\bt\u0001t"
            + "\u0001t\u0001u\u0001u\u0001u\u0001u\u0001u\u0003u\u05a6\bu\u0001u\u0001"
            + "u\u0001u\u0001u\u0001u\u0001v\u0001v\u0003v\u05af\bv\u0001w\u0001w\u0001"
            + "w\u0001x\u0001x\u0001x\u0005x\u05b7\bx\nx\fx\u05ba\tx\u0001x\u0001x\u0003"
            + "x\u05be\bx\u0001x\u0003x\u05c1\bx\u0001x\u0001x\u0001x\u0001x\u0001x\u0003"
            + "x\u05c8\bx\u0003x\u05ca\bx\u0001y\u0003y\u05cd\by\u0001y\u0003y\u05d0"
            + "\by\u0001y\u0001y\u0003y\u05d4\by\u0001y\u0003y\u05d7\by\u0001y\u0001"
            + "y\u0003y\u05db\by\u0001z\u0001z\u0001z\u0003z\u05e0\bz\u0001{\u0003{\u05e3"
            + "\b{\u0001|\u0004|\u05e6\b|\u000b|\f|\u05e7\u0001}\u0001}\u0001}\u0001"
            + "}\u0001~\u0005~\u05ef\b~\n~\f~\u05f2\t~\u0001~\u0003~\u05f5\b~\u0001~"
            + "\u0004~\u05f8\b~\u000b~\f~\u05f9\u0001~\u0005~\u05fd\b~\n~\f~\u0600\t"
            + "~\u0001~\u0005~\u0603\b~\n~\f~\u0606\t~\u0001\u007f\u0003\u007f\u0609"
            + "\b\u007f\u0001\u007f\u0001\u007f\u0003\u007f\u060d\b\u007f\u0001\u007f"
            + "\u0003\u007f\u0610\b\u007f\u0001\u0080\u0001\u0080\u0001\u0080\u0001\u0080"
            + "\u0005\u0080\u0616\b\u0080\n\u0080\f\u0080\u0619\t\u0080\u0001\u0080\u0003"
            + "\u0080\u061c\b\u0080\u0003\u0080\u061e\b\u0080\u0001\u0080\u0001\u0080"
            + "\u0001\u0081\u0001\u0081\u0001\u0081\u0001\u0081\u0001\u0081\u0001\u0081"
            + "\u0001\u0081\u0001\u0081\u0001\u0081\u0001\u0081\u0001\u0081\u0001\u0081"
            + "\u0001\u0081\u0001\u0081\u0001\u0081\u0003\u0081\u0631\b\u0081\u0001\u0081"
            + "\u0001\u0081\u0003\u0081\u0635\b\u0081\u0001\u0082\u0001\u0082\u0001\u0082"
            + "\u0001\u0082\u0003\u0082\u063b\b\u0082\u0001\u0082\u0001\u0082\u0001\u0082"
            + "\u0001\u0082\u0001\u0083\u0001\u0083\u0001\u0083\u0003\u0083\u0644\b\u0083"
            + "\u0001\u0083\u0001\u0083\u0001\u0083\u0001\u0083\u0001\u0083\u0001\u0084"
            + "\u0001\u0084\u0001\u0084\u0001\u0084\u0001\u0084\u0001\u0084\u0001\u0084"
            + "\u0003\u0084\u0652\b\u0084\u0001\u0085\u0001\u0085\u0001\u0085\u0003\u0085"
            + "\u0657\b\u0085\u0003\u0085\u0659\b\u0085\u0001\u0085\u0001\u0085\u0001"
            + "\u0086\u0001\u0086\u0001\u0086\u0005\u0086\u0660\b\u0086\n\u0086\f\u0086"
            + "\u0663\t\u0086\u0001\u0087\u0003\u0087\u0666\b\u0087\u0001\u0087\u0001"
            + "\u0087\u0003\u0087\u066a\b\u0087\u0001\u0088\u0001\u0088\u0001\u0088\u0005"
            + "\u0088\u066f\b\u0088\n\u0088\f\u0088\u0672\t\u0088\u0001\u0089\u0001\u0089"
            + "\u0001\u0089\u0001\u0089\u0003\u0089\u0678\b\u0089\u0001\u0089\u0003\u0089"
            + "\u067b\b\u0089\u0001\u0089\u0001\u0089\u0001\u0089\u0001\u0089\u0001\u0089"
            + "\u0001\u0089\u0003\u0089\u0683\b\u0089\u0001\u0089\u0001\u0089\u0001\u0089"
            + "\u0001\u0089\u0001\u0089\u0003\u0089\u068a\b\u0089\u0001\u0089\u0001\u0089"
            + "\u0001\u0089\u0001\u0089\u0001\u0089\u0001\u0089\u0001\u0089\u0001\u0089"
            + "\u0001\u0089\u0001\u0089\u0001\u0089\u0001\u0089\u0001\u0089\u0001\u0089"
            + "\u0001\u0089\u0001\u0089\u0001\u0089\u0001\u0089\u0001\u0089\u0001\u0089"
            + "\u0001\u0089\u0001\u0089\u0001\u0089\u0001\u0089\u0001\u0089\u0001\u0089"
            + "\u0001\u0089\u0003\u0089\u06a7\b\u0089\u0001\u0089\u0001\u0089\u0001\u0089"
            + "\u0001\u0089\u0001\u0089\u0001\u0089\u0001\u0089\u0001\u0089\u0001\u0089"
            + "\u0001\u0089\u0003\u0089\u06b3\b\u0089\u0003\u0089\u06b5\b\u0089\u0001"
            + "\u0089\u0001\u0089\u0001\u0089\u0001\u0089\u0001\u0089\u0001\u0089\u0001"
            + "\u0089\u0001\u0089\u0001\u0089\u0001\u0089\u0001\u0089\u0001\u0089\u0001"
            + "\u0089\u0001\u0089\u0001\u0089\u0001\u0089\u0001\u0089\u0001\u0089\u0001"
            + "\u0089\u0001\u0089\u0001\u0089\u0001\u0089\u0003\u0089\u06cd\b\u0089\u0001"
            + "\u0089\u0001\u0089\u0001\u0089\u0001\u0089\u0001\u0089\u0001\u0089\u0001"
            + "\u0089\u0001\u0089\u0001\u0089\u0001\u0089\u0001\u0089\u0001\u0089\u0001"
            + "\u0089\u0001\u0089\u0001\u0089\u0001\u0089\u0001\u0089\u0001\u0089\u0001"
            + "\u0089\u0001\u0089\u0001\u0089\u0001\u0089\u0001\u0089\u0001\u0089\u0001"
            + "\u0089\u0001\u0089\u0001\u0089\u0001\u0089\u0001\u0089\u0001\u0089\u0001"
            + "\u0089\u0001\u0089\u0001\u0089\u0001\u0089\u0001\u0089\u0001\u0089\u0001"
            + "\u0089\u0001\u0089\u0001\u0089\u0001\u0089\u0001\u0089\u0001\u0089\u0001"
            + "\u0089\u0003\u0089\u06fa\b\u0089\u0001\u0089\u0001\u0089\u0001\u0089\u0001"
            + "\u0089\u0001\u0089\u0001\u0089\u0003\u0089\u0702\b\u0089\u0001\u0089\u0001"
            + "\u0089\u0003\u0089\u0706\b\u0089\u0001\u0089\u0001\u0089\u0003\u0089\u070a"
            + "\b\u0089\u0001\u0089\u0001\u0089\u0003\u0089\u070e\b\u0089\u0001\u0089"
            + "\u0001\u0089\u0003\u0089\u0712\b\u0089\u0001\u0089\u0001\u0089\u0003\u0089"
            + "\u0716\b\u0089\u0001\u0089\u0001\u0089\u0001\u0089\u0001\u0089\u0001\u0089"
            + "\u0001\u0089\u0001\u0089\u0001\u0089\u0001\u0089\u0001\u0089\u0001\u0089"
            + "\u0001\u0089\u0001\u0089\u0001\u0089\u0001\u0089\u0005\u0089\u0727\b\u0089"
            + "\n\u0089\f\u0089\u072a\t\u0089\u0001\u008a\u0001\u008a\u0001\u008a\u0003"
            + "\u008a\u072f\b\u008a\u0001\u008a\u0003\u008a\u0732\b\u008a\u0001\u008b"
            + "\u0001\u008b\u0001\u008b\u0001\u008b\u0003\u008b\u0738\b\u008b\u0001\u008c"
            + "\u0001\u008c\u0003\u008c\u073c\b\u008c\u0001\u008c\u0001\u008c\u0003\u008c"
            + "\u0740\b\u008c\u0001\u008c\u0001\u008c\u0003\u008c\u0744\b\u008c\u0001"
            + "\u008c\u0001\u008c\u0003\u008c\u0748\b\u008c\u0001\u008c\u0001\u008c\u0001"
            + "\u008c\u0001\u008c\u0001\u008c\u0003\u008c\u074f\b\u008c\u0001\u008d\u0003"
            + "\u008d\u0752\b\u008d\u0001\u008d\u0001\u008d\u0003\u008d\u0756\b\u008d"
            + "\u0001\u008d\u0001\u008d\u0001\u008d\u0001\u008e\u0001\u008e\u0001\u008e"
            + "\u0003\u008e\u075e\b\u008e\u0001\u008e\u0003\u008e\u0761\b\u008e\u0001"
            + "\u008f\u0001\u008f\u0001\u008f\u0001\u008f\u0001\u008f\u0003\u008f\u0768"
            + "\b\u008f\u0001\u0090\u0001\u0090\u0001\u0091\u0001\u0091\u0001\u0091\u0001"
            + "\u0091\u0001\u0091\u0001\u0091\u0001\u0091\u0003\u0091\u0773\b\u0091\u0001"
            + "\u0092\u0001\u0092\u0005\u0092\u0777\b\u0092\n\u0092\f\u0092\u077a\t\u0092"
            + "\u0001\u0092\u0001\u0092\u0001\u0093\u0001\u0093\u0001\u0093\u0001\u0093"
            + "\u0001\u0093\u0001\u0093\u0003\u0093\u0784\b\u0093\u0001\u0094\u0001\u0094"
            + "\u0001\u0095\u0001\u0095\u0001\u0096\u0001\u0096\u0001\u0096\u0001\u0096"
            + "\u0001\u0097\u0001\u0097\u0001\u0097\u0001\u0097\u0001\u0098\u0001\u0098"
            + "\u0003\u0098\u0794\b\u0098\u0001\u0099\u0001\u0099\u0001\u009a\u0001\u009a"
            + "\u0001\u009a\u0003\u009a\u079b\b\u009a\u0001\u009b\u0001\u009b\u0001\u009b"
            + "\u0003\u009b\u07a0\b\u009b\u0001\u009c\u0001\u009c\u0001\u009d\u0001\u009d"
            + "\u0001\u009d\u0001\u009d\u0003\u009d\u07a8\b\u009d\u0001\u009d\u0000\u0004"
            + "\u0014\u0016n\u0112\u009e\u0000\u0002\u0004\u0006\b\n\f\u000e\u0010\u0012"
            + "\u0014\u0016\u0018\u001a\u001c\u001e \"$&(*,.02468:<>@BDFHJLNPRTVXZ\\"
            + "^`bdfhjlnprtvxz|~\u0080\u0082\u0084\u0086\u0088\u008a\u008c\u008e\u0090"
            + "\u0092\u0094\u0096\u0098\u009a\u009c\u009e\u00a0\u00a2\u00a4\u00a6\u00a8"
            + "\u00aa\u00ac\u00ae\u00b0\u00b2\u00b4\u00b6\u00b8\u00ba\u00bc\u00be\u00c0"
            + "\u00c2\u00c4\u00c6\u00c8\u00ca\u00cc\u00ce\u00d0\u00d2\u00d4\u00d6\u00d8"
            + "\u00da\u00dc\u00de\u00e0\u00e2\u00e4\u00e6\u00e8\u00ea\u00ec\u00ee\u00f0"
            + "\u00f2\u00f4\u00f6\u00f8\u00fa\u00fc\u00fe\u0100\u0102\u0104\u0106\u0108"
            + "\u010a\u010c\u010e\u0110\u0112\u0114\u0116\u0118\u011a\u011c\u011e\u0120"
            + "\u0122\u0124\u0126\u0128\u012a\u012c\u012e\u0130\u0132\u0134\u0136\u0138"
            + "\u013a\u0000\u0011\u0002\u0000((**\u0001\u0000\u000b\f\u0002\u0000pqt"
            + "t\u0002\u0000wwzz\u0002\u0000de\u008a\u008a\u0003\u0000MMkkoo\u0001\u0000"
            + "ef\u0002\u0000\r\r\u0010\u0010\u0001\u0000\u0019\u001b\u0001\u0000\u0015"
            + "\u0016\u0001\u0000 #\u0001\u0000$\'\u0001\u0000-9\u0001\u0000=A\u0001"
            + "\u0000BE\u0006\u0000`acceev\u0083\u0087\u0087\u008a\u008a\u0006\u0000"
            + "Feguwwyz\u0081\u0081\u0084\u0085\u0896\u0000\u013c\u0001\u0000\u0000\u0000"
            + "\u0002\u0141\u0001\u0000\u0000\u0000\u0004\u0143\u0001\u0000\u0000\u0000"
            + "\u0006\u0149\u0001\u0000\u0000\u0000\b\u015a\u0001\u0000\u0000\u0000\n"
            + "\u015c\u0001\u0000\u0000\u0000\f\u015f\u0001\u0000\u0000\u0000\u000e\u0165"
            + "\u0001\u0000\u0000\u0000\u0010\u016d\u0001\u0000\u0000\u0000\u0012\u0176"
            + "\u0001\u0000\u0000\u0000\u0014\u0178\u0001\u0000\u0000\u0000\u0016\u019a"
            + "\u0001\u0000\u0000\u0000\u0018\u01b8\u0001\u0000\u0000\u0000\u001a\u01ba"
            + "\u0001\u0000\u0000\u0000\u001c\u01be\u0001\u0000\u0000\u0000\u001e\u01c7"
            + "\u0001\u0000\u0000\u0000 \u01c9\u0001\u0000\u0000\u0000\"\u01cf\u0001"
            + "\u0000\u0000\u0000$\u01d3\u0001\u0000\u0000\u0000&\u01e4\u0001\u0000\u0000"
            + "\u0000(\u01e6\u0001\u0000\u0000\u0000*\u01eb\u0001\u0000\u0000\u0000,"
            + "\u01ef\u0001\u0000\u0000\u0000.\u01fb\u0001\u0000\u0000\u00000\u0205\u0001"
            + "\u0000\u0000\u00002\u0211\u0001\u0000\u0000\u00004\u021e\u0001\u0000\u0000"
            + "\u00006\u0221\u0001\u0000\u0000\u00008\u022e\u0001\u0000\u0000\u0000:"
            + "\u0232\u0001\u0000\u0000\u0000<\u024c\u0001\u0000\u0000\u0000>\u024e\u0001"
            + "\u0000\u0000\u0000@\u0258\u0001\u0000\u0000\u0000B\u025b\u0001\u0000\u0000"
            + "\u0000D\u026b\u0001\u0000\u0000\u0000F\u0271\u0001\u0000\u0000\u0000H"
            + "\u027a\u0001\u0000\u0000\u0000J\u027e\u0001\u0000\u0000\u0000L\u0280\u0001"
            + "\u0000\u0000\u0000N\u028c\u0001\u0000\u0000\u0000P\u0293\u0001\u0000\u0000"
            + "\u0000R\u029a\u0001\u0000\u0000\u0000T\u02a6\u0001\u0000\u0000\u0000V"
            + "\u02b6\u0001\u0000\u0000\u0000X\u02c7\u0001\u0000\u0000\u0000Z\u02ca\u0001"
            + "\u0000\u0000\u0000\\\u02d3\u0001\u0000\u0000\u0000^\u02dd\u0001\u0000"
            + "\u0000\u0000`\u02e1\u0001\u0000\u0000\u0000b\u02e9\u0001\u0000\u0000\u0000"
            + "d\u02ef\u0001\u0000\u0000\u0000f\u02f9\u0001\u0000\u0000\u0000h\u0305"
            + "\u0001\u0000\u0000\u0000j\u030b\u0001\u0000\u0000\u0000l\u030f\u0001\u0000"
            + "\u0000\u0000n\u031a\u0001\u0000\u0000\u0000p\u0324\u0001\u0000\u0000\u0000"
            + "r\u0328\u0001\u0000\u0000\u0000t\u032d\u0001\u0000\u0000\u0000v\u034e"
            + "\u0001\u0000\u0000\u0000x\u0350\u0001\u0000\u0000\u0000z\u0357\u0001\u0000"
            + "\u0000\u0000|\u035b\u0001\u0000\u0000\u0000~\u0364\u0001\u0000\u0000\u0000"
            + "\u0080\u0373\u0001\u0000\u0000\u0000\u0082\u0375\u0001\u0000\u0000\u0000"
            + "\u0084\u0386\u0001\u0000\u0000\u0000\u0086\u038d\u0001\u0000\u0000\u0000"
            + "\u0088\u038f\u0001\u0000\u0000\u0000\u008a\u0391\u0001\u0000\u0000\u0000"
            + "\u008c\u0396\u0001\u0000\u0000\u0000\u008e\u039c\u0001\u0000\u0000\u0000"
            + "\u0090\u039f\u0001\u0000\u0000\u0000\u0092\u03b3\u0001\u0000\u0000\u0000"
            + "\u0094\u03bf\u0001\u0000\u0000\u0000\u0096\u03c1\u0001\u0000\u0000\u0000"
            + "\u0098\u03d2\u0001\u0000\u0000\u0000\u009a\u03da\u0001\u0000\u0000\u0000"
            + "\u009c\u03f9\u0001\u0000\u0000\u0000\u009e\u03fb\u0001\u0000\u0000\u0000"
            + "\u00a0\u0406\u0001\u0000\u0000\u0000\u00a2\u0415\u0001\u0000\u0000\u0000"
            + "\u00a4\u0417\u0001\u0000\u0000\u0000\u00a6\u041c\u0001\u0000\u0000\u0000"
            + "\u00a8\u0483\u0001\u0000\u0000\u0000\u00aa\u0485\u0001\u0000\u0000\u0000"
            + "\u00ac\u0487\u0001\u0000\u0000\u0000\u00ae\u048e\u0001\u0000\u0000\u0000"
            + "\u00b0\u0495\u0001\u0000\u0000\u0000\u00b2\u049c\u0001\u0000\u0000\u0000"
            + "\u00b4\u04a3\u0001\u0000\u0000\u0000\u00b6\u04a9\u0001\u0000\u0000\u0000"
            + "\u00b8\u04af\u0001\u0000\u0000\u0000\u00ba\u04bc\u0001\u0000\u0000\u0000"
            + "\u00bc\u04c0\u0001\u0000\u0000\u0000\u00be\u04c6\u0001\u0000\u0000\u0000"
            + "\u00c0\u04cb\u0001\u0000\u0000\u0000\u00c2\u04cf\u0001\u0000\u0000\u0000"
            + "\u00c4\u04d4\u0001\u0000\u0000\u0000\u00c6\u04dd\u0001\u0000\u0000\u0000"
            + "\u00c8\u04e9\u0001\u0000\u0000\u0000\u00ca\u04ec\u0001\u0000\u0000\u0000"
            + "\u00cc\u04f0\u0001\u0000\u0000\u0000\u00ce\u0500\u0001\u0000\u0000\u0000"
            + "\u00d0\u0514\u0001\u0000\u0000\u0000\u00d2\u0519\u0001\u0000\u0000\u0000"
            + "\u00d4\u0522\u0001\u0000\u0000\u0000\u00d6\u0525\u0001\u0000\u0000\u0000"
            + "\u00d8\u052f\u0001\u0000\u0000\u0000\u00da\u054e\u0001\u0000\u0000\u0000"
            + "\u00dc\u0551\u0001\u0000\u0000\u0000\u00de\u055c\u0001\u0000\u0000\u0000"
            + "\u00e0\u0561\u0001\u0000\u0000\u0000\u00e2\u0571\u0001\u0000\u0000\u0000"
            + "\u00e4\u0581\u0001\u0000\u0000\u0000\u00e6\u058f\u0001\u0000\u0000\u0000"
            + "\u00e8\u0592\u0001\u0000\u0000\u0000\u00ea\u05a0\u0001\u0000\u0000\u0000"
            + "\u00ec\u05ae\u0001\u0000\u0000\u0000\u00ee\u05b0\u0001\u0000\u0000\u0000"
            + "\u00f0\u05c9\u0001\u0000\u0000\u0000\u00f2\u05cc\u0001\u0000\u0000\u0000"
            + "\u00f4\u05dc\u0001\u0000\u0000\u0000\u00f6\u05e2\u0001\u0000\u0000\u0000"
            + "\u00f8\u05e5\u0001\u0000\u0000\u0000\u00fa\u05e9\u0001\u0000\u0000\u0000"
            + "\u00fc\u05f0\u0001\u0000\u0000\u0000\u00fe\u0608\u0001\u0000\u0000\u0000"
            + "\u0100\u0611\u0001\u0000\u0000\u0000\u0102\u0634\u0001\u0000\u0000\u0000"
            + "\u0104\u0636\u0001\u0000\u0000\u0000\u0106\u0640\u0001\u0000\u0000\u0000"
            + "\u0108\u0651\u0001\u0000\u0000\u0000\u010a\u0653\u0001\u0000\u0000\u0000"
            + "\u010c\u065c\u0001\u0000\u0000\u0000\u010e\u0665\u0001\u0000\u0000\u0000"
            + "\u0110\u066b\u0001\u0000\u0000\u0000\u0112\u06b4\u0001\u0000\u0000\u0000"
            + "\u0114\u0731\u0001\u0000\u0000\u0000\u0116\u0737\u0001\u0000\u0000\u0000"
            + "\u0118\u074e\u0001\u0000\u0000\u0000\u011a\u0751\u0001\u0000\u0000\u0000"
            + "\u011c\u0760\u0001\u0000\u0000\u0000\u011e\u0767\u0001\u0000\u0000\u0000"
            + "\u0120\u0769\u0001\u0000\u0000\u0000\u0122\u0772\u0001\u0000\u0000\u0000"
            + "\u0124\u0774\u0001\u0000\u0000\u0000\u0126\u0783\u0001\u0000\u0000\u0000"
            + "\u0128\u0785\u0001\u0000\u0000\u0000\u012a\u0787\u0001\u0000\u0000\u0000"
            + "\u012c\u0789\u0001\u0000\u0000\u0000\u012e\u078d\u0001\u0000\u0000\u0000"
            + "\u0130\u0793\u0001\u0000\u0000\u0000\u0132\u0795\u0001\u0000\u0000\u0000"
            + "\u0134\u079a\u0001\u0000\u0000\u0000\u0136\u079f\u0001\u0000\u0000\u0000"
            + "\u0138\u07a1\u0001\u0000\u0000\u0000\u013a\u07a7\u0001\u0000\u0000\u0000"
            + "\u013c\u013d\u0005\r\u0000\u0000\u013d\u013e\u0003\u0112\u0089\u0000\u013e"
            + "\u0001\u0001\u0000\u0000\u0000\u013f\u0142\u0003\u00fa}\u0000\u0140\u0142"
            + "\u0003\u0100\u0080\u0000\u0141\u013f\u0001\u0000\u0000\u0000\u0141\u0140"
            + "\u0001\u0000\u0000\u0000\u0142\u0003\u0001\u0000\u0000\u0000\u0143\u0145"
            + "\u0005 \u0000\u0000\u0144\u0146\u0003\u0006\u0003\u0000\u0145\u0144\u0001"
            + "\u0000\u0000\u0000\u0145\u0146\u0001\u0000\u0000\u0000\u0146\u0147\u0001"
            + "\u0000\u0000\u0000\u0147\u0148\u0005!\u0000\u0000\u0148\u0005\u0001\u0000"
            + "\u0000\u0000\u0149\u014e\u0003\b\u0004\u0000\u014a\u014b\u0005\f\u0000"
            + "\u0000\u014b\u014d\u0003\b\u0004\u0000\u014c\u014a\u0001\u0000\u0000\u0000"
            + "\u014d\u0150\u0001\u0000\u0000\u0000\u014e\u014c\u0001\u0000\u0000\u0000"
            + "\u014e\u014f\u0001\u0000\u0000\u0000\u014f\u0007\u0001\u0000\u0000\u0000"
            + "\u0150\u014e\u0001\u0000\u0000\u0000\u0151\u0153\u0003\u0132\u0099\u0000"
            + "\u0152\u0154\u0003\n\u0005\u0000\u0153\u0152\u0001\u0000\u0000\u0000\u0153"
            + "\u0154\u0001\u0000\u0000\u0000\u0154\u015b\u0001\u0000\u0000\u0000\u0155"
            + "\u0156\u0003\u0132\u0099\u0000\u0156\u0157\u0005\r\u0000\u0000\u0157\u0158"
            + "\u0003\u0010\b\u0000\u0158\u015b\u0001\u0000\u0000\u0000\u0159\u015b\u0003"
            + "\u0004\u0002\u0000\u015a\u0151\u0001\u0000\u0000\u0000\u015a\u0155\u0001"
            + "\u0000\u0000\u0000\u015a\u0159\u0001\u0000\u0000\u0000\u015b\t\u0001\u0000"
            + "\u0000\u0000\u015c\u015d\u0005i\u0000\u0000\u015d\u015e\u0003\u0012\t"
            + "\u0000\u015e\u000b\u0001\u0000\u0000\u0000\u015f\u0161\u0005 \u0000\u0000"
            + "\u0160\u0162\u0003\u000e\u0007\u0000\u0161\u0160\u0001\u0000\u0000\u0000"
            + "\u0161\u0162\u0001\u0000\u0000\u0000\u0162\u0163\u0001\u0000\u0000\u0000"
            + "\u0163\u0164\u0005!\u0000\u0000\u0164\r\u0001\u0000\u0000\u0000\u0165"
            + "\u016a\u0003\u0010\b\u0000\u0166\u0167\u0005\f\u0000\u0000\u0167\u0169"
            + "\u0003\u0010\b\u0000\u0168\u0166\u0001\u0000\u0000\u0000\u0169\u016c\u0001"
            + "\u0000\u0000\u0000\u016a\u0168\u0001\u0000\u0000\u0000\u016a\u016b\u0001"
            + "\u0000\u0000\u0000\u016b\u000f\u0001\u0000\u0000\u0000\u016c\u016a\u0001"
            + "\u0000\u0000\u0000\u016d\u016e\u0003\u0012\t\u0000\u016e\u0011\u0001\u0000"
            + "\u0000\u0000\u016f\u0171\u0007\u0000\u0000\u0000\u0170\u016f\u0001\u0000"
            + "\u0000\u0000\u0170\u0171\u0001\u0000\u0000\u0000\u0171\u0172\u0001\u0000"
            + "\u0000\u0000\u0172\u0177\u0003\u0014\n\u0000\u0173\u0177\u0003.\u0017"
            + "\u0000\u0174\u0177\u00030\u0018\u0000\u0175\u0177\u0003\u001c\u000e\u0000"
            + "\u0176\u0170\u0001\u0000\u0000\u0000\u0176\u0173\u0001\u0000\u0000\u0000"
            + "\u0176\u0174\u0001\u0000\u0000\u0000\u0176\u0175\u0001\u0000\u0000\u0000"
            + "\u0177\u0013\u0001\u0000\u0000\u0000\u0178\u0179\u0006\n\uffff\uffff\u0000"
            + "\u0179\u017a\u0003\u0016\u000b\u0000\u017a\u0183\u0001\u0000\u0000\u0000"
            + "\u017b\u017c\n\u0003\u0000\u0000\u017c\u017d\u0005*\u0000\u0000\u017d"
            + "\u0182\u0003\u0014\n\u0004\u017e\u017f\n\u0002\u0000\u0000\u017f\u0180"
            + "\u0005(\u0000\u0000\u0180\u0182\u0003\u0014\n\u0003\u0181\u017b\u0001"
            + "\u0000\u0000\u0000\u0181\u017e\u0001\u0000\u0000\u0000\u0182\u0185\u0001"
            + "\u0000\u0000\u0000\u0183\u0181\u0001\u0000\u0000\u0000\u0183\u0184\u0001"
            + "\u0000\u0000\u0000\u0184\u0015\u0001\u0000\u0000\u0000\u0185\u0183\u0001"
            + "\u0000\u0000\u0000\u0186\u0187\u0006\u000b\uffff\uffff\u0000\u0187\u0188"
            + "\u0005\u0006\u0000\u0000\u0188\u0189\u0003\u0012\t\u0000\u0189\u018a\u0005"
            + "\u0007\u0000\u0000\u018a\u019b\u0001\u0000\u0000\u0000\u018b\u019b\u0003"
            + "\u0018\f\u0000\u018c\u019b\u0003\u001a\r\u0000\u018d\u019b\u0003 \u0010"
            + "\u0000\u018e\u018f\u0005\u0004\u0000\u0000\u018f\u0190\u0003,\u0016\u0000"
            + "\u0190\u0191\u0005\u0005\u0000\u0000\u0191\u019b\u0001\u0000\u0000\u0000"
            + "\u0192\u019b\u00032\u0019\u0000\u0193\u019b\u0005X\u0000\u0000\u0194\u0195"
            + "\u0003\u001a\r\u0000\u0195\u0196\u0005\u0088\u0000\u0000\u0196\u0197\u0003"
            + "\u0016\u000b\u0002\u0197\u019b\u0001\u0000\u0000\u0000\u0198\u0199\u0005"
            + "\u0080\u0000\u0000\u0199\u019b\u0003\u0016\u000b\u0001\u019a\u0186\u0001"
            + "\u0000\u0000\u0000\u019a\u018b\u0001\u0000\u0000\u0000\u019a\u018c\u0001"
            + "\u0000\u0000\u0000\u019a\u018d\u0001\u0000\u0000\u0000\u019a\u018e\u0001"
            + "\u0000\u0000\u0000\u019a\u0192\u0001\u0000\u0000\u0000\u019a\u0193\u0001"
            + "\u0000\u0000\u0000\u019a\u0194\u0001\u0000\u0000\u0000\u019a\u0198\u0001"
            + "\u0000\u0000\u0000\u019b\u01a5\u0001\u0000\u0000\u0000\u019c\u019d\n\u0006"
            + "\u0000\u0000\u019d\u019e\u0004\u000b\u0003\u0000\u019e\u01a0\u0005\u0004"
            + "\u0000\u0000\u019f\u01a1\u0003\u0016\u000b\u0000\u01a0\u019f\u0001\u0000"
            + "\u0000\u0000\u01a0\u01a1\u0001\u0000\u0000\u0000\u01a1\u01a2\u0001\u0000"
            + "\u0000\u0000\u01a2\u01a4\u0005\u0005\u0000\u0000\u01a3\u019c\u0001\u0000"
            + "\u0000\u0000\u01a4\u01a7\u0001\u0000\u0000\u0000\u01a5\u01a3\u0001\u0000"
            + "\u0000\u0000\u01a5\u01a6\u0001\u0000\u0000\u0000\u01a6\u0017\u0001\u0000"
            + "\u0000\u0000\u01a7\u01a5\u0001\u0000\u0000\u0000\u01a8\u01b9\u0005v\u0000"
            + "\u0000\u01a9\u01b9\u0005;\u0000\u0000\u01aa\u01b9\u0005w\u0000\u0000\u01ab"
            + "\u01b9\u0005=\u0000\u0000\u01ac\u01b9\u0005y\u0000\u0000\u01ad\u01b9\u0005"
            + "<\u0000\u0000\u01ae\u01b9\u0005z\u0000\u0000\u01af\u01b9\u0005\u008b\u0000"
            + "\u0000\u01b0\u01b2\u0005{\u0000\u0000\u01b1\u01b0\u0001\u0000\u0000\u0000"
            + "\u01b1\u01b2\u0001\u0000\u0000\u0000\u01b2\u01b3\u0001\u0000\u0000\u0000"
            + "\u01b3\u01b9\u0005|\u0000\u0000\u01b4\u01b9\u0005x\u0000\u0000\u01b5\u01b9"
            + "\u0005}\u0000\u0000\u01b6\u01b9\u0005~\u0000\u0000\u01b7\u01b9\u0005Q"
            + "\u0000\u0000\u01b8\u01a8\u0001\u0000\u0000\u0000\u01b8\u01a9\u0001\u0000"
            + "\u0000\u0000\u01b8\u01aa\u0001\u0000\u0000\u0000\u01b8\u01ab\u0001\u0000"
            + "\u0000\u0000\u01b8\u01ac\u0001\u0000\u0000\u0000\u01b8\u01ad\u0001\u0000"
            + "\u0000\u0000\u01b8\u01ae\u0001\u0000\u0000\u0000\u01b8\u01af\u0001\u0000"
            + "\u0000\u0000\u01b8\u01b1\u0001\u0000\u0000\u0000\u01b8\u01b4\u0001\u0000"
            + "\u0000\u0000\u01b8\u01b5\u0001\u0000\u0000\u0000\u01b8\u01b6\u0001\u0000"
            + "\u0000\u0000\u01b8\u01b7\u0001\u0000\u0000\u0000\u01b9\u0019\u0001\u0000"
            + "\u0000\u0000\u01ba\u01bc\u0003\u001e\u000f\u0000\u01bb\u01bd\u0003\u001c"
            + "\u000e\u0000\u01bc\u01bb\u0001\u0000\u0000\u0000\u01bc\u01bd\u0001\u0000"
            + "\u0000\u0000\u01bd\u001b\u0001\u0000\u0000\u0000\u01be\u01bf\u0005 \u0000"
            + "\u0000\u01bf\u01c1\u0003\u000e\u0007\u0000\u01c0\u01c2\u0003\u001c\u000e"
            + "\u0000\u01c1\u01c0\u0001\u0000\u0000\u0000\u01c1\u01c2\u0001\u0000\u0000"
            + "\u0000\u01c2\u01c3\u0001\u0000\u0000\u0000\u01c3\u01c4\u0005!\u0000\u0000"
            + "\u01c4\u001d\u0001\u0000\u0000\u0000\u01c5\u01c8\u0003\u0132\u0099\u0000"
            + "\u01c6\u01c8\u0003f3\u0000\u01c7\u01c5\u0001\u0000\u0000\u0000\u01c7\u01c6"
            + "\u0001\u0000\u0000\u0000\u01c8\u001f\u0001\u0000\u0000\u0000\u01c9\u01cb"
            + "\u0005\b\u0000\u0000\u01ca\u01cc\u0003\"\u0011\u0000\u01cb\u01ca\u0001"
            + "\u0000\u0000\u0000\u01cb\u01cc\u0001\u0000\u0000\u0000\u01cc\u01cd\u0001"
            + "\u0000\u0000\u0000\u01cd\u01ce\u0005\n\u0000\u0000\u01ce!\u0001\u0000"
            + "\u0000\u0000\u01cf\u01d1\u0003$\u0012\u0000\u01d0\u01d2\u0007\u0001\u0000"
            + "\u0000\u01d1\u01d0\u0001\u0000\u0000\u0000\u01d1\u01d2\u0001\u0000\u0000"
            + "\u0000\u01d2#\u0001\u0000\u0000\u0000\u01d3\u01d8\u0003&\u0013\u0000\u01d4"
            + "\u01d5\u0007\u0001\u0000\u0000\u01d5\u01d7\u0003&\u0013\u0000\u01d6\u01d4"
            + "\u0001\u0000\u0000\u0000\u01d7\u01da\u0001\u0000\u0000\u0000\u01d8\u01d6"
            + "\u0001\u0000\u0000\u0000\u01d8\u01d9\u0001\u0000\u0000\u0000\u01d9%\u0001"
            + "\u0000\u0000\u0000\u01da\u01d8\u0001\u0000\u0000\u0000\u01db\u01e5\u0003"
            + "6\u001b\u0000\u01dc\u01e5\u0003:\u001d\u0000\u01dd\u01e5\u0003L&\u0000"
            + "\u01de\u01e5\u0003N\'\u0000\u01df\u01e2\u0003P(\u0000\u01e0\u01e1\u0005"
            + ":\u0000\u0000\u01e1\u01e3\u0003\u0012\t\u0000\u01e2\u01e0\u0001\u0000"
            + "\u0000\u0000\u01e2\u01e3\u0001\u0000\u0000\u0000\u01e3\u01e5\u0001\u0000"
            + "\u0000\u0000\u01e4\u01db\u0001\u0000\u0000\u0000\u01e4\u01dc\u0001\u0000"
            + "\u0000\u0000\u01e4\u01dd\u0001\u0000\u0000\u0000\u01e4\u01de\u0001\u0000"
            + "\u0000\u0000\u01e4\u01df\u0001\u0000\u0000\u0000\u01e5\'\u0001\u0000\u0000"
            + "\u0000\u01e6\u01e7\u0003\u0016\u000b\u0000\u01e7\u01e8\u0004\u0014\u0004"
            + "\u0000\u01e8\u01e9\u0005\u0004\u0000\u0000\u01e9\u01ea\u0005\u0005\u0000"
            + "\u0000\u01ea)\u0001\u0000\u0000\u0000\u01eb\u01ec\u0005\u0004\u0000\u0000"
            + "\u01ec\u01ed\u0003,\u0016\u0000\u01ed\u01ee\u0005\u0005\u0000\u0000\u01ee"
            + "+\u0001\u0000\u0000\u0000\u01ef\u01f4\u0003\u0012\t\u0000\u01f0\u01f1"
            + "\u0005\f\u0000\u0000\u01f1\u01f3\u0003\u0012\t\u0000\u01f2\u01f0\u0001"
            + "\u0000\u0000\u0000\u01f3\u01f6\u0001\u0000\u0000\u0000\u01f4\u01f2\u0001"
            + "\u0000\u0000\u0000\u01f4\u01f5\u0001\u0000\u0000\u0000\u01f5\u01f8\u0001"
            + "\u0000\u0000\u0000\u01f6\u01f4\u0001\u0000\u0000\u0000\u01f7\u01f9\u0005"
            + "\f\u0000\u0000\u01f8\u01f7\u0001\u0000\u0000\u0000\u01f8\u01f9\u0001\u0000"
            + "\u0000\u0000\u01f9-\u0001\u0000\u0000\u0000\u01fa\u01fc\u0003\u0004\u0002"
            + "\u0000\u01fb\u01fa\u0001\u0000\u0000\u0000\u01fb\u01fc\u0001\u0000\u0000"
            + "\u0000\u01fc\u01fd\u0001\u0000\u0000\u0000\u01fd\u01ff\u0005\u0006\u0000"
            + "\u0000\u01fe\u0200\u0003<\u001e\u0000\u01ff\u01fe\u0001\u0000\u0000\u0000"
            + "\u01ff\u0200\u0001\u0000\u0000\u0000\u0200\u0201\u0001\u0000\u0000\u0000"
            + "\u0201\u0202\u0005\u0007\u0000\u0000\u0202\u0203\u0005:\u0000\u0000\u0203"
            + "\u0204\u0003\u0012\t\u0000\u0204/\u0001\u0000\u0000\u0000\u0205\u0207"
            + "\u0005L\u0000\u0000\u0206\u0208\u0003\u0004\u0002\u0000\u0207\u0206\u0001"
            + "\u0000\u0000\u0000\u0207\u0208\u0001\u0000\u0000\u0000\u0208\u0209\u0001"
            + "\u0000\u0000\u0000\u0209\u020b\u0005\u0006\u0000\u0000\u020a\u020c\u0003"
            + "<\u001e\u0000\u020b\u020a\u0001\u0000\u0000\u0000\u020b\u020c\u0001\u0000"
            + "\u0000\u0000\u020c\u020d\u0001\u0000\u0000\u0000\u020d\u020e\u0005\u0007"
            + "\u0000\u0000\u020e\u020f\u0005:\u0000\u0000\u020f\u0210\u0003\u0012\t"
            + "\u0000\u02101\u0001\u0000\u0000\u0000\u0211\u0212\u0005I\u0000\u0000\u0212"
            + "\u0213\u00034\u001a\u0000\u02133\u0001\u0000\u0000\u0000\u0214\u021f\u0003"
            + "\u0132\u0099\u0000\u0215\u0216\u0003\u0130\u0098\u0000\u0216\u0217\u0005"
            + "\u0012\u0000\u0000\u0217\u0219\u0001\u0000\u0000\u0000\u0218\u0215\u0001"
            + "\u0000\u0000\u0000\u0219\u021a\u0001\u0000\u0000\u0000\u021a\u0218\u0001"
            + "\u0000\u0000\u0000\u021a\u021b\u0001\u0000\u0000\u0000\u021b\u021c\u0001"
            + "\u0000\u0000\u0000\u021c\u021d\u0003\u0130\u0098\u0000\u021d\u021f\u0001"
            + "\u0000\u0000\u0000\u021e\u0214\u0001\u0000\u0000\u0000\u021e\u0218\u0001"
            + "\u0000\u0000\u0000\u021f5\u0001\u0000\u0000\u0000\u0220\u0222\u0005b\u0000"
            + "\u0000\u0221\u0220\u0001\u0000\u0000\u0000\u0221\u0222\u0001\u0000\u0000"
            + "\u0000\u0222\u0223\u0001\u0000\u0000\u0000\u0223\u0225\u0003\u0108\u0084"
            + "\u0000\u0224\u0226\u0005\u000e\u0000\u0000\u0225\u0224\u0001\u0000\u0000"
            + "\u0000\u0225\u0226\u0001\u0000\u0000\u0000\u0226\u0228\u0001\u0000\u0000"
            + "\u0000\u0227\u0229\u00038\u001c\u0000\u0228\u0227\u0001\u0000\u0000\u0000"
            + "\u0228\u0229\u0001\u0000\u0000\u0000\u0229\u022c\u0001\u0000\u0000\u0000"
            + "\u022a\u022b\u0005:\u0000\u0000\u022b\u022d\u0003\u0012\t\u0000\u022c"
            + "\u022a\u0001\u0000\u0000\u0000\u022c\u022d\u0001\u0000\u0000\u0000\u022d"
            + "7\u0001\u0000\u0000\u0000\u022e\u022f\u0005\u0010\u0000\u0000\u022f\u0230"
            + "\u0003\u0012\t\u0000\u02309\u0001\u0000\u0000\u0000\u0231\u0233\u0003"
            + "\u0004\u0002\u0000\u0232\u0231\u0001\u0000\u0000\u0000\u0232\u0233\u0001"
            + "\u0000\u0000\u0000\u0233\u0234\u0001\u0000\u0000\u0000\u0234\u0236\u0005"
            + "\u0006\u0000\u0000\u0235\u0237\u0003<\u001e\u0000\u0236\u0235\u0001\u0000"
            + "\u0000\u0000\u0236\u0237\u0001\u0000\u0000\u0000\u0237\u0238\u0001\u0000"
            + "\u0000\u0000\u0238\u023a\u0005\u0007\u0000\u0000\u0239\u023b\u00038\u001c"
            + "\u0000\u023a\u0239\u0001\u0000\u0000\u0000\u023a\u023b\u0001\u0000\u0000"
            + "\u0000\u023b;\u0001\u0000\u0000\u0000\u023c\u024d\u0003D\"\u0000\u023d"
            + "\u0242\u0003@ \u0000\u023e\u023f\u0005\f\u0000\u0000\u023f\u0241\u0003"
            + "@ \u0000\u0240\u023e\u0001\u0000\u0000\u0000\u0241\u0244\u0001\u0000\u0000"
            + "\u0000\u0242\u0240\u0001\u0000\u0000\u0000\u0242\u0243\u0001\u0000\u0000"
            + "\u0000\u0243\u0247\u0001\u0000\u0000\u0000\u0244\u0242\u0001\u0000\u0000"
            + "\u0000\u0245\u0246\u0005\f\u0000\u0000\u0246\u0248\u0003D\"\u0000\u0247"
            + "\u0245\u0001\u0000\u0000\u0000\u0247\u0248\u0001\u0000\u0000\u0000\u0248"
            + "\u024a\u0001\u0000\u0000\u0000\u0249\u024b\u0005\f\u0000\u0000\u024a\u0249"
            + "\u0001\u0000\u0000\u0000\u024a\u024b\u0001\u0000\u0000\u0000\u024b\u024d"
            + "\u0001\u0000\u0000\u0000\u024c\u023c\u0001\u0000\u0000\u0000\u024c\u023d"
            + "\u0001\u0000\u0000\u0000\u024d=\u0001\u0000\u0000\u0000\u024e\u0253\u0003"
            + "F#\u0000\u024f\u0250\u0005\f\u0000\u0000\u0250\u0252\u0003F#\u0000\u0251"
            + "\u024f\u0001\u0000\u0000\u0000\u0252\u0255\u0001\u0000\u0000\u0000\u0253"
            + "\u0251\u0001\u0000\u0000\u0000\u0253\u0254\u0001\u0000\u0000\u0000\u0254"
            + "?\u0001\u0000\u0000\u0000\u0255\u0253\u0001\u0000\u0000\u0000\u0256\u0259"
            + "\u0003F#\u0000\u0257\u0259\u0003B!\u0000\u0258\u0256\u0001\u0000\u0000"
            + "\u0000\u0258\u0257\u0001\u0000\u0000\u0000\u0259A\u0001\u0000\u0000\u0000"
            + "\u025a\u025c\u0003j5\u0000\u025b\u025a\u0001\u0000\u0000\u0000\u025b\u025c"
            + "\u0001\u0000\u0000\u0000\u025c\u025e\u0001\u0000\u0000\u0000\u025d\u025f"
            + "\u0003H$\u0000\u025e\u025d\u0001\u0000\u0000\u0000\u025e\u025f\u0001\u0000"
            + "\u0000\u0000\u025f\u0260\u0001\u0000\u0000\u0000\u0260\u0269\u0003J%\u0000"
            + "\u0261\u0263\u0005\u000e\u0000\u0000\u0262\u0264\u00038\u001c\u0000\u0263"
            + "\u0262\u0001\u0000\u0000\u0000\u0263\u0264\u0001\u0000\u0000\u0000\u0264"
            + "\u026a\u0001\u0000\u0000\u0000\u0265\u0267\u00038\u001c\u0000\u0266\u0265"
            + "\u0001\u0000\u0000\u0000\u0266\u0267\u0001\u0000\u0000\u0000\u0267\u0268"
            + "\u0001\u0000\u0000\u0000\u0268\u026a\u0003\u0000\u0000\u0000\u0269\u0261"
            + "\u0001\u0000\u0000\u0000\u0269\u0266\u0001\u0000\u0000\u0000\u026aC\u0001"
            + "\u0000\u0000\u0000\u026b\u026c\u0005\u0011\u0000\u0000\u026c\u026e\u0003"
            + "\u0112\u0089\u0000\u026d\u026f\u00038\u001c\u0000\u026e\u026d\u0001\u0000"
            + "\u0000\u0000\u026e\u026f\u0001\u0000\u0000\u0000\u026fE\u0001\u0000\u0000"
            + "\u0000\u0270\u0272\u0003j5\u0000\u0271\u0270\u0001\u0000\u0000\u0000\u0271"
            + "\u0272\u0001\u0000\u0000\u0000\u0272\u0274\u0001\u0000\u0000\u0000\u0273"
            + "\u0275\u0003H$\u0000\u0274\u0273\u0001\u0000\u0000\u0000\u0274\u0275\u0001"
            + "\u0000\u0000\u0000\u0275\u0276\u0001\u0000\u0000\u0000\u0276\u0278\u0003"
            + "J%\u0000\u0277\u0279\u00038\u001c\u0000\u0278\u0277\u0001\u0000\u0000"
            + "\u0000\u0278\u0279\u0001\u0000\u0000\u0000\u0279G\u0001\u0000\u0000\u0000"
            + "\u027a\u027b\u0007\u0002\u0000\u0000\u027bI\u0001\u0000\u0000\u0000\u027c"
            + "\u027f\u0003\u0130\u0098\u0000\u027d\u027f\u0003\u0002\u0001\u0000\u027e"
            + "\u027c\u0001\u0000\u0000\u0000\u027e\u027d\u0001\u0000\u0000\u0000\u027f"
            + "K\u0001\u0000\u0000\u0000\u0280\u0282\u0005L\u0000\u0000\u0281\u0283\u0003"
            + "\u0004\u0002\u0000\u0282\u0281\u0001\u0000\u0000\u0000\u0282\u0283\u0001"
            + "\u0000\u0000\u0000\u0283\u0284\u0001\u0000\u0000\u0000\u0284\u0286\u0005"
            + "\u0006\u0000\u0000\u0285\u0287\u0003<\u001e\u0000\u0286\u0285\u0001\u0000"
            + "\u0000\u0000\u0286\u0287\u0001\u0000\u0000\u0000\u0287\u0288\u0001\u0000"
            + "\u0000\u0000\u0288\u028a\u0005\u0007\u0000\u0000\u0289\u028b\u00038\u001c"
            + "\u0000\u028a\u0289\u0001\u0000\u0000\u0000\u028a\u028b\u0001\u0000\u0000"
            + "\u0000\u028bM\u0001\u0000\u0000\u0000\u028c\u028d\u0005\u0004\u0000\u0000"
            + "\u028d\u028e\u0003\u0132\u0099\u0000\u028e\u028f\u0005\u0010\u0000\u0000"
            + "\u028f\u0290\u0007\u0003\u0000\u0000\u0290\u0291\u0005\u0005\u0000\u0000"
            + "\u0291\u0292\u00038\u001c\u0000\u0292O\u0001\u0000\u0000\u0000\u0293\u0295"
            + "\u0003\u0108\u0084\u0000\u0294\u0296\u0005\u000e\u0000\u0000\u0295\u0294"
            + "\u0001\u0000\u0000\u0000\u0295\u0296\u0001\u0000\u0000\u0000\u0296\u0297"
            + "\u0001\u0000\u0000\u0000\u0297\u0298\u0003:\u001d\u0000\u0298Q\u0001\u0000"
            + "\u0000\u0000\u0299\u029b\u0005l\u0000\u0000\u029a\u0299\u0001\u0000\u0000"
            + "\u0000\u029a\u029b\u0001\u0000\u0000\u0000\u029b\u029c\u0001\u0000\u0000"
            + "\u0000\u029c\u029d\u0005\u0081\u0000\u0000\u029d\u029f\u0003\u0132\u0099"
            + "\u0000\u029e\u02a0\u0003\u0004\u0002\u0000\u029f\u029e\u0001\u0000\u0000"
            + "\u0000\u029f\u02a0\u0001\u0000\u0000\u0000\u02a0\u02a1\u0001\u0000\u0000"
            + "\u0000\u02a1\u02a2\u0005\r\u0000\u0000\u02a2\u02a3\u0003\u0012\t\u0000"
            + "\u02a3\u02a4\u0003\u013a\u009d\u0000\u02a4S\u0001\u0000\u0000\u0000\u02a5"
            + "\u02a7\u0003H$\u0000\u02a6\u02a5\u0001\u0000\u0000\u0000\u02a6\u02a7\u0001"
            + "\u0000\u0000\u0000\u02a7\u02a8\u0001\u0000\u0000\u0000\u02a8\u02a9\u0005"
            + "\u0082\u0000\u0000\u02a9\u02ab\u0005\u0006\u0000\u0000\u02aa\u02ac\u0003"
            + "\u00f0x\u0000\u02ab\u02aa\u0001\u0000\u0000\u0000\u02ab\u02ac\u0001\u0000"
            + "\u0000\u0000\u02ac\u02ad\u0001\u0000\u0000\u0000\u02ad\u02b3\u0005\u0007"
            + "\u0000\u0000\u02ae\u02af\u0005\b\u0000\u0000\u02af\u02b0\u0003\u00f6{"
            + "\u0000\u02b0\u02b1\u0005\n\u0000\u0000\u02b1\u02b4\u0001\u0000\u0000\u0000"
            + "\u02b2\u02b4\u0005\u000b\u0000\u0000\u02b3\u02ae\u0001\u0000\u0000\u0000"
            + "\u02b3\u02b2\u0001\u0000\u0000\u0000\u02b3\u02b4\u0001\u0000\u0000\u0000"
            + "\u02b4U\u0001\u0000\u0000\u0000\u02b5\u02b7\u0005l\u0000\u0000\u02b6\u02b5"
            + "\u0001\u0000\u0000\u0000\u02b6\u02b7\u0001\u0000\u0000\u0000\u02b7\u02b9"
            + "\u0001\u0000\u0000\u0000\u02b8\u02ba\u0005\u0086\u0000\u0000\u02b9\u02b8"
            + "\u0001\u0000\u0000\u0000\u02b9\u02ba\u0001\u0000\u0000\u0000\u02ba\u02bb"
            + "\u0001\u0000\u0000\u0000\u02bb\u02bc\u0005r\u0000\u0000\u02bc\u02be\u0003"
            + "\u0132\u0099\u0000\u02bd\u02bf\u0003\u0004\u0002\u0000\u02be\u02bd\u0001"
            + "\u0000\u0000\u0000\u02be\u02bf\u0001\u0000\u0000\u0000\u02bf\u02c1\u0001"
            + "\u0000\u0000\u0000\u02c0\u02c2\u0003X,\u0000\u02c1\u02c0\u0001\u0000\u0000"
            + "\u0000\u02c1\u02c2\u0001\u0000\u0000\u0000\u02c2\u02c3\u0001\u0000\u0000"
            + "\u0000\u02c3\u02c5\u0003 \u0010\u0000\u02c4\u02c6\u0005\u000b\u0000\u0000"
            + "\u02c5\u02c4\u0001\u0000\u0000\u0000\u02c5\u02c6\u0001\u0000\u0000\u0000"
            + "\u02c6W\u0001\u0000\u0000\u0000\u02c7\u02c8\u0005i\u0000\u0000\u02c8\u02c9"
            + "\u0003Z-\u0000\u02c9Y\u0001\u0000\u0000\u0000\u02ca\u02cf\u0003\u001a"
            + "\r\u0000\u02cb\u02cc\u0005\f\u0000\u0000\u02cc\u02ce\u0003\u001a\r\u0000"
            + "\u02cd\u02cb\u0001\u0000\u0000\u0000\u02ce\u02d1\u0001\u0000\u0000\u0000"
            + "\u02cf\u02cd\u0001\u0000\u0000\u0000\u02cf\u02d0\u0001\u0000\u0000\u0000"
            + "\u02d0[\u0001\u0000\u0000\u0000\u02d1\u02cf\u0001\u0000\u0000\u0000\u02d2"
            + "\u02d4\u0005k\u0000\u0000\u02d3\u02d2\u0001\u0000\u0000\u0000\u02d3\u02d4"
            + "\u0001\u0000\u0000\u0000\u02d4\u02d5\u0001\u0000\u0000\u0000\u02d5\u02d6"
            + "\u0005h\u0000\u0000\u02d6\u02d7\u0003\u0132\u0099\u0000\u02d7\u02d9\u0005"
            + "\b\u0000\u0000\u02d8\u02da\u0003^/\u0000\u02d9\u02d8\u0001\u0000\u0000"
            + "\u0000\u02d9\u02da\u0001\u0000\u0000\u0000\u02da\u02db\u0001\u0000\u0000"
            + "\u0000\u02db\u02dc\u0005\n\u0000\u0000\u02dc]\u0001\u0000\u0000\u0000"
            + "\u02dd\u02df\u0003`0\u0000\u02de\u02e0\u0005\f\u0000\u0000\u02df\u02de"
            + "\u0001\u0000\u0000\u0000\u02df\u02e0\u0001\u0000\u0000\u0000\u02e0_\u0001"
            + "\u0000\u0000\u0000\u02e1\u02e6\u0003b1\u0000\u02e2\u02e3\u0005\f\u0000"
            + "\u0000\u02e3\u02e5\u0003b1\u0000\u02e4\u02e2\u0001\u0000\u0000\u0000\u02e5"
            + "\u02e8\u0001\u0000\u0000\u0000\u02e6\u02e4\u0001\u0000\u0000\u0000\u02e6"
            + "\u02e7\u0001\u0000\u0000\u0000\u02e7a\u0001\u0000\u0000\u0000\u02e8\u02e6"
            + "\u0001\u0000\u0000\u0000\u02e9\u02ec\u0003\u0108\u0084\u0000\u02ea\u02eb"
            + "\u0005\r\u0000\u0000\u02eb\u02ed\u0003\u0112\u0089\u0000\u02ec\u02ea\u0001"
            + "\u0000\u0000\u0000\u02ec\u02ed\u0001\u0000\u0000\u0000\u02edc\u0001\u0000"
            + "\u0000\u0000\u02ee\u02f0\u0005\u0086\u0000\u0000\u02ef\u02ee\u0001\u0000"
            + "\u0000\u0000\u02ef\u02f0\u0001\u0000\u0000\u0000\u02f0\u02f1\u0001\u0000"
            + "\u0000\u0000\u02f1\u02f2\u0005\u0083\u0000\u0000\u02f2\u02f3\u0003f3\u0000"
            + "\u02f3\u02f5\u0005\b\u0000\u0000\u02f4\u02f6\u0003z=\u0000\u02f5\u02f4"
            + "\u0001\u0000\u0000\u0000\u02f5\u02f6\u0001\u0000\u0000\u0000\u02f6\u02f7"
            + "\u0001\u0000\u0000\u0000\u02f7\u02f8\u0005\n\u0000\u0000\u02f8e\u0001"
            + "\u0000\u0000\u0000\u02f9\u0302\u0003\u0132\u0099\u0000\u02fa\u02fc\u0005"
            + "\u0012\u0000\u0000\u02fb\u02fa\u0001\u0000\u0000\u0000\u02fc\u02fd\u0001"
            + "\u0000\u0000\u0000\u02fd\u02fb\u0001\u0000\u0000\u0000\u02fd\u02fe\u0001"
            + "\u0000\u0000\u0000\u02fe\u02ff\u0001\u0000\u0000\u0000\u02ff\u0301\u0003"
            + "\u0132\u0099\u0000\u0300\u02fb\u0001\u0000\u0000\u0000\u0301\u0304\u0001"
            + "\u0000\u0000\u0000\u0302\u0300\u0001\u0000\u0000\u0000\u0302\u0303\u0001"
            + "\u0000\u0000\u0000\u0303g\u0001\u0000\u0000\u0000\u0304\u0302\u0001\u0000"
            + "\u0000\u0000\u0305\u0306\u0003\u0132\u0099\u0000\u0306\u0307\u0005\r\u0000"
            + "\u0000\u0307\u0308\u0003f3\u0000\u0308\u0309\u0005\u000b\u0000\u0000\u0309"
            + "i\u0001\u0000\u0000\u0000\u030a\u030c\u0003l6\u0000\u030b\u030a\u0001"
            + "\u0000\u0000\u0000\u030c\u030d\u0001\u0000\u0000\u0000\u030d\u030b\u0001"
            + "\u0000\u0000\u0000\u030d\u030e\u0001\u0000\u0000\u0000\u030ek\u0001\u0000"
            + "\u0000\u0000\u030f\u0312\u0005\u0089\u0000\u0000\u0310\u0313\u0003n7\u0000"
            + "\u0311\u0313\u0003p8\u0000\u0312\u0310\u0001\u0000\u0000\u0000\u0312\u0311"
            + "\u0001\u0000\u0000\u0000\u0313m\u0001\u0000\u0000\u0000\u0314\u0315\u0006"
            + "7\uffff\uffff\u0000\u0315\u031b\u0003\u0132\u0099\u0000\u0316\u0317\u0005"
            + "\u0006\u0000\u0000\u0317\u0318\u0003\u0112\u0089\u0000\u0318\u0319\u0005"
            + "\u0007\u0000\u0000\u0319\u031b\u0001\u0000\u0000\u0000\u031a\u0314\u0001"
            + "\u0000\u0000\u0000\u031a\u0316\u0001\u0000\u0000\u0000\u031b\u0321\u0001"
            + "\u0000\u0000\u0000\u031c\u031d\n\u0002\u0000\u0000\u031d\u031e\u0005\u0012"
            + "\u0000\u0000\u031e\u0320\u0003\u0130\u0098\u0000\u031f\u031c\u0001\u0000"
            + "\u0000\u0000\u0320\u0323\u0001\u0000\u0000\u0000\u0321\u031f\u0001\u0000"
            + "\u0000\u0000\u0321\u0322\u0001\u0000\u0000\u0000\u0322o\u0001\u0000\u0000"
            + "\u0000\u0323\u0321\u0001\u0000\u0000\u0000\u0324\u0325\u0003n7\u0000\u0325"
            + "\u0326\u0003\u010a\u0085\u0000\u0326q\u0001\u0000\u0000\u0000\u0327\u0329"
            + "\u0003\u00f8|\u0000\u0328\u0327\u0001\u0000\u0000\u0000\u0328\u0329\u0001"
            + "\u0000\u0000\u0000\u0329\u032a\u0001\u0000\u0000\u0000\u032a\u032b\u0005"
            + "\u0000\u0000\u0001\u032bs\u0001\u0000\u0000\u0000\u032c\u032e\u0005l\u0000"
            + "\u0000\u032d\u032c\u0001\u0000\u0000\u0000\u032d\u032e\u0001\u0000\u0000"
            + "\u0000\u032e\u032f\u0001\u0000\u0000\u0000\u032f\u0330\u0003v;\u0000\u0330"
            + "u\u0001\u0000\u0000\u0000\u0331\u034f\u0003x<\u0000\u0332\u034f\u0003"
            + "\u009cN\u0000\u0333\u034f\u0003~?\u0000\u0334\u034f\u0003\u0092I\u0000"
            + "\u0335\u034f\u0003\u00a2Q\u0000\u0336\u034f\u0003|>\u0000\u0337\u034f"
            + "\u0003\u00ceg\u0000\u0338\u034f\u0003\u00ccf\u0000\u0339\u034f\u0003\u00a4"
            + "R\u0000\u033a\u034f\u0003V+\u0000\u033b\u034f\u0003d2\u0000\u033c\u034f"
            + "\u0003\u00a6S\u0000\u033d\u034f\u0003\u00a8T\u0000\u033e\u034f\u0003\u00ac"
            + "V\u0000\u033f\u034f\u0003\u00aeW\u0000\u0340\u034f\u0003\u00b0X\u0000"
            + "\u0341\u034f\u0003\u00b2Y\u0000\u0342\u034f\u0003\u00b4Z\u0000\u0343\u034f"
            + "\u0003\u00c0`\u0000\u0344\u034f\u0003\u00b6[\u0000\u0345\u034f\u0003\u00c2"
            + "a\u0000\u0346\u034f\u0003\u00c4b\u0000\u0347\u034f\u0003\u00cae\u0000"
            + "\u0348\u034f\u0003\u011a\u008d\u0000\u0349\u034f\u0003\u00e2q\u0000\u034a"
            + "\u034f\u0003R)\u0000\u034b\u034f\u0003\\.\u0000\u034c\u034d\u0005l\u0000"
            + "\u0000\u034d\u034f\u0003v;\u0000\u034e\u0331\u0001\u0000\u0000\u0000\u034e"
            + "\u0332\u0001\u0000\u0000\u0000\u034e\u0333\u0001\u0000\u0000\u0000\u034e"
            + "\u0334\u0001\u0000\u0000\u0000\u034e\u0335\u0001\u0000\u0000\u0000\u034e"
            + "\u0336\u0001\u0000\u0000\u0000\u034e\u0337\u0001\u0000\u0000\u0000\u034e"
            + "\u0338\u0001\u0000\u0000\u0000\u034e\u0339\u0001\u0000\u0000\u0000\u034e"
            + "\u033a\u0001\u0000\u0000\u0000\u034e\u033b\u0001\u0000\u0000\u0000\u034e"
            + "\u033c\u0001\u0000\u0000\u0000\u034e\u033d\u0001\u0000\u0000\u0000\u034e"
            + "\u033e\u0001\u0000\u0000\u0000\u034e\u033f\u0001\u0000\u0000\u0000\u034e"
            + "\u0340\u0001\u0000\u0000\u0000\u034e\u0341\u0001\u0000\u0000\u0000\u034e"
            + "\u0342\u0001\u0000\u0000\u0000\u034e\u0343\u0001\u0000\u0000\u0000\u034e"
            + "\u0344\u0001\u0000\u0000\u0000\u034e\u0345\u0001\u0000\u0000\u0000\u034e"
            + "\u0346\u0001\u0000\u0000\u0000\u034e\u0347\u0001\u0000\u0000\u0000\u034e"
            + "\u0348\u0001\u0000\u0000\u0000\u034e\u0349\u0001\u0000\u0000\u0000\u034e"
            + "\u034a\u0001\u0000\u0000\u0000\u034e\u034b\u0001\u0000\u0000\u0000\u034e"
            + "\u034c\u0001\u0000\u0000\u0000\u034fw\u0001\u0000\u0000\u0000\u0350\u0352"
            + "\u0005\b\u0000\u0000\u0351\u0353\u0003z=\u0000\u0352\u0351\u0001\u0000"
            + "\u0000\u0000\u0352\u0353\u0001\u0000\u0000\u0000\u0353\u0354\u0001\u0000"
            + "\u0000\u0000\u0354\u0355\u0005\n\u0000\u0000\u0355y\u0001\u0000\u0000"
            + "\u0000\u0356\u0358\u0003v;\u0000\u0357\u0356\u0001\u0000\u0000\u0000\u0358"
            + "\u0359\u0001\u0000\u0000\u0000\u0359\u0357\u0001\u0000\u0000\u0000\u0359"
            + "\u035a\u0001\u0000\u0000\u0000\u035a{\u0001\u0000\u0000\u0000\u035b\u0360"
            + "\u0005\u0087\u0000\u0000\u035c\u035d\u0003\u0132\u0099\u0000\u035d\u035e"
            + "\u0003:\u001d\u0000\u035e\u0361\u0001\u0000\u0000\u0000\u035f\u0361\u0003"
            + "\u009cN\u0000\u0360\u035c\u0001\u0000\u0000\u0000\u0360\u035f\u0001\u0000"
            + "\u0000\u0000\u0361\u0362\u0001\u0000\u0000\u0000\u0362\u0363\u0003\u013a"
            + "\u009d\u0000\u0363}\u0001\u0000\u0000\u0000\u0364\u0365\u0005m\u0000\u0000"
            + "\u0365\u0366\u0003\u0080@\u0000\u0366\u007f\u0001\u0000\u0000\u0000\u0367"
            + "\u0369\u0003\u008aE\u0000\u0368\u0367\u0001\u0000\u0000\u0000\u0368\u0369"
            + "\u0001\u0000\u0000\u0000\u0369\u036c\u0001\u0000\u0000\u0000\u036a\u036d"
            + "\u0003\u008cF\u0000\u036b\u036d\u0003\u0082A\u0000\u036c\u036a\u0001\u0000"
            + "\u0000\u0000\u036c\u036b\u0001\u0000\u0000\u0000\u036d\u036e\u0001\u0000"
            + "\u0000\u0000\u036e\u036f\u0003\u008eG\u0000\u036f\u0370\u0003\u013a\u009d"
            + "\u0000\u0370\u0374\u0001\u0000\u0000\u0000\u0371\u0372\u0005\u008b\u0000"
            + "\u0000\u0372\u0374\u0003\u013a\u009d\u0000\u0373\u0368\u0001\u0000\u0000"
            + "\u0000\u0373\u0371\u0001\u0000\u0000\u0000\u0374\u0081\u0001\u0000\u0000"
            + "\u0000\u0375\u037b\u0005\b\u0000\u0000\u0376\u0377\u0003\u0084B\u0000"
            + "\u0377\u0378\u0005\f\u0000\u0000\u0378\u037a\u0001\u0000\u0000\u0000\u0379"
            + "\u0376\u0001\u0000\u0000\u0000\u037a\u037d\u0001\u0000\u0000\u0000\u037b"
            + "\u0379\u0001\u0000\u0000\u0000\u037b\u037c\u0001\u0000\u0000\u0000\u037c"
            + "\u0382\u0001\u0000\u0000\u0000\u037d\u037b\u0001\u0000\u0000\u0000\u037e"
            + "\u0380\u0003\u0084B\u0000\u037f\u0381\u0005\f\u0000\u0000\u0380\u037f"
            + "\u0001\u0000\u0000\u0000\u0380\u0381\u0001\u0000\u0000\u0000\u0381\u0383"
            + "\u0001\u0000\u0000\u0000\u0382\u037e\u0001\u0000\u0000\u0000\u0382\u0383"
            + "\u0001\u0000\u0000\u0000\u0383\u0384\u0001\u0000\u0000\u0000\u0384\u0385"
            + "\u0005\n\u0000\u0000\u0385\u0083\u0001\u0000\u0000\u0000\u0386\u0389\u0003"
            + "\u0086C\u0000\u0387\u0388\u0005`\u0000\u0000\u0388\u038a\u0003\u0088D"
            + "\u0000\u0389\u0387\u0001\u0000\u0000\u0000\u0389\u038a\u0001\u0000\u0000"
            + "\u0000\u038a\u0085\u0001\u0000\u0000\u0000\u038b\u038e\u0003\u0130\u0098"
            + "\u0000\u038c\u038e\u0005\u008b\u0000\u0000\u038d\u038b\u0001\u0000\u0000"
            + "\u0000\u038d\u038c\u0001\u0000\u0000\u0000\u038e\u0087\u0001\u0000\u0000"
            + "\u0000\u038f\u0390\u0007\u0004\u0000\u0000\u0390\u0089\u0001\u0000\u0000"
            + "\u0000\u0391\u0392\u0003\u0090H\u0000\u0392\u0393\u0005\f\u0000\u0000"
            + "\u0393\u008b\u0001\u0000\u0000\u0000\u0394\u0397\u0005\u0019\u0000\u0000"
            + "\u0395\u0397\u0003\u0130\u0098\u0000\u0396\u0394\u0001\u0000\u0000\u0000"
            + "\u0396\u0395\u0001\u0000\u0000\u0000\u0397\u039a\u0001\u0000\u0000\u0000"
            + "\u0398\u0399\u0005`\u0000\u0000\u0399\u039b\u0003\u0130\u0098\u0000\u039a"
            + "\u0398\u0001\u0000\u0000\u0000\u039a\u039b\u0001\u0000\u0000\u0000\u039b"
            + "\u008d\u0001\u0000\u0000\u0000\u039c\u039d\u0005a\u0000\u0000\u039d\u039e"
            + "\u0005\u008b\u0000\u0000\u039e\u008f\u0001\u0000\u0000\u0000\u039f\u03a2"
            + "\u0003\u0130\u0098\u0000\u03a0\u03a1\u0005`\u0000\u0000\u03a1\u03a3\u0003"
            + "\u0130\u0098\u0000\u03a2\u03a0\u0001\u0000\u0000\u0000\u03a2\u03a3\u0001"
            + "\u0000\u0000\u0000\u03a3\u0091\u0001\u0000\u0000\u0000\u03a4\u03a6\u0005"
            + "l\u0000\u0000\u03a5\u03a7\u0005Z\u0000\u0000\u03a6\u03a5\u0001\u0000\u0000"
            + "\u0000\u03a6\u03a7\u0001\u0000\u0000\u0000\u03a7\u03aa\u0001\u0000\u0000"
            + "\u0000\u03a8\u03ab\u0003\u0094J\u0000\u03a9\u03ab\u0003\u009aM\u0000\u03aa"
            + "\u03a8\u0001\u0000\u0000\u0000\u03aa\u03a9\u0001\u0000\u0000\u0000\u03ab"
            + "\u03ac\u0001\u0000\u0000\u0000\u03ac\u03ad\u0003\u013a\u009d\u0000\u03ad"
            + "\u03b4\u0001\u0000\u0000\u0000\u03ae\u03af\u0005l\u0000\u0000\u03af\u03b0"
            + "\u0005Z\u0000\u0000\u03b0\u03b1\u0003\u0112\u0089\u0000\u03b1\u03b2\u0003"
            + "\u013a\u009d\u0000\u03b2\u03b4\u0001\u0000\u0000\u0000\u03b3\u03a4\u0001"
            + "\u0000\u0000\u0000\u03b3\u03ae\u0001\u0000\u0000\u0000\u03b4\u0093\u0001"
            + "\u0000\u0000\u0000\u03b5\u03b6\u0003\u008cF\u0000\u03b6\u03b7\u0003\u008e"
            + "G\u0000\u03b7\u03b8\u0003\u013a\u009d\u0000\u03b8\u03c0\u0001\u0000\u0000"
            + "\u0000\u03b9\u03bb\u0003\u0096K\u0000\u03ba\u03bc\u0003\u008eG\u0000\u03bb"
            + "\u03ba\u0001\u0000\u0000\u0000\u03bb\u03bc\u0001\u0000\u0000\u0000\u03bc"
            + "\u03bd\u0001\u0000\u0000\u0000\u03bd\u03be\u0003\u013a\u009d\u0000\u03be"
            + "\u03c0\u0001\u0000\u0000\u0000\u03bf\u03b5\u0001\u0000\u0000\u0000\u03bf"
            + "\u03b9\u0001\u0000\u0000\u0000\u03c0\u0095\u0001\u0000\u0000\u0000\u03c1"
            + "\u03c7\u0005\b\u0000\u0000\u03c2\u03c3\u0003\u0098L\u0000\u03c3\u03c4"
            + "\u0005\f\u0000\u0000\u03c4\u03c6\u0001\u0000\u0000\u0000\u03c5\u03c2\u0001"
            + "\u0000\u0000\u0000\u03c6\u03c9\u0001\u0000\u0000\u0000\u03c7\u03c5\u0001"
            + "\u0000\u0000\u0000\u03c7\u03c8\u0001\u0000\u0000\u0000\u03c8\u03ce\u0001"
            + "\u0000\u0000\u0000\u03c9\u03c7\u0001\u0000\u0000\u0000\u03ca\u03cc\u0003"
            + "\u0098L\u0000\u03cb\u03cd\u0005\f\u0000\u0000\u03cc\u03cb\u0001\u0000"
            + "\u0000\u0000\u03cc\u03cd\u0001\u0000\u0000\u0000\u03cd\u03cf\u0001\u0000"
            + "\u0000\u0000\u03ce\u03ca\u0001\u0000\u0000\u0000\u03ce\u03cf\u0001\u0000"
            + "\u0000\u0000\u03cf\u03d0\u0001\u0000\u0000\u0000\u03d0\u03d1\u0005\n\u0000"
            + "\u0000\u03d1\u0097\u0001\u0000\u0000\u0000\u03d2\u03d5\u0003\u0086C\u0000"
            + "\u03d3\u03d4\u0005`\u0000\u0000\u03d4\u03d6\u0003\u0086C\u0000\u03d5\u03d3"
            + "\u0001\u0000\u0000\u0000\u03d5\u03d6\u0001\u0000\u0000\u0000\u03d6\u0099"
            + "\u0001\u0000\u0000\u0000\u03d7\u03db\u0003\u009cN\u0000\u03d8\u03db\u0003"
            + "\u00ceg\u0000\u03d9\u03db\u0003\u00ccf\u0000\u03da\u03d7\u0001\u0000\u0000"
            + "\u0000\u03da\u03d8\u0001\u0000\u0000\u0000\u03da\u03d9\u0001\u0000\u0000"
            + "\u0000\u03db\u009b\u0001\u0000\u0000\u0000\u03dc\u03de\u0003\u0002\u0001"
            + "\u0000\u03dd\u03df\u00038\u001c\u0000\u03de\u03dd\u0001\u0000\u0000\u0000"
            + "\u03de\u03df\u0001\u0000\u0000\u0000\u03df\u03e0\u0001\u0000\u0000\u0000"
            + "\u03e0\u03e2\u0003\u0000\u0000\u0000\u03e1\u03e3\u0005\u000b\u0000\u0000"
            + "\u03e2\u03e1\u0001\u0000\u0000\u0000\u03e2\u03e3\u0001\u0000\u0000\u0000"
            + "\u03e3\u03fa\u0001\u0000\u0000\u0000\u03e4\u03e6\u0003H$\u0000\u03e5\u03e4"
            + "\u0001\u0000\u0000\u0000\u03e5\u03e6\u0001\u0000\u0000\u0000\u03e6\u03e8"
            + "\u0001\u0000\u0000\u0000\u03e7\u03e9\u0003\u00aaU\u0000\u03e8\u03e7\u0001"
            + "\u0000\u0000\u0000\u03e8\u03e9\u0001\u0000\u0000\u0000\u03e9\u03eb\u0001"
            + "\u0000\u0000\u0000\u03ea\u03ec\u0005b\u0000\u0000\u03eb\u03ea\u0001\u0000"
            + "\u0000\u0000\u03eb\u03ec\u0001\u0000\u0000\u0000\u03ec\u03ed\u0001\u0000"
            + "\u0000\u0000\u03ed\u03ef\u0003\u009eO\u0000\u03ee\u03f0\u0005\u000b\u0000"
            + "\u0000\u03ef\u03ee\u0001\u0000\u0000\u0000\u03ef\u03f0\u0001\u0000\u0000"
            + "\u0000\u03f0\u03fa\u0001\u0000\u0000\u0000\u03f1\u03f3\u0005\u0086\u0000"
            + "\u0000\u03f2\u03f4\u0003\u00aaU\u0000\u03f3\u03f2\u0001\u0000\u0000\u0000"
            + "\u03f3\u03f4\u0001\u0000\u0000\u0000\u03f4\u03f5\u0001\u0000\u0000\u0000"
            + "\u03f5\u03f7\u0003\u009eO\u0000\u03f6\u03f8\u0005\u000b\u0000\u0000\u03f7"
            + "\u03f6\u0001\u0000\u0000\u0000\u03f7\u03f8\u0001\u0000\u0000\u0000\u03f8"
            + "\u03fa\u0001\u0000\u0000\u0000\u03f9\u03dc\u0001\u0000\u0000\u0000\u03f9"
            + "\u03e5\u0001\u0000\u0000\u0000\u03f9\u03f1\u0001\u0000\u0000\u0000\u03fa"
            + "\u009d\u0001\u0000\u0000\u0000\u03fb\u0400\u0003\u00a0P\u0000\u03fc\u03fd"
            + "\u0005\f\u0000\u0000\u03fd\u03ff\u0003\u00a0P\u0000\u03fe\u03fc\u0001"
            + "\u0000\u0000\u0000\u03ff\u0402\u0001\u0000\u0000\u0000\u0400\u03fe\u0001"
            + "\u0000\u0000\u0000\u0400\u0401\u0001\u0000\u0000\u0000\u0401\u009f\u0001"
            + "\u0000\u0000\u0000\u0402\u0400\u0001\u0000\u0000\u0000\u0403\u0407\u0003"
            + "\u0134\u009a\u0000\u0404\u0407\u0003\u00fa}\u0000\u0405\u0407\u0003\u0100"
            + "\u0080\u0000\u0406\u0403\u0001\u0000\u0000\u0000\u0406\u0404\u0001\u0000"
            + "\u0000\u0000\u0406\u0405\u0001\u0000\u0000\u0000\u0407\u0409\u0001\u0000"
            + "\u0000\u0000\u0408\u040a\u00038\u001c\u0000\u0409\u0408\u0001\u0000\u0000"
            + "\u0000\u0409\u040a\u0001\u0000\u0000\u0000\u040a\u040c\u0001\u0000\u0000"
            + "\u0000\u040b\u040d\u0003\u0112\u0089\u0000\u040c\u040b\u0001\u0000\u0000"
            + "\u0000\u040c\u040d\u0001\u0000\u0000\u0000\u040d\u0413\u0001\u0000\u0000"
            + "\u0000\u040e\u0410\u0005\r\u0000\u0000\u040f\u0411\u0003\u0004\u0002\u0000"
            + "\u0410\u040f\u0001\u0000\u0000\u0000\u0410\u0411\u0001\u0000\u0000\u0000"
            + "\u0411\u0412\u0001\u0000\u0000\u0000\u0412\u0414\u0003\u0112\u0089\u0000"
            + "\u0413\u040e\u0001\u0000\u0000\u0000\u0413\u0414\u0001\u0000\u0000\u0000"
            + "\u0414\u00a1\u0001\u0000\u0000\u0000\u0415\u0416\u0005\u000b\u0000\u0000"
            + "\u0416\u00a3\u0001\u0000\u0000\u0000\u0417\u0418\u0004R\u0006\u0000\u0418"
            + "\u041a\u0003\u0110\u0088\u0000\u0419\u041b\u0005\u000b\u0000\u0000\u041a"
            + "\u0419\u0001\u0000\u0000\u0000\u041a\u041b\u0001\u0000\u0000\u0000\u041b"
            + "\u00a5\u0001\u0000\u0000\u0000\u041c\u041d\u0005[\u0000\u0000\u041d\u041e"
            + "\u0005\u0006\u0000\u0000\u041e\u041f\u0003\u0110\u0088\u0000\u041f\u0420"
            + "\u0005\u0007\u0000\u0000\u0420\u0423\u0003v;\u0000\u0421\u0422\u0005K"
            + "\u0000\u0000\u0422\u0424\u0003v;\u0000\u0423\u0421\u0001\u0000\u0000\u0000"
            + "\u0423\u0424\u0001\u0000\u0000\u0000\u0424\u00a7\u0001\u0000\u0000\u0000"
            + "\u0425\u0426\u0005G\u0000\u0000\u0426\u0427\u0003v;\u0000\u0427\u0428"
            + "\u0005U\u0000\u0000\u0428\u0429\u0005\u0006\u0000\u0000\u0429\u042a\u0003"
            + "\u0110\u0088\u0000\u042a\u042b\u0005\u0007\u0000\u0000\u042b\u042c\u0003"
            + "\u013a\u009d\u0000\u042c\u0484\u0001\u0000\u0000\u0000\u042d\u042e\u0005"
            + "U\u0000\u0000\u042e\u042f\u0005\u0006\u0000\u0000\u042f\u0430\u0003\u0110"
            + "\u0088\u0000\u0430\u0431\u0005\u0007\u0000\u0000\u0431\u0432\u0003v;\u0000"
            + "\u0432\u0484\u0001\u0000\u0000\u0000\u0433\u0434\u0005S\u0000\u0000\u0434"
            + "\u0436\u0005\u0006\u0000\u0000\u0435\u0437\u0003\u0110\u0088\u0000\u0436"
            + "\u0435\u0001\u0000\u0000\u0000\u0436\u0437\u0001\u0000\u0000\u0000\u0437"
            + "\u0438\u0001\u0000\u0000\u0000\u0438\u043a\u0005\u000b\u0000\u0000\u0439"
            + "\u043b\u0003\u0110\u0088\u0000\u043a\u0439\u0001\u0000\u0000\u0000\u043a"
            + "\u043b\u0001\u0000\u0000\u0000\u043b\u043c\u0001\u0000\u0000\u0000\u043c"
            + "\u043e\u0005\u000b\u0000\u0000\u043d\u043f\u0003\u0110\u0088\u0000\u043e"
            + "\u043d\u0001\u0000\u0000\u0000\u043e\u043f\u0001\u0000\u0000\u0000\u043f"
            + "\u0440\u0001\u0000\u0000\u0000\u0440\u0441\u0005\u0007\u0000\u0000\u0441"
            + "\u0484\u0003v;\u0000\u0442\u0443\u0005S\u0000\u0000\u0443\u0444\u0005"
            + "\u0006\u0000\u0000\u0444\u0445\u0003\u00aaU\u0000\u0445\u0446\u0003\u009e"
            + "O\u0000\u0446\u0448\u0005\u000b\u0000\u0000\u0447\u0449\u0003\u0110\u0088"
            + "\u0000\u0448\u0447\u0001\u0000\u0000\u0000\u0448\u0449\u0001\u0000\u0000"
            + "\u0000\u0449\u044a\u0001\u0000\u0000\u0000\u044a\u044c\u0005\u000b\u0000"
            + "\u0000\u044b\u044d\u0003\u0110\u0088\u0000\u044c\u044b\u0001\u0000\u0000"
            + "\u0000\u044c\u044d\u0001\u0000\u0000\u0000\u044d\u044e\u0001\u0000\u0000"
            + "\u0000\u044e\u044f\u0005\u0007\u0000\u0000\u044f\u0450\u0003v;\u0000\u0450"
            + "\u0484\u0001\u0000\u0000\u0000\u0451\u0452\u0005S\u0000\u0000\u0452\u0453"
            + "\u0005\u0006\u0000\u0000\u0453\u0454\u0003\u0112\u0089\u0000\u0454\u0455"
            + "\u0005^\u0000\u0000\u0455\u0456\u0003\u0110\u0088\u0000\u0456\u0457\u0005"
            + "\u0007\u0000\u0000\u0457\u0458\u0003v;\u0000\u0458\u0484\u0001\u0000\u0000"
            + "\u0000\u0459\u045a\u0005S\u0000\u0000\u045a\u045b\u0005\u0006\u0000\u0000"
            + "\u045b\u045c\u0003\u00aaU\u0000\u045c\u045d\u0003\u00a0P\u0000\u045d\u045e"
            + "\u0005^\u0000\u0000\u045e\u045f\u0003\u0110\u0088\u0000\u045f\u0460\u0005"
            + "\u0007\u0000\u0000\u0460\u0461\u0003v;\u0000\u0461\u0484\u0001\u0000\u0000"
            + "\u0000\u0462\u0464\u0005S\u0000\u0000\u0463\u0465\u0005d\u0000\u0000\u0464"
            + "\u0463\u0001\u0000\u0000\u0000\u0464\u0465\u0001\u0000\u0000\u0000\u0465"
            + "\u0466\u0001\u0000\u0000\u0000\u0466\u0467\u0005\u0006\u0000\u0000\u0467"
            + "\u0468\u0003\u0112\u0089\u0000\u0468\u0469\u0003\u0132\u0099\u0000\u0469"
            + "\u046a\u0004T\u0007\u0000\u046a\u046d\u0003\u0110\u0088\u0000\u046b\u046c"
            + "\u0005`\u0000\u0000\u046c\u046e\u0003\u0012\t\u0000\u046d\u046b\u0001"
            + "\u0000\u0000\u0000\u046d\u046e\u0001\u0000\u0000\u0000\u046e\u046f\u0001"
            + "\u0000\u0000\u0000\u046f\u0470\u0005\u0007\u0000\u0000\u0470\u0471\u0003"
            + "v;\u0000\u0471\u0484\u0001\u0000\u0000\u0000\u0472\u0474\u0005S\u0000"
            + "\u0000\u0473\u0475\u0005d\u0000\u0000\u0474\u0473\u0001\u0000\u0000\u0000"
            + "\u0474\u0475\u0001\u0000\u0000\u0000\u0475\u0476\u0001\u0000\u0000\u0000"
            + "\u0476\u0477\u0005\u0006\u0000\u0000\u0477\u0478\u0003\u00aaU\u0000\u0478"
            + "\u0479\u0003\u00a0P\u0000\u0479\u047a\u0003\u0132\u0099\u0000\u047a\u047b"
            + "\u0004T\b\u0000\u047b\u047e\u0003\u0110\u0088\u0000\u047c\u047d\u0005"
            + "`\u0000\u0000\u047d\u047f\u0003\u0012\t\u0000\u047e\u047c\u0001\u0000"
            + "\u0000\u0000\u047e\u047f\u0001\u0000\u0000\u0000\u047f\u0480\u0001\u0000"
            + "\u0000\u0000\u0480\u0481\u0005\u0007\u0000\u0000\u0481\u0482\u0003v;\u0000"
            + "\u0482\u0484\u0001\u0000\u0000\u0000\u0483\u0425\u0001\u0000\u0000\u0000"
            + "\u0483\u042d\u0001\u0000\u0000\u0000\u0483\u0433\u0001\u0000\u0000\u0000"
            + "\u0483\u0442\u0001\u0000\u0000\u0000\u0483\u0451\u0001\u0000\u0000\u0000"
            + "\u0483\u0459\u0001\u0000\u0000\u0000\u0483\u0462\u0001\u0000\u0000\u0000"
            + "\u0483\u0472\u0001\u0000\u0000\u0000\u0484\u00a9\u0001\u0000\u0000\u0000"
            + "\u0485\u0486\u0007\u0005\u0000\u0000\u0486\u00ab\u0001\u0000\u0000\u0000"
            + "\u0487\u048a\u0005R\u0000\u0000\u0488\u0489\u0004V\t\u0000\u0489\u048b"
            + "\u0003\u0132\u0099\u0000\u048a\u0488\u0001\u0000\u0000\u0000\u048a\u048b"
            + "\u0001\u0000\u0000\u0000\u048b\u048c\u0001\u0000\u0000\u0000\u048c\u048d"
            + "\u0003\u013a\u009d\u0000\u048d\u00ad\u0001\u0000\u0000\u0000\u048e\u0491"
            + "\u0005F\u0000\u0000\u048f\u0490\u0004W\n\u0000\u0490\u0492\u0003\u0132"
            + "\u0099\u0000\u0491\u048f\u0001\u0000\u0000\u0000\u0491\u0492\u0001\u0000"
            + "\u0000\u0000\u0492\u0493\u0001\u0000\u0000\u0000\u0493\u0494\u0003\u013a"
            + "\u009d\u0000\u0494\u00af\u0001\u0000\u0000\u0000\u0495\u0498\u0005P\u0000"
            + "\u0000\u0496\u0497\u0004X\u000b\u0000\u0497\u0499\u0003\u0110\u0088\u0000"
            + "\u0498\u0496\u0001\u0000\u0000\u0000\u0498\u0499\u0001\u0000\u0000\u0000"
            + "\u0499\u049a\u0001\u0000\u0000\u0000\u049a\u049b\u0003\u013a\u009d\u0000"
            + "\u049b\u00b1\u0001\u0000\u0000\u0000\u049c\u049f\u0007\u0006\u0000\u0000"
            + "\u049d\u049e\u0004Y\f\u0000\u049e\u04a0\u0003\u0110\u0088\u0000\u049f"
            + "\u049d\u0001\u0000\u0000\u0000\u049f\u04a0\u0001\u0000\u0000\u0000\u04a0"
            + "\u04a1\u0001\u0000\u0000\u0000\u04a1\u04a2\u0003\u013a\u009d\u0000\u04a2"
            + "\u00b3\u0001\u0000\u0000\u0000\u04a3\u04a4\u0005Y\u0000\u0000\u04a4\u04a5"
            + "\u0005\u0006\u0000\u0000\u04a5\u04a6\u0003\u0110\u0088\u0000\u04a6\u04a7"
            + "\u0005\u0007\u0000\u0000\u04a7\u04a8\u0003v;\u0000\u04a8\u00b5\u0001\u0000"
            + "\u0000\u0000\u04a9\u04aa\u0005T\u0000\u0000\u04aa\u04ab\u0005\u0006\u0000"
            + "\u0000\u04ab\u04ac\u0003\u0110\u0088\u0000\u04ac\u04ad\u0005\u0007\u0000"
            + "\u0000\u04ad\u04ae\u0003\u00b8\\\u0000\u04ae\u00b7\u0001\u0000\u0000\u0000"
            + "\u04af\u04b1\u0005\b\u0000\u0000\u04b0\u04b2\u0003\u00ba]\u0000\u04b1"
            + "\u04b0\u0001\u0000\u0000\u0000\u04b1\u04b2\u0001\u0000\u0000\u0000\u04b2"
            + "\u04b7\u0001\u0000\u0000\u0000\u04b3\u04b5\u0003\u00be_\u0000\u04b4\u04b6"
            + "\u0003\u00ba]\u0000\u04b5\u04b4\u0001\u0000\u0000\u0000\u04b5\u04b6\u0001"
            + "\u0000\u0000\u0000\u04b6\u04b8\u0001\u0000\u0000\u0000\u04b7\u04b3\u0001"
            + "\u0000\u0000\u0000\u04b7\u04b8\u0001\u0000\u0000\u0000\u04b8\u04b9\u0001"
            + "\u0000\u0000\u0000\u04b9\u04ba\u0005\n\u0000\u0000\u04ba\u00b9\u0001\u0000"
            + "\u0000\u0000\u04bb\u04bd\u0003\u00bc^\u0000\u04bc\u04bb\u0001\u0000\u0000"
            + "\u0000\u04bd\u04be\u0001\u0000\u0000\u0000\u04be\u04bc\u0001\u0000\u0000"
            + "\u0000\u04be\u04bf\u0001\u0000\u0000\u0000\u04bf\u00bb\u0001\u0000\u0000"
            + "\u0000\u04c0\u04c1\u0005J\u0000\u0000\u04c1\u04c2\u0003\u0110\u0088\u0000"
            + "\u04c2\u04c4\u0005\u0010\u0000\u0000\u04c3\u04c5\u0003z=\u0000\u04c4\u04c3"
            + "\u0001\u0000\u0000\u0000\u04c4\u04c5\u0001\u0000\u0000\u0000\u04c5\u00bd"
            + "\u0001\u0000\u0000\u0000\u04c6\u04c7\u0005Z\u0000\u0000\u04c7\u04c9\u0005"
            + "\u0010\u0000\u0000\u04c8\u04ca\u0003z=\u0000\u04c9\u04c8\u0001\u0000\u0000"
            + "\u0000\u04c9\u04ca\u0001\u0000\u0000\u0000\u04ca\u00bf\u0001\u0000\u0000"
            + "\u0000\u04cb\u04cc\u0003\u0132\u0099\u0000\u04cc\u04cd\u0005\u0010\u0000"
            + "\u0000\u04cd\u04ce\u0003v;\u0000\u04ce\u00c1\u0001\u0000\u0000\u0000\u04cf"
            + "\u04d0\u0005\\\u0000\u0000\u04d0\u04d1\u0004a\r\u0000\u04d1\u04d2\u0003"
            + "\u0110\u0088\u0000\u04d2\u04d3\u0003\u013a\u009d\u0000\u04d3\u00c3\u0001"
            + "\u0000\u0000\u0000\u04d4\u04d5\u0005_\u0000\u0000\u04d5\u04db\u0003x<"
            + "\u0000\u04d6\u04d8\u0003\u00c6c\u0000\u04d7\u04d9\u0003\u00c8d\u0000\u04d8"
            + "\u04d7\u0001\u0000\u0000\u0000\u04d8\u04d9\u0001\u0000\u0000\u0000\u04d9"
            + "\u04dc\u0001\u0000\u0000\u0000\u04da\u04dc\u0003\u00c8d\u0000\u04db\u04d6"
            + "\u0001\u0000\u0000\u0000\u04db\u04da\u0001\u0000\u0000\u0000\u04dc\u00c5"
            + "\u0001\u0000\u0000\u0000\u04dd\u04e5\u0005N\u0000\u0000\u04de\u04df\u0005"
            + "\u0006\u0000\u0000\u04df\u04e1\u0003\u0132\u0099\u0000\u04e0\u04e2\u0003"
            + "8\u001c\u0000\u04e1\u04e0\u0001\u0000\u0000\u0000\u04e1\u04e2\u0001\u0000"
            + "\u0000\u0000\u04e2\u04e3\u0001\u0000\u0000\u0000\u04e3\u04e4\u0005\u0007"
            + "\u0000\u0000\u04e4\u04e6\u0001\u0000\u0000\u0000\u04e5\u04de\u0001\u0000"
            + "\u0000\u0000\u04e5\u04e6\u0001\u0000\u0000\u0000\u04e6\u04e7\u0001\u0000"
            + "\u0000\u0000\u04e7\u04e8\u0003x<\u0000\u04e8\u00c7\u0001\u0000\u0000\u0000"
            + "\u04e9\u04ea\u0005O\u0000\u0000\u04ea\u04eb\u0003x<\u0000\u04eb\u00c9"
            + "\u0001\u0000\u0000\u0000\u04ec\u04ed\u0005V\u0000\u0000\u04ed\u04ee\u0003"
            + "\u013a\u009d\u0000\u04ee\u00cb\u0001\u0000\u0000\u0000\u04ef\u04f1\u0005"
            + "c\u0000\u0000\u04f0\u04ef\u0001\u0000\u0000\u0000\u04f0\u04f1\u0001\u0000"
            + "\u0000\u0000\u04f1\u04f2\u0001\u0000\u0000\u0000\u04f2\u04f4\u0005W\u0000"
            + "\u0000\u04f3\u04f5\u0005\u0019\u0000\u0000\u04f4\u04f3\u0001\u0000\u0000"
            + "\u0000\u04f4\u04f5\u0001\u0000\u0000\u0000\u04f5\u04f6\u0001\u0000\u0000"
            + "\u0000\u04f6\u04f7\u0003\u0132\u0099\u0000\u04f7\u04fd\u0003:\u001d\u0000"
            + "\u04f8\u04f9\u0005\b\u0000\u0000\u04f9\u04fa\u0003\u00f6{\u0000\u04fa"
            + "\u04fb\u0005\n\u0000\u0000\u04fb\u04fe\u0001\u0000\u0000\u0000\u04fc\u04fe"
            + "\u0005\u000b\u0000\u0000\u04fd\u04f8\u0001\u0000\u0000\u0000\u04fd\u04fc"
            + "\u0001\u0000\u0000\u0000\u04fe\u00cd\u0001\u0000\u0000\u0000\u04ff\u0501"
            + "\u0003j5\u0000\u0500\u04ff\u0001\u0000\u0000\u0000\u0500\u0501\u0001\u0000"
            + "\u0000\u0000\u0501\u0506\u0001\u0000\u0000\u0000\u0502\u0504\u0005l\u0000"
            + "\u0000\u0503\u0505\u0005Z\u0000\u0000\u0504\u0503\u0001\u0000\u0000\u0000"
            + "\u0504\u0505\u0001\u0000\u0000\u0000\u0505\u0507\u0001\u0000\u0000\u0000"
            + "\u0506\u0502\u0001\u0000\u0000\u0000\u0506\u0507\u0001\u0000\u0000\u0000"
            + "\u0507\u0509\u0001\u0000\u0000\u0000\u0508\u050a\u0005\u0087\u0000\u0000"
            + "\u0509\u0508\u0001\u0000\u0000\u0000\u0509\u050a\u0001\u0000\u0000\u0000"
            + "\u050a\u050b\u0001\u0000\u0000\u0000\u050b\u050c\u0005g\u0000\u0000\u050c"
            + "\u050e\u0003\u0132\u0099\u0000\u050d\u050f\u0003\u0004\u0002\u0000\u050e"
            + "\u050d\u0001\u0000\u0000\u0000\u050e\u050f\u0001\u0000\u0000\u0000\u050f"
            + "\u0510\u0001\u0000\u0000\u0000\u0510\u0511\u0003\u00d0h\u0000\u0511\u0512"
            + "\u0003\u00d2i\u0000\u0512\u00cf\u0001\u0000\u0000\u0000\u0513\u0515\u0003"
            + "\u00d4j\u0000\u0514\u0513\u0001\u0000\u0000\u0000\u0514\u0515\u0001\u0000"
            + "\u0000\u0000\u0515\u0517\u0001\u0000\u0000\u0000\u0516\u0518\u0003\u00d6"
            + "k\u0000\u0517\u0516\u0001\u0000\u0000\u0000\u0517\u0518\u0001\u0000\u0000"
            + "\u0000\u0518\u00d1\u0001\u0000\u0000\u0000\u0519\u051d\u0005\b\u0000\u0000"
            + "\u051a\u051c\u0003\u00d8l\u0000\u051b\u051a\u0001\u0000\u0000\u0000\u051c"
            + "\u051f\u0001\u0000\u0000\u0000\u051d\u051b\u0001\u0000\u0000\u0000\u051d"
            + "\u051e\u0001\u0000\u0000\u0000\u051e\u0520\u0001\u0000\u0000\u0000\u051f"
            + "\u051d\u0001\u0000\u0000\u0000\u0520\u0521\u0005\n\u0000\u0000\u0521\u00d3"
            + "\u0001\u0000\u0000\u0000\u0522\u0523\u0005i\u0000\u0000\u0523\u0524\u0003"
            + "\u001a\r\u0000\u0524\u00d5\u0001\u0000\u0000\u0000\u0525\u0526\u0005n"
            + "\u0000\u0000\u0526\u0527\u0003Z-\u0000\u0527\u00d7\u0001\u0000\u0000\u0000"
            + "\u0528\u0530\u0003T*\u0000\u0529\u052b\u0003j5\u0000\u052a\u0529\u0001"
            + "\u0000\u0000\u0000\u052a\u052b\u0001\u0000\u0000\u0000\u052b\u052c\u0001"
            + "\u0000\u0000\u0000\u052c\u0530\u0003\u00dam\u0000\u052d\u0530\u0003\u00de"
            + "o\u0000\u052e\u0530\u0003v;\u0000\u052f\u0528\u0001\u0000\u0000\u0000"
            + "\u052f\u052a\u0001\u0000\u0000\u0000\u052f\u052d\u0001\u0000\u0000\u0000"
            + "\u052f\u052e\u0001\u0000\u0000\u0000\u0530\u00d9\u0001\u0000\u0000\u0000"
            + "\u0531\u0532\u0003\u00dcn\u0000\u0532\u0534\u0003\u0108\u0084\u0000\u0533"
            + "\u0535\u0005\u000e\u0000\u0000\u0534\u0533\u0001\u0000\u0000\u0000\u0534"
            + "\u0535\u0001\u0000\u0000\u0000\u0535\u0537\u0001\u0000\u0000\u0000\u0536"
            + "\u0538\u00038\u001c\u0000\u0537\u0536\u0001\u0000\u0000\u0000\u0537\u0538"
            + "\u0001\u0000\u0000\u0000\u0538\u053a\u0001\u0000\u0000\u0000\u0539\u053b"
            + "\u0003\u0000\u0000\u0000\u053a\u0539\u0001\u0000\u0000\u0000\u053a\u053b"
            + "\u0001\u0000\u0000\u0000\u053b\u053c\u0001\u0000\u0000\u0000\u053c\u053d"
            + "\u0005\u000b\u0000\u0000\u053d\u054f\u0001\u0000\u0000\u0000\u053e\u053f"
            + "\u0003\u00dcn\u0000\u053f\u0540\u0003\u0108\u0084\u0000\u0540\u0546\u0003"
            + ":\u001d\u0000\u0541\u0542\u0005\b\u0000\u0000\u0542\u0543\u0003\u00f6"
            + "{\u0000\u0543\u0544\u0005\n\u0000\u0000\u0544\u0547\u0001\u0000\u0000"
            + "\u0000\u0545\u0547\u0005\u000b\u0000\u0000\u0546\u0541\u0001\u0000\u0000"
            + "\u0000\u0546\u0545\u0001\u0000\u0000\u0000\u0547\u054f\u0001\u0000\u0000"
            + "\u0000\u0548\u054b\u0003\u00dcn\u0000\u0549\u054c\u0003\u0104\u0082\u0000"
            + "\u054a\u054c\u0003\u0106\u0083\u0000\u054b\u0549\u0001\u0000\u0000\u0000"
            + "\u054b\u054a\u0001\u0000\u0000\u0000\u054c\u054f\u0001\u0000\u0000\u0000"
            + "\u054d\u054f\u0003|>\u0000\u054e\u0531\u0001\u0000\u0000\u0000\u054e\u053e"
            + "\u0001\u0000\u0000\u0000\u054e\u0548\u0001\u0000\u0000\u0000\u054e\u054d"
            + "\u0001\u0000\u0000\u0000\u054f\u00db\u0001\u0000\u0000\u0000\u0550\u0552"
            + "\u0003H$\u0000\u0551\u0550\u0001\u0000\u0000\u0000\u0551\u0552\u0001\u0000"
            + "\u0000\u0000\u0552\u0554\u0001\u0000\u0000\u0000\u0553\u0555\u0005c\u0000"
            + "\u0000\u0554\u0553\u0001\u0000\u0000\u0000\u0554\u0555\u0001\u0000\u0000"
            + "\u0000\u0555\u0557\u0001\u0000\u0000\u0000\u0556\u0558\u0005u\u0000\u0000"
            + "\u0557\u0556\u0001\u0000\u0000\u0000\u0557\u0558\u0001\u0000\u0000\u0000"
            + "\u0558\u055a\u0001\u0000\u0000\u0000\u0559\u055b\u0005b\u0000\u0000\u055a"
            + "\u0559\u0001\u0000\u0000\u0000\u055a\u055b\u0001\u0000\u0000\u0000\u055b"
            + "\u00dd\u0001\u0000\u0000\u0000\u055c\u055d\u0003N\'\u0000\u055d\u055e"
            + "\u0005\u000b\u0000\u0000\u055e\u00df\u0001\u0000\u0000\u0000\u055f\u0560"
            + "\u0005c\u0000\u0000\u0560\u0562\u0004p\u000e\u0000\u0561\u055f\u0001\u0000"
            + "\u0000\u0000\u0561\u0562\u0001\u0000\u0000\u0000\u0562\u0564\u0001\u0000"
            + "\u0000\u0000\u0563\u0565\u0005\u0019\u0000\u0000\u0564\u0563\u0001\u0000"
            + "\u0000\u0000\u0564\u0565\u0001\u0000\u0000\u0000\u0565\u0566\u0001\u0000"
            + "\u0000\u0000\u0566\u0567\u0003\u0108\u0084\u0000\u0567\u0569\u0005\u0006"
            + "\u0000\u0000\u0568\u056a\u0003\u00f0x\u0000\u0569\u0568\u0001\u0000\u0000"
            + "\u0000\u0569\u056a\u0001\u0000\u0000\u0000\u056a\u056b\u0001\u0000\u0000"
            + "\u0000\u056b\u056c\u0005\u0007\u0000\u0000\u056c\u056d\u0005\b\u0000\u0000"
            + "\u056d\u056e\u0003\u00f6{\u0000\u056e\u056f\u0005\n\u0000\u0000\u056f"
            + "\u00e1\u0001\u0000\u0000\u0000\u0570\u0572\u0005c\u0000\u0000\u0571\u0570"
            + "\u0001\u0000\u0000\u0000\u0571\u0572\u0001\u0000\u0000\u0000\u0572\u0573"
            + "\u0001\u0000\u0000\u0000\u0573\u0574\u0005W\u0000\u0000\u0574\u0576\u0005"
            + "\u0019\u0000\u0000\u0575\u0577\u0003\u0132\u0099\u0000\u0576\u0575\u0001"
            + "\u0000\u0000\u0000\u0576\u0577\u0001\u0000\u0000\u0000\u0577\u0578\u0001"
            + "\u0000\u0000\u0000\u0578\u057a\u0005\u0006\u0000\u0000\u0579\u057b\u0003"
            + "\u00f0x\u0000\u057a\u0579\u0001\u0000\u0000\u0000\u057a\u057b\u0001\u0000"
            + "\u0000\u0000\u057b\u057c\u0001\u0000\u0000\u0000\u057c\u057d\u0005\u0007"
            + "\u0000\u0000\u057d\u057e\u0005\b\u0000\u0000\u057e\u057f\u0003\u00f6{"
            + "\u0000\u057f\u0580\u0005\n\u0000\u0000\u0580\u00e3\u0001\u0000\u0000\u0000"
            + "\u0581\u0582\u0005\b\u0000\u0000\u0582\u0587\u0003\u00e6s\u0000\u0583"
            + "\u0584\u0005\f\u0000\u0000\u0584\u0586\u0003\u00e6s\u0000\u0585\u0583"
            + "\u0001\u0000\u0000\u0000\u0586\u0589\u0001\u0000\u0000\u0000\u0587\u0585"
            + "\u0001\u0000\u0000\u0000\u0587\u0588\u0001\u0000\u0000\u0000\u0588\u058b"
            + "\u0001\u0000\u0000\u0000\u0589\u0587\u0001\u0000\u0000\u0000\u058a\u058c"
            + "\u0005\f\u0000\u0000\u058b\u058a\u0001\u0000\u0000\u0000\u058b\u058c\u0001"
            + "\u0000\u0000\u0000\u058c\u058d\u0001\u0000\u0000\u0000\u058d\u058e\u0005"
            + "\n\u0000\u0000\u058e\u00e5\u0001\u0000\u0000\u0000\u058f\u0590\u0005\u0019"
            + "\u0000\u0000\u0590\u0591\u0003\u00eau\u0000\u0591\u00e7\u0001\u0000\u0000"
            + "\u0000\u0592\u0593\u0005\b\u0000\u0000\u0593\u0598\u0003\u00eau\u0000"
            + "\u0594\u0595\u0005\f\u0000\u0000\u0595\u0597\u0003\u00eau\u0000\u0596"
            + "\u0594\u0001\u0000\u0000\u0000\u0597\u059a\u0001\u0000\u0000\u0000\u0598"
            + "\u0596\u0001\u0000\u0000\u0000\u0598\u0599\u0001\u0000\u0000\u0000\u0599"
            + "\u059c\u0001\u0000\u0000\u0000\u059a\u0598\u0001\u0000\u0000\u0000\u059b"
            + "\u059d\u0005\f\u0000\u0000\u059c\u059b\u0001\u0000\u0000\u0000\u059c\u059d"
            + "\u0001\u0000\u0000\u0000\u059d\u059e\u0001\u0000\u0000\u0000\u059e\u059f"
            + "\u0005\n\u0000\u0000\u059f\u00e9\u0001\u0000\u0000\u0000\u05a0\u05a1\u0005"
            + "\u0004\u0000\u0000\u05a1\u05a2\u0003\u0112\u0089\u0000\u05a2\u05a3\u0005"
            + "\u0005\u0000\u0000\u05a3\u05a5\u0005\u0006\u0000\u0000\u05a4\u05a6\u0003"
            + "\u00f0x\u0000\u05a5\u05a4\u0001\u0000\u0000\u0000\u05a5\u05a6\u0001\u0000"
            + "\u0000\u0000\u05a6\u05a7\u0001\u0000\u0000\u0000\u05a7\u05a8\u0005\u0007"
            + "\u0000\u0000\u05a8\u05a9\u0005\b\u0000\u0000\u05a9\u05aa\u0003\u00f6{"
            + "\u0000\u05aa\u05ab\u0005\n\u0000\u0000\u05ab\u00eb\u0001\u0000\u0000\u0000"
            + "\u05ac\u05af\u0003\u0108\u0084\u0000\u05ad\u05af\u0003\u00eew\u0000\u05ae"
            + "\u05ac\u0001\u0000\u0000\u0000\u05ae\u05ad\u0001\u0000\u0000\u0000\u05af"
            + "\u00ed\u0001\u0000\u0000\u0000\u05b0\u05b1\u0005\u001e\u0000\u0000\u05b1"
            + "\u05b2\u0003\u0130\u0098\u0000\u05b2\u00ef\u0001\u0000\u0000\u0000\u05b3"
            + "\u05b8\u0003\u00f2y\u0000\u05b4\u05b5\u0005\f\u0000\u0000\u05b5\u05b7"
            + "\u0003\u00f2y\u0000\u05b6\u05b4\u0001\u0000\u0000\u0000\u05b7\u05ba\u0001"
            + "\u0000\u0000\u0000\u05b8\u05b6\u0001\u0000\u0000\u0000\u05b8\u05b9\u0001"
            + "\u0000\u0000\u0000\u05b9\u05bd\u0001\u0000\u0000\u0000\u05ba\u05b8\u0001"
            + "\u0000\u0000\u0000\u05bb\u05bc\u0005\f\u0000\u0000\u05bc\u05be\u0003\u00f4"
            + "z\u0000\u05bd\u05bb\u0001\u0000\u0000\u0000\u05bd\u05be\u0001\u0000\u0000"
            + "\u0000\u05be\u05c0\u0001\u0000\u0000\u0000\u05bf\u05c1\u0005\f\u0000\u0000"
            + "\u05c0\u05bf\u0001\u0000\u0000\u0000\u05c0\u05c1\u0001\u0000\u0000\u0000"
            + "\u05c1\u05ca\u0001\u0000\u0000\u0000\u05c2\u05ca\u0003\u00f4z\u0000\u05c3"
            + "\u05ca\u0003\u00fa}\u0000\u05c4\u05c7\u0003\u0100\u0080\u0000\u05c5\u05c6"
            + "\u0005\u0010\u0000\u0000\u05c6\u05c8\u0003\u00f0x\u0000\u05c7\u05c5\u0001"
            + "\u0000\u0000\u0000\u05c7\u05c8\u0001\u0000\u0000\u0000\u05c8\u05ca\u0001"
            + "\u0000\u0000\u0000\u05c9\u05b3\u0001\u0000\u0000\u0000\u05c9\u05c2\u0001"
            + "\u0000\u0000\u0000\u05c9\u05c3\u0001\u0000\u0000\u0000\u05c9\u05c4\u0001"
            + "\u0000\u0000\u0000\u05ca\u00f1\u0001\u0000\u0000\u0000\u05cb\u05cd\u0003"
            + "l6\u0000\u05cc\u05cb\u0001\u0000\u0000\u0000\u05cc\u05cd\u0001\u0000\u0000"
            + "\u0000\u05cd\u05cf\u0001\u0000\u0000\u0000\u05ce\u05d0\u0003H$\u0000\u05cf"
            + "\u05ce\u0001\u0000\u0000\u0000\u05cf\u05d0\u0001\u0000\u0000\u0000\u05d0"
            + "\u05d1\u0001\u0000\u0000\u0000\u05d1\u05d3\u0003\u0116\u008b\u0000\u05d2"
            + "\u05d4\u0005\u000e\u0000\u0000\u05d3\u05d2\u0001\u0000\u0000\u0000\u05d3"
            + "\u05d4\u0001\u0000\u0000\u0000\u05d4\u05d6\u0001\u0000\u0000\u0000\u05d5"
            + "\u05d7\u00038\u001c\u0000\u05d6\u05d5\u0001\u0000\u0000\u0000\u05d6\u05d7"
            + "\u0001\u0000\u0000\u0000\u05d7\u05da\u0001\u0000\u0000\u0000\u05d8\u05d9"
            + "\u0005\r\u0000\u0000\u05d9\u05db\u0003\u0112\u0089\u0000\u05da\u05d8\u0001"
            + "\u0000\u0000\u0000\u05da\u05db\u0001\u0000\u0000\u0000\u05db\u00f3\u0001"
            + "\u0000\u0000\u0000\u05dc\u05dd\u0005\u0011\u0000\u0000\u05dd\u05df\u0003"
            + "\u0132\u0099\u0000\u05de\u05e0\u00038\u001c\u0000\u05df\u05de\u0001\u0000"
            + "\u0000\u0000\u05df\u05e0\u0001\u0000\u0000\u0000\u05e0\u00f5\u0001\u0000"
            + "\u0000\u0000\u05e1\u05e3\u0003\u00f8|\u0000\u05e2\u05e1\u0001\u0000\u0000"
            + "\u0000\u05e2\u05e3\u0001\u0000\u0000\u0000\u05e3\u00f7\u0001\u0000\u0000"
            + "\u0000\u05e4\u05e6\u0003t:\u0000\u05e5\u05e4\u0001\u0000\u0000\u0000\u05e6"
            + "\u05e7\u0001\u0000\u0000\u0000\u05e7\u05e5\u0001\u0000\u0000\u0000\u05e7"
            + "\u05e8\u0001\u0000\u0000\u0000\u05e8\u00f9\u0001\u0000\u0000\u0000\u05e9"
            + "\u05ea\u0005\u0004\u0000\u0000\u05ea\u05eb\u0003\u00fc~\u0000\u05eb\u05ec"
            + "\u0005\u0005\u0000\u0000\u05ec\u00fb\u0001\u0000\u0000\u0000\u05ed\u05ef"
            + "\u0005\f\u0000\u0000\u05ee\u05ed\u0001\u0000\u0000\u0000\u05ef\u05f2\u0001"
            + "\u0000\u0000\u0000\u05f0\u05ee\u0001\u0000\u0000\u0000\u05f0\u05f1\u0001"
            + "\u0000\u0000\u0000\u05f1\u05f4\u0001\u0000\u0000\u0000\u05f2\u05f0\u0001"
            + "\u0000\u0000\u0000\u05f3\u05f5\u0003\u00fe\u007f\u0000\u05f4\u05f3\u0001"
            + "\u0000\u0000\u0000\u05f4\u05f5\u0001\u0000\u0000\u0000\u05f5\u05fe\u0001"
            + "\u0000\u0000\u0000\u05f6\u05f8\u0005\f\u0000\u0000\u05f7\u05f6\u0001\u0000"
            + "\u0000\u0000\u05f8\u05f9\u0001\u0000\u0000\u0000\u05f9\u05f7\u0001\u0000"
            + "\u0000\u0000\u05f9\u05fa\u0001\u0000\u0000\u0000\u05fa\u05fb\u0001\u0000"
            + "\u0000\u0000\u05fb\u05fd\u0003\u00fe\u007f\u0000\u05fc\u05f7\u0001\u0000"
            + "\u0000\u0000\u05fd\u0600\u0001\u0000\u0000\u0000\u05fe\u05fc\u0001\u0000"
            + "\u0000\u0000\u05fe\u05ff\u0001\u0000\u0000\u0000\u05ff\u0604\u0001\u0000"
            + "\u0000\u0000\u0600\u05fe\u0001\u0000\u0000\u0000\u0601\u0603\u0005\f\u0000"
            + "\u0000\u0602\u0601\u0001\u0000\u0000\u0000\u0603\u0606\u0001\u0000\u0000"
            + "\u0000\u0604\u0602\u0001\u0000\u0000\u0000\u0604\u0605\u0001\u0000\u0000"
            + "\u0000\u0605\u00fd\u0001\u0000\u0000\u0000\u0606\u0604\u0001\u0000\u0000"
            + "\u0000\u0607\u0609\u0005\u0011\u0000\u0000\u0608\u0607\u0001\u0000\u0000"
            + "\u0000\u0608\u0609\u0001\u0000\u0000\u0000\u0609\u060c\u0001\u0000\u0000"
            + "\u0000\u060a\u060d\u0003\u0112\u0089\u0000\u060b\u060d\u0003\u0132\u0099"
            + "\u0000\u060c\u060a\u0001\u0000\u0000\u0000\u060c\u060b\u0001\u0000\u0000"
            + "\u0000\u060d\u060f\u0001\u0000\u0000\u0000\u060e\u0610\u0005\f\u0000\u0000"
            + "\u060f\u060e\u0001\u0000\u0000\u0000\u060f\u0610\u0001\u0000\u0000\u0000"
            + "\u0610\u00ff\u0001\u0000\u0000\u0000\u0611\u061d\u0005\b\u0000\u0000\u0612"
            + "\u0617\u0003\u0102\u0081\u0000\u0613\u0614\u0005\f\u0000\u0000\u0614\u0616"
            + "\u0003\u0102\u0081\u0000\u0615\u0613\u0001\u0000\u0000\u0000\u0616\u0619"
            + "\u0001\u0000\u0000\u0000\u0617\u0615\u0001\u0000\u0000\u0000\u0617\u0618"
            + "\u0001\u0000\u0000\u0000\u0618\u061b\u0001\u0000\u0000\u0000\u0619\u0617"
            + "\u0001\u0000\u0000\u0000\u061a\u061c\u0005\f\u0000\u0000\u061b\u061a\u0001"
            + "\u0000\u0000\u0000\u061b\u061c\u0001\u0000\u0000\u0000\u061c\u061e\u0001"
            + "\u0000\u0000\u0000\u061d\u0612\u0001\u0000\u0000\u0000\u061d\u061e\u0001"
            + "\u0000\u0000\u0000\u061e\u061f\u0001\u0000\u0000\u0000\u061f\u0620\u0005"
            + "\n\u0000\u0000\u0620\u0101\u0001\u0000\u0000\u0000\u0621\u0622\u0003\u0108"
            + "\u0084\u0000\u0622\u0623\u0007\u0007\u0000\u0000\u0623\u0624\u0003\u0112"
            + "\u0089\u0000\u0624\u0635\u0001\u0000\u0000\u0000\u0625\u0626\u0005\u0004"
            + "\u0000\u0000\u0626\u0627\u0003\u0112\u0089\u0000\u0627\u0628\u0005\u0005"
            + "\u0000\u0000\u0628\u0629\u0005\u0010\u0000\u0000\u0629\u062a\u0003\u0112"
            + "\u0089\u0000\u062a\u0635\u0001\u0000\u0000\u0000\u062b\u0635\u0003\u0104"
            + "\u0082\u0000\u062c\u0635\u0003\u0106\u0083\u0000\u062d\u0635\u0003\u00e0"
            + "p\u0000\u062e\u0635\u0003\u0134\u009a\u0000\u062f\u0631\u0005\u0011\u0000"
            + "\u0000\u0630\u062f\u0001\u0000\u0000\u0000\u0630\u0631\u0001\u0000\u0000"
            + "\u0000\u0631\u0632\u0001\u0000\u0000\u0000\u0632\u0635\u0003\u0112\u0089"
            + "\u0000\u0633\u0635\u0003D\"\u0000\u0634\u0621\u0001\u0000\u0000\u0000"
            + "\u0634\u0625\u0001\u0000\u0000\u0000\u0634\u062b\u0001\u0000\u0000\u0000"
            + "\u0634\u062c\u0001\u0000\u0000\u0000\u0634\u062d\u0001\u0000\u0000\u0000"
            + "\u0634\u062e\u0001\u0000\u0000\u0000\u0634\u0630\u0001\u0000\u0000\u0000"
            + "\u0634\u0633\u0001\u0000\u0000\u0000\u0635\u0103\u0001\u0000\u0000\u0000"
            + "\u0636\u0637\u0003\u012c\u0096\u0000\u0637\u0638\u0005\u0006\u0000\u0000"
            + "\u0638\u063a\u0005\u0007\u0000\u0000\u0639\u063b\u00038\u001c\u0000\u063a"
            + "\u0639\u0001\u0000\u0000\u0000\u063a\u063b\u0001\u0000\u0000\u0000\u063b"
            + "\u063c\u0001\u0000\u0000\u0000\u063c\u063d\u0005\b\u0000\u0000\u063d\u063e"
            + "\u0003\u00f6{\u0000\u063e\u063f\u0005\n\u0000\u0000\u063f\u0105\u0001"
            + "\u0000\u0000\u0000\u0640\u0641\u0003\u012e\u0097\u0000\u0641\u0643\u0005"
            + "\u0006\u0000\u0000\u0642\u0644\u0003\u00f0x\u0000\u0643\u0642\u0001\u0000"
            + "\u0000\u0000\u0643\u0644\u0001\u0000\u0000\u0000\u0644\u0645\u0001\u0000"
            + "\u0000\u0000\u0645\u0646\u0005\u0007\u0000\u0000\u0646\u0647\u0005\b\u0000"
            + "\u0000\u0647\u0648\u0003\u00f6{\u0000\u0648\u0649\u0005\n\u0000\u0000"
            + "\u0649\u0107\u0001\u0000\u0000\u0000\u064a\u0652\u0003\u0130\u0098\u0000"
            + "\u064b\u0652\u0005\u008b\u0000\u0000\u064c\u0652\u0003\u0128\u0094\u0000"
            + "\u064d\u064e\u0005\u0004\u0000\u0000\u064e\u064f\u0003\u0112\u0089\u0000"
            + "\u064f\u0650\u0005\u0005\u0000\u0000\u0650\u0652\u0001\u0000\u0000\u0000"
            + "\u0651\u064a\u0001\u0000\u0000\u0000\u0651\u064b\u0001\u0000\u0000\u0000"
            + "\u0651\u064c\u0001\u0000\u0000\u0000\u0651\u064d\u0001\u0000\u0000\u0000"
            + "\u0652\u0109\u0001\u0000\u0000\u0000\u0653\u0658\u0005\u0006\u0000\u0000"
            + "\u0654\u0656\u0003\u010c\u0086\u0000\u0655\u0657\u0005\f\u0000\u0000\u0656"
            + "\u0655\u0001\u0000\u0000\u0000\u0656\u0657\u0001\u0000\u0000\u0000\u0657"
            + "\u0659\u0001\u0000\u0000\u0000\u0658\u0654\u0001\u0000\u0000\u0000\u0658"
            + "\u0659\u0001\u0000\u0000\u0000\u0659\u065a\u0001\u0000\u0000\u0000\u065a"
            + "\u065b\u0005\u0007\u0000\u0000\u065b\u010b\u0001\u0000\u0000\u0000\u065c"
            + "\u0661\u0003\u010e\u0087\u0000\u065d\u065e\u0005\f\u0000\u0000\u065e\u0660"
            + "\u0003\u010e\u0087\u0000\u065f\u065d\u0001\u0000\u0000\u0000\u0660\u0663"
            + "\u0001\u0000\u0000\u0000\u0661\u065f\u0001\u0000\u0000\u0000\u0661\u0662"
            + "\u0001\u0000\u0000\u0000\u0662\u010d\u0001\u0000\u0000\u0000\u0663\u0661"
            + "\u0001\u0000\u0000\u0000\u0664\u0666\u0005\u0011\u0000\u0000\u0665\u0664"
            + "\u0001\u0000\u0000\u0000\u0665\u0666\u0001\u0000\u0000\u0000\u0666\u0669"
            + "\u0001\u0000\u0000\u0000\u0667\u066a\u0003\u0112\u0089\u0000\u0668\u066a"
            + "\u0003\u0132\u0099\u0000\u0669\u0667\u0001\u0000\u0000\u0000\u0669\u0668"
            + "\u0001\u0000\u0000\u0000\u066a\u010f\u0001\u0000\u0000\u0000\u066b\u0670"
            + "\u0003\u0112\u0089\u0000\u066c\u066d\u0005\f\u0000\u0000\u066d\u066f\u0003"
            + "\u0112\u0089\u0000\u066e\u066c\u0001\u0000\u0000\u0000\u066f\u0672\u0001"
            + "\u0000\u0000\u0000\u0670\u066e\u0001\u0000\u0000\u0000\u0670\u0671\u0001"
            + "\u0000\u0000\u0000\u0671\u0111\u0001\u0000\u0000\u0000\u0672\u0670\u0001"
            + "\u0000\u0000\u0000\u0673\u0674\u0006\u0089\uffff\uffff\u0000\u0674\u06b5"
            + "\u0003\u0118\u008c\u0000\u0675\u0677\u0005g\u0000\u0000\u0676\u0678\u0003"
            + "\u0132\u0099\u0000\u0677\u0676\u0001\u0000\u0000\u0000\u0677\u0678\u0001"
            + "\u0000\u0000\u0000\u0678\u067a\u0001\u0000\u0000\u0000\u0679\u067b\u0003"
            + "\u0004\u0002\u0000\u067a\u0679\u0001\u0000\u0000\u0000\u067a\u067b\u0001"
            + "\u0000\u0000\u0000\u067b\u067c\u0001\u0000\u0000\u0000\u067c\u067d\u0003"
            + "\u00d0h\u0000\u067d\u067e\u0003\u00d2i\u0000\u067e\u06b5\u0001\u0000\u0000"
            + "\u0000\u067f\u0680\u0005L\u0000\u0000\u0680\u0682\u0003\u0112\u0089\u0000"
            + "\u0681\u0683\u0003\f\u0006\u0000\u0682\u0681\u0001\u0000\u0000\u0000\u0682"
            + "\u0683\u0001\u0000\u0000\u0000\u0683\u0684\u0001\u0000\u0000\u0000\u0684"
            + "\u0685\u0003\u010a\u0085\u0000\u0685\u06b5\u0001\u0000\u0000\u0000\u0686"
            + "\u0687\u0005L\u0000\u0000\u0687\u0689\u0003\u0112\u0089\u0000\u0688\u068a"
            + "\u0003\f\u0006\u0000\u0689\u0688\u0001\u0000\u0000\u0000\u0689\u068a\u0001"
            + "\u0000\u0000\u0000\u068a\u06b5\u0001\u0000\u0000\u0000\u068b\u068c\u0005"
            + "]\u0000\u0000\u068c\u06b5\u0003\u0112\u0089*\u068d\u068e\u0005Q\u0000"
            + "\u0000\u068e\u06b5\u0003\u0112\u0089)\u068f\u0690\u0005I\u0000\u0000\u0690"
            + "\u06b5\u0003\u0112\u0089(\u0691\u0692\u0005\u0013\u0000\u0000\u0692\u06b5"
            + "\u0003\u0112\u0089\'\u0693\u0694\u0005\u0014\u0000\u0000\u0694\u06b5\u0003"
            + "\u0112\u0089&\u0695\u0696\u0005\u0015\u0000\u0000\u0696\u06b5\u0003\u0112"
            + "\u0089%\u0697\u0698\u0005\u0016\u0000\u0000\u0698\u06b5\u0003\u0112\u0089"
            + "$\u0699\u069a\u0005\u0017\u0000\u0000\u069a\u06b5\u0003\u0112\u0089#\u069b"
            + "\u069c\u0005\u0018\u0000\u0000\u069c\u06b5\u0003\u0112\u0089\"\u069d\u069e"
            + "\u0005d\u0000\u0000\u069e\u06b5\u0003\u0112\u0089!\u069f\u06b5\u0003\u00e8"
            + "t\u0000\u06a0\u06b5\u0003\u00e4r\u0000\u06a1\u06b5\u0003\u00e2q\u0000"
            + "\u06a2\u06b5\u0003\u00b2Y\u0000\u06a3\u06b5\u0005X\u0000\u0000\u06a4\u06a6"
            + "\u0003\u0130\u0098\u0000\u06a5\u06a7\u0003\u0112\u0089\u0000\u06a6\u06a5"
            + "\u0001\u0000\u0000\u0000\u06a6\u06a7\u0001\u0000\u0000\u0000\u06a7\u06b5"
            + "\u0001\u0000\u0000\u0000\u06a8\u06b5\u0005j\u0000\u0000\u06a9\u06b5\u0003"
            + "\u0122\u0091\u0000\u06aa\u06b5\u0003\u00fa}\u0000\u06ab\u06b5\u0003\u0100"
            + "\u0080\u0000\u06ac\u06ad\u0005\u0006\u0000\u0000\u06ad\u06ae\u0003\u0110"
            + "\u0088\u0000\u06ae\u06af\u0005\u0007\u0000\u0000\u06af\u06b5\u0001\u0000"
            + "\u0000\u0000\u06b0\u06b2\u0003\f\u0006\u0000\u06b1\u06b3\u0003\u0110\u0088"
            + "\u0000\u06b2\u06b1\u0001\u0000\u0000\u0000\u06b2\u06b3\u0001\u0000\u0000"
            + "\u0000\u06b3\u06b5\u0001\u0000\u0000\u0000\u06b4\u0673\u0001\u0000\u0000"
            + "\u0000\u06b4\u0675\u0001\u0000\u0000\u0000\u06b4\u067f\u0001\u0000\u0000"
            + "\u0000\u06b4\u0686\u0001\u0000\u0000\u0000\u06b4\u068b\u0001\u0000\u0000"
            + "\u0000\u06b4\u068d\u0001\u0000\u0000\u0000\u06b4\u068f\u0001\u0000\u0000"
            + "\u0000\u06b4\u0691\u0001\u0000\u0000\u0000\u06b4\u0693\u0001\u0000\u0000"
            + "\u0000\u06b4\u0695\u0001\u0000\u0000\u0000\u06b4\u0697\u0001\u0000\u0000"
            + "\u0000\u06b4\u0699\u0001\u0000\u0000\u0000\u06b4\u069b\u0001\u0000\u0000"
            + "\u0000\u06b4\u069d\u0001\u0000\u0000\u0000\u06b4\u069f\u0001\u0000\u0000"
            + "\u0000\u06b4\u06a0\u0001\u0000\u0000\u0000\u06b4\u06a1\u0001\u0000\u0000"
            + "\u0000\u06b4\u06a2\u0001\u0000\u0000\u0000\u06b4\u06a3\u0001\u0000\u0000"
            + "\u0000\u06b4\u06a4\u0001\u0000\u0000\u0000\u06b4\u06a8\u0001\u0000\u0000"
            + "\u0000\u06b4\u06a9\u0001\u0000\u0000\u0000\u06b4\u06aa\u0001\u0000\u0000"
            + "\u0000\u06b4\u06ab\u0001\u0000\u0000\u0000\u06b4\u06ac\u0001\u0000\u0000"
            + "\u0000\u06b4\u06b0\u0001\u0000\u0000\u0000\u06b5\u0728\u0001\u0000\u0000"
            + "\u0000\u06b6\u06b7\n2\u0000\u0000\u06b7\u06b8\u0005\u000f\u0000\u0000"
            + "\u06b8\u0727\u0003\u0112\u00893\u06b9\u06ba\n \u0000\u0000\u06ba\u06bb"
            + "\u0005\u001c\u0000\u0000\u06bb\u0727\u0003\u0112\u0089 \u06bc\u06bd\n"
            + "\u001f\u0000\u0000\u06bd\u06be\u0007\b\u0000\u0000\u06be\u0727\u0003\u0112"
            + "\u0089 \u06bf\u06c0\n\u001e\u0000\u0000\u06c0\u06c1\u0007\t\u0000\u0000"
            + "\u06c1\u0727\u0003\u0112\u0089\u001f\u06c2\u06c3\n\u001d\u0000\u0000\u06c3"
            + "\u06c4\u0005\u001d\u0000\u0000\u06c4\u0727\u0003\u0112\u0089\u001e\u06c5"
            + "\u06cc\n\u001c\u0000\u0000\u06c6\u06cd\u0005\u001f\u0000\u0000\u06c7\u06c8"
            + "\u0005!\u0000\u0000\u06c8\u06cd\u0005!\u0000\u0000\u06c9\u06ca\u0005!"
            + "\u0000\u0000\u06ca\u06cb\u0005!\u0000\u0000\u06cb\u06cd\u0005!\u0000\u0000"
            + "\u06cc\u06c6\u0001\u0000\u0000\u0000\u06cc\u06c7\u0001\u0000\u0000\u0000"
            + "\u06cc\u06c9\u0001\u0000\u0000\u0000\u06cd\u06ce\u0001\u0000\u0000\u0000"
            + "\u06ce\u0727\u0003\u0112\u0089\u001d\u06cf\u06d0\n\u001b\u0000\u0000\u06d0"
            + "\u06d1\u0007\n\u0000\u0000\u06d1\u0727\u0003\u0112\u0089\u001c\u06d2\u06d3"
            + "\n\u001a\u0000\u0000\u06d3\u06d4\u0005H\u0000\u0000\u06d4\u0727\u0003"
            + "\u0112\u0089\u001b\u06d5\u06d6\n\u0019\u0000\u0000\u06d6\u06d7\u0005^"
            + "\u0000\u0000\u06d7\u0727\u0003\u0112\u0089\u001a\u06d8\u06d9\n\u0018\u0000"
            + "\u0000\u06d9\u06da\u0007\u000b\u0000\u0000\u06da\u0727\u0003\u0112\u0089"
            + "\u0019\u06db\u06dc\n\u0017\u0000\u0000\u06dc\u06dd\u0005(\u0000\u0000"
            + "\u06dd\u0727\u0003\u0112\u0089\u0018\u06de\u06df\n\u0016\u0000\u0000\u06df"
            + "\u06e0\u0005)\u0000\u0000\u06e0\u0727\u0003\u0112\u0089\u0017\u06e1\u06e2"
            + "\n\u0015\u0000\u0000\u06e2\u06e3\u0005*\u0000\u0000\u06e3\u0727\u0003"
            + "\u0112\u0089\u0016\u06e4\u06e5\n\u0014\u0000\u0000\u06e5\u06e6\u0005+"
            + "\u0000\u0000\u06e6\u0727\u0003\u0112\u0089\u0015\u06e7\u06e8\n\u0013\u0000"
            + "\u0000\u06e8\u06e9\u0005,\u0000\u0000\u06e9\u0727\u0003\u0112\u0089\u0014"
            + "\u06ea\u06eb\n\u0012\u0000\u0000\u06eb\u06ec\u0005\u000e\u0000\u0000\u06ec"
            + "\u06ed\u0003\u0112\u0089\u0000\u06ed\u06ee\u0005\u0010\u0000\u0000\u06ee"
            + "\u06ef\u0003\u0112\u0089\u0013\u06ef\u0727\u0001\u0000\u0000\u0000\u06f0"
            + "\u06f1\n\u0011\u0000\u0000\u06f1\u06f2\u0005\r\u0000\u0000\u06f2\u0727"
            + "\u0003\u0112\u0089\u0012\u06f3\u06f4\n\u0010\u0000\u0000\u06f4\u06f5\u0003"
            + "\u0120\u0090\u0000\u06f5\u06f6\u0003\u0112\u0089\u0011\u06f6\u0727\u0001"
            + "\u0000\u0000\u0000\u06f7\u06f9\n3\u0000\u0000\u06f8\u06fa\u0005\u000f"
            + "\u0000\u0000\u06f9\u06f8\u0001\u0000\u0000\u0000\u06f9\u06fa\u0001\u0000"
            + "\u0000\u0000\u06fa\u06fb\u0001\u0000\u0000\u0000\u06fb\u06fc\u0005\u0004"
            + "\u0000\u0000\u06fc\u06fd\u0003\u0110\u0088\u0000\u06fd\u06fe\u0005\u0005"
            + "\u0000\u0000\u06fe\u0727\u0001\u0000\u0000\u0000\u06ff\u0701\n1\u0000"
            + "\u0000\u0700\u0702\u0005\u0018\u0000\u0000\u0701\u0700\u0001\u0000\u0000"
            + "\u0000\u0701\u0702\u0001\u0000\u0000\u0000\u0702\u0703\u0001\u0000\u0000"
            + "\u0000\u0703\u0705\u0005\u0012\u0000\u0000\u0704\u0706\u0005\u001e\u0000"
            + "\u0000\u0705\u0704\u0001\u0000\u0000\u0000\u0705\u0706\u0001\u0000\u0000"
            + "\u0000\u0706\u0707\u0001\u0000\u0000\u0000\u0707\u0709\u0003\u0130\u0098"
            + "\u0000\u0708\u070a\u0003\u001c\u000e\u0000\u0709\u0708\u0001\u0000\u0000"
            + "\u0000\u0709\u070a\u0001\u0000\u0000\u0000\u070a\u0727\u0001\u0000\u0000"
            + "\u0000\u070b\u070d\n0\u0000\u0000\u070c\u070e\u0005\u000e\u0000\u0000"
            + "\u070d\u070c\u0001\u0000\u0000\u0000\u070d\u070e\u0001\u0000\u0000\u0000"
            + "\u070e\u070f\u0001\u0000\u0000\u0000\u070f\u0711\u0005\u0012\u0000\u0000"
            + "\u0710\u0712\u0005\u001e\u0000\u0000\u0711\u0710\u0001\u0000\u0000\u0000"
            + "\u0711\u0712\u0001\u0000\u0000\u0000\u0712\u0713\u0001\u0000\u0000\u0000"
            + "\u0713\u0715\u0003\u0130\u0098\u0000\u0714\u0716\u0003\u001c\u000e\u0000"
            + "\u0715\u0714\u0001\u0000\u0000\u0000\u0715\u0716\u0001\u0000\u0000\u0000"
            + "\u0716\u0727\u0001\u0000\u0000\u0000\u0717\u0718\n-\u0000\u0000\u0718"
            + "\u0727\u0003\u010a\u0085\u0000\u0719\u071a\n,\u0000\u0000\u071a\u071b"
            + "\u0004\u0089&\u0000\u071b\u0727\u0005\u0013\u0000\u0000\u071c\u071d\n"
            + "+\u0000\u0000\u071d\u071e\u0004\u0089(\u0000\u071e\u0727\u0005\u0014\u0000"
            + "\u0000\u071f\u0720\n\u000f\u0000\u0000\u0720\u0727\u0003\u0124\u0092\u0000"
            + "\u0721\u0722\n\u0002\u0000\u0000\u0722\u0723\u0005`\u0000\u0000\u0723"
            + "\u0727\u0003\u0114\u008a\u0000\u0724\u0725\n\u0001\u0000\u0000\u0725\u0727"
            + "\u0005\u0018\u0000\u0000\u0726\u06b6\u0001\u0000\u0000\u0000\u0726\u06b9"
            + "\u0001\u0000\u0000\u0000\u0726\u06bc\u0001\u0000\u0000\u0000\u0726\u06bf"
            + "\u0001\u0000\u0000\u0000\u0726\u06c2\u0001\u0000\u0000\u0000\u0726\u06c5"
            + "\u0001\u0000\u0000\u0000\u0726\u06cf\u0001\u0000\u0000\u0000\u0726\u06d2"
            + "\u0001\u0000\u0000\u0000\u0726\u06d5\u0001\u0000\u0000\u0000\u0726\u06d8"
            + "\u0001\u0000\u0000\u0000\u0726\u06db\u0001\u0000\u0000\u0000\u0726\u06de"
            + "\u0001\u0000\u0000\u0000\u0726\u06e1\u0001\u0000\u0000\u0000\u0726\u06e4"
            + "\u0001\u0000\u0000\u0000\u0726\u06e7\u0001\u0000\u0000\u0000\u0726\u06ea"
            + "\u0001\u0000\u0000\u0000\u0726\u06f0\u0001\u0000\u0000\u0000\u0726\u06f3"
            + "\u0001\u0000\u0000\u0000\u0726\u06f7\u0001\u0000\u0000\u0000\u0726\u06ff"
            + "\u0001\u0000\u0000\u0000\u0726\u070b\u0001\u0000\u0000\u0000\u0726\u0717"
            + "\u0001\u0000\u0000\u0000\u0726\u0719\u0001\u0000\u0000\u0000\u0726\u071c"
            + "\u0001\u0000\u0000\u0000\u0726\u071f\u0001\u0000\u0000\u0000\u0726\u0721"
            + "\u0001\u0000\u0000\u0000\u0726\u0724\u0001\u0000\u0000\u0000\u0727\u072a"
            + "\u0001\u0000\u0000\u0000\u0728\u0726\u0001\u0000\u0000\u0000\u0728\u0729"
            + "\u0001\u0000\u0000\u0000\u0729\u0113\u0001\u0000\u0000\u0000\u072a\u0728"
            + "\u0001\u0000\u0000\u0000\u072b\u072e\u0003\u0018\f\u0000\u072c\u072d\u0005"
            + "\u0004\u0000\u0000\u072d\u072f\u0005\u0005\u0000\u0000\u072e\u072c\u0001"
            + "\u0000\u0000\u0000\u072e\u072f\u0001\u0000\u0000\u0000\u072f\u0732\u0001"
            + "\u0000\u0000\u0000\u0730\u0732\u0003\u0112\u0089\u0000\u0731\u072b\u0001"
            + "\u0000\u0000\u0000\u0731\u0730\u0001\u0000\u0000\u0000\u0732\u0115\u0001"
            + "\u0000\u0000\u0000\u0733\u0738\u0003\u0132\u0099\u0000\u0734\u0738\u0003"
            + "\u0138\u009c\u0000\u0735\u0738\u0003\u00fa}\u0000\u0736\u0738\u0003\u0100"
            + "\u0080\u0000\u0737\u0733\u0001\u0000\u0000\u0000\u0737\u0734\u0001\u0000"
            + "\u0000\u0000\u0737\u0735\u0001\u0000\u0000\u0000\u0737\u0736\u0001\u0000"
            + "\u0000\u0000\u0738\u0117\u0001\u0000\u0000\u0000\u0739\u074f\u0003\u00cc"
            + "f\u0000\u073a\u073c\u0005c\u0000\u0000\u073b\u073a\u0001\u0000\u0000\u0000"
            + "\u073b\u073c\u0001\u0000\u0000\u0000\u073c\u073d\u0001\u0000\u0000\u0000"
            + "\u073d\u073f\u0005W\u0000\u0000\u073e\u0740\u0005\u0019\u0000\u0000\u073f"
            + "\u073e\u0001\u0000\u0000\u0000\u073f\u0740\u0001\u0000\u0000\u0000\u0740"
            + "\u0741\u0001\u0000\u0000\u0000\u0741\u0743\u0005\u0006\u0000\u0000\u0742"
            + "\u0744\u0003\u00f0x\u0000\u0743\u0742\u0001\u0000\u0000\u0000\u0743\u0744"
            + "\u0001\u0000\u0000\u0000\u0744\u0745\u0001\u0000\u0000\u0000\u0745\u0747"
            + "\u0005\u0007\u0000\u0000\u0746\u0748\u00038\u001c\u0000\u0747\u0746\u0001"
            + "\u0000\u0000\u0000\u0747\u0748\u0001\u0000\u0000\u0000\u0748\u0749\u0001"
            + "\u0000\u0000\u0000\u0749\u074a\u0005\b\u0000\u0000\u074a\u074b\u0003\u00f6"
            + "{\u0000\u074b\u074c\u0005\n\u0000\u0000\u074c\u074f\u0001\u0000\u0000"
            + "\u0000\u074d\u074f\u0003\u011a\u008d\u0000\u074e\u0739\u0001\u0000\u0000"
            + "\u0000\u074e\u073b\u0001\u0000\u0000\u0000\u074e\u074d\u0001\u0000\u0000"
            + "\u0000\u074f\u0119\u0001\u0000\u0000\u0000\u0750\u0752\u0005c\u0000\u0000"
            + "\u0751\u0750\u0001\u0000\u0000\u0000\u0751\u0752\u0001\u0000\u0000\u0000"
            + "\u0752\u0753\u0001\u0000\u0000\u0000\u0753\u0755\u0003\u011c\u008e\u0000"
            + "\u0754\u0756\u00038\u001c\u0000\u0755\u0754\u0001\u0000\u0000\u0000\u0755"
            + "\u0756\u0001\u0000\u0000\u0000\u0756\u0757\u0001\u0000\u0000\u0000\u0757"
            + "\u0758\u0005:\u0000\u0000\u0758\u0759\u0003\u011e\u008f\u0000\u0759\u011b"
            + "\u0001\u0000\u0000\u0000\u075a\u0761\u0003\u0108\u0084\u0000\u075b\u075d"
            + "\u0005\u0006\u0000\u0000\u075c\u075e\u0003\u00f0x\u0000\u075d\u075c\u0001"
            + "\u0000\u0000\u0000\u075d\u075e\u0001\u0000\u0000\u0000\u075e\u075f\u0001"
            + "\u0000\u0000\u0000\u075f\u0761\u0005\u0007\u0000\u0000\u0760\u075a\u0001"
            + "\u0000\u0000\u0000\u0760\u075b\u0001\u0000\u0000\u0000\u0761\u011d\u0001"
            + "\u0000\u0000\u0000\u0762\u0768\u0003\u0112\u0089\u0000\u0763\u0764\u0005"
            + "\b\u0000\u0000\u0764\u0765\u0003\u00f6{\u0000\u0765\u0766\u0005\n\u0000"
            + "\u0000\u0766\u0768\u0001\u0000\u0000\u0000\u0767\u0762\u0001\u0000\u0000"
            + "\u0000\u0767\u0763\u0001\u0000\u0000\u0000\u0768\u011f\u0001\u0000\u0000"
            + "\u0000\u0769\u076a\u0007\f\u0000\u0000\u076a\u0121\u0001\u0000\u0000\u0000"
            + "\u076b\u0773\u0005;\u0000\u0000\u076c\u0773\u0005<\u0000\u0000\u076d\u0773"
            + "\u0005\u008b\u0000\u0000\u076e\u0773\u0003\u0124\u0092\u0000\u076f\u0773"
            + "\u0005\u0003\u0000\u0000\u0770\u0773\u0003\u0128\u0094\u0000\u0771\u0773"
            + "\u0003\u012a\u0095\u0000\u0772\u076b\u0001\u0000\u0000\u0000\u0772\u076c"
            + "\u0001\u0000\u0000\u0000\u0772\u076d\u0001\u0000\u0000\u0000\u0772\u076e"
            + "\u0001\u0000\u0000\u0000\u0772\u076f\u0001\u0000\u0000\u0000\u0772\u0770"
            + "\u0001\u0000\u0000\u0000\u0772\u0771\u0001\u0000\u0000\u0000\u0773\u0123"
            + "\u0001\u0000\u0000\u0000\u0774\u0778\u0005\u008c\u0000\u0000\u0775\u0777"
            + "\u0003\u0126\u0093\u0000\u0776\u0775\u0001\u0000\u0000\u0000\u0777\u077a"
            + "\u0001\u0000\u0000\u0000\u0778\u0776\u0001\u0000\u0000\u0000\u0778\u0779"
            + "\u0001\u0000\u0000\u0000\u0779\u077b\u0001\u0000\u0000\u0000\u077a\u0778"
            + "\u0001\u0000\u0000\u0000\u077b\u077c\u0005\u008c\u0000\u0000\u077c\u0125"
            + "\u0001\u0000\u0000\u0000\u077d\u0784\u0005\u0094\u0000\u0000\u077e\u077f"
            + "\u0005\u0093\u0000\u0000\u077f\u0780\u0003\u0112\u0089\u0000\u0780\u0781"
            + "\u0005\t\u0000\u0000\u0781\u0784\u0001\u0000\u0000\u0000\u0782\u0784\u0005"
            + "\u0092\u0000\u0000\u0783\u077d\u0001\u0000\u0000\u0000\u0783\u077e\u0001"
            + "\u0000\u0000\u0000\u0783\u0782\u0001\u0000\u0000\u0000\u0784\u0127\u0001"
            + "\u0000\u0000\u0000\u0785\u0786\u0007\r\u0000\u0000\u0786\u0129\u0001\u0000"
            + "\u0000\u0000\u0787\u0788\u0007\u000e\u0000\u0000\u0788\u012b\u0001\u0000"
            + "\u0000\u0000\u0789\u078a\u0004\u0096,\u0000\u078a\u078b\u0003\u0132\u0099"
            + "\u0000\u078b\u078c\u0003\u00ecv\u0000\u078c\u012d\u0001\u0000\u0000\u0000"
            + "\u078d\u078e\u0004\u0097-\u0000\u078e\u078f\u0003\u0132\u0099\u0000\u078f"
            + "\u0790\u0003\u00ecv\u0000\u0790\u012f\u0001\u0000\u0000\u0000\u0791\u0794"
            + "\u0003\u0132\u0099\u0000\u0792\u0794\u0003\u0136\u009b\u0000\u0793\u0791"
            + "\u0001\u0000\u0000\u0000\u0793\u0792\u0001\u0000\u0000\u0000\u0794\u0131"
            + "\u0001\u0000\u0000\u0000\u0795\u0796\u0007\u000f\u0000\u0000\u0796\u0133"
            + "\u0001\u0000\u0000\u0000\u0797\u079b\u0003\u0132\u0099\u0000\u0798\u079b"
            + "\u0005\u0081\u0000\u0000\u0799\u079b\u0005\u0084\u0000\u0000\u079a\u0797"
            + "\u0001\u0000\u0000\u0000\u079a\u0798\u0001\u0000\u0000\u0000\u079a\u0799"
            + "\u0001\u0000\u0000\u0000\u079b\u0135\u0001\u0000\u0000\u0000\u079c\u07a0"
            + "\u0003\u0138\u009c\u0000\u079d\u07a0\u0005;\u0000\u0000\u079e\u07a0\u0005"
            + "<\u0000\u0000\u079f\u079c\u0001\u0000\u0000\u0000\u079f\u079d\u0001\u0000"
            + "\u0000\u0000\u079f\u079e\u0001\u0000\u0000\u0000\u07a0\u0137\u0001\u0000"
            + "\u0000\u0000\u07a1\u07a2\u0007\u0010\u0000\u0000\u07a2\u0139\u0001\u0000"
            + "\u0000\u0000\u07a3\u07a8\u0005\u000b\u0000\u0000\u07a4\u07a8\u0005\u0000"
            + "\u0000\u0001\u07a5\u07a8\u0004\u009d.\u0000\u07a6\u07a8\u0004\u009d/\u0000"
            + "\u07a7\u07a3\u0001\u0000\u0000\u0000\u07a7\u07a4\u0001\u0000\u0000\u0000"
            + "\u07a7\u07a5\u0001\u0000\u0000\u0000\u07a7\u07a6\u0001\u0000\u0000\u0000"
            + "\u07a8\u013b\u0001\u0000\u0000\u0000\u0102\u0141\u0145\u014e\u0153\u015a"
            + "\u0161\u016a\u0170\u0176\u0181\u0183\u019a\u01a0\u01a5\u01b1\u01b8\u01bc"
            + "\u01c1\u01c7\u01cb\u01d1\u01d8\u01e2\u01e4\u01f4\u01f8\u01fb\u01ff\u0207"
            + "\u020b\u021a\u021e\u0221\u0225\u0228\u022c\u0232\u0236\u023a\u0242\u0247"
            + "\u024a\u024c\u0253\u0258\u025b\u025e\u0263\u0266\u0269\u026e\u0271\u0274"
            + "\u0278\u027e\u0282\u0286\u028a\u0295\u029a\u029f\u02a6\u02ab\u02b3\u02b6"
            + "\u02b9\u02be\u02c1\u02c5\u02cf\u02d3\u02d9\u02df\u02e6\u02ec\u02ef\u02f5"
            + "\u02fd\u0302\u030d\u0312\u031a\u0321\u0328\u032d\u034e\u0352\u0359\u0360"
            + "\u0368\u036c\u0373\u037b\u0380\u0382\u0389\u038d\u0396\u039a\u03a2\u03a6"
            + "\u03aa\u03b3\u03bb\u03bf\u03c7\u03cc\u03ce\u03d5\u03da\u03de\u03e2\u03e5"
            + "\u03e8\u03eb\u03ef\u03f3\u03f7\u03f9\u0400\u0406\u0409\u040c\u0410\u0413"
            + "\u041a\u0423\u0436\u043a\u043e\u0448\u044c\u0464\u046d\u0474\u047e\u0483"
            + "\u048a\u0491\u0498\u049f\u04b1\u04b5\u04b7\u04be\u04c4\u04c9\u04d8\u04db"
            + "\u04e1\u04e5\u04f0\u04f4\u04fd\u0500\u0504\u0506\u0509\u050e\u0514\u0517"
            + "\u051d\u052a\u052f\u0534\u0537\u053a\u0546\u054b\u054e\u0551\u0554\u0557"
            + "\u055a\u0561\u0564\u0569\u0571\u0576\u057a\u0587\u058b\u0598\u059c\u05a5"
            + "\u05ae\u05b8\u05bd\u05c0\u05c7\u05c9\u05cc\u05cf\u05d3\u05d6\u05da\u05df"
            + "\u05e2\u05e7\u05f0\u05f4\u05f9\u05fe\u0604\u0608\u060c\u060f\u0617\u061b"
            + "\u061d\u0630\u0634\u063a\u0643\u0651\u0656\u0658\u0661\u0665\u0669\u0670"
            + "\u0677\u067a\u0682\u0689\u06a6\u06b2\u06b4\u06cc\u06f9\u0701\u0705\u0709"
            + "\u070d\u0711\u0715\u0726\u0728\u072e\u0731\u0737\u073b\u073f\u0743\u0747"
            + "\u074e\u0751\u0755\u075d\u0760\u0767\u0772\u0778\u0783\u0793\u079a\u079f" + "\u07a7";
    public static final ATN _ATN = new ATNDeserializer().deserialize(_serializedATN.toCharArray());
    static {
        _decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
        for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
            _decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
        }
    }
}
