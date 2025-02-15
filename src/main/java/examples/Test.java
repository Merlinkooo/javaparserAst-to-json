package examples;

import JsonASTDeserialiser.JsonAstDeserialiser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.visitor.GenericVisitorAdapter;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import java.io.*;

public class Test {

    public static void main(String[] args) throws IOException {
        CompilationUnit cu = StaticJavaParser.parse(new File("src/main/java/examples/ReversePolishNotation.java"));
       JsonAstDeserialiser visitor = new JsonAstDeserialiser();

        try(FileWriter fileWriter = new FileWriter(new File("src/main/java/examples/Output.json"))) {
            String jsonFormat = visitor.convertToJson(cu);
            fileWriter.write(jsonFormat);
            System.out.println(jsonFormat);
        }catch (Exception e){
            e.printStackTrace();
        }
        //System.out.println(objectNode);

    }
}
