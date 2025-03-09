package Referencies;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class ParamVarRefExpr extends ReferenceExpression {
    String nameOfReferencedEntity;

    public ParamVarRefExpr(String nameOfReferencedEntity) {
        super("ParamVarRefExpr");
        this.nameOfReferencedEntity = nameOfReferencedEntity;
    }

    @Override
    public ObjectNode toJson(ObjectMapper mapper) {
        ObjectNode node = super.toJson(mapper);
        node.put("name",nameOfReferencedEntity);
        return node;
    }
}
