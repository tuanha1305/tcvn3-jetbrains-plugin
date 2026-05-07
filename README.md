# tcvn3-jetbrains-plugin

Vietnamese **TCVN3** (also called ABC) charset support for JetBrains IDEs (IntelliJ IDEA, WebStorm, PyCharm, GoLand, Rider, RustRover, ...).

After installing, "TCVN3" appears in the IDE's encoding picker (status bar / `File | File Encoding`). File open / save / Find in Files / refactoring all work with TCVN3-encoded files like any built-in charset.

## Features

| | |
|---|---|
| **Charset registration** | via Java SPI (`java.nio.charset.spi.CharsetProvider`). Integrates with the IDE's built-in encoding system - no patches, no IDE files modified. |
| **Round-trip safe** | Non-TCVN3 bytes (e.g. GBK Chinese filenames mixed into game data files) decode into Unicode Private Use Area and re-encode byte-identical. Save without corrupting paths. |
| **Clipboard bridge** | Two extra actions for interop with Notepad++/Notepad in `ANSI + .VnTime` mode (which expect raw byte values, not Unicode). |
| **Cross-IDE** | Built against IntelliJ Community 2024.3, runs on every IntelliJ-based IDE from 2023.3 onwards. |

## Install

### Option 1: from GitHub Release (recommended)

1. Go to [Releases](https://github.com/tuanha1305/tcvn3-jetbrains-plugin/releases) and download the latest `tcvn3-jetbrains-plugin-X.Y.Z.zip`.
2. In your IDE: **Settings | Plugins | gear icon | Install Plugin from Disk...** and pick the `.zip`.
3. Restart the IDE.

### Option 2: from JetBrains Marketplace

(Pending publication.)

### Option 3: build from source

Requires JDK 17+ and an internet connection (Gradle will fetch the IntelliJ Platform).

```bash
git clone https://github.com/tuanha1305/tcvn3-jetbrains-plugin.git
cd tcvn3-jetbrains-plugin
gradle wrapper && ./gradlew buildPlugin
# Artifact: build/distributions/tcvn3-jetbrains-plugin-1.0.0.zip
```

## Usage

### Open / save TCVN3 files

After install + restart, any file opened in the IDE can be reopened in TCVN3:

- Click the encoding indicator in the status bar (right side) -> **Reload in another encoding...** -> **TCVN3**.
- Or set TCVN3 as the project default in **Settings | Editor | File Encodings**.

The IDE will then read/write that file's bytes through the TCVN3 codec.

### Clipboard bridge (Notepad++ interop)

Tools like Notepad++ display TCVN3 files via `ANSI + .VnTime` font - they treat each byte as a separate Unicode codepoint, not as a real Vietnamese character. Pasting Unicode from the IDE into such tools (or vice versa) drops bytes.

Use the bridge actions:

| Action | Default keymap | When to use |
|---|---|---|
| **TCVN3: Copy as Bytes** | `Ctrl+Alt+C` | Before pasting your selection into Notepad/Notepad++ in ANSI + .VnTime. |
| **TCVN3: Paste from Bytes** | `Ctrl+Alt+V` | After copying from Notepad/Notepad++ in ANSI + .VnTime. |

## Tests

```bash
./gradlew test
```

Covers reference vectors from the C# port, full round-trip of all 134 Vietnamese characters, PUA passthrough for unmapped bytes, mixed TCVN3+GBK byte streams, and streaming decoder chunk boundaries.

## Related

- **VS Code**: [vs-code-tcvn3-customize](https://github.com/tuanha1305/vs-code-tcvn3-customize) - same codec, drop-in `iconv-lite-umd` replacement.
- **Original codec**: [Custom TCVN3 Encoding Implementation - A Deep Dive](https://tuanha1305.github.io/2025/09/13/custom-tcvn3-encoding-implementation-a-deep-dive.html).

## License

MIT.
