# Change Log

## Version 2.25.1 (2019-11-21)
- Add support for abstract and native methods generation - [#144](https://github.com/linkedin/dexmaker/pull/144)
- Allow ProxyBuilder to call hidden APIs - [#150](https://github.com/linkedin/dexmaker/pull/150)

## Version 2.25.0 (2019-03-21)
- Update Mockito to 2.25.0
- Fix crash in InspectClass - [#134](https://github.com/linkedin/dexmaker/issues/134)
- Implement new InlineMockMaker interface in Mockito 2.25.0 - [#137](https://github.com/linkedin/dexmaker/issues/137)

## Version 2.21.0 (2019-01-21)
- Correctly handle shared classloaders
- Update to Mockito 2.21.0
- Allow for AccessFlags.ACC_SYNTHETIC
- Add support for BRIDGE methods and SYNTHETIC fields
- Update Dalvik-dx to 9.0.0_r3
- Do not consider method modifers while sorting [#70](https://github.com/linkedin/dexmaker/issues/70)
- Consider interfaces implemented when generating proxy classes [#124](https://github.com/linkedin/dexmaker/issues/124)
- Improve AppDataDirGuesser to work for secondary Android users [#128](https://github.com/linkedin/dexmaker/issues/128)

## Version 2.19.1 (2018-07-25)
- Allow starting of static-spy-session for objects without 0-arg constructor
- Allow static advice to see hidden APIs

## Version 2.19.0 (2018-07-06)
- Removed last usages of internal Mockito APIs - #46
- Added experimental support for mocking static methods on Android P+ via the `dexmaker-mockito-inline-extended` artifact - #97
- Updated `dexmaker-mockito-inline` to compile against finalized Android P APIs
- Updated `dexmaker-mockito-inline` to allow mocks to call blacklisted APIs on Android P - #106
- Updated underlying Mockito version to 2.19.0

## Version 2.16.0 (2018-03-26)
- New dexmaker-mockito-inline artifact which implements the Mockito inline API and allows mocking final classes & methods on Android P+
  - **NOTE:** Using this artifact requires compiling and running with at least Android P Developer Preview 1
- ByteBuddy is now excluded from the transitive dependency on Mockito, which should bring significant method count savings for dexmaker-mockito users - #82
- Update underlying Mockito version to 2.16.0 - #85
- Allow opt-in mocking of package private classes - #72
- Better stack trace cleaning for dexmaker-mockito - #73

## Version 2.12.1 (2018-01-15)
- Add support for generating method annotations - #75

## Version 2.12.0 (2018-01-15)
- Update underlying Mockito version to 2.12.0
- Add support for generating static initializers - #57
- Add support for generating if-testz instructions - #58
- Fixed bug where `ProxyBuilder` did not correctly identify all interfaces to be implemented - #61

## Version 2.2.0 (2016-12-09)
- Update underlying Mockito version to 2.2.29
- Major and minor version numbers will now be in sync with the Mockito version that's supported by dexmaker-mockito

## Version 1.5.1 (2016-12-07)
- Fix bug in generated pom files that broke transitive dependencies

## Version 1.5.0 (2016-12-07)

- Project ownership transferred to LinkedIn
- Updated dx dependency to latest tag release from AOSP (7.1.0_r7) and switched from copied source to [Jake Wharton's repackaged artifact](https://github.com/JakeWharton/dalvik-dx)
- Converted tests to run as Android tests rather than in Vogar
- Fixed monitorExit instructions being added as monitorEnter
- Updated Mockito dependency to version 1.10.19
- Fixed transitive dependency configuration [#22](https://github.com/linkedin/dexmaker/issues/22)

## Version 1.4 (2015-07-23)

## Version 1.3 (2015-06-22)
