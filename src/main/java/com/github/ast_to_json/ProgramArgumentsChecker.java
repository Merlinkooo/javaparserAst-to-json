package com.github.ast_to_json;

import java.io.File;
import java.util.Arrays;

public class ProgramArgumentsChecker {
    private static String[] allowedValues = new String[]{"-f","-d","-p"};



    public static void checkNumberOfArguments(String[] args){
        if (args.length < 3){
            StringBuilder builder = new StringBuilder();
            builder.append("Not enough arguments given\n");
            builder.append("Expected number of arguments 3\n");
            builder.append("Given number of arguments " + args.length);
            throw new RuntimeException(builder.toString());
        }
    }

    public static void checkFirstArgument(String argument){

        if (Arrays.stream(ProgramArgumentsChecker.allowedValues).noneMatch(value -> value.equals(argument))) {
            StringBuilder builder = new StringBuilder();
            builder.append("Inncorect first argument\n");
            builder.append("Allowed values for first argument : -f -d -p\n");
            builder.append("Given argument:  + " + argument);
            throw new RuntimeException(builder.toString());
        }
    }
    public static File checkSourceFile(String filePath,String option){
        File source = new File(filePath);
        if((option.equals("-d") || option.equals("-p")) && source.isFile()){
            StringBuilder builder = new StringBuilder();
            builder.append("Given option " + option + "requires directory as source\n");
            builder.append("File with path " +  filePath + "is file,not directory");
            throw new RuntimeException(builder.toString());
        }
        if(option.equals("-f") && source.isDirectory()){
            StringBuilder builder = new StringBuilder();
            builder.append("Given option " + option + "requires file as source\n");
            builder.append("File with path " +  filePath + "is directory,not file");
            throw new RuntimeException(builder.toString());
        }
        return source;
    }

    public static File  checkOutputFile(String filePath){
        File outputFile = new File(filePath);
        if (outputFile.isDirectory()){
            StringBuilder builder = new StringBuilder();
            builder.append("Output file with path " +  filePath + "is directory,not file");
            throw new RuntimeException(builder.toString());
        }
        return outputFile;
    }
}
