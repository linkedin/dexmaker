dispatcher.jar is the classes.dex of the apk created by dexmaker-mockito-inline-dispatcher
repackaged into a jar. We should automate this.

unzip dexmaker-mockito-inline-dispatcher/build/outputs/apk/release/dexmaker-mockito-inline
-dispatcher-release-unsigned.apk classes.dex
jar -cf dexmaker-mockito-inline/src/main/resources/dispatcher.jar classes.dex
rm classes.dex
