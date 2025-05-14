module networking {
    exports org.example.networking.jsonprotocol;
    exports org.example.networking.utils;
    exports org.example.networking.dto;
    requires com.google.gson;
    requires io.grpc;
    requires io.grpc.stub;
    requires org.apache.logging.log4j;
    requires services;
    requires model;
}