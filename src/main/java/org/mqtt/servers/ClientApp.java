package org.mqtt.servers;

import org.mqtt.echo.Echo;

import java.rmi.Naming;

public class ClientApp {
    public static void main(String[] args) {
        try {
            run(0);
        } catch (InterruptedException e) {
            System.out.println(e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private static void run(int tries) throws InterruptedException {
        Echo remoteEcho;

        try {
            remoteEcho = (Echo) Naming.lookup("//localhost:8088/EchoServer/master");
            int i = 0;
            while(true){
                i++;
                System.out.println(remoteEcho.echo(String.format("Teste %d", i)));
                if(i%5==0){
                    System.out.println(remoteEcho.getListOfMsg());
                }
                tries = 0;
                Thread.sleep(5000);
            }
        } catch (Exception e) {
            tries++;
            while(tries < 5){
                Thread.sleep(5000);
                run(tries);
                return;
            }
            throw new RuntimeException();
        }
    }
}
