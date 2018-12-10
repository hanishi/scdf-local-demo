package demo.app.plugin;

import io.spring.initializr.metadata.Dependency;

import java.util.ArrayList;
import java.util.List;

public class BinderMetadata {

    List<Dependency> forceDependencies = new ArrayList<>();

    public List<Dependency> getForceDependencies() {
        return forceDependencies;
    }

    public void setForceDependencies(List<Dependency> forceDependencies) {
        this.forceDependencies = forceDependencies;
    }

}
