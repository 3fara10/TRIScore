module server {
    requires org.apache.logging.log4j;
    requires spring.security.crypto;
    requires services;
    requires persistence;
    requires networking;
    requires model;
    exports org.example.server;
}