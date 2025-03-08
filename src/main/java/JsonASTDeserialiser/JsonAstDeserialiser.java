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
import com.github.javaparser.ast.AccessSpecifier;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.nodeTypes.NodeWithVariables;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.type.*;
import com.github.javaparser.ast.visitor.GenericVisitorAdapter;
import com.github.javaparser.resolution.model.SymbolReference;
import com.github.javaparser.utils.SourceRoot;

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
    private NodeCreator synteticNodeCreator = new NodeCreator(this.objectMapper);
    private BlockStmt lasBlockStmtJson = null;
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


            var accessSpecifier =  n.getAccessSpecifier();
            methodJson.put("access",accessSpecifier == AccessSpecifier.NONE ? "internal" :
                                                                        accessSpecifier.asString());

            ArrayNode parameters = this.objectMapper.createArrayNode();
            methodJson.put("parameters",parameters);
            n.getParameters().forEach(param -> parameters.add(this.visit(param,null)));


            methodJson.put("return_type",n.getType().accept(this,methodJson));


            methodJson.put("body",n.getBody().isPresent() ? this.visit(n.getBody().get(),null) : NullNode.getInstance());
            methodJson.put("virtuality","yes");

            return methodJson;
    }

    // if inside of this statement is VariableDeclarationExpr,then returned Json representation will
    //be either DefStmt - if we are declaring multiple variables or LocalVarDefStmt if only one variable
    //logic to recognise which of that 2 cases is actuall is implemented in VariableDeclarationExpr
    @Override
    public JsonNode visit(ExpressionStmt n, JsonNode arg) {
        if (n.getExpression() instanceof VariableDeclarationExpr) {
            return n.getExpression().accept(this,null);
        }

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
        returnStmtJson.put("value",returnStmtJson.isEmpty() ? NullNode.getInstance() :
                returnExpr.get().accept(this,null));

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
        return this.objectMapper.createObjectNode().put("node","Unknown");
    }

    @Override
    public JsonNode visit(VoidType n, JsonNode arg) {
        return this.objectMapper.createObjectNode().put("node","Void");
    }

    //We are only interested in access modifiers in this stage
    //so if there is no modifier ,it means it s default modifier-and equivalent of default is internal in ASTFRI
    @Override
    public JsonNode visit(Modifier n, JsonNode arg) {
        String modifStr = n.getKeyword().asString();

        if (modifStr.equals("private") || modifStr.equals("public") || modifStr.equals("protected")){
            return TextNode.valueOf(modifStr);
        }

        return TextNode.valueOf("internal");

    }

    @Override
    public JsonNode visit(CharLiteralExpr n, JsonNode arg) {
        ObjectNode charLiteralExprJson = objectMapper.createObjectNode();
        charLiteralExprJson.put("node", "CharLitExpr");
        charLiteralExprJson.put("value",TextNode.valueOf(n.getValue()));
        return charLiteralExprJson;
    }

    @Override
    public JsonNode visit(SwitchStmt n, JsonNode arg) {
        ObjectNode switchStmtJson = objectMapper.createObjectNode();
        switchStmtJson.put("node","SwitchStmt");
        switchStmtJson.put("entry", n.getSelector().accept(this,null));

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

       ArrayNode caseExpressions = this.objectMapper.createArrayNode();
       n.getLabels().forEach(expr -> caseExpressions.add(expr.accept(this,null)));

       switchEntryJson.put("expressions",caseExpressions);
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
               statements.add(statement.accept(this,null));
            });
        }else {
            var stmt = n.getStatements().getFirst();
            if (stmt.isPresent()){
                switchEntryJson.put("body",stmt.get().accept(this,null));
            }else {
                //? what if case is empty,it makes no sense te create empty case,but this code can be compiled
                switchEntryJson.put("body",NullNode.getInstance());
            }

        }

       return switchEntryJson;
    }

    @Override
    public JsonNode visit(WhileStmt n, JsonNode arg) {
        ObjectNode whileStmtJson = this.objectMapper.createObjectNode();
        whileStmtJson.put("node","WhileStmt");
        whileStmtJson.set("condition",n.getCondition().accept(this,null));


        whileStmtJson.set("body",n.getBody().accept(this,null));



        return whileStmtJson;

    }

    @Override
    public JsonNode visit(TypeParameter n, JsonNode arg) {
        ObjectNode genParamJson = this.objectMapper.createObjectNode();
        genParamJson.put("node","GenericParam");
        genParamJson.put("name",n.getName().toString());
        // If there is some restriction to generic param-we take only first one or put there null
        genParamJson.put("constraint",n.getTypeBound().isEmpty() ?
                null : n.getTypeBound().get(0).asString());


        return genParamJson;
    }

    @Override
    public JsonNode visit(DoStmt n, JsonNode arg) {
        ObjectNode doStmtJson = this.objectMapper.createObjectNode();
        doStmtJson.put("node","DoWhileStmt");
        doStmtJson.set("condition",n.getCondition().accept(this,null));
        Statement body = n.getBody();
        doStmtJson.set("body",body.accept(this,null));



        return doStmtJson;

    }

    //If second parameter is not null ,it determines that multiple source files were parsed and different
    //logic is applied in this method
    @Override
    public JsonNode visit(CompilationUnit n, JsonNode arg) {
        if (arg == null) {
            ObjectNode compUnitJson = this.objectMapper.createObjectNode();
            compUnitJson.put("node", "TranslationUnit");
            ArrayNode classes = this.objectMapper.createArrayNode();
            compUnitJson.put("classes", classes);
            //we are only interested in ClassOrInterfaceDeclarations-what exact case it is resolved in
            // ClassOrInterfaceDeclaration method itself
            n.getChildNodes().forEach(node ->
                    {
                        if (node instanceof ClassOrInterfaceDeclaration) {
                            ClassOrInterfaceDeclaration classNode = (ClassOrInterfaceDeclaration) node;
                            classes.add(this.visit(classNode, null));
                        }
                    }
            );

            ArrayNode interfaces = this.objectMapper.createArrayNode();
            compUnitJson.put("interfaces", interfaces);
            //we are only interested in ClassOrInterfaceDeclarations-what exact case it is resolved in
            // ClassOrInterfaceDeclaration method itself
            n.getChildNodes().forEach(node ->
                    {
                        if (node instanceof ClassOrInterfaceDeclaration) {
                            ClassOrInterfaceDeclaration classNode = (ClassOrInterfaceDeclaration) node;
                            if (classNode.isInterface())
                                interfaces.add(this.visit(classNode, null));
                        }
                    }
            );

            compUnitJson.put("functions", this.objectMapper.createArrayNode());
            compUnitJson.put("globals", this.objectMapper.createArrayNode());
            return compUnitJson;
        }
        ArrayNode classes = (ArrayNode) arg.get("classes");
        n.findAll(ClassOrInterfaceDeclaration.class).forEach(classOrInterfaceDeclaration -> {
            if (!classOrInterfaceDeclaration.isInterface())
            classes.add(this.visit(classOrInterfaceDeclaration,null));
        });

        ArrayNode interfaces = (ArrayNode) arg.get("interfaces");
        n.findAll(ClassOrInterfaceDeclaration.class).forEach(classOrInterfaceDeclaration -> {
            if (classOrInterfaceDeclaration.isInterface())
                interfaces.add(this.visit(classOrInterfaceDeclaration,null));
        });
        return arg;
    }
    @Override
    public JsonNode visit(ForStmt n, JsonNode arg) {
        ObjectNode forStmtJson = this.objectMapper.createObjectNode();
        forStmtJson.put("node","ForStmt");
        //InitPart

        //Check if there is something in this part
        if (n.getInitialization().size() == 0) {
            forStmtJson.put("init", NullNode.getInstance());
        }else {
            //In this phase,we take only first expression,ForStm in init part has Statement,so if there is assigntExpr,
            //or UnaryExpr,whatewer we need to create virtual ExpressionStmt,if there is Declaration expression visit method
            //of this node returns proper format-either DefStmt or LocalVarDefStmt
            Expression initExpr = n.getInitialization().get(0);

            if (initExpr instanceof VariableDeclarationExpr) {
                forStmtJson.put("init",initExpr.accept(this,null));

            } else {
                //If expression inside is not VariableDeclarationExpr,create virtual ExpressionStmt
                ObjectNode exprStmtJson = this.objectMapper.createObjectNode();
                exprStmtJson.put("node", "ExpressionStmt");
                exprStmtJson.put("expression", initExpr.accept(this, null));

                forStmtJson.put("init", exprStmtJson);


            }
        }

        //Condition part
        Optional<Expression> condition = n.getCompare();
        forStmtJson.put("condition", condition.isPresent() ?
                                                condition.get().accept(this,null) :
                                                NullNode.getInstance());


        //Update part
        //Same as with init part we take only first expression now,later it can be solved by implementing
        //logic with BinOpExpr with comma

        //First check count of expressions
        if (n.getUpdate().size() == 0){
            forStmtJson.put("step", NullNode.getInstance());
        }else{
            //Create virtual ExpressionStmt,
            ObjectNode exprStmtJson = this.objectMapper.createObjectNode();
            exprStmtJson.put("node", "ExpressionStmt");
            exprStmtJson.put("expression", n.getUpdate().get(0).accept(this, null));

            forStmtJson.put("step", exprStmtJson);
        }

        forStmtJson.set("body",n.getBody().accept(this,null));

        return forStmtJson;
    }

    @Override
    public JsonNode visit(ThrowStmt n, JsonNode arg) {
        ObjectNode throwStatementJson = objectMapper.createObjectNode();
        throwStatementJson.put("node","ThrowStmt");

        throwStatementJson.put("expression",n.getExpression().accept(this,null));
        return throwStatementJson;
    }



    @Override
    public JsonNode visit(SuperExpr n, JsonNode arg) {
        return super.visit(n, arg);
    }

    @Override
    public JsonNode visit(ExplicitConstructorInvocationStmt n, JsonNode arg) {
        ObjectNode baseInitStmtJson = this.objectMapper.createObjectNode();
        baseInitStmtJson.put("node","BaseInitializerStmt");

        //need to find out if there is anncestor ClassOrInterfaceDeclaration,because we want name of class itself
        //or name of anncestor of the class,we consider only theese options in ASTFRI
        Optional<ClassOrInterfaceDeclaration> anncestor = n.findAncestor(ClassOrInterfaceDeclaration.class);

        if (anncestor.isPresent()) {
            ClassOrInterfaceDeclaration classDec = anncestor.get();
            //check if constructor was called with this

            if (n.isThis()) {
                baseInitStmtJson.put("base",classDec.getNameAsString());
            //or it is with super,we dont consider any other option
            }else{
                baseInitStmtJson.put("base", classDec.getExtendedTypes().get(0).getNameAsString());
            }

        }


        ArrayNode arguments = this.objectMapper.createArrayNode();
        baseInitStmtJson.put("arguments",arguments);

        n.getArguments().forEach(argument -> arguments.add(argument.accept(this,null)));

        return baseInitStmtJson;
    }

    @Override
    public JsonNode visit(ConstructorDeclaration n, JsonNode arg) {

        ObjectNode constructorDefStmtJson = this.objectMapper.createObjectNode();
        constructorDefStmtJson.put("node","ConstructorDefStmt");
        constructorDefStmtJson.put("owner", n.getName().asString());

        ArrayNode params = this.objectMapper.createArrayNode();
        constructorDefStmtJson.put("parameters",params);

        n.getParameters().forEach(param -> params.add(this.visit(param,null)));
        //Need to check in body if there are some ExplicitConstructorInvocationStmt,because they cant be part of
        //body but we need them as separated statemets-see BaseInitializerStmt in ASTFRI
        ArrayNode baseInitializers = this.objectMapper.createArrayNode();
        constructorDefStmtJson.put("base_initializers",baseInitializers);
        n.getBody().getStatements().forEach(stmt -> {
            if (stmt instanceof ExplicitConstructorInvocationStmt)
                baseInitializers.add(stmt.accept(this,null));

        });

        constructorDefStmtJson.put("body",this.visit(n.getBody(),null));

        var access = n.getAccessSpecifier();
        constructorDefStmtJson.put("access",access == AccessSpecifier.NONE ? "internal" :
                                                              access.asString());

        return constructorDefStmtJson;
    }

    @Override
    public JsonNode visit(TryStmt n, JsonNode arg) {
        return super.visit(n, arg);

    }

    // Equivalent for NewExpr in ASTFri
    @Override
    public JsonNode visit(ObjectCreationExpr n, JsonNode arg) {
        //This node is created,because in ASTFRI ,there is NewExpr which is parent Node for ConstructorCallExpr
        ObjectNode newExprJson = this.objectMapper.createObjectNode();
        newExprJson.put("node","NewExpr");

        //Object representing ConstructorCallExpr
        ObjectNode constructorCallExprJson = this.objectMapper.createObjectNode();
        constructorCallExprJson.put("node","ConstructorCallExpr" );
        constructorCallExprJson.put("type",this.visit(n.getType(),null));

        ArrayNode arguments = this.objectMapper.createArrayNode();
        constructorCallExprJson.put("arguments",arguments);
        n.getArguments().forEach(argument -> arguments.add(argument.accept(this,null)));

        newExprJson.put("init",constructorCallExprJson);

        return newExprJson;
    }

    @Override
    public JsonNode visit(BlockStmt n, JsonNode arg) {
        ObjectNode blockStatementJson = objectMapper.createObjectNode();
        blockStatementJson.put("node","CompoundStmt");
        ArrayNode statements = objectMapper.createArrayNode();
        blockStatementJson.put("statements",statements);
        this.lasBlockStmtJson = lasBlockStmtJson;

        n.getStatements().forEach(statement -> {
            //check for ExplicitConstructorInvocationStmt ,we dont want to this node to be added among
            // statements in BlockStmt, look at the way how ConstructorDefStmt in ASTFRI looks like
            if (!(statement instanceof ExplicitConstructorInvocationStmt)) {
                statements.add(statement.accept(this, null));
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

    /*This method returns either DefStmt Json object representation-if there are multiple variables declared inside ,
     *or
     *LocalVarDefStmt only one variable inside this node
     */
    @Override
    public JsonNode visit(VariableDeclarationExpr n, JsonNode arg) {
        var variables = n.getVariables();

        //If there are more than one variable,create DefStmt
        if (variables.size() > 1){
            ObjectNode defStmtJson = this.objectMapper.createObjectNode();
            defStmtJson.put("node","DefStmt");
            //array for particular VarDefStmts
            ArrayNode definitions = this.objectMapper.createArrayNode();
            defStmtJson.put("definitions", definitions);

            variables.forEach(variable -> {
                //object for LocalVarDefStmt
                ObjectNode localVarDefStmtJson = this.objectMapper.createObjectNode();
                localVarDefStmtJson.put("node","LocalVarDefStmt");

                //visit variable and add properties to Json object representation of LocalVarDefStmt
                this.visit(variable,localVarDefStmtJson);

                //add completed representation of LocalVarDefStm into array
                definitions.add(localVarDefStmtJson);

            });
            return defStmtJson;
        }

        ObjectNode localVarDefStmtJson = this.objectMapper.createObjectNode();
        localVarDefStmtJson.put("node","LocalVarDefStmt");
        //add to localVarDefStmtJson properties
        this.visit(variables.get(0),localVarDefStmtJson);

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

        varDefStmtJson.put("initializer", initialzer.isPresent()
                                        ? initialzer.get().accept(this,null) :
                                        NullNode.getInstance());

        //returns input output parameter
        return arg;


    }

    @Override
    public JsonNode visit(BreakStmt n, JsonNode arg) {
        ObjectNode breakStmtjson = this.objectMapper.createObjectNode();
        breakStmtjson.put("node","BreakStmt");
        return breakStmtjson;
    }

    @Override
    public JsonNode visit(ContinueStmt n, JsonNode arg) {
        ObjectNode continueStmtjson = this.objectMapper.createObjectNode();
        continueStmtjson.put("node","ContinueStmt");
        return continueStmtjson;
    }

    @Override
    public JsonNode visit(PrimitiveType n, JsonNode arg) {

        switch (n.getType()){
            case INT -> {return  this.objectMapper.createObjectNode().put("node","Int");}
            case BYTE -> {return this.objectMapper.createObjectNode().put("node","Int");}
            case SHORT -> {return this.objectMapper.createObjectNode().put("node","Int");}
            case LONG ->  {return this.objectMapper.createObjectNode().put("node","Int");}
            case CHAR -> {return this.objectMapper.createObjectNode().put("node","Char");}
            case FLOAT -> {return this.objectMapper.createObjectNode().put("node","Float");}
            case DOUBLE -> {return this.objectMapper.createObjectNode().put("node","Float");}
            case BOOLEAN -> {return  this.objectMapper.createObjectNode().put("node","Bool");}
            default -> {return this.objectMapper.createObjectNode().put("node","Unknown");}
        }
    }

    @Override
    public JsonNode visit(Parameter n, JsonNode arg) {
            ObjectNode parameterNodeJson = objectMapper.createObjectNode();
                parameterNodeJson.put("node","ParamVarDefStmt");
                parameterNodeJson.put("name",n.getNameAsString());
                parameterNodeJson.put("type",n.getType().accept(this,null));
                parameterNodeJson.put("initializer",NullNode.getInstance());


        return parameterNodeJson;

    }
    //Class or Interface types are represented in ASTFRI as IndirectionType,so as a result we want
    //IndirectionType , which is pointing to ClassOrInterface type
    // with two exceptions,and thhose are when this ClassType is used in context of ObjecCreationExpr or when
    // this node is used in context of inheritance/implements-in this cases
    //we want this type to be represented as UserType,to recognise this context we check if parent node
    //is ObjectCreationExpr or ClassOrInterfaceDeclaration,and depending on result of this check we apply different logic
    @Override
    public JsonNode visit(ClassOrInterfaceType n, JsonNode arg) {
        ObjectNode classTypeJson = this.objectMapper.createObjectNode();
        var parent = n.getParentNode();

        if(parent.isPresent() && (parent.get() instanceof ObjectCreationExpr ||
                                    parent.get() instanceof ClassOrInterfaceDeclaration)) {

            classTypeJson.put("node", "User");
            classTypeJson.put("name", n.getNameAsString());

        }else {

            classTypeJson.put("node", "Indirection");

            ObjectNode inderectTypeJson = this.objectMapper.createObjectNode();
            inderectTypeJson.put("node", "User");
            inderectTypeJson.put("name", n.getNameAsString());
            classTypeJson.put("indirect", inderectTypeJson);
        }
        return classTypeJson;
    }

    @Override
    public JsonNode visit(ClassOrInterfaceDeclaration n, JsonNode arg) {
        ObjectNode classOrInterfDeclJson = objectMapper.createObjectNode();

        //check if this node is interface-different logic apply for class and interface
        if (n.isInterface()){
            classOrInterfDeclJson.put("node","InterfaceDefStmt");
            classOrInterfDeclJson.put("name", n.getNameAsString());

            ArrayNode methods = this.objectMapper.createArrayNode();
            classOrInterfDeclJson.put("methods", methods);
            n.getMethods().forEach(m -> methods.add(visit(m, null)));

            ArrayNode generic_params = objectMapper.createArrayNode();
            classOrInterfDeclJson.put("generic_parameters", generic_params);

            n.getTypeParameters().forEach(gen_param -> {

                generic_params.add(this.visit(gen_param, null));
            });

            ArrayNode bases = this.objectMapper.createArrayNode();
            classOrInterfDeclJson.put("bases",bases);

            n.getExtendedTypes().forEach(type -> {
                bases.add(this.synteticNodeCreator.createInterfaceDefStmt(type));
            });



            return classOrInterfDeclJson;
        }else {
            classOrInterfDeclJson.put("node","ClassDefStmt");
            classOrInterfDeclJson.put("name", n.getNameAsString());
        /*
        ArrayNode modifiers = objectMapper.createArrayNode();
        classOrInterfDeclJson.put("modifiers",modifiers);
        n.getModifiers().forEach(mod -> modifiers.add(this.visit(mod,null)));*/

            ArrayNode attributes = objectMapper.createArrayNode();
            classOrInterfDeclJson.put("attributes", attributes);

            n.getFields().forEach(field -> {
                this.createVarDefStmts(field, "Member").forEach(variable -> {
                    var access = field.getAccessSpecifier();

                    variable.put("acces", access == AccessSpecifier.NONE ?
                            "internal" : access.asString());
                    attributes.add(variable);

                });

            });
            //part for constructors
            ArrayNode constructors = this.objectMapper.createArrayNode();
            classOrInterfDeclJson.put("constructors", constructors);
            n.getConstructors().forEach(constr -> constructors.add(this.visit(constr, null)));


            classOrInterfDeclJson.put("destructors", this.objectMapper.createArrayNode());

            ArrayNode methods = objectMapper.createArrayNode();
            classOrInterfDeclJson.put("methods", methods);
            n.getMethods().forEach(m -> methods.add(visit(m, null)));

            ArrayNode generic_params = objectMapper.createArrayNode();
            classOrInterfDeclJson.put("generic_parameters", generic_params);

            n.getTypeParameters().forEach(gen_param -> {

                generic_params.add(this.visit(gen_param, null));
            });

            ArrayNode interfaces = this.objectMapper.createArrayNode();
            classOrInterfDeclJson.set("interfaces",interfaces);
            n.getImplementedTypes().forEach(type ->
                    interfaces.add(this.synteticNodeCreator.createInterfaceDefStmt(type)));

            ArrayNode bases = this.objectMapper.createArrayNode();
            classOrInterfDeclJson.set("bases",bases);
            n.getExtendedTypes().forEach(type ->
                    this.synteticNodeCreator.createClassDefStmt(type));

            return classOrInterfDeclJson;
        }
    }


    @Override
    public JsonNode visit(ArrayAccessExpr n, JsonNode arg) {
         ObjectNode jsonArrAccExpr = objectMapper.createObjectNode();
         jsonArrAccExpr.put("node","ArrayAccesExpr");
         n.getName().ifNameExpr( e -> jsonArrAccExpr.put("name",e.asNameExpr().toString()));
         jsonArrAccExpr.put("index",n.getIndex().asIntegerLiteralExpr().asNameExpr().toString());
         return jsonArrAccExpr;
    }

    @Override
    public JsonNode visit(AssignExpr n, JsonNode arg) {
        ObjectNode assignExprJson = objectMapper.createObjectNode();

        assignExprJson.put("node","BinOpExpr");
        assignExprJson.put("left",n.getTarget().accept(this,null));
        assignExprJson.put("right",n.getValue().accept(this,null));
        assignExprJson.put("operator",n.getOperator().asString());


        return assignExprJson;
    }


    @Override
    public JsonNode visit(BinaryExpr n, JsonNode arg) {
        ObjectNode binaryExprJson = objectMapper.createObjectNode();
        binaryExprJson.put("node","BinOpExpr");
        Expression left = n.getLeft();
        binaryExprJson.put("left",left.accept(this,null));

        Expression right = n.getRight();
        binaryExprJson.put("right",right.accept(this,null));

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
        unaryExprJson.put("argument",expr.accept(this,null));

        return unaryExprJson;
    }

    @Override
    public JsonNode visit(ConditionalExpr n, JsonNode arg) {
        ObjectNode condExprJson = this.objectMapper.createObjectNode();
        condExprJson.put("node","IfExpr");

        Expression cond = n.getCondition();
        condExprJson.put("condition",cond.accept(this,null));
        Expression then = n.getThenExpr();
        condExprJson.put("ifTrue",then.accept(this,null));

        Expression or = n.getElseExpr();
        condExprJson.put("ifFalse",or.accept(this,null));
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
        booleanLitExprJson.put("node","BoolLitExpr");
        booleanLitExprJson.put("value",n.getValue());
        return booleanLitExprJson;
    }

    @Override
    public JsonNode visit(DoubleLiteralExpr n, JsonNode arg) {
        ObjectNode doubleLitExprJson = objectMapper.createObjectNode();
        doubleLitExprJson.put("node","FloatLitExpr");
        doubleLitExprJson.put("value",Double.parseDouble(n.getValue()));
        return doubleLitExprJson;
    }

    @Override
    public JsonNode visit(IntegerLiteralExpr n, JsonNode arg) {
        ObjectNode intLitExprJson = objectMapper.createObjectNode();
        intLitExprJson.put("node","IntLitExpr");
        intLitExprJson.put("value",Integer.parseInt(n.getValue()));
        return intLitExprJson;
    }

    @Override
    public JsonNode visit(LongLiteralExpr n, JsonNode arg) {
        //return this.createJsonLiteralExpr("LongLiteralExpr",n.getValue());
        ObjectNode intLitExprJson = objectMapper.createObjectNode();
        intLitExprJson.put("node","IntLitExpr");
        intLitExprJson.put("value",Integer.parseInt(n.getValue()));
        return intLitExprJson;
    }

    @Override
    public JsonNode visit(NullLiteralExpr n, JsonNode arg) {
        ObjectNode nullLitExprJson = objectMapper.createObjectNode();
        nullLitExprJson.put("node","NullLitExpr");
        return nullLitExprJson;
    }

    @Override
    public JsonNode visit(StringLiteralExpr n, JsonNode arg) {
        ObjectNode stringLitExprJson = objectMapper.createObjectNode();
        stringLitExprJson.put("node","StringLitExpr");
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
        return this.referenceTypeResolver.determineReferenceType(n).toJson(this.objectMapper);

    }

    @Override
    public JsonNode visit(MethodCallExpr n, JsonNode arg) {
        ObjectNode methodCallExprJson = objectMapper.createObjectNode();
        methodCallExprJson.put("node","MethodCallExpr");
        methodCallExprJson.put("name",n.getNameAsString());

        if(n.getScope().isPresent()){
                methodCallExprJson.put("owner", n.getScope().get().accept(this,null));

        }else{
            //if there is no scope, method owner will be either ThisExpr or ClassRefExpr-it s static method
            // - implemented in ReferenceTypeResolver-method resolveMethodOwner
            var methodOwner = this.referenceTypeResolver.resolveMethodOwner(n);
            methodCallExprJson.put("owner",methodOwner.isPresent()?
                                                        methodOwner.get().toJson(this.objectMapper) :
                                                         null);

        }


        ArrayNode arguments = objectMapper.createArrayNode();
        n.getArguments().forEach(e -> arguments.add(e.accept(this,null)));

        methodCallExprJson.put("arguments",arguments);
        return methodCallExprJson;
    }

    @Override
    public JsonNode visit(FieldAccessExpr n, JsonNode arg) {
        return new Reference(Reference.ReferenceType.MEMBER_VAR_REFERENCE,
                n.getNameAsString()).toJson(this.objectMapper);

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
    //this method is created in order to make it possible to create one Compilation unit representation of whole
    //directory ,the root node of ASTFRI library is Translation Unit,so we want to create logic for representation
    //of whole parsed directory,which consists of multiple Compilation units as one CompilationUnit
    public String convertToJson(SourceRoot sourceRoot){
        ObjectNode jsonRepresentationSourceRoot = this.objectMapper.createObjectNode();
        jsonRepresentationSourceRoot.put("node","TranslationUnit");
        jsonRepresentationSourceRoot.set("classes",this.objectMapper.createArrayNode());
        jsonRepresentationSourceRoot.set("interfaces",this.objectMapper.createArrayNode());
        jsonRepresentationSourceRoot.set("functions",this.objectMapper.createArrayNode());
        jsonRepresentationSourceRoot.set("globals",this.objectMapper.createArrayNode());

        sourceRoot.getCompilationUnits().forEach(cu ->{
            this.visit(cu,jsonRepresentationSourceRoot);
        });
        try{
            DefaultPrettyPrinter printer = new DefaultPrettyPrinter();
            printer.indentArraysWith(DefaultPrettyPrinter.NopIndenter.instance);
            ObjectWriter writer = objectMapper.writer(printer);


            return writer.writeValueAsString(jsonRepresentationSourceRoot);

        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return null;
        }
    }
    private <T extends NodeWithVariables<?>> List<ObjectNode> createVarDefStmts(T expr,String type) {
        List<ObjectNode> varDefstmts = new ArrayList<>();
        expr.getVariables().forEach(var -> {
            ObjectNode varDefstmtJson = this.objectMapper.createObjectNode();
            varDefstmtJson.put("node", type+"VarDefStmt");
            this.visit(var, varDefstmtJson);
            varDefstmts.add(varDefstmtJson);
        });
        return varDefstmts;
    }

}
