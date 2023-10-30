package org.infrastructurebuilder.maven;

import javax.inject.Named;

import org.apache.maven.project.MavenProject;
import org.eclipse.sisu.Typed;

@Named(JavaTestGeneratorComponent.JAVA_TEST)
@Typed(GeneratorComponent.class)
public class JavaTestGeneratorComponent extends GeneratorComponent {

  static final String JAVA_TEST = "java-test";

  @Override
  protected void addSourceFolderToProject(final MavenProject mavenProject) {
    mavenProject.addTestCompileSourceRoot(getOutputDirectory().toAbsolutePath().toString());
  }

  @Override
  protected boolean isTestGeneration() {
    return true;
  }

}
