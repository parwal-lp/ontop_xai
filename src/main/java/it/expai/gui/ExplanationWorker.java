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
    private final Consumer<String> explCallback;
    private final Runnable onComplete;
    private final Runnable onError;
    private final Runnable onStopped;
    
    private volatile boolean running = false;
    private ExplainableAIOntop app; // Instance of the main computation class
    
    public ExplanationWorker(String propertyFile, int radius,
                            Consumer<String> outputCallback,
                            Consumer<String> explCallback,
                            Runnable onComplete,
                            Runnable onError,
                            Runnable onStopped) {
        this.propertyFile = propertyFile;
        this.radius = radius;
        this.outputCallback = outputCallback;
        this.explCallback = explCallback;
        this.onComplete = onComplete;
        this.onError = onError;
        this.onStopped = onStopped;
    }
    
    public boolean isRunning() {
        return running;
    }
    
    
    @Override
    public void run() {
        running = true;
        
        try {
            int ret = runExplanation();
            if (ret == 1) onComplete.run();
            else if (ret == -1) onStopped.run();
            else onError.run();
        } catch (Exception e) {
            e.printStackTrace();
            outputCallback.accept("ERROR: " + e.getMessage());
            onError.run();
        } finally {
            running = false;
        }
    }
    
    private int runExplanation() throws Exception {
        // Redirect System.out to capture output for GUI
        PrintStream originalOut = System.out;
        PrintStream originalErr = System.err;

        int ret;
        
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
            ret = app.computeExplanation(propertyFile, radius, explCallback);
        } finally {
            // Restore original System.out
            System.setOut(originalOut);
            System.setErr(originalErr);
        }
        return ret;
    }

    public void stopExplanation() {
        if (app != null) {
            app.stop();
        }
    }
    

}
