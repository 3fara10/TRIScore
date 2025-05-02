package org.example.networking.jsonprotocol;

import org.example.service.IAuthentificationService;
import org.example.service.IParticipantService;
import org.example.service.IResultService;

/**
 * Combined server proxy interface that extends all service interfaces
 * and provides a unified API for client communication with the server.
 */
public interface IServerJsonProxy extends
        IAuthentificationService,
        IResultService,
        IParticipantService,
        AutoCloseable {

    // No additional methods required as this is a composite interface
}