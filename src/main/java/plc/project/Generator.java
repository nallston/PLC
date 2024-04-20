package plc.project;

import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.BigInteger;

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

            visit(global);
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
            visit(function);
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
        if(ast.getValue().get() instanceof Ast.Expression.PlcList){
            print("[]");
        }
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
            for(int i = 0; i < ast.getParameters().size() - 1; i++){
                String currentType = Environment.getType(ast.getParameterTypeNames().get(i)).getJvmName();
                print(currentType, " ", ast.getParameters().get(i), ", ");
            }
            int lastArgIndex = ast.getParameters().size() - 1;
            print(Environment.getType(ast.getParameterTypeNames().get(lastArgIndex)).getJvmName(), " ", ast.getParameters().get(lastArgIndex));
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
    }

    @Override
    public Void visit(Ast.Statement.Declaration ast) {
        if(!ast.getVariable().getMutable()){
            print("final ");
        }
        if(ast.getTypeName().isPresent()){
            print(Environment.getType(ast.getTypeName().get()).getJvmName());

            print(" " + ast.getVariable().getJvmName());
        }
        else if(ast.getValue().isPresent()){
           print(ast.getValue().get().getType().getJvmName() + " ");
           print(ast.getVariable().getJvmName());
           print(" = ");
//           visit(ast.getValue().get());
            print(ast.getValue().get());
        }
        print(";");
        return null;
        //TODO check more and PLCLISTS?
    }

    @Override
    public Void visit(Ast.Statement.Assignment ast) {
        visit(ast.getReceiver());
        print(" = ");
        visit(ast.getValue());
        print(";");
        return null;
    }

    @Override
    public Void visit(Ast.Statement.If ast) {
        print("if (");
        visit(ast.getCondition());
        print(") {");
        newline(++indent);
        ast.getThenStatements().forEach(this::visit);
        if(!ast.getElseStatements().isEmpty()){
            newline(--indent);
            print("} else {");
            newline(++indent);
            ast.getElseStatements().forEach(this::visit);
        }
        newline(--indent);
        print("}");
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Switch ast) {

        print("switch (", ast.getCondition(), ") {");
        newline(++indent);

        for(int i = 0; i < ast.getCases().size(); i++){
//            print("case ", ast.getCases().get(i).getValue(), );
            if(ast.getCases().get(i).getValue().isEmpty()){
                print("default:", ast.getCases().get(i));
            } else {
                print("case ", ast.getCases().get(i).getValue().get(), ":", ast.getCases().get(i));
            }
        }

//        print("default:");
//        print(ast.getCases().getLast());

//        throw new UnsupportedOperationException(); //TODO
        --indent;
        print("}");
        return null;
    }


    @Override
    public Void visit(Ast.Statement.Case ast) {

        newline(++indent);

//        for(Ast.Statement statement : ast.getStatements()){
//            print(statement);
////            newline(indent);
//        }

        for(int i = 0; i < ast.getStatements().size(); i++){
            print(ast.getStatements().get(i));
            if(i != ast.getStatements().size()-1){
                newline(indent);
            }
        }

        if(ast.getValue().isPresent()){
            newline(indent);
            print("break;");
        } else {
            --indent;
        }

        newline(--indent);
//        throw new UnsupportedOperationException(); //TODO
        return null;
    }

    @Override
    public Void visit(Ast.Statement.While ast) {
//        throw new UnsupportedOperationException(); //TODO
        print("while (", ast.getCondition(), ") {");
        if(ast.getStatements().isEmpty()){
            print(" }");
            return null;
        } else {
            newline(++indent);
            int size = ast.getStatements().size();
            for(int i = 0; i < size; i++){
                print(ast.getStatements().get(i));
                if(i < size - 1){
                    newline(indent);
                }
            }

            newline(--indent);
            print("}");
            return null;
        }

    }

    @Override
    public Void visit(Ast.Statement.Return ast) {
        print("return ");
        visit(ast.getValue());
        print(";");
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Literal ast) {
        String type = ast.getType().getJvmName();
        Object literal;
        if(type.equals("boolean")){
            literal = Boolean.valueOf(ast.getLiteral().toString());
            print(literal);
        }
        else if(type.equals("int")){
            literal = Integer.valueOf(ast.getLiteral().toString());
            print(literal);
        }
        else if(type.equals("double")){
            //TODO fix decimal precision
            literal = ast.getLiteral().toString();
            print(literal);
        }
        else if(type.equals("char")){
            literal = ast.getLiteral().toString();
            print("'" + literal + "'");
        }
        else if(type.equals("String")){
            literal = ast.getLiteral().toString();
            print("\"" + literal.toString() + "\"");
        }
       return null;
    }

    @Override
    public Void visit(Ast.Expression.Group ast) {
        print("(");
        print(ast.getExpression());
        print(")");
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Binary ast) {
        if(ast.getOperator().equals("^")){
            print("Math.pow(");
            visit(ast.getLeft());
            print(", ");
            visit(ast.getRight());
            print(")");
        }
        else{
            visit(ast.getLeft());
            print(" " + ast.getOperator() + " ");
            visit(ast.getRight());
        }
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Access ast) {
        if(ast.getOffset().isPresent()){
            print(ast.getVariable().getJvmName());
            print("[");
            visit(ast.getOffset().get());
            print("]");
        }
        else{
            print(ast.getVariable().getJvmName());
        }
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Function ast) {
        Environment.Function function = ast.getFunction();

        print(function.getJvmName(), "(");

        if(!ast.getArguments().isEmpty()){
            for(int i = 0; i < ast.getArguments().size() - 1; i++){
                print(ast.getArguments().get(i), ", ");
            }
            print(ast.getArguments().getLast());
        }

        print(")");
//        print(";");



        return null;

//        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Void visit(Ast.Expression.PlcList ast) {

        print("{");
        for(int i = 0; i < ast.getValues().size()-1; i++){
            print(ast.getValues().get(i), ", " );
        }
        print(ast.getValues().getLast(), "}");
//        throw new UnsupportedOperationException(); //TODO
        return null;
    }

}
