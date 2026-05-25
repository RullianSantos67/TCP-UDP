package br.edu.ifsuldeminas.sd.chat;

/**
 * Fábrica de chat TCP.
 * Espelha ChatFactory (UDP), mas usa TCPReceiver e TCPSender.
 * Os clientes não precisam saber qual protocolo está sendo usado internamente.
 */
public class TCPChatFactory {

    public static Sender build(String serverName, int serverPort,
            int localPort, MessageContainer container) throws ChatException {
        // Inicia o receiver (ServerSocket) antes de criar o sender
        new TCPReceiver(localPort, container);
        return new TCPSender(serverName, serverPort);
    }
}
