package plc.project;

import com.sun.jdi.connect.Connector;

import javax.lang.model.type.NullType;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The parser takes the sequence of tokens emitted by the lexer and turns that
 * into a structured representation of the program, called the Abstract Syntax
 * Tree (AST).
 * <p>
 * The parser has a similar architecture to the lexer, just with {@link Token}s
 * instead of characters. As before, {@link #peek(Object...)} and {@link
 * #match(Object...)} are helpers to make the implementation easier.
 * <p>
 * This type of parser is called <em>recursive descent</em>. Each rule in our
 * grammar will have it's own function, and reference to other rules correspond
 * to calling that functions.
 */
public final class Parser {

    private final TokenStream tokens;

    public Parser(List<Token> tokens) {
        this.tokens = new TokenStream(tokens);
    }

    /**
     * Parses the {@code source} rule.
     */

    public Ast.Source parseSource() throws ParseException {

        try {
            List<Ast.Global> globals = new ArrayList<>();
            List<Ast.Function> functions = new ArrayList<>();
            boolean functionEncountered = false;

            while (tokens.has(0)) {
                if (peek("LIST") || peek("VAR") || peek("VAL")) {
                    if (functionEncountered) {
                        throw new ParseException("Invalid Source: Globals Cannot Come After Functions", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
                    }
                    globals.add(parseGlobal());
                } else if (peek("FUN")) {
                    functions.add(parseFunction());
                    functionEncountered = true;
                }
            }
            return new Ast.Source(globals, functions);
        } catch (ParseException p) {
            throw new ParseException(p.getMessage(), p.getIndex());
        }
    }

    /**
     * Parses the {@code global} rule. This method should only be called if the
     * next tokens start a global, aka {@code LIST|VAL|VAR}.
     */
    public Ast.Global parseGlobal() throws ParseException {
        try {
            if (match("LIST")) {

                return parseList();
            } else if (match("VAR")) {
                return parseMutable();
            } else if (match("VAL")) {
//                System.out.println("Global search -- immutable found");
                return parseImmutable();
            } else {
                throw new ParseException("Invalid Global", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
            }
        } catch (ParseException p) {
            throw new ParseException(p.getMessage(), p.getIndex());
        }
    }

    /**
     * Parses the {@code list} rule. This method should only be called if the
     * next token declares a list, aka {@code LIST}.
     */
    public Ast.Global parseList() throws ParseException {
        try {
            // 'LIST' already matched
            String name;
            String typeName;
            boolean mutable = true;
            List<Ast.Expression> expressions = new ArrayList<>();
            if (!match(Token.Type.IDENTIFIER)) {
                throw new ParseException("Invalid List: Missing Identifier", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
            } else {
                name = tokens.get(-1).getLiteral();
            }
            if(!match(":")){
                throw new ParseException("Invalid List: Missing ':'", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
            }
            if(match(Token.Type.IDENTIFIER)){
                typeName = tokens.get(-1).getLiteral();
            } else {
                throw new ParseException("Invalid List: Missing type name", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
            }

            if (!match("=")) {
                throw new ParseException("Invalid List: Missing '='", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
            }
            if (!match("[")) {
                throw new ParseException("Invalid List: Expecting '['", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
            } else {
                // '(' found and need to extract everything else
                expressions.add(parseExpression());
                if (!peek("]")) {

                    while (match(",")) {
                        if (peek("]")) {
                            throw new ParseException("Invalid List: Expected Another Expression", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());

                        }
                        try {
                            expressions.add(parseExpression());
                        } catch (ParseException p2) {
                            throw new ParseException(p2.getMessage(), p2.getIndex());
                        }
                    }

                }

                if (!match("]")) {
                    throw new ParseException("Invalid List: Expecting ']'", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
                }
            }
            if (!match(";")) {
                throw new ParseException("semicolon missing", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
            } else {
                return new Ast.Global(name, typeName, mutable, Optional.of(new Ast.Expression.PlcList(expressions)));
            }


//            Ast.Expression.PlcList list;
//            return new Ast.Global(name, mutable, Optional.of(list));
        } catch (ParseException p) {
            throw new ParseException(p.getMessage(), p.getIndex());
        }
    }

    /**
     * Parses the {@code mutable} rule. This method should only be called if the
     * next token declares a mutable global variable, aka {@code VAR}.
     */
    public Ast.Global parseMutable() throws ParseException {
        try {
            boolean mutable = true;
            //expecting that we already parse "VAL"
            String name;
            String typeName;
            if (match(Token.Type.IDENTIFIER)) {
                name = tokens.get(-1).getLiteral();
            } else {
                throw new ParseException("No identifier found", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
            }

            if (!match(":")) {
                throw new ParseException("Invalid Mutable: Expecting ':'", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
            }
            if (match(Token.Type.IDENTIFIER)) {
                typeName = tokens.get(-1).getLiteral();
            } else {
                throw new ParseException("Invalid Mutable: missing type name", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
            }

            if (match("=")) {
                Ast.Expression expression = parseExpression();
                if (!match(";")) {
                    throw new ParseException("Invalid mutable: semicolon missing (1)", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
                } else {
                    return new Ast.Global(name, typeName, mutable, Optional.of(expression));
                }
            } else if (!match(";")) {
                throw new ParseException("Invalid mutable: semicolon missing (2)", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
            } else {
                return new Ast.Global(name, mutable, Optional.empty());
            }

        } catch (ParseException p) {
            throw new ParseException(p.getMessage(), p.getIndex());
        }
    }

    /**
     * Parses the {@code immutable} rule. This method should only be called if the
     * next token declares an immutable global variable, aka {@code VAL}.
     */
    public Ast.Global parseImmutable() throws ParseException {
        try {
            boolean mutable = false;
            //expecting that we already parse "VAL"
            String name;
            String typeName;
            if (match(Token.Type.IDENTIFIER)) {

                name = tokens.get(-1).getLiteral();
//                System.out.println("Found Identifier");
            } else {
                throw new ParseException("No identifier found", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
            }

            // look for : -> IDENTIFIER Type -> =
            if(!match(":")){
                throw new ParseException("Invalid immutable: Missing :", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
            }

            if(match(Token.Type.IDENTIFIER)){
                typeName = tokens.get(-1).getLiteral();
            } else {
                throw new ParseException("Invalid immutable: Missing Type", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());

            }


            if (!match("=")) {
//                System.out.println("Here");
                throw new ParseException("Invalid immutable", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
            } else {
                Ast.Expression expression = parseExpression();
                if (!match(";")) {
                    throw new ParseException("semicolon missing", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
                } else {
                    return new Ast.Global(name, typeName, mutable, Optional.of(expression));
                }
            }
        } catch (ParseException p) {
            throw new ParseException(p.getMessage(), p.getIndex());
        }
    }

    /**
     * Parses the {@code function} rule. This method should only be called if the
     * next tokens start a method, aka {@code FUN}.
     */
    public Ast.Function parseFunction() throws ParseException {

        //Has Identifier () 'DO' Block 'END'
        try {
            String name;
            String retType = "";
            List<String> paramTypes = new ArrayList<>();
            List<String> parameters = new ArrayList<>();
            if (!match("FUN")) {
                throw new ParseException("Invalid Funciton: Missing 'FUN' Keyword", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
            }
            if (!match(Token.Type.IDENTIFIER)) {
                throw new ParseException("Missing function name", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
            } else {
                name = tokens.get(-1).getLiteral();
            }

            if (!match("(")) {
                throw new ParseException("Invalid Funciton: Expecting '('", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
            } else {
                // '(' found and need to extract everything else
                if (!peek(")")) {
                    while (match(Token.Type.IDENTIFIER)) {
                        parameters.add(tokens.get(-1).getLiteral());
                        if(!match(":")){
                            throw new ParseException("Invalid Function (Parse parameters): Expecting ':'", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
                        }
                        if (match(Token.Type.IDENTIFIER)) {
                            paramTypes.add(tokens.get(-1).getLiteral());
                        } else {
                            throw new ParseException("Invalid Function (Parse parameters): Expecting parameter type", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
                        }

                        if (!match(",")) {
                            if (!peek(")")) {
                                throw new ParseException("Invalid Function: Expected ',' between parameters", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
                            }
                        }
                    }

                }
                if (!match(")")) {
                    throw new ParseException("Invalid Function: Expecting ')'", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
                }
            }

            // look for : -> IDENTIFIER type
            if (match(":")) {   // if this does not exist, assume it is a void function
//                throw new ParseException("Invalid Function: Expecting ':'", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
                if (match(Token.Type.IDENTIFIER)) {
                    retType = tokens.get(-1).getLiteral();
                } else {
                    throw new ParseException("Invalid Function: Missing return type", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());

                }
            }


            //'DO' Block 'END'
            if (!match("DO")) {
                throw new ParseException("Invalid Function: Invalid DO", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
            }

            List<Ast.Statement> statements = parseBlock();


            if (!match("END")) {
                throw new ParseException("Invalid Function: Missing 'END' Keyword", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
            }

//            List<String> paramTypes;

            return new Ast.Function(name, parameters, paramTypes, Optional.ofNullable(retType), statements);

        } catch (ParseException p) {
            throw new ParseException(p.getMessage(), p.getIndex());
        }

    }

    /**
     * Parses the {@code block} rule. This method should only be called if the
     * preceding token indicates the opening a block of statements.
     */
    public List<Ast.Statement> parseBlock() throws ParseException {
        //TODO Fix
        //Peeks for ways Block can conclude, does not match those conditions
        try {
            List<Ast.Statement> StatementList = new ArrayList<>();
            while (!peek("END") && !peek("ELSE") && !peek("DEFAULT") && !peek("CASE")) {
                StatementList.add(parseStatement());
            }

            return StatementList;

        } catch (ParseException p) {
            if (!tokens.has(0)) {
                List<Ast.Statement> StatementList = new ArrayList<>();
                return StatementList;
            } else {
                throw new ParseException(p.getMessage() + " Invalid Block.", p.getIndex());
            }
        }


    }

    /**
     * Parses the {@code statement} rule and delegates to the necessary method.
     * If the next tokens do not start a declaration, if, while, or return
     * statement, then it is an expression/assignment statement.
     */
    public Ast.Statement parseStatement() throws ParseException {
        //TODO: (later) be able to determine what type of statement to parse
        // for now, just parse expressions

        //All parse logic contained in sub functions, except for Statement.Expression

        //Let
        if (peek("LET")) {
            try {
                return parseDeclarationStatement();
            } catch (ParseException p) {
                throw new ParseException(p.getMessage(), p.getIndex());
            }
        }

        //Switch
        else if (peek("SWITCH")) {
            try {
                return parseSwitchStatement();
            } catch (ParseException p) {
                throw new ParseException(p.getMessage(), p.getIndex());
            }
        }

        //IF
        else if (peek("IF")) {
            try {
                return parseIfStatement();
            } catch (ParseException p) {
                throw new ParseException(p.getMessage(), p.getIndex());
            }
        }

        //While
        else if (peek("WHILE")) {
            try {
                return parseWhileStatement();
            } catch (ParseException p) {
                throw new ParseException(p.getMessage(), p.getIndex());
            }
        }

        //Return
        else if (peek("RETURN")) {
            try {
                return parseReturnStatement();
            } catch (ParseException p) {
                throw new ParseException(p.getMessage(), p.getIndex());
            }
        } else {
            Ast.Expression initExpr = parseExpression();
            if (!peek("=")) {
                if (!match(";")) {
                    throw new ParseException("Invalid statement: semicolon missing", tokens.get(-1).getIndex());
                }
                Ast.Statement.Expression help = new Ast.Statement.Expression(initExpr);

                return help;
            } else {
                try {
                    match("=");
                    Ast.Expression assignExpr = parseExpression();
                    if (!match(";")) {
                        throw new ParseException("Invalid statement: semicolon missing", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
                    }
                    return new Ast.Statement.Assignment(initExpr, assignExpr);
                } catch (ParseException p) {
                    throw new ParseException(p.getMessage(), p.getIndex());
                }

            }
        }

    }

    /**
     * Parses a declaration statement from the {@code statement} rule. This
     * method should only be called if the next tokens start a declaration
     * statement, aka {@code LET}.
     */
    public Ast.Statement.Declaration parseDeclarationStatement() throws ParseException {
        //'LET' identifier ('=' expression)? ';'
        if (match("LET")) {
            if (match(Token.Type.IDENTIFIER)) {
                String IdentifierString = tokens.get(-1).getLiteral();

                String typeName;

                //Initialization
                if (match("=")) {
                    try {
                        Ast.Expression Expr = parseExpression();
                        if (match(";")) {
                            return new Ast.Statement.Declaration(IdentifierString, Optional.of(Expr));
                        } else {
                            throw new ParseException("Exception missing ';'", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
                        }
                    } catch (ParseException p) {
                        throw new ParseException(p.getMessage(), p.getIndex());
                    }
                }
                //Definition

                //TODO Added this to help with declaration problem
                if (match(";")) {
                    return new Ast.Statement.Declaration(IdentifierString, Optional.ofNullable(null), Optional.empty());
                }
                if(!match(":")){
                    throw new ParseException("Invalid Declaration: Exception missing ':'", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
                }
                if(match(Token.Type.IDENTIFIER)){
                    typeName = tokens.get(-1).getLiteral();
                } else {
                    throw new ParseException("Invalid Declaration: Exception missing type name", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
                }
                if (match(";")) {
                    return new Ast.Statement.Declaration(IdentifierString, Optional.ofNullable(typeName), Optional.empty());
                } else {
//                    System.out.println("Throwing Here");
                    throw new ParseException("Exception missing ';'", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
                }

            } else {
                throw new ParseException("Exception missing identifier", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
            }
        } else {
            throw new ParseException("Invalid parseDeclarationStatement.", tokens.get(0).getIndex());
        }
    }

    /**
     * Parses an if statement from the {@code statement} rule. This method
     * should only be called if the next tokens start an if statement, aka
     * {@code IF}.
     */
    public Ast.Statement.If parseIfStatement() throws ParseException {
        if (match("IF")) {
            try {
                Ast.Expression Condition = parseExpression();
                if (match("DO")) {
                    List<Ast.Statement> DoBLock = parseBlock();
                    if (match("ELSE")) {
                        List<Ast.Statement> ElseBLock = parseBlock();
                        if (match("END")) {
                            return new Ast.Statement.If(Condition, DoBLock, ElseBLock);
                        } else {
                            throw new ParseException("Expected if else END.", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length()+1);
                        }
                    } else {
                        if (match("END")) {
                            List<Ast.Statement> EmptyElse = new ArrayList<>();
                            return new Ast.Statement.If(Condition, DoBLock, EmptyElse);
                        } else {
                            throw new ParseException("Expected if END.", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length()+1);
                        }
                    }
                } else {
                    // TODO: fails for the test case "IF expr THEN"
                    // Exception throws at index 7, but I think it should be at index 8
                    throw new ParseException("Expected if DO.", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length()+1);
                }
            } catch (ParseException p) {
                throw new ParseException(p.getMessage(), p.getIndex());
            }
        } else {
            throw new ParseException("Invalid parseIfStatement.", tokens.get(0).getIndex());
        }
    }

    /**
     * Parses a switch statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a switch statement, aka
     * {@code SWITCH}.
     */
    public Ast.Statement.Switch parseSwitchStatement() throws ParseException {
        if (match("SWITCH")) {
            try {
                Ast.Expression Condition = parseExpression();
                List<Ast.Statement.Case> Cases = new ArrayList<>();
                while (peek("CASE")) {
                    Cases.add(parseCaseStatement());
                }
                if (peek("DEFAULT")) {
                    Cases.add(parseCaseStatement());
                    if (match("END")) {
                        return new Ast.Statement.Switch(Condition, Cases);
                    } else {
                        throw new ParseException("Expected switch END.", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length()+1);
                    }
                } else {
                    throw new ParseException("Expected switch DEFAULT.", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length()+1);
                }
            } catch (ParseException p) {
                throw new ParseException(p.getMessage(), p.getIndex());
            }
        } else {
            throw new ParseException("Invalid parseSwitchStatement.", tokens.get(0).getIndex());
        }
    }

    /**
     * Parses a case or default statement block from the {@code switch} rule.
     * This method should only be called if the next tokens start the case or
     * default block of a switch statement, aka {@code CASE} or {@code DEFAULT}.
     */
    public Ast.Statement.Case parseCaseStatement() throws ParseException {
        if (match("CASE")) {
            Ast.Expression CaseValue = parseExpression();
            if (match(":")) {
                return new Ast.Statement.Case(Optional.of(CaseValue), parseBlock());
            } else {
                throw new ParseException("Expected case :.", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
            }
        } else if (match("DEFAULT")) {
            return new Ast.Statement.Case(Optional.empty(), parseBlock());
        } else {
            throw new ParseException("Invalid parseCaseStatement.", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
        }
    }

    /**
     * Parses a while statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a while statement, aka
     * {@code WHILE}.
     */
    public Ast.Statement.While parseWhileStatement() throws ParseException {
        //'WHILE' expression 'DO' block 'END'
        if (match("WHILE")) {
            try {
                Ast.Expression Condition = parseExpression();
                if (match("DO")) {
                    List<Ast.Statement> DoBlock = parseBlock();
                    if (match("END")) {
                        return new Ast.Statement.While(Condition, DoBlock);
                    } else {
                        throw new ParseException("Expected while END.", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length()+1);
                    }
                } else {
                    throw new ParseException("Expected while DO.", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length()+1);
                }
            } catch (ParseException p) {
                throw new ParseException(p.getMessage(), p.getIndex());
            }
        }
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses a return statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a return statement, aka
     * {@code RETURN}.
     */
    public Ast.Statement.Return parseReturnStatement() throws ParseException {

        if (match("RETURN")) {
            try {
                Ast.Expression expression = parseExpression();
                if (match(";")) {
                    return new Ast.Statement.Return(expression);
                } else {
                    throw new ParseException("Expected ;.", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
                }
            } catch (ParseException p) {
                throw new ParseException(p.getMessage(), p.getIndex());
            }
        } else {
            throw new ParseException("Invalid parseReturnStatement.", tokens.get(0).getIndex());
        }

    }

    /**
     * Parses the {@code expression} rule.
     */
    public Ast.Expression parseExpression() throws ParseException {
        if (!tokens.has(0)) {
            throw new ParseException("Expected expression.", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
        }
        return parseLogicalExpression();


    }

    /**
     * Parses the {@code logical-expression} rule.
     */
    public Ast.Expression parseLogicalExpression() throws ParseException {
        try {
            Ast.Expression left = parseComparisonExpression();

            while (match("&&") || match("||")) {
                String operator = tokens.get(-1).getLiteral();
                Ast.Expression right = parseComparisonExpression();
                left = new Ast.Expression.Binary(operator, left, right);
            }
            return left;

        } catch (ParseException p) {
            throw new ParseException(p.getMessage(), p.getIndex());
        }


    }

    /**
     * Parses the {@code comparison-expression} rule.
     */
    public Ast.Expression parseComparisonExpression() throws ParseException {
        try {
            Ast.Expression left = parseAdditiveExpression();

            while (match(">") || match("<") || match("==") || match(("!="))) {
                String operator = tokens.get(-1).getLiteral();
                Ast.Expression right = parseAdditiveExpression();
                left = new Ast.Expression.Binary(operator, left, right);
            }
            return left;

        } catch (ParseException p) {
            throw new ParseException(p.getMessage(), p.getIndex());
        }
    }

    /**
     * Parses the {@code additive-expression} rule.
     */
    public Ast.Expression parseAdditiveExpression() throws ParseException {
        try {
            Ast.Expression left = parseMultiplicativeExpression();

            while (match("+") || match("-")) {
                String operator = tokens.get(-1).getLiteral();
                Ast.Expression right = parseMultiplicativeExpression();
                left = new Ast.Expression.Binary(operator, left, right);
            }
            return left;

        } catch (ParseException p) {
            throw new ParseException(p.getMessage(), p.getIndex());
        }
    }

    /**
     * Parses the {@code multiplicative-expression} rule.
     */
    public Ast.Expression parseMultiplicativeExpression() throws ParseException {
        try {

            Ast.Expression left = parsePrimaryExpression();


            while (match("*") || match("/") || match("^")) {
                String operator = tokens.get(-1).getLiteral();
                Ast.Expression right = parsePrimaryExpression();
                left = new Ast.Expression.Binary(operator, left, right);
            }

            return left;


        } catch (ParseException p) {
            throw new ParseException(p.getMessage(), p.getIndex());
        }
    }

    /**
     * Parses the {@code primary-expression} rule. This is the top-level rule
     * for expressions and includes literal values, grouping, variables, and
     * functions. It may be helpful to break these up into other methods but is
     * not strictly necessary.
     */
    public Ast.Expression parsePrimaryExpression() throws ParseException {
        if (tokens.has(0)) {
            if (match("NIL")) {
                return new Ast.Expression.Literal(null);
            } else if (match("TRUE")) {
                return new Ast.Expression.Literal(Boolean.TRUE);
            } else if (match("FALSE")) {
                return new Ast.Expression.Literal(Boolean.FALSE);
            } else if (match(Token.Type.INTEGER)) {
                return new Ast.Expression.Literal(new BigInteger(tokens.get(-1).getLiteral()));
            } else if (match(Token.Type.DECIMAL)) {
                return new Ast.Expression.Literal(new BigDecimal(tokens.get(-1).getLiteral()));
            } else if (match(Token.Type.CHARACTER)) {
                String found = tokens.get(-1).getLiteral();
                return new Ast.Expression.Literal(found.charAt(1));
            } else if (match(Token.Type.STRING)) {
                String found = tokens.get(-1).getLiteral();
                found = found.substring(1, found.length() - 1);

                if (found.contains("\\")) {
                    // need to replace the whitespace/escape characters in the string
                    found = found.replace("\\b", "\b").replace("\\n", "\n").replace("\\r", "\r").replace("\\t", "\t").replace("\\'", "'").replace("\\\"", "\"").replace("\\\\", "\\");
                }
                // TODO: need to implement specific whitespace properties
                return new Ast.Expression.Literal(found);
            } else if (match("(")) {
                Ast.Expression grouped = parseExpression();
                if (!match(")")) {
                    throw new ParseException("Expected ')'.", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
                }

                return new Ast.Expression.Group(grouped);
            } else if (match(Token.Type.IDENTIFIER)) {
                String identifier = tokens.get(-1).getLiteral();
                // Function Call
                if (peek("(")) {
                    match("(");
                    List<Ast.Expression> arguments = new ArrayList<>();


                    if (!tokens.has(0)) {
                        throw new ParseException("Expected expression.", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
                    }
                    if (!peek(")")) {
                        try {
                            arguments.add(parseExpression());
                            while (match(",")) {
                                if (peek(")")) {
                                    throw new ParseException("Expected expression.", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
                                }
                                arguments.add(parseExpression());
                            }
                        } catch (ParseException p) {
                            throw new ParseException(p.getMessage(), p.getIndex());
                        }
                    }

                    if (!match(")")) {
                        throw new ParseException("Expected ')'.", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
                    }
                    return new Ast.Expression.Function(identifier, arguments);

                } else if (peek("[")) {
                    match("[");
                    Ast.Expression index = parseExpression();
                    if (!match("]")) {
                        throw new ParseException("Invalid call to list.", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
                    }
                    return new Ast.Expression.Access(Optional.of(index), identifier);
                } else if (!match("(") && !match("[")) {

                    return new Ast.Expression.Access(Optional.empty(), identifier);

                } else {
                    throw new ParseException("Invalid identifier call", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
                }
           /* TODO: look for the following
                identifier ('(' (expression (',' expression)*)? ')')? |
                identifier '[' expression ']'
            */
            } else {
                throw new ParseException("Invalid expression.", tokens.get(0).getIndex());
            }
        } else {
            if (tokens.has(-1)) {
                throw new ParseException("Expected Ast.Expression.", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
            } else {
                throw new ParseException("Invalid TokenStream", 0);
            }
        }
    }

    /**
     * As in the lexer, returns {@code true} if the current sequence of tokens
     * matches the given patterns. Unlike the lexer, the pattern is not a regex;
     * instead it is either a {@link Token.Type}, which matches if the token's
     * type is the same, or a {@link String}, which matches if the token's
     * literal is the same.
     * <p>
     * In other words, {@code Token(IDENTIFIER, "literal")} is matched by both
     * {@code peek(Token.Type.IDENTIFIER)} and {@code peek("literal")}.
     */
    private boolean peek(Object... patterns) {

        for (int i = 0; i < patterns.length; i++) {
            if (!tokens.has(i)) {
                return false;
            } else if (patterns[i] instanceof Token.Type) {
                if (patterns[i] != tokens.get(i).getType()) {
                    return false;
                }
            } else if (patterns[i] instanceof String) {
                if (!patterns[i].equals(tokens.get(i).getLiteral())) {
                    return false;
                }
            } else {
                throw new ParseException("Invalid Pattern: " + patterns[i].getClass(), i);
            }
        }

        return true;
    }

    /**
     * As in the lexer, returns {@code true} if {@link #peek(Object...)} is true
     * and advances the token stream.
     */
    private boolean match(Object... patterns) {

//        System.out.println("TEST == " + patterns.);
        boolean peek = peek(patterns);
        if (peek) {
            for (int i = 0; i < patterns.length; i++) {
//                System.out.println("TEST -- " + tokens.get(i));
                tokens.advance();
            }
        }
        return peek;
    }

    private static final class TokenStream {

        private final List<Token> tokens;
        private int index = 0;

        private TokenStream(List<Token> tokens) {
            this.tokens = tokens;
        }

        /**
         * Returns true if there is a token at index + offset.
         */
        public boolean has(int offset) {
            return index + offset < tokens.size();
        }

        /**
         * Gets the token at index + offset.
         */
        public Token get(int offset) {
            return tokens.get(index + offset);
        }

        /**
         * Advances to the next token, incrementing the index.
         */
        public void advance() {
            index++;
        }

    }

}