package org.infrastructurebuilder.maven;

import javax.inject.Named;

import org.eclipse.sisu.Typed;

@Named(JavaGeneratorComponent.JAVA)
@Typed(GeneratorComponent.class)
public class JavaGeneratorComponent extends GeneratorComponent {

  static final String JAVA = "java";

  @Override
  protected String getType() {
    return "java";
  }

  @Override
  protected boolean isTestGeneration() {
    return false;
  }
//  @Override
//  protected void addSourceFolderToProject(final MavenProject mavenProject) {
//    mavenProject.addCompileSourceRoot(getOutputDirectory().toString());
//  }

}
