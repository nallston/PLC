package plc.project;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * See the specification for information about what the different visit
 * methods should do.
 */
public final class Analyzer implements Ast.Visitor<Void> {

    public Scope scope;
    private Ast.Function function;

    public Analyzer(Scope parent) {
        scope = new Scope(parent);
        scope.defineFunction("print", "System.out.println", Arrays.asList(Environment.Type.ANY), Environment.Type.NIL, args -> Environment.NIL);
    }

    public Scope getScope() {
        return scope;
    }

    @Override
    public Void visit(Ast.Source ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Global ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Function ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Statement.Expression ast) {
        visit(ast.getExpression());
        return null;
        // TODO Might be wrong... something about having to be a Ast.Expression.Function
    }

    @Override
    public Void visit(Ast.Statement.Declaration ast) {
        Environment.Type type = null;
        if(ast.getTypeName().isPresent() || ast.getValue().isPresent()){
            if(ast.getTypeName().isPresent()){
                type = Environment.getType(ast.getTypeName().get());
            }
            if ( ast.getValue().isPresent()) {
                visit(ast.getValue().get());
                if (type == null){
                    type = ast.getValue().get().getType();
                }
                requireAssignable(type, ast.getValue().get().getType());
            }
            Environment.Variable variable = scope.defineVariable(ast.getName(), ast.getName(), type, true, Environment.NIL);
            ast.setVariable(variable);
        }
        else{
            throw new RuntimeException("Declaration must have Type or Value");
        }
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Assignment ast) {
        if(ast.getReceiver() instanceof Ast.Expression.Access){
            visit(ast.getReceiver());
            visit(ast.getValue());
            requireAssignable(ast.getReceiver().getType(), ast.getValue().getType());
        } else {
            throw new RuntimeException("Expected Ast.Expression.Access expression");
        }
        return null;
    }

    @Override
    public Void visit(Ast.Statement.If ast) {
        visit(ast.getCondition());
        requireAssignable(Environment.Type.BOOLEAN, ast.getCondition().getType());
        if(ast.getThenStatements().isEmpty()){
            throw new RuntimeException("No then statement in if");
        }
        //then
        try{
            scope = new Scope(scope);
            for(Ast.Statement stmt : ast.getThenStatements()){
                visit(stmt);
            }
            System.out.println("true");
        }
        finally{
            scope = scope.getParent();
        }
        //else
        try{
            scope = new Scope(scope);
            for(Ast.Statement stmt : ast.getElseStatements()){
                visit(stmt);
            }
            System.out.println("false");
        }
        finally{
            scope = scope.getParent();
        }


//
//
//        if(ast.getCondition() instanceof Ast.Expression.Literal){
//            if((Boolean)((Ast.Expression.Literal) ast.getCondition()).getLiteral()){
//                try{
//                    scope = new Scope(scope);
//                    for(Ast.Statement stmt : ast.getThenStatements()){
//                        visit(stmt);
//                    }
//                    System.out.println("true");
//                }
//                finally{
//                    scope = scope.getParent();
//                }
//            }
//            else{
//                try{
//                    scope = new Scope(scope);
//                    for(Ast.Statement stmt : ast.getElseStatements()){
//                        visit(stmt);
//                    }
//                    System.out.println("false");
//                }
//                finally{
//                    scope = scope.getParent();
//                }
//
//            }
       // }
        return null;  // TODO
    }

    @Override
    public Void visit(Ast.Statement.Switch ast) {
        //Case Types must match switch condition Type
        //Last case is Default and doesn't contain a condition
        visit(ast.getCondition());
        try{
            scope = new Scope(scope);
            int i = 0;
            for(Ast.Statement.Case cases : ast.getCases()){
                i++;
                if(i == ast.getCases().size()){
                    if(cases.getValue().isEmpty()){
                        visit(cases);
                    }
                    else{
                        throw new RuntimeException("Final case must be Default with no Value");
                    }
                }
                else if(cases.getValue().isPresent()){
                    visit(cases);
                    requireAssignable(ast.getCondition().getType(), cases.getValue().get().getType());
                }
                else{
                    throw new RuntimeException("Case does not have a Value");
                }
            }
        }
        finally {
            scope = scope.getParent();
        }
        return null;  // TODO Review after Function
    }

    @Override
    public Void visit(Ast.Statement.Case ast) {
        //TODO Review after Function.
        //Validates Case, scope added
        try{
            scope = new Scope(scope);
            if(ast.getValue().isPresent()){
                for(Ast.Statement stmt : ast.getStatements()){
                    visit(stmt);
                }
            }
            else{
                for(Ast.Statement stmt : ast.getStatements()){
                    visit(stmt);
                }
            }
        }
        finally{
            scope = scope.getParent();
        }
        return null;
    }

    @Override
    public Void visit(Ast.Statement.While ast) {
        //make sure ast.getCondition() is set up to grab type
        visit(ast.getCondition());
        requireAssignable(Environment.Type.BOOLEAN, ast.getCondition().getType());
        try{
            scope = new Scope(scope);
            for(Ast.Statement stmt : ast.getStatements()){
                visit(stmt);
            }
        }
        finally{
            scope = scope.getParent();
        }
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Return ast) {
        if(ast.getValue().getType() != function.getFunction().getReturnType()){
            throw new RuntimeException("Invalid return type");
        }
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Literal ast) {
        Object literal = ast.getLiteral();
        if(literal == null){ ast.setType(Environment.Type.NIL); }
        if(literal instanceof Character){ ast.setType(Environment.Type.CHARACTER); }
        if(literal instanceof String){ ast.setType(Environment.Type.STRING); }
        if(literal instanceof Boolean){ ast.setType(Environment.Type.BOOLEAN); }
        if(literal instanceof BigInteger){
            BigInteger number = (BigInteger) literal;
            if(number.compareTo(BigInteger.valueOf(Integer.MAX_VALUE)) <= 0 && number.compareTo(BigInteger.valueOf(Integer.MIN_VALUE)) >= 0){
                ast.setType(Environment.Type.INTEGER);
            }
            else{
                throw new RuntimeException("Not valid BigInteger value");
            }
        }
        if(literal instanceof BigDecimal){
            BigDecimal number = (BigDecimal) literal;
            if(number.compareTo(BigDecimal.valueOf(Double.MAX_VALUE)) <= 0 && number.compareTo(BigDecimal.valueOf(Double.MIN_VALUE)) >= 0){
                ast.setType(Environment.Type.DECIMAL);
            }
            else{
                throw new RuntimeException("Not a valid BigDecimal value");
            }
        }
       return null;
    }

    @Override
    public Void visit(Ast.Expression.Group ast) {
        if(ast.getExpression() instanceof Ast.Expression.Group){
            visit(ast.getExpression());
            ast.setType(ast.getExpression().getType());
        } else {
            throw new RuntimeException("Expected Ast.Expression.Binary ==> [visit(Ast.Expression.Group ast)]");
        }
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Binary ast) {
        visit(ast.getLeft());
        visit(ast.getRight());
        switch(ast.getOperator()){
            case "&&", "||":
                // && || Boolean only
                requireAssignable(Environment.Type.BOOLEAN, ast.getLeft().getType());
                requireAssignable(Environment.Type.BOOLEAN, ast.getRight().getType());
                ast.setType(Environment.Type.BOOLEAN);
                return null;
            case "<",">","<=",">=":
                //< > >= <= Comparables, result is Boolean
                requireAssignable(Environment.Type.COMPARABLE, ast.getLeft().getType());
                requireAssignable(Environment.Type.COMPARABLE, ast.getRight().getType());
                ast.setType(Environment.Type.BOOLEAN);
                return null;
            case "+":
                // + If either side is a String result is a String, other side is any.
                if((ast.getLeft().getType().equals(Environment.Type.STRING)) || (ast.getRight().getType().equals(Environment.Type.STRING))){
                    requireAssignable(Environment.Type.ANY, ast.getLeft().getType());
                    requireAssignable(Environment.Type.ANY, ast.getRight().getType());
                    ast.setType(Environment.Type.STRING);
                }
                // LHS is Integer or Decimal, RHS & Result is same.
                else if(ast.getLeft().getType().equals(Environment.Type.INTEGER)){
                    requireAssignable(Environment.Type.INTEGER,ast.getRight().getType());
                    ast.setType(Environment.Type.INTEGER);
                }
                else if(ast.getLeft().getType().equals(Environment.Type.DECIMAL)){
                    requireAssignable(Environment.Type.DECIMAL, ast.getRight().getType());
                    ast.setType(Environment.Type.DECIMAL);
                }
                else{
                    throw new RuntimeException("Invalid use of +");
                }
                return null;
            case "-","*","/":
                //- * / LHS must be Integer or Decimal. RHS & Result are the same as Left
                if(ast.getLeft().getType().equals(Environment.Type.INTEGER)){
                    requireAssignable(Environment.Type.INTEGER,ast.getRight().getType());
                    ast.setType(Environment.Type.INTEGER);
                }
                else if(ast.getLeft().getType().equals(Environment.Type.DECIMAL)){
                    requireAssignable(Environment.Type.DECIMAL, ast.getRight().getType());
                    ast.setType(Environment.Type.DECIMAL);
                }
                else{
                    throw new RuntimeException("LHS must be Integer or Decimal.");
                }
                return null;
            case "^":
                //^ Both must be Integer. Result is Integer
                requireAssignable(Environment.Type.INTEGER, ast.getLeft().getType());
                requireAssignable(Environment.Type.INTEGER, ast.getRight().getType());
                ast.setType(Environment.Type.INTEGER);
                return null;
        }
        return null;  // TODO
    }

    @Override
    public Void visit(Ast.Expression.Access ast) {
        if(ast.getOffset().isPresent()){
            Ast.Expression expr = ast.getOffset().get();
            visit(expr);
            ast.setVariable(expr.getType().getGlobal(ast.getName()));
        }
        else{
            ast.setVariable(scope.lookupVariable(ast.getName()));
        }
        return null;
        // TODO
    }

    @Override
    public Void visit(Ast.Expression.Function ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Expression.PlcList ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    public static void requireAssignable(Environment.Type target, Environment.Type type) {
        if(target.getName().equals(Environment.Type.ANY.getName())) {
        }
        else if (target.getName().equals(Environment.Type.COMPARABLE.getName())) {
            if(type.getName().equals(Environment.Type.INTEGER.getName()) || type.getName().equals(Environment.Type.DECIMAL.getName()) || type.getName().equals(Environment.Type.CHARACTER.getName()) || type.getName().equals(Environment.Type.STRING.getName())){
            }
            else{
                throw new RuntimeException("Expected type " + target.getName() + ", recieved " + type.getName() + ".");
            }
        }
        else if(target.getName().equals(type.getName())){
        }
        else{
            throw new RuntimeException("Expected type " + target.getName() + ", recieved " + type.getName() + ".");
        }
    }
}
