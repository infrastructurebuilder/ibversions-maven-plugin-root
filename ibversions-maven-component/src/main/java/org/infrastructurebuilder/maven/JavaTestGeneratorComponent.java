package org.infrastructurebuilder.maven;

import javax.inject.Named;

@Named(JavaTestGeneratorComponent.JAVA_TEST)
//@Typed(GeneratorComponent.class)
public class JavaTestGeneratorComponent extends GeneratorComponent {

  static final String JAVA_TEST = "java-test";

  @Override
  protected String getType() {
    return "java";
  }

  @Override
  protected boolean isTestGeneration() {
    return true;
  }

  @Override
  public String getHint() {
    return JAVA_TEST;
  }
}
