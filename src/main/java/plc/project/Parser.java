package plc.project;

import com.sun.jdi.connect.Connector;

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

        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses the {@code global} rule. This method should only be called if the
     * next tokens start a global, aka {@code LIST|VAL|VAR}.
     */
    public Ast.Global parseGlobal() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses the {@code list} rule. This method should only be called if the
     * next token declares a list, aka {@code LIST}.
     */
    public Ast.Global parseList() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses the {@code mutable} rule. This method should only be called if the
     * next token declares a mutable global variable, aka {@code VAR}.
     */
    public Ast.Global parseMutable() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses the {@code immutable} rule. This method should only be called if the
     * next token declares an immutable global variable, aka {@code VAL}.
     */
    public Ast.Global parseImmutable() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses the {@code function} rule. This method should only be called if the
     * next tokens start a method, aka {@code FUN}.
     */
    public Ast.Function parseFunction() throws ParseException {

        //Has Identifier () 'DO' Block 'END'



        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses the {@code block} rule. This method should only be called if the
     * preceding token indicates the opening a block of statements.
     */
    public List<Ast.Statement> parseBlock() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses the {@code statement} rule and delegates to the necessary method.
     * If the next tokens do not start a declaration, if, while, or return
     * statement, then it is an expression/assignment statement.
     */
    public Ast.Statement parseStatement() throws ParseException {
        //TODO: (later) be able to determine what type of statement to parse
        // for now, just parse expressions
        Ast.Expression initExpr = parseExpression();

        if(!peek("=")){
            if(!match(";")){
                throw new ParseException("Invalid statement: semicolon missing", tokens.get(-1).getIndex());
            }
            return new Ast.Statement.Expression(initExpr);
        } else {
            try{
                match("=");
                Ast.Expression assignExpr = parseExpression();
                if(!match(";")){
                    throw new ParseException("Invalid statement: semicolon missing", tokens.get(-1).getIndex());
                }
                return new Ast.Statement.Assignment(initExpr, assignExpr);
            } catch (ParseException p){
                throw new ParseException(p.getMessage(), p.getIndex());
            }
        }
    }

    /**
     * Parses a declaration statement from the {@code statement} rule. This
     * method should only be called if the next tokens start a declaration
     * statement, aka {@code LET}.
     */
    public Ast.Statement.Declaration parseDeclarationStatement() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses an if statement from the {@code statement} rule. This method
     * should only be called if the next tokens start an if statement, aka
     * {@code IF}.
     */
    public Ast.Statement.If parseIfStatement() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses a switch statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a switch statement, aka
     * {@code SWITCH}.
     */
    public Ast.Statement.Switch parseSwitchStatement() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses a case or default statement block from the {@code switch} rule.
     * This method should only be called if the next tokens start the case or
     * default block of a switch statement, aka {@code CASE} or {@code DEFAULT}.
     */
    public Ast.Statement.Case parseCaseStatement() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses a while statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a while statement, aka
     * {@code WHILE}.
     */
    public Ast.Statement.While parseWhileStatement() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses a return statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a return statement, aka
     * {@code RETURN}.
     */
    public Ast.Statement.Return parseReturnStatement() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses the {@code expression} rule.
     */
    public Ast.Expression parseExpression() throws ParseException {

        return parseLogicalExpression();

//        throw new UnsupportedOperationException(); //TODO
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


            while (match("*") || match("/")) {
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
                throw new ParseException("Invalid Grouping", tokens.get(-1).getIndex());
            }
            return new Ast.Expression.Group(grouped);
        } else if (match(Token.Type.IDENTIFIER)) {
            String identifier = tokens.get(-1).getLiteral();
            // Function Call
            if (peek("(")) {
                match("(");
                List<Ast.Expression> arguments = new ArrayList<>();
                if(!peek(")")){
                    try{
                        arguments.add(parseExpression());
                        while (match(",")){
                            arguments.add(parseExpression());
                        }
                    }
                   catch(ParseException p) {
                       throw new ParseException(p.getMessage(), p.getIndex());
                   }
                }
                if(!match(")")){
                    throw new ParseException("Invalid Function, missing closing bracket after", tokens.get(-1).getIndex());
                }
                return new Ast.Expression.Function(identifier, arguments);

            } else if (peek("[")) {
                match("[");
                System.out.println("List Access");
                Ast.Expression index = parseExpression();
                if (!match("]")) {
                    throw new ParseException("Invalid call to list", tokens.get(-1).getIndex());
                }
                return new Ast.Expression.Access(Optional.of(index), identifier);
            } else if (!match("(") && !match("[")) {

                return new Ast.Expression.Access(Optional.empty(), identifier);
            }else {
                throw new ParseException("Invalid identifier call", tokens.get(-1).getIndex());
            }
           /* TODO: look for the following
                identifier ('(' (expression (',' expression)*)? ')')? |
                identifier '[' expression ']'
            */
        } else {
            throw new ParseException("Invalid Primary Expression", tokens.get(-1).getIndex());
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
                if (patterns[i] != tokens.get(i).getLiteral()) {
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

        boolean peek = peek(patterns);
        if (peek) {
            for (int i = 0; i < patterns.length; i++) {
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