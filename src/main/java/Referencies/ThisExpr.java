package Referencies;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
//ThisExpr is implemented as singleton
public class ThisExpr extends ReferenceExpression {
    static ThisExpr instance;
    private ThisExpr() {
        super( "ThisExpr");
    }

    @Override
    public ObjectNode toJson(ObjectMapper mapper) {
        return super.toJson(mapper);
    }
    public static ThisExpr getInstance(){
        if (instance == null){
            instance= new ThisExpr();
        }
        return instance;
    }
}
