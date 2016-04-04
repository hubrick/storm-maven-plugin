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

import com.google.gson.Gson;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugins.annotations.Parameter;

abstract class AbstractStormMojo extends AbstractMojo {

    protected static final Gson GSON = new Gson();

    @Parameter(defaultValue = "${session}", readonly = true)
    protected MavenSession session;

    @Parameter(defaultValue = "${mojoExecution}", readonly = true)
    protected MojoExecution execution;

    /**
     * The nimbus host
     */
    @Parameter(property = "nimbusHost", required = true)
    protected String nimbusHost;

    /**
     * The nimbus port
     */
    @Parameter(property = "nimbusPort", required = false, defaultValue = "6627")
    protected Integer nimbusPort;

    /**
     * The name of the topology
     */
    @Parameter(property = "topologyName", required = true)
    protected String topologyName;

    /**
     * The packed topology that should be submitted
     */
    @Parameter(property = "jarFile", required = true)
    protected String jarFile;
}
