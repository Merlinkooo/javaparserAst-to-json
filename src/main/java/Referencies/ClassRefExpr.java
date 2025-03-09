package Referencies;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class ClassRefExpr extends ReferenceExpression {
    String nameOfReferencedEntity;
    public ClassRefExpr(String name) {
        super("ClassRefExpr");
        this.nameOfReferencedEntity = name;
    }

    @Override
    public ObjectNode toJson(ObjectMapper mapper) {
        ObjectNode node = super.toJson(mapper);
        node.put("name",nameOfReferencedEntity);
        return node;
    }
}
