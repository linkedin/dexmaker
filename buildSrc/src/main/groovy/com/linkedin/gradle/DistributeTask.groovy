package com.linkedin.gradle

import groovy.json.JsonBuilder
import org.apache.http.client.fluent.Request
import org.apache.http.entity.ContentType
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.jfrog.gradle.plugin.artifactory.dsl.ArtifactoryPluginConvention

class DistributeTask extends DefaultTask {

    @TaskAction
    public void distributeBuild() {
        ArtifactoryPluginConvention convention = project.convention.plugins.artifactory
        def buildNumber = convention.clientConfig.info.buildNumber
        def buildName = convention.clientConfig.info.buildName
        def context = convention.clientConfig.publisher.contextUrl
        def password = convention.clientConfig.publisher.password

        def body = [
                "publish"              : "true",
                "overrideExistingFiles": "false",
                "async"                : "true",
                "targetRepo"           : "maven",
                "sourceRepos"          : ["dexmaker"],
                "dryRun"               : "false"
        ]

        def bodyString = new JsonBuilder(body).toString()

        def content = Request.Post("$context/api/build/distribute/$buildName/$buildNumber")
                .bodyString(bodyString, ContentType.APPLICATION_JSON)
                .addHeader("X-JFrog-Art-Api", password)
                .execute()
                .returnContent()

        logger.lifecycle("Distribute Response: {}", content.asString())
    }
}
