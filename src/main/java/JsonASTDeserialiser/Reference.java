package JsonASTDeserialiser;

public class Reference {
    private ReferenceType type;

    public Reference(ReferenceType type){
        this.type = type;
    }

    public ReferenceType getType() {
        return type;
    }

    public enum ReferenceType{
        PARAM_REFERENCE("ParamReference"),
        LOCAL_VAR_REFERENCE("LocalVarReference"),
        MEMBER_VAR_REFERENCE("MemberVarReference"),
        THIS_REFERENCE("ThisReference");

        private String name;
        ReferenceType(String name){
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }
}
