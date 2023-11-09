package org.mqtt.echo;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ServerApp {
    public static void main(String[] args) {
        try {
            System.setProperty("java.rmi.server.hostname","127.0.0.1");
            LocateRegistry.createRegistry(8088);
            EchoServer echoServer = new EchoServer();
            String serverName = bindName(echoServer);
            System.out.println("ObjetoServidor esta ativo! Com nome de servidor: "+ serverName);
        } catch (Exception e) {
            System.err.println("Exceção no servidor Echo: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void healthCheckMaster() throws RemoteException {
        var executor = Executors.newSingleThreadScheduledExecutor();
        executor.schedule(() -> {
            try {
                Echo objetoRemoto = (Echo) Naming.lookup("EchoServer/master");
                while(true) {
                    objetoRemoto.healthCheck();
                    Thread.sleep(60000);
                }

            } catch (RemoteException e) {
                electeNewMaster();
            } catch (MalformedURLException | NotBoundException e) {
                throw new RuntimeException(e);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }, 10, TimeUnit.SECONDS);
    }

    private static void electeNewMaster() {
        System.out.println("TODO: Eleger novo servidor Mestre");
    }

    private static String bindName(EchoServer echoServer) throws MalformedURLException, RemoteException {
        String nameSpace = "//localhost:8088/EchoServer";
        String nameTag = "/master";
        try {
            Naming.lookup(nameSpace + nameTag);
        } catch (NotBoundException e) {
            Naming.rebind(nameSpace + nameTag, echoServer);
            return nameSpace + nameTag;
        }
        return bindClone(echoServer, 0);
    }

    private static String bindClone(EchoServer echoServer, int i) throws MalformedURLException, RemoteException {
        String nameSpace = "//localhost:8088/EchoServer";
        String nameTag = String.format("/Clone/%d", i);
        try {
            Naming.lookup(nameSpace + nameTag);
        } catch (NotBoundException e) {
            Naming.rebind(nameSpace + nameTag, echoServer);
            return nameSpace + nameTag;
        }
        return bindClone(echoServer, i + 1);
    }
}
