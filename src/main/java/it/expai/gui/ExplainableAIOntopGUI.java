package it.expai.gui;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import java.io.File;
import java.util.function.Consumer;

public class ExplainableAIOntopGUI extends Application {

    private TextField lambdaFileField;
    private TextField radiusField;
    private Label titleLabel;
    private TextArea detailsArea;
    private TextArea explanationArea;
    private Button startButton;
    private Button stopButton;
    private Button clearButton;
    private Label statusLabel;
    
    private ExplanationWorker currentWorker;
    
    // Configurazione iniziale - memorizzata in memoria
    private String configuredPropertyFile;
    @SuppressWarnings("unused") // Reserved for future use and configuration display
    private String configuredOwlFile;
    @SuppressWarnings("unused") // Reserved for future use and configuration display
    private String configuredMappingFile;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("KG-XAI tool");

        // Main layout
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(10));

        // Top section - Config
        VBox topSection = createConfigurationSection();
        root.setTop(topSection);

        // Status label section
        HBox statusSection = new HBox();
        statusSection.setPadding(new Insets(10, 10, 5, 10));
        statusLabel = new Label("Ready");
        statusLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold;");
        statusSection.getChildren().add(statusLabel);

        // Center section - Log + Explanation + Status
        VBox centerSection = createOutputSection();
        VBox centerWithStatus = new VBox();
        centerWithStatus.getChildren().addAll(centerSection, statusSection);
        VBox.setVgrow(centerSection, Priority.ALWAYS);
        root.setCenter(centerWithStatus);

        // Bottom section - Controls
        HBox bottomSection = createControlSection();
        root.setBottom(bottomSection);

        Scene scene = new Scene(root, 1000, 700);
        primaryStage.setScene(scene);
        primaryStage.show();
        
        // Show initial setup dialog after the UI is fully initialized
        Platform.runLater(() -> {
            if (!showInitialSetupDialog()) {
                Platform.exit();
            }
        });
        
        // Handle window close
        primaryStage.setOnCloseRequest(event -> {
            Platform.exit();
        });
    }

    private VBox createConfigurationSection() {
        VBox section = new VBox(10);
        section.setPadding(new Insets(10, 10, 10, 10));
        section.setStyle("-fx-border-color: #cccccc; -fx-border-width: 1; -fx-border-radius: 5;");

        // Titolo con nome del database
        titleLabel = new Label("Input Data for computing the Explanation");
        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        // Lambda file selection (data samples to explain)
        HBox lambdaFileBox = new HBox(10);
        lambdaFileBox.setAlignment(Pos.CENTER_LEFT);
        Label lambdaLabel = new Label("Data samples:");
        lambdaLabel.setPrefWidth(120);
        lambdaFileField = new TextField();
        lambdaFileField.setPrefWidth(500);
        lambdaFileField.setPromptText("Select CSV file with data samples to explain");
        Button browseLambdaButton = new Button("Browse...");
        browseLambdaButton.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Select Data Samples File");
            fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("CSV Files", "*.csv")
            );
            File selectedFile = fileChooser.showOpenDialog(lambdaFileField.getScene().getWindow());
            if (selectedFile != null) {
                lambdaFileField.setText(selectedFile.getAbsolutePath());
            }
        });
        lambdaFileBox.getChildren().addAll(lambdaLabel, lambdaFileField, browseLambdaButton);

        // Radius selection
        HBox radiusBox = new HBox(10);
        radiusBox.setAlignment(Pos.CENTER_LEFT);
        Label radiusLabel = new Label("Radius:");
        radiusLabel.setPrefWidth(120);
        radiusField = new TextField("1");
        radiusField.setPrefWidth(100);
        radiusBox.getChildren().addAll(radiusLabel, radiusField);

        section.getChildren().addAll(titleLabel, lambdaFileBox, radiusBox);
        return section;
    }

    private VBox createOutputSection() {
        VBox section = new VBox(10);
        section.setPadding(new Insets(10));
        VBox.setVgrow(section, Priority.ALWAYS);

        // Output tabs
        TabPane tabPane = new TabPane();
        VBox.setVgrow(tabPane, Priority.ALWAYS);

        // Computation Details tab
        Tab detailsTab = new Tab("Computation Details");
        detailsTab.setClosable(false);
        detailsArea = new TextArea();
        detailsArea.setEditable(false);
        detailsArea.setWrapText(true);
        detailsArea.setStyle("-fx-font-family: monospace; -fx-font-size: 12px;");
        detailsTab.setContent(detailsArea);

        // Explanation tab
        Tab explanationTab = new Tab("Explanation");
        explanationTab.setClosable(false);
        explanationArea = new TextArea();
        explanationArea.setEditable(false);
        explanationArea.setWrapText(true);
        explanationArea.setStyle("-fx-font-family: monospace; -fx-font-size: 12px;");
        explanationTab.setContent(explanationArea);

        tabPane.getTabs().addAll(detailsTab, explanationTab);
        section.getChildren().add(tabPane);

        return section;
    }

    private HBox createControlSection() {
        HBox section = new HBox(10);
        section.setPadding(new Insets(10, 10, 10, 10));
        section.setAlignment(Pos.CENTER_LEFT);

        startButton = new Button("Compute Explanation");
        startButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold;");
        startButton.setPrefWidth(200);
        startButton.setOnAction(e -> startExplanation());

        stopButton = new Button("Stop");
        stopButton.setStyle("-fx-background-color: #f44336; -fx-text-fill: white; -fx-font-weight: bold;");
        stopButton.setPrefWidth(100);
        stopButton.setDisable(true);
        stopButton.setOnAction(e -> stopExplanation());

        clearButton = new Button("Clear Output");
        clearButton.setPrefWidth(120);
        clearButton.setOnAction(e -> {
            detailsArea.clear();
            explanationArea.clear();
            statusLabel.setText("Ready");
        });

        section.getChildren().addAll(
            startButton, stopButton, clearButton
        );

        return section;
    }

    private void startExplanation() {
        // Use configured property file from initial setup
        String propertyFile = configuredPropertyFile;
        String lambdaFile = lambdaFileField.getText().trim();
        if (propertyFile == null || propertyFile.isEmpty()) {
            showAlert("Error", "Configuration not found. Please restart the application.");
            return;
        }
        
        if (!new File(propertyFile).exists()) {
            showAlert("Error", "Property file does not exist: " + propertyFile);
            return;
        }
        
        int radius;
        try {
            radius = Integer.parseInt(radiusField.getText().trim());
            if (radius < 0) {
                showAlert("Error", "Radius must be a non-negative integer");
                return;
            }
        } catch (NumberFormatException e) {
            showAlert("Error", "Invalid radius value");
            return;
        }

        detailsArea.clear();
        explanationArea.clear();

        startButton.setDisable(true);
        stopButton.setDisable(false);
        statusLabel.setText("Running...");


        Consumer<String> outputCallback = message -> 
            Platform.runLater(() -> detailsArea.appendText(message + "\n"));
        
        Consumer<String> explCallback = message -> 
            Platform.runLater(() -> explanationArea.appendText(message));
        
        
        Runnable onComplete = () -> Platform.runLater(() -> {
            startButton.setDisable(false);
            stopButton.setDisable(true);
            statusLabel.setText("Completed");
        });
        
        Runnable onError = () -> Platform.runLater(() -> {
            startButton.setDisable(false);
            stopButton.setDisable(true);
            statusLabel.setText("Error occurred");
        });

        Runnable onStopped = () -> Platform.runLater(() -> {
            startButton.setDisable(false);
            stopButton.setDisable(true);
            detailsArea.appendText("\nComputation Stopped by User.");
            statusLabel.setText("Stopped");
        });

        currentWorker = new ExplanationWorker(
            propertyFile, lambdaFile, radius, outputCallback, explCallback, 
            onComplete, onError, onStopped
        );
        
        new Thread(currentWorker).start();
    }

    public void stopExplanation() {
        if (currentWorker != null) {
            currentWorker.stopExplanation();
            startButton.setDisable(true);
            stopButton.setDisable(true);
            clearButton.setDisable(true);
            statusLabel.setText("Stopping...");
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private boolean showInitialSetupDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Initial Configuration");
        dialog.setHeaderText("Configure Database and Ontology");
        
        // Create form fields
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        
        // Database name
        TextField dbNameField = new TextField();
        dbNameField.setPrefWidth(300);
        
        // Ontology file
        TextField owlFileField = new TextField();
        owlFileField.setPrefWidth(300);
        Button browseOwlButton = new Button("Browse...");
        HBox owlBox = new HBox(5, owlFileField, browseOwlButton);
        
        browseOwlButton.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Select OWL Ontology File");
            fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("OWL Files", "*.owl")
            );
            File selectedFile = fileChooser.showOpenDialog(dialog.getOwner());
            if (selectedFile != null) {
                owlFileField.setText(selectedFile.getAbsolutePath());
            }
        });
        
        // Mapping file
        TextField mappingFileField = new TextField();
        mappingFileField.setPrefWidth(300);
        Button browseMappingButton = new Button("Browse...");
        HBox mappingBox = new HBox(5, mappingFileField, browseMappingButton);
        
        browseMappingButton.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Select R2RML Mapping File");
            fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("R2RML Files", "*.r2rml", "*.ttl")
            );
            File selectedFile = fileChooser.showOpenDialog(dialog.getOwner());
            if (selectedFile != null) {
                mappingFileField.setText(selectedFile.getAbsolutePath());
            }
        });
        
        // Add to grid
        grid.add(new Label("Database Name:"), 0, 0);
        grid.add(dbNameField, 1, 0);
        
        grid.add(new Label("Ontology File (.owl):"), 0, 1);
        grid.add(owlBox, 1, 1);
        
        grid.add(new Label("Mapping File (.r2rml):"), 0, 2);
        grid.add(mappingBox, 1, 2);
        
        dialog.getDialogPane().setContent(grid);
        
        ButtonType okButtonType = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButtonType = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(okButtonType, cancelButtonType);
        
        // Request focus
        Platform.runLater(() -> dbNameField.requestFocus());
        
        // Show dialog
        var result = dialog.showAndWait();
        
        if (result.isPresent() && result.get() == okButtonType) {
            String dbName = dbNameField.getText().trim();
            String owlPath = owlFileField.getText().trim();
            String mappingPath = mappingFileField.getText().trim();
            
            // Validate input
            if (dbName.isEmpty() || owlPath.isEmpty() || mappingPath.isEmpty()) {
                showAlert("Configuration Error", "All fields are required!");
                return showInitialSetupDialog(); // Retry
            }
            
            if (!new File(owlPath).exists()) {
                showAlert("File Not Found", "Ontology file does not exist: " + owlPath);
                return showInitialSetupDialog();
            }
            
            if (!new File(mappingPath).exists()) {
                showAlert("File Not Found", "Mapping file does not exist: " + mappingPath);
                return showInitialSetupDialog();
            }
            
            // Generate property file
            try {
                String generatedPropertyFile = generatePropertyFile(dbName, owlPath, mappingPath);
                
                // Store configuration in memory
                configuredPropertyFile = generatedPropertyFile;
                configuredOwlFile = owlPath;
                configuredMappingFile = mappingPath;

                // Update the title label with the database name
                Platform.runLater(() -> {
                    titleLabel.setText("Input for computing the explanation (" + dbName + ")");
                });
                
                return true;
            } catch (Exception e) {
                showAlert("Configuration Error", "Failed to generate property file: " + e.getMessage());
                e.printStackTrace();
                return false;
            }
        }
        
        return false; // User cancelled the computation
    }
    
    private String generatePropertyFile(String dbName, String owlPath, String mappingPath) throws Exception {
        // Load user properties from config/local.properties
        java.util.Properties userProps = new java.util.Properties();
        File userFile = new File("config/local.properties");
        
        if (!userFile.exists()) {
            throw new Exception("Base configuration file not found: config/local.properties\n" +
                              "Please create it with jdbc.user, jdbc.password, jdbc.driver, jdbc.host, jdbc.port");
        }
        
        try (java.io.FileInputStream fis = new java.io.FileInputStream(userFile)) {
            userProps.load(fis);
        }
        
        // Build jdbc.url with the database name
        String host = userProps.getProperty("jdbc.host", "localhost");
        String port = userProps.getProperty("jdbc.port", "3306");
        String jdbcUrl = String.format(
            "jdbc:mysql://%s:%s/%s?useUnicode=true&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=UTC",
            host, port, dbName
        );
        
        // Create the resources/dbname directory
        File resourcesDir = new File("resources/" + dbName);
        if (!resourcesDir.exists()) {
            resourcesDir.mkdirs();
            //System.out.println(">>> Created directory: " + resourcesDir.getAbsolutePath());
        }
        
        // Create the property file
        java.util.Properties fullProps = new java.util.Properties();
        fullProps.setProperty("jdbc.url", jdbcUrl);
        fullProps.setProperty("jdbc.user", userProps.getProperty("jdbc.user", "root"));
        fullProps.setProperty("jdbc.password", userProps.getProperty("jdbc.password", "password"));
        fullProps.setProperty("jdbc.driver", userProps.getProperty("jdbc.driver", "com.mysql.cj.jdbc.Driver"));
        fullProps.setProperty("owlFile", owlPath);
        fullProps.setProperty("mappingFile", mappingPath);
        fullProps.setProperty("aboxFile", "resources/" + dbName + "/abox.nt");
        fullProps.setProperty("logFile", "output/" + dbName + "/log.txt");
        fullProps.setProperty("explFile", "output/" + dbName + "/explanation.txt");
        
        // Save to resources/dbname/dbname.properties
        File propertyFile = new File(resourcesDir, dbName + ".properties");
        
        try (java.io.FileOutputStream fos = new java.io.FileOutputStream(propertyFile)) {
            fullProps.store(fos, "Generated configuration for " + dbName);
        }
        
        return propertyFile.getAbsolutePath();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
