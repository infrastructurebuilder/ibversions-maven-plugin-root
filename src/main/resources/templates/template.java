package @project.groupId@;

public final class @classFromProjectArtifactId@ {
  final static String coordinates = "{" +
      "\"groupId\"  : \"" + getGroupId() + "\"" +
      "," +
      "\"artifactId\" : \"" + getArtifactId() + "\"" +
      "," +
      "\"version\" : \"" + getVersion() + "\"" +
      "," +
      "\"apiVersion\" : \"" + getApiVersion() + "\"" +
      "," +
      "\"extension\" : \"" + getExtension() + "\"" +

      "}";
  final static String xml = "<gav>" +
      "<groupId>"+ getGroupId() + "</groupId>" +
      "<artifactId>" + getArtifactId() + "</artifactId>" +
      "<version>" + getVersion() + "</version>" +
      "<extension>" + getExtension() + "</extension>" +
      "</gav>";
   public final static String getJSONCoordinates() {
     return coordinates;
   }
   public final static String getXMLCoordinates() {
     return xml;
   }
  public final static String getVersion() {
    return "@project.version@";
  }
  public final static String getExtension() {
    return "@project.packaging@";
  }
  public final static String getGroupId() {
    return "@project.groupId@";
  }
  public final static String getArtifactId() {
    return "@project.artifactId@";
  }

  public final static String getApiVersion() {
    String[] v = getVersion().split("\\.");
    return v[0]+"." + v[1]; // This is risky
  }
}