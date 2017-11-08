/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Node;

import Message.Ack;
import Message.Commit;
import Message.Prepare;
import Message.Register;
import Message.Request;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Ashwin
 */
public class Process {
    String hostName;
    int port;
    int pid;
    Socket processSocket;
    
    public Process(String hostName, int port){
        this.hostName = hostName;
        this.port = port;
    }
    
    public void print(Object o){
        System.out.println("[process@"+processSocket.getLocalSocketAddress()+"]$:"+o.toString());
    }
    public void send(ObjectOutputStream out, Object o){
        try {
            out.writeObject(o);
            out.flush();
            print(o);
        } catch (IOException ex) {
            Logger.getLogger(Process.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }
    public void start(){
        ObjectInputStream objin = null;
        try {
            try {
                processSocket = new Socket(hostName, port);
            } catch (IOException ex) {
                Logger.getLogger(Process.class.getName()).log(Level.SEVERE, null, ex);
            }
            print("Connection established with "+hostName+"@"+port+".");
            ObjectOutputStream objout = new ObjectOutputStream(processSocket.getOutputStream());
            objin = new ObjectInputStream(processSocket.getInputStream());
            
            
            Register registerMsg = new Register("Register");
            send(objout, registerMsg);
            while(true){
                try {
                    Object recvMsg = objin.readObject();
                    
                    if(recvMsg instanceof Register){
                        print(recvMsg);
                        pid = ((Register) recvMsg).getpid();
                    }
                    
                    if(recvMsg instanceof Request){
                        print(recvMsg);
                        Ack sendAck = new Ack(pid);
                        sendAck.settext("Agree to commit");
                        send(objout, sendAck);
                    }
                    
                    if(recvMsg instanceof Prepare){
                        print(recvMsg);
                        Ack sendAck = new Ack(pid);
                        sendAck.settext("Ready to commit");
                        send(objout, sendAck);
                    }
                    
                    if(recvMsg instanceof Commit){
                        print(recvMsg);
                        print("Received value="+((Commit) recvMsg).getval());
                    }
                    
                    
                } catch (ClassNotFoundException ex) {
                    Logger.getLogger(Process.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(Process.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                objin.close();
            } catch (IOException ex) {
                Logger.getLogger(Process.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        
    }
}
