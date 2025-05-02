module networking {
    requires com.google.gson;
    requires org.apache.logging.log4j;

    requires services;
    requires model;
    exports org.example.networking.dto;
    exports org.example.networking.jsonprotocol;
    exports org.example.networking.utils;
}