# Dexmaker
[![Build Status](https://img.shields.io/github/workflow/status/linkedin/dexmaker/Merge%20checks)](https://img.shields.io/github/workflow/status/linkedin/dexmaker/Merge%20checks)

A Java-language API for doing compile time or runtime code generation targeting the Dalvik VM. Unlike
[cglib](http://cglib.sourceforge.net/) or [ASM](http://asm.ow2.org/), this library creates Dalvik `.dex`
files instead of Java `.class` files.

It has a small, close-to-the-metal API. This API mirrors the
[Dalvik bytecode specification](http://source.android.com/devices/tech/dalvik/dalvik-bytecode.html) giving you tight
control over the bytecode emitted. Code is generated instruction-by-instruction; you bring your own abstract
syntax tree if you need one. And since it uses Dalvik's `dx` tool as a backend, you get efficient register
allocation and regular/wide instruction selection for free.

## What does it do?

### Mockito Mocks
Dexmaker lets you use the [Mockito](https://github.com/mockito/mockito) mocking library in your
Android projects by generating Dalvik bytecode class proxies. Just add an
`androidTestImplementation` dependency on `dexmaker-mockito` and you can use Mockito in your Android Instrumentation tests.

The version of Mockito that Dexmaker targets can be found in `dexmaker-mockito`'s [build.gradle](https://github.com/linkedin/dexmaker/blob/master/dexmaker-mockito/build.gradle) file. The general rule is that the major and minor version of Dexmaker will match the underlying major and minor version of Mockito.

### Mocking Final Classes & Methods
Starting in Android "P", it is possible to mock final classes and methods using the `dexmaker-mockito-inline` library. If you execute your tests on a device or emulator running Android P or above, you can add an `androidTestImplementation` dependency on `dexmaker-mockito-inline` (instead of `dexmaker-mockito`; don't add both) and you can use the normal Mockito APIs to mock final classes and methods in your Android Instrumentation tests.

**NOTE:** This functionality requires OS APIs which were introduced in Android P and cannot work on older versions of Android.

### Class Proxies
Dexmaker includes a stock code generator for [class proxies](https://github.com/crittercism/dexmaker/blob/master/dexmaker/src/main/java/com/android/dx/stock/ProxyBuilder.java).
If you just want to do AOP or class mocking, you don't need to mess around with bytecodes.

### Runtime Code Generation
This example generates a class and a method. It then loads that class into the current process and invokes its method.

``` java
public final class HelloWorldMaker {
    public static void main(String[] args) throws Exception {
        DexMaker dexMaker = new DexMaker();

        // Generate a HelloWorld class.
        TypeId<?> helloWorld = TypeId.get("LHelloWorld;");
        dexMaker.declare(helloWorld, "HelloWorld.generated", Modifier.PUBLIC, TypeId.OBJECT);
        generateHelloMethod(dexMaker, helloWorld);

        // Create the dex file and load it.
        File outputDir = new File(".");
        ClassLoader loader = dexMaker.generateAndLoad(HelloWorldMaker.class.getClassLoader(),
                outputDir, outputDir);
        Class<?> helloWorldClass = loader.loadClass("HelloWorld");

        // Execute our newly-generated code in-process.
        helloWorldClass.getMethod("hello").invoke(null);
    }

    /**
     * Generates Dalvik bytecode equivalent to the following method.
     *    public static void hello() {
     *        int a = 0xabcd;
     *        int b = 0xaaaa;
     *        int c = a - b;
     *        String s = Integer.toHexString(c);
     *        System.out.println(s);
     *        return;
     *    }
     */
    private static void generateHelloMethod(DexMaker dexMaker, TypeId<?> declaringType) {
        // Lookup some types we'll need along the way.
        TypeId<System> systemType = TypeId.get(System.class);
        TypeId<PrintStream> printStreamType = TypeId.get(PrintStream.class);

        // Identify the 'hello()' method on declaringType.
        MethodId hello = declaringType.getMethod(TypeId.VOID, "hello");

        // Declare that method on the dexMaker. Use the returned Code instance
        // as a builder that we can append instructions to.
        Code code = dexMaker.declare(hello, Modifier.STATIC | Modifier.PUBLIC);

        // Declare all the locals we'll need up front. The API requires this.
        Local<Integer> a = code.newLocal(TypeId.INT);
        Local<Integer> b = code.newLocal(TypeId.INT);
        Local<Integer> c = code.newLocal(TypeId.INT);
        Local<String> s = code.newLocal(TypeId.STRING);
        Local<PrintStream> localSystemOut = code.newLocal(printStreamType);

        // int a = 0xabcd;
        code.loadConstant(a, 0xabcd);

        // int b = 0xaaaa;
        code.loadConstant(b, 0xaaaa);

        // int c = a - b;
        code.op(BinaryOp.SUBTRACT, c, a, b);

        // String s = Integer.toHexString(c);
        MethodId<Integer, String> toHexString
                = TypeId.get(Integer.class).getMethod(TypeId.STRING, "toHexString", TypeId.INT);
        code.invokeStatic(toHexString, s, c);

        // System.out.println(s);
        FieldId<System, PrintStream> systemOutField = systemType.getField(printStreamType, "out");
        code.sget(systemOutField, localSystemOut);
        MethodId<PrintStream, Void> printlnMethod = printStreamType.getMethod(
                TypeId.VOID, "println", TypeId.STRING);
        code.invokeVirtual(printlnMethod, null, localSystemOut, s);

        // return;
        code.returnVoid();
    }
}
```

## Download

For Mockito support, download the latest .jar via Maven:
```xml
    <dependency>
      <groupId>com.linkedin.dexmaker</groupId>
      <artifactId>dexmaker-mockito</artifactId>
      <version>2.28.1</version>
      <type>pom</type>
    </dependency>
```

or Gradle:
```
    androidTestImplementation 'com.linkedin.dexmaker:dexmaker-mockito:2.28.1'
```

_Note: The dependency on Mockito will be transitively included, so there's no need to specify both Mockito AND dexmaker-mockito_

## Snapshots

You can use snapshot builds to test the latest unreleased changes. A new snapshot is published
after every merge to the main branch by the [Deploy Snapshot Github Action workflow](.github/workflows/deploy-snapshot.yml).

Just add the Sonatype snapshot repository to your Gradle scripts:
```gradle
repositories {
    maven {
        url "https://oss.sonatype.org/content/repositories/snapshots/"
    }
}
```

You can find the latest snapshot version to use in the [gradle.properties](gradle.properties) file.
