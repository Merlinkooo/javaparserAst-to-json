package examples;



import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicReference;

public class Example<T extends Number> extends ExampleParent implements Comparable<Example<T>>{
    private int number;
    private String text;

    public int calculate(int first,int second,char operator){

        ArrayList<Character> operators = new ArrayList<>(Arrays.asList('+','-','*','/'));
        switch (operator){
            case '*' : return first * second;
            case '/' : return first / second;
            case '+' : return first + second;
            case '-' : return first - second;
            default : {
                char op;
                Scanner sc = new Scanner(System.in);
                do {
                    System.out.println("Invalid operator given");
                    System.out.println("Choose one from following operators : + - * /");
                    System.out.print(">");
                    op = sc.next().charAt(0);
                } while (!isValidOperator(operators,op));
                return calculate(first,second,op);
            }
        }
    }

    public Example(String text){
        super(10);
        this.text = text;
    }
    public Example(){
        super(10);
    }
    public int getNumber(){
        return number;
    }
    public void printLongerText(String param) {
        if (param.length() > this.text.length() ){
            System.out.println(param);
        }else if (this.text.length() > param.length()){
            System.out.println(this.text);
        }else{
            System.out.println("Length of parameter is the same ass lentgth of attribute");
            System.out.println("Attribute: " + this.text);
            System.out.println("Parameter: " + param);
        }

    }
    public void printTextNTimes(int count){
        int e;
        int i;
        for (e=0,i=0 ; i < count; e++,i++) {
            System.out.println(10);
        }
    }

    public boolean isValidOperator(ArrayList<Character> operarators,char operator){
        return operarators.stream().anyMatch(op -> op == operator);
    }

    public void multiplyElements(ArrayList<Integer> list, int times){
        list.forEach(number -> number = number * times);
    }

    public boolean constainsStr(String substr){
        return this.text.contains(substr);
    }

    public String getText() {
        return text;
    }

    @Override
    public int compareTo(Example<T> o) {
        if(number > o.number) return 1;
        if (number < o.number) return -1;
        return 0;
    }
}
