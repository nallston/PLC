package plc.project;

import java.util.ArrayList;
import java.util.List;


/**
 * The lexer works through three main functions:
 *
 *  - {@link #lex()}, which repeatedly calls lexToken() and skips whitespace
 *  - {@link #lexToken()}, which lexes the next token
 *  - {@link CharStream}, which manages the state of the lexer and literals
 *
 * If the lexer fails to parse something (such as an unterminated string) you
 * should throw a {@link ParseException} with an index at the character which is
 * invalid.
 *
 * The {@link #peek(String...)} and {@link #match(String...)} functions are * helpers you need to use, they will make the implementation a lot easier. */
public final class Lexer {
    private final CharStream chars;

    public Lexer(String input) {
        chars = new CharStream(input);

    }


    /**
     * Repeatedly lexes the input using {@link #lexToken()}, also skipping over
     * whitespace where appropriate.
     */
    public List<Token> lex() {


        List<Token> TokenList = new ArrayList<>();

        while(this.chars.has(0)){

            lexEscape();
            if(this.chars.has(0)){
                TokenList.add(this.lexToken());
            }

        }
        return TokenList;
    }

    /**
     * This method determines the type of the next token, delegating to the
     * appropriate lex method. As such, it is best for this method to not change
     * the state of the char stream (thus, use peek not match).
     *
     * The next character should start a valid token since whitespace is handled
     * by {@link #lex()}
     */
    public Token lexToken() {

        Token returnToken;
        if (this.peek("@|[A-Za-z]")) {
            returnToken = lexIdentifier();
        } else if (this.peek("0|-|[1-9]")) {
            returnToken = lexNumber();
        } else if (this.peek("'")) {
            returnToken = lexCharacter();
        } else if (this.peek("\"")) {
            returnToken = lexString();
        }
        //Operater peek
        //"!|=|&|\\||\\."
        else {
            returnToken = lexOperator();
        }
        return returnToken;
    }

    public Token lexIdentifier() {
        this.match("@|[A-Za-z]");
        boolean end=false;

        while(this.chars.has(0) && !end){
            if(!this.match("[A-Za-z0-9_-]")){
                end = true;
            }
        }
        return chars.emit(Token.Type.IDENTIFIER);
    }

    public Token lexNumber() {
        boolean Decimal = false;
        boolean Negative = false;
        if(this.match("-")) {
            if(this.chars.has(0)){
                Negative = true;
            }
            else{
                throw new ParseException(this.chars.input, this.chars.index);
            }
        }
        if(this.peek("0")){
            if(!this.chars.has(1) && !Negative){
                this.match("0");
                return chars.emit(Token.Type.INTEGER);
            }
            else if (this.chars.has(2) && Negative){
                this.match("0");
                this.match("\\.","[0-9]");
                Decimal = true;
            }
            else{
                throw new ParseException(this.chars.input, this.chars.index);
            }
        }
        boolean isnumber = true;
        while(this.chars.has(0) && isnumber){
            if(this.peek("\\.")){
                if(this.peek("\\.") && !Decimal) {
                    this.match("\\.");
                    Decimal = true;
                }
                if(!this.chars.has(0)){
                    throw new ParseException(this.chars.input, this.chars.index);
                }
            }
            if(this.peek("[0-9]")){
                this.match("[0-9]");
            }
            else {
                isnumber = false;
            }
        }
        if (Decimal){
            return chars.emit(Token.Type.DECIMAL);
        }
        else{
            return chars.emit(Token.Type.INTEGER);
        }

    }

    public Token lexCharacter() {
        this.match("'");

        if (chars.has(0)){

            // backslash case
            if(this.peek("\\\\")){
                chars.advance();
                if(this.chars.has(0)){
                    if(!this.match("b|n|r|t|'|\"")){
                        throw new ParseException(this.chars.input, this.chars.index);
                    }
                }
                else{
                    throw new ParseException(this.chars.input, this.chars.index);
                }
            }
            else{
                //make sure to not match single quote
                this.match(".");
            }
            if(chars.has(0)){
                if(this.match("'")){
                    return chars.emit((Token.Type.CHARACTER));
                }
                else{
                    throw new ParseException(this.chars.input, this.chars.index);
                }
            }
            else{
                throw new ParseException(this.chars.input, this.chars.index);
            }
        }
        return chars.emit(Token.Type.CHARACTER);
    }

    public Token lexString() {
        this.match("\"");


        while(!peek("\"")){
            if(chars.has(0)) {

                if(peek("\\\\")){
                    chars.advance();
                    if(this.chars.has(0)){
                        if(!this.match("b|n|r|t|'|\"")){
                            throw new ParseException(this.chars.input, this.chars.index);
                        }
                    }
                    else{
                        throw new ParseException(this.chars.input, this.chars.index);
                    }
                } else {
                    chars.advance();
                }
            } else {
                throw new ParseException(this.chars.input, this.chars.index);
            }


        }

        this.match("\"");
        return chars.emit((Token.Type.STRING));
    }

    public void lexEscape() {
        boolean escapeloop = true;
        while(this.chars.has(0) && escapeloop){
            if(this.peek(" |\\\b|\\\t|\\\r|\\\n|\\\\")){
                chars.advance();
                chars.skip();
            }
            else {
                escapeloop = false;
            }

        }

//        int i=0;
//        while(!this.peek(" |\\\s|\\\t|\\\r|\\\n") && i+this.tokenendindex < this.chars.input.length()){
//            chars.advance();
//            i++;
//        }
//        System.out.println("End Loop" + i + this.tokenendindex);
//        chars.length-=i;
//        chars.index-=i;
//        this.tokenendindex += i;

    }

    public Token lexOperator() {
        // equals logic
        if(this.peek("[!=]")){
            this.match("!|=");
            if(this.chars.has(0)){
               this.match("=");
            }

        }
        else if (this.match("&")){
            if (this.chars.has(0)){
                this.match("&");
            }
        }
        else if(this.match("|")){
            if (this.chars.has(0)){
                this.match("|");
            }
        }
        else {
            chars.advance();
        }


        return chars.emit(Token.Type.OPERATOR);
    }

    public Boolean escape(){
        String Escapes = " |\\\b|\\\t|\\\r|\\\n|\\\\";
        return this.peek(Escapes);
    }


    /**
     * Returns true if the next sequence of characters match the given patterns,
     * which should be a regex. For example, {@code peek("a", "b", "c")} would
     * return true if the next characters are {@code 'a', 'b', 'c'}.
     */
    public boolean peek(String... patterns) {
        for (int i =0; i < patterns.length; i++){
            if(!chars.has(i) || !String.valueOf(chars.get(i)).matches(patterns[i])){
                return false;
            }

        }

        return true;
    }

    /**
     * Returns true in the same way as {@link #peek(String...)}, but also
     * advances the character stream past all matched characters if peek returns
     * true. Hint - it's easiest to have this method simply call peek.
     */
    public boolean match(String... patterns) {
        boolean peek = peek(patterns);
        if(peek){
            for (int i = 0; i < patterns.length; i++){
                chars.advance();
            }
        }
        return peek;
    }

    /**
     * A helper class maintaining the input string, current index of the char
     * stream, and the current length of the token being matched.
     *
     * You should rely on peek/match for state management in nearly all cases.
     * The only field you need to access is {@link #index} for any {@link
     * ParseException} which is thrown.
     */
    public static final class CharStream {

        private final String input;
        private int index = 0;
        private int length = 0;

        public CharStream(String input) {
            this.input = input;
        }

        public boolean has(int offset) {
            return index + offset < input.length();
        }

        public char get(int offset) {
            return input.charAt(index + offset);
        }

        public void advance() {
            index++;
            length++;
        }

        public void skip() {
            length = 0;
        }

        public Token emit(Token.Type type) {
            int start = index - length;
            skip();
            return new Token(type, input.substring(start, index), start);
        }

    }

}
