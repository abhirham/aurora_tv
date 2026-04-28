# Project Notes

- Always refer to @CODE_INDEX.md to figure out which file to work on for a task.
- Refer to @schema.md for the actual data schema (Room tables, Xtream source fields, sync behavior) used to render media and live TV in the app.
- Java location for Gradle builds: `JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home` (the system `java` is not on PATH, so set this when invoking `./gradlew`).
