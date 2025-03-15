package com.github.ast_to_json;


import JsonASTDeserialiser.JsonAstSerialiser;
import com.github.javaparser.*;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.symbolsolver.utils.SymbolSolverCollectionStrategy;
import com.github.javaparser.utils.ProjectRoot;
import com.github.javaparser.utils.SourceRoot;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Main {
    public static void main(String[] args) throws FileNotFoundException {
        ProgramArgumentsChecker.checkNumberOfArguments(args);
        ProgramArgumentsChecker.checkFirstArgument(args[0]);

        File sourceFile = ProgramArgumentsChecker.checkSourceFile(args[1],args[0]);
        File outputFile = ProgramArgumentsChecker.checkOutputFile(args[2]);

        CombinedTypeSolver typeSolver = new CombinedTypeSolver();
        typeSolver.add(new ReflectionTypeSolver());
        typeSolver.add(new JavaParserTypeSolver(sourceFile));
        JavaSymbolSolver symbolSolver = new JavaSymbolSolver(typeSolver);

        ParserConfiguration conf = new ParserConfiguration();
        conf.setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21);
        conf.setSymbolResolver(symbolSolver);

        StaticJavaParser.setConfiguration(conf);

        JsonAstSerialiser serialiser = new JsonAstSerialiser(sourceFile);
        Path path = Paths.get(sourceFile.getPath());

        String jsonFormat="";
        switch (args[0]){
            case "-f":{
                CompilationUnit result = StaticJavaParser.parse(sourceFile);
                serialiser.convertToJson(result);
                break;
            }
            case "-d": {
                SourceRoot sourceRoot = new SourceRoot(path,conf);
                try {
                    sourceRoot.tryToParse();
                    jsonFormat = serialiser.convertToJson(sourceRoot);
                } catch (IOException e) {
                    System.out.println(e);
                }
                break;
            }
            case "-p" : {
                SymbolSolverCollectionStrategy collectionStrategy = new SymbolSolverCollectionStrategy();
                collectionStrategy.getParserConfiguration().setSymbolResolver(symbolSolver);
                collectionStrategy.getParserConfiguration().setLanguageLevel(conf.getLanguageLevel());

                ProjectRoot projectRoot = collectionStrategy.collect(path);

                projectRoot.getSourceRoots().forEach(sourceRoot -> {
                    try {
                        sourceRoot.tryToParse();

                    } catch (IOException e) {
                        System.out.println(e);
                    }

                });
                jsonFormat = serialiser.convertToJson(projectRoot);
            }
        }
        try(FileWriter writer = new FileWriter(outputFile)) {
            writer.write(jsonFormat);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    }
