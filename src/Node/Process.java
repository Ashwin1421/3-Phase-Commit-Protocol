/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Node;

import Message.Abort;
import Message.Ack;
import Message.Commit;
import Message.Prepare;
import Message.Register;
import Message.Request;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Ashwin
 */
public class Process{
    String hostName;
    int port;
    int pid;
    Socket processSocket;
    PrintWriter fileWriter = null;
    static int transaction_id = 0;
    Scanner sc = new Scanner(System.in);
    String file_name;
    File file;
    
    public Process(String hostName, int port){
        this.hostName = hostName;
        this.port = port;
    }
    
    
    public void connect() throws IOException{
        processSocket = new Socket(hostName, port);
        print("Connection successful.");
        if(processSocket.isConnected()){
            start();
        }else{
            //start();
        }
    }
    
    public void reconnect(){
        print(processSocket);
        print(processSocket.isClosed());
    }
    private void save_state(int transaction_id, Object val){
        try {
            file = new File(file_name);
            fileWriter = new PrintWriter(new FileWriter(file, true));
            if(file.exists() && !file.isDirectory()){
                fileWriter.append("Transaction : "+transaction_id+", commited value: "+val+"\n");
                fileWriter.close();
            }else{
                fileWriter.println("Transaction : "+transaction_id+", commited value: "+val);
                fileWriter.close();
            }
            
        } catch (IOException ex) {
            Logger.getLogger(Process.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    private void print(Object o){
        System.out.println("[process@"+processSocket.getLocalSocketAddress()+"]$:"+o.toString());
    }
    private void send(ObjectOutputStream out, Object o){
        try {
            out.writeObject(o);
            out.flush();
            print("Sent: "+o);
        } catch (IOException ex) {
            Logger.getLogger(Process.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }

    public void start(){
        try {
            print("Connection established with "+hostName+"@"+port+".");
            ObjectOutputStream objout = new ObjectOutputStream(processSocket.getOutputStream());
            ObjectInputStream objin = new ObjectInputStream(processSocket.getInputStream());
            
            Register registerMsg = new Register("Register");
            send(objout, registerMsg);
            while(true){
                if(processSocket.isConnected()){
                    Object recvMsg = objin.readObject();
                    
                    if(recvMsg instanceof Register){
                        print("Received: "+recvMsg);
                        pid = ((Register) recvMsg).getpid();
                        this.file_name = "state_"+pid+".out";
                    }
                    
                    if(recvMsg instanceof Request){
                        print("Received: "+recvMsg);
                        print("Do you wish to commit ? (y/n)");
                        String text = sc.nextLine();
                        Ack sendAck = new Ack(pid);
                        if(text.equalsIgnoreCase("y")){
                            sendAck.settext("Agree");
                            send(objout, sendAck);
                        }else{
                            Abort abortMsg = new Abort(pid);
                            abortMsg.settext("Abort");
                            send(objout, abortMsg);
                            System.exit(0);
                        }
                    }
                    print(processSocket.isOutputShutdown());
                    if(processSocket.isInputShutdown()){
                        print("Timeout");
                        System.exit(0);
                    }
                    if(recvMsg instanceof Prepare){
                        print("Received: "+recvMsg);
                        Ack sendAck = new Ack(pid);
                        sendAck.settext("Ready to commit");
                        send(objout, sendAck);
                    }
                    
                    if(recvMsg instanceof Commit){
                        print("Received: "+recvMsg);
                        print("Received value="+((Commit) recvMsg).getval());
                        save_state(transaction_id, ((Commit) recvMsg).getval());
                        System.exit(0);
                    }
                    
                    if(recvMsg instanceof Abort){
                        print("Received: "+recvMsg);
                        print("Aborting...");
                        System.exit(0);
                    }
                }else{
                    print("Connection lost.");
                }
            }
        } catch (IOException ex) {
            //Logger.getLogger(Process.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(Process.class.getName()).log(Level.SEVERE, null, ex);
        } 
        
    }
}
