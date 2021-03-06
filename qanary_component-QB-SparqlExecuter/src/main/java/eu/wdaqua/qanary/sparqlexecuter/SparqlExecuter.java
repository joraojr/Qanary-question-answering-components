package eu.wdaqua.qanary.sparqlexecuter;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import eu.wdaqua.qanary.commons.QanaryMessage;
import eu.wdaqua.qanary.commons.QanaryQuestion;
import eu.wdaqua.qanary.commons.QanaryUtils;
import eu.wdaqua.qanary.component.QanaryComponent;


@Component
/**
 * This component connected automatically to the Qanary pipeline.
 * The Qanary pipeline endpoint defined in application.properties (spring.boot.admin.url)
 * @see <a href="https://github.com/WDAqua/Qanary/wiki/How-do-I-integrate-a-new-component-in-Qanary%3F" target="_top">Github wiki howto</a>
 */
public class SparqlExecuter extends QanaryComponent {
	private static final Logger logger = LoggerFactory.getLogger(SparqlExecuter.class);

	/**
	 * implement this method encapsulating the functionality of your Qanary
	 * component
	 * @throws Exception 
	 */
	@Override
	public QanaryMessage process(QanaryMessage myQanaryMessage) throws Exception {
		logger.info("process: {}", myQanaryMessage);
		QanaryUtils myQanaryUtils = this.getUtils(myQanaryMessage);
	      QanaryQuestion<String> myQanaryQuestion = new QanaryQuestion(myQanaryMessage);
	      String myQuestion = myQanaryQuestion.getTextualRepresentation();
	      URI myQuestionUri = myQanaryQuestion.getUri();
		// TODO: implement processing of question
		String sparql="PREFIX qa: <http://www.wdaqua.eu/qa#> " //
    			+ "PREFIX oa: <http://www.w3.org/ns/openannotation/core/> " //
  			+ "SELECT ?sparql " //
  			+ "FROM <"+ myQanaryMessage.getInGraph() + "> " //
  			+ "WHERE { " //
  			+ "  ?a a qa:AnnotationOfAnswerSPARQL . " //
  			+ "  OPTIONAL {?a oa:hasBody ?sparql } " //
  			+ "  ?a qa:hasScore ?score . " //
            + "  ?a oa:annotatedAt ?time . " // 
            + "  { " //
            + "    SELECT ?time { " //
            + "     ?a a qa:AnnotationOfAnswerSPARQL . " //
            + "     ?a oa:annotatedAt ?time . " //
  	        + "    } ORDER BY DESC(?time) limit 2 " //
            + "  } " //
  			+ "} "
  			+ "ORDER BY DESC(?score) LIMIT 1"  ;
		ResultSet resultset = myQanaryUtils.selectFromTripleStore(sparql);
                String sparqlQuery="";
                while (resultset.hasNext()) {
			sparqlQuery = resultset.next().get("sparql").toString();	
		}
		logger.info("Generated SPARQL query: {} ", sparqlQuery);
		// STEP 2: execute the first sparql query
		
        String endpoint = "";
        if (sparqlQuery.contains("http://dbpedia.org")){
        	endpoint = "http://dbpedia.org/sparql";
            logger.info("use DBpedia endpoint");
        } else {
        	endpoint = "https://query.wikidata.org/sparql";
            logger.info("use Wikidata endpoint");
        }
        // @TODO: extend functionality to use qa:TargetDataset if present
        
		Query query = QueryFactory.create(sparqlQuery);
        	String json;
        	if (query.isAskType()){
                	Boolean result = myQanaryUtils.askTripleStore(sparqlQuery, endpoint);
                	ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                	ResultSetFormatter.outputAsJSON(outputStream, result);
                	json = new String(outputStream.toByteArray(), "UTF-8");
        	} else {
               		ResultSet result = myQanaryUtils.selectFromTripleStore(sparqlQuery, endpoint);
                	ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                	ResultSetFormatter.outputAsJSON(outputStream, result);
                	json = new String(outputStream.toByteArray(), "UTF-8");
        	}
        	logger.info("Generated answers in RDF json: {}", json);

        // STEP 3: Push the the json object to the named graph reserved for the question
        logger.info("Push the the JSON object to the named graph reserved for the answer");
		sparql = "PREFIX qa: <http://www.wdaqua.eu/qa#> " // 
                	+ "PREFIX oa: <http://www.w3.org/ns/openannotation/core/> " // 
                	+ "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> " // 
                	+ "INSERT { " // 
                	+ "GRAPH <" + myQanaryUtils.getOutGraph() + "> { " // 
                	+ "  ?b a qa:AnnotationOfAnswerJSON ; " // 
                	+ "     oa:hasTarget <"+myQuestionUri.toString()+"> ; " //   
                	+ "     oa:hasBody \"" + json.replace("\n", " ").replace("\"", "\\\"") + "\" ; " // 
                	+ "     oa:annotatedBy <urn:qanary:QE#qanary.sparqlexecuter> ; " // 
                	+ "     oa:annotatedAt ?time  " // 
                	+ "}} " // 
                	+ "WHERE { " // 
                	+ "  BIND (IRI(str(RAND())) AS ?b) ." // 
                	+ "  BIND (now() as ?time) " // 
                	+ "}";
        myQanaryUtils.updateTripleStore(sparql, myQanaryMessage.getEndpoint().toString());


		return myQanaryMessage;
	}

}
