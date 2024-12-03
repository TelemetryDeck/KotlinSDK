## Releasing a new version of the library

1. Update the library coordinates by incrementing the version in https://github.com/TelemetryDeck/KotlinSDK/blob/lib/build.gradle.kts#L103.
2. Update the README.md to instruct new users to use the latest version.
3. Commit and push.

### Publishing using GitHub Actions

Using the Publish GitHub action will release the library update.

### Publishing an update locally

The update can be triggered locally provided all environment variables are set:

```bash
./gradlew publishAndReleaseToMavenCentral --no-configuration-cache
```

The update progress can be tracked at https://central.sonatype.com/publishing/deployments