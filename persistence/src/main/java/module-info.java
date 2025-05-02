module persistence {
    requires org.apache.logging.log4j;
    requires model;
    requires java.sql;
    exports org.example.utils;
    exports org.example.repository;
}