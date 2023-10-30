package org.infrastructurebuilder.maven;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class IBVersionsComponentExecutionResult {

  private String classFromArtifactId;
  private Path testSources;
  private Path sources;
  private List<Path> addedSources = new ArrayList<>();

  public void setClassFromArtifactId(String cnfai) {
    this.classFromArtifactId = Objects.requireNonNull(cnfai);
  }

  public String getClassFromArtifactId() {
    return this.classFromArtifactId;
  }

  public void setSource(Path outputDirectory, boolean test) {
    if (test)
      this.testSources = outputDirectory;
    else
      this.sources = outputDirectory;
  }

  public Optional<Path> getSources() {
    return Optional.ofNullable(this.sources);
  }

  public Optional<Path> getTestSources() {
    return Optional.ofNullable(testSources);
  }

  public void addSourceToCompile(Path source) {
    this.addedSources .add(source);
  }

  public List<Path> getAddedSources() {
    return addedSources;
  }

  public int getAddedSourcesCount() {
    return getAddedSources().size();
  }

}
