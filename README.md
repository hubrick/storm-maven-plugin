storm-maven-plugin
=====================

# Maven documentation

For detailed documentation of goals and configuration options run in your project:
`mvn com.hubrick.maven:storm-maven-plugin:help -Ddetail=true`

# Basic usage

Add plugin configuration to your `pom.xml`:

```xml
<plugin>
    <groupId>com.hubrick.maven</groupId>
    <artifactId>storm-maven-plugin</artifactId>
    <version>0.1.0</version>
    <configuration>
        <nimbusHost>${nimbusHost}</nimbusHost>
        <topologyName>my-topology</topologyName>
        <jarFile>topology.jar</jarFile>
        <mainClass>com.example.Topology</mainClass>
        <arguments>
            <param>param</param>
        </arguments>
    </configuration>
    <executions>
        <execution>
            <id>deploy</id>
            <phase>deploy</phase>
            <goals>
                <goal>deploy</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```
