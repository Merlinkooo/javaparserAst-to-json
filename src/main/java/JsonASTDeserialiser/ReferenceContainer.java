package JsonASTDeserialiser;


import java.util.Map;
import java.util.Optional;

public class ReferenceContainer {
    private Map<String,ReferencType> references;

    public void put(String name,ReferencType refType){
        this.references.put(name,refType);
    }

    public Optional<ReferencType> get(String name){
        return Optional.of(this.references.get(name));
    }
}
