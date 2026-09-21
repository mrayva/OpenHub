package com.thirtydegreesray.openhub.http.error;

/**
 * GitHub's primary ("API rate limit exceeded...") or secondary ("You have
 * exceeded a secondary rate limit...") rate limit, both surfaced as a plain
 * 403 with a human-readable "message" field in the JSON error body - GitHub
 * gives no other reliable signal for the secondary case: confirmed live by
 * hammering the search endpoint that X-RateLimit-Remaining can still be
 * nonzero (6 of 10 requests unused) when a secondary-limit 403 lands, so
 * that header alone can't be used to detect it - the response body's
 * "message" text is the only reliable signal for both cases.
 *
 * retryAfterSeconds is only ever populated when GitHub's Retry-After header
 * is present, which is not guaranteed (especially for the secondary limit,
 * which in practice omits it) - -1 means "unknown, GitHub just says wait a
 * few minutes" rather than 0, so callers don't mistake it for "retry now".
 */
public class RateLimitError extends Error {

    private final long retryAfterSeconds;

    public RateLimitError(String githubMessage, long retryAfterSeconds) {
        super(githubMessage);
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
