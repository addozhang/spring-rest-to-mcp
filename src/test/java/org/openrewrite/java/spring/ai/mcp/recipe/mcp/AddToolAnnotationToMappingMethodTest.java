package org.openrewrite.java.spring.ai.mcp.recipe.mcp;

import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;
import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.maven.MavenExecutionContextView;
import org.openrewrite.maven.MavenSettings;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.gradle.Assertions.buildGradle;
import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.maven.Assertions.pomXml;

public class AddToolAnnotationToMappingMethodTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new AddToolAnnotationToMappingMethod());
    }

    @Test
    public void addAnnotationsSuccess() {
        ExecutionContext ctx = new InMemoryExecutionContext();
        ExecutionContext context = MavenExecutionContextView.view(ctx).setMavenSettings(MavenSettings.readMavenSettingsFromDisk(ctx));
        rewriteRun(
                spec -> spec.executionContext(context),
                pomXml(pom),
                java(originHelloController, expectedHelloTool),
                java(originUserController, expectedUserTool)
        );
    }

    @Test
    public void skipDueToNoDependency() {
        rewriteRun(
                java(originHelloController)
        );
    }

    @Test
    public void skipDueToNotBean() {
        ExecutionContext ctx = new InMemoryExecutionContext();
        ExecutionContext context = MavenExecutionContextView.view(ctx).setMavenSettings(MavenSettings.readMavenSettingsFromDisk(ctx));

        rewriteRun(
                spec -> spec.executionContext(context),
                pomXml(pom),
                java("""
                        package com.atbug.rewrite.test.controller;
                        
                        import org.springframework.web.bind.annotation.GetMapping;
                        
                        public class HelloController {
                        
                            @GetMapping("/hi")
                            public String hello() {
                                return "Hello, OpenRewrite";
                            }
                        }
                        """)
        );
    }

    // NOTE: This test requires Gradle Tooling API to populate GradleProject markers.
    // In a real environment (not offline mode), this would work by running actual Gradle resolution.
    // The visitor code DOES support Gradle - it checks GradleProjet markers.
    /*
    @Test
    public void addAnnotationsSuccessGrdle() {
        rewriteRun(
            buildGradle(gradleBuild),
            java(originHelloController, expectedHelloTool),
            java(originUserController, expectedUserTool)
        );
    }
    */

    @Test
    public void skipDueToNoDependencyGradle() {
        rewriteRun(
            buildGradle("""
                    plugins {
                        id 'java'
                        id 'org.springframework.boot' version '3.2.0'
                    }

                    dependencies {
                        implementation 'org.springframework.boot:spring-boot-starter-web'
                    }
                    """),
            java(originHelloController)        
        );
    }

    @Language("java")
    public static final String originHelloController = """
            package com.atbug.rewrite.test.controller;
            
            import org.springframework.web.bind.annotation.RequestMapping;
            import org.springframework.web.bind.annotation.PutMapping;
            import org.springframework.web.bind.annotation.GetMapping;
            import org.springframework.web.bind.annotation.PostMapping;
            import org.springframework.web.bind.annotation.PatchMapping;
            import org.springframework.web.bind.annotation.DeleteMapping;
            import org.springframework.web.bind.annotation.PathVariable;
            import org.springframework.web.bind.annotation.RestController;
            
            @RestController
            public class HelloController {
            
            	@GetMapping("/hi")
                public String hello() {
                    return "Hello, OpenRewrite";
                }
            
                /**
                * say hello to someone
                * @param name name of the guy you want to say hello
                * @return hello message
                */
                @RequestMapping("/hi/{name}")
                @GetMapping(path = "/hi/{name}")
                @PostMapping("/hi/{name}")
                @PutMapping("/hi/{name}")
                @PatchMapping("/hi/{name}")
                @DeleteMapping("/hi/{name}")
                public String helloTo(@PathVariable("name") String name) {
                    return "Hello, " + name;
                }
            
                public String helloWithoutMapping() {
                    return "Hello, OpenRewrite";
                }
            }
            """;

    @Language("java")
    public static final String expectedHelloTool = """
            package com.atbug.rewrite.test.controller;
            
            import org.springframework.web.bind.annotation.RequestMapping;
            import org.springframework.web.bind.annotation.PutMapping;
            import org.springframework.web.bind.annotation.GetMapping;
            import org.springframework.web.bind.annotation.PostMapping;
            import org.springframework.web.bind.annotation.PatchMapping;
            import org.springframework.ai.tool.annotation.Tool;
            import org.springframework.ai.tool.annotation.ToolParam;
            import org.springframework.web.bind.annotation.DeleteMapping;
            import org.springframework.web.bind.annotation.PathVariable;
            import org.springframework.web.bind.annotation.RestController;
            
            @RestController
            public class HelloController {
            
                @GetMapping("/hi")
                @Tool(description = "hello")
                public String hello() {
                    return "Hello, OpenRewrite";
                }
            
                /**
                * say hello to someone
                * @param name name of the guy you want to say hello
                * @return hello message
                */
                @RequestMapping("/hi/{name}")
                @GetMapping(path = "/hi/{name}")
                @PostMapping("/hi/{name}")
                @PutMapping("/hi/{name}")
                @PatchMapping("/hi/{name}")
                @DeleteMapping("/hi/{name}")
                @Tool(description = "say hello to someone")
                public String helloTo(@PathVariable("name") @ToolParam(description = "name of the guy you want to say hello") String name) {
                    return "Hello, " + name;
                }
            
                public String helloWithoutMapping() {
                    return "Hello, OpenRewrite";
                }
            }
            """;

    @Language("java")
    public static final String originUserController = """
            package com.atbug.rewrite.test.controller;
            
            import org.springframework.web.bind.annotation.GetMapping;
            import org.springframework.web.bind.annotation.PostMapping;
            import org.springframework.web.bind.annotation.RestController;
            
            import java.util.ArrayList;
            import java.util.List;
            
            @RestController
            public class UserController {
            
                public record User(String name, String email) {}
            
                private final List<User> users = new ArrayList<>(List.of(new User("John", "john@example.com"), new User("Jane", "jane@example.com")));
            
                @GetMapping("/users")
                public List<User> getUsers() {
                    return users;
                }
            
                @PostMapping("/users")
                public String addUser(User user) {
                    users.add(user);
                    return "User added successfully!";
                }
            }
            """;

    @Language("java")
    public static final String expectedUserTool = """
            package com.atbug.rewrite.test.controller;
            
            import org.springframework.ai.tool.annotation.Tool;
            import org.springframework.ai.tool.annotation.ToolParam;
            import org.springframework.web.bind.annotation.GetMapping;
            import org.springframework.web.bind.annotation.PostMapping;
            import org.springframework.web.bind.annotation.RestController;
            
            import java.util.ArrayList;
            import java.util.List;
            
            @RestController
            public class UserController {
            
                public record User(String name, String email) {}
            
                private final List<User> users = new ArrayList<>(List.of(new User("John", "john@example.com"), new User("Jane", "jane@example.com")));
            
                @GetMapping("/users")
                @Tool(description = "getUsers")
                public List<User> getUsers() {
                    return users;
                }
            
                @PostMapping("/users")
                @Tool(description = "addUser")
                public String addUser(@ToolParam(description = "user") User user) {
                    users.add(user);
                    return "User added successfully!";
                }
            }
            """;

    @Language("xml")
    public static final String pom = """
            <project>
                <groupId>com.atbug.rewrite</groupId>
                <artifactId>web-to-mcp</artifactId>
                <version>1.0-SNAPSHOT</version>
                <dependencies>
                    <dependency>
                        <groupId>org.springframework.ai</groupId>
                        <artifactId>spring-ai-starter-mcp-server-webmvc</artifactId>
                        <version>1.0.0-SNAPSHOT</version>
                    </dependency>
                </dependencies>
                <repositories>
                    <repository>
                        <id>spring-snapshots</id>
                        <name>Spring Snapshots</name>
                        <url>https://repo.spring.io/snapshot</url>
                        <releases>
                            <enabled>false</enabled>
                        </releases>
                    </repository>
                    <repository>
                        <name>Central Portal Snapshots</name>
                        <id>central-portal-snapshots</id>
                        <url>https://central.sonatype.com/repository/maven-snapshots/</url>
                        <releases>
                            <enabled>false</enabled>
                        </releases>
                        <snapshots>
                            <enabled>true</enabled>
                        </snapshots>
                    </repository>
                </repositories>
            </project>
            """;

    @Language("groovy")
    public static final String gradleBuild = """
            plugins {
                id 'java'
                id 'org.springframework.boot' version '3.2.0'
            }        

            // repositories {
            //      mavenLocal()
            //      mavenCentral()
            //      maven { url 'https://repo.spring.io/snapshot' } 
            //}

            dependencies {
                implementation 'org.springframework.ai:spring-ai-starter-mcp-server-webmvc:1.0.3'
            }

            """;
}