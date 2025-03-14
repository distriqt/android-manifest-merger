built by [distriqt //](https://distriqt.com)

---

## Android Manifest Merger

This is a wrapper around the [`com.android.tools.build:manifest-merger` library](https://mvnrepository.com/artifact/com.android.tools.build/manifest-merger) allowing access to the library using the packaged `jar` on the command line

It allows you to manually merge Android manifest files.

---

### Usage

```
java -jar manifest-merger.jar
    --main mainAndroidManifest.xml
    --log [VERBOSE, INFO, WARNING, ERROR]
    --libs [path separated list of lib's manifests]
    --overlays [path separated list of overlay's manifests]
    --property [PACKAGE | VERSION_CODE | VERSION_NAME | MIN_SDK_VERSION | TARGET_SDK_VERSION | MAX_SDK_VERSION | NAME | TARGET_PACKAGE | FUNCTIONAL_TEST | HANDLE_PROFILING | LABEL=value]
    --placeholder [name=value]
    --out [path of the output file]
    --remove-tools-declarations
```

Eg:

```
java -jar manifest-merger.jar
    --main AndroidManifest.xml
    --libs libraryA/AndroidManifest.xml:libraryB/AndroidManifest.xml
```

---

### Build

This is a gradle project and includes an IntelliJ project for editing.

To build, create a local.properties file in the root of the project with the following:

```properties
sdk.dir=/path/to/android/sdk
```

Then run:

```
./gradlew build
```
