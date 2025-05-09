package org.sofing.control;

import org.json.JSONArray;
import org.sofing.model.Match;

public interface FootballWebScraping {
    Match betfairScraping();
    JSONArray matchDataToJson(Match match);
}