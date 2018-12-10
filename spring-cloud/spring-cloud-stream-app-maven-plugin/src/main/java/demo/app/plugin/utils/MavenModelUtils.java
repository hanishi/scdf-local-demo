package demo.app.plugin.utils;

import demo.app.plugin.Bom;
import org.apache.maven.model.Build;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Exclusion;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CopyOnWriteArrayList;

public class MavenModelUtils {

    private MavenModelUtils() {

    }

    public static Model populateModel(String artifactId, String groupId, String version) {
        Model model = new Model();
        model.setGroupId(groupId);
        model.setArtifactId(artifactId);
        model.setPackaging("pom");
        model.setVersion(version);
        model.setModelVersion("4.0.0");

        getBuildWithDockerPluginDefinition(model);

        return model;
    }

    private static void getBuildWithDockerPluginDefinition(Model model) {
        Build build = new Build();
        model.setBuild(build);
        Plugin plugin = new Plugin();
        plugin.setGroupId("io.fabric8");
        plugin.setArtifactId("docker-maven-plugin");
        plugin.setVersion("0.14.2");
        build.addPlugin(plugin);
    }

    public static Model getModelFromContainerPom(File genProjecthome, String groupId, String version) throws IOException, XmlPullParserException {
        File pom = new File(genProjecthome, "pom.xml");
        Model model = pom.exists() ? getModel(pom) : null;
        if (model != null) {
            model.setGroupId(groupId);
            model.setArtifactId(genProjecthome.getName());
            model.setVersion(version);
            model.setName("Apps Container");
            model.setDescription("Container project for generated apps");
            getBuildWithDockerPluginDefinition(model);
        }
        return model;
    }

    public static boolean addModuleIntoModel(Model model, String module) {
        if (!model.getModules().contains(module)) {
            model.addModule(module);
        }
        return true;
    }

    public static void writeModelToFile(Model model, OutputStream os) throws IOException {
        final MavenXpp3Writer writer = new MavenXpp3Writer();
        OutputStreamWriter w = new OutputStreamWriter(os, "utf-8");
        writer.write(w, model);
        w.close();
    }

    private static Model getModel(File pom) throws IOException, XmlPullParserException {
        MavenXpp3Reader reader = new MavenXpp3Reader();
        return reader.read(new FileReader(pom));
    }

    public static Model getModel(InputStream is) {
        final MavenXpp3Reader reader = new MavenXpp3Reader();
        try {
            return reader.read(is);
        } catch (IOException | XmlPullParserException e) {
            throw new IllegalStateException(e);
        }
    }

    public static void addExtraPlugins(Model pomModel) throws IOException {

        pomModel.getBuild().addPlugin(getSurefirePlugin());
        pomModel.getBuild().addPlugin(getJavadocPlugin());
        pomModel.getBuild().addPlugin(getSourcePlugin());

        pomModel.getProperties().setProperty("skipTests", "true");
    }

    public static void addBomsWithHigherPrecedence(Model pomModel, String bomsWithHigherPrecedence) throws IOException {
        DependencyManagement dependencyManagement = pomModel.getDependencyManagement();
        int i = 0;
        String[] boms = StringUtils.commaDelimitedListToStringArray(bomsWithHigherPrecedence);
        for (String bom : boms) {
            String[] coordinates = StringUtils.delimitedListToStringArray(bom, ":");
            if (coordinates.length != 3) {
                throw new IllegalStateException("Coordinates for additional boms are not defined properly.\n" +
                        "It needs to follow a comma separated pattern of groupId:artifactId:version");
            }
            String groupId = coordinates[0];
            String artifactId = coordinates[1];
            String version = coordinates[2];

            Dependency dependency = new Dependency();
            dependency.setGroupId(groupId);
            dependency.setArtifactId(artifactId);
            dependency.setVersion(version);
            dependency.setType("pom");
            dependency.setScope("import");
            dependencyManagement.getDependencies().add(i++, dependency);
        }

        pomModel.setDependencyManagement(dependencyManagement);
    }

    public static void addDockerPlugin(String artifactId, String version, String dockerHubOrg, InputStream is, OutputStream os) throws IOException {
        final MavenXpp3Reader reader = new MavenXpp3Reader();

        Model pomModel;
        try {
            pomModel = reader.read(is);
        }
        catch (IOException | XmlPullParserException e) {
            throw new IllegalStateException(e);
        }

        final Plugin dockerPlugin = new Plugin();
        dockerPlugin.setGroupId("io.fabric8");
        dockerPlugin.setArtifactId("docker-maven-plugin");
        dockerPlugin.setVersion("0.14.2");

        final Xpp3Dom mavenPluginConfiguration = new Xpp3Dom("configuration");

        final Xpp3Dom images = addElement(mavenPluginConfiguration, "images");

        final Xpp3Dom image = addElement(images, "image");
        if (!version.endsWith("BUILD-SNAPSHOT")) {
            addElement(image, "name", dockerHubOrg + "/${project.artifactId}:" + version);
        }
        else {
            addElement(image, "name", dockerHubOrg + "/${project.artifactId}");
        }

        final Xpp3Dom build = addElement(image, "build");
        addElement(build, "from", "anapsix/alpine-java:8");

        final Xpp3Dom volumes = addElement(build, "volumes");
        addElement(volumes, "volume", "/tmp");

        final Xpp3Dom entryPoint = new Xpp3Dom("entryPoint");
        build.addChild(entryPoint);

        final Xpp3Dom exec = new Xpp3Dom("exec");
        entryPoint.addChild(exec);

        addElement(exec, "arg", "java");
        addElement(exec, "arg", "-jar");
        addElement(exec, "arg", "/maven/" + artifactId + ".jar");

        final Xpp3Dom assembly = addElement(build, "assembly");
        addElement(assembly, "descriptor", "assembly.xml");

        dockerPlugin.setConfiguration(mavenPluginConfiguration);

        pomModel.getBuild().addPlugin(dockerPlugin);
        writeModelToFile(pomModel, os);
    }

    public static void addExclusionsForKafka(Model pomModel) throws IOException {

        List<Dependency> dependencies = pomModel.getDependencies();
        CopyOnWriteArrayList<Dependency> cowal = new CopyOnWriteArrayList<>(dependencies);
        for (Dependency dep : cowal) {
            if (dep.getArtifactId().startsWith("kafka_")) {
                pomModel.removeDependency(dep);
                Exclusion exclusion = new Exclusion();
                exclusion.setArtifactId("slf4j-log4j12");
                exclusion.setGroupId("org.slf4j");
                dep.addExclusion(exclusion);
                pomModel.addDependency(dep);
            }
        }
    }

    public static void addAdditionalBoms(Model pomModel, List<Bom> additionalBoms) throws IOException {
        DependencyManagement dependencyManagement = pomModel.getDependencyManagement();
        int i = 0;
        for (Bom bom : additionalBoms) {
            Dependency dependency = new Dependency();
            dependency.setGroupId(bom.getGroupId());
            dependency.setArtifactId(bom.getArtifactId());
            dependency.setVersion(bom.getVersion());
            dependency.setType("pom");
            dependency.setScope("import");
            dependencyManagement.getDependencies().add(++i, dependency);
        }

        pomModel.setDependencyManagement(dependencyManagement);
    }

    public static void addProperties(Model pomModel, Properties properties) {
        Properties pomProperties = pomModel.getProperties();
        if (properties != null) {
            for (Map.Entry<Object, Object> entry : properties.entrySet()) {
                pomProperties.put(entry.getKey(), entry.getValue());
            }
        }
    }

    private static Plugin getSurefirePlugin() {
        final Plugin surefirePlugin = new Plugin();
        surefirePlugin.setGroupId("org.apache.maven.plugins");
        surefirePlugin.setArtifactId("maven-surefire-plugin");
        surefirePlugin.setVersion("2.19.1");
        final Xpp3Dom mavenPluginConfiguration = new Xpp3Dom("configuration");
        final Xpp3Dom skipTests = new Xpp3Dom("skipTests");
        skipTests.setValue("${skipTests}");
        mavenPluginConfiguration.addChild(skipTests);

        surefirePlugin.setConfiguration(mavenPluginConfiguration);
        return surefirePlugin;
    }

    private static Plugin getJavadocPlugin() {
        final Plugin javadocPlugin = new Plugin();
        javadocPlugin.setGroupId("org.apache.maven.plugins");
        javadocPlugin.setArtifactId("maven-javadoc-plugin");

        PluginExecution pluginExecution = new PluginExecution();
        pluginExecution.setId("javadoc");
        List<String> goals = new ArrayList<>();
        goals.add("jar");
        pluginExecution.setGoals(goals);
        pluginExecution.setPhase("package");
        List<PluginExecution> pluginExecutions = new ArrayList<>();
        pluginExecutions.add(pluginExecution);
        javadocPlugin.setExecutions(pluginExecutions);

        final Xpp3Dom javadocConfig = new Xpp3Dom("configuration");
        final Xpp3Dom quiet = new Xpp3Dom("quiet");
        quiet.setValue("true");
        javadocConfig.addChild(quiet);

        javadocPlugin.setConfiguration(javadocConfig);
        return javadocPlugin;
    }

    private static Plugin getSourcePlugin() {
        final Plugin sourcePlugin = new Plugin();
        sourcePlugin.setGroupId("org.apache.maven.plugins");
        sourcePlugin.setArtifactId("maven-source-plugin");

        PluginExecution pluginExecution = new PluginExecution();
        pluginExecution.setId("attach-sources");
        List<String> goals = new ArrayList<>();
        goals.add("jar");
        pluginExecution.setGoals(goals);
        pluginExecution.setPhase("package");
        List<PluginExecution> pluginExecutions = new ArrayList<>();
        pluginExecutions.add(pluginExecution);
        sourcePlugin.setExecutions(pluginExecutions);

        return sourcePlugin;
    }

    private static Xpp3Dom addElement(Xpp3Dom parentElement, String elementName) {
        return addElement(parentElement, elementName, null);
    }

    private static Xpp3Dom addElement(Xpp3Dom parentElement, String elementName, String elementValue) {
        Xpp3Dom child = new Xpp3Dom(elementName);
        child.setValue(elementValue);
        parentElement.addChild(child);
        return child;
    }
}

