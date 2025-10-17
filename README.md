# Explainable AI with Ontop

A Java application for generating explanations of query answers using Ontology-Based Data Access (OBDA) with the Ontop framework.

## Features

- 🖥️ **Graphical User Interface (GUI)**: Easy-to-use JavaFX interface
- ⌨️ **Command-Line Interface (CLI)**: Traditional terminal-based execution  
- 🔍 **ABox Materialization**: Automatic generation of ontology assertions from database
- 📊 **SPARQL Query Generation**: Creates Union of Conjunctive Queries (UCQs) as explanations
- 🎯 **Configurable Radius**: Control explanation scope with border radius parameter
- 📈 **Progress Tracking**: Real-time feedback on computation progress

## Quick Start

### GUI Mode (Recommended)

### CLI Mode (Developer)

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

## Configuration

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

## Using the GUI

1. **Launch**: Run `./run-gui.sh` or `mvn javafx:run`
2. **Configure**: 
   - Select your property file (Browse button)
   - Set the radius parameter (default: 1)
3. **Run**: Click "Start Explanation"
4. **Monitor**: Watch real-time progress in Console Output tab
5. **Review**: Check results in output files or Detailed Log tab

### GUI Features

- **Real-time Progress Bar**: Visual feedback during computation
- **Dual Output Panels**: 
  - Console Output: Main execution logs
  - Detailed Log: Algorithm-level details
- **Interactive Controls**: Start, Stop, Clear buttons
- **File Browser**: Easy navigation to configuration files

## How It Works

1. **Connect to Database**: Establishes connection via Ontop to your data source
2. **Materialize ABox**: Generates RDF triples representing your data as ontology assertions
3. **Load Lambda**: Reads input tuples (query answers to explain) from CSV
4. **Compute Borders**: For each tuple, finds assertions within specified radius
5. **Generate SPARQL**: Translates borders into SPARQL Union of Conjunctive Queries
6. **Output Results**: Writes explanation queries to file

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

## Dependencies

- **Ontop 5.2.0**: OBDA framework for ontology-database mapping
- **RDF4J 4.1.3**: RDF processing and querying
- **JavaFX 21.0.1**: Modern GUI framework
- **MySQL Connector 9.3.0**: Database connectivity
- **OpenCSV 5.3**: CSV file parsing
- **Logback 1.2.13**: Logging framework