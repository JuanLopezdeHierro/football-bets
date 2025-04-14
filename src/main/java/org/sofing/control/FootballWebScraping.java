package org.sofing.control;

import org.json.JSONObject;
import org.sofing.model.Match;

public interface FootballWebScraping {
    Match betfairScraping();
    JSONObject matchDataToJson(Match match);
}