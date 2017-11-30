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
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import Utils.Constants;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Scanner;

/**
 *
 * @author Ashwin
 */
public class Coordinator{
    public static int pCount = 0;
    String hostName;
    int port;
    public static int pid;
    public static ServerSocket coordinatorSocket;
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
    static long t1, t2;
    Map<Socket, Integer> pSockets;
    Constants con = new Constants();
    static Map<ObjectOutputStream, Integer> pList = new HashMap<>();
    Scanner sc = new Scanner(System.in);
    PrintWriter fileWriter = null;
    static int transaction_id = 0;
    String file_name = "state_"+Coordinator.pid+".out";
    File file;
    Socket processSocket;
    int pid;
    
    public processHandler(int pid, Socket processSocket, Map<Socket, Integer> pSockets){
        this.pid = pid;
        this.processSocket = processSocket;
        this.pSockets = pSockets;
    }
    
    public void save_state(int transaction_id, Object val){
        try {
            file = new File(file_name);
            fileWriter = new PrintWriter(new FileWriter(file, true));
            if(file.exists() && !file.isDirectory()){
                BufferedReader bf = new BufferedReader(new FileReader(file));
                String line;
                while((line=bf.readLine())!=null){
                    int prev_transaction_id = Integer.parseInt(line.split(",|:")[1].trim());
                    transaction_id = prev_transaction_id + 1;
                }
                fileWriter.append("Transaction : "+transaction_id+", commited value: "+val+"\n");
                fileWriter.close();
            }else{
                fileWriter.println("Transaction : "+transaction_id+", commited value: "+val);
                fileWriter.close();
            }
        } catch (IOException ex) {
            Logger.getLogger(processHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
        
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
                print("Sent: "+o);
            }
        }
    }
    
    private void sendAll(Object o){
        for(Integer i : pList.values()){
            sendTo(i, o);
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
                    recvMsg = objin.readObject();
                    if(recvMsg instanceof Register){
                        print("Received: "+recvMsg);
                        if(pSockets.containsKey(processSocket)){
                            pList.put(objout, pSockets.get(processSocket));
                        }
                        Coordinator.pCount++;
                        if(Coordinator.pCount == (con.N-1)){
                            
                            for(Integer i : pList.values()){
                                Register sendMsg = new Register("assigned pid");
                                sendMsg.setpid(i);
                                sendTo(i, sendMsg);
                            }
                            
                            Request commitRequest = new Request(pid);
                            commitRequest.settext("Commit Request");
                            sendAll(commitRequest);
                            t1 = System.currentTimeMillis();
                        }
                    }
                    
                    if(recvMsg instanceof Ack){
                        print("Received: "+recvMsg);
                        if(((Ack)recvMsg).gettext().equalsIgnoreCase("Agree")){
                            agreedCount++;
                            if(agreedCount == (con.N-1)){
                                t2 = System.currentTimeMillis();
                                if(Math.abs(t1-t2) >= 10000){
                                    print("Coordinator timed out.");
                                    Coordinator.coordinatorSocket.close();
                                    Abort abortMsg = new Abort(pid);
                                    abortMsg.settext("Abort");
                                    sendAll(abortMsg);
                                    System.exit(0);
                                }
                                Prepare prepareCommit = new Prepare(pid);
                                prepareCommit.settext("Prepare to commit");
                                sendAll(prepareCommit);
                            }
                        }
                        
                        if(((Ack)recvMsg).gettext().equalsIgnoreCase("Ready to commit")){
                            readyCount++;
                            
                            if(readyCount == (con.N-1)){
                                print("Please enter a value to commit.");
                                Object val = sc.nextInt();
                                Commit commitMsg = new Commit(pid);
                                commitMsg.settext("Commit");
                                commitMsg.setval(val);
                                sendAll(commitMsg);
                                sc.close();
                                save_state(transaction_id, val);
                                System.exit(0);
                            }
                        }
                    }
                    
                    if(recvMsg instanceof Abort){
                        print("Received: "+recvMsg);
                        Abort abortMsg = new Abort(pid);
                        abortMsg.settext("Abort");
                        sendAll(abortMsg);
                        System.exit(0);
                    }
            }
        } catch (IOException ex) {
            Logger.getLogger(processHandler.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(processHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}