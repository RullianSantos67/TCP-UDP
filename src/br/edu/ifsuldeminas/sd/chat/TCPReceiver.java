package br.edu.ifsuldeminas.sd.chat;

import java.io.*;
import java.net.*;

/**
 * Implementação TCP da interface Receiver.
 * Usa ServerSocket para aguardar conexões na porta local.
 * Cada conexão aceita é tratada em uma thread separada,
 * permitindo receber mensagens de múltiplos remetentes.
 */
class TCPReceiver implements Receiver {

    private static final int MIN_PORT = 1024;

    private final int port;
    private final MessageContainer container;
    private ServerSocket serverSocket;
    private boolean isRunning = false;

    public TCPReceiver(int port, MessageContainer container) throws ChatException {
        if (port <= MIN_PORT)
            throw new ChatException("O receiver TCP não pode usar portas reservadas (≤ 1024).",
                new IllegalArgumentException());
        if (container == null)
            throw new ChatException("Container de mensagens não pode ser nulo.",
                new IllegalArgumentException());
        this.port      = port;
        this.container = container;
        try {
            serverSocket = new ServerSocket(port);
        } catch (IOException e) {
            throw new ChatException(
                "Erro ao abrir ServerSocket TCP na porta " + port + ". Porta já em uso?", e);
        }
        new Thread(this, "TCP-Receiver-" + port).start();
    }

    @Override
    public void run() {
        if (!isRunning) {
            isRunning = true;
            while (!serverSocket.isClosed()) {
                try {
                    Socket client = serverSocket.accept();
                    // Cada conexão recebida roda em sua própria thread
                    Thread t = new Thread(() -> handleClient(client), "TCP-Client-Handler");
                    t.setDaemon(true);
                    t.start();
                } catch (IOException e) {
                    if (!serverSocket.isClosed()) {
                        container.newMessage("Erro ao aceitar conexão TCP.");
                    }
                }
            }
        }
    }

    private void handleClient(Socket socket) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(socket.getInputStream(), "UTF-8"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                final String msg = line.trim();
                if (!msg.isEmpty()) {
                    container.newMessage(msg);
                }
            }
        } catch (IOException e) {
            // Conexão encerrada pelo remetente — comportamento normal
        }
    }
}
