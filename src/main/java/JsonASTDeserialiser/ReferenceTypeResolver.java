package JsonASTDeserialiser;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ThisExpr;
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
    public Reference determineReferenceType(NameExpr name) {
        //if (name instanceof ThisExpr) return new Reference(Reference.ReferenceType.THIS_REFERENCE,"this",this.objectMapper);

        //Tries ty find the nearest anncestor for node ,is looking for MethodDecalaration and constructorDecl
        Optional<Node> parentMethodOrConstructor = name.findAncestor(MethodDeclaration.class)
                .map(m -> (Node) m)
                .or(() -> name.findAncestor(ConstructorDeclaration.class));

        if (parentMethodOrConstructor.isPresent()) {
            Node methodOrConstructor = parentMethodOrConstructor.get();

            //Tries to find out if name of parameter of method or constructor declaration matches with name
            if (methodOrConstructor.findAll(Parameter.class).stream().anyMatch(p -> p.getNameAsString().equals(name.toString()))) {
                System.out.println("  🔹 Objekt je PARAMETER metódy.");
                return new Reference(Reference.ReferenceType.PARAM_REFERENCE, name.toString(), this.objectMapper);
            }

            // Skontrolujeme, či sa referenciu nachádza medzi lokálnymi premennými
            if (methodOrConstructor.findAll(VariableDeclarator.class).stream().anyMatch(v -> v.getNameAsString().equals(name.toString()))) {
                System.out.println("  🔹 Objekt je LOKÁLNA PREMENNÁ.");
                return new Reference(Reference.ReferenceType.LOCAL_VAR_REFERENCE, name.toString(), this.objectMapper);
            }
        }

        // Skontrolujeme, či sa referenciu nachádza medzi atribútmi triedy
        Optional<ClassOrInterfaceDeclaration> classDecl = name.findAncestor(ClassOrInterfaceDeclaration.class);
        if (classDecl.isPresent()) {
            if (classDecl.get().findAll(FieldDeclaration.class).stream()
                    .flatMap(field -> field.getVariables().stream())
                    .anyMatch(v -> v.getNameAsString().equals(name.toString()))) {
                System.out.println("  🔹 Objekt je ATRIBÚT triedy.");
                return new Reference(Reference.ReferenceType.MEMBER_VAR_REFERENCE, name.toString(), this.objectMapper);
            }
        }
        return new Reference(Reference.ReferenceType.CLASS_REFERENCE,name.toString(),this.objectMapper);
    }

    public Optional<Reference> resolveMethodOwner(MethodCallExpr callExpr){
        Optional<ClassOrInterfaceDeclaration> classDecl = callExpr.findAncestor(ClassOrInterfaceDeclaration.class);

        AtomicReference<Reference> ref = new AtomicReference<>(null);
        //find method with same name as methodCallExpr and figure out,if this method has static keyword among
        //modifiers
        if (classDecl.isPresent()){
            //This part can be modified to different syntax in order to enable ending of cycle in case there is match
            //with names
            classDecl.get().getMethods().forEach(method -> {
                //check if names are same
                if (method.getName().asString().equals(callExpr.getNameAsString())) {
                    AtomicBoolean isStatic = new AtomicBoolean(false);
                    //check if among modifiers is static keyWord
                    method.getModifiers().forEach( modifier -> {
                        if (modifier.getKeyword().asString().equals("static")) isStatic.set(true);
                    });
                    //if it variable is static is true set reference as ClassReference or set it as ThisReference
                    if(isStatic.get()) {
                        ref.set(new Reference(Reference.ReferenceType.CLASS_REFERENCE,
                                classDecl.get().getNameAsString(), this.objectMapper)) ;
                    }else {
                        ref.set(new Reference(Reference.ReferenceType.THIS_REFERENCE,
                                classDecl.get().getNameAsString(), this.objectMapper));
                    }

                }

            });

        }

        return Optional.of(ref.get());
    }
}
