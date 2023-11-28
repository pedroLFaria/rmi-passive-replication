package org.mqtt.servers;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
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
            if(initiateRegistry()) return;
            System.setProperty("java.rmi.server.hostname","127.0.0.1");
            setServerName();
            mqttService = new MqttService(getServerName());
            EchoServer echoServer = new EchoServer(mqttService);
            bindName(echoServer);
            if(ServerTypeEnum.CLONE.equals(serverType)) mqttService.subscribe();
            mqttService.setMqttCallBack(callback(echoServer));
            if(ServerTypeEnum.CLONE.equals(serverType))healthCheckMaster(echoServer);
            System.out.println("ObjetoServidor esta ativo! Com nome de servidor: "+ getServerName());
        } catch (Exception e) {
            System.err.println("Exceção no servidor Echo: " + e.getMessage());
            e.printStackTrace();
        }
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
            System.out.println("Executando a Registry!");
        } catch (ExportException e) {
            System.out.println("Registry já iniciado!");
            return false;
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
        return true;
    }


    private static void healthCheckMaster(EchoServer echoServer) {
        var executor = Executors.newSingleThreadScheduledExecutor();
        executor.schedule(() -> {
            try {
                Echo objetoRemoto = (Echo) Naming.lookup(getMasterAddress());
                while(true) {
                    objetoRemoto.healthCheck();
                    Thread.sleep(2000);
                    System.out.println("Server Master still alive!");
                }
            } catch (RemoteException e) {
                electNewMaster(echoServer);
            } catch (MalformedURLException | NotBoundException e) {
                throw new RuntimeException(e);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }, 2, TimeUnit.SECONDS);
    }

    private static void electNewMaster(EchoServer echoServer) {
        try {
            System.out.println("Electing the NEW MASTER!");
            if(shouldBeNewMaster()){
                System.out.println("I am the NEW MASTER!");
                serverType = ServerTypeEnum.MASTER;
                bindName(echoServer);
            }
        } catch (MalformedURLException | RemoteException e) {
            System.out.println("I wasn't able to be the NEW MASTER!");
            throw new RuntimeException(e);
        }
    }

    private static boolean shouldBeNewMaster() throws MalformedURLException, RemoteException {
        int nextCloneId = 1;
        while(cloneId > nextCloneId){
            try {
                Naming.lookup(getCloneFullAddress(nextCloneId));
            } catch (NotBoundException e) {
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
