module persistence {
    exports org.example.repository;
    requires model;
    requires org.apache.logging.log4j;
    requires org.hibernate.orm.core;
    requires java.naming;
    requires com.microsoft.sqlserver.jdbc;
    requires jakarta.persistence;
    requires jakarta.transaction;
    requires spring.beans;
    requires spring.context;
}