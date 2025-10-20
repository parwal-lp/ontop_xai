package it.expai;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import com.opencsv.exceptions.CsvValidationException;

import it.unibz.inf.ontop.spec.mapping.PrefixManager;

public interface IUtils {

	/**
	 * Reads and parses a CSV file representing the set lambda
	 * 
	 * @param path The path of the csv file containing the tuples
	 * @return The set of positive examples lambda expressed as a list of tuples
	 *         (each being represented as a list of String)
	 * @throws IOException
	 * @throws FileNotFoundException
	 * @throws CsvValidationException
	 */
	List<List<String>> getLambda(String path) throws FileNotFoundException, IOException, CsvValidationException;


    /**
	 * 
	 * @param membershipAssertions set of membership assertions containing constants
	 * @return dictionary with mapping from each constant to the corresponding existential variable 
     * @throws FileNotFoundException 
     * @throws IOException 
	 */
	HashMap<String, Integer> existentialVarsMapping(File membershipAssertions) throws FileNotFoundException, IOException;


    /**
	 * !!!DEPRECATED!!!
	 * Operates a variable substitution (xs and ys) over the atoms composing a
	 * single disjunct
	 * 
	 * @param tuple      List of strings representing the current positive example
	 *                   (tuple) from lambda
	 * @param assertions The materialised ABox, i.e., all the membership assertions
	 *                   constituting the chase
	 * @return The disjunct whose size is the same of the chase but with xs and ys
	 *         substituted to the original values
     * @throws FileNotFoundException 
     * @throws IOException 
	 */
	// List<MembershipAssertion> generateDisjunct(List<String> tuple, File abox, HashMap<String, Integer> existentialVars) throws FileNotFoundException, IOException;


    /**
     * Parses a triple in N-Triples format and returns the corresponding membership assertion
     *
     * @param row the triple in N-Triples format as a String
     * 
     * @return the corresponding membership assertion (Concept or Role)
     */
    MembershipAssertion assertionFromTriple(String row);


	/**
	 * Generates a SPARQL query whose triples are obtained through the collection of
	 * atoms provided as input
	 * 
	 * @param facts  Collection of MembershipAssertion (atoms) to be converted into
	 *               triples
	 * @param prefix Ontology URI
	 * @return A SPARQL query as a String object
	 * @throws PrefixManagerException
	 */
	String sparqlTranslate(List<MembershipAssertion> facts, String prefix, PrefixManager pm);


	/** Retrieves the string corresponding to the ontology prefix 
	 * @throws PrefixManagerException*/
	String getPrefix(PrefixManager pm);


	/**
	 * @param prefix_list String containing the list of ontology prefixes
	 * @param sparqlDisjunctBodies List of String, each being the set of atoms constituting the body of a disjunct
	 * 
	 * @return A SPARQL UCQ
	 */
	StringBuilder generateSparqlUCQ(Integer n, String prefix_list, List<String> sparqlDisjunctsBodies);


	/**
	 * !!!DEPRECTAED!!!
	 * Generates the border with radius 0 of a given tuple from lambda
	 * @param tuple tuple from lambda
	 * @param abox abox file materialization
	 * @return the border with radius 0 of the given tuple
	 * @throws IOException 
	 */
	// List<MembershipAssertion> generateBorder0(List<String> tuple, File abox) throws IOException;

	
	/**
	 * Generates the border with radius n of a given tuple from lambda
	 * @param tuple tuple from lambda
	 * @param abox abox file materialization
	 * @param radius radius n
	 * @param logOut print stream del file utilizzato per stampare i log per scopi di debug
	 * @param borders lista di dizionari che mappano ogni raggio con il corrispondente elenco di assertions (il suo bordo)
	 * @return the border with radius n of the given tuple
	 * @throws IOException 
	 */
    Border generateBorderN(List<String> tuple, File abox, int radius, PrintStream logOut, List<Border> borders) throws IOException;


	/**
	 * Generates the CQ disjunct with variables stating from the border with constants
	 * @param tuple tuple from lambda
	 * @param border border with constants
	 * @return the CQ disjunct with variables corresponding to the given border
	 * @throws IOException
	 */
	List<MembershipAssertion> replaceConstVar(List<String> tuple, Set<MembershipAssertion> border, HashMap<String, Integer> existentialVars) throws IOException;



}