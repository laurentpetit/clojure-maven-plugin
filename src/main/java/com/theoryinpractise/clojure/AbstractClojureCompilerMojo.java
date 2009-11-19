/*
 * Created by IntelliJ IDEA.
 * User: amrk
 * Date: Apr 18, 2009
 * Time: 1:08:16 PM
 */
package com.theoryinpractise.clojure;

import org.apache.commons.exec.Executor;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.ExecuteStreamHandler;
import org.apache.commons.exec.ExecuteException;


import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

public abstract class AbstractClojureCompilerMojo extends AbstractMojo {
    /**
     * Project classpath.
     *
     * @parameter default-value="${project.compileClasspathElements}"
     * @required
     * @readonly
     */
    protected List<String> classpathElements;

    /**
     * Location of the file.
     *
     * @parameter default-value="${project.build.outputDirectory}"
     * @required
     */
    protected File outputDirectory;

    /**
     * Location of the source files.
     *
     * @parameter
     */
    protected File[] sourceDirectories = new File[]{new File("src/main/clojure")};

    /**
     * Location of the source files.
     *
     * @parameter
     */
    protected File[] testSourceDirectories = new File[]{new File("src/test/clojure")};

    /**
     * Location of the source files.
     *
     * @parameter default-value="${project.build.testSourceDirectory}"
     * @required
     */
    protected File baseTestSourceDirectory;

    /**
     * Location of the generated source files.
     *
     * @parameter default-value="${project.build.outputDirectory}/../generated-sources"
     * @required
     */
    protected File generatedSourceDirectory;

    /**
     * Should we compile all namespaces or only those defined?
     *
     * @parameter defaut-value="false"
     */
    protected boolean compileDeclaredNamespaceOnly;

    /**
     * A list of namespaces to compile
     *
     * @parameter
     */
    protected String[] namespaces;

    /**
     * Classes to put onto the command line before the main class
     *
     * @parameter
     */
    private List<String> prependClasses;

    protected String[] discoverNamespaces() throws MojoExecutionException {
        return new NamespaceDiscovery(getLog(), compileDeclaredNamespaceOnly).discoverNamespacesIn(namespaces, sourceDirectories);
    }


    protected void callClojureWith(
            File[] sourceDirectory,
            File outputDirectory,
            List<String> compileClasspathElements,
            String mainClass,
            String[] clojureArgs) throws MojoExecutionException {

        outputDirectory.mkdirs();

        String cp = "";
        for (File directory : sourceDirectory) {
            cp = cp + directory.getPath() + File.pathSeparator;
        }

        cp = cp + outputDirectory.getPath() + File.pathSeparator;

        for (Object classpathElement : compileClasspathElements) {
            cp = cp + File.pathSeparator + classpathElement;
        }

        getLog().debug("Clojure classpath: " + cp);
        CommandLine cl = new CommandLine("java");

        cl.addArgument("-cp");
        cl.addArgument(cp);
        cl.addArgument("-Dclojure.compile.path=" + outputDirectory.getPath() + "");

        if (prependClasses != null) {
            cl.addArguments(prependClasses.toArray(new String[prependClasses.size()]));
        }

        cl.addArgument(mainClass);

        if (clojureArgs != null) {
            cl.addArguments(clojureArgs, false);
        }

        Executor exec = new DefaultExecutor();
        Map<String, String> env = new HashMap<String, String>(System.getenv());
        env.put("path", ";");
        env.put("path", System.getProperty("java.home"));

        ExecuteStreamHandler handler = new CustomPumpStreamHandler(System.out, System.err, System.in);
        exec.setStreamHandler(handler);

        int status;
        try {
            status = exec.execute(cl, env);
        } catch (ExecuteException e) {
            status = e.getExitValue();
        } catch (IOException e) {
            status = 1;
        }

        if (status != 0) {
            throw new MojoExecutionException("Clojure failed.");
        }
    }
}
