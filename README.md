# Explainable AI with Ontop

A tool for generating explanations to the decisions of binary classifiers, through the Ontology-Based Data Access (OBDA) paradigm.

## Features

- 🖥️ **Graphical User Interface (GUI)**: Easy-to-use JavaFX interface
- 🔗 **MySQL Integration**: Connects to your database instance in MySQL Server
- 🔗 **Ontop Integration**: Connects to the Ontop APIs to manage an OBDA instance with your database and ontology
- 🎯 **Configurable**: Adjust the computed explanation through the border radius parameter

### Installation
1. Download the latest `ontop_xai.jar` from the releases
2. Download the source code .zip for some useful examples
3. Ensure MySQL Server is running
4. Create a configuration file

### Configuration
Create a file called `local.properties` with the following template, and fill it with the information to connect to your installation of MySQL Server:

```properties
jdbc.user =
jdbc.password =
jdbc.driver =
default.jdbc.host =
default.jdbc.port =
```

An example configuration is in `config/local.properties`.

## Requirements

- **Java 21** or higher
- **MySQL Server**

## Running
```bash
java -jar ontop_xai.jar
```

## Usage
1. **Specify Domain**: when the tool starts running, the first window asks the user to specify the following:
    - The name of the database instance in MySQL to connect to
    - The file with the ontology over the domain of interest
    - The file with the mappings from the database instance to the domain ontology
2. **Load Lambda File**: Select the .csv file containing the classification output (i.e., the data samples to explain)
4. **Set Radius**: Adjust the explanation radius with a non negative number (default: 1)
5. **Generate Explanation**: Click "Compute Explanation" to start the explanation computation
6. **Adjust Radius**: After the computation is completed, the explanation is displayed, and you can choose to keep it and save it to file, or recompute the explanation with a different radius
7. **View Results**: Find the computed explanation ad `output/<dbname>/explanation_<YY-mm-dd_HH:mm:ss>.txt`


## Examples
Example datasets are included in the source code at `resources/npd` and `resources/books`:
- **Books**: Simple dataset with authors, books, and editions
- **NPD**: Norwegian Petroleum Directorate real-world data