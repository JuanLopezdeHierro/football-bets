package org.sofing.control;

import org.sofing.model.Match;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

public class DataStorage {
    private static final String DB_URL = "jdbc:sqlite:football.db";

    public DataStorage() {
        createNewDatabase();
        createMatchTable();
    }

    private void createNewDatabase() {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            if (conn != null) {
                System.out.println("A new database has been created.");
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    private void createMatchTable() {
        String sql = "CREATE TABLE IF NOT EXISTS matches (\n"
                + " id INTEGER PRIMARY KEY AUTOINCREMENT,\n"
                + " team1 TEXT NOT NULL,\n"
                + " team2 TEXT NOT NULL,\n"
                + " dateTime TEXT NOT NULL,\n"
                + " field TEXT,\n"
                + " referee TEXT,\n"
                + " league TEXT,\n"
                + " oddHomeTeam REAL,\n"  // Añadir coma aquí
                + " oddDraw REAL,\n"      // Añadir coma aquí
                + " oddAwayTeam REAL\n"   // Añadir coma aquí
                + ");";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.execute();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public void insertMatch(Match match) {
        String sql = "INSERT INTO matches(team1, team2, dateTime, field, referee, league, oddHomeTeam, oddDraw, oddAwayTeam) VALUES(?,?,?,?,?,?,?,?,?)";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            List<String> teams = match.getTeams();
            List<String> dateTimes = match.getDateTimes();
            List<Double> odds = match.getOdds();

            for (int i = 0; i < teams.size(); i += 2) {
                pstmt.setString(1, teams.get(i));       // Equipo 1
                pstmt.setString(2, teams.get(i + 1));   // Equipo 2
                pstmt.setString(3, dateTimes.get(i / 2)); // Fecha/Hora
                pstmt.setString(4, match.getField().isEmpty() ? null : match.getField().get(i / 2)); // Campo
                pstmt.setString(5, match.getReferee()); // Árbitro
                pstmt.setString(6, match.getLeague());  // Liga

                // Si hay cuotas disponibles, se asignan
                if (i / 2 < odds.size() / 3) {
                    pstmt.setDouble(7, odds.get(i / 2 * 3));     // Cuota equipo local
                    pstmt.setDouble(8, odds.get(i / 2 * 3 + 1)); // Cuota empate
                    pstmt.setDouble(9, odds.get(i / 2 * 3 + 2)); // Cuota equipo visitante
                } else {
                    pstmt.setNull(7, java.sql.Types.DOUBLE);
                    pstmt.setNull(8, java.sql.Types.DOUBLE);
                    pstmt.setNull(9, java.sql.Types.DOUBLE);
                }

                pstmt.addBatch();
            }

            pstmt.executeBatch();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }
}