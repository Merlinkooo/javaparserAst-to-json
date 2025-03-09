package JsonASTDeserialiser;


import Referencies.ReferenceExpression;

import java.util.Map;
import java.util.Optional;

public class ReferenceContainer {
    private Map<String, ReferenceExpression> references;

    public void put(String name,ReferenceExpression expression){
        this.references.put(name,expression);
    }

    public Optional<ReferenceExpression> get(String name){
        return Optional.of(this.references.get(name));
    }
}
