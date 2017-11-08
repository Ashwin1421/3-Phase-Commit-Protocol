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
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import Utils.Constants;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Scanner;

/**
 *
 * @author Ashwin
 */
public class Coordinator {
    public static int pCount = 0;
    String hostName;
    int port;
    int pid;
    ServerSocket coordinatorSocket;
    Socket processSocket;
    static Map<Socket, Integer> pSockets = new HashMap<>();
    Constants con = new Constants();
    
    public Coordinator(int port, int pid){
        this.port = port;
        this.pid = pid;
    }
    
    public void start(){
        try {
            coordinatorSocket = new ServerSocket(port);
            coordinatorSocket.setReuseAddress(true);
            coordinatorSocket.setSoTimeout(60*60*1000);
            System.out.println("Coordinator started at "+coordinatorSocket.getLocalSocketAddress()+".");
        } catch (IOException ex) {
            Logger.getLogger(Coordinator.class.getName()).log(Level.SEVERE, null, ex);
        }
        int p = pid;
        while(p<=con.N){
            try {
                processSocket = coordinatorSocket.accept();
                p++;
                pSockets.put(processSocket, p);
                new processHandler(pid, processSocket, pSockets).start();
            } catch (IOException ex) {
                Logger.getLogger(Coordinator.class.getName()).log(Level.SEVERE, null, ex);
            }
            
        }
    }
}


class processHandler extends Thread{
    static int agreedCount = 0;
    static int readyCount = 0;
    Map<Socket, Integer> pSockets;
    Constants con = new Constants();
    static Map<ObjectOutputStream, Integer> pList = new HashMap<>();
    Socket processSocket;
    int pid;
    
    public processHandler(int pid, Socket processSocket, Map<Socket, Integer> pSockets){
        this.pid = pid;
        this.processSocket = processSocket;
        this.pSockets = pSockets;
    }
    
    public void print(Object o){
        System.out.println("[coordinator@"+con.coordinator+"]$:"+o.toString());
    }
    public void sendTo(Integer i, Object o){
        for(ObjectOutputStream out : pList.keySet()){
            if(pList.get(out).equals(i)){
                try {
                    out.writeObject(o);
                    out.flush();
                } catch (IOException ex) {
                    Logger.getLogger(processHandler.class.getName()).log(Level.SEVERE, null, ex);
                }
                print(o);
            }
        }
    }
    @Override
    public void run(){
        try {
            print("Connected to "+processSocket.getRemoteSocketAddress());
            ObjectOutputStream objout = new ObjectOutputStream(processSocket.getOutputStream());
            ObjectInputStream objin = new ObjectInputStream(processSocket.getInputStream());
            
            
            while(true){
                Object recvMsg;
                try {
                    recvMsg = objin.readObject();
                    if(recvMsg instanceof Register){
                        print(recvMsg);
                        if(pSockets.containsKey(processSocket)){
                            pList.put(objout, pSockets.get(processSocket));
                        }
                        Coordinator.pCount++;
                        if(Coordinator.pCount == (con.N-1)){
                            for(Integer i : pList.values()){
                                Register sendMsg = new Register("assigned pid");
                                sendMsg.setpid(i);
                                sendTo(i, sendMsg);
                                
                                Request commitRequest = new Request(pid);
                                commitRequest.settext("Commit Request");
                                sendTo(i, commitRequest);
                            }
                        }
                    }
                    
                    if(recvMsg instanceof Ack){
                        print(recvMsg);
                        if(((Ack)recvMsg).gettext().equalsIgnoreCase("Agree to commit")){
                            agreedCount++;
                            if(agreedCount == (con.N-1)){
                                for(Integer i: pList.values()){
                                    Prepare prepareCommit = new Prepare(pid);
                                    prepareCommit.settext("Prepare to commit");
                                    sendTo(i, prepareCommit);
                                }
                            }
                        }
                        
                        if(((Ack)recvMsg).gettext().equalsIgnoreCase("Ready to commit")){
                            readyCount++;
                            
                            if(readyCount == (con.N-1)){
                                Scanner sc = new Scanner(System.in);
                                print("Please enter a value to commit.");
                                Object val = sc.nextInt();
                                for(Integer i : pList.values()){
                                    Commit commitMsg = new Commit(pid);
                                    commitMsg.settext("Commit");
                                    commitMsg.setval(val);
                                    sendTo(i, commitMsg);
                                }
                                sc.close();
                            }
                        }
                    }
                } catch (ClassNotFoundException ex) {
                    Logger.getLogger(processHandler.class.getName()).log(Level.SEVERE, null, ex);
                }
                
            }
        } catch (IOException ex) {
            Logger.getLogger(processHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}