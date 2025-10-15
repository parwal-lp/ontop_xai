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

import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.ntriples.NTriplesWriter;

import it.unibz.inf.ontop.injection.OntopSQLOWLAPIConfiguration;
import it.unibz.inf.ontop.materialization.MaterializationParams;
import it.unibz.inf.ontop.rdf4j.materialization.RDF4JMaterializer;
import it.unibz.inf.ontop.rdf4j.query.MaterializationGraphQuery;
import it.unibz.inf.ontop.rdf4j.repository.OntopRepository;
import it.unibz.inf.ontop.spec.mapping.PrefixManager;
import it.unibz.inf.ontop.spec.mapping.pp.SQLPPMapping;

public class ExplainableAIOntop {

    static UtilsImpl ui = new UtilsImpl();

    static String owlFile;
    static String obdaFile;
    static String lambdaFile;
    static String aboxFile;
    static String logFile;
    static String explFile;

    static int radius = 1;

	public static void main(String[] args)
			throws Exception {

        // ========================================================
        // Setup Properties for connection to Database and to Ontop
        // ========================================================
        
        // String propertyFile = "src/main/resources/example/books/exampleBooks.properties";
        String propertyFile = "src/main/resources/npd/npd.properties";

        Properties p = new Properties();
        FileInputStream propertyFileStream = new FileInputStream(propertyFile);
        p.load(propertyFileStream);

        owlFile = p.getProperty("owlFile");
        obdaFile = p.getProperty("obdaFile");
        lambdaFile = p.getProperty("lambdaFile");
        aboxFile = p.getProperty("aboxFile");
        logFile = p.getProperty("logFile");
        explFile = p.getProperty("explFile");

        propertyFileStream.close();

        PrintStream fileOut = new PrintStream(new FileOutputStream(explFile));
        PrintStream logOut = new PrintStream(new FileOutputStream(logFile));
        long start, end;
        float seconds;

        // ================
        // Connect to Ontop
        // ================
        System.out.println("\n===========================\nConnessione a Ontop e MySQL\n===========================");
        OntopSQLOWLAPIConfiguration configuration = OntopSQLOWLAPIConfiguration.defaultBuilder()
                .ontologyFile(owlFile)
                .nativeOntopMappingFile(obdaFile)
                .propertyFile(propertyFile)
                .enableTestMode()
                .build();

		Repository repo = OntopRepository.defaultRepository(configuration);
        repo.init();
        repo.getConnection();
        System.out.println("Connessione avvenuta con successo!");

        SQLPPMapping ppMapping = configuration.loadProvidedPPMapping();
        PrefixManager pm = ppMapping.getPrefixManager();
        String prefixList = ui.getPrefix(pm);


        //==================
        // Retrieve the ABox
        //==================
        System.out.println("\n=================\nRetrieve the ABox\n=================");
        MaterializationParams materializationParams = MaterializationParams.defaultBuilder().build();
        RDF4JMaterializer materializer = RDF4JMaterializer.defaultMaterializer(configuration, materializationParams);
		MaterializationGraphQuery graphQuery = materializer.materialize();

		File abox = Paths.get(aboxFile).toFile();
		if (abox.exists()) {
            System.out.println("File " + abox.getAbsolutePath() + " already exists!\n(the system will use the existing file, skipping the ABox materialization step)");

            // System.out.println("\nLoading existing ABox from file...");
			// membershipAssertions = ui.loadABoxFromFile(aboxFile, membershipAssertions, pm);
			
		} else {
            System.out.println("File " + abox.getAbsolutePath() + " does not exist!\nABox materialization will start.");
            try (BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(abox, true)))) {
                System.out.println("\nComputing ABox...");
                start = System.currentTimeMillis();
                RDFWriter writer = new NTriplesWriter(out);
                graphQuery.evaluate(writer);

                long numberOfTriples = graphQuery.getTripleCountSoFar();
                end = System.currentTimeMillis();
                seconds=((end - start)/1000F);
                System.out.println("Generated Abox with "+numberOfTriples+" triples in "+seconds);
                abox = Paths.get(aboxFile).toFile();
		    }
        }
        


        // =========================
        // Retrieve Lambda from file
        // =========================
		System.out.println("\n===============\nRetrieve Lambda\n===============");
        List<List<String>> lambda = ui.getLambda(lambdaFile);
		// for (List<String> tuple : lambda) {
		// 	System.out.println(tuple);
		// }
        int lambdaSize = lambda.size();
        System.out.println("Lambda size: " + lambdaSize);



        // =================
        // Compute Variables
        // =================
        System.out.println("\n=================\nCompute Variables\n=================");
        System.out.println("Computing existential variables...");
        start = System.nanoTime();
		HashMap<String, Integer> existentialVars = ui.existentialVarsMapping(abox);
        end = System.nanoTime();
        System.out.println("Computed " + existentialVars.size() + " existential variables in " + (end - start) / 1_000_000_000.0 + " seconds");


        // ==================
        // Compute Exlanation
        // ==================
        System.out.println("\n==================\nCompute Exlanation\n==================");
        int count = 0;
		
        
        StringBuilder head = new StringBuilder("SELECT ");
        for (int i=0; i<lambda.get(0).size(); i++){
            String index = String.valueOf(i+1);
			head.append("?x"+index+ " ");
        }
        head.append("\nWHERE {\n");

        fileOut.println(head);


        long startExpl = System.nanoTime();
        for (List<String> tuple : lambda) {
            count++;

            System.out.println("\n============ Processing tuple "+count +"/"+lambdaSize+": "+tuple+" ============");

            start = System.nanoTime();
			List<MembershipAssertion> border = ui.generateBorderN(tuple, abox, 1, logOut);
            end = System.nanoTime();
            //fileOut.println("\nDISJUNCT FOR TUPLE "+tuple);
            //fileOut.println(temp);
            System.out.println("Border computed in " + (end - start) / 1_000_000_000.0 + " seconds");



            start = System.nanoTime();
			List<MembershipAssertion> disj = ui.replaceConstVar(tuple, border, existentialVars);
            end = System.nanoTime();
            System.out.println("Disjunct computed in " + (end - start) / 1_000_000_000.0 + " seconds");
            

            start = System.nanoTime();
			String query_sparql = ui.sparqlTranslate(disj, prefixList, pm);
            end = System.nanoTime();
            System.out.println("SPARQL Query computed in " + (end - start) / 1_000_000_000.0 + " seconds");
            
            fileOut.println(query_sparql);
            if (count < lambdaSize) {
                fileOut.println("\nUNION\n");
            }
            
		}
        fileOut.println("\n}");

        long endExpl = System.nanoTime();
        System.out.println("\nExplanation computed and printed to file ("+explFile+"). [" + ((endExpl - startExpl) / 1_000_000_000.0 / 60.0) + " minutes]");


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
		
	}

}