package plc.project;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class Interpreter implements Ast.Visitor<Environment.PlcObject> {

    private Scope scope = new Scope(null);

    public Interpreter(Scope parent) {
        scope = new Scope(parent);
        scope.defineFunction("print", 1, args -> {
//            System.out.println("TEST: " + args.get(0).getValue());
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
            System.out.println("visiting function " + function.getName());
            visit(function);
        }

        // TODO: remove try catch later(????)
        try {
            scope.lookupFunction("main", 0);
        } catch (RuntimeException e) {
            throw new RuntimeException("Evaluation Failed - visit(Ast.Source ast) => no main");
        }

        List<Environment.PlcObject> args = new ArrayList<>();

        return scope.lookupFunction("main", 0).invoke(args);

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
        Ast.Expression AccessReciever = ast.getReceiver();

        if(AccessReciever instanceof Ast.Expression.Access){
            if(((Ast.Expression.Access) AccessReciever).getOffset().isEmpty() ){
                Environment.Variable variable = scope.lookupVariable(((Ast.Expression.Access) AccessReciever).getName());
                if(variable.getMutable()){
                    variable.setValue(visit(ast.getValue()));
                }
                else {
                    throw new RuntimeException("Variable is not mutable");
                }
            }
            else{
                Environment.PlcObject offsetVal = visit(((Ast.Expression.Access) AccessReciever).getOffset().get());
                Environment.Variable variable = scope.lookupVariable(((Ast.Expression.Access) AccessReciever).getName());
                if(variable.getMutable()){

                   // System.out.println(variable.toString());
                    //System.out.println(variable.getValue().toString());
                   // System.out.println(variable.getValue().getValue().toString());
                   // System.out.println(offsetVal.getValue().toString());
                   // System.out.println(((List<Object>) variable.getValue().getValue()).set(((BigInteger)offsetVal.getValue()).intValue(),visit(ast.getValue()).getValue()));
                   // System.out.println(variable.getValue().getValue().toString());

                    ((List<Object>) variable.getValue().getValue()).set(((BigInteger)offsetVal.getValue()).intValue(),visit(ast.getValue()).getValue());

                }
                else {
                    throw new RuntimeException("Variable is not mutable");
                }

            }

        }


        return Environment.NIL;
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
        try {
            scope = new Scope(scope);
            Environment.PlcObject condition = visit(ast.getCondition());
            for (Ast.Statement.Case c : ast.getCases()) {
                if (c.getValue().isPresent()) {

                    if (condition.getValue().equals(visit(c.getValue().get()).getValue())) {
                        visit(c);
                        break;
                    }
                } else {
                    visit(c);
                }
            }

        } finally {
//            System.out.println("FInally -- switch");
            scope = scope.getParent();
        }
//        throw new UnsupportedOperationException(); //TODO
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Case ast) {
//        System.out.println("Looking at case");
        try{
            scope = new Scope(scope);
            ast.getStatements().forEach(this::visit);

        } finally{
            scope = scope.getParent();
        }
        return Environment.NIL;
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
        // get left side PlcObject from this Binary
        Environment.PlcObject LeftSide = visit(ast.getLeft());
        String operator = ast.getOperator();
        boolean IsInteger = false;
        boolean IsDecimal = false;
        //Boolean or and special null case
        if(operator.equals("||")){
            if(LeftSide.getValue() instanceof Boolean){
                if(requireType(Boolean.class, LeftSide)) {
                    return Environment.create(true);
                }
                else if (requireType(Boolean.class, visit(ast.getRight()))) {
                    return Environment.create(true);
                }
                else{
                    return Environment.create(false);
                }
            }
        }

        Environment.PlcObject RightSide = visit(ast.getRight());
        switch(operator){
            // Boolean and
            case "&&":
                if(LeftSide.getValue() instanceof Boolean){
                    if(requireType(Boolean.class, LeftSide) && requireType(Boolean.class, RightSide)){
                        return Environment.create(true);
                    }
                    else{
                        return Environment.create(false);
                    }
                }
                else{
                    throw new RuntimeException("Not a Boolean to && or ||");
                }
            //Multiplications
            case "*","/","^","-":
                if(LeftSide.getValue() instanceof BigInteger){
                    IsInteger = true;
                } else if (LeftSide.getValue() instanceof BigDecimal) {
                    IsDecimal = true;
                }
                switch (operator){
                    case "*":
                        if(IsInteger){
                            return Environment.create(((BigInteger)LeftSide.getValue()).multiply(requireType(BigInteger.class, RightSide)));
                        }
                        else if(IsDecimal) {
                            return Environment.create(((BigDecimal)LeftSide.getValue()).multiply(requireType(BigDecimal.class, RightSide)));
                        }
                        else{
                            throw new RuntimeException("Not a BigDecimal or BigInteger to multiply");
                        }

                    case "/":
                        if(IsInteger){
                            return Environment.create(((BigInteger)LeftSide.getValue()).divide(requireType(BigInteger.class, RightSide)));
                        }
                        else if(IsDecimal) {
                            if (RightSide.getValue() == BigDecimal.valueOf(0)) {
                                throw new RuntimeException("Cannot divide by 0");
                            } else {
                                return Environment.create(((BigDecimal)LeftSide.getValue()).divide(requireType(BigDecimal.class, RightSide), RoundingMode.HALF_EVEN));
                            }
                        }
                        else{
                            throw new RuntimeException("Not a BigDecimal or BigInteger to divide");
                        }

                    case "^":
                        if(IsInteger){
                            if(RightSide.getValue() instanceof BigInteger){
                                int Base =  ((BigInteger) LeftSide.getValue()).intValue();
                                int Exponent = ((BigInteger) RightSide.getValue()).intValue();
                                return Environment.create(BigInteger.valueOf((int)Math.pow(Base,Exponent)));
                            }
                            else {
                                throw new RuntimeException("Exponent is not a Big Integer");
                            }
                        }
                        else{
                            throw new RuntimeException("Base is not a BigInteger");
                        }
                    case "-":
                        if(IsInteger){
                            return Environment.create(((BigInteger)LeftSide.getValue()).subtract(requireType(BigInteger.class, RightSide)));
                        }
                        else if(IsDecimal) {
                            return Environment.create(((BigDecimal)LeftSide.getValue()).subtract(requireType(BigDecimal.class, RightSide)));
                        }
                        else{
                            throw new RuntimeException("Not a BigDecimal or BigInteger to subtract");
                        }
                }

            //addition
            case "+":
                boolean RightString = false;
                boolean LeftString = false;
                if(LeftSide.getValue() instanceof String){
                    LeftString = true;
                }
                else if (RightSide.getValue() instanceof String) {
                    RightString = true;
                }
                //concatenation
                if(RightString || LeftString){
                   return Environment.create(LeftSide.getValue().toString() + RightSide.getValue().toString());
                }

                if(LeftSide.getValue() instanceof BigInteger){
                    IsInteger = true;
                } else if (LeftSide.getValue() instanceof BigDecimal) {
                    IsDecimal = true;
                }


                if(IsInteger){
                    return Environment.create(((BigInteger)LeftSide.getValue()).add(requireType(BigInteger.class, RightSide)));
                }
                else if(IsDecimal) {
                    return Environment.create(((BigDecimal)LeftSide.getValue()).add(requireType(BigDecimal.class, RightSide)));
                }
                else{
                    throw new RuntimeException("Not a BigDecimal or BigInteger to add");
                }
            //Comparables
            case ">","<":
                if (LeftSide.getValue() instanceof Comparable){
                    if(operator.equals(">")){
                        if(((Comparable) LeftSide.getValue()).compareTo(((Comparable)RightSide.getValue())) > 0){
                            return Environment.create(true);
                        }
                        else if (((Comparable) LeftSide.getValue()).compareTo(((Comparable)RightSide.getValue())) <= 0) {
                            return Environment.create(false);
                        }
                        throw new RuntimeException("Not a Comparable to > or <");
                    }
                    else if(operator.equals("<")){
                        if(((Comparable) LeftSide.getValue()).compareTo(((Comparable)RightSide.getValue())) >= 0){
                            return Environment.create(false);
                        }
                        else if (((Comparable) LeftSide.getValue()).compareTo(((Comparable)RightSide.getValue())) < 0) {
                            return Environment.create(true);
                        }
                        throw new RuntimeException("Not a Comparable to > or <");
                    }
                }
                else{
                    throw new RuntimeException("Not a Comparable to > or <");
                }
            //equals
            case "==":
                return Environment.create(Objects.equals(LeftSide, RightSide));
            //not equals
            case"!=":
                return Environment.create(Objects.equals(LeftSide, RightSide));

        }
        throw new RuntimeException();
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Access ast) {
        Environment.Variable var = scope.lookupVariable(ast.getName());
        if(ast.getOffset().isEmpty()){
            return scope.lookupVariable(ast.getName()).getValue();
        }
        else{
            Environment.PlcObject offsetVal = visit(ast.getOffset().get());
            List<Object> list = (List<Object>) var.getValue().getValue();
            if(!(offsetVal.getValue() instanceof BigInteger)){
                throw new RuntimeException("Offset needs to be a BigInteger");
            }
            int i = ((BigInteger)offsetVal.getValue()).intValue();
            if(i >= list.size() || i < 0){
                throw new RuntimeException("PlcList out of bounds");
            }
            return Environment.create(list.get(i));
        }
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Function ast) {

        Environment.Function function = scope.lookupFunction(ast.getName(), ast.getArguments().size());
        List<Environment.PlcObject> args = new ArrayList<>();

        // convert arguments to Environment.PlcObject
        for (Ast.Expression arg : ast.getArguments()) {
            args.add(visit(arg));
        }

        return function.invoke(args);
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
