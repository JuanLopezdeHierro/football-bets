package org.sofing.control;

import org.sofing.model.Match;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

public class DataStorageImpl implements DataStorage {
    private static final String DB_URL = "jdbc:sqlite:football.db";

    public DataStorageImpl() {
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
                + " oddHomeTeam REAL,\n"
                + " oddDraw REAL,\n"
                + " oddAwayTeam REAL\n"
                + ");";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.execute();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    private void dropMatchTable() {
        String sql = "DROP TABLE IF EXISTS matches";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.execute();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    @Override
    public void insertMatch(Match match) {
        dropMatchTable();
        createMatchTable();

        String sql = "INSERT INTO matches(team1, team2, dateTime, field, referee, league, oddHomeTeam, oddDraw, oddAwayTeam) VALUES(?,?,?,?,?,?,?,?,?)";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            List<String> teams = match.getTeams();
            List<String> dateTimes = match.getDateTimes();
            List<Double> odds = match.getOdds();

            for (int i = 0; i < teams.size(); i += 2) {
                pstmt.setString(1, teams.get(i));
                pstmt.setString(2, teams.get(i + 1));
                pstmt.setString(3, dateTimes.get(i / 2));
                pstmt.setString(4, match.getField().isEmpty() ? null : match.getField().get(i / 2));
                pstmt.setString(5, match.getReferee());
                pstmt.setString(6, match.getLeague());

                // Si hay cuotas disponibles, se asignan
                if (i / 2 < odds.size() / 3) {
                    pstmt.setDouble(7, odds.get(i / 2 * 3));
                    pstmt.setDouble(8, odds.get(i / 2 * 3 + 1));
                    pstmt.setDouble(9, odds.get(i / 2 * 3 + 2));
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