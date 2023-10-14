package org.infrastructurebuilder.maven;

import javax.inject.Named;

import org.apache.maven.project.MavenProject;
import org.eclipse.sisu.Typed;

@Named(JavaGeneratorComponent.JAVA)
@Typed(GeneratorComponent.class)
public class JavaGeneratorComponent extends GeneratorComponent {

  static final String JAVA = "java";

  @Override
  protected void addSourceFolderToProject(final MavenProject mavenProject) {
    mavenProject.addCompileSourceRoot(getOutputDirectory().getAbsolutePath());
  }

}
