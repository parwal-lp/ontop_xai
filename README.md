# KG-XAI Tool

A tool for generating explanations to the decisions of binary classifiers, through the Ontology-Based Data Access (OBDA) paradigm.

## Features

- 🖥️ **Graphical User Interface (GUI)**: Easy-to-use JavaFX interface
- 🔗 **MySQL Integration**: Connects to your MySQL Server database instance
- 🔗 **Ontop Integration**: Connects to the Ontop APIs to manage an OBDA instance with your database and ontology
- 🎯 **Configurable**: Customize the computed explanations through the border radius parameter

## Quick Start
Quick guide to install and execute the tool.

### Requirements
- **Java 21** or higher - [Download Java](https://www.oracle.com/java/technologies/downloads/)
- **MySQL Server** - [Download MySQL Server](https://dev.mysql.com/downloads/mysql/)
- [Optional] **MySQL Workbench** - [Download MySQL Workbench](https://dev.mysql.com/downloads/workbench/) (Graphical User Interface for MySQL)

  
### Installation
1. Download `ontop_xai.jar` from the [latest release](https://github.com/parwal-lp/ontop_xai/releases/latest)
2. Ensure MySQL Server is running
3. [Optional] For some useful examples, also download the content of the [`examples/`](/examples/) folder

### Configuration
Create a file named `local.properties` in the same directory you saved the executable `ontop_xai.jar`.
Edit the file using the following template, and fill in the information needed to connect to your MySQL Server installation:

```local.properties
jdbc.user =
jdbc.password =
jdbc.driver =
default.jdbc.host =
default.jdbc.port =
```

An example configuration file can be found in the source code at `config/local.properties`.

### Running
Open a terminal in the folder that contains both the executable `ontop_xai.jar` and the configuration file `local.properties`, and run the following command:
```bash
java -jar ontop_xai.jar
```

## Usage Guide
1. **Choose the Domain**: when the tool starts running, the first window asks you to specify the following:
    - The name of the database instance in MySQL to connect to
    - The ontology file for the chosen domain of interest
    - The mapping file linking the database instance to the domain ontology
2. **Load Lambda File**: In the next window, select the .csv file containing the classification output (i.e., the data samples you want to explain)
4. **Set Radius**: Adjust the explanation radius with a non negative number (default: 1)
5. **Generate Explanation**: Click "Compute Explanation" to start the explanation computation
6. **Adjust Radius**: Once the computation is completed, the explanation is displayed. You can either choose to keep it and save it to file, or recompute it with a different radius
7. **View Results**: When you obtain an explanation that is suitable for your needs, answer "no" when prompted to adjust the radius again. The computed explanation will be saved to: `output/<dbname>/explanation_<YY-mm-dd_HH:mm:ss>.txt`

During execution the tool creates two new folders in the same location of the `ontop_xai.jar` file:
- `output/` contains one subfolder per domain, containing the generated explanation files (e.g. `output/books/explanation_25-10-30_10:12:42.txt`), and a log file for debug purposes only.
- `domains/` stores per‑domain configuration and resources, used to speed up subsequent runs on the same domain (e.g. `domains/books/abox.nt`).


## Examples
Example domains (dataset, ontology, mapping) are included in the source code at `examples/`:
- **Books**: Simple dataset with authors, books, and editions
- **NPD**: Norwegian Petroleum Directorate Dataset

In order to run the tool on an example domain, follow these steps after completing the [Installation](#installation) and [Configuration](#configuration) phases.
1. Download the content of the folder `examples/npd` if you want to try the NDP domain, or `examples/books` for the Books domain. 
Here you will find four files: 
    - the .sql script to create the database;
    - the .owl file for the ontology;
    - the .r2rml file for the mappings;
    - the .csv file containing the samples to explain.
2. Run in MySQL Server the .sql script you just downloaded from either `examples/npd` or `examples/books`.
3. Run the `ontop_xai.jar` file using the command `java -jar ontop_xai.jar`, as explained in the [Running](#running) section.
4. When promted by the system, specify `npd` as database name for the NPD domain, and `books` for the Books domain.
Then select the .owl file as ontology, and the .r2rml file as mappings.
5. Select the .csv file as Data samples file, and then choose a radius (non-negative integer).
6. Click on "Compute Explanation" to generate the explanation.