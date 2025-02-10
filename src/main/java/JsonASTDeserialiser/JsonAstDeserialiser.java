package JsonASTDeserialiser;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import java.io.FileWriter;
import java.io.IOException;

public class JsonAstDeserialiser extends VoidVisitorAdapter<FileWriter> {
    @Override
    public void visit(MethodDeclaration n, FileWriter arg) {
        super.visit(n, arg);
        try {
            arg.write("\"Type\" : \"" + n.getName() +"\",\n");
            arg.write("\"ReturnType\": ");
            n.getType().accept(this,arg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
