package com.thirtydegreesray.openhub.mvp.model.request;

import java.util.Map;

/**
 * Body shape GitHub's GraphQL endpoint (POST /graphql) expects, regardless of
 * which query/mutation is being sent.
 */
public class GraphQLRequest {

    private String query;
    private Map<String, Object> variables;

    public GraphQLRequest(String query, Map<String, Object> variables) {
        this.query = query;
        this.variables = variables;
    }

    public String getQuery() {
        return query;
    }

    public Map<String, Object> getVariables() {
        return variables;
    }

}
