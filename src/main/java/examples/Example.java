package examples;

public class Example<T extends Number> {
    private String text = "Hello";

    private String  text3;
    private ReversePolishNotation<Integer> notation= new ReversePolishNotation<>();

    public Example(String text){
        this.text = text;
    }
    public Example(){
        this("test");
    }
    public void test(String param) {
        String text = "Local";
        text.length();
        param.length();
        {text = "10";}
        // Parameter//
        switch(text){
            case "priezvisko","meno":{

            }
            case "text" :

        }

    }

}
