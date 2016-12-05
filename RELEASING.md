Releasing
========

 1. Change the version in `gradle.properties` to a non-SNAPSHOT version.
 2. Update the `CHANGELOG.md` for the impending release.
 3. Update the `README.md` with the new version.
 4. `git commit -am "Prepare for release X.Y.Z."` (where X.Y.Z is the new version)
 5. `git tag -a X.Y.Z -m "Version X.Y.Z"` (where X.Y.Z is the new version)
 6. `./gradlew clean artifactory distributeBuild`
 7. Update the `gradle.properties` to the next SNAPSHOT version.
 8. `git commit -am "Prepare next development version."`
 9. `git push && git push --tags`
 10. Create a new release in the releases tab on GitHub


Prerequisites
-------------

Set the following environment variables:

 * `ARTIFACTORY_USER` - LinkedIn Artifactory username for releasing to `com.linkedin.dexmaker`.
 * `ARTIFACTORY_KEY` - LinkedIn Artifactory API key for releasing to `com.linkedin.dexmaker`.