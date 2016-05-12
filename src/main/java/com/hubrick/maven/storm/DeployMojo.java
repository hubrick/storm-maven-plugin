/**
 * Copyright (C) ${project.inceptionYear} Etaia AS (oss@hubrick.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hubrick.maven.storm;

import backtype.storm.Config;
import backtype.storm.generated.ClusterSummary;
import backtype.storm.generated.TopologySummary;
import backtype.storm.security.auth.SimpleTransportPlugin;
import backtype.storm.utils.NimbusClient;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.jayway.awaitility.Awaitility;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.thrift7.TException;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Deploys via Marathon by sending config.
 */
@Mojo(name = "deploy", defaultPhase = LifecyclePhase.DEPLOY)
public class DeployMojo extends AbstractStormMojo {

    /**
     * Main class of the topology
     */
    @Parameter(property = "mainClass", required = true)
    private String mainClass;

    /**
     * Arguments to pass to the topology
     */
    @Parameter(property = "arguments", required = false)
    private List<String> arguments = Collections.emptyList();

    /**
     * Max time to wait in sec that the previous running topology terminates.
     */
    @Parameter(property = "waitOnRunningDeploymentTimeoutInSec", required = false, defaultValue = "300")
    private Integer waitForTerminationTimeoutInSec;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        final File file = new File(jarFile);
        if (!file.exists()) {
            throw new MojoExecutionException("No such file: " + file.getAbsolutePath());
        }

        try {
            final Config config = createClientConfig();
            final NimbusClient nimbusClient = NimbusClient.getConfiguredClient(config);
            final Optional<TopologySummary> topologySummary = getTopologySummary(topologyName, nimbusClient);

            initSystemProperties();

            if (topologySummary.isPresent()) {
                getLog().info("Topology " + topologyName + " found");
                getLog().info("Killing topology " + topologyName + "...");

                nimbusClient.getClient().killTopology(topologyName);
                getLog().info("Topology " + topologyName + " killed. Waiting to be removed from cluster...");
                Awaitility.await()
                        .pollInterval(5, TimeUnit.SECONDS)
                        .atMost(waitForTerminationTimeoutInSec, TimeUnit.SECONDS).until(() -> {
                    try {
                        final Optional<TopologySummary> currentTopologySummary = getTopologySummary(topologyName, nimbusClient);
                        if (currentTopologySummary.isPresent()) {
                            getLog().info("Topology " + topologyName + " still alive.");
                            return false;
                        } else {
                            getLog().info("Topology " + topologyName + " removed from cluster.");
                            return true;
                        }
                    } catch (TException e) {
                        throw new RuntimeException("Unable to connect to nimbus", e);
                    }
                });
            } else {
                getLog().info("Topology " + topologyName + " not found");
            }

            getLog().info("Submitting storm topology " + topologyName + "...");

            // upload topology jar to Cluster using StormSubmitter
            //final String uploadedJarLocation = StormSubmitter.submitJar(config, jarFile);
            //getLog().info("Storm topology " + topologyName + " submitted successfully");

            getLog().info("Calling main method in class " + mainClass + " in jar " + jarFile);
            invokeMainInJar(jarFile, mainClass);
        } catch (Exception e) {
            throw new MojoExecutionException("Failed to deploy topology " + topologyName, e);
        }
    }

    private void initSystemProperties() {
        System.setProperty(
                "storm.options",
                Joiner.on(",").withKeyValueSeparator("=").join(
                        ImmutableMap.of(
                                Config.NIMBUS_HOST, nimbusHost,
                                Config.NIMBUS_THRIFT_PORT, nimbusPort.toString(),
                                Config.STORM_THRIFT_TRANSPORT_PLUGIN, SimpleTransportPlugin.class.getCanonicalName()
                        )
                )
        );

        System.setProperty("storm.jar", jarFile);
        getLog().info("Setting storm.jar property to " + jarFile);
    }

    private final void invokeMainInJar(String jarFile, String mainClass) throws MalformedURLException, ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException, InterruptedException {
        final File file = new File(jarFile);
        final URL[] urls = {file.toURI().toURL()};
        final URLClassLoader loader = new URLClassLoader(urls, this.getClass().getClassLoader());
        final Class<?> cls = loader.loadClass(mainClass);
        final Method main = cls.getDeclaredMethod("main", String[].class);

        final Thread thread = new Thread(() -> {
            try {
                main.invoke(null, new Object[]{arguments.toArray(new String[0])});
            } catch (Exception e) {
                getLog().info("Failed to execute main method in jar file " + jarFile, e);
            }
        });

        thread.setContextClassLoader(loader);
        thread.start();
        thread.join();
    }

    private Optional<TopologySummary> getTopologySummary(final String name, final NimbusClient client) throws TException {
        final ClusterSummary clusterInfo = client.getClient().getClusterInfo();
        return clusterInfo.get_topologies().stream().filter(topologySummary -> name.equals(topologySummary.get_name())).findFirst();
    }

    private Config createClientConfig() {
        final Config nimbusConfig = new Config();

        nimbusConfig.put(Config.NIMBUS_HOST, nimbusHost);
        nimbusConfig.put(Config.NIMBUS_THRIFT_PORT, nimbusPort);
        nimbusConfig.put(Config.STORM_THRIFT_TRANSPORT_PLUGIN, SimpleTransportPlugin.class.getCanonicalName());

        return nimbusConfig;
    }
}
