package org.hypergraphql;

import com.fasterxml.jackson.databind.JsonNode;
import graphql.*;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by szymon on 01/11/2017.
 */
public class GraphqlService {
    private Config config;
    static Logger logger = Logger.getLogger(GraphqlService.class);

    public GraphqlService(Config config) { this.config = config; }


    public Map<String, Object> results(String query) {

        Map<String, Object> result = new HashMap<>();
        Map<String, Object> data = new HashMap<>();
        Map<Object, Object> extensions = new HashMap<>();
        List<GraphQLError> errors = new ArrayList<>();

        result.put("data", data);
        result.put("errors", errors);
        result.put("extensions", extensions);

        ExecutionInput executionInput;
        ExecutionResult qlResult;

        List<Map<String, String>> sparqlQueries;



        if (!query.contains("IntrospectionQuery") && !query.contains("__")) {

            Converter converter = new Converter(config);

            Map<String, Object> preprocessedQuery = converter.query2json(query);

            System.out.println(((JsonNode) preprocessedQuery.get("query")).toString());

            List<GraphQLError> validationErrors = (List<GraphQLError>) preprocessedQuery.get("errors");
            errors.addAll(validationErrors);

            if (validationErrors.size()>0) {

              //  result.put("errors", errors);

                return result;

            }

            JsonNode jsonQuery = converter.includeContextInQuery((JsonNode) preprocessedQuery.get("query"));

            sparqlQueries = converter.graphql2sparql(jsonQuery);

            // uncomment this lines if you want to include the generated SPARQL queries in the GraphQL response for debugging purposes
            // extensions.put("sparqlQueries", sparqlQueries);

            logger.info("Generated SPARQL queries:");
            logger.info(sparqlQueries.toString());

            SparqlClient client = new SparqlClient(sparqlQueries, config);

            executionInput = ExecutionInput.newExecutionInput()
                    .query(query)
                    .context(client)
                    .build();

            qlResult = config.graphql().execute(executionInput);

            data.putAll(qlResult.getData());
            data.put("@context", preprocessedQuery.get("context"));
        } else {
            qlResult = config.graphql().execute(query);
            data.putAll(qlResult.getData());
        }

        errors.addAll(qlResult.getErrors());

        return result;
    }
}
