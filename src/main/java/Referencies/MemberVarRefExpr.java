package Referencies;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class MemberVarRefExpr extends ReferenceExpression {
    private ReferenceExpression owner;
    private String nameOfRefencedEntity;
    public MemberVarRefExpr(String name, ReferenceExpression owner) {
        super("MemberVarRefExpr");
        nameOfRefencedEntity = name;
        this.owner = owner;
    }

    @Override
    public ObjectNode toJson(ObjectMapper mapper) {
        ObjectNode node =  super.toJson(mapper);
        node.put("name",nameOfRefencedEntity);
        node.set("owner",owner.toJson(mapper));
        return node;
    }
}
