module ProiectJava {
    requires org.apache.logging.log4j;
    requires java.sql;
    requires spring.security.crypto;
    exports org.example.repository;
    exports org.example.service;
    exports org.example.model;
    exports org.example.utils;
}