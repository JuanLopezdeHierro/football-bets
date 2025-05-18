package com.example.businessunit.controller;

import com.example.businessunit.model.MatchEvent;
import com.example.businessunit.service.MatchDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.Optional;
// import java.time.format.DateTimeFormatter; // Si ya no lo usas para el display de hora

@Controller
@RequestMapping("/laliga")
public class MatchController {

    private final MatchDataService matchDataService;
    // private final DateTimeFormatter timeFormatter; // Comentado si ya no se usa

    @Autowired
    public MatchController(MatchDataService matchDataService /*, DateTimeFormatter timeFormatter */) {
        this.matchDataService = matchDataService;
        // this.timeFormatter = timeFormatter;
    }

    @GetMapping("/matches")
    public String listAllMatchesOverview(Model model) {
        model.addAttribute("liveMatches", matchDataService.getLiveMatches());
        model.addAttribute("upcomingMatches", matchDataService.getUpcomingMatches());
        model.addAttribute("historicalMatches", matchDataService.getHistoricalMatches());
        return "matches_overview";
    }

    @GetMapping("/match/{matchId}")
    public String viewMatchDetails(@PathVariable("matchId") String idDelPartido, Model model) {
        List<MatchEvent> allMatchesForSidebar = matchDataService.getAllMatchEvents();
        Optional<MatchEvent> selectedMatchOpt = matchDataService.getMatchEventById(idDelPartido);

        model.addAttribute("allMatches", allMatchesForSidebar);

        if (selectedMatchOpt.isPresent()) {
            MatchEvent selectedMatch = selectedMatchOpt.get();
            model.addAttribute("selectedMatch", selectedMatch);

            // --- Lógica Corregida para Nombres de Logo ---
            String homeTeamName = selectedMatch.getTeamHome(); // Ej: "Atlético de Madrid"
            String awayTeamName = selectedMatch.getTeamAway();

            // Reemplazar espacios con guiones bajos. Mantiene mayúsculas/minúsculas y tildes.
            String homeLogoFileName = homeTeamName.replace(" ", "_") + ".png"; // Ej: "Atlético_de_Madrid.png"
            String awayLogoFileName = awayTeamName.replace(" ", "_") + ".png";

            model.addAttribute("homeLogo", "/images/logos/" + homeLogoFileName);
            model.addAttribute("awayLogo", "/images/logos/" + awayLogoFileName);
            // --- Fin de Lógica Corregida ---

        } else {
            model.addAttribute("selectedMatch", null);
            model.addAttribute("errorMessage", "Partido no encontrado con ID: " + idDelPartido);
        }
        return "match_details";
    }
}