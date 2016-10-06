This is a fork of the original Crittercism Dexmaker. See https://github.com/crittercism/dexmaker for the main project.

Why does this exist? We wanted to use Mockito 2.1.0, which isn't compatible with the current Dexmaker. So we decided to patch up the small bug rather than wait for the owners of Dexmaker to adopt the fix. Once they create a new release, this will likely be removed.

To use this version of the library, you can use Jitpack:

```groovy
allprojects {
  repositories {
    ...
    maven { url "https://jitpack.io" }
  }
}
```

```groovy
dependencies {
  compile 'com.github.blueapron:dexmaker:0.0.0'
```