package com.github.ast_to_json;

import JsonASTDeserialiser.JsonAstDeserialiser;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import com.github.javaparser.symbolsolver.utils.SymbolSolverCollectionStrategy;
import com.github.javaparser.utils.SourceRoot;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ProjectRoot {
    public static void main(String[] args) throws IOException {

        File file = new File("src/main/java");
        Path root = Paths.get("src\\main\\java");
        SymbolSolverCollectionStrategy collectionStrategy = new SymbolSolverCollectionStrategy();

        CombinedTypeSolver typeSolver = new CombinedTypeSolver();
        typeSolver.add(new ReflectionTypeSolver());
        typeSolver.add(new JavaParserTypeSolver(file));


        JavaSymbolSolver symbolSolver = new JavaSymbolSolver(typeSolver);

        collectionStrategy.getParserConfiguration().setSymbolResolver(symbolSolver);
        collectionStrategy.getParserConfiguration().setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21);

        com.github.javaparser.utils.ProjectRoot projectRoot =collectionStrategy.collect(root);



         try(FileWriter writer = new FileWriter(new File("target/generated-sources/Output.json"))) {

             JsonAstDeserialiser deserialiser = new JsonAstDeserialiser(new File(root.toString()));
             projectRoot.getSourceRoots().forEach(sr -> {
                 try {
                     sr.tryToParse();
                     writer.write(deserialiser.convertToJson(sr));
                 } catch (IOException e) {
                     throw new RuntimeException(e);
                 }
             });
         }catch (Exception e){
             e.printStackTrace();
         }
    }
}
