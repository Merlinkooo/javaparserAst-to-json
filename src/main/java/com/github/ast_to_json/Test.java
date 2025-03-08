package com.github.ast_to_json;

import JsonASTDeserialiser.JsonAstDeserialiser;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.utils.SourceRoot;

import java.io.*;
import java.nio.file.Paths;

public class Test {
    private static int cislo=10;
    public static void main(String[] args) throws IOException {

        /*
        CombinedTypeSolver typeSolver = new CombinedTypeSolver();
        typeSolver.add(new ReflectionTypeSolver()); // Použije reflexiu pre bežné Java triedy
        typeSolver.add(new JavaParserTypeSolver(new File("src"))); // Analyzuje lokálny kód


        JavaSymbolSolver symbolSolver = new JavaSymbolSolver(typeSolver);
        StaticJavaParser.getConfiguration().putSymbolResolver(symbolSolver);*/
        //JsonAstDeserialiser visitor = new JsonAstDeserialiser("src/main/java/examples/ReversePolishNotation.java");
        File file = new File("src/main/java/examples");

        ParserConfiguration conf = new ParserConfiguration();
        conf.setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21);
        StaticJavaParser.setConfiguration(conf);


        SourceRoot sourceRoot=new SourceRoot(Paths.get("src/main/java/examples"),conf);
        sourceRoot.tryToParse();





        JsonAstDeserialiser deserialiser = new JsonAstDeserialiser(file);
        try(FileWriter fileWriter = new FileWriter(new File("target/generated-sources/Output.json"))) {
            String jsonFormat = deserialiser.convertToJson(sourceRoot);
            fileWriter.write(jsonFormat);
            System.out.println(jsonFormat);
        }catch (Exception e){
            e.printStackTrace();
        }



    }
}
