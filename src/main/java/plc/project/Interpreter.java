package plc.project;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class Interpreter implements Ast.Visitor<Environment.PlcObject> {

    private Scope scope = new Scope(null);

    public Interpreter(Scope parent) {
        scope = new Scope(parent);
        scope.defineFunction("print", 1, args -> {
            System.out.println(args.get(0).getValue());
            return Environment.NIL;
        });
    }

    public Scope getScope() {
        return scope;
    }

    @Override
    public Environment.PlcObject visit(Ast.Source ast) {

        for (Ast.Global global : ast.getGlobals()) {
            visit(global);
        }

        for (Ast.Function function : ast.getFunctions()) {
            visit(function);
        }

        // TODO: remove try catch later(????)
        try {
            scope.lookupFunction("Main", 0);
        } catch (RuntimeException e) {
            throw new RuntimeException("Evaluation Failed - visit(Ast.Source ast) => no main");
        }

        return Environment.NIL;

    }

    @Override
    public Environment.PlcObject visit(Ast.Global ast) {
        Environment.PlcObject value = Environment.NIL;
        if (ast.getValue().isPresent()) {
            value = visit(ast.getValue().get());
        }
        scope.defineVariable(ast.getName(), ast.getMutable(), value);  // TODO: make sure getMutable actually does what we need it to do here
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Function ast) {

        scope.defineFunction(ast.getName(), ast.getParameters().size(), args -> {
            try {
                scope = new Scope(scope);
                // TODO: Need to look through params and statements

                for (int i = 0; i < args.size(); i++) {
                    scope.defineVariable(ast.getParameters().get(i), false, args.get(i));   // TODO: figure out the mutable stuff here
                }

                for (Ast.Statement statement : ast.getStatements()) {
                    visit(statement);
                }

            } catch (Return ret) {   // if return statement
                return ret.value;
            } finally {   // if no return statement
                scope = scope.getParent();
            }
            return Environment.NIL;
        });

        return Environment.NIL;

    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Expression ast) {
        visit(ast.getExpression());
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Declaration ast) {
        if (ast.getValue().isEmpty()) {
            scope.defineVariable(ast.getName(), true, Environment.NIL);
        } else {
            scope.defineVariable(ast.getName(), true, visit(ast.getValue().get()));
        }
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Assignment ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.If ast) {

        if (requireType(Boolean.class, visit(ast.getCondition())) != null) {

            try {

                scope = new Scope(scope);
                if ((Boolean) visit(ast.getCondition()).getValue()) {
                    ast.getThenStatements().forEach(this::visit);
                } else {
                    ast.getElseStatements().forEach(this::visit);
                }

            } finally {
                scope = scope.getParent();
            }
        }
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Switch ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Case ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.While ast) {
        while (requireType(Boolean.class, visit(ast.getCondition()))) {
            try {
                scope = new Scope(scope);
                ast.getStatements().forEach(this::visit);
            } finally {
                scope = scope.getParent();
            }
        }
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Return ast) {
        Environment.PlcObject value = visit(ast.getValue());
        throw new Return(value);
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Literal ast) {
        if (ast.getLiteral() == null) {
            return Environment.NIL;
        } else {
            return Environment.create(ast.getLiteral());
        }
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Group ast) {
        return (visit(ast.getExpression()));
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Binary ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Access ast) {


        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Function ast) {
        throw new UnsupportedOperationException(); //TODO


    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.PlcList ast) {

        List<Object> elements = new ArrayList<>();
        for(Ast.Expression expr : ast.getValues()){
            elements.add(visit(expr).getValue());
        }
        return Environment.create(elements);

    }

    /**
     * Helper function to ensure an object is of the appropriate type.
     */
    private static <T> T requireType(Class<T> type, Environment.PlcObject object) {
        if (type.isInstance(object.getValue())) {
            return type.cast(object.getValue());
        } else {
            throw new RuntimeException("Expected type " + type.getName() + ", received " + object.getValue().getClass().getName() + ".");
        }
    }

    /**
     * Exception class for returning values.
     */
    public static class Return extends RuntimeException {

        private final Environment.PlcObject value;

        private Return(Environment.PlcObject value) {
            this.value = value;
        }

    }

}
