package plc.project;

import java.io.PrintWriter;

public final class Generator implements Ast.Visitor<Void> {

    private final PrintWriter writer;
    private int indent = 0;

    public Generator(PrintWriter writer) {
        this.writer = writer;
    }

    private void print(Object... objects) {
        for (Object object : objects) {
            if (object instanceof Ast) {
                visit((Ast) object);
            } else {
                writer.write(object.toString());
            }
        }
    }

    private void newline(int indent) {
        writer.println();
        for (int i = 0; i < indent; i++) {
            writer.write("    ");
        }
    }

    @Override
    public Void visit(Ast.Source ast) {

        print("public class Main {");
        newline(indent);
        newline(++indent);

        for(Ast.Global global : ast.getGlobals()){
            print(global);
            newline(indent);
        }

        // public static void main
        print("public static void main(String[] args) {");
        newline(++indent);
        print("System.exit(new Main().main());");
        newline(--indent);
        print("}");
        newline(--indent);

        // other functions
        for(Ast.Function function: ast.getFunctions()){
            newline(++indent);
            print(function);
            newline(--indent);
        }

        newline(indent);


        //end
        print("}");
        return null;


//        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Void visit(Ast.Global ast) {

        print(Environment.getType(ast.getTypeName()).getJvmName());
        print(" ", ast.getName());

        if(ast.getValue().isPresent()){
            print(" = ");
            print(ast.getValue().get());
        }
        print(";");


        return null;
//        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Void visit(Ast.Function ast) {

        if(ast.getReturnTypeName().isPresent()){
            print(Environment.getType(ast.getReturnTypeName().get()).getJvmName());
        }

        print(" ", ast.getName(), "(");
        int numParameters = ast.getParameters().size();
        int numParameterTypes = ast.getParameterTypeNames().size();

        if (numParameters == numParameterTypes && numParameters > 0) {

        }
        print(") ", "{");

        if(!ast.getStatements().isEmpty()){
            indent++;
            for(Ast.Statement statement : ast.getStatements()){
                newline(indent);
                print(statement);
            }
            newline(--indent);
        }


        print("}");
        return null;
//        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Void visit(Ast.Statement.Expression ast) {

        print(ast.getExpression());
        print(";");
        return null;
        //throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Void visit(Ast.Statement.Declaration ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Void visit(Ast.Statement.Assignment ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Void visit(Ast.Statement.If ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Void visit(Ast.Statement.Switch ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Void visit(Ast.Statement.Case ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Void visit(Ast.Statement.While ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Void visit(Ast.Statement.Return ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Void visit(Ast.Expression.Literal ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Void visit(Ast.Expression.Group ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Void visit(Ast.Expression.Binary ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Void visit(Ast.Expression.Access ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Void visit(Ast.Expression.Function ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Void visit(Ast.Expression.PlcList ast) {
        throw new UnsupportedOperationException(); //TODO
    }

}
