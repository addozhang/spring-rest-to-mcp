package org.openrewrite.java.spring.ai.mcp.visitor;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.gradle.marker.GradleProject;
import org.openrewrite.maven.tree.MavenResolutionResult;

import java.util.concurrent.atomic.AtomicBoolean;

@Value
@EqualsAndHashCode(callSuper = false)
public class SpringAIMcpVisitor<P> extends TreeVisitor<Tree, P> {

    @NonNull AtomicBoolean aiMcpEnabled;

    @Override
    public @Nullable Tree visit(@Nullable Tree tree, P p){
        if (tree instanceof SourceFile) {
            SourceFile sourceFile = (SourceFile) tree;

            // Check for Maven dependencies via MavenResolutionResult marker
            sourceFile.getMarkers()
                .findFirst(MavenResolutionResult.class)
                .ifPresent(mrr -> {
                    boolean present = mrr.getPom().getRequestedDependencies().stream()
                        .anyMatch(d -> "org.springframework.ai".equals(d.getGroupId())
                                    && "spring-ai-starter-mcp-server-webmvc".equals(d.getArtifactId()));
                    if (present) {
                        aiMcpEnabled.set(true);
                    }
                });

            // Check for Gradle dependencies via GradleProject marker
            sourceFile.getMarkers()
                .findFirst(GradleProject.class)
                .ifPresent(gp -> {
                    boolean present = gp.getConfigurations().stream()
                        .flatMap(cfg -> cfg.getRequested().stream())
                        .anyMatch(d -> "org.springframework.ai".equals(d.getGroupId())
                                    && "spring-ai-starter-mcp-server-webmvc".equals(d.getArtifactId()));
                    if (present) {
                        aiMcpEnabled.set(true);
                    }
                });    
        }
        return super.visit(tree, p);
    }
}
