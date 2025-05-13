package com.example.businessunit.controller;


import com.example.businessunit.model.MatchEvent;
import com.example.businessunit.service.MatchDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/laliga")
public class MatchController {

    private final MatchDataService matchDataService;
    // private final DateTimeFormatter timeFormatter; // Eliminado si no se usa

    @Autowired
    public MatchController(MatchDataService matchDataService /*, DateTimeFormatter timeFormatter */) {
        this.matchDataService = matchDataService;
        // this.timeFormatter = timeFormatter; // Eliminado
    }

    @GetMapping("/matches")
    public String listAllMatchesOverview(Model model) { // Renombrado para claridad
        model.addAttribute("liveMatches", matchDataService.getLiveMatches());
        model.addAttribute("upcomingMatches", matchDataService.getUpcomingMatches());
        model.addAttribute("historicalMatches", matchDataService.getHistoricalMatches());
        // Para depuración, también podemos añadir todos los eventos
        // model.addAttribute("allDebugMatches", matchDataService.getAllMatchEvents());
        return "matches_overview"; // Nombre de la plantilla para la vista general
    }

    @GetMapping("/match/{matchId}")
    public String viewMatchDetails(@PathVariable("matchId") String idDelPartido, Model model) {
        // Pasamos todos los partidos para la sidebar, independientemente de su estado actual
        // para que el usuario pueda navegar entre ellos si lo desea.
        // La lógica de clasificación de la sidebar puede ser diferente a la de la vista general.
        List<MatchEvent> allMatchesForSidebar = matchDataService.getAllMatchEvents();
        Optional<MatchEvent> selectedMatchOpt = matchDataService.getMatchEventById(idDelPartido);

        model.addAttribute("allMatches", allMatchesForSidebar); // Para la lista en la sidebar

        if (selectedMatchOpt.isPresent()) {
            MatchEvent selectedMatch = selectedMatchOpt.get();
            model.addAttribute("selectedMatch", selectedMatch);

            // Lógica para nombres de archivo de logo
            String homeLogoFileName = selectedMatch.getTeamHome()
                    .toLowerCase()
                    .replaceAll("\\s+", "")
                    + ".png";
            String awayLogoFileName = selectedMatch.getTeamAway()
                    .toLowerCase()
                    .replaceAll("\\s+", "")
                    + ".png";
            model.addAttribute("homeLogo", "/images/logos/" + homeLogoFileName);
            model.addAttribute("awayLogo", "/images/logos/" + awayLogoFileName);
        } else {
            model.addAttribute("selectedMatch", null);
            model.addAttribute("errorMessage", "Partido no encontrado con ID: " + idDelPartido);
        }
        return "match_details";
    }
}