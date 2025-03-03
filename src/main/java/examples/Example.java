package examples;

public class Example<T extends Number> {
    private int number;

    public Example(String text){

    }
    public Example(){
        this("test");
    }
    public int getNumber(){
        return number;
    }
    void test(String param) {
        String text = "Local";
        text.length();
        param.length();
        {text = "10";}
        // Parameter//
        switch(getNumber()){
            case 10:{

            }
            case 11 :

        }

    }

}
