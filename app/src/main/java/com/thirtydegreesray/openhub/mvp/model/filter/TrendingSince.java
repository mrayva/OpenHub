package com.thirtydegreesray.openhub.mvp.model.filter;

/**
 * Created by ThirtyDegreesRay on 2018/1/5 12:00:14
 */

public enum TrendingSince {
    Daily, Weekly, Monthly,
    // Not used by real (scraped) trending - GitHub's trending page has no yearly
    // timeframe - only by CreatedActivity's search-API-based "created in last N" tabs.
    Yearly
}
