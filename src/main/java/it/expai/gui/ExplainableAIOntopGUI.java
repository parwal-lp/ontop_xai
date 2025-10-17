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

    private TextField propertyFileField;
    private TextField radiusField;
    private TextArea detailsArea;
    private TextArea explanationArea;
    private Button startButton;
    private Button stopButton;
    private Label statusLabel;
    
    private ExplanationWorker currentWorker;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("KG-XAI tool");

        // Main layout
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(10));

        // Top section - Configuration
        VBox topSection = createConfigurationSection();
        root.setTop(topSection);

        // Status section
        HBox statusSection = new HBox();
        statusSection.setPadding(new Insets(10, 10, 5, 10));
        statusLabel = new Label("Ready");
        statusLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold;");
        statusSection.getChildren().add(statusLabel);

        // Center section - Output
        VBox centerSection = createOutputSection();
        
        // Combine status and center in a VBox
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
        
        // Handle window close
        primaryStage.setOnCloseRequest(event -> {
            Platform.exit();
        });
    }

    private VBox createConfigurationSection() {
        VBox section = new VBox(10);
        section.setPadding(new Insets(10, 10, 10, 10));
        section.setStyle("-fx-border-color: #cccccc; -fx-border-width: 1; -fx-border-radius: 5;");

        Label titleLabel = new Label("Configuration");
        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        // Property file selection
        HBox propertyFileBox = new HBox(10);
        propertyFileBox.setAlignment(Pos.CENTER_LEFT);
        Label propertyLabel = new Label("Property File:");
        propertyLabel.setPrefWidth(120);
        propertyFileField = new TextField("src/main/resources/npd/npd.properties");
        propertyFileField.setPrefWidth(500);
        Button browseButton = new Button("Browse...");
        browseButton.setOnAction(e -> browsePropertyFile());
        propertyFileBox.getChildren().addAll(propertyLabel, propertyFileField, browseButton);

        // Radius selection
        HBox radiusBox = new HBox(10);
        radiusBox.setAlignment(Pos.CENTER_LEFT);
        Label radiusLabel = new Label("Radius:");
        radiusLabel.setPrefWidth(120);
        radiusField = new TextField("1");
        radiusField.setPrefWidth(100);
        radiusBox.getChildren().addAll(radiusLabel, radiusField);

        section.getChildren().addAll(titleLabel, propertyFileBox, radiusBox);
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
        //stopButton.setOnAction(e -> stopExplanation());

        Button clearButton = new Button("Clear Output");
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

    private void browsePropertyFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Property File");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Properties Files", "*.properties")
        );
        
        File currentFile = new File(propertyFileField.getText());
        if (currentFile.getParentFile() != null && currentFile.getParentFile().exists()) {
            fileChooser.setInitialDirectory(currentFile.getParentFile());
        }
        
        File selectedFile = fileChooser.showOpenDialog(propertyFileField.getScene().getWindow());
        if (selectedFile != null) {
            propertyFileField.setText(selectedFile.getAbsolutePath());
        }
    }

    private void startExplanation() {
        // Validate inputs
        String propertyFile = propertyFileField.getText().trim();
        if (propertyFile.isEmpty()) {
            showAlert("Error", "Please select a property file");
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

        // Clear output
        detailsArea.clear();
        explanationArea.clear();

        // Disable start button, enable stop button
        startButton.setDisable(true);
        stopButton.setDisable(false);
        statusLabel.setText("Running...");

        // Create and start worker
        Consumer<String> outputCallback = message -> 
            Platform.runLater(() -> detailsArea.appendText(message + "\n"));
        
        Consumer<String> logCallback = message -> 
            Platform.runLater(() -> explanationArea.appendText(message + "\n"));
        
        
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

        currentWorker = new ExplanationWorker(
            propertyFile, radius, outputCallback, logCallback, 
            onComplete, onError
        );
        
        new Thread(currentWorker).start();
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
