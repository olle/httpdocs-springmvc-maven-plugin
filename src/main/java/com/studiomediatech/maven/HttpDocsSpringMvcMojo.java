package com.studiomediatech.maven;

import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import org.codehaus.plexus.classworlds.realm.ClassRealm;

import org.reflections.ReflectionUtils;
import org.reflections.Reflections;

import java.io.File;
import java.io.IOException;

import java.lang.annotation.Annotation;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;


@Mojo(
    name = "httpdocs-springmvc", defaultPhase = LifecyclePhase.PACKAGE,
    requiresDependencyResolution = ResolutionScope.COMPILE, requiresProject = true
)
public class HttpDocsSpringMvcMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Parameter(defaultValue = "${plugin}", readonly = true)
    private PluginDescriptor pluginDescriptor;

    @Parameter(property = "httpdocs-springmvc.dir", defaultValue = "docs/")
    private String targetDirectory;

    @Parameter(property = "httpdocs-springmvc.packageName", defaultValue = "")
    private String packageName;

    @SuppressWarnings("unchecked")
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        // printing the ClassRealm content containing plugin classpath dependencies
        final ClassRealm classRealm = pluginDescriptor.getClassRealm();

        List<URL> pathUrls = new ArrayList<>();

        try {
            for (String mavenCompilePath : project.getCompileClasspathElements()) {
                pathUrls.add(new File(mavenCompilePath).toURI().toURL());
            }
        } catch (MalformedURLException | DependencyResolutionRequiredException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        pathUrls.forEach(classRealm::addURL);

        // need to define parent classloader which knows all dependencies of the plugin
        System.out.println(" GENERATING INTO: " + targetDirectory);
        System.out.println(" LOOKING UP: " + packageName);

        Reflections r = new Reflections(packageName);

        Class<? extends Annotation> a1 = (Class<? extends Annotation>) ReflectionUtils.forName(
                "org.springframework.stereotype.Controller");

        Set<Class<?>> typesAnnotatedWith = r.getTypesAnnotatedWith(a1);
        typesAnnotatedWith.stream().forEachOrdered(c -> System.out.println("######### FOUND: " + c.getName()));
    }


    public static void findMvcAnnotations() throws IOException, URISyntaxException, ClassNotFoundException {

        @SuppressWarnings("unchecked")
        Class<? extends Annotation> a1 = (Class<? extends Annotation>) Class.forName(
                "org.springframework.stereotype.Controller");
        @SuppressWarnings("unchecked")
        Class<? extends Annotation> a2 = (Class<? extends Annotation>) Class.forName(
                "org.springframework.web.bind.annotation.GetMapping");
        @SuppressWarnings("unchecked")
        Class<? extends Annotation> a3 = (Class<? extends Annotation>) Class.forName(
                "org.springframework.web.bind.annotation.PostMapping");

        String packageName = "net.contargo";
        File dir = new File("target/classes/net/contargo");

        findClasses(dir, packageName).stream().filter(clazz -> clazz.isAnnotationPresent(a1)).peek(c ->
                    System.out.println(c.getName()))
            .flatMap(clazz -> Stream.of(clazz.getMethods())).filter(method ->
                    method.isAnnotationPresent(a2) || method.isAnnotationPresent(a3))
            .peek(m -> System.out.println("   " + m.getName()))
            .flatMap(m -> Stream.of(m.getAnnotations())).forEach(a -> System.out.println("      " + a));
    }


    private static List<Class<?>> findClasses(File dir, String packageName) throws ClassNotFoundException {

        List<Class<?>> classes = new ArrayList<Class<?>>();

        if (!dir.exists()) {
            return classes;
        }

        File[] files = dir.listFiles();

        for (File file : files) {
            if (file.isDirectory()) {
                classes.addAll(findClasses(file, packageName + "." + file.getName()));
            } else if (file.getName().endsWith(".class")) {
                classes.add(Class.forName(
                        packageName + '.' + file.getName().substring(0, file.getName().length() - 6)));
            }
        }

        return classes;
    }
}
