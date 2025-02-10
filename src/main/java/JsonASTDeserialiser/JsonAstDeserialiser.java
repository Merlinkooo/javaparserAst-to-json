package JsonASTDeserialiser;

import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import java.io.FileWriter;
import java.io.IOException;

public class JsonAstDeserialiser extends VoidVisitorAdapter<FileWriter> {
    @Override
    public void visit(MethodDeclaration n, FileWriter arg) {
        super.visit(n, arg);
        try {
            arg.write("{\n");
            arg.write("\"NodeType\" : \"MethodDeclaration\"\n");
            arg.write("\"Name\" : \"" + n.getName() +"\",\n");
            arg.write("\"ReturnType\": ");
            n.getType().accept(this,arg);
            arg.write("\n\"Modifiers\": [");
            for(Modifier modifier : n.getModifiers()){
                arg.write("{ ");
                this.visit(modifier,arg);
                arg.write("}");
                if(!modifier.equals(n.getModifiers().getLast())) arg.write(", ");
            }
            arg.write("],\n");
            arg.write("\"Parameters\" : [");
            for (Parameter parameter : n.getParameters()){
                this.visit(parameter,arg);
            }
            arg.write("],\n");
            this.visit(n.getBody().get(),arg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void visit(Modifier n, FileWriter arg) {
        super.visit(n, arg);
        try {
            arg.write("\"NodeType : \"Modifier\",\n");
            arg.write("\"Name\" : ");
            arg.write("\"" + n.getKeyword().asString() + "\"");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void visit(Parameter n, FileWriter arg) {
        super.visit(n, arg);
        try {
            arg.write("{\n");
            arg.write("\"NodeType :\" : \"" + "Parameter\",\n");
            arg.write("\"Name\" : \"" + n.getName() + "\", ");
            n.getType().accept(this,arg);
            arg.write("\n\"Modifiers\": [");
            for(Modifier modifier : n.getModifiers()){
                arg.write("{ ");
                this.visit(modifier,arg);
                arg.write("}");
                if(!modifier.equals(n.getModifiers().getLast())) arg.write(", ");
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        n.getType().accept(this,arg);

    }

    @Override
    public void visit(ClassOrInterfaceType n, FileWriter arg) {
        super.visit(n, arg);
    }

}
