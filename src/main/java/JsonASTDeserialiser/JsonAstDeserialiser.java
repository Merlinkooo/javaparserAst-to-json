package JsonASTDeserialiser;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.node.*;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.PrimitiveType;
import com.github.javaparser.ast.type.VoidType;
import com.github.javaparser.ast.visitor.GenericVisitorAdapter;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Optional;
import java.util.function.Consumer;


public class JsonAstDeserialiser extends GenericVisitorAdapter<JsonNode,JsonNode> {
    private ObjectMapper objectMapper = new ObjectMapper();
    /*
    private <N extends Node> void addElementsToArray(ArrayNode array, List<N> elements){
        for (Node element : elements){
            ObjectNode jsonNode = objectMapper.createObjectNode();
            this.visit(element,jsonNode);
            array.add(jsonNode);

        }
    }*/
    /*ass second argument should be ArrayNode
    */
    @Override
    public JsonNode visit(MethodDeclaration n, JsonNode arg) {


            ObjectNode method = objectMapper.createObjectNode();
            method.put("node_type","MethodDeclaration");
            method.put("name",n.getName().asString());

            method.put("return_type",n.getType().accept(this,method));

            ArrayNode modifiers = objectMapper.createArrayNode();
            method.put("modifiers",modifiers);
            for(Modifier modifier : n.getModifiers()){
                modifiers.add(visit(modifier,modifiers));
            }
            ArrayNode parameters = objectMapper.createArrayNode();

            for (Parameter parameter : n.getParameters()){
                this.visit(parameter,arg);
                parameters.add(this.visit(parameter,arg));
            }

            method.put("body",n.getBody().isPresent() ? this.visit(n.getBody().get(),null) : NullNode.getInstance());


            return method;
    }

    @Override
    public JsonNode visit(LambdaExpr n, JsonNode arg) {

        ObjectNode lambdaExprJson = objectMapper.createObjectNode();
        lambdaExprJson.put("type","LambdaExpr");
        ArrayNode params = objectMapper.createArrayNode();
        lambdaExprJson.put("params",params);
        n.getParameters().forEach(param -> this.visit(param,params));
        lambdaExprJson.put("body",n.getBody().accept(this,null));
        return lambdaExprJson;
    }

    @Override
    public JsonNode visit(VoidType n, JsonNode arg) {
        return new TextNode("void");
    }

    @Override
    public JsonNode visit(Modifier n, JsonNode arg) {


        TextNode textNode = TextNode.valueOf(n.toString());
        //if (arg instanceof ArrayNode) ((ArrayNode)arg).add(textNode);
        return textNode;

    }

    @Override
    public JsonNode visit(CharLiteralExpr n, JsonNode arg) {
        ObjectNode charLiteralExprJson = objectMapper.createObjectNode();
        charLiteralExprJson.put("type", TextNode.valueOf("CharLitExpr"));
        charLiteralExprJson.put("value",TextNode.valueOf(n.getValue()));
        return charLiteralExprJson;
    }

    @Override
    public JsonNode visit(SwitchStmt n, JsonNode arg) {
        ObjectNode switchStmtJson = objectMapper.createObjectNode();
        switchStmtJson.put("type",TextNode.valueOf("SwitchStatement"));
        switchStmtJson.put("entry",n.getSelector().accept(this,null));

        ArrayNode entries = objectMapper.createArrayNode();
        n.getEntries().forEach(entry -> entries.add(this.visit(entry,null)));
        return switchStmtJson;
    }

    @Override
    public JsonNode visit(SwitchEntry n, JsonNode arg) {
        ObjectNode switchEntryJson = objectMapper.createObjectNode();

       switchEntryJson.put("type" ,n.getType().toString());
       ArrayNode body = objectMapper.createArrayNode();
       switchEntryJson.put("body",body);
       n.getStatements().forEach(stmt -> body.add(stmt.accept(this,null)));
       return switchEntryJson;
    }

    @Override
    public JsonNode visit(BlockStmt n, JsonNode arg) {
        ObjectNode blockStatementJson = objectMapper.createObjectNode();
        blockStatementJson.put("type","CompoundStmt");
        ArrayNode statements = objectMapper.createArrayNode();
        blockStatementJson.put("statements",statements);
        n.getStatements().forEach(statement -> statements.add(statement.accept(this,null)));
        return blockStatementJson;
    }

    @Override
    public JsonNode visit(FieldDeclaration n, JsonNode arg) {
        ObjectNode fieldDeclarationJson = objectMapper.createObjectNode();
        fieldDeclarationJson.put("node_type","FieldDeaclaration");


        ArrayNode variableDeclarations = objectMapper.createArrayNode();
        fieldDeclarationJson.put("variable_declarations",variableDeclarations);
        n.getVariables().forEach(e -> variableDeclarations.add(e.accept(this,variableDeclarations)));

        ArrayNode modifiers = objectMapper.createArrayNode();
        fieldDeclarationJson.put("modifiers",modifiers);
        n.getModifiers().forEach(mod -> modifiers.add(mod.accept(this,null)));

        return fieldDeclarationJson;
    }
    /*VariableDeclarationExpr is parent Node for Variable Declarator in purpose
    to cover cases where multiple variables are declared in same row like
        int x,y
    * */
    @Override
    public JsonNode visit(VariableDeclarationExpr n, JsonNode arg) {
        /*
        n.getVariables();
        n.getModifiers();
        return null;*/
    }


    @Override
    public JsonNode visit(VariableDeclarator n, JsonNode arg) {

            ObjectNode variableDeclaratorJson = objectMapper.createObjectNode();
            variableDeclaratorJson.put("variable_name", n.getName().toString());
            variableDeclaratorJson.put("initial_value", n.getInitializer().isPresent() ?
                    n.getInitializer().get().accept(this, null) : new TextNode(""));

            variableDeclaratorJson.put("type", n.getType().asString());
            return variableDeclaratorJson;

    }

    @Override
    public JsonNode visit(PrimitiveType n, JsonNode arg) {
        return super.visit(n, arg);
    }
    /*
    * This visit method expects second argument ArrayNode to which
    * final Json represantion of parameter is added
    * */
    @Override
    public JsonNode visit(Parameter n, JsonNode arg) {
            ObjectNode parameterNodeJson = objectMapper.createObjectNode();

                parameterNodeJson.put("name",n.getNameAsString());
                parameterNodeJson.put("type",n.getType().asString());
                parameterNodeJson.put("initialiser",NullNode.getInstance());

                if (arg instanceof ArrayNode && arg != null)
                ((ArrayNode) arg).add(parameterNodeJson);




        return parameterNodeJson;

    }

    @Override
    public JsonNode visit(ClassOrInterfaceType n, JsonNode arg) {
        super.visit(n, arg);
        TextNode textNode = new TextNode(n.toString());
        return textNode;
    }

    @Override
    public JsonNode visit(ClassOrInterfaceDeclaration n, JsonNode arg) {
        //return super.visit(n, arg);
        ObjectNode classOrInterfDecl = objectMapper.createObjectNode();
        classOrInterfDecl.put("node_type","ClassOrInterfaceDeclaration");
        ArrayNode modifiers = objectMapper.createArrayNode();
        classOrInterfDecl.put("modifiers",modifiers);
        n.getModifiers().forEach(mod -> modifiers.add(this.visit(mod,null)));


        ArrayNode attributes = objectMapper.createArrayNode();
        classOrInterfDecl.put("fieldDeclarations",attributes);
        n.getFields().forEach(field -> attributes.add(this.visit(field,null)));


        ArrayNode methods = objectMapper.createArrayNode();
        classOrInterfDecl.put("methods",methods);
        n.getMethods().forEach(m -> methods.add(visit(m,null)));
        return classOrInterfDecl;
    }

    @Override
    public JsonNode visit(ArrayAccessExpr n, JsonNode arg) {
         ObjectNode jsonArrAccExpr = objectMapper.createObjectNode();
         jsonArrAccExpr.put("type_node","ArrayAccesExpression");
         n.getName().ifNameExpr( e -> jsonArrAccExpr.put("name",e.asNameExpr().toString()));
         jsonArrAccExpr.put("index",n.getIndex().asIntegerLiteralExpr().asNameExpr().toString());
         return jsonArrAccExpr;
    }

    @Override
    public JsonNode visit(AssignExpr n, JsonNode arg) {
        ObjectNode assignExprJson = objectMapper.createObjectNode();
        assignExprJson.put("type_node","AssignExpression");
        assignExprJson.put("operator",n.getOperator().asString());
        assignExprJson.put("left_side",n.getTarget().accept(this,null));
        assignExprJson.put("right_side",n.getValue().accept(this,null));
        return assignExprJson;
    }

    @Override
    public JsonNode visit(BinaryExpr n, JsonNode arg) {
        ObjectNode binaryExprJson = objectMapper.createObjectNode();
        binaryExprJson.put("node_type","BinaryExpr");
        binaryExprJson.put("operator",n.getOperator().asString());
        binaryExprJson.put("left_operand",n.getLeft().accept(this,null));
        binaryExprJson.put("right_operand",n.getRight().accept(this,null));
        return binaryExprJson;
    }

    @Override
    public JsonNode visit(IfStmt n, JsonNode arg) {
        ObjectNode ifStmtJsonNode = objectMapper.createObjectNode();
        ifStmtJsonNode.put("node_type","IfStmt");
        ifStmtJsonNode.put("condition",n.getCondition().accept(this,null));
        ifStmtJsonNode.put("ifTrue",n.getThenStmt().accept(this,null));
        Optional<Statement> elseStmt = n.getElseStmt();
        if (elseStmt.isPresent()){
            ifStmtJsonNode.put("else",elseStmt.get().accept(this,null));
        }
        return ifStmtJsonNode;
    }

    @Override
    public JsonNode visit(BooleanLiteralExpr n, JsonNode arg) {
        ObjectNode booleanLitExprJson = objectMapper.createObjectNode();
        booleanLitExprJson.put("type","BoolLitExpr");
        booleanLitExprJson.put("value",n.getValue());
        return booleanLitExprJson;
    }

    @Override
    public JsonNode visit(DoubleLiteralExpr n, JsonNode arg) {
        ObjectNode doubleLitExprJson = objectMapper.createObjectNode();
        doubleLitExprJson.put("type","IntLitExpr");
        doubleLitExprJson.put("value",n.getValue());
        return doubleLitExprJson;
    }

    @Override
    public JsonNode visit(IntegerLiteralExpr n, JsonNode arg) {
        ObjectNode intLitExprJson = objectMapper.createObjectNode();
        intLitExprJson.put("type","IntLitExpr");
        intLitExprJson.put("value",n.getValue());
        return intLitExprJson;
    }

    @Override
    public JsonNode visit(LongLiteralExpr n, JsonNode arg) {
        //return this.createJsonLiteralExpr("LongLiteralExpr",n.getValue());
        ObjectNode intLitExprJson = objectMapper.createObjectNode();
        intLitExprJson.put("type","IntLitExpr");
        intLitExprJson.put("value",n.getValue());
        return intLitExprJson;
    }

    @Override
    public JsonNode visit(NullLiteralExpr n, JsonNode arg) {
        ObjectNode nullLitExprJson = objectMapper.createObjectNode();
        nullLitExprJson.put("type","NullLitExpr");
        nullLitExprJson.put("value", NullNode.getInstance());
        return nullLitExprJson;
    }

    @Override
    public JsonNode visit(StringLiteralExpr n, JsonNode arg) {
        ObjectNode stringLitExprJson = objectMapper.createObjectNode();
        stringLitExprJson.put("type","StringLiteralExpr");
        stringLitExprJson.put("value",n.getValue());
        return stringLitExprJson;
    }

    @Override
    public JsonNode visit(ThisExpr n, JsonNode arg) {
        ObjectNode thisExprJson = objectMapper.createObjectNode();
        thisExprJson.put("type","ThisExpr");
        return thisExprJson;
    }


    private <T> JsonNode createJsonLiteralExpr(String type, T value){
        ObjectNode jsonNode = objectMapper.createObjectNode();
        jsonNode.put("type",type);
        if (value == null){
            jsonNode.putNull("value");
        } else if(value instanceof String){
            jsonNode.put("value",(String)value);
        }else if(value instanceof Number){
            jsonNode.put("value", String.valueOf(value));
        } else if(value instanceof Boolean){
            jsonNode.put(" value",(boolean)value);
        }
        return jsonNode;
    }
    public Optional<JsonNode> createJsonObject(Class clasType){
        if(clasType == ObjectNode.class) return Optional.of(objectMapper.createObjectNode());
        if(clasType == ArrayNode.class) return Optional.of(objectMapper.createArrayNode());
        if(clasType == TextNode.class) return Optional.of(new TextNode(null));
        return Optional.empty();
    }

    @Override
    public JsonNode visit(NameExpr n, JsonNode arg) {
        return new TextNode(n.getNameAsString());
        //return null;
    }

    @Override
    public JsonNode visit(MethodCallExpr n, JsonNode arg) {
        ObjectNode methodCallExprJson = objectMapper.createObjectNode();
        methodCallExprJson.put("node_type","MethodCallExpr");
        methodCallExprJson.put("name",n.getNameAsString());
        methodCallExprJson.put("owner",n.getScope().isPresent() ? n.getScope().get().accept(this,null): new TextNode("this"));

        ArrayNode arguments = objectMapper.createArrayNode();
        n.getArguments().forEach(e -> arguments.add(e.accept(this,null)));
        methodCallExprJson.put("arguments",arguments);

        return methodCallExprJson;
    }

    public String convertToJson(final Node node){
        JsonNode jsonRepresentation = node.accept(this,null);

        try{
            ObjectWriter writer = objectMapper.writerWithDefaultPrettyPrinter();

            return writer.writeValueAsString(jsonRepresentation);

        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return null;
        }

    }
}
