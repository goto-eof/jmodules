# JModules: A Java Module Info Extractor for JPackage Preparation

JModules is a lightweight Swing-based desktop application designed to **simplify the process of extracting module
information** from Java applications. By identifying precisely which Java modules are required, it helps developers
create optimized `jpackage` distributions, thereby **reducing the final package size** for more efficient deployment. It
processes modules from individual JARs, entire directories of JARs, or Maven `pom.xml` files.

---

### Prerequisites:

Before using JModules, ensure you have the following installed and properly configured in your system's PATH:

* **Java Development Kit (JDK):** Required for `jdeps` (typically included with JDK 9+).
* **Apache Maven:** Required for processing `pom.xml` files.

---

### Key Features:

* **Flexible JAR Input:** Process single JAR files, recursively scan entire directories for JARs, or extract
  dependencies from a Maven `pom.xml` file.
* **Module Information Extraction:** Utilizes the `jdeps` command-line tool (part of the JDK) to analyze JARs and
  determine their required Java modules.
* **Optimized Packaging Preparation:** Helps you list *only* the necessary modules, enabling `jpackage` to create
  smaller, more efficient application bundles.
* **GUI-Driven Workflow:** A user-friendly Swing interface allows for easy input of paths, selection of Java versions,
  and visualization of extracted module data.
* **Progress Tracking:** A progress bar provides visual feedback during potentially long processing operations.
* **Clipboard Integration:** Easily copy the generated `jpackage --add-modules` command string to your clipboard,
  streamlining your build process.
* **Asynchronous Processing:** All heavy-lifting operations run on a dedicated background thread, ensuring the GUI
  remains responsive and preventing freezing.
* **Robust Error Handling:** Provides clear error messages to the user via `JOptionPane` for issues encountered during
  file processing or command execution, with detailed logging for debugging.

---

### How it Works:

JModules acts as a wrapper around the `jdeps` tool. You provide the application with the path to your JAR(s) or
`pom.xml`, specify the Java version (e.g., `11`), and the application does the rest:

1. **Locate JARs:** Scans the specified path to identify all relevant JAR files.
2. **Run `jdeps`:** Executes `jdeps` for each identified JAR to determine its module dependencies.
3. **Aggregate Modules:** Collects and deduplicates all discovered module names.
4. **Generate Command:** Formats the collected module names into a `jpackage` compatible `--add-modules` argument.
5. **Display Results:** Shows the individual JAR-to-module mappings and the final aggregated module list in the GUI.

This tool is particularly useful for migrating legacy applications to the Java Module System or for simplifying the
creation of self-contained `jpackage` installers with **a minimal footprint**.