# tcvn3-jetbrains-plugin

Vietnamese **TCVN3** (also called ABC) charset for JetBrains IDEs (IntelliJ IDEA, WebStorm, PyCharm, GoLand, Rider, RustRover, ...).

After installing, "TCVN3" appears in the IDE's encoding picker (status bar / `File | File Encoding`). File open / save / Find in Files / refactoring all work with TCVN3-encoded files like any built-in charset.

## How it works

The plugin registers `TCVN3` as a `java.nio.charset.Charset` via the standard Java SPI (`META-INF/services/java.nio.charset.spi.CharsetProvider`). The IntelliJ Platform's encoding system reads from `Charset.availableCharsets()`, so no IDE files are patched - it just plugs into the JVM's built-in charset framework.

**Round-trip safe**: non-TCVN3 bytes (e.g. GBK Chinese filenames inside game data files) decode into the Unicode Private Use Area (`U+E080..U+E0FF`) and re-encode byte-identical. Saving never corrupts mixed-encoding files.

## Install

### From GitHub Release

1. Go to [Releases](https://github.com/tuanha1305/tcvn3-jetbrains-plugin/releases) and download the latest `tcvn3-jetbrains-plugin-X.Y.Z.zip`.
2. In your IDE: **Settings | Plugins | gear icon | Install Plugin from Disk...** -> pick the `.zip`.
3. Restart the IDE.

### From source

Requires JDK 17+ and an internet connection (Gradle fetches the IntelliJ Platform).

```bash
git clone https://github.com/tuanha1305/tcvn3-jetbrains-plugin.git
cd tcvn3-jetbrains-plugin
gradle wrapper && ./gradlew buildPlugin
# Artifact: build/distributions/tcvn3-jetbrains-plugin-1.0.0.zip
```

## Usage

After install + restart:

- **Per file**: status bar (right side) -> click encoding indicator -> **Reload in another encoding...** -> **TCVN3**.
- **Per project**: **Settings | Editor | File Encodings** -> pick **TCVN3** as project default or per-path mapping.

The IDE then reads/writes that file's bytes through the TCVN3 codec. Find in Files, refactoring, diff/merge - everything that uses the IDE's text infrastructure works.

## Tests

```bash
./gradlew test
```

Covers:
- Reference vectors from the C# port.
- Full round-trip of all 134 Vietnamese characters.
- PUA passthrough for unmapped bytes (round-trip safety).
- Mixed TCVN3 + GBK byte streams (real-world game data file pattern).
- Streaming decoder chunk boundaries.

## Related

- **VS Code**: [vs-code-tcvn3-customize](https://github.com/tuanha1305/vs-code-tcvn3-customize) - same codec, drop-in `iconv-lite-umd` replacement.
- **Original codec deep-dive**: [Custom TCVN3 Encoding Implementation - A Deep Dive](https://tuanha1305.github.io/2025/09/13/custom-tcvn3-encoding-implementation-a-deep-dive.html).

## License

MIT.
