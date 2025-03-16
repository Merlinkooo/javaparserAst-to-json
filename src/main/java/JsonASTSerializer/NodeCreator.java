package JsonASTSerializer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

//Class for creating syntetic JSON representation of nodes,which don't exist in JavaParser AST,but are required
//in ASTFRI
public class NodeCreator {
    ObjectMapper mapper;

    public NodeCreator(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public String createFullName(ClassOrInterfaceType type){
        StringBuilder builder = new StringBuilder();


        builder.append(type.getName().asString());

        var typeArguments = type.getTypeArguments();
        if (typeArguments.isEmpty() || typeArguments.get().size() == 0) return builder.toString();


        AtomicInteger i= new AtomicInteger(0);
        builder.append("<");
        typeArguments.get().forEach(extendedType ->{
            builder.append(createFullName((ClassOrInterfaceType) extendedType));
            if(i.get() == typeArguments.get().size() - 1) {
                builder.append(">");

            }else{
                builder.append(",");
            }
            i.incrementAndGet();
        });

        return builder.toString();

    }

    public ObjectNode createCompoundStmt(List<Statement> statements,JsonAstSerialiser serialiser){
        ObjectNode compoundStmt = this.mapper.createObjectNode();
        compoundStmt.put("node","CompoundStmt");

        ArrayNode statementsJson = this.mapper.createArrayNode();
        compoundStmt.set("statements",statementsJson);

        statements.forEach(statement -> {
            statementsJson.add(statement.accept(serialiser,null));
        });
        return compoundStmt;
    }

    public ObjectNode createUnknownExpression(){
        ObjectNode unknownExpr = this.mapper.createObjectNode();
        unknownExpr.put("node","UnknownExpr");
        return unknownExpr;
    }

    public ObjectNode createExpressionStmt(ObjectNode expression){
        ObjectNode binOpExpressionStmt  = this.mapper.createObjectNode();
        binOpExpressionStmt.put("node","ExpressionStmt");
        binOpExpressionStmt.set("expression",expression);
        return binOpExpressionStmt;
    }

    public ObjectNode createBinOpExpr(List<Expression> expressions, String operator,
                                      int indexOfExpression, JsonAstSerialiser visitor){
        if (expressions.size() == 0) throw new RuntimeException("Empty list of expressions");
        if (indexOfExpression >= expressions.size()) throw new IndexOutOfBoundsException();
        if (indexOfExpression == expressions.size() - 1) return (ObjectNode) expressions.get(indexOfExpression).accept(visitor,null);
        ObjectNode binOpExpr = this.mapper.createObjectNode();
        binOpExpr.put("node","BinOpExpr");


        binOpExpr.set("left",expressions.get(indexOfExpression).accept(visitor,null));

        binOpExpr.put("operator",operator);
        binOpExpr.put("right",this.createBinOpExpr(expressions,operator,
                ++indexOfExpression,visitor));

        return binOpExpr;
    }
}
