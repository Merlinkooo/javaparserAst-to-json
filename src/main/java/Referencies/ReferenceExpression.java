package Referencies;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class ReferenceExpression {

    private String type;

    protected ReferenceExpression(String type) {

        this.type = type;
    }

    public ObjectNode toJson(ObjectMapper mapper){
        ObjectNode refJson = mapper.createObjectNode();
        refJson.put("node",type);
        return refJson;
    }
}
