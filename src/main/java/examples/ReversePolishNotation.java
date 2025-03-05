package examples;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Stack;
import java.util.stream.Stream;

public class ReversePolishNotation<T extends Number> {
    public static int ONE_BILLION=1000000000,e;
    private double memory;
    private String meno;

    public boolean getBool(){
        return true;
    }
    public Double calc(String input){
        String[] tokens = input.split(" ");
        Stack<Double> numbers = new Stack<>();

        Stream.of(tokens).forEach(t -> {
            double a;
            double b;
            switch(t){
                case "+":
                    b = numbers.pop();
                    a = numbers.pop();
                    numbers.push(a +b);
                    break;
                case "/":
                    b = numbers.pop();
                    a = numbers.pop();
                    numbers.push(a /b);
                    break;
                case "-":
                    b = numbers.pop();
                    a = numbers.pop();
                    numbers.push(a - b);
                    break;
                case "*":
                    b = numbers.pop();
                    a = numbers.pop();
                    numbers.push(a * b);
                    break;
                default:
                    numbers.push(Double.valueOf(t));
            }
        });
        return numbers.pop();
    }

    public double memoryRecall(){
        return memory;
    }

    public void memoryClear(){
        memory = 0;
    }

    public void memoryStore(double value){
        memory = value;
    }
}

