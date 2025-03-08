# Library for deserialization Javaparser AST into JSON format
This project was created for deserialization Javaparser AST to JSON format in order to serialize output JSON file to [ASTFRI AST](https://github.com/kifriosse/astfri) 
## Dependencies:
This projects uses two open source libraries:

[JavaParser](https://github.com/javaparser/javaparser)-Java library for parsing Java source code into AST-Abstract Syntax Tree

[Jackson](https://github.com/FasterXML/jackson]) -Java JSON library

### Dependencies in pom.xml file:
```xml
<dependencies>
        <dependency>
            <groupId>com.github.javaparser</groupId>
            <artifactId>javaparser-core</artifactId>
            <version>3.26.3</version>
        </dependency>
        <dependency>
            <groupId>com.github.javaparser</groupId>
            <artifactId>javaparser-symbol-solver-core</artifactId>
            <version>3.26.3</version>
        </dependency>
        <dependency>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-databind</artifactId>
            <version>2.15.2</version>
        </dependency>
</dependencies>
