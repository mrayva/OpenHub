package com.thirtydegreesray.openhub.mvp.model.graphql;

import com.thirtydegreesray.openhub.util.StringUtils;

import java.util.List;

/**
 * GitHub's GraphQL endpoint almost always answers with HTTP 200 even when
 * the query itself failed (bad permissions, discussions disabled, unknown
 * repo, etc.) - the failure shows up as a non-empty "errors" array in an
 * otherwise-200 body, with "data" left null. Every GraphQL caller must check
 * hasErrors() before trusting data - the REST-oriented HTTP-status error
 * handling elsewhere in this app (HttpPageNoFoundError/UnauthorizedError/etc,
 * see BasePresenter) never sees this kind of failure at all.
 */
public class GraphQLResponse<T> {

    private T data;
    private List<GraphQLError> errors;

    public T getData() {
        return data;
    }

    public boolean hasErrors() {
        return errors != null && !errors.isEmpty();
    }

    public String getErrorMessage() {
        if (!hasErrors()) return null;
        StringBuilder sb = new StringBuilder();
        for (GraphQLError error : errors) {
            if (StringUtils.isBlank(error.getMessage())) continue;
            if (sb.length() > 0) sb.append("\n");
            sb.append(error.getMessage());
        }
        return sb.toString();
    }

    public static class GraphQLError {
        private String message;

        public String getMessage() {
            return message;
        }
    }

}
