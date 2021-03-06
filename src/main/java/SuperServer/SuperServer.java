package SuperServer;

import ServerRemoteInterface.MyTubeInterface;
import SuperServerInterfaceImpl.SuperServerImpl;

import java.util.concurrent.TimeUnit;
import java.net.UnknownHostException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;

public class SuperServer {

    private static final String rmi_server_name = "MyTube";
    private static final String registryName = "MySuperServer";
    private static ArrayList<MyTubeInterface> stubs = new ArrayList<>();
    private static SuperServerImpl stub;
    private Registry registry;
    private final String registryURL;
    private String ip;
    private int port;

    private SuperServer(String ip, int port) {
        this.ip = ip;
        this.port = port;
        this.registryURL = "rmi://" + ip + ":" + port + "/" + registryName;
    }

    static private MyTubeInterface connectToTheServer(String ip, int port) throws NotBoundException {
        MyTubeInterface stub = null;

        try {
            System.setProperty("java.security.policy", "security.policy");
            if (System.getSecurityManager() == null) {
                System.setSecurityManager(new SecurityManager());
            }

            Registry registry = LocateRegistry.getRegistry(ip, port);
            stub = (MyTubeInterface) registry.lookup(rmi_server_name);

            System.out.println("SuperServer connected on: " + rmi_server_name);

        } catch (RemoteException ex) {
            System.err.println("Can't connect to the server");
            System.exit(1);
        }

        return stub;
    }

    public static void main(String args[]) throws RemoteException, UnknownHostException {

        //Reads SuperServer Info
        String superServerIP = Utils.Reader.ipSuperServerReader();
        int superServerPort = Utils.Reader.portSuperServerReader();

        SuperServer superServer = new SuperServer(superServerIP, superServerPort);

        final Thread mainThread = Thread.currentThread();
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void start() {
                System.out.println("Stopping server!");
                try {
                    mainThread.join();
                } catch (Exception e) {
                    System.out.println("Can not finalize main process");
                }
                try {
                    stopServer(superServer.getRegistry(), registryName, superServer.getStub());
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                System.exit(0);
            }
        });
        superServer.runServer();
        threadLauncher();

        while (true) {

            String serverIP = Utils.Reader.ipServerReader();
            int serverPort = Utils.Reader.portServerReader();
            try {
                stubs.add(connectToTheServer(serverIP, serverPort));
            } catch (NotBoundException e) {
                e.printStackTrace();
            }
        }
    }

    private static void stopServer(Registry registry, String registryName, SuperServerImpl stub) throws RemoteException {
        try{
            registry.unbind(registryName);
            UnicastRemoteObject.unexportObject(stub, true);
            System.out.println("Server stopped correctly");
        } catch (Exception ex) {
            System.err.println("Server failed on stop");
        }
    }

    /**
     * Runs the Server
     */
    private void runServer() {
        try {
            System.setProperty("java.rmi.server.hostname", ip);
            System.setProperty("java.security.policy", "security.policy");
            if (System.getSecurityManager() == null) {
                System.setSecurityManager(new SecurityManager());
            }
            stub = new SuperServerImpl();
            registry = Utils.Registrator.getRegistry(ip, port);
            registry.rebind(registryName, stub);
            System.out.println("MyTube Server ready on: " + registryURL);
        } catch (Exception ex) {
            System.err.println("Server error: " + ex.toString());
        }
    }

    /***
     * In charge of starting the thread.
     * @throws UnknownHostException unknown host exception
     */
    private static void threadLauncher() throws UnknownHostException {
        Thread theThread = new Thread();
        theThread.start();
    }

    private Registry getRegistry(){
        return registry;
    }

    private SuperServerImpl getStub(){
        return stub;
    }

    public static List<String> getAllDistributedContent() throws RemoteException{
        List<String> allcontent = new ArrayList<>();
        for(MyTubeInterface stub:stubs){
            allcontent.addAll(stub.searchAll());
        }

        try {
            TimeUnit.SECONDS.sleep(2);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return allcontent;
    }

    public static List<String> searchDistributedFromKeyword(String keyword) throws RemoteException{
        List<String> allcontent = new ArrayList<>();
        for(MyTubeInterface stub:stubs){
            allcontent.addAll(stub.searchFromKeyword(keyword));

        }

        return allcontent;
    }

    public static byte[] downloadDistributedContent(String id, String title, String user) throws RemoteException {
        byte[] content;
        for(MyTubeInterface stub:stubs){
            content = stub.downloadSpecificContent(id, title, user);
            if(content != null){
                return  content;
            }
        }
        return null;
    }
}
