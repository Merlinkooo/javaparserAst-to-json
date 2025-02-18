package examples;

import JsonASTDeserialiser.JsonAstDeserialiser;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.ThisExpr;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.util.Optional;

public class TestWithSymbolSolver {

        public static void main(String[] args) throws FileNotFoundException {
            /*
            CombinedTypeSolver typeSolver = new CombinedTypeSolver();
            typeSolver.add(new ReflectionTypeSolver());
            typeSolver.add(new JavaParserTypeSolver(new File("src")));

            JavaSymbolSolver symbolSolver = new JavaSymbolSolver(typeSolver);
            StaticJavaParser.getConfiguration().setSymbolResolver(symbolSolver);
            */

            // Parsovanie Java súboru
            CompilationUnit cu = StaticJavaParser.parse(new File("src/main/java/examples/Example.java"));

            // Prechádzanie všetkých volaní metód
            cu.findAll(MethodCallExpr.class).forEach(methodCall -> {
                System.out.println("Metóda: " + methodCall.getName());

                // Ak má metóda volajúci objekt (napr. obj.metoda())
                if (methodCall.getScope().isPresent()) {
                    Expression scope = methodCall.getScope().get();
                    determineReferenceType(methodCall, scope);
                    try {
                        ResolvedType resolvedType = scope.calculateResolvedType();
                        System.out.println("  Volaná na objekte typu: " + resolvedType.describe());

                        // Určíme typ referencie (či je to atribút, lokálna premenná alebo parameter)
                        determineReferenceType(methodCall, scope);
                    } catch (UnsolvedSymbolException e) {
                        System.out.println("  Nepodarilo sa vyriešiť typ.");
                    }
                } else {
                    System.out.println("  Metóda je volaná bez objektu (pravdepodobne statická).");
                }
            });

            }
    private static void determineReferenceType(MethodCallExpr methodCall, Expression scope) {
        Optional<Node> parentMethodOrConstructor = methodCall.findAncestor(MethodDeclaration.class)
                .map(m -> (Node) m)
                .or(() -> methodCall.findAncestor(ConstructorDeclaration.class));

        if (parentMethodOrConstructor.isPresent()) {
            Node methodOrConstructor = parentMethodOrConstructor.get();

            // Skontrolujeme, či sa referenciu nachádza medzi parametrami metódy
            if (methodOrConstructor.findAll(Parameter.class).stream().anyMatch(p -> p.getNameAsString().equals(scope.toString()))) {
                System.out.println("  🔹 Objekt je PARAMETER metódy.");
                return;
            }

            // Skontrolujeme, či sa referenciu nachádza medzi lokálnymi premennými
            if (methodOrConstructor.findAll(VariableDeclarator.class).stream().anyMatch(v -> v.getNameAsString().equals(scope.toString()))) {
                System.out.println("  🔹 Objekt je LOKÁLNA PREMENNÁ.");
                return;
            }
        }

        // Skontrolujeme, či sa referenciu nachádza medzi atribútmi triedy
        Optional<ClassOrInterfaceDeclaration> classDecl = methodCall.findAncestor(ClassOrInterfaceDeclaration.class);
        if (classDecl.isPresent()) {
            if (classDecl.get().findAll(FieldDeclaration.class).stream()
                    .flatMap(field -> field.getVariables().stream())
                    .anyMatch(v -> v.getNameAsString().equals(scope.toString()))) {
                System.out.println("  🔹 Objekt je ATRIBÚT triedy.");
                return;
            }
        }

        System.out.println("  ⚠️ Typ referencie neznámy.");
        }
}
