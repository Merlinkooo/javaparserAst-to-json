package examples;

import JsonASTDeserialiser.JsonAstDeserialiser;
import JsonASTDeserialiser.ReferenceTypeResolver;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.visitor.GenericVisitorAdapter;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;

import java.io.*;

public class Test {

    public static void main(String[] args) throws IOException {
        /*
        CombinedTypeSolver typeSolver = new CombinedTypeSolver();
        typeSolver.add(new ReflectionTypeSolver()); // Použije reflexiu pre bežné Java triedy
        typeSolver.add(new JavaParserTypeSolver(new File("src"))); // Analyzuje lokálny kód


        JavaSymbolSolver symbolSolver = new JavaSymbolSolver(typeSolver);
        StaticJavaParser.getConfiguration().setSymbolResolver(symbolSolver);*/
        //JsonAstDeserialiser visitor = new JsonAstDeserialiser("src/main/java/examples/ReversePolishNotation.java");
        File file = new File("src/main/java/examples/ReversePolishNotation.java");
        CompilationUnit cu = StaticJavaParser.parse(file);
        JsonAstDeserialiser deserialiser = new JsonAstDeserialiser(file);

        try(FileWriter fileWriter = new FileWriter(new File("src/main/java/examples/Output.json"))) {
            String jsonFormat = deserialiser.convertToJson(cu);
            fileWriter.write(jsonFormat);
            System.out.println(jsonFormat);
        }catch (Exception e){
            e.printStackTrace();
        }



    }
}
