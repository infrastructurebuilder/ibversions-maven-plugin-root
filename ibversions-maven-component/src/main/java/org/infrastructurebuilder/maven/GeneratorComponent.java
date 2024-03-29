package org.infrastructurebuilder.maven;

import static java.nio.file.Files.newOutputStream;
import static java.util.Objects.requireNonNull;
import static java.util.Objects.requireNonNullElse;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Optional;

import org.codehaus.plexus.util.FileUtils;
import org.infrastructurebuilder.util.versions.GAVBasic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract public class GeneratorComponent {

  // Copied from IBUtils
  static final int BUFFER_SIZE = 10240;
  public static void copy(final InputStream source, final OutputStream sink) throws IOException {
    final byte[] buffer = new byte[BUFFER_SIZE];
    for (int n = 0; (n = requireNonNull(source, "source").read(buffer)) > 0;) {
      requireNonNull(sink, "sink").write(buffer, 0, n);
    }
    return;
  }
  private final static Logger logger = LoggerFactory.getLogger(GeneratorComponent.class);
//  private static final String CLASS_FROM_PROJECT_ARTIFACT_ID = "classFromProjectArtifactId";
  private static final String SNAPSHOT = "-snapshot";
  /*
   * The working directory Where templates will be copied from the classpath or overridden locations
   */
  private Path workDirectory;
//  /*
//   * The target output directory This directory will be added as a source or test source, depending on type
//   */
//  private Path outputDirectory;
  /*
   * You can, but shouldn't, override the generated class name
   */
  private String overriddenGeneratedClassName;
  /*
   * You can, but shouldn't, override the template file
   */
  private Path overriddenTemplateFile;
  private GAVBasic gav;
  private String overriddenPackageName;

  public Optional<IBVersionsComponentExecutionResult> execute() throws IOException {
    Objects.requireNonNull(this.gav);
    IBVersionsComponentExecutionResult result = new IBVersionsComponentExecutionResult();
    result.setClassFromArtifactId(getClassNameFromArtifactId());
    final Path p = getResourcePath();
    result.addSourceToCompile(p);
    result.setSource(getWorkDirectory(), isTestGeneration());
    result.setTargetPackageName(getPackageName());
    logInfo("Source directory: %s added.", getWorkDirectory());
    return Optional.ofNullable(result);
  }

  public Path getWorkDirectory() {
    return workDirectory;
  }

  protected Path getResourcePath() throws IOException {
    Path wp = this.workDirectory.toAbsolutePath();
    final Path filePath = Paths //
        .get(wp.toString(), requireNonNull(getPackageName()).split("\\.")) // source directory/the/group/id/expanded
        .resolve((isTestGeneration() ? "Test" : "") + getClassNameFromArtifactId() + "." + getType()); // final filename
    final Path templatePath = filePath;
    logInfo("writing template to " + templatePath.toAbsolutePath());

    try {
      final Path parents = templatePath.getParent();
      if (!Files.exists(parents)) {
        logDebug("Creating " + parents);
        Files.createDirectories(parents);
      }
      if (overriddenTemplateFile != null) {
        logDebug("Copying overriden template file " + overriddenTemplateFile + " to " + templatePath);
        FileUtils.copyFile(overriddenTemplateFile.toFile(), templatePath.toFile());
      } else {
        final String rPath = getTemplatesDir() + "template";
        getLogger().info("Target path for copied resource is " + templatePath);
        try (InputStream res = getClass().getResourceAsStream(rPath); OutputStream os = newOutputStream(templatePath)) {
          copy(res, os);
        }
      }
    } catch (final IOException e) {
      throw new IOException("Failed to copy files", e);
    }
    return templatePath;
  }

  private String getPackageName() {
    return requireNonNullElse(this.overriddenPackageName, gav.getGroupId());
  }

  protected String getClassNameFromArtifactId() {
    if (overriddenGeneratedClassName != null)
      return overriddenGeneratedClassName;

    String ver = gav.getVersion().get();
    if (ver.toLowerCase().endsWith(SNAPSHOT))
      ver = ver.substring(0, ver.length() - SNAPSHOT.length()); // Remove snapshotting
    final String nonJavaMethodName = gav.getArtifactId();
    final StringBuilder nameBuilder = new StringBuilder();
    boolean capitalizeNextChar = true;
    boolean first = true;

    for (int i = 0; i < nonJavaMethodName.length(); i++) {
      char c = nonJavaMethodName.charAt(i);
      if (c == '.')
        c = '_';
      if (c != '_' && !Character.isLetterOrDigit(c)) {
        if (!first) {
          capitalizeNextChar = true;
        }
      } else {
        nameBuilder.append(capitalizeNextChar ? Character.toUpperCase(c) : Character.toLowerCase(c));
        capitalizeNextChar = false;
        first = false;
      }
    }

    nameBuilder.append("Versioning");
    return nameBuilder.toString();

  }

  private void logDebug(final String format, final Object... args) {
    getLogger().debug(String.format(format, args));
  }

  private void logInfo(final String format, final Object... args) {
    getLogger().info(String.format(format, args));
  }

  public void setOverriddenGeneratedClassName(final String overriddenGeneratedClassName) {
    this.overriddenGeneratedClassName = overriddenGeneratedClassName;
  }

  public void setOverriddenTemplateFile(Path overriddenTemplateFile) throws IOException {
    if (overriddenTemplateFile == null) {
      this.overriddenTemplateFile = null;
      return;
    }
    Path ot = overriddenTemplateFile.toAbsolutePath();
    if (!Files.isRegularFile(ot))
      throw new IOException("Overriden template file " + ot + " is not a regular file");
    this.overriddenTemplateFile = ot;

  }

  public void setOverriddenPackageName(String packageName)  {
    if (this.overriddenPackageName == null)
      this.overriddenPackageName = packageName;
  }
  public void setWorkDirectory(Path workDirectory) throws IOException {
    this.workDirectory = requireNonNull(workDirectory, "Work directory cannot be null").toAbsolutePath();
    Files.createDirectories(this.workDirectory);
  }

  public Logger getLogger() {
    return logger;
  }

  protected String getTemplatesDir() {
    return "/" + getType() + "/" + (isTestGeneration() ? "test-" : "") + "templates/";
  }

  public void setGAV(GAVBasic gav) {
    this.gav = requireNonNull(gav);
  }

  public String getHint() {
    return JavaGeneratorComponent.JAVA;
  }

  abstract protected String getType();

  abstract protected boolean isTestGeneration();

}
