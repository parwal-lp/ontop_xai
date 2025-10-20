package it.expai.gui;

import it.expai.*;
import java.io.*;
import java.util.function.Consumer;

public class ExplanationWorker implements Runnable {
    
    private final String propertyFile;
    private final int radius;
    private final Consumer<String> outputCallback;
    private final Consumer<String> explCallback;
    private final Runnable onComplete;
    private final Runnable onError;
    private final Runnable onStopped;
    
    private volatile boolean running = false;
    private ExplainableAIOntop app;
    
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
        PrintStream originalOut = System.out;
        PrintStream originalErr = System.err;

        int ret;
        
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
        
        
        System.setOut(outputStream);
        System.setErr(outputStream);
        
        app = new ExplainableAIOntop();
        
        try {
            ret = app.computeExplanation(propertyFile, radius, explCallback);
        } finally {
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
