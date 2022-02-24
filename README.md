# GraalVM Spring Native Hints 

This is an integration of all the Spring Native hints that don't yet have another home. 
I want to incubate as many integrations as possible in the lead up to Spring Framework 6. 
This project will serve Spring Native users first and then move over to Spring Framework 6 
and Spring Boot 3 when it ships.

## Usage 

Add the dependency itself: 

```xml 
    <dependency>
        <groupId>io.cloudnativejava</groupId>
        <artifactId>hints</artifactId>
        <version>0.0.1-SNAPSHOT</version>
    </dependency>
```

Add my Artifactory repository to the build: 

```xml 
    <repositories>
        <repository>
            <snapshots>
                <enabled>false</enabled>
            </snapshots>
            <id>central</id>
            <name>libs-release</name>
            <url>
                https://cloudnativejava.jfrog.io/cloudnativejava/libs-release
            </url>
        </repository>
        <repository>
            <snapshots/>
            <id>snapshots</id>
            <name>libs-snapshot</name>
            <url>
                https://cloudnativejava.jfrog.io/cloudnativejava/libs-snapshot
            </url>
        </repository>
        <repository>
            <id>maven_central</id>
            <name>Maven Central</name>
            <url>https://repo.maven.apache.org/maven2/</url>
        </repository>
    </repositories>
    <pluginRepositories>
        <pluginRepository>
            <snapshots>
                <enabled>false</enabled>
            </snapshots>
            <id>central</id>
            <name>plugins-release</name>
            <url>
                https://cloudnativejava.jfrog.io/cloudnativejava/plugins-release
            </url>
        </pluginRepository>
        <pluginRepository>
            <snapshots/>
            <id>snapshots</id>
            <name>plugins-snapshot</name>
            <url>
                https://cloudnativejava.jfrog.io/cloudnativejava/plugins-snapshot
            </url>
        </pluginRepository>
    </pluginRepositories>
```

Then create a [Spring Native-aware project on the Spring Initializr](https://start.spring.io/#!type=maven-project&language=java&platformVersion=2.6.3&packaging=jar&jvmVersion=17&groupId=com.example&artifactId=demo&name=demo&description=Demo%20project%20for%20Spring%20Boot&packageName=com.example.demo&dependencies=native) and enjoy!