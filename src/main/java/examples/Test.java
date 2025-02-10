package examples;

import JsonASTDeserialiser.JsonAstDeserialiser;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class Test {

    public static void main(String[] args) throws IOException {
        CompilationUnit cu = StaticJavaParser.parse(new File("src/main/java/examples/ReversePolishNotation.java"));
        VoidVisitorAdapter visitor = new JsonAstDeserialiser();
        try(FileWriter fileWriter = new FileWriter(new File("src/main/java/examples/Output.json"))) {
            visitor.visit(cu,fileWriter);
        }catch (Exception e){
            e.printStackTrace();
        }


    }
}
