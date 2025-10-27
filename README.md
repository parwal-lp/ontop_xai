# Explainable AI with Ontop

A tool for generating explanations to the decisions of binary classifiers, through the Ontology-Based Data Access (OBDA) paradigm.

## Features

- 🖥️ **Graphical User Interface (GUI)**: Easy-to-use JavaFX interface
- 🔗 **MySQL Integration**: Connects to your database instance in MySQL Server
- 🔗 **Ontop Integration**: Connects to the Ontop APIs to manage an OBDA instance with your database and ontology
- 🎯 **Configurable**: Adjust the computed explanation through the border radius parameter

## Requirements

- **Java 21** or higher
- **MySQL Server**
  
### Installation
1. Download the latest `ontop_xai.jar` from the releases
2. Ensure MySQL Server is running
3. [Optional] Download the source code .zip for some useful examples

### Configuration
Create a file called `local.properties` in the same directory you saved the executable `ontop_xai.jar`.
Edit the file using the following template, and fill it with the information to connect to your installation of MySQL Server.

```properties
jdbc.user =
jdbc.password =
jdbc.driver =
default.jdbc.host =
default.jdbc.port =
```

An example configuration is in the source code at `config/local.properties`.



## Running
Execute the following command from the directory where you saved both the executable `local.properties` and the configuration file `local.properties`.
```bash
java -jar ontop_xai.jar
```

## Usage Guide
1. **Choose the Domain**: when the tool starts running, the first window asks the user to specify the following:
    - The name of the database instance in MySQL to connect to
    - The file of the ontology for the chosen the domain of interest
    - The file with the mappings from the database instance to the domain ontology
2. **Load Lambda File**: In the next window, select the .csv file containing the classification output (i.e., the data samples you want to explain)
4. **Set Radius**: Adjust the explanation radius with a non negative number (default: 1)
5. **Generate Explanation**: Click "Compute Explanation" to start the explanation computation
6. **Adjust Radius**: After the computation is completed, the explanation is displayed, and you can choose to keep it and save it to file, or recompute the explanation with a different radius
7. **View Results**: When you obtain an explanation that is suitable for your needs, answer "no" to the question that propmts for radius adjustment, and find the computed explanation ad `output/<dbname>/explanation_<YY-mm-dd_HH:mm:ss>.txt`


## Examples
Example datasets are included in the source code at `resources/npd` and `resources/books`:
- **Books**: Simple dataset with authors, books, and editions
- **NPD**: Norwegian Petroleum Directorate Dataset
