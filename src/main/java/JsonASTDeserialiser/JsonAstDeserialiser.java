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
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.type.*;
import com.github.javaparser.ast.visitor.GenericVisitorAdapter;
import com.github.javaparser.resolution.model.SymbolReference;

import javax.naming.Name;
import javax.swing.plaf.IconUIResource;
import javax.swing.text.html.Option;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.function.Consumer;


public class JsonAstDeserialiser extends GenericVisitorAdapter<JsonNode,JsonNode> {
    private ObjectMapper objectMapper = new ObjectMapper();
    private ReferenceTypeResolver referenceTypeResolver;
    private Map<String,ObjectNode> localVardDefStmts = new HashMap<>();
    private ObjectNode lasBlockStmtJson = null;
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
            ObjectNode methodJson = objectMapper.createObjectNode();
            methodJson.put("node","MethodDefStmt");

            Optional<ClassOrInterfaceDeclaration> anncestor = n.findAncestor(ClassOrInterfaceDeclaration.class);

            methodJson.put("owner", anncestor.isPresent() ?
                        anncestor.get().getNameAsString() : null);

            methodJson.put("name",n.getName().asString());

            //We are only interested in access modifier,should be first one
            methodJson.put("access", this.visit(n.getModifiers().get(0),null));

            ArrayNode parameters = this.objectMapper.createArrayNode();
            methodJson.put("parameters",parameters);
            n.getParameters().forEach(param -> parameters.add(this.visit(param,null)));


            methodJson.put("return_type",n.getType().accept(this,methodJson));


            methodJson.put("body",n.getBody().isPresent() ? this.visit(n.getBody().get(),null) : NullNode.getInstance());


            return methodJson;
    }


    @Override
    public JsonNode visit(ExpressionStmt n, JsonNode arg) {


        ObjectNode expressionStmt = this.objectMapper.createObjectNode();
            expressionStmt.put("node","ExpressionStmt");
            expressionStmt.put("expression", n.getExpression().accept(this, null));
            return expressionStmt;
        }



    @Override
    public JsonNode visit(ReturnStmt n, JsonNode arg) {
        ObjectNode returnStmtJson = this.objectMapper.createObjectNode();
        returnStmtJson.put("node","ReturnStmt");
        var returnExpr = n.getExpression();
        if (returnExpr.isPresent()) returnStmtJson.put("value",
                this.visit(returnExpr.get()));

        return returnStmtJson;
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

    //We are only interested in access modifiers in this stage
    @Override
    public JsonNode visit(Modifier n, JsonNode arg) {


        return TextNode.valueOf(n.getKeyword().asString());

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
        switchStmtJson.put("node",TextNode.valueOf("SwitchStmt"));
        switchStmtJson.put("entry",
                n.getSelector() instanceof NameExpr ?
                this.referenceTypeResolver.determineReferenceType(n,(NameExpr)n.getSelector()).toJson() :
                n.getSelector().accept(this,null));

        ArrayNode cases = objectMapper.createArrayNode();
        n.getEntries().forEach(entry -> cases.add(this.visit(entry,cases)));
        switchStmtJson.put("cases",cases);
        return switchStmtJson;
    }

    /*
    Switch entry is gona take as expression only first expression,because ASTFRI doesnt implement
    possibility do declare more expressions in case,maybe it s gonna change
     */

    @Override
    public JsonNode visit(SwitchEntry n, JsonNode arg) {
        ObjectNode switchEntryJson = objectMapper.createObjectNode();

        switchEntryJson.put("node", "CaseStmt");

       if(n.getLabels().size() > 0) {
           Expression entryExpr = n.getLabels().get(0);
           switchEntryJson.put("expression", this.visit(entryExpr));
       }else{
           switchEntryJson.put("expression", "default");
       }

        //If there is multiple statements instead of BlockStmt create Json representation of imaginary
        //BlockStmt node in order to serialize it easily int ASTFRI-there CaseStmt counts only with one
        //statement
        if( n.getStatements().size() > 1){
            ObjectNode compoundStmt = this.objectMapper.createObjectNode();
            compoundStmt.put("node","CompoundStmt");
            ArrayNode statements = this.objectMapper.createArrayNode();
            compoundStmt.put("statements",statements);
            switchEntryJson.put("body",compoundStmt);
            n.getStatements().forEach(statement -> {

                if (statement instanceof ExpressionStmt) {
                    ExpressionStmt expressionStmt = (ExpressionStmt) statement;
                    //Check if expression is VariableDeclarationExpr,if true use different logic-method for
                    //creating LocalVarDefStmt
                    if (expressionStmt.getExpression() instanceof VariableDeclarationExpr) {
                        VariableDeclarationExpr declarationExpr = (VariableDeclarationExpr) expressionStmt.getExpression();
                        this.createVarDefStmts(declarationExpr).forEach(localVar -> statements.add(localVar));
                    } else {
                        statements.add(statement.accept(this, statements));
                    }

                } else {
                    statements.add(statement.accept(this, statements));
                }
            });
        }else {
            var stmt = n.getStatements().getFirst();
            if (stmt.isPresent()){
                switchEntryJson.put("body",stmt.get().accept(this,null));
            }else {
                switchEntryJson.put("body",this.objectMapper.createObjectNode());
            }

        }

       return switchEntryJson;
    }

    @Override
    public JsonNode visit(WhileStmt n, JsonNode arg) {
        ObjectNode whileStmtJson = this.objectMapper.createObjectNode();
        whileStmtJson.put("node","WhileStmt");
        whileStmtJson.put("condition",this.visit(n.getCondition()));

        whileStmtJson.put("body",n.getBody().accept(this,null));
        return whileStmtJson;

    }

    @Override
    public JsonNode visit(TypeParameter n, JsonNode arg) {
        ObjectNode genParamJson = this.objectMapper.createObjectNode();
        genParamJson.put("node","GenericParam");
        genParamJson.put("name",n.getName().toString());
        genParamJson.put("constraint",n.getTypeBound().isEmpty() ?
                null : n.getTypeBound().get(0).asString());


        return genParamJson;
    }

    @Override
    public JsonNode visit(DoStmt n, JsonNode arg) {
        ObjectNode doStmtJson = this.objectMapper.createObjectNode();
        doStmtJson.put("node","DoWhileStmt");
        doStmtJson.put("condition",this.visit(n.getCondition()));


        doStmtJson.put("body",n.getBody().accept(this,null));
        return doStmtJson;

    }

    @Override
    public JsonNode visit(CompilationUnit n, JsonNode arg) {
        ObjectNode compUnitJson = this.objectMapper.createObjectNode();
        compUnitJson.put("node","TranslationUnit");
        ArrayNode classes = this.objectMapper.createArrayNode();
        compUnitJson.put("classes",classes);
        //we are only interested in ClassOrInterfaceDeclarations
        n.getChildNodes().forEach( node ->
                {
                    if(node instanceof ClassOrInterfaceDeclaration){
                        ClassOrInterfaceDeclaration classNode = (ClassOrInterfaceDeclaration)node;
                        classes.add(this.visit(classNode,null));
                    }
                }
        );

        compUnitJson.put("functions",this.objectMapper.createArrayNode());
        compUnitJson.put("globals",this.objectMapper.createArrayNode());
        return compUnitJson;
    }

    @Override
    public JsonNode visit(ForStmt n, JsonNode arg) {
        ObjectNode forStmtJson = this.objectMapper.createObjectNode();
        forStmtJson.put("node","ForStmt");
        //TODO
        n.getInitialization();

        //Condition part
        if(n.getCompare().isPresent()) {
            Expression cond = n.getCompare().get();
            forStmtJson.put("condition",cond.accept(this,null));
        }

        //TODO
        n.getUpdate();
        //TODO
        n.getBody();
        return forStmtJson;
    }

    @Override
    public JsonNode visit(ThrowStmt n, JsonNode arg) {
        ObjectNode throwStatementJson = objectMapper.createObjectNode();
        throwStatementJson.put("node","ThrowStmt");

        throwStatementJson.put("expression",this.visit(n.getExpression()));
        return throwStatementJson;
    }

    private ObjectNode visit(Expression expression){
        return expression instanceof NameExpr ?
                this.referenceTypeResolver.determineReferenceType(expression,(NameExpr) expression).toJson():
                ((ObjectNode)expression.accept(this,null));

    }

    @Override
    public JsonNode visit(ConstructorDeclaration n, JsonNode arg) {
        //TODO
        return null;
    }

    @Override
    public JsonNode visit(ObjectCreationExpr n, JsonNode arg) {
        ObjectNode constrCallExprJson = this.objectMapper.createObjectNode();
        constrCallExprJson.put("node","ConstructorCallExpr");
        return constrCallExprJson;
    }

    @Override
    public JsonNode visit(BlockStmt n, JsonNode arg) {
        ObjectNode blockStatementJson = objectMapper.createObjectNode();
        blockStatementJson.put("type","CompoundStmt");
        ArrayNode statements = objectMapper.createArrayNode();
        blockStatementJson.put("statements",statements);
        this.lasBlockStmtJson = lasBlockStmtJson;

        n.getStatements().forEach(statement ->{

            if(statement instanceof ExpressionStmt){
                ExpressionStmt expressionStmt = (ExpressionStmt)statement;
                //Check if expression is VariableDeclarationExpr,if true use different logic-method for
                //creating LocalVarDefStmt
                if(expressionStmt.getExpression() instanceof VariableDeclarationExpr){
                    VariableDeclarationExpr declarationExpr = (VariableDeclarationExpr) expressionStmt.getExpression();
                     this.createVarDefStmts(declarationExpr).forEach(localVar -> statements.add(localVar));
                }else{
                    statements.add(statement.accept(this,statements));
                }

            } else{
                statements.add(statement.accept(this,statements));
            }

        });
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

    /*
        Second parameter should be ArrayNode  property of parent Node
     */
    @Override
    public JsonNode visit(VariableDeclarationExpr n, JsonNode arg) {
        n.getVariables().forEach(variableDeclarator -> {
            this.localVardDefStmts.put(variableDeclarator.getNameAsString(),this.objectMapper.createObjectNode());
        });

        n.getModifiers();
        return null;
    }
    private ObjectNode createLocalVarDefStmt(VariableDeclarator declNode,int index){
        ObjectNode localVarDefStmtJson = this.objectMapper.createObjectNode();
        localVarDefStmtJson.put("node","LocalVarDefStmt");
        this.visit(declNode,localVarDefStmtJson);
        return localVarDefStmtJson;
    }

    /*
    * Second parameter should be ObjectNode representation either of
    * MemberVarDefStmt or LocalVarDefStmt
    * calling node should ingnore return value
    * */
    @Override
    public JsonNode visit(VariableDeclarator n, JsonNode arg) {

        if (!(arg instanceof ObjectNode)) throw new IllegalArgumentException();
        ObjectNode varDefStmtJson = (ObjectNode) arg;

        varDefStmtJson.put("name", n.getName().toString());
        varDefStmtJson.put("type", n.getType().accept(this,null));
        var initialzer = n.getInitializer();
        if(initialzer.isPresent()) {
            varDefStmtJson.put("initialzer",this.visit(initialzer.get()));
        }else {
            varDefStmtJson.put("initializer", NullNode.getInstance());
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
        ObjectNode classOrInterfDeclJson = objectMapper.createObjectNode();
        classOrInterfDeclJson.put("node","ClassDefStmt");

        classOrInterfDeclJson.put("name",n.getNameAsString());
        /*
        ArrayNode modifiers = objectMapper.createArrayNode();
        classOrInterfDeclJson.put("modifiers",modifiers);
        n.getModifiers().forEach(mod -> modifiers.add(this.visit(mod,null)));*/

        ArrayNode attributes = objectMapper.createArrayNode();
        classOrInterfDeclJson.put("atrributes",attributes);
        n.getFields().forEach(field -> {
            field.getVariables().forEach(variable ->{
                    ObjectNode memberDefStmtJson = this.objectMapper.createObjectNode(); //this.visit(variable,null);
                    memberDefStmtJson.put("node","MemberVarDefStmt");

                    //We are only interested in access modifiers and we assume that access mod is first
                    memberDefStmtJson.put("acces",this.visit(field.getModifiers().get(0),null));
                    this.visit(variable,memberDefStmtJson);
                    attributes.add(memberDefStmtJson);
            });

        });



        ArrayNode methods = objectMapper.createArrayNode();
        classOrInterfDeclJson.put("methods",methods);
        n.getMethods().forEach(m -> methods.add(visit(m,null)));

        ArrayNode generic_params = objectMapper.createArrayNode();
        classOrInterfDeclJson.put("generic_parameters",generic_params);

        n.getTypeParameters().forEach(gen_param -> {

            generic_params.add(this.visit(gen_param,null));
        });
        return classOrInterfDeclJson;
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
            assignExprJson.put("left",this.visit(n.getTarget()));

            assignExprJson.put("right",this.visit(n.getValue()));
        }else{
            assignExprJson.put("node", "CompoundAssignExpr");
            assignExprJson.put("left",this.visit(n.getTarget()));

            assignExprJson.put("right",this.visit(n.getValue()));
            assignExprJson.put("operator",op.asString());
        }
        return assignExprJson;
    }


    @Override
    public JsonNode visit(BinaryExpr n, JsonNode arg) {
        ObjectNode binaryExprJson = objectMapper.createObjectNode();
        binaryExprJson.put("node","BinOpExpr");
        Expression left = n.getLeft();
        binaryExprJson.put("left",this.visit(left));

        Expression right = n.getRight();
        binaryExprJson.put("right",this.visit(right));

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
        unaryExprJson.put("argument",this.visit(expr));

        return unaryExprJson;
    }

    @Override
    public JsonNode visit(ConditionalExpr n, JsonNode arg) {
        ObjectNode condExprJson = this.objectMapper.createObjectNode();
        condExprJson.put("node","ConditionalExpr");
        Expression then = n.getThenExpr();
        condExprJson.put("ifTrue",this.visit(then));

        Expression or = n.getElseExpr();
        condExprJson.put("ifFalse",this.visit(or));
        return condExprJson;
    }

    @Override
    public JsonNode visit(IfStmt n, JsonNode arg) {
        ObjectNode ifStmtJsonNode = objectMapper.createObjectNode();
        ifStmtJsonNode.put("node","IfStmt");
        ifStmtJsonNode.put("condition",n.getCondition().accept(this,null));
        ifStmtJsonNode.put("ifTrue",n.getThenStmt().accept(this,null));
        Optional<Statement> elseStmt = n.getElseStmt();
        if (elseStmt.isPresent()){
            ifStmtJsonNode.put("ifFalse",elseStmt.get().accept(this,null));
        }else {
            ifStmtJsonNode.put("IfFalse",NullNode.getInstance());
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
        n.getArguments().forEach(e -> arguments.add(this.visit(e)));
            // If argument is type of ThisExpr,visit ThisExpr
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
    private List<ObjectNode> createVarDefStmts(VariableDeclarationExpr expr){


            List<ObjectNode> varDefstmts = new ArrayList<>();
            expr.getVariables().forEach(var -> {
                ObjectNode varDefstmtJson = this.objectMapper.createObjectNode();
                varDefstmtJson.put("node","LocalVarDefStmt");
                this.visit(var,varDefstmtJson);
                varDefstmts.add(varDefstmtJson);
            });
            return varDefstmts;

    }
}
