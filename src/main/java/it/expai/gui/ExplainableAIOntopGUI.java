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
    private TextArea outputArea;
    private TextArea logArea;
    private Button startButton;
    private Button stopButton;
    private ProgressBar progressBar;
    private Label statusLabel;
    
    private ExplanationWorker currentWorker;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("KG-XAI");

        // Main layout
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(10));

        // Top section - Configuration
        VBox topSection = createConfigurationSection();
        root.setTop(topSection);

        // Center section - Output
        VBox centerSection = createOutputSection();
        root.setCenter(centerSection);

        // Bottom section - Controls
        HBox bottomSection = createControlSection();
        root.setBottom(bottomSection);

        Scene scene = new Scene(root, 1000, 700);
        primaryStage.setScene(scene);
        primaryStage.show();
        
        // Handle window close
        primaryStage.setOnCloseRequest(event -> {
            if (currentWorker != null && currentWorker.isRunning()) {
                currentWorker.cancel();
            }
            Platform.exit();
        });
    }

    private VBox createConfigurationSection() {
        VBox section = new VBox(10);
        section.setPadding(new Insets(10));
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

        // Console output tab
        Tab outputTab = new Tab("Console Output");
        outputTab.setClosable(false);
        outputArea = new TextArea();
        outputArea.setEditable(false);
        outputArea.setWrapText(true);
        outputArea.setStyle("-fx-font-family: monospace; -fx-font-size: 12px;");
        outputTab.setContent(outputArea);

        // Log tab
        Tab logTab = new Tab("Detailed Log");
        logTab.setClosable(false);
        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setWrapText(true);
        logArea.setStyle("-fx-font-family: monospace; -fx-font-size: 12px;");
        logTab.setContent(logArea);

        tabPane.getTabs().addAll(outputTab, logTab);
        section.getChildren().add(tabPane);

        return section;
    }

    private HBox createControlSection() {
        HBox section = new HBox(10);
        section.setPadding(new Insets(10, 0, 0, 0));
        section.setAlignment(Pos.CENTER_LEFT);

        startButton = new Button("Start Explanation");
        startButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold;");
        startButton.setPrefWidth(150);
        startButton.setOnAction(e -> startExplanation());

        stopButton = new Button("Stop");
        stopButton.setStyle("-fx-background-color: #f44336; -fx-text-fill: white; -fx-font-weight: bold;");
        stopButton.setPrefWidth(100);
        stopButton.setDisable(true);
        stopButton.setOnAction(e -> stopExplanation());

        Button clearButton = new Button("Clear Output");
        clearButton.setPrefWidth(120);
        clearButton.setOnAction(e -> {
            outputArea.clear();
            logArea.clear();
            statusLabel.setText("Ready");
            progressBar.setProgress(0);
        });

        progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(200);

        statusLabel = new Label("Ready");
        statusLabel.setPrefWidth(300);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        section.getChildren().addAll(
            startButton, stopButton, clearButton, spacer, 
            progressBar, statusLabel
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
        outputArea.clear();
        logArea.clear();
        progressBar.setProgress(0);

        // Disable start button, enable stop button
        startButton.setDisable(true);
        stopButton.setDisable(false);
        statusLabel.setText("Running...");

        // Create and start worker
        Consumer<String> outputCallback = message -> 
            Platform.runLater(() -> outputArea.appendText(message + "\n"));
        
        Consumer<String> logCallback = message -> 
            Platform.runLater(() -> logArea.appendText(message + "\n"));
        
        Consumer<Double> progressCallback = progress -> 
            Platform.runLater(() -> progressBar.setProgress(progress));
        
        Runnable onComplete = () -> Platform.runLater(() -> {
            startButton.setDisable(false);
            stopButton.setDisable(true);
            statusLabel.setText("Completed");
            progressBar.setProgress(1.0);
        });
        
        Runnable onError = () -> Platform.runLater(() -> {
            startButton.setDisable(false);
            stopButton.setDisable(true);
            statusLabel.setText("Error occurred");
        });
        
        Runnable onCancelled = () -> Platform.runLater(() -> {
            startButton.setDisable(false);
            stopButton.setDisable(true);
            statusLabel.setText("Cancelled");
        });

        currentWorker = new ExplanationWorker(
            propertyFile, radius, outputCallback, logCallback, 
            progressCallback, onComplete, onError, onCancelled
        );
        
        new Thread(currentWorker).start();
    }

    private void stopExplanation() {
        if (currentWorker != null && currentWorker.isRunning()) {
            currentWorker.cancel();
            outputArea.appendText("\n[Cancellation requested...]\n");
        }
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
