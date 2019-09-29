package @project.groupId@;

public final class Test@classFromProjectArtifactId@
{
    @org.junit.Test
    public void testVersion()
    {
      org.json.JSONObject version = new org.json.JSONObject(@classFromProjectArtifactId@.getCoordinates());
      org.junit.Assert.assertEquals("Group id = @project.groupId@","@project.groupId@", version.getString("groupId"));
      org.junit.Assert.assertNotNull("Existence", new @classFromProjectArtifactId@());
      org.junit.Assert.assertEquals("Artifact id = @project.artifactId@", "@project.artifactId@", version.getString("artifactId"));
      org.junit.Assert.assertEquals("Version = @project.version@", "@project.version@", version.getString("version"));
      org.junit.Assert.assertEquals("Extension = @project.packaging@", "@project.packaging@", version.getString("extension"));
      org.junit.Assert.assertTrue("Version starts with API version (not a great test)",  "@project.version@".startsWith(version.getString("apiVersion")));


    }
}
