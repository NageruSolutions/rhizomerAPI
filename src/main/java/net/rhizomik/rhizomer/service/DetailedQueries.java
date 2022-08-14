package net.rhizomik.rhizomer.service;

import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.vocabulary.RDFS;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;

/**
 * Created by http://rhizomik.net/~roberto/
 */
@Service
public class DetailedQueries implements Queries {

    @Override
    public Query getQueryClasses() {
        return QueryFactory.create(prefixes +
            "SELECT ?class ?label (COUNT(DISTINCT ?instance) as ?n) \n" +
            "WHERE { \n" +
            "\t { ?instance a ?class . FILTER ( !isBlank(?class) ) " +
            "\t } UNION \n" +
            "\t { ?instance ?p ?o . FILTER(NOT EXISTS {?instance a ?c} ) BIND(rdfs:Resource AS ?class) } \n" +
            "\t OPTIONAL { GRAPH ?g { ?class rdfs:label ?label \n" +
            "\t\t FILTER LANGMATCHES(LANG(?label), \"en\")  } } \n" +
            "\t OPTIONAL { GRAPH ?g { ?class rdfs:label ?label } } \n" +
            "} GROUP BY ?class ?label");
    }

    @Override
    public Query getQueryClassFacets(String classUri, int sampleSize, int classCount, double coverage) {
        ParameterizedSparqlString pQuery = new ParameterizedSparqlString();
        if (sampleSize > 0 && coverage > 0.0) {
            pQuery.setCommandText(prefixes +
                "SELECT ?property ?range (COUNT(?instance) AS ?uses) (COUNT(DISTINCT ?object) AS ?values) " +
                "       (MIN(?isLiteral) as ?allLiteral) (SAMPLE(?labels) AS ?label) (SAMPLE(?rlabels) AS ?rlabel) \n" +
                "WHERE { \n" +
                "\t { { SELECT ?instance WHERE { ?instance a ?class } OFFSET 0 "+"LIMIT "+sampleSize+" } \n" +
                addSamples(classCount, sampleSize, coverage) + " } \n" +
                "\t ?instance ?property ?object \n" +
                "\t OPTIONAL { ?object a ?type }\n" +
                "\t BIND(if(bound(?type), ?type, if(isLiteral(?object), datatype(?object), rdfs:Resource)) AS ?range) \n" +
                "\t BIND(isLiteral(?object) AS ?isLiteral) \n" +
                "\t OPTIONAL { GRAPH ?g { ?property rdfs:label ?labels FILTER LANGMATCHES(LANG(?labels), \"en\") } } \n" +
                "\t OPTIONAL { GRAPH ?g { ?property rdfs:label ?labels } } \n" +
                "\t OPTIONAL { GRAPH ?g { ?range rdfs:label ?rlabels FILTER LANGMATCHES(LANG(?rlabels), \"en\") } } \n" +
                "\t OPTIONAL { GRAPH ?g { ?range rdfs:label ?rlabels } } \n" +
                "} GROUP BY ?property ?range");
        } else {
            pQuery.setCommandText(prefixes +
                "SELECT ?property ?range (COUNT(?instance) AS ?uses) (COUNT(DISTINCT ?object) AS ?values) " +
                "       (MIN(?isLiteral) as ?allLiteral) (SAMPLE(?labels) AS ?label) (SAMPLE(?rlabels) AS ?rlabel) \n" +
                "WHERE { \n" +
                "\t { SELECT ?instance WHERE { ?instance a ?class } " + ((sampleSize>0) ? "LIMIT "+sampleSize : "") + " } \n" +
                "\t ?instance ?property ?object \n" +
                "\t OPTIONAL { ?object a ?type }\n" +
                "\t BIND(if(bound(?type), ?type, if(isLiteral(?object), datatype(?object), rdfs:Resource)) AS ?range) \n" +
                "\t BIND(isLiteral(?object) AS ?isLiteral) \n" +
                "\t OPTIONAL { GRAPH ?g { ?property rdfs:label ?labels FILTER LANGMATCHES(LANG(?labels), \"en\") } } \n" +
                "\t OPTIONAL { GRAPH ?g { ?property rdfs:label ?labels } } \n" +
                "\t OPTIONAL { GRAPH ?g { ?range rdfs:label ?rlabels FILTER LANGMATCHES(LANG(?rlabels), \"en\") } } \n" +
                "\t OPTIONAL { GRAPH ?g { ?range rdfs:label ?rlabels } } \n" +
                "} GROUP BY ?property ?range");
        }
        pQuery.setIri("class", classUri);
        return pQuery.asQuery();
    }

    @Override
    public Query getQueryFacetRangeValues(String classUri, String facetUri, String rangeUri,
                    MultiValueMap<String, String> filters, boolean isLiteral, int limit, int offset, boolean ordered) {
        ParameterizedSparqlString pQuery = new ParameterizedSparqlString();
        pQuery.setCommandText(prefixes +
            "SELECT ?value ?count (GROUP_CONCAT(?langLabel; SEPARATOR = \" || \") AS ?label) \n" +
            "\t WHERE { \n" +
            "\t { SELECT ?value (COUNT(?value) AS ?count) \n" +
            "\t\t WHERE { \n" +
            "\t\t { SELECT DISTINCT ?instance " +
            "\t\t\t WHERE { \n" +
            "\t\t\t\t ?instance a ?class . \n" +
            getFilterPatternsAnd(filters) +
            "\t\t\t } \n" +
            "\t\t } \n" +
            "\t\t ?instance ?property ?resource . \n" +
            "\t\t BIND(?resource AS ?value)\n \n" +
            ( isLiteral ?
                "\t\t FILTER( ISLITERAL(?resource) && DATATYPE(?resource) = <" + rangeUri + "> )\n" :
                !rangeUri.equals(RDFS.Resource.getURI()) ?
                    "\t\t ?resource a <" + rangeUri + "> \n" :
                    "\t\t OPTIONAL { ?resource a ?type } FILTER( (!BOUND(?type) || ?type=rdfs:Resource ) && !ISLITERAL(?resource) ) \n" ) +
            "\t\t } GROUP BY ?value } \n" +
            "\t OPTIONAL { ?value rdfs:label ?l BIND (CONCAT(?l, IF(LANG(?l),\"@\",\"\"), LANG(?l)) AS ?langLabel) } \n" +
            "\t OPTIONAL { GRAPH ?g { ?value rdfs:label ?l } BIND (CONCAT(?l, IF(LANG(?l),\"@\",\"\"), LANG(?l)) AS ?langLabel) } \n" +
            "} GROUP BY ?value ?count");
        pQuery.setIri("class", classUri);
        pQuery.setIri("property", facetUri);
        Query query = pQuery.asQuery();
        if (limit > 0) query.setLimit(limit);
        if (offset > 0) query.setOffset(offset);
        if (ordered) query.addOrderBy("count", -1);
        return query;
    }

    @Override
    public Query getQueryFacetRangeValuesContaining(String classUri, String facetUri, String rangeUri,
        MultiValueMap<String, String> filters, boolean isLiteral, String containing, int top) {
        ParameterizedSparqlString pQuery = new ParameterizedSparqlString();
        pQuery.setCommandText(prefixes +
                "SELECT DISTINCT ?value ?label \n" +
                "WHERE { \n" +
                "\t { SELECT DISTINCT ?instance " +
                "\t\t WHERE { \n" +
                "\t\t\t ?instance a ?class . \n" +
                getFilterPatternsAnd(filters) +
                "\t\t } \n" +
                "\t } \n" +
                "\t ?instance ?property ?resource . \n" +
                "\t OPTIONAL { ?resource rdfs:label ?label \n" +
                "\t\t FILTER LANGMATCHES(LANG(?label), \"en\")  } \n" +
                "\t OPTIONAL { ?resource rdfs:label ?label } \n" +
                ( isLiteral ?
                    "\t FILTER( ISLITERAL(?resource) && DATATYPE(?resource) = <" + rangeUri + "> )\n" :
                    !rangeUri.equals(RDFS.Resource.getURI()) ?
                        "\t ?resource a <" + rangeUri + "> \n" :
                        "\t OPTIONAL { ?resource a ?type } FILTER( (!BOUND(?type) || ?type=rdfs:Resource ) && !ISLITERAL(?resource) ) \n" ) +
                "\t BIND(?resource AS ?value) \n" +
                "\t FILTER( CONTAINS(LCASE(STR(?resource)), LCASE(?containing)) || CONTAINS(LCASE(?label), LCASE(?containing)) ) \n" +
                "}");
        pQuery.setIri("class", classUri);
        pQuery.setIri("property", facetUri);
        pQuery.setLiteral("containing", containing);
        Query query = pQuery.asQuery();
        if (top > 0) query.setLimit(top);
        return query;
    }

    @Override
    public String convertFilterToSparqlPattern(String property, String range, String value) {
        String pattern = "";
        if (property.equalsIgnoreCase("urn:rhz:contains")) {
            pattern += "\t ?instance ?anyProperty ?text . FILTER ( CONTAINS(LCASE(STR(?text)), "
                    + value.toLowerCase() + ") )";
        }
        else {
            String propertyValueVar = Integer.toUnsignedString(property.hashCode() + value.hashCode());
            pattern = "\t ?instance <" + property + "> ?v" + propertyValueVar + " . \n";
            if (!value.equals("null")) {
                pattern +=
                        ( value.startsWith("<") && value.endsWith(">") ?
                                "\t FILTER( ?v" + propertyValueVar + " = " + value + " )\n" :
                                "\t FILTER( STR(?v" + propertyValueVar + ") = " + value +
                                        " && ISLITERAL(?v" + propertyValueVar + ")" +
                                        (range != null ? " && DATATYPE(?v" + propertyValueVar + ") = <" + range + ">" : "") +
                                        " )\n"
                        );
            }
        }
        return pattern;
    }
}
