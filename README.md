# Explainable AI with Ontop

A tool for generating explanations to the decisions of binary classifiers, through the Ontology-Based Data Access (OBDA) paradigm.

## Features

- 🖥️ **Graphical User Interface (GUI)**: Easy-to-use JavaFX interface
- 🔗 **MySQL Integration**: Connects to your database instance in MySQL Server
- 🔗 **Ontop Integration**: Connects to the Ontop APIs to manage an OBDA instance with your database and ontology
- 🎯 **Configurable**: Control the computed explanation with border radius parameter

## Quick Start

### Installing

### Configuration
Create a `.properties` file with your database and ontology settings:

```properties
# Database connection
jdbc.url=jdbc:mysql://localhost:3306/database_name
jdbc.user=username
jdbc.password=password
jdbc.driver=com.mysql.cj.jdbc.Driver

# Ontology files
owlFile=path/to/ontology.owl
mappingFile=path/to/mappings.r2rml

# Input/Output files
lambdaFile=path/to/lambda.csv
aboxFile=path/to/output/abox.nt
logFile=path/to/output/log.txt
explFile=path/to/output/explanation.txt
```

Example configurations are in `src/main/resources/`.

## Requirements

- **Java 21** or higher
- **Maven 3.6+**
- **MySQL**
- **JavaFX SDK**

## Project Structure

```
ontop_xai/
├── src/main/java/it/expai/
│   ├── gui/                              # GUI components
│   │   ├── ExplainableAIOntopGUI.java   # Main GUI application
│   │   └── ExplanationWorker.java       # Background worker thread
│   ├── ExplainableAIOntop.java          # CLI main class
│   ├── UtilsImpl.java                   # Core algorithms
│   ├── Concept.java                     # Concept assertions
│   ├── Role.java                        # Role assertions
│   └── MembershipAssertion.java         # Base assertion class
├── src/main/resources/
│   ├── books/                           # Example: Books dataset
│   ├── npd/                             # Example: Norwegian Petroleum dataset
│   └── logback.xml                      # Logging configuration
├── pom.xml                               # Maven configuration
├── run-gui.sh                            # GUI launcher script
├── README.md                             # This file
└── GUI_README.md                         # Detailed GUI documentation

```

## Examples

### Books Dataset
Simple example with authors, books, and editions:
```bash
# Edit property file to use books example
# src/main/resources/example/books/exampleBooks.properties
```

### Norwegian Petroleum Directorate (NPD)
Real-world dataset about oil & gas operations:
```bash
# Edit property file to use NPD example  
# src/main/resources/npd/npd.properties
```