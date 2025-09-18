package it.expai;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.stream.Collectors;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;

import it.unibz.inf.ontop.spec.mapping.PrefixManager;

public class UtilsImpl implements IUtils {

    @Override
    public List<List<String>> getLambda(String path) throws FileNotFoundException, IOException, CsvValidationException {
        List<List<String>> res = new LinkedList<List<String>>();
		
		// Read csv file and derive tuples of lambda
		
		List<String> tuple = new LinkedList<String>();
		
		try (CSVReader reader = new CSVReader(new FileReader(path))) {
		      String[] lineInArray;
		      while ((lineInArray = reader.readNext()) != null) {
		    	  for(String s : lineInArray) {
		    		  tuple.add(s);
		    	  }
		    	  res.add(tuple);
		    	  tuple = new LinkedList<String>();
		      }
		  }
		
		return res;
    }

	@Override
	public HashMap<String, Integer> existentialVarsMapping(File abox) throws FileNotFoundException{		
		HashMap<String, Integer> res = new HashMap<String, Integer>();
		int y_counter = 1;


		Scanner myReader = new Scanner(abox, "UTF-8");
		while (myReader.hasNextLine()) {

			String row = myReader.nextLine();
			MembershipAssertion assertion = assertionFromTriple(row);
			//System.out.println(assertion);

			if(assertion.getClass().equals(Concept.class)) {
				Concept mac = (Concept)assertion;
				String term = mac.getConceptTerm();		
				if(!res.containsKey(term)) {
						res.put(term, y_counter++);
				}	
			}

			if(assertion.getClass().equals(Role.class)) {
				Role mar = (Role)assertion;
				String term_domain = mar.getDomainTerm();
				String term_range = mar.getRangeTerm();
				if(!res.containsKey(term_domain)) {
						res.put(term_domain, y_counter++);
				}
				if(!res.containsKey(term_range)) {
						res.put(term_range, y_counter++);
				}
			}
		}

		myReader.close();

		return res;
	}


	@Override
    public MembershipAssertion assertionFromTriple(String row) {
		String subject ="", predicate="", object="";
			
		String regex = "(<[^>]+>)\\s+(<[^>]+>)\\s+(\"[^\"]+\"|<[^>]+>|\"[^>]+>)\\s*\\.";
		java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(regex);
		java.util.regex.Matcher matcher = pattern.matcher(row);

		if (matcher.find()) {
			subject = matcher.group(1);
			predicate = matcher.group(2);
			object = matcher.group(3);
		} else {
			System.out.println("Trovata riga con formato non riconosciuto!");
			return null;
		}

		// System.out.println("\n-------------------------------------\n"+row);
		// System.out.println("subject: "+subject);
		// System.out.println("predicate: "+predicate);
		// System.out.println("object: "+object);

		MembershipAssertion assertion;

		if(predicate.endsWith("#type>")){ // è un concetto
			//concept like
			String namespace = object.substring(object.indexOf("<")+1, object.indexOf("#"));
			// System.out.println("namespace: "+namespace);
			String localName = object.substring(object.indexOf("#"), object.indexOf(">"));
			// System.out.println("localname: "+localName);

			String terms = subject.substring(subject.indexOf("<")+1, subject.indexOf(">"));
			assertion = new Concept(namespace, localName, terms);

			// System.out.println("terms "+terms);
		}
		else { // è un ruolo
			//role like
			String namespace = predicate.substring(predicate.indexOf("<")+1, predicate.indexOf("#"));
			// System.out.println("namespace: "+namespace);
			String localName = predicate.substring(predicate.indexOf("#"), predicate.indexOf(">"));
			// System.out.println("localname: "+localName);

			String terms = subject.replace(",", "")+','+object.replace(",", "");
			String domain = terms.split(",")[0];
			String range = terms.split(",")[1];

			//il range può essere un literal oppure un IRI
			if(range.contains("\"")){ //se è un literal tengo solo il suo valore
				range = range.substring(range.indexOf("\"") + 1, range.indexOf("\"", range.indexOf("\"")+1));
			}

			assertion = new Role(namespace, localName, domain, range);
			
			// System.out.println("domain: "+domain);
			// System.out.println("range: "+range);
		}

		return assertion;
	}

	@Override
    public List<MembershipAssertion> generateDisjunct(List<String> tuple, File abox, HashMap<String, Integer> existentialVars) throws FileNotFoundException {
        List<MembershipAssertion> query = new LinkedList<MembershipAssertion>();
		Map<String, Integer> dictionary = new HashMap<String, Integer>();	
		int x_counter = 1, y_counter = 1;
		
		for(String t : tuple) 
			dictionary.put(t, x_counter++);

		//System.out.println("\nTUPLA CORRENTE:");
		//System.out.println(tuple);
		
		Scanner myReader = new Scanner(abox, "UTF-8");
		while (myReader.hasNextLine()) {
			String row = myReader.nextLine();
			MembershipAssertion assertion = assertionFromTriple(row);
			//System.out.println("\nORA GUARDO L'ASSERZIONE "+assertion);
			if(assertion.getClass().equals(Concept.class)) {
				Concept mac = (Concept)assertion;
				String term = mac.getConceptTerm();
				MembershipAssertion temp;
				
				if(tuple.contains(term)) {
					//it's an 'x'
					//yet present in dictionary
					//take value and transform in an 'x'
					temp = new Concept(mac.getNamespace(), mac.getLocalName(), "x"+String.valueOf(dictionary.get(term)));
				}	
				else {
					//it's a 'y'
					//if not seen before, put it in dictionary and increment y counter, otherwise, take its value
					//if(!dictionary.containsKey(term)) {
					//	dictionary.put(term, y_counter++);
					//}		
					//temp = new Concept(mac.getNamespace(), mac.getLocalName(), "y"+String.valueOf(dictionary.get(term)));
					temp = new Concept(mac.getNamespace(), mac.getLocalName(), "y"+String.valueOf(existentialVars.get(term)));
				}
				query.add(temp);
			}
			
			
			if(assertion.getClass().equals(Role.class)) {
				Role mar = (Role)assertion;
				String term_domain = mar.getDomainTerm();
				String term_range = mar.getRangeTerm();
				MembershipAssertion temp;
				
				String new_dom_term = "";
				String new_ran_term = "";
				
				//analize domain term
				//domain term is an 'x'
				if(tuple.contains(term_domain)) {
					new_dom_term = "x"+String.valueOf(dictionary.get(term_domain));
				}
				//term is a 'y'
				else {
					//add it if not present
					if(!dictionary.containsKey(term_domain)) {
						dictionary.put(term_domain, y_counter++);
					}
					//new_dom_term = "y"+String.valueOf(dictionary.get(term_domain));
					new_dom_term = "y"+String.valueOf(existentialVars.get(term_domain));
				}

				//analize range term
				//range term is an 'x'
				if(tuple.contains(term_range)) {
					new_ran_term = "x"+String.valueOf(dictionary.get(term_range));
				}
				//range term is a 'y'
				else {
					//add if not present
					if(!dictionary.containsKey(term_range)) {
						dictionary.put(term_range, y_counter++);
					}
					//new_ran_term = "y"+String.valueOf(dictionary.get(term_range));
					new_ran_term = "y"+String.valueOf(existentialVars.get(term_range));
				}
				
				temp = new Role(mar.getNamespace(), mar.getLocalName(), new_dom_term, new_ran_term);
				query.add(temp);
			}
		}
		myReader.close();
	
		return query;
    }


    @Override
    public String sparqlTranslate(List<MembershipAssertion> facts, String prefixList, PrefixManager pm) {
        List<String> variables = new LinkedList<String>();

		int max_x = 0;		
		System.out.println("sono " + facts.size() + " membership assertions");
		int index = 0;
		for(MembershipAssertion m : facts) {
			System.out.println(index++);
			if(m.getClass().equals(Concept.class)) {
				String term = ((Concept)m).getConceptTerm();
				if(term.substring(0,1).equals("x")) {
					System.out.println("trovato una x in un term di un concetto");
					max_x = Math.max(max_x, Integer.parseInt(term.substring(1)));
				}	
			}
			else {
				if(((Role)m).getDomainTerm().substring(0,1).equals("x")) {
					String dom_term = ((Role)m).getDomainTerm();
					if(dom_term.substring(0,1).equals("x")) {
						System.out.println("trovato una x in un dominio di un ruolo");
						max_x = Math.max(max_x, Integer.parseInt(dom_term.substring(1)));
					}
				}
				if(((Role)m).getRangeTerm().substring(0,1).equals("x")) {
					String ran_term = ((Role)m).getRangeTerm();
					if(ran_term.substring(0,1).equals("x")) {
						System.out.println("trovato una x in un range di un ruolo");
						max_x = Math.max(max_x, Integer.parseInt(ran_term.substring(1)));
					}
				}
			}
		}

		//String x = "";
		System.out.println("aggiungo x alle variabili (inizio for)");
		for(int i=0; i<max_x; i++) {
			String new_x = "x"+String.valueOf(i+1);
			variables.add(new_x);
			
			//x += "?x"+String.valueOf(i+1)+" ";
		}
		System.out.println("finito calcolo variabili (fine for)");

		//String hat = prefixList + "SELECT DISTINCT " + x + "\nWHERE { \n";
		//String hat = "SELECT DISTINCT " + x + "\nWHERE { \n";
		String hat = "{\n";
		String body = "";
		
		System.out.println("inizio di nuovo a scorrere tutte le "+facts.size()+" membership assertions");
		index = 0;
		for(MembershipAssertion atom : facts) {	
			System.out.println(index++);
			String prefix = pm.getShortForm(atom.getNamespace());
			//System.out.println("prefix for "+atom.getNamespace()+" is "+prefix);
			if(atom.getClass().equals(Concept.class)) {
				body += "?"+((Concept)atom).getConceptTerm() + " a "+ prefix + atom.getLocalName()+". \n";
				
				if(!variables.contains(((Concept)atom).getConceptTerm())) {
					System.out.println("nuova variabile per concetto");
					variables.add(((Concept)atom).getConceptTerm());
				}
				
			}
			else {
				body += "?"+((Role)atom).getDomainTerm()+" "+ prefix + atom.getLocalName()+" ?"+((Role)atom).getRangeTerm()+". \n";
				
				if(!variables.contains(((Role)atom).getDomainTerm())) {
					System.out.println("nuova variabile per dominio");
					variables.add(((Role)atom).getDomainTerm());
				}
				if(!variables.contains(((Role)atom).getRangeTerm())) {
					System.out.println("nuova variabile per ruolo");
					variables.add(((Role)atom).getRangeTerm());
				}
			}
		}
		
		//String q = hat+body+"} \n";
		String q = hat+body;
		System.out.println("fine calcolo query SPARQL");

		return q.substring(0, q.length()-2)+"\n}";
    }


    @Override
    public String getPrefix(PrefixManager pm) {
        //extract all prefixes and their namespace and build the PREFIX part of the SPARQL queries

		String prefix = "";
		
		//System.out.println("PREFIXES HERE!!!:");
		//System.out.println(pm.getPrefixMap().toString());

		for (Map.Entry<String, String> entry : pm.getPrefixMap().entrySet()) {
			prefix += "PREFIX " + entry.getKey() + " <" + entry.getValue() +"> \n";
		}

		// for(String p : pm.getPrefixes()){
		// 	prefix += "PREFIX " + p + " <" + pm.getNamespace(p) +"> \n";
		// }

		return prefix;
    }


	public StringBuilder generateSparqlUCQ(Integer n, String prefix_list, List<String> sparqlDisjunctsBodies){

		/*
		 * Nota: la sintassi prevede una singola SELECT e una serie di blocci in UNION all'interno della WHERE
		 * Valutare come modificare coerentemente rispetto all'algoritmo dell'articolo
		 */

		StringBuilder res = new StringBuilder(prefix_list+"\n");

		StringBuilder distinguishedVariables = new StringBuilder("SELECT ");

		for(int i=0; i<n; i++){
			String index = String.valueOf(i+1);
			distinguishedVariables.append("?x"+index+ " ");
		}

		res.append(distinguishedVariables);
		res.append("\nWHERE { \n");

		int size = sparqlDisjunctsBodies.size();

		for (int d = 0; d < size; d++) {
    		res.append(sparqlDisjunctsBodies.get(d)); //questo aggiunge una graffa chiusa di troppo
    		if (d < size-1) {
        		res.append(" \n\nUNION\n\n");
    		}
		}

		return res.append("\n}");

	}


	List<HashMap<Integer, Set<MembershipAssertion>>> generateBorders(List<List<MembershipAssertion>> cqs){

		List<HashMap<Integer, Set<MembershipAssertion>>> borders = new LinkedList<HashMap<Integer, Set<MembershipAssertion>>>();

		List<MembershipAssertion> disjunct = null;
		Iterator<List<MembershipAssertion>> it = cqs.iterator();

		while(it.hasNext()){
			disjunct = it.next();

			// create a new dictionary for all the borders of current disjunct
			HashMap<Integer, Set<MembershipAssertion>> d_borders = computeDisjunctBorders(disjunct);

			borders.add(d_borders);
		}
		return borders;
	}
    


	HashMap<Integer, Set<MembershipAssertion>> computeDisjunctBorders(List<MembershipAssertion> disjunct){
		
    	HashMap<Integer, Set<MembershipAssertion>> dBorders = new HashMap<>();
    	Set<MembershipAssertion> alreadyIncluded = new HashSet<>();
    	Set<String> accumulatedTerms = new HashSet<>();

    	Set<MembershipAssertion> radius0 = disjunct.stream().filter(a -> extractTerms(a).stream().anyMatch(t -> t.startsWith("x"))).collect(Collectors.toSet());

    	dBorders.put(0, radius0);
    	alreadyIncluded.addAll(radius0);
    	accumulatedTerms.addAll(radius0.stream().flatMap(a -> extractTerms(a).stream()).collect(Collectors.toSet()));

		int radius = 1;

		while (true) {
        	Set<MembershipAssertion> currentRadius = disjunct.stream().filter(a -> !alreadyIncluded.contains(a)).filter(a -> extractTerms(a).stream().anyMatch(accumulatedTerms::contains)).collect(Collectors.toSet());

        	if (currentRadius.isEmpty()) {
            	break;
        	}

        	dBorders.put(radius, currentRadius);
        	alreadyIncluded.addAll(currentRadius);
        	accumulatedTerms.addAll(currentRadius.stream().flatMap(a -> extractTerms(a).stream()).collect(Collectors.toSet()));

        radius++;
    	}
		return dBorders;
	}


	private static Set<String> extractTerms(MembershipAssertion a) {
    	Set<String> terms = new HashSet<>();
    	if (a instanceof Concept) {
        	terms.add(((Concept) a).getConceptTerm());
    	} else if (a instanceof Role) {
        	terms.add(((Role) a).getDomainTerm());
        	terms.add(((Role) a).getRangeTerm());
    	}
    	return terms;
	}


	List<List<MembershipAssertion>> computeApproximation(List<HashMap<Integer, Set<MembershipAssertion>>> dictionaries, int radius){
		List<List<MembershipAssertion>> res = new LinkedList<List<MembershipAssertion>>();

		//costruire List<MembershipAssertion> per ogni disgiunto prendendo gli atomi dai valori entro k=r
		//chiamare sparqlTranslate sulla List creata e aggiungere a List<String>
		//una volta fatto per tutti i disgiunti, chiamare generateSparqlUCQ sulla List<String> per generare la query intera

		for(HashMap<Integer, Set<MembershipAssertion>> dBorder : dictionaries){
			List<MembershipAssertion> disjunctAtoms = new LinkedList<MembershipAssertion>();
			
			Set<MembershipAssertion> combined = new LinkedHashSet<>();

			for(int i=0; i<=radius; i++){
				Set<MembershipAssertion> atomsAtRadius = dBorder.get(i);

				if (atomsAtRadius != null) {
                	combined.addAll(atomsAtRadius);
            	}
			}

			for(MembershipAssertion ma : combined){
				disjunctAtoms.add(ma);
			}
			res.add(disjunctAtoms);
		}
		
		return res;
	}


}
