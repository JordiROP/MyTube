package InterfaceImplement;

import RemoteInterface.MyTubeCallbackInterface;
import RemoteInterface.MyTubeInterface;
import XMLDatabase.XMLCreator;
import XMLDatabase.XMLParser;
import org.jdom2.JDOMException;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.*;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static InterfaceImplement.MyTubeLinuxCalls.makeALinuxCall;
import static InterfaceImplement.MyTubeLinuxCalls.readSystemCallAsString;

public class MyTubeImpl extends UnicastRemoteObject implements MyTubeInterface {
    private static XMLCreator xmlcreator;
    private Set<MyTubeCallbackInterface> callbackObjects;

    public MyTubeImpl() throws IOException, SAXException, ParserConfigurationException {
        super();
        xmlcreator = new XMLCreator();
        callbackObjects = new HashSet<MyTubeCallbackInterface>();
    }

    @Override
    public String getTitleFromKey(int key) throws RemoteException {
        XMLParser xmlParser = new XMLParser();

        return xmlParser.getNameById(Integer.toString(key));
    }

    @Override
    public String getContentFromKey(int key) throws RemoteException {
        String contentName;
        String pathToFile = "./server01/" + Integer.toString(key) + "/";

        XMLParser xmlParser = new XMLParser();
        contentName = xmlParser.getNameById(Integer.toString(key));
        pathToFile = pathToFile + contentName;

        return pathToFile;
    }

    @Override
    public String getContentFromTitle(String title) throws RemoteException {
        int id;
        String path = "";
        try{
            XMLParser xmlParser = new XMLParser();
            id = xmlParser.XMLFindIdByTitle(title);
            path = getContentFromKey(id);
        }catch(IOException ex ){
            ex.printStackTrace();
        }

        return path;
    }

    @Override
    public List<String> searchFromKeyword(String keyword) throws RemoteException {
        List<String> contentFound;
        XMLParser xmlParser = new XMLParser();
        contentFound =  xmlParser.XMLFindByKeyWord(keyword);

        return contentFound;
    }

    @Override
    public List<String> searchAll() throws RemoteException {
        List<String> allContent;
        XMLParser xmlParser = new XMLParser();
        allContent = xmlParser.XMLShowALL();

        return allContent;
    }


    @Override
    public synchronized String uploadContent(String title, String description, byte[] fileData, String userName) throws RemoteException {

        XMLParser xmlParser = new XMLParser();
        String hash = xmlParser.newID();
        String response = "";
        FileOutputStream output;
        String pathOfFile = "." + File.separator + "server01" + File.separator + hash + File.separator + title;
        System.out.println(title);
        makeALinuxCall("mkdir ./server01/" + hash);

        try {
            File file = new File(pathOfFile);
            file.getParentFile().mkdirs();
            file.createNewFile();
            output = new FileOutputStream(file);
            output.write(fileData, 0, fileData.length);
            output.flush();
            output.close();
            notifyAllNewContent(title);

            response = "Successful upload!";

        } catch (FileNotFoundException e) {
            System.err.println("FileServer Exception " + e.getMessage());
            response = "there has been a problem with the file :S";
            e.printStackTrace();

        } catch (IOException e) {
            System.err.println("FileServer Exception " + e.getMessage());
            response = "there has been a IO problem :S";
            e.printStackTrace();
        }

        xmlcreator.addElement(hash, title, description, userName);

        return response;
    }

    @Override
    public String modifyContent(String id, String title, String description) throws RemoteException{
        try {
            String actualTitle = readSystemCallAsString(makeALinuxCall("ls ./server01/" + id));
            String oldTitle = "./server01/" + id + "/" + actualTitle;
            String newTitle = "./server01/" + id + "/" + title;
            makeALinuxCall("mv " + oldTitle + " " + newTitle);
            return xmlcreator.updateElement(id, title, description);
        } catch (Exception e) {
            System.out.println("FileImpl " + e.getMessage());
            e.printStackTrace();
            return "Problem modifyig content";
        }
    }

    @Override
    public String deleteContent(String id, String userName) throws RemoteException {
        XMLParser xmlParser = new XMLParser();
        try {
            if(xmlParser.userIsUploader(userName, id)){
                makeALinuxCall("rm -r ./server01/" + id);
                return xmlcreator.deleteElement(id);
            }else{
                return "Sorry, this file isn't yours.";
            }
        }catch(TransformerException ex){
            ex.printStackTrace();
            return "There has been a problem deleting the content.";
        }
    }

    @Override
    public byte[] downloadContent(int id) throws RemoteException {
        String path;
        try {
            path = getContentFromKey(id);

            File file = new File(path);
            byte buffer[] = new byte[(int) file.length()];
            BufferedInputStream input = new BufferedInputStream(new FileInputStream(path));

            input.read(buffer, 0, buffer.length);
            input.close();

            return (buffer);

        } catch (Exception e) {
            System.out.println("FileImpl " + e.getMessage());
            e.printStackTrace();

            return (null);
        }
    }

    @Override
    public void exit() throws RemoteException {
        //TODO
    }



    public List<String> showOwnFiles(String userName) throws RemoteException {
        XMLParser xmlParser = new XMLParser();
        return xmlParser.XMLFindByUserName(userName);
    }

    @Override
    public void addCallback(MyTubeCallbackInterface callbackObject)
            throws RemoteException {
        callbackObjects.add(callbackObject);
        System.out.println("New client registered on callback list. (" + callbackObjects.size() + " clients)");
    }

    @Override
    public void removeCallback(MyTubeCallbackInterface callbackObject) throws RemoteException {
        callbackObjects.remove(callbackObject);
        System.out.println("A client have been unregistered "
                + "from callback list. (" + callbackObjects.size() + " clients)");

    }

    private void notifyAllNewContent(String title) {
        for (MyTubeCallbackInterface callback : callbackObjects) {
            try {
                callback.notifyNewContent(title);
            } catch (RemoteException ex) {
                System.err.println("Can not notify new content to all clients");
            }
        }
    }

}
