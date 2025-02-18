package JsonASTDeserialiser;


import java.util.Map;
import java.util.Optional;

public class ReferenceContainer {
    private Map<String,Reference> references;

    public void put(String name,Reference refType){
        this.references.put(name,refType);
    }

    public Optional<Reference> get(String name){
        return Optional.of(this.references.get(name));
    }
}
