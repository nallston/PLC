package plc.project;

public class Tester {
    public static void main(String[] args){
        String str = "let this";
        Lexer testing = new Lexer(str);


        System.out.println(testing.peek("(@|[A-Za-z])[A-Za-z0-9_-]*"));
        testing = new Lexer("5683");
        System.out.println(testing.lexToken());

    }
}
