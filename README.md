# Football Data Scraping and API Project

This project performs football data scraping and combines it with data obtained from an API. The data is stored in an SQLite database and updated every 10 minutes.

## Requirements

- Java 11 or higher
- Maven
- API key for the football API

## Installation

1. Clone the repository:
    ```sh
    git clone https://github.com/JuanLopezdeHierro/football-bets.git
    cd football-bets
    ```

2. Build the project using Maven:
    ```sh
    mvn clean install
    ```

## Usage

1. Run the application:
    ```sh
    mvn exec:java -Dexec.mainClass="org.sofing.Main"
    ```

2. Enter your API key when prompted:
    ```sh
    Please enter your API key: <your-api-key>
    ```

The application will start downloading and storing data every 10 minutes.

## Project Structure

- `src/main/java/org/sofing/Main.java`: Main class that starts the application.
- `src/main/java/org/sofing/control/Controller.java`: Controller that manages data downloading and storage.
- `src/main/java/org/sofing/control/FootballApiClient.java`: Interface for the football API client.
- `src/main/java/org/sofing/control/FootballApiClientImpl.java`: Implementation of the football API client.
- `src/main/java/org/sofing/control/FootballWebScraping.java`: Interface for the web scraping.
- `src/main/java/org/sofing/control/FootballWebScrapingImpl.java`: Implementation of the web scraping.
- `src/main/java/org/sofing/control/DataStorage.java`: Interface for the SQLite database management.
- `src/main/java/org/sofing/control/DataStorageImpl.java`: Implementation of the SQLite database management.
- `src/main/java/org/sofing/model/Match.java`: Data model for football matches.

## Technical Details

### `Controller` Class

Manages data downloading and storage every 10 minutes using a `ScheduledExecutorService`.

### `FootballApiClient` Interface

Defines the method to update match fields using the football API.

### `FootballApiClientImpl` Class

Connects to the football API using the API key provided by the user and updates match data.

### `FootballWebScraping` Interface

Defines the method for web scraping.

### `FootballWebScrapingImpl` Class

Performs data scraping from a web source.

### `DataStorage` Interface

Defines the method to insert match data into the SQLite database.

### `DataStorageImpl` Class

Manages the SQLite database. Each time new data is inserted, the `matches` table is dropped and recreated.

## Contributions

Contributions are welcome. Please open an issue or a pull request to discuss any changes you wish to make.

## License

This project is licensed under the MIT License. See the `LICENSE` file for details.
