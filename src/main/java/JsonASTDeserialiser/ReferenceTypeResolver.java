package JsonASTDeserialiser;

import Referencies.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.resolution.TypeSolver;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;

import java.io.File;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class ReferenceTypeResolver {
    private ObjectMapper objectMapper;



    public ReferenceTypeResolver(ObjectMapper objectMapper,File file) {
        this.objectMapper = objectMapper;
        //Folowing part of code is added in order to determine object Types of name references,
        // which can be useful in future

        CombinedTypeSolver typeSolver = new CombinedTypeSolver();
        typeSolver.add(new ReflectionTypeSolver());
        typeSolver.add(new JavaParserTypeSolver(Paths.get(file.getParentFile().getPath())));

        JavaSymbolSolver symbolSolver = new JavaSymbolSolver(typeSolver);
        StaticJavaParser.getConfiguration().setSymbolResolver(symbolSolver);
    }

    /*Returns Reference if there is some match
    * */
    public ReferenceExpression determineReferenceType(NameExpr name) {
        //if (name instanceof ThisExpr) return new Reference(Reference.ReferenceType.THIS_REFERENCE,"this",this.objectMapper);

        //Tries ty find the nearest anncestor for node of type either MethodDecalaration or ConstructorDecl
        Optional<Node> parentMethodOrConstructor = name.findAncestor(MethodDeclaration.class)
                .map(m -> (Node) m)
                .or(() -> name.findAncestor(ConstructorDeclaration.class));


        if (parentMethodOrConstructor.isPresent()) {
            Node methodOrConstructor = parentMethodOrConstructor.get();

            //Tries to find out if name of parameter of method or constructor declaration matches with name
            if (methodOrConstructor.findAll(Parameter.class).stream().anyMatch(p -> p.getNameAsString().equals(name.toString()))) {
                return new ParamVarRefExpr(name.getNameAsString());
            }

            // Tries to find out if nameExpr is equal to name one of the local variables
            if (methodOrConstructor.findAll(VariableDeclarator.class).stream().anyMatch(v -> v.getNameAsString().equals(name.toString()))) {
                return new LocalVarRefExpr(name.getNameAsString());
            }
        }

        //Check for match of the namexpr with names of the attributes
        Optional<ClassOrInterfaceDeclaration> classDecl = name.findAncestor(ClassOrInterfaceDeclaration.class);
        if (classDecl.isPresent()) {
            if (classDecl.get().findAll(FieldDeclaration.class).stream()
                    .flatMap(field -> field.getVariables().stream())
                    .anyMatch(v -> v.getNameAsString().equals(name.toString()))) {

                return new MemberVarRefExpr(name.getNameAsString(),ThisExpr.getInstance());
            }
        }
        //If there was no match with names till this point ,we can assume nameExpr given is name of class
        return new ClassRefExpr(name.getNameAsString());
    }
    //To do- check number and types of parameters
    public Optional<ReferenceExpression> resolveMethodOwner(MethodCallExpr callExpr){
        Optional<ClassOrInterfaceDeclaration> classDecl = callExpr.findAncestor(ClassOrInterfaceDeclaration.class);


        //find method with same name as methodCallExpr and figure out,if this method has static keyword among
        //modifiers
        if(classDecl.isPresent()){
            var methods = classDecl.get().getMethods();

            //seach for method declaration  with same as method call expr in parameter and then search
            //for static keyword among modifiers
            for (int i = 0; i < methods.size(); i++) {
                var method = methods.get(i);
                if (method.getName().asString().equals(callExpr.getNameAsString())) {

                    if (method.isStatic()) {
                        return Optional.of(new ClassRefExpr(classDecl.get().getNameAsString()));
                    } else {
                        return Optional.of(ThisExpr.getInstance());
                    }


                }
            }


        }
        return Optional.empty();
    }
}
