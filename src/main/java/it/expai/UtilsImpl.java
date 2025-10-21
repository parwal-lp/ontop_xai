package it.expai;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;

import it.unibz.inf.ontop.spec.mapping.PrefixManager;

public class UtilsImpl implements IUtils {

	private static final Pattern TRIPLE_PATTERN = Pattern.compile(
		"(<[^>]+>)\\s+(<[^>]+>)\\s+(\"[^\"]+\"|<[^>]+>|\"[^>]+>)\\s*\\."
	);

    @Override
    public List<List<String>> getLambda(String path) throws FileNotFoundException, IOException, CsvValidationException {
        List<List<String>> res = new LinkedList<List<String>>();
		
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
	public HashMap<String, Integer> existentialVarsMapping(File abox) throws IOException{		
		HashMap<String, Integer> res = new HashMap<String, Integer>();
		int y_counter = 1;

		FileReader fr = new FileReader(abox);
        BufferedReader br = new BufferedReader(fr);
		String row;
		

		while ((row = br.readLine()) != null) {
			
			MembershipAssertion assertion = assertionFromTriple(row);

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

		br.close();
		return res;
	}


	@Override
    public MembershipAssertion assertionFromTriple(String row) {
		String subject ="", predicate="", object="";
			
		//String regex = "(<[^>]+>)\\s+(<[^>]+>)\\s+(\"[^\"]+\"|<[^>]+>|\"[^>]+>)\\s*\\.";
		//java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(regex);
		java.util.regex.Matcher matcher = TRIPLE_PATTERN.matcher(row);

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


		if(predicate.endsWith("#type>")){ // è un concetto
			int hashIndex = object.indexOf('#');
			String namespace = object.substring(1, hashIndex+1);
			String localName = object.substring(hashIndex+1, object.length()-1);
			String term = subject.substring(0, subject.length());
			return new Concept(namespace, localName, term);
		}
		else { // è un ruolo
			int hashIndex = predicate.indexOf('#');
			String namespace = predicate.substring(1, hashIndex+1);
			String localName = predicate.substring(hashIndex+1, predicate.length()-1);

			String[] terms = new String[2];
			String subj = subject.replace(",", "");
			String obj = object.replace(",", "");
			terms[0] = subj;
			terms[1] = obj;

			// se range è literal, prendi solo il valore
			if (terms[1].contains("\"")) {
				int firstQuote = terms[1].indexOf('"');
				int secondQuote = terms[1].indexOf('"', firstQuote + 1);
				terms[1] = terms[1].substring(firstQuote + 1, secondQuote);
			}

			return new Role(namespace, localName, terms[0], terms[1]);
		}
	}



	@Override
    public List<MembershipAssertion> generateBorderN(List<String> tuple, File abox, int radius, PrintStream logOut) throws IOException {
    	Set<String> allFoundTerms = new HashSet<>(tuple);
    	Set<MembershipAssertion> disjunct = new HashSet<>();
		
		// traccia le assertions trovate e aggiunte usando una chiave custom stringa
		// serve perché l'oggetto assertion viene creato appena prima di aggiungerlo a disjunct
		// e quindi controllando l'hash non verrebbe mai trovata dentro il set disjunct, perché ogni volta ha un hash nuovo quello appena creato
		//diverso da quelli inserito prima
		Set<String> addedAssertionKeys = new HashSet<>();



		int currentRadius = 0;
		while (currentRadius <= radius) {
			Set<String> newTerms = new HashSet<>(); //qui vado a mettere quelli nuovi che serviranno al prossimo raggio
			
			FileReader fr = new FileReader(abox);
			BufferedReader br = new BufferedReader(fr);
			String row;

			while ((row = br.readLine()) != null) {
				MembershipAssertion assertion = assertionFromTriple(row);
				
				
				if(assertion instanceof Concept) {
					Concept mac = (Concept)assertion;
					String term = mac.getConceptTerm();

					String assertionKey = "C:" + mac.getConceptName() + ":" + term;


					if(allFoundTerms.contains(term) && !addedAssertionKeys.contains(assertionKey)) {
						disjunct.add(assertion);
						addedAssertionKeys.add(assertionKey);
						newTerms.add(mac.getConceptName());
					}
				}
				else if(assertion instanceof Role) {
					Role mar = (Role)assertion;
					String term_domain = mar.getDomainTerm();
					String term_range = mar.getRangeTerm();

					String assertionKey = "R:" + mar.getNamespace() + mar.getLocalName() + ":" + term_domain + ":" + term_range;
					
					if(allFoundTerms.contains(term_domain) && !addedAssertionKeys.contains(assertionKey)) {
						disjunct.add(assertion);
						addedAssertionKeys.add(assertionKey);
						newTerms.add(term_range);
					}
					else if(allFoundTerms.contains(term_range) && !addedAssertionKeys.contains(assertionKey)) {
						disjunct.add(assertion);
						addedAssertionKeys.add(assertionKey);
						newTerms.add(term_domain);
					}
				}

			}
			br.close();

			logOut.println("\nterms used to generate disjuncts at radius "+currentRadius+": "+newTerms);
			logOut.println("disjunct size at radius "+currentRadius+": "+disjunct.size());

			allFoundTerms.addAll(newTerms);
			currentRadius++;

		}
        return new LinkedList<MembershipAssertion>(disjunct);
    }

	@Override
	public List<MembershipAssertion> replaceConstVar(List<String> tuple, List<MembershipAssertion> border, HashMap<String, Integer> existentialVars) throws IOException{
		List<MembershipAssertion> query = new LinkedList<MembershipAssertion>();
		Map<String, Integer> dictionary = new HashMap<String, Integer>();
		int x_counter = 1, y_counter = 1;

		for(String t : tuple) 
			dictionary.put(t, x_counter++);


		Set<String> tupleSet = new HashSet<String>(tuple);

		for (MembershipAssertion assertion : border){
			if(assertion instanceof Concept) {
				Concept mac = (Concept)assertion;
				String term = mac.getConceptTerm();
				MembershipAssertion temp;
				
				if(tupleSet.contains(term)) {
					//it's an 'x'
					//yet present in dictionary
					//take value and transform in an 'x'
					temp = new Concept(mac.getNamespace(), mac.getLocalName(), "x"+dictionary.get(term));
				}	
				else {
					//it's a 'y'
					//if not seen before, put it in dictionary and increment y counter, otherwise, take its value
					temp = new Concept(mac.getNamespace(), mac.getLocalName(), "y"+existentialVars.get(term));
				}
				query.add(temp);
			}
			
			else if(assertion instanceof Role) {
				Role mar = (Role)assertion;
				String term_domain = mar.getDomainTerm();
				String term_range = mar.getRangeTerm();
				
				String new_dom_term = "";
				String new_ran_term = "";
				
				//analize domain term
				//domain term is an 'x'
				if(tupleSet.contains(term_domain)) {
					new_dom_term = "x"+dictionary.get(term_domain);
				}
				//term is a 'y'
				else {
					//add it if not present
					if(!dictionary.containsKey(term_domain)) {
						dictionary.put(term_domain, y_counter++);
					}
					//new_dom_term = "y"+String.valueOf(dictionary.get(term_domain));
					new_dom_term = "y"+existentialVars.get(term_domain);
				}

				//analize range term
				//range term is an 'x'
				if(tupleSet.contains(term_range)) {
					new_ran_term = "x"+dictionary.get(term_range);
				}
				//range term is a 'y'
				else {
					//add if not present
					if(!dictionary.containsKey(term_range)) {
						dictionary.put(term_range, y_counter++);
					}
					//new_ran_term = "y"+String.valueOf(dictionary.get(term_range));
					new_ran_term = "y"+existentialVars.get(term_range);
				}
				
				MembershipAssertion temp = new Role(mar.getNamespace(), mar.getLocalName(), new_dom_term, new_ran_term);
				query.add(temp);
			}
		}
		return query;
	}


    @Override
    public String sparqlTranslate(List<MembershipAssertion> facts, String prefixList, PrefixManager pm) {
		Set<String> variables = new HashSet<>();

		int max_x = 0;		
		int index = 0;
		for(MembershipAssertion m : facts) {
			//System.out.println(index++);
			if(m.getClass().equals(Concept.class)) {
				String term = ((Concept)m).getConceptTerm();
				if(term.substring(0,1).equals("x")) {
					//System.out.println("trovato una x in un term di un concetto");
					max_x = Math.max(max_x, Integer.parseInt(term.substring(1)));
				}	
			}
			else {
				if(((Role)m).getDomainTerm().substring(0,1).equals("x")) {
					String dom_term = ((Role)m).getDomainTerm();
					if(dom_term.substring(0,1).equals("x")) {
						//System.out.println("trovato una x in un dominio di un ruolo");
						max_x = Math.max(max_x, Integer.parseInt(dom_term.substring(1)));
					}
				}
				if(((Role)m).getRangeTerm().substring(0,1).equals("x")) {
					String ran_term = ((Role)m).getRangeTerm();
					if(ran_term.substring(0,1).equals("x")) {
						//System.out.println("trovato una x in un range di un ruolo");
						max_x = Math.max(max_x, Integer.parseInt(ran_term.substring(1)));
					}
				}
			}
		}

		for(int i=0; i<max_x; i++) {
			String new_x = "x"+String.valueOf(i+1);
			variables.add(new_x);
		}
		String hat = "{\n";
		StringBuilder sb = new StringBuilder();
		sb.append(hat);
		
		index = 0;
		long lastPrintTime = System.currentTimeMillis(); // time of last print
		long interval = 10_000;
		for(MembershipAssertion atom : facts) {	
			long now = System.currentTimeMillis();
			if (now - lastPrintTime >= interval) {
				lastPrintTime = now; // reset last print time
				SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");    
				Date date = new Date(now);
				String pretty_now = sdf.format(date);
				System.out.println(pretty_now + ": iterazione "+ index +" di " + facts.size());
				
			}
			index++;
			
			if(atom instanceof Concept) {
				Concept c = (Concept) atom;
				String term = c.getConceptTerm();

				String namespace = c.getNamespace();  // Ricostruisci il namespace completo
            	String prefix = findPrefixForNamespace(pm, namespace);
				if (prefix.equals("not found")) {
					sb.append("?").append(term)
						.append(" a ").append(c.getConceptName())
						.append(".\n");
				} else {
					sb.append("?").append(term)
						.append(" a ").append(prefix).append(c.getConceptLocalName())
						.append(".\n");
				}
				variables.add(term);				
			}
			else {
				Role r = (Role) atom;
				String domain = r.getDomainTerm();
				String range = r.getRangeTerm();

				String namespace = r.getNamespace();  // Ricostruisci il namespace completo
            	String prefix = findPrefixForNamespace(pm, namespace);
				if (prefix.equals("not found")) {
					sb.append("?").append(domain)
						.append(" <").append(r.getName()).append(">")
						.append(" ?").append(range)
						.append(".\n");
				} else {
					sb.append("?").append(domain)
						.append(" ").append(prefix).append(r.getLocalName())
						.append(" ?").append(range)
						.append(".\n");
				}

				variables.add(domain);
				variables.add(range);
			}
		}
		
		sb.append("}");
		return sb.toString();
    }


    @Override
    public String getPrefix(PrefixManager pm) {
        //extract all prefixes and their namespace and build the PREFIX part of the SPARQL queries

		String prefix = "";

		for (Map.Entry<String, String> entry : pm.getPrefixMap().entrySet()) {
			prefix += "PREFIX " + entry.getKey() + " <" + entry.getValue() +"> \n";
		}

		return prefix;
    }

	// Metodo helper per trovare il prefisso abbreviato dato il namespace completo
	private String findPrefixForNamespace(PrefixManager pm, String namespace) {
		for (Map.Entry<String, String> entry : pm.getPrefixMap().entrySet()) {
			if (entry.getValue().equals(namespace)) {
				return entry.getKey();  // Ritorna la chiave (prefisso abbreviato)
			}
		}
		// Fallback: se non trovato, usa il namespace completo tra <>
		return "not found";
	}


	public StringBuilder generateSparqlUCQ(Integer n, String prefix_list, List<String> sparqlDisjunctsBodies){

		/*
		 * Nota: la sintassi prevede una singola SELECT e una serie di blocchi in UNION all'interno della WHERE
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
    		res.append(sparqlDisjunctsBodies.get(d));
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

	/** 
	 * !!!DEPRECATED!!!
	 * method substitued by the more general and powerful generateBorderN
	 * obtain the same behavior of generateBorder0 by calling generateBorderN passing radius=0
	 */
	// @Override
    // public List<MembershipAssertion> generateBorder0(List<String> tuple, File abox) throws IOException {
    //     List<MembershipAssertion> disjunct = new LinkedList<MembershipAssertion>();
	// 	Set<String> tupleSet = new HashSet<String>(tuple);
	// 	FileReader fr = new FileReader(abox);
    //     BufferedReader br = new BufferedReader(fr);
	// 	String row;
		
	// 	while ((row = br.readLine()) != null) {
			
	// 		MembershipAssertion assertion = assertionFromTriple(row);
			
	// 		if(assertion instanceof Concept) {
	// 			Concept mac = (Concept)assertion;
	// 			String term = mac.getConceptTerm();
	// 			MembershipAssertion temp;
				
	// 			if(tupleSet.contains(term)) {
	// 				temp = new Concept(mac.getNamespace(), mac.getLocalName(), term);
	// 				disjunct.add(temp);
	// 			}
	// 		}
	// 		else if(assertion instanceof Role) {
	// 			Role mar = (Role)assertion;
	// 			String term_domain = mar.getDomainTerm();
	// 			String term_range = mar.getRangeTerm();
			
	// 			MembershipAssertion temp;
				
	// 			if(tupleSet.contains(term_domain) || tupleSet.contains(term_range)) {
	// 				temp = new Role(mar.getNamespace(), mar.getLocalName(), term_domain, term_range);
	// 				disjunct.add(temp);
	// 			}
	// 		}
	// 	}
	// 	br.close();
	// 	return disjunct;
    // }


	/**
	 * !!!DEPRECATED!!!
	 * old method, substituted by generateBorder (this one computes the minimally complete disjunct)
	 */
	// @Override
    // public List<MembershipAssertion> generateDisjunct(List<String> tuple, File abox, HashMap<String, Integer> existentialVars) throws IOException {
    //     List<MembershipAssertion> query = new LinkedList<MembershipAssertion>();
	// 	Map<String, Integer> dictionary = new HashMap<String, Integer>();	
	// 	int x_counter = 1, y_counter = 1;
	// 	//long start, end;
		
	// 	for(String t : tuple) 
	// 		dictionary.put(t, x_counter++);

	// 	//System.out.println("\nTUPLA CORRENTE:");
	// 	//System.out.println(tuple);

	// 	Set<String> tupleSet = new HashSet<String>(tuple);

	// 	//start = System.nanoTime();

	// 	FileReader fr = new FileReader(abox);
    //     BufferedReader br = new BufferedReader(fr);
	// 	String row;
		
	// 	while ((row = br.readLine()) != null) {
			
	// 		MembershipAssertion assertion = assertionFromTriple(row);			
			
	// 		if(assertion instanceof Concept) {
	// 			Concept mac = (Concept)assertion;
	// 			String term = mac.getConceptTerm();
	// 			MembershipAssertion temp;
				
	// 			if(tupleSet.contains(term)) {
	// 				//it's an 'x'
	// 				//yet present in dictionary
	// 				//take value and transform in an 'x'
	// 				temp = new Concept(mac.getNamespace(), mac.getLocalName(), "x"+dictionary.get(term));
	// 			}	
	// 			else {
	// 				//it's a 'y'
	// 				//if not seen before, put it in dictionary and increment y counter, otherwise, take its value
	// 				temp = new Concept(mac.getNamespace(), mac.getLocalName(), "y"+existentialVars.get(term));
	// 			}
	// 			query.add(temp);
	// 		}
			
	// 		else if(assertion instanceof Role) {
	// 			Role mar = (Role)assertion;
	// 			String term_domain = mar.getDomainTerm();
	// 			String term_range = mar.getRangeTerm();
				
	// 			String new_dom_term = "";
	// 			String new_ran_term = "";
				
	// 			//analize domain term
	// 			//domain term is an 'x'
	// 			if(tupleSet.contains(term_domain)) {
	// 				new_dom_term = "x"+dictionary.get(term_domain);
	// 			}
	// 			//term is a 'y'
	// 			else {
	// 				//add it if not present
	// 				if(!dictionary.containsKey(term_domain)) {
	// 					dictionary.put(term_domain, y_counter++);
	// 				}
	// 				//new_dom_term = "y"+String.valueOf(dictionary.get(term_domain));
	// 				new_dom_term = "y"+existentialVars.get(term_domain);
	// 			}

	// 			//analize range term
	// 			//range term is an 'x'
	// 			if(tupleSet.contains(term_range)) {
	// 				new_ran_term = "x"+dictionary.get(term_range);
	// 			}
	// 			//range term is a 'y'
	// 			else {
	// 				//add if not present
	// 				if(!dictionary.containsKey(term_range)) {
	// 					dictionary.put(term_range, y_counter++);
	// 				}
	// 				//new_ran_term = "y"+String.valueOf(dictionary.get(term_range));
	// 				new_ran_term = "y"+existentialVars.get(term_range);
	// 			}
				
	// 			MembershipAssertion temp = new Role(mar.getNamespace(), mar.getLocalName(), new_dom_term, new_ran_term);
	// 			query.add(temp);
	// 		}

	// 	}
	// 	br.close();
	
	// 	return query;
    // }
	
}
