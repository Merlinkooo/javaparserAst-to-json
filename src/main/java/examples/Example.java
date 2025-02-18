package examples;

public class Example {
    private String text = "Hello";  // Atribút triedy
    private static String text2="afa";
    public void test(String param) {  // Parameter metódy
        String text = "Local";       // Lokálna premenná
        text.length();                // Atribút
        param.length();

        // Parameter//
    }
    public int cislo(){
        return text2.length();
    }
    public  void print(){
        System.out.println(this.cislo());
    }
    public void moj(){}
}
