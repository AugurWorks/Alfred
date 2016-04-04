package com.augurworks.alfred.server;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.IOUtils;

import com.augurworks.alfred.server.AlfredServer.Command;

public class AlfredCommunicationHandler implements Runnable {

    private final AlfredDirectoryListener alfredListener;
    private final BufferedReader reader;
    private final PrintWriter writer;
    private final Socket socket;

    public AlfredCommunicationHandler(AlfredDirectoryListener listener,
                                      BufferedReader reader,
                                      PrintWriter writer,
                                      Socket socket) {
        this.alfredListener = listener;
        this.reader = reader;
        this.writer = writer;
        this.socket = socket;
    }

    public AlfredCommunicationHandler(AlfredDirectoryListener listener,
                                      BufferedReader reader,
                                      PrintWriter writer) {
        this(listener, reader, writer, null);
    }

    @Override
    public void run() {
        try {
            boolean shutdownRequested = false;
            while (!shutdownRequested) {
                writer.flush();
                String nextLine = reader.readLine();
                if (nextLine.trim().isEmpty()) {
                    continue;
                }
                Command command = Command.fromString(nextLine);
                if (command == null) {
                    writer.write(AlfredServer.commands());
                    continue;
                }
                if (command == Command.SHUTDOWN || command == Command.SHUTDOWN_NOW) {
                    shutdownRequested = true;
                }
                handleCommand(command, nextLine, alfredListener);
            }
        } catch (Throwable t) {
            System.err.println("Exception caught while communicating with client");
            t.printStackTrace();
        } finally {
            closeResources();
        }
    }

    private void closeResources() {
        try {
            IOUtils.closeQuietly(reader);
            IOUtils.closeQuietly(writer);
            IOUtils.closeQuietly(socket);
        } catch (Throwable t) {
            System.err.println("Error thrown while closing resources");
            t.printStackTrace();
        }
    }

    private void handleCommand(Command command, String line, AlfredDirectoryListener alfredListener) {
        switch(command) {
        case SHUTDOWN:
            String[] split = line.split(" ");
            if (split.length < 2) {
                System.err.println("Could not parse time to wait for shutdown. Will shutdown now.");
                writer.write("Could not parse time to wait for shutdown. Will shutdown now.");
            } else {
                String minutesToWaitString = split[1];
                try {
                    int minutesToWait = Integer.parseInt(minutesToWaitString);
                    alfredListener.shutdownAndAwaitTermination(minutesToWait, TimeUnit.MINUTES);
                    return;
                } catch (NumberFormatException e) {
                    System.err.println("Could not parse time to wait for shutdown. Will shutdown now.");
                    writer.write("Could not parse time to wait for shutdown. Will shutdown now.");
                }
            }
        case SHUTDOWN_NOW:
            alfredListener.shutdownNow();
            return;
        case STATUS:
            writer.write(alfredListener.printStatus());
            return;
        case CANCEL_JOB:
            String[] split2 = line.split(" ");
            if (split2.length < 2) {
                System.err.println("Could not parse file name to cancel.");
                writer.write("Could not parse file name to cancel.");
            } else {
                alfredListener.cancelJob(split2[1]);
            }
        default:
            return;
        }
    }

}
