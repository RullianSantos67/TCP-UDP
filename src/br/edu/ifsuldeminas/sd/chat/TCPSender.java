package br.edu.ifsuldeminas.sd.chat;

import java.io.*;
import java.net.*;

/**
 * Implementação TCP da interface Sender.
 * A conexão é estabelecida na primeira chamada a send() (lazy connect),
 * o que permite que ambos os lados iniciem seus receivers antes de tentar enviar.
 */
class TCPSender implements Sender {

    private final String host;
    private final int port;
    private Socket socket       = null;
    private PrintWriter writer  = null;

    public TCPSender(String host, int port) throws ChatException {
        if (host == null || host.trim().isEmpty())
            throw new ChatException("Host inválido.", new IllegalArgumentException("host nulo"));
        if (port <= 1024)
            throw new ChatException("Porta inválida para TCPSender.", new IllegalArgumentException());
        this.host = host;
        this.port = port;
    }

    @Override
    public synchronized void send(String message) throws ChatException {
        try {
            // Conecta na primeira vez ou reconecta se a conexão caiu
            if (socket == null || socket.isClosed() || !socket.isConnected()) {
                socket = new Socket();
                // Timeout de 10s para o outro lado estar pronto
                socket.connect(new InetSocketAddress(host, port), 10_000);
                writer = new PrintWriter(
                    new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);
            }
            writer.println(message);
            if (writer.checkError()) {
                socket = null; // força reconexão na próxima vez
                throw new ChatException("Erro ao escrever no stream TCP.",
                    new IOException("PrintWriter error"));
            }
        } catch (IOException e) {
            socket = null;
            throw new ChatException(
                "Não foi possível enviar via TCP. Verifique se o outro lado está conectado. Detalhe: "
                + e.getMessage(), e);
        }
    }
}
