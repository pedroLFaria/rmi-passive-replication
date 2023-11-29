package org.mqtt.servers;

import org.mqtt.echo.Echo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

public class ClientApp {
    public static void main(String[] args) {
        run();
    }

    private static void run() {
        Echo remoteEcho;

        try {
            System.out.println("Tentando se conectar com o servidor Master");
            remoteEcho = connectToMaster();
            System.out.println("Sucesso!");
            runCommandLine(remoteEcho);
        } catch (MalformedURLException e) {
            System.out.println("Url ilegal utilizada!");
            if(retentativa()){
                run();
            } else throw new RuntimeException(e);
        } catch (NotBoundException e) {
            System.out.println("Servidor Master não encontrado!");
            if(retentativa()){
                run();
            } else throw new RuntimeException(e);
        } catch (RemoteException e) {
            System.out.println("Erro ao chamar o Servidor Master!");
            if(retentativa()){
                run();
            } else throw new RuntimeException(e);
        } catch (InterruptedException e) {
            System.out.println("Erro com as threads do sistema!");
            if(retentativa()){
                run();
            } else throw new RuntimeException(e);
        }
    }

    private static void runCommandLine(Echo remoteEcho) throws RemoteException, InterruptedException {
        String userInput = "";
        System.out.println("\n----------------\n" +
                "Comandos:\n" +
                "s -> para enviar uma nova mensagem\n" +
                "a -> para pegar todas as mensagens\n" +
                "q -> para sair\n");
        while(!"q".equalsIgnoreCase(userInput.trim())){
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
            System.out.println("Digite o comando: ");
            userInput = getUserInput(br);
            if("s".equalsIgnoreCase(userInput.trim())){
                System.out.println("Digite a nova mensagem: ");
                userInput = getUserInput(br);
                remoteEcho.echo(userInput);
                System.out.println("Mensagem enviada com sucesso!");
            } else if("a".equalsIgnoreCase(userInput.trim())){
                System.out.println("Buscando todas as mensagens: ");
                System.out.println(remoteEcho.getListOfMsg());
            } else  if("q".equalsIgnoreCase(userInput.trim())) {
                System.out.println("Saindo!");
            } else {
                System.out.println("Comando desconhecido! \n -------------- \n");
                runCommandLine(remoteEcho);
            }
            Thread.sleep(500);
        }
    }

    private static boolean retentativa() {
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        System.out.println("Tentar executar novamente? (s ou n)");
        String userInput = getUserInput(br);
        if("s".equalsIgnoreCase(userInput.trim())){
            System.out.println("Tentando se reconectar!");
            return true;
        }
        if("n".equalsIgnoreCase(userInput.trim())){
            System.out.println("Saindo!");
            return false;
        }
        System.out.println("Comando desconhecido! \n -------------- \n");
        return retentativa();
    }

    private static String getUserInput(BufferedReader br) {
        String userInput = null;
        try {
            userInput = br.readLine();
        } catch (IOException e){
            System.out.println(e.getMessage());
        }
        return userInput;
    }

    private static Echo connectToMaster() throws NotBoundException, MalformedURLException, RemoteException, InterruptedException {
        Thread.sleep(500);
        return (Echo) Naming.lookup("//localhost:8088/EchoServer/master");
    }
}
