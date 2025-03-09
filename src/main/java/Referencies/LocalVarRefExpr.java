package Referencies;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class LocalVarRefExpr extends ReferenceExpression {
    String nameOfReferencedEntity;
    public LocalVarRefExpr(String name) {
        super("LocalVarRefExpr");
        nameOfReferencedEntity = name;
    }

    @Override
    public ObjectNode toJson(ObjectMapper mapper) {
        ObjectNode node = super.toJson(mapper);
        node.put("name",nameOfReferencedEntity);
        return node;
    }
}
