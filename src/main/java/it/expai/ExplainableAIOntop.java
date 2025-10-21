package it.expai;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.function.Consumer;

import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.ntriples.NTriplesWriter;
import org.semanticweb.owlapi.model.OWLOntology;

import it.expai.gui.ExplainableAIOntopGUI;
import it.unibz.inf.ontop.injection.OntopSQLOWLAPIConfiguration;
import it.unibz.inf.ontop.materialization.MaterializationParams;
import it.unibz.inf.ontop.rdf4j.materialization.RDF4JMaterializer;
import it.unibz.inf.ontop.rdf4j.query.MaterializationGraphQuery;
import it.unibz.inf.ontop.rdf4j.repository.OntopRepository;
import it.unibz.inf.ontop.spec.mapping.PrefixManager;
import it.unibz.inf.ontop.spec.mapping.pp.SQLPPMapping;

public class ExplainableAIOntop {
    
    private String owlFile;
    private String mappingFile;
    private String aboxFile;
    private OWLOntology tbox;
    private String logFile;
    private String explFile;
    private boolean stopFlag = false;

    public void stop() {
        System.out.println("\nStopping the computation...\n");
        stopFlag = true;
    }

    public void configure() {
        // ========================================================
        // Setup Properties for connection to Database and to Ontop
        // ========================================================
    }

    public int computeExplanation(String propertyFile, String lambdaFile, int radius, Consumer<String> explCallback) throws Exception {
        // ========================================================
        // Setup Properties for connection to Database and to Ontop
        // ========================================================

        UtilsImpl ui = new UtilsImpl();

        Properties p = new Properties();
        FileInputStream propertyFileStream = new FileInputStream(propertyFile);
        p.load(propertyFileStream);

        owlFile = p.getProperty("owlFile");
        mappingFile = p.getProperty("mappingFile");
        aboxFile = p.getProperty("aboxFile");
        logFile = p.getProperty("logFile");
        explFile = p.getProperty("explFile");

        propertyFileStream.close();

        // Create directories for output files if they don't exist
        File explFileObj = new File(explFile);
        if (explFileObj.getParentFile() != null && !explFileObj.getParentFile().exists()) {
            explFileObj.getParentFile().mkdirs();
            System.out.println("Created directory: " + explFileObj.getParentFile().getAbsolutePath());
        }
        
        File logFileObj = new File(logFile);
        if (logFileObj.getParentFile() != null && !logFileObj.getParentFile().exists()) {
            logFileObj.getParentFile().mkdirs();
            System.out.println("Created directory: " + logFileObj.getParentFile().getAbsolutePath());
        }
        
        File aboxFileObj = new File(aboxFile);
        if (aboxFileObj.getParentFile() != null && !aboxFileObj.getParentFile().exists()) {
            aboxFileObj.getParentFile().mkdirs();
            System.out.println("Created directory: " + aboxFileObj.getParentFile().getAbsolutePath());
        }

        PrintStream fileOut = new PrintStream(new FileOutputStream(explFile)) {
            @Override
            public void println(String x) {
                super.println(x);
                if (explCallback != null) {
                    explCallback.accept(x + "\n");
                }
            }
        };


        PrintStream logOut = new PrintStream(new FileOutputStream(logFile));
        long start, end;
        float seconds;


        // ================
        // Connect to Ontop
        // ================
        if (stopFlag) return -1;
        System.out.println("\n===========================\nConnessione a Ontop e MySQL\n===========================");
        OntopSQLOWLAPIConfiguration configuration = OntopSQLOWLAPIConfiguration.defaultBuilder()
                .ontologyFile(owlFile)
                .r2rmlMappingFile(mappingFile)
                .propertyFile(propertyFile)
                .enableTestMode()
                .build();

        tbox = configuration.loadInputOntology().orElseThrow(
            () -> new RuntimeException("Failed to load ontology from: " + owlFile)
        );

		Repository repo = OntopRepository.defaultRepository(configuration);
        repo.init();
        repo.getConnection();
        if (!stopFlag) System.out.println("Connessione avvenuta con successo!");

        SQLPPMapping ppMapping = configuration.loadProvidedPPMapping();
        PrefixManager pm = ppMapping.getPrefixManager();
        String prefixList = ui.getPrefix(pm);




        //==================
        // Retrieve the ABox
        //==================
        if (stopFlag) return -1;
        System.out.println("\n=================\nRetrieve the ABox\n=================");
        MaterializationParams materializationParams = MaterializationParams.defaultBuilder().build();
        RDF4JMaterializer materializer = RDF4JMaterializer.defaultMaterializer(configuration, materializationParams);
		MaterializationGraphQuery graphQuery = materializer.materialize();

		File abox = Paths.get(aboxFile).toFile();
		if (abox.exists()) {
            if (!stopFlag) System.out.println("File " + abox.getAbsolutePath() + " already exists!\n(the system will use the existing file, skipping the ABox materialization step)");

            // System.out.println("\nLoading existing ABox from file...");
			// membershipAssertions = ui.loadABoxFromFile(aboxFile, membershipAssertions, pm);
			
		} else {
            if (!stopFlag) System.out.println("File " + abox.getAbsolutePath() + " does not exist!\nABox materialization will start.");
            try (BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(abox, true)))) {
                if (!stopFlag) System.out.println("\nComputing ABox...");
                start = System.currentTimeMillis();
                RDFWriter writer = new NTriplesWriter(out);
                graphQuery.evaluate(writer);

                long numberOfTriples = graphQuery.getTripleCountSoFar();
                end = System.currentTimeMillis();
                seconds=((end - start)/1000F);
                if (!stopFlag) System.out.println("Generated Abox with "+numberOfTriples+" triples in "+seconds);
                abox = Paths.get(aboxFile).toFile();
		    }
        }


        // =========================
        // Retrieve Lambda from file
        // =========================
        if (stopFlag) return -1;
		System.out.println("\n===============\nRetrieve Lambda\n===============");
        List<List<String>> lambda = ui.getLambda(lambdaFile);
		// for (List<String> tuple : lambda) {
		// 	System.out.println(tuple);
		// }
        int lambdaSize = lambda.size();
        if (!stopFlag) System.out.println("Lambda size: " + lambdaSize);


        // =================
        // Compute Variables
        // =================
        if (stopFlag) return -1;
        System.out.println("\n=================\nCompute Variables\n=================");
        if (!stopFlag) System.out.println("Computing existential variables...");
        start = System.nanoTime();
		HashMap<String, Integer> existentialVars = ui.existentialVarsMapping(abox);
        end = System.nanoTime();
        if (!stopFlag) System.out.println("Computed " + existentialVars.size() + " existential variables in " + (end - start) / 1_000_000_000.0 + " seconds");

        // ==================
        // Compute Exlanation
        // ==================
        if (stopFlag) return -1;
        System.out.println("\n===================\nCompute Explanation\n===================");
        long startExpl = System.nanoTime();
        int count = 0;
		
        
        StringBuilder head = new StringBuilder(prefixList+"\n");

        head.append("SELECT ");
        for (int i=0; i<lambda.get(0).size(); i++){
            String index = String.valueOf(i+1);
			head.append("?x"+index+ " ");
        }
        head.append("\nWHERE {\n");

        fileOut.println(head.toString());

        List<MembershipAssertion> border;
        List<MembershipAssertion> refinedBorder;

        for (List<String> tuple : lambda) {
            if (stopFlag) return -1;
            long startTuple, endTuple;
            count++;

            System.out.println("\n============ Processing tuple "+count +"/"+lambdaSize+": "+tuple+" ============");
            startTuple = System.nanoTime();

            start = System.nanoTime();
			border = ui.generateBorderN(tuple, abox, radius, logOut);
            end = System.nanoTime();
            //fileOut.println("\nDISJUNCT FOR TUPLE "+tuple);
            //fileOut.println(temp);
            if (!stopFlag) System.out.println("Border computed in " + (end - start) / 1_000_000_000.0 + " seconds");

            refinedBorder = ui.refineBorder(border, abox, tbox, logOut);

            start = System.nanoTime();
			List<MembershipAssertion> disj = ui.replaceConstVar(tuple, refinedBorder, existentialVars);
            end = System.nanoTime();
            if (!stopFlag) System.out.println("Disjunct computed in " + (end - start) / 1_000_000_000.0 + " seconds");
            

            start = System.nanoTime();
			String query_sparql = ui.sparqlTranslate(disj, prefixList, pm);
            end = System.nanoTime();
            if (!stopFlag) System.out.println("SPARQL Query computed in " + (end - start) / 1_000_000_000.0 + " seconds");
            
            fileOut.println(query_sparql);
            if (count < lambdaSize) {
                fileOut.println("\nUNION\n");
            }
            endTuple = System.nanoTime();
            if (!stopFlag) System.out.println("Total time for tuple processing [" + (endTuple - startTuple) / 1_000_000_000.0 + " seconds]");
		}
        fileOut.println("\n}");

        long endExpl = System.nanoTime();
        long totElapsedTime = endExpl - startExpl;
        System.out.println("\n============ END COMPUTATION ============");
        if (totElapsedTime > 60_000_000_000L)
            if (!stopFlag) System.out.println("Explanation computed and printed to file ("+explFile+").\nTotal time for computing the explanation [" + (totElapsedTime / 1_000_000_000.0 / 60.0) + " minutes]");
        else
            if (!stopFlag) System.out.println("Explanation computed and printed to file ("+explFile+").\nTotal time for computing the explanation [" + (totElapsedTime / 1_000_000_000.0) + " seconds]");

        // =======================
        // Compute Certain Answers
        // =======================
        // System.out.println("\n=======================\nCompute Certain Answers\n=======================");

        // String sparqlQuery = Files.readString(Paths.get("src/main/resources/npd/query.txt"));
        // TupleQueryResult result = conn.prepareTupleQuery(QueryLanguage.SPARQL, sparqlQuery).evaluate();
        // while (result.hasNext()) {
        //     BindingSet bindingSet = result.next();
        //     System.out.println(bindingSet);
        // }
            

        fileOut.close();
        logOut.close();

        
        repo.shutDown();
        System.out.println("\n===================\nConnessione chiusa.\n===================");
		return 0;

    }

	public static void main(String[] args) throws Exception {

        // ExplainableAIOntop kg_xai = new ExplainableAIOntop();

        // kg_xai.computeExplanation(
        //     "src/main/resources/npd/npd.properties",
        //     "src/main/resources/npd/test.csv",
        //     1,
        //     null
        // );

        ExplainableAIOntopGUI.launch(ExplainableAIOntopGUI.class, args);

	}

}