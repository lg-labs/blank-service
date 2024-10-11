# Container Module
Part I.

- Main Application
- Create Beans(e.g: Domain Service)
- Configurations
- Properties

## Build Image

It has two plugins prepared for build image. Only, you need to add the plugin.

### Using Jib

```xml

<properties>
    <docker.from.image.platform.architecture>arm64</docker.from.image.platform.architecture>
    <docker.from.image.platform.os>linux</docker.from.image.platform.os>
</properties>
```

```xml

<build>
    <plugins>
        <plugin>
            <groupId>com.google.cloud.tools</groupId>
            <artifactId>jib-maven-plugin</artifactId>
        </plugin>
        ...
    </plugins>
</build>
```

### Using spring-boot-maven-plugin

```xml

<build>
    <plugins>
        <plugin>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-maven-plugin</artifactId>
        </plugin>
        ...
    </plugins>
</build>
```