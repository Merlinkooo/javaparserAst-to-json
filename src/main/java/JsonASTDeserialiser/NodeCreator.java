package JsonASTDeserialiser;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.type.ClassOrInterfaceType;

import java.util.List;

//Class for creating syntetic JSON representation of nodes,which don't exist in JavaParser AST,but are required
//in ASTFRI
public class NodeCreator {
    ObjectMapper mapper;

    public NodeCreator(ObjectMapper mapper) {
        this.mapper = mapper;
    }


    public ObjectNode createClassDefStmt(ClassOrInterfaceType type){
        ObjectNode classDefStmtJson =  this.mapper.createObjectNode();
        classDefStmtJson.put("node","ClassDefStmt");
        classDefStmtJson.put("name",type.getName().asString());
        classDefStmtJson.set("attributes",this.mapper.createArrayNode());
        classDefStmtJson.set("constructors",this.mapper.createArrayNode());
        classDefStmtJson.set("destructors",this.mapper.createArrayNode());
        classDefStmtJson.set("methods",this.mapper.createArrayNode());

        ArrayNode genericParameters = this.mapper.createArrayNode();
        classDefStmtJson.set("generic_parameters",genericParameters);
        //we can get Generic types from classOrInterfaceTypes so we can create list of this generic types as
        //generic parameters
        var typeArguments = type.getTypeArguments();
        typeArguments.ifPresent(types -> types.forEach(genericType -> {
            ObjectNode genericParam = this.mapper.createObjectNode();
            genericParam.put("node", "GenericParam");
            genericParam.put("name", "T");
            //we dont know if there is some constraint in original Class declaration,so we give  null to
            //constraint property
            genericParam.set("constraint", null);
            genericParameters.add(genericParam);
        }));
        classDefStmtJson.set("interfaces",this.mapper.createArrayNode());
        classDefStmtJson.set("bases",this.mapper.createObjectNode());
        return classDefStmtJson;
    }

    public ObjectNode createInterfaceDefStmt(ClassOrInterfaceType type){
        ObjectNode interfaceDefStmt = this.mapper.createObjectNode();
        interfaceDefStmt.put("node","InterfaceDefStmt");
        interfaceDefStmt.put("name",type.getName().asString());
        interfaceDefStmt.set("methods",this.mapper.createArrayNode());
        ArrayNode genericParameters = this.mapper.createArrayNode();
        interfaceDefStmt.set("generic_parameters",genericParameters);
        //we can get Generic types from classOrInterfaceTypes so we can create list of this generic types as
        //generic parameters
        var typeArguments = type.getTypeArguments();
        typeArguments.ifPresent(types -> types.forEach(genericType -> {
            ObjectNode genericParam = this.mapper.createObjectNode();
            genericParam.put("node", "GenericParam");
            genericParam.put("name", "T");
            //we dont know if there is some constraint in original Class declaration,so we give  null to
            //constraint property
            genericParam.set("constraint", null);
            genericParameters.add(genericParam);
        }));
        interfaceDefStmt.set("bases",this.mapper.createArrayNode());
        return interfaceDefStmt;
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
