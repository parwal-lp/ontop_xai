package it.expai.gui;

import it.expai.*;
import it.unibz.inf.ontop.injection.OntopSQLOWLAPIConfiguration;
import it.unibz.inf.ontop.materialization.MaterializationParams;
import it.unibz.inf.ontop.rdf4j.materialization.RDF4JMaterializer;
import it.unibz.inf.ontop.rdf4j.query.MaterializationGraphQuery;
import it.unibz.inf.ontop.rdf4j.repository.OntopRepository;
import it.unibz.inf.ontop.spec.mapping.PrefixManager;
import it.unibz.inf.ontop.spec.mapping.pp.SQLPPMapping;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.ntriples.NTriplesWriter;

import java.io.*;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.function.Consumer;

/**
 * Background worker for running the explanation computation.
 * Runs in a separate thread and provides callbacks for UI updates.
 */
public class ExplanationWorker implements Runnable {
    
    private final String propertyFile;
    private final int radius;
    private final Consumer<String> outputCallback;
    private final Consumer<String> logCallback;
    private final Consumer<Double> progressCallback;
    private final Runnable onComplete;
    private final Runnable onError;
    private final Runnable onCancelled;
    
    private volatile boolean running = false;
    private volatile boolean cancelled = false;
    
    public ExplanationWorker(String propertyFile, int radius,
                            Consumer<String> outputCallback,
                            Consumer<String> logCallback,
                            Consumer<Double> progressCallback,
                            Runnable onComplete,
                            Runnable onError,
                            Runnable onCancelled) {
        this.propertyFile = propertyFile;
        this.radius = radius;
        this.outputCallback = outputCallback;
        this.logCallback = logCallback;
        this.progressCallback = progressCallback;
        this.onComplete = onComplete;
        this.onError = onError;
        this.onCancelled = onCancelled;
    }
    
    public boolean isRunning() {
        return running;
    }
    
    public void cancel() {
        cancelled = true;
    }
    
    @Override
    public void run() {
        running = true;
        
        try {
            runExplanation();
            
            if (cancelled) {
                onCancelled.run();
            } else {
                onComplete.run();
            }
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
        UtilsImpl ui = new UtilsImpl();
        
        // Custom PrintStream that forwards to callbacks
        PrintStream outputStream = new PrintStream(new OutputStream() {
            private StringBuilder buffer = new StringBuilder();
            
            @Override
            public void write(int b) {
                if (b == '\n') {
                    outputCallback.accept(buffer.toString());
                    buffer.setLength(0);
                } else {
                    buffer.append((char) b);
                }
            }
        }, true);
        
        PrintStream logStream = new PrintStream(new OutputStream() {
            private StringBuilder buffer = new StringBuilder();
            
            @Override
            public void write(int b) {
                if (b == '\n') {
                    logCallback.accept(buffer.toString());
                    buffer.setLength(0);
                } else {
                    buffer.append((char) b);
                }
            }
        }, true);
        
        // Redirect System.out temporarily
        PrintStream originalOut = System.out;
        System.setOut(outputStream);
        
        try {
            // Load properties
            outputCallback.accept("========================================================");
            outputCallback.accept("Setup Properties for connection to Database and to Ontop");
            outputCallback.accept("========================================================");
            
            Properties p = new Properties();
            try (FileInputStream propertyFileStream = new FileInputStream(propertyFile)) {
                p.load(propertyFileStream);
            }
            
            String owlFile = p.getProperty("owlFile");
            String mappingFile = p.getProperty("mappingFile");
            String lambdaFile = p.getProperty("lambdaFile");
            String aboxFile = p.getProperty("aboxFile");
            String logFile = p.getProperty("logFile");
            String explFile = p.getProperty("explFile");
            
            if (cancelled) return;
            
            // Connect to Ontop
            outputCallback.accept("\n===========================");
            outputCallback.accept("Connessione a Ontop e MySQL");
            outputCallback.accept("===========================");
            
            OntopSQLOWLAPIConfiguration configuration = OntopSQLOWLAPIConfiguration.defaultBuilder()
                    .ontologyFile(owlFile)
                    .r2rmlMappingFile(mappingFile)
                    .propertyFile(propertyFile)
                    .enableTestMode()
                    .build();
            
            Repository repo = OntopRepository.defaultRepository(configuration);
            repo.init();
            repo.getConnection();
            outputCallback.accept("Connessione avvenuta con successo!");
            
            SQLPPMapping ppMapping = configuration.loadProvidedPPMapping();
            PrefixManager pm = ppMapping.getPrefixManager();
            String prefixList = ui.getPrefix(pm);
            
            if (cancelled) {
                repo.shutDown();
                return;
            }
            
            progressCallback.accept(0.1);
            
            // Retrieve ABox
            outputCallback.accept("\n=================");
            outputCallback.accept("Retrieve the ABox");
            outputCallback.accept("=================");
            
            MaterializationParams materializationParams = MaterializationParams.defaultBuilder().build();
            RDF4JMaterializer materializer = RDF4JMaterializer.defaultMaterializer(configuration, materializationParams);
            MaterializationGraphQuery graphQuery = materializer.materialize();
            
            File abox = Paths.get(aboxFile).toFile();
            if (abox.exists()) {
                outputCallback.accept("File " + abox.getAbsolutePath() + " already exists!");
                outputCallback.accept("(the system will use the existing file, skipping the ABox materialization step)");
            } else {
                outputCallback.accept("File " + abox.getAbsolutePath() + " does not exist!");
                outputCallback.accept("ABox materialization will start.");
                
                try (BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(abox, true)))) {
                    outputCallback.accept("\nComputing ABox...");
                    long start = System.currentTimeMillis();
                    RDFWriter writer = new NTriplesWriter(out);
                    graphQuery.evaluate(writer);
                    
                    long numberOfTriples = graphQuery.getTripleCountSoFar();
                    long end = System.currentTimeMillis();
                    float seconds = ((end - start) / 1000F);
                    outputCallback.accept("Generated Abox with " + numberOfTriples + " triples in " + seconds + " seconds");
                    abox = Paths.get(aboxFile).toFile();
                }
            }
            
            if (cancelled) {
                repo.shutDown();
                return;
            }
            
            progressCallback.accept(0.2);
            
            // Retrieve Lambda
            outputCallback.accept("\n===============");
            outputCallback.accept("Retrieve Lambda");
            outputCallback.accept("===============");
            
            List<List<String>> lambda = ui.getLambda(lambdaFile);
            int lambdaSize = lambda.size();
            outputCallback.accept("Lambda size: " + lambdaSize);
            
            if (cancelled) {
                repo.shutDown();
                return;
            }
            
            progressCallback.accept(0.25);
            
            // Compute Variables
            outputCallback.accept("\n=================");
            outputCallback.accept("Compute Variables");
            outputCallback.accept("=================");
            outputCallback.accept("Computing existential variables...");
            
            long start = System.nanoTime();
            HashMap<String, Integer> existentialVars = ui.existentialVarsMapping(abox);
            long end = System.nanoTime();
            outputCallback.accept("Computed " + existentialVars.size() + " existential variables in " + 
                                 (end - start) / 1_000_000_000.0 + " seconds");
            
            if (cancelled) {
                repo.shutDown();
                return;
            }
            
            progressCallback.accept(0.3);
            
            // Compute Explanation
            outputCallback.accept("\n==================");
            outputCallback.accept("Compute Explanation");
            outputCallback.accept("==================");
            
            long startExpl = System.nanoTime();
            int count = 0;
            
            // Create output files
            PrintStream fileOut = new PrintStream(new FileOutputStream(explFile));
            PrintStream logOut = new PrintStream(new FileOutputStream(logFile));
            
            // Redirect log output to both file and callback
            PrintStream dualLogStream = new PrintStream(new OutputStream() {
                @Override
                public void write(int b) throws IOException {
                    logOut.write(b);
                    logStream.write(b);
                }
            });
            
            StringBuilder head = new StringBuilder("SELECT ");
            for (int i = 0; i < lambda.get(0).size(); i++) {
                String index = String.valueOf(i + 1);
                head.append("?x").append(index).append(" ");
            }
            head.append("\nWHERE {\n");
            
            fileOut.println(head);
            
            for (List<String> tuple : lambda) {
                if (cancelled) {
                    break;
                }
                
                long startTuple;
                count++;
                
                outputCallback.accept("\n============ Processing tuple " + count + "/" + lambdaSize + ": " + tuple + " ============");
                startTuple = System.nanoTime();
                
                start = System.nanoTime();
                List<MembershipAssertion> border = ui.generateBorderN(tuple, abox, radius, dualLogStream);
                end = System.nanoTime();
                outputCallback.accept("Border computed in " + (end - start) / 1_000_000_000.0 + " seconds");
                
                start = System.nanoTime();
                List<MembershipAssertion> disj = ui.replaceConstVar(tuple, border, existentialVars);
                end = System.nanoTime();
                outputCallback.accept("Disjunct computed in " + (end - start) / 1_000_000_000.0 + " seconds");
                
                start = System.nanoTime();
                String query_sparql = ui.sparqlTranslate(disj, prefixList, pm);
                end = System.nanoTime();
                outputCallback.accept("SPARQL Query computed in " + (end - start) / 1_000_000_000.0 + " seconds");
                
                fileOut.println(query_sparql);
                if (count < lambdaSize) {
                    fileOut.println("\nUNION\n");
                }
                
                long endTuple = System.nanoTime();
                outputCallback.accept("Total time for tuple processing [" + 
                                     (endTuple - startTuple) / 1_000_000_000.0 + " seconds]");
                
                // Update progress
                double progress = 0.3 + (0.7 * count / lambdaSize);
                progressCallback.accept(progress);
            }
            
            fileOut.println("\n}");
            fileOut.close();
            logOut.close();
            
            long endExpl = System.nanoTime();
            long totElapsedTime = endExpl - startExpl;
            
            outputCallback.accept("\n============ END COMPUTATION ============");
            if (totElapsedTime > 60_000_000_000L) {
                outputCallback.accept("Explanation computed and printed to file (" + explFile + ").");
                outputCallback.accept("Total time for computing the explanation [" + 
                                     (totElapsedTime / 1_000_000_000.0 / 60.0) + " minutes]");
            } else {
                outputCallback.accept("Explanation computed and printed to file (" + explFile + ").");
                outputCallback.accept("Total time for computing the explanation [" + 
                                     (totElapsedTime / 1_000_000_000.0) + " seconds]");
            }
            
            repo.shutDown();
            outputCallback.accept("\n===================");
            outputCallback.accept("Connessione chiusa.");
            outputCallback.accept("===================");
            
        } finally {
            System.setOut(originalOut);
        }
    }
    
    private String getStackTrace(Exception e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        return sw.toString();
    }
}
