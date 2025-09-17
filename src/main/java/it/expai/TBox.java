package it.expai;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.RepositoryConnection;

import org.eclipse.rdf4j.model.Value;


public class TBox {
    private final RepositoryConnection conn;

    public TBox(RepositoryConnection conn) {
        this.conn = conn;
    }

    public List<String> testQuery() throws Exception{
        String sparql = """
            PREFIX : <http://meraka/moss/exampleBooks.owl#>
            SELECT DISTINCT ?x ?title ?author ?genre ?edition
            WHERE { ?x a :Book; :title ?title; :genre ?genre; :writtenBy ?y; :hasEdition ?z.
                    ?y a :Author; :name ?author.
                    ?z a :Edition; :editionNumber ?edition
            }
        """;
        return executeSPARQL(sparql, "class");
    }

    public List<String> getConcepts() throws Exception {
        String sparql = """
            PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
            PREFIX owl: <http://www.w3.org/2002/07/owl#>
            SELECT DISTINCT ?class WHERE {
                ?class rdf:type owl:Class .
            }
        """;
        return executeSPARQL(sparql, "class");
    }

    public List<String> getObjectProperties() throws Exception {
        String sparql = """
            PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
            PREFIX owl: <http://www.w3.org/2002/07/owl#>
            SELECT DISTINCT ?property WHERE {
                ?property rdf:type owl:ObjectProperty .
            }
        """;
        return executeSPARQL(sparql, "property");
    }

    public List<String> getDataProperties() throws Exception {
        String sparql = """
            PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
            PREFIX owl: <http://www.w3.org/2002/07/owl#>
            SELECT DISTINCT ?property WHERE {
                ?property rdf:type owl:DatatypeProperty .
            }
        """;
        return executeSPARQL(sparql, "property");
    }

    private List<String> executeSPARQL(String sparql, String varName) throws Exception {
        List<String> resultsList = new ArrayList<>();
        TupleQuery query = conn.prepareTupleQuery(sparql);
        try (TupleQueryResult result = query.evaluate()) {
            while (result.hasNext()) {
                Value value = result.next().getValue(varName);
                resultsList.add(value.stringValue());
            }
        }
        return resultsList;
    }
}
