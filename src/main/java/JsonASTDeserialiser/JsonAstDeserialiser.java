package JsonASTDeserialiser;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
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
import com.github.javaparser.ast.type.UnknownType;
import com.github.javaparser.ast.type.VoidType;
import com.github.javaparser.ast.visitor.GenericVisitorAdapter;
import com.github.javaparser.resolution.model.SymbolReference;

import javax.naming.Name;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Optional;
import java.util.function.Consumer;


public class JsonAstDeserialiser extends GenericVisitorAdapter<JsonNode,JsonNode> {
    private ObjectMapper objectMapper = new ObjectMapper();
    private ReferenceTypeResolver referenceTypeResolver;

    public JsonAstDeserialiser(File file) {
        this.referenceTypeResolver = new ReferenceTypeResolver(this.objectMapper,file);
    }

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
                modifiers.add(this.visit(modifier,modifiers));
            }
            ArrayNode parameters = objectMapper.createArrayNode();

            for (Parameter parameter : n.getParameters()){
                this.visit(parameter,arg);
                parameters.add(this.visit(parameter,arg));
            }
            method.put("params",parameters);
            method.put("body",n.getBody().isPresent() ? this.visit(n.getBody().get(),null) : NullNode.getInstance());


            return method;
    }

    @Override
    public JsonNode visit(ExpressionStmt n, JsonNode arg) {
        ObjectNode expressionStmt = this.objectMapper.createObjectNode();
        expressionStmt.put("node","ExpressionStmt");
        expressionStmt.put("expression",n.getExpression().accept(this,null));
        return expressionStmt;
    }

    @Override
    public JsonNode visit(LambdaExpr n, JsonNode arg) {

        ObjectNode lambdaExprJson = objectMapper.createObjectNode();
        lambdaExprJson.put("node","LambdaExpr");
        ArrayNode params = objectMapper.createArrayNode();
        lambdaExprJson.put("params",params);
        n.getParameters().forEach(param -> params.add(this.visit(param,null)));
        lambdaExprJson.put("body",n.getBody().accept(this,null));
        return lambdaExprJson;
    }

    @Override
    public JsonNode visit(UnknownType n, JsonNode arg) {
        return new TextNode("Unknown");
    }

    @Override
    public JsonNode visit(VoidType n, JsonNode arg) {
        return new TextNode("Void");
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
        charLiteralExprJson.put("type", "CharLitExpr");
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
    /*FieldDeclaration is parent Node for Variable Declarator in purpose
   to cover cases where multiple attributes are declared  in same row like
       int x,y
   * */
    /*@Override
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
    }*/
    /*VariableDeclarationExpr is parent Node for Variable Declarator in purpose
    to cover cases where multiple variables are declared in same row like
        int x,y
    * */
    @Override
    public JsonNode visit(VariableDeclarationExpr n, JsonNode arg) {

        n.getVariables();
        n.getModifiers();
        return null;
    }

    /*
    * Second parameter should be ObjectNode representation either of
    * MemberVarDefStmt or LocalVarDefStmt
    * */
    @Override
    public JsonNode visit(VariableDeclarator n, JsonNode arg) {

        if (!(arg instanceof ObjectNode)) throw new IllegalArgumentException();
        ObjectNode varDefStmtJson = (ObjectNode) arg;

        varDefStmtJson.put("name", n.getName().toString());
        varDefStmtJson.put("type", n.getType().accept(this,null));
        var initialzer = n.getInitializer();
        if(initialzer.isPresent()) {
            varDefStmtJson.put("initialzer", initialzer.get() instanceof NameExpr ?
                    this.referenceTypeResolver.determineReferenceType(n,(NameExpr) initialzer.get()).toJson():
                    initialzer.get().accept(this,null));
        }else {
            varDefStmtJson.put("initializer", "");
        }

        return arg;


    }

    @Override
    public JsonNode visit(PrimitiveType n, JsonNode arg) {

        switch (n.getType()){
            case INT -> {return  new TextNode("Int");}
            case BYTE -> {return new TextNode("Int");}
            case SHORT -> {return new TextNode("Int");}
            case LONG ->  {return new TextNode("Int");}
            case CHAR -> {return new TextNode("Char");}
            case FLOAT -> {return new TextNode("Float");}
            case DOUBLE -> {return new TextNode("Float");}
            case BOOLEAN ->  {return new TextNode("Bool");}
            default ->  {return new TextNode("Unknown");}
        }
    }
    /*
    * This visit method expects second argument ArrayNode to which
    * final Json represantion of parameter is added
    * */

    @Override
    public JsonNode visit(Parameter n, JsonNode arg) {
            ObjectNode parameterNodeJson = objectMapper.createObjectNode();
                parameterNodeJson.put("node","ParamVarDefStmt");
                parameterNodeJson.put("name",n.getNameAsString());
                parameterNodeJson.put("type",n.getType().accept(this,null));
                parameterNodeJson.put("initialiser",NullNode.getInstance());


        return parameterNodeJson;

    }

    @Override
    public JsonNode visit(ClassOrInterfaceType n, JsonNode arg) {

        return new TextNode("UserType");

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
        classOrInterfDecl.put("atrributes",attributes);
        n.getFields().forEach(field -> {
            field.getVariables().forEach(variable ->{
                    ObjectNode memberDefStmtJson = this.objectMapper.createObjectNode(); //this.visit(variable,null);
                    memberDefStmtJson.put("node","MemberVarDefStmt");
                    ArrayNode access = this.objectMapper.createArrayNode();
                    memberDefStmtJson.put("acces",access);
                    field.getModifiers().forEach(mod -> access.add(this.visit(mod,null)));
                    this.visit(variable,memberDefStmtJson);
                    attributes.add(memberDefStmtJson);
            });

        });


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
        AssignExpr.Operator op  = n.getOperator();
        if (op == AssignExpr.Operator.ASSIGN) {
            assignExprJson.put("node", "AssignExpr");
            assignExprJson.put("left",n.getTarget().isNameExpr() ?
                    this.referenceTypeResolver.determineReferenceType(n,(NameExpr) n.getTarget()).toJson():
            n.getTarget().accept(this, null));

            assignExprJson.put("right",n.getValue().isNameExpr() ?
                    this.referenceTypeResolver.determineReferenceType(n,(NameExpr) n.getValue()).toJson():
                    n.getValue().accept(this, null));
        }else{
            assignExprJson.put("node", "CompoundAssignExpr");
            assignExprJson.put("left",n.getTarget().isNameExpr() ?
                    this.referenceTypeResolver.determineReferenceType(n,(NameExpr) n.getTarget()).toJson():
                    n.getTarget().accept(this, null));

            assignExprJson.put("right",n.getValue().isNameExpr() ?
                    this.referenceTypeResolver.determineReferenceType(n,(NameExpr) n.getValue()).toJson():
                    n.getValue().accept(this, null));
            assignExprJson.put("operator",op.asString());
        }
        return assignExprJson;
    }


    @Override
    public JsonNode visit(BinaryExpr n, JsonNode arg) {
        ObjectNode binaryExprJson = objectMapper.createObjectNode();
        binaryExprJson.put("node","BinOpExpr");
        Expression left = n.getLeft();
        binaryExprJson.put("left",left instanceof NameExpr ?
                this.referenceTypeResolver.determineReferenceType(n,(NameExpr) left).toJson():
                n.getLeft().accept(this,null));

        Expression right = n.getRight();
        binaryExprJson.put("right",right instanceof NameExpr ?
                this.referenceTypeResolver.determineReferenceType(n,(NameExpr) right).toJson() :
                n.getRight().accept(this,null));

        binaryExprJson.put("operator",n.getOperator().asString());
        return binaryExprJson;
    }

    @Override
    public JsonNode visit(UnaryExpr n, JsonNode arg) {
        ObjectNode unaryExprJson = objectMapper.createObjectNode();
        UnaryExpr.Operator operator = n.getOperator();
        unaryExprJson.put("node","UnaryOpExpr");
        unaryExprJson.put("operator",operator.asString());
        unaryExprJson.put("isPostfix", operator.isPostfix() ? true : false);
        Expression expr = n.getExpression();
        unaryExprJson.put("argument",
                expr instanceof NameExpr ?
                this.referenceTypeResolver.determineReferenceType(n,(NameExpr)expr).toJson() :
                        expr.accept(this,null));

        return unaryExprJson;
    }

    @Override
    public JsonNode visit(ConditionalExpr n, JsonNode arg) {
        ObjectNode condExprJson = this.objectMapper.createObjectNode();
        condExprJson.put("node","ConditionalExpr");
        Expression then = n.getThenExpr();
        condExprJson.put("ifTrue",then instanceof NameExpr ?
                this.referenceTypeResolver.determineReferenceType(n,(NameExpr) then).toJson():
                then.accept(this,null));

        Expression or = n.getElseExpr();
        condExprJson.put("ifFalse",or instanceof NameExpr ?
                this.referenceTypeResolver.determineReferenceType(n,(NameExpr) or).toJson():
                then.accept(this,null));
        return condExprJson;
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
        doubleLitExprJson.put("type","FloatLitExpr");
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
        return nullLitExprJson;
    }

    @Override
    public JsonNode visit(StringLiteralExpr n, JsonNode arg) {
        ObjectNode stringLitExprJson = objectMapper.createObjectNode();
        stringLitExprJson.put("type","StringLitExpr");
        stringLitExprJson.put("value",n.getValue());
        return stringLitExprJson;
    }

    @Override
    public JsonNode visit(ThisExpr n, JsonNode arg) {
        ObjectNode thisExprJson = objectMapper.createObjectNode();
        thisExprJson.put("node","ThisExpr");
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
        methodCallExprJson.put("node","MethodCallExpr");
        methodCallExprJson.put("name",n.getNameAsString());

        if(n.getScope().isPresent()){
            if (n.getScope().get() instanceof NameExpr){
                methodCallExprJson.put("owner",
                        this.referenceTypeResolver.determineReferenceType(n,(NameExpr) n.getScope().get()).toJson());
            } else{
                methodCallExprJson.put("owner",n.getScope().get().accept(this,null));
            }
        }
        ArrayNode arguments = objectMapper.createArrayNode();
        /*Question is how to determine types of reference expressions in case some of argument
        *is this reference expression
        */
        n.getArguments().forEach(e -> {
            // If argument is type of ThisExpr,visit ThisExpr
            if(e instanceof NameExpr) {
                arguments.add(this.referenceTypeResolver.determineReferenceType(n, (NameExpr) e).toJson());
            }else{
                arguments.add(e.accept(this,null));
            }

        });
        methodCallExprJson.put("arguments",arguments);
        return methodCallExprJson;
    }

    @Override
    public JsonNode visit(FieldAccessExpr n, JsonNode arg) {
        return new Reference(Reference.ReferenceType.MEMBER_VAR_REFERENCE,
                n.getNameAsString(),this.objectMapper).toJson();
    }

    public String convertToJson(final Node node){
        JsonNode jsonRepresentation = node.accept(this,null);

        try{
            DefaultPrettyPrinter printer = new DefaultPrettyPrinter();
            printer.indentArraysWith(DefaultPrettyPrinter.NopIndenter.instance);
            ObjectWriter writer = objectMapper.writer(printer);


            return writer.writeValueAsString(jsonRepresentation);

        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return null;
        }

    }
}
