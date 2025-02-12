package JsonASTDeserialiser;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.ArrayAccessExpr;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.visitor.GenericVisitorAdapter;

import java.lang.classfile.Signature;

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
        super.visit(n, arg);
            if (!(arg instanceof ArrayNode)) return null;
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

            ObjectNode bodyNode = objectMapper.createObjectNode();
            this.visit(n.getBody().get(),arg);
            method.put("body",bodyNode);

            return method;
    }

    @Override
    public JsonNode visit(Modifier n, JsonNode arg) {
        super.visit(n, arg);
        TextNode textNode = TextNode.valueOf(n.toString());
        return textNode;

    }

    @Override
    public JsonNode visit(Parameter n, JsonNode arg) {
        super.visit(n, arg);
            ObjectNode parameterNode = objectMapper.createObjectNode();
            parameterNode.put("nodeType","Parameter");
            parameterNode.put("name",n.getName().toString());
            //typ musi pridat do argumentu arg svoj typ;
            n.getType().accept(this,parameterNode);
            ArrayNode arrayNode = objectMapper.createArrayNode();

            for(Modifier modifier : n.getModifiers()){
                this.visit(modifier,arrayNode);
            }

        return parameterNode;

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
        classOrInterfDecl.put("attributes",attributes);
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

    public String convertToJson(final Node node){
        JsonNode jsonRepresentation = node.accept(this,null);

        try{
            return objectMapper.writeValueAsString(jsonRepresentation);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return null;
        }

    }
}
