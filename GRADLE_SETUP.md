# Gradle Wrapper Initialization

The gradle-wrapper.jar file is not included in the repository by default for security reasons.

## To initialize Gradle wrapper locally:

1. **Install Gradle** (if not already installed):
   ```bash
   # On macOS
   brew install gradle

   # On Linux
   wget https://gradle.org/install/ && follow instructions

   # Or using SDKMAN
   sdk install gradle
   ```

2. **Generate gradle-wrapper.jar**:
   ```bash
   cd CompositionHelper
   gradle wrapper --gradle-version 8.2
   ```

3. **Commit the gradle-wrapper.jar**:
   ```bash
   git add gradle/wrapper/gradle-wrapper.jar
   git commit -m "Add gradle-wrapper.jar"
   git push origin android-apk
   ```

## After adding gradle-wrapper.jar:

GitHub Actions will be able to build the APK automatically on every push to the android-apk branch.

## Alternative: Using Gradle without wrapper

If you prefer not to commit gradle-wrapper.jar, you can modify the workflow to use the Gradle distribution directly:

```yaml
- name: Build APK
  run: |
    curl -s "https://get.sdkman.io" | bash
    source "$HOME/.sdkman/bin/sdkman-init.sh"
    sdk install gradle 8.2
    gradle assembleDebug
```

However, using the wrapper is recommended for consistency across different environments.
