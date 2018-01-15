# Change Log

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
