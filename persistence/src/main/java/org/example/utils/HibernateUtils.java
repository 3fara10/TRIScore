package org.example.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

import java.util.Properties;

public class HibernateUtils {
    private static final Logger logger = LogManager.getLogger();
    private static SessionFactory sessionFactory;

    public HibernateUtils(Properties props) {
        initialize();
    }

    public void initialize() {
        logger.traceEntry();

        if (sessionFactory == null) {
            try {
                Configuration configuration = new Configuration().configure();
                sessionFactory = configuration.buildSessionFactory();
                logger.info("Hibernate SessionFactory initialized successfully using hibernate.cfg.xml");
                logger.traceExit(sessionFactory);
            } catch (Exception e) {
                logger.error("Could not initialize Hibernate SessionFactory", e);
                System.out.println("Error initializing Hibernate: " + e);
                throw new RuntimeException("Could not initialize Hibernate SessionFactory: " + e.getMessage(), e);
            }
        }
    }

    public SessionFactory getSessionFactory() {
        logger.traceEntry();
        if (sessionFactory == null || sessionFactory.isClosed()) {
            initialize();
        }
        logger.traceExit(sessionFactory);
        return sessionFactory;
    }

    public void close() {
        logger.traceEntry();
        if (sessionFactory != null && !sessionFactory.isClosed()) {
            sessionFactory.close();
            sessionFactory = null;
            logger.info("Hibernate SessionFactory closed");
        }
        logger.traceExit();
    }
}
