package org.mqtt.servers;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.mqtt.echo.Echo;
import org.mqtt.echo.EchoServer;
import org.mqtt.echo.ServerTypeEnum;
import org.mqtt.services.MqttService;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.ExportException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ServerApp {
    static String host = "//localhost";
    static String port = "8088";
    static String serviceName = "EchoServer";
    static MqttService mqttService;
    static ServerTypeEnum serverType;
    static int cloneId = 0;

    public static void main(String[] args) {
        try {
            setSystemProperty();

            choseServerType();
            if(serverType.equals(ServerTypeEnum.REGISTRY)) while(true);

            EchoServer echoServer = getEchoServer();
            bindName(echoServer);

            if(serverType.equals(ServerTypeEnum.CLONE)) {
                mqttService.subscribe();
                mqttService.setMqttCallBack(callback(echoServer));
                initiateCommunicationWithMaster(echoServer);
            }
            System.out.println("ObjetoServidor esta ativo! Com nome de servidor: "+ getServerName());
        } catch (Exception e) {
            System.err.println("Exceção no servidor Echo: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static EchoServer getEchoServer() throws RemoteException {
        mqttService = new MqttService(getServerName());
        return new EchoServer(mqttService);
    }

    private static void setSystemProperty() {
        System.setProperty("java.rmi.server.hostname","127.0.0.1");
    }

    private static void choseServerType() throws MalformedURLException, RemoteException {
        if(initiateRegistry()) return;
        setServerName();
    }

    private static String getServerName() {
        return ServerTypeEnum.MASTER.equals(serverType) ? serverType.toString() : String.format("%s/%d", serverType, cloneId);
    }

    private static void setServerName() throws MalformedURLException, RemoteException {
        serverType = cloneId == 0 ? ServerTypeEnum.MASTER :  ServerTypeEnum.CLONE;
        try {
            Naming.lookup(getFullAddress());
        } catch (NotBoundException e) {
            return;
        }
        cloneId++;
        setServerName();
    }

    private static boolean initiateRegistry(){
        try {
            LocateRegistry.createRegistry(8088);
            serverType = ServerTypeEnum.REGISTRY;
            System.out.printf("Executando a Registry na porta %d!\n", 8080);
        } catch (ExportException e) {
            System.out.println("Registry já iniciado!");
            return false;
        } catch (RemoteException e) {
            System.out.println("Falha ao iniciar a Registry: " + e.getMessage());
            throw new RuntimeException(e);
        }
        return true;
    }


    private static void initiateCommunicationWithMaster(EchoServer echoServer) throws MalformedURLException, NotBoundException, RemoteException {
        Echo remoteEchoMaster = (Echo) Naming.lookup(getMasterAddress());
        echoServer.setMessages(remoteEchoMaster.getListOfMsg());
        var executor = Executors.newSingleThreadScheduledExecutor();
        executor.schedule(() -> {
            try {
                healthCheckLoop(echoServer, remoteEchoMaster);
            } catch (InterruptedException e) {
                System.out.println("Failure when dealing with threads: " + e.getMessage());
                throw new RuntimeException(e);
            }
        }, 2, TimeUnit.SECONDS);
    }

    private static void healthCheckLoop(EchoServer echoServer, Echo remoteEchoMaster) throws InterruptedException {
        try {
            while(true) {
                remoteEchoMaster.healthCheck();
                System.out.println("Server Master still alive!");
                Thread.sleep(2000);
            }
        } catch (RemoteException e) {
            electNewMaster(echoServer, 0);
        }  catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private static void electNewMaster(EchoServer echoServer,int numberOfTries) throws InterruptedException {
        try {
            System.out.println("Electing the NEW MASTER!");
            if(shouldBeNewMaster()){
                System.out.println("I am the NEW MASTER!");
                serverType = ServerTypeEnum.MASTER;
                bindName(echoServer);
                mqttService.unsubscribe();
            } else {
                Echo newEchoMaster = (Echo) Naming.lookup(getMasterAddress());
                healthCheckLoop(echoServer, newEchoMaster);
            }
        } catch (MalformedURLException | RemoteException | MqttException e) {
            System.out.println("I wasn't able to be the NEW MASTER: " + e.getMessage());
            throw new RuntimeException(e);
        } catch (NotBoundException e) {
            if(numberOfTries < 5) {
                System.out.println("Failure to find NEW MASTER, retrying: " + numberOfTries);
                Thread.sleep(1500);
                electNewMaster(echoServer, ++numberOfTries);
            } else {
                System.out.println("Total failure during election time: " + e.getMessage());
                throw new RuntimeException(e);
            }
        }
    }

    private static boolean shouldBeNewMaster() throws MalformedURLException {
        int nextCloneId = 1;
        while(cloneId > nextCloneId){
            try {
                Echo cloneServer = (Echo) Naming.lookup(getCloneFullAddress(nextCloneId));
                cloneServer.healthCheck();
            } catch (NotBoundException | RemoteException e) {
                nextCloneId++;
            }
        }
        return cloneId == nextCloneId;
    }

    private static void bindName(EchoServer echoServer) throws MalformedURLException, RemoteException {
        String fullAddress = getFullAddress();
        Naming.rebind(fullAddress, echoServer);
    }

    private static String getFullAddress(){
        if(serverType.equals(ServerTypeEnum.MASTER)){
            return getMasterAddress();
        }
        return getCloneFullAddress(cloneId);
    }

    private static String getMasterAddress() {
        return String.format("%s:%s/%s/%s", host, port, serviceName, ServerTypeEnum.MASTER);
    }

    private static String getCloneFullAddress(int cloneId) {
        return String.format("%s:%s/%s/%s/%d", host, port, serviceName, serverType, cloneId);
    }

    private static MqttCallback callback(EchoServer echoServer){
        return new MqttCallback(){

            @Override
            public void connectionLost(Throwable throwable) {
                System.out.println("Disconected from Mqtt Server!");
            }

            @Override
            public void messageArrived(String topic, MqttMessage mqttMessage) throws Exception {
                echoServer.addMessage(mqttMessage.toString());
                System.out.println("Mensagem adicionada no servidor: " + mqttMessage.toString());
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {

            }
        };
    }
}
