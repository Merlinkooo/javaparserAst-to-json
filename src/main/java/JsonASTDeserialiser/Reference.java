package JsonASTDeserialiser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class Reference {
    private ReferenceType type;
    private ObjectMapper objectMapper;
    private String nameOfReferencedEntity;


    public Reference(ReferenceType type,String nameOfReferencedEntity,ObjectMapper objectMapper){
        this.type = type;
        this.nameOfReferencedEntity = nameOfReferencedEntity;
        this.objectMapper = objectMapper;
    }

    public ReferenceType getType() {
        return type;
    }
    public ObjectNode toJson(){
        ObjectNode node =  objectMapper.createObjectNode();
        node.put("node",this.type.getName());
        node.put("name",this.nameOfReferencedEntity);
        return node;
    }

    public enum ReferenceType{
        PARAM_REFERENCE("ParamVarRefExpr"),
        LOCAL_VAR_REFERENCE("LocalVarRefExpr"),
        MEMBER_VAR_REFERENCE("MemberVarRefExpr"),
        THIS_REFERENCE("ThisRefExpr"),
        CLASS_REFERENCE("ClassRefExpr");
        private String name;
        ReferenceType(String name){
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }
}
