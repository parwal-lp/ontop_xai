# Explainable AI with Ontop

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
- **MySQL Server** - [Download MySQL](https://dev.mysql.com/downloads/mysql/)

  
### Installation
1. Download `ontop_xai.jar` from the latest release
2. Ensure MySQL Server is running
3. [Optional] For some useful examples download the source code (zip or tar.gz) of the latest release

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
Run the following command from the directory where both the executable `ontop_xai.jar` and the configuration file `local.properties` are located:
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


## Examples
Example domains (dataset, ontology, mapping) are included in the source code at `resources/npd` and `resources/books`:
- **Books**: Simple dataset with authors, books, and editions
- **NPD**: Norwegian Petroleum Directorate Dataset
