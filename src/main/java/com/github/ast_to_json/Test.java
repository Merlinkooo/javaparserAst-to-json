package com.github.ast_to_json;

import JsonASTDeserialiser.JsonAstDeserialiser;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import com.github.javaparser.utils.SourceRoot;

import java.io.*;
import java.nio.file.Paths;

public class Test {

    public static void main(String[] args) throws IOException {

        File file = new File("src/main/java/examples");

        CombinedTypeSolver typeSolver = new CombinedTypeSolver();
        typeSolver.add(new ReflectionTypeSolver());
        typeSolver.add(new JavaParserTypeSolver(file));


        JavaSymbolSolver symbolSolver = new JavaSymbolSolver(typeSolver);


        ParserConfiguration conf = new ParserConfiguration();
        conf.setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21);
        conf.setSymbolResolver(symbolSolver);


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
