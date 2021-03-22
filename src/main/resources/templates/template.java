package @project.groupId@;

@javax.inject.Named(
      @classFromProjectArtifactId@ .COMPONENTVERSIONNAME)
@javax.inject.Singleton
public final class @classFromProjectArtifactId@ implements org.infrastructurebuilder.util.versions.IBVersionsSupplier {
  public final static String COMPONENTVERSIONNAME = "@project.groupId@:@project.artifactId@:@project.version@";
  final static String coordinates = "{" +
      "\"groupId\"  : \"" + groupId() + "\"" +
      "," +
      "\"artifactId\" : \"" + artifactId() + "\"" +
      "," +
      "\"version\" : \"" + version() + "\"" +
      "," +
      "\"apiVersion\" : \"" + apiVersion() + "\"" +
      "," +
      "\"extension\" : \"" + extension() + "\"" +

      "}";
  final static String xml = "<gav>" +
      "<groupId>"+ groupId() + "</groupId>" +
      "<artifactId>" + artifactId() + "</artifactId>" +
      "<version>" + version() + "</version>" +
      "<extension>" + extension() + "</extension>" +
      "</gav>";
   public final static String getJSONCoordinates() {
     return coordinates;
   }
   public final static String getXMLCoordinates() {
     return xml;
   }
  public final static String version() {
    return "@project.version@";
  }
  public final static String extension() {
    return "@project.packaging@";
  }
  public final static String groupId() {
    return "@project.groupId@";
  }
  public final static String artifactId() {
    return "@project.artifactId@";
  }

  public final static String apiVersion() {
    String[] v = version().split("\\.");
    return v[0]+"." + v[1]; // This is risky
  }

  @javax.inject.Inject
  public @classFromProjectArtifactId@() {
  }

  public java.util.function.Supplier<String> getGroupId() {
    return () -> @classFromProjectArtifactId@.groupId();
  }
  public java.util.function.Supplier<String> getArtifactId() {
    return () -> @classFromProjectArtifactId@.artifactId();
  }
  public java.util.function.Supplier<String> getVersion() {
    return () -> @classFromProjectArtifactId@.version();
  }
  public java.util.function.Supplier<String> getExtension() {
    return () -> @classFromProjectArtifactId@.extension();
  }
  public java.util.function.Supplier<String> getAPIVersion() {
    return () -> @classFromProjectArtifactId@.apiVersion();
  }
}