package @project.groupId@;

import static org.junit.Assert.*;

public final class Test@classFromProjectArtifactId@
{
  @org.junit.Test
  public void testVersion()
  {
    org.json.JSONObject version = new org.json.JSONObject(@classFromProjectArtifactId@.getJSONCoordinates());
    assertEquals("Group id = @project.groupId@","@project.groupId@", version.getString("groupId"));
    assertNotNull("Existence", new @classFromProjectArtifactId@());
    assertNotNull("XML Existence", @classFromProjectArtifactId@.getXMLCoordinates());
    assertEquals("Artifact id = @project.artifactId@", "@project.artifactId@", version.getString("artifactId"));
    assertEquals("Version = @project.version@", "@project.version@", version.getString("version"));
    assertEquals("Extension = @project.packaging@", "@project.packaging@", version.getString("extension"));
    assertTrue("Version starts with API version (not a great test)",  "@project.version@".startsWith(version.getString("apiVersion")));
    assertTrue("String contains artifactId",  new @classFromProjectArtifactId@().getArtifactDependency().get().contains("@project.artifactId@"));
    assertTrue("Api contains api", "@project.version@".startsWith(new @classFromProjectArtifactId@().getAPIVersion().get()));
    assertNotNull("Extension is not null",  new @classFromProjectArtifactId@().getExtension().get());

  }
}
