package it.expai.gui;

import it.expai.*;
import java.io.*;
import java.util.function.Consumer;

/**
 * Background worker for running the explanation computation.
 * Runs in a separate thread and provides callbacks for UI updates.
 * This is a thin wrapper that redirects output to the GUI and calls
 * the main computation logic from ExplainableAIOntop.
 */
public class ExplanationWorker implements Runnable {
    
    private final String propertyFile;
    private final int radius;
    private final Consumer<String> outputCallback;
    private final Consumer<String> logCallback;
    private final Runnable onComplete;
    private final Runnable onError;
    private final Runnable onCancelled;
    
    private volatile boolean running = false;
    private ExplainableAIOntop app; // Keep reference to app instance
    
    public ExplanationWorker(String propertyFile, int radius,
                            Consumer<String> outputCallback,
                            Consumer<String> logCallback,
                            Runnable onComplete,
                            Runnable onError,
                            Runnable onCancelled) {
        this.propertyFile = propertyFile;
        this.radius = radius;
        this.outputCallback = outputCallback;
        this.logCallback = logCallback;
        this.onComplete = onComplete;
        this.onError = onError;
        this.onCancelled = onCancelled;
    }
    
    public boolean isRunning() {
        return running;
    }
    
    public void cancel() {
        // Also tell the app instance to stop if it exists
    }
    
    @Override
    public void run() {
        running = true;
        
        try {
            runExplanation();
            
            if (Thread.currentThread().isInterrupted()) {
                onCancelled.run();
            } else {
                onComplete.run();
            }
        } catch (InterruptedException e) {
            // Thread was interrupted - treat as cancellation
            outputCallback.accept("Computation interrupted");
            onCancelled.run();
        } catch (Exception e) {
            e.printStackTrace();
            outputCallback.accept("ERROR: " + e.getMessage());
            logCallback.accept("Exception occurred:\n" + getStackTrace(e));
            onError.run();
        } finally {
            running = false;
        }
    }
    
    private void runExplanation() throws Exception {
        // Redirect System.out to capture output for GUI
        PrintStream originalOut = System.out;
        PrintStream originalErr = System.err;
        
        // Create custom PrintStream that forwards to callbacks
        PrintStream outputStream = new PrintStream(new OutputStream() {
            private StringBuilder buffer = new StringBuilder();
            
            @Override
            public void write(int b) throws IOException {
                if (b == '\n') {
                    outputCallback.accept(buffer.toString());
                    buffer.setLength(0);
                } else {
                    buffer.append((char) b);
                }
            }
        }, true);
        
        // Redirect System.out to our custom stream
        System.setOut(outputStream);
        System.setErr(outputStream);
        
        // Create an ExplainableAIOntop instance and keep reference
        app = new ExplainableAIOntop();
        
        try {
            // Call the computation method
            // The output will be captured by our redirected System.out
            app.computeExplanation(propertyFile, radius);
            
        } catch (InterruptedException e) {
            // Computation was cancelled
            outputCallback.accept("\n>>> Computation cancelled <<<");
            throw e;
        } finally {
            // Restore original System.out
            System.setOut(originalOut);
            System.setErr(originalErr);
            
            // Make sure to stop the app in case it's still running
        }
    }
    
    private String getStackTrace(Exception e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        return sw.toString();
    }
}
