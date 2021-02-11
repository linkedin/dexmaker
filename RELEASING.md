# Releasing

1. Change the version in `gradle.properties` to a non-SNAPSHOT version.
2. Update the `CHANGELOG.md` for the impending release.
3. Update the `README.md` with the new version.
4. `git commit -am "Prepare for release X.Y.Z."` (where X.Y.Z is the new version)
5. `git tag -a X.Y.Z -m "Version X.Y.Z"` (where X.Y.Z is the new version)
6. Update the `gradle.properties` to the next SNAPSHOT version.
7. `git commit -am "Prepare next development version."`
8. `git push && git push --tags`
9. Create a new release in the releases tab on GitHub
10. Wait for the [publish-maven-central.yml](.github/workflows/publish-maven-central.yml) action to complete.
11. Visit [Sonatype Nexus](https://oss.sonatype.org/) and promote the artifact.

## How it works

The [deploy-snapshot.yml](.github/workflows/deploy-snapshot.yml) workflow runs on every
push to the main branch as long as the commit message does not contain `Prepare for release`. This
workflow calls Gradle to publish to the Sonatype snapshot repository.

For actual releases, there is a separate [publish-maven-central.yml](.github/workflows/publish-maven-central.yml)
workflow which runs after a new release is created in the GitHub UI. This will call Gradle on the
tagged release commit and upload to the staging repository. After that completes, you will need to
go and promote the artifacts to production.
