package control;


import model.MatchEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@Controller
public class MatchController {

    private final MatchDataService matchDataService;

    @Autowired
    public MatchController(MatchDataService matchDataService) {
        this.matchDataService = matchDataService;
    }

    @GetMapping("/")
    public String listMatches(Model model) {
        List<MatchEvent> matches = matchDataService.getAllMatchEvents();
        model.addAttribute("matches", matches);
        return "index";
    }

    @GetMapping("/match/{id}")
    public String viewMatch(@PathVariable String id, Model model) {
        List<MatchEvent> matches = matchDataService.getAllMatchEvents();
        MatchEvent selectedMatch = matchDataService.getMatchEventById(id).orElse(null);

        model.addAttribute("matches", matches);
        model.addAttribute("selectedMatch", selectedMatch);
        return "match";
    }
}