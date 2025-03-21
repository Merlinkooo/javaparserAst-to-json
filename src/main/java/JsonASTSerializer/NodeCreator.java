package JsonASTSerializer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.javaparser.ast.ArrayCreationLevel;
import com.github.javaparser.ast.expr.ArrayCreationExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.type.ArrayType;
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

    public ObjectNode createIndirectionTypeFromArrayType(ArrayType arrayType,JsonAstSerialiser serialiser,int level){
        ObjectNode indirectionTypeJson = mapper.createObjectNode();
        if(level == 1){
            indirectionTypeJson.put("node","IndirectionType");
            indirectionTypeJson.set("indirect",arrayType.getElementType() instanceof ClassOrInterfaceType ?
                    serialiser.visit(arrayType.getElementType(),null).get("indirect"):
                    serialiser.visit(arrayType.getElementType(),null));
            return indirectionTypeJson;
        }



        indirectionTypeJson.put("node","IndirectionType");
        indirectionTypeJson.put("indirect",
                this.createIndirectionTypeFromArrayType(arrayType,serialiser,--level));

        return indirectionTypeJson;
    }

    public String createFullName(ClassOrInterfaceType type){
        StringBuilder builder = new StringBuilder();

        try {
            builder.append(type.resolve().describe());
            return builder.toString();
        } catch (Exception e) {
            builder.append(type.getName().asString());
        }

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
                                      int indexOfExpression, JsonAstSerialiser visitor) {
        if (expressions.size() == 0) return null;
        if (indexOfExpression >= expressions.size()) throw new IndexOutOfBoundsException();
        if (indexOfExpression == expressions.size() - 1)
            return (ObjectNode) expressions.get(indexOfExpression).accept(visitor, null);
        ObjectNode binOpExpr = this.mapper.createObjectNode();
        binOpExpr.put("node", "BinOpExpr");


        binOpExpr.set("left", expressions.get(indexOfExpression).accept(visitor, null));

        binOpExpr.put("operator", operator);
        binOpExpr.put("right", this.createBinOpExpr(expressions, operator,
                ++indexOfExpression, visitor));

        return binOpExpr;
    }
    ObjectNode createNewExpr(ObjectNode constructorCallExprJson){
        ObjectNode newExpr = this.mapper.createObjectNode();
        newExpr.put("node","NewExpr");
        newExpr.set("init",constructorCallExprJson);
        return newExpr;
    }

        public ObjectNode createNewExprFromArrayCreationExpr(ArrayCreationExpr expr,JsonAstSerialiser serialiser){

            //ArrayCreationLevel in case int[4][5] : 1. = 4; 2.=5
            int dimensions = expr.getLevels().size();

            ObjectNode lastIndirectionType;
            if (expr.getElementType() instanceof ClassOrInterfaceType){
                 lastIndirectionType = this.createIndirectionType((ObjectNode) expr.getElementType().accept(serialiser, null).get("indirect"));
            }else {
                 lastIndirectionType = this.createIndirectionType((ObjectNode) expr.getElementType().accept(serialiser, null));
            }

            ObjectNode lastNewExpr = this.createNewExpr(
                    this.createConstructorCallExpr(lastIndirectionType,mapper.createArrayNode()));

            for (int i = 0; i <dimensions - 1 ; i++) {
                lastIndirectionType = this.createIndirectionType(lastIndirectionType);
                lastNewExpr =  createNewExpr(this.createConstructorCallExpr(lastIndirectionType,
                        mapper.createArrayNode().add(lastNewExpr)));
            }
            return lastNewExpr;
    }

        public ObjectNode createIndirectionType(ObjectNode nodeType){
            ObjectNode indirectionTypeJson = this.mapper.createObjectNode();
            indirectionTypeJson.put("node","IndirectionType");
            indirectionTypeJson.set("indirect",nodeType);
            return indirectionTypeJson;
        }

        public ObjectNode createConstructorCallExpr(ObjectNode typeNode,ArrayNode arguments){
            ObjectNode constructorCallExpr = this.mapper.createObjectNode();
            constructorCallExpr.put("node","ConstructorCallExpr");
            constructorCallExpr.set("type",typeNode);
            constructorCallExpr.set("arguments",arguments);
            return constructorCallExpr;
        }

}
