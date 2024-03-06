package dataAccess;

import java.sql.*;
import java.util.Properties;
import static java.sql.Types.NULL;

public class DatabaseManager {
    private static final String databaseName;
    private static final String user;
    private static final String password;
    private static final String connectionUrl;

    /*
     * Load the database information for the db.properties file.
     */
    static {
        try {
            try (var propStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("db.properties")) {
                if (propStream == null) throw new Exception("Unable to load db.properties");
                Properties props = new Properties();
                props.load(propStream);
                databaseName = props.getProperty("db.name");
                user = props.getProperty("db.user");
                password = props.getProperty("db.password");

                var host = props.getProperty("db.host");
                var port = Integer.parseInt(props.getProperty("db.port"));
                connectionUrl = String.format("jdbc:mysql://%s:%d", host, port);
            }
        } catch (Exception ex) {
            throw new RuntimeException("unable to process db.properties. " + ex.getMessage());
        }
    }

    /**
     * Creates the database if it does not already exist.
     */
    static void createDatabase() throws DataAccessException {
        try {
            var statement = "CREATE DATABASE IF NOT EXISTS " + databaseName;
            var conn = DriverManager.getConnection(connectionUrl, user, password);
            try (var preparedStatement = conn.prepareStatement(statement)) {
                preparedStatement.executeUpdate();
            }
        } catch (SQLException e) {
            throw new DataAccessException(e.getMessage(), 500);
        }
    }

    /**
     * Create a connection to the database and sets the catalog based upon the
     * properties specified in db.properties. Connections to the database should
     * be short-lived, and you must close the connection when you are done with it.
     * The easiest way to do that is with a try-with-resource block.
     * <br/>
     * <code>
     * try (var conn = DbInfo.getConnection(databaseName)) {
     * // execute SQL statements.
     * }
     * </code>
     */
    static Connection getConnection() throws DataAccessException {
        try {
            var conn = DriverManager.getConnection(connectionUrl, user, password);
            conn.setCatalog(databaseName);
            return conn;
        } catch (SQLException e) {
            throw new DataAccessException(e.getMessage(), 500);
        }
    }

    private final String[] createStatements = {
        """
        CREATE TABLE IF NOT EXISTS  user (
          `username` varchar(256) NOT NULL ,
          `password` varchar(256) NOT NULL,
          `email` varchar(256) NOT NULL,
          `json` TEXT DEFAULT NULL,
          PRIMARY KEY (`username`)
        )
        """,

        """
        CREATE TABLE IF NOT EXISTS  auth (
          `authToken` varchar(256) NOT NULL ,
          `username` varchar(256) NOT NULL,
          `json` TEXT DEFAULT NULL,
          PRIMARY KEY (`authToken`)
        )
        """,

        """
        CREATE TABLE IF NOT EXISTS  game (
          `gameID` int NOT NULL ,
          `whiteUsername` varchar(256) DEFAULT NULL,
          `blackUsername` varchar(256) DEFAULT NULL,
          `gameName` varchar(256) NOT NULL,
          `jsonChessGame` TEXT DEFAULT NULL,
          `json` TEXT DEFAULT NULL,
          PRIMARY KEY (`gameID`)
        )
        """
    };

    public void configureDatabase() throws DataAccessException {
        createDatabase();
        try (var conn = DatabaseManager.getConnection()) {
            for (var statement : createStatements) {
                try (var preparedStatement = conn.prepareStatement(statement)) {
                    preparedStatement.executeUpdate();
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException(e.getMessage(), 500);
        }
    }

    public void executeUpdate(String statement, Object... params) throws DataAccessException {
        try (var conn = getConnection()) {
            try (var ps = conn.prepareStatement(statement)) {
                for (var i = 0; i < params.length; i++) {
                    var param = params[i];
                    switch (param) {
                        case String p -> ps.setString(i + 1, p);
                        case Integer p -> ps.setInt(i + 1, p);
                        case null -> ps.setNull(i + 1, NULL);
                        default -> {
                        }
                    }
                }
                ps.executeUpdate();

            }
        } catch (SQLException e) {
            throw new DataAccessException(e.getMessage(), 500);
        }
    }
}
