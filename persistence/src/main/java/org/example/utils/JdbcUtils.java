package org.example.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class JdbcUtils {
    private Properties jdbcProps;
    private static final Logger logger = LogManager.getLogger();

    public JdbcUtils(Properties props) {
        jdbcProps = props;
    }

    public Connection getConnection() {
        logger.traceEntry();
        String url = jdbcProps.getProperty("jdbc.url");
        String user = jdbcProps.getProperty("jdbc.user");
        String pass = jdbcProps.getProperty("jdbc.pass");
        logger.info("trying to connect to database ... {}", url);

        if (user != null && pass != null) {
            logger.info("user: {}", user);
            logger.info("pass: {}", pass);
        } else {
            logger.info("Using integrated security");
        }

        try {
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");

            Connection con;
            if (user != null && pass != null)
                con = DriverManager.getConnection(url, user, pass);
            else
                con = DriverManager.getConnection(url);

            logger.traceExit(con);
            return con;
        } catch (SQLException e) {
            logger.error("Error getting connection", e);
            System.out.println("Error getting connection " + e);
            throw new RuntimeException("Couldn't connect to the database: " + e.getMessage(), e);
        } catch (ClassNotFoundException e) {
            logger.error("JDBC Driver not found", e);
            System.out.println("JDBC Driver not found " + e);
            throw new RuntimeException("JDBC Driver not found: " + e.getMessage(), e);
        }
    }
}