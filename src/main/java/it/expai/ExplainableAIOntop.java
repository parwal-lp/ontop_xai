package it.expai;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;
import java.util.Set;

import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
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

    static int radius = 1;

	public static void main(String[] args)
			throws Exception {

        // ========================================================
        // Setup Properties for connection to Database and to Ontop
        // ========================================================
        
        //String propertyFile = "src/main/resources/example/books/exampleBooks.properties";
        String propertyFile = "src/main/resources/npd/npd.properties";

        Properties p = new Properties();
        FileInputStream fis = new FileInputStream(propertyFile);
        p.load(fis);

        owlFile = p.getProperty("owlFile");
        obdaFile = p.getProperty("obdaFile");
        lambdaFile = p.getProperty("lambdaFile");
        aboxFile = p.getProperty("aboxFile");
        logFile = p.getProperty("logFile");

        fis.close();

        PrintStream fileOut = new PrintStream(new FileOutputStream(logFile));

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
        try (RepositoryConnection conn = repo.getConnection()) {
            System.out.println("Connessione avvenuta con successo!");
        }

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
            System.out.println("Output file " + abox.getAbsolutePath() + " already exists!\n(the system will use the existing file, skipping the ABox materialization step)");
            System.out.println("\nLoading existing ABox from file...");
            long start = System.currentTimeMillis();
			//membershipAssertions = ui.loadABoxFromFile(aboxFile, membershipAssertions, pm);
			long end = System.currentTimeMillis();
			float seconds=((end - start)/1000F);
			System.out.println("ABox loaded in " + seconds + " seconds.");
		} else {
            System.out.println("Output file " + abox.getAbsolutePath() + " does not exist!\nABox materialization will start.");
            try (BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(abox, true)))) {
                System.out.println("\nWriting ABox...");
                RDFWriter writer = new NTriplesWriter(out);
                graphQuery.evaluate(writer);

                long numberOfTriples = graphQuery.getTripleCountSoFar();
                System.out.println("Generated Abox with "+numberOfTriples+" triples.");
                abox = Paths.get(aboxFile).toFile();
		    }
        }
        


        // =========================
        // Retrieve Lambda from file
        // =========================
        List<List<String>> lambda = ui.getLambda(lambdaFile);
		System.out.println("\n===============\nRetrieve Lambda\n===============");
		// for (List<String> tuple : lambda) {
		// 	System.out.println(tuple);
		// }
        int lambdaSize = lambda.size();
        System.out.println("Lambda size: " + lambdaSize);



        // ==================
        // Generate Disjuncts
        // ==================
        System.out.println("\n==================\nGenerate Disjuncts\n==================");
        List<List<MembershipAssertion>> cqs = new LinkedList<List<MembershipAssertion>>();
        System.out.println("Computing existential variables...");
        long start = System.nanoTime();
		HashMap<String, Integer> existentialVars = ui.existentialVarsMapping(abox);
        long end = System.nanoTime();
        System.out.println("Computed " + existentialVars.size() + " existential variables in " + (end - start) / 1_000_000_000.0 + "seconds");
        //System.out.println(existentialVars);
        
        //fileOut.println("\nEXISTENTIAL VARIABLES -------------------------\n" + existentialVars);

        int count = 0;
        System.out.println("\nGenerating disjuncts... (" + lambdaSize +")");
		for (List<String> tuple : lambda) {
            count++;
            start = System.nanoTime();
			List<MembershipAssertion> temp = ui.generateDisjunct(tuple, abox, existentialVars);
            end = System.nanoTime();
            //fileOut.println("\nDISJUNCT FOR TUPLE "+tuple);
            //fileOut.println(temp);
            System.out.println("\nDisjunct ["+count +"/"+lambdaSize+"] (CQ) for tuple " + tuple + " completed");
            System.out.println("Computation time: " + (end - start) / 1_000_000_000.0 + "seconds");
            long heapFreeSize = Runtime.getRuntime().freeMemory();  // Free heap size
            System.out.println("Free Heap Size: " + (heapFreeSize / (1024 * 1024)) + " MB");
			cqs.add(temp);
		}
        System.out.println("\nAll CQs computed. Total number of CQs: " + cqs.size());

        


        List<List<MembershipAssertion>> minComp_disjuncts = new LinkedList<>(cqs); //equivalente alle righe commentate sotto
        // Iterator<List<MembershipAssertion>> it = cqs.iterator();
		// int i = 0;

		// List<List<MembershipAssertion>> minComp_disjuncts = new LinkedList<List<MembershipAssertion>>();

		// while (i++ < cqs.size()) {
		// 	minComp_disjuncts.add(it.next());
		// }

		List<String> minComp_sparql_disjuncts = new LinkedList<String>();
		int disjunctsCount = 1;


        // ===============================
        // Translate Disjuncts into SPARQL
        // ===============================
        System.out.println("\n===============================\nTranslate Disjuncts into SPARQL\n===============================");
		for (List<MembershipAssertion> dis : minComp_disjuncts) {
            //System.out.println("inizio a iterare le asserzioni del disgiunto " +disjunctsCount);
            start = System.nanoTime();
			String sparqlDisjunct = ui.sparqlTranslate(dis, prefixList, pm);
			minComp_sparql_disjuncts.add(sparqlDisjunct);
            end = System.nanoTime();
            //fileOut.println("\nDISGIUNTO "+ disjunctsCount +" IN SPARQL:\n" + sparqlDisjunct);
			System.out.println("Disjunct "+ (disjunctsCount) + " translated in SPARQL in " + (end - start) / 1_000_000_000.0 + "seconds");
            disjunctsCount++;
            
		}



        // ===================
        // Generate UCQ SPARQL
        // ===================
        System.out.println("\n===================\nGenerate UCQ SPARQL\n===================");
        StringBuilder minCompSparqlQuery = ui.generateSparqlUCQ(lambda.get(0).size(), prefixList, minComp_sparql_disjuncts);

		System.out.print("\nUCQ SPARQL query generated (p: print to file, enter: continue): ");
		Scanner inputReader = new Scanner(System.in);
		if(inputReader.nextLine().equals("p")){

            fileOut.println("\n---------------------------UCQ-----------------------------\n" + minCompSparqlQuery);
		}



        // ======================================
        // Compute Approximations through Borders
        // ======================================
        System.out.println("\n======================================\nCompute Approximations through Borders\n======================================");

        List<HashMap<Integer, Set<MembershipAssertion>>> border_dictionary = ui.generateBorders(cqs);
        System.out.println("Generated " + border_dictionary.size() + " borders.");


		boolean cycle = true;
		while (cycle) {
			System.out.print("\nDo you want to try changing the radius? (y/n) ");
			String choice = inputReader.nextLine();

			switch (choice) {
				case "n":
					return;

				case "y": {
					// Ask to choose the radius (incr, decr, exact_value)
					System.out.print("Provide a value for radius: ");

					// Update radius value
					radius = Integer.parseInt(inputReader.nextLine());


					List<List<MembershipAssertion>> aprxucq = ui.computeApproximation(border_dictionary, radius);
					List<String> sparqlAprxDisjuncts = new LinkedList<String>();
					for(List<MembershipAssertion> ad : aprxucq){
						sparqlAprxDisjuncts.add(ui.sparqlTranslate(ad, prefixList, pm));
					}
					StringBuilder sparql_approximated_queries = ui.generateSparqlUCQ(lambda.get(0).size(), prefixList, sparqlAprxDisjuncts);

					System.out.println("\n\nSPARQL query (radius=" + radius + "): \n\n" + sparql_approximated_queries);
                    fileOut.println("\n\nSPARQL query (radius=" + radius + "): \n\n" + sparql_approximated_queries);
					
				}

			}
		}

        inputReader.close();
        fileOut.close();

        // =========================
        // Test SELECT SPARQL query
        // =========================
        /*
        String sparqlFile = "src/main/resources/npd/01.rq";

        String sparqlQuery = Files.readString(Paths.get(sparqlFile));

        try (
                RepositoryConnection conn = repo.getConnection() ;
                TupleQueryResult result = conn.prepareTupleQuery(QueryLanguage.SPARQL, sparqlQuery).evaluate()
        ) {
            System.out.println("Connesso correttamente a Ontop!");
            while (result.hasNext()) {
                BindingSet bindingSet = result.next();
                System.out.println(bindingSet);
            }

            // Only for debugging purpose, not for end users: this will redo the query reformulation, which can be expensive
            //String sqlQuery = ((OntopRepositoryConnection) conn).reformulate(sparqlQuery);
            String sqlQuery = ((OntopRepositoryConnection) conn).reformulateIntoNativeQuery(sparqlQuery);
            System.out.println();
            System.out.println("The reformulated SQL query:");
            System.out.println("=======================");
            System.out.println(sqlQuery);
            System.out.println();
        }
        */
        // ============================
        // Test CONSTRUCT SPARQL query
        // ============================
        /*
        String constructFile = "src/main/resources/npd/c1.rq";

        String sparqlConstructQuery = Files.readString(Paths.get(constructFile));

        System.out.println();
        System.out.println("The input SPARQL construct query:");
        System.out.println("=======================");
        System.out.println(sparqlConstructQuery);
        System.out.println();
        
        try (
                RepositoryConnection conn = repo.getConnection() ;
                GraphQueryResult result = conn.prepareGraphQuery(QueryLanguage.SPARQL, sparqlConstructQuery).evaluate()
        ) {
            while (result.hasNext()) {
                Statement statement = result.next();
                System.out.println(statement);
            }
            result.close();
        }
        */
        
        repo.shutDown();
        System.out.println("\n===================\nConnessione chiusa.\n===================");
		
	}

}