package demo.app.plugin;

public class Bom {

    private String groupId;
    private String artifactId;
    private String name;
    private String version;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    public io.spring.initializr.metadata.Dependency toInitializerMetadataDependency() {
        io.spring.initializr.metadata.Dependency dependency = new io.spring.initializr.metadata.Dependency();
        dependency.setId(this.getArtifactId());
        dependency.setGroupId(this.getGroupId());
        dependency.setArtifactId(this.getArtifactId());
        return dependency;
    }

    @Override
    public String toString() {
        return "Bom{" +
                "groupId='" + groupId + '\'' +
                ", artifactId='" + artifactId + '\'' +
                ", name='" + name + '\'' +
                ", version='" + version + '\'' +
                '}';
    }
}
