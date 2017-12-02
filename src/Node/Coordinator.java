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
import java.nio.file.Files;
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
            //Logger.getLogger(Coordinator.class.getName()).log(Level.SEVERE, null, ex);
        }
        int p = pid;
        while(p<=con.N){
            try {
                processSocket = coordinatorSocket.accept();
                p++;
                pSockets.put(processSocket, p);
                new processHandler(pid, processSocket, pSockets).start();
            } catch (IOException ex) {
                //Logger.getLogger(Coordinator.class.getName()).log(Level.SEVERE, null, ex);
            }
            
        }
    }
}


class processHandler extends Thread{
    
    static int agreedCount = 0;
    static int readyCount = 0;
    static long t1, t2, t3, t4;
    static long t5, t6;
    static int fcount = 0;
    static String stateInfo; 
    String recoveryFileName = "coordinator.log";
    
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
            //Logger.getLogger(processHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }
    /**
     * Pretty print messages within the 
     * program.
     */
    public void print(Object o){
        System.out.println("[coordinator@"+con.coordinator+"]$:"+o.toString());
    }
    /**
     * A generic send method to send
     * any kind of object to the cohorts.
     * The object can any data structure so
     * it is used to send all sorts of
     * messages.
     */
    public void sendTo(Integer i, Object o){
        for(ObjectOutputStream out : pList.keySet()){
            if(pList.get(out).equals(i)){
                try {
                    out.writeObject(o);
                    out.flush();
                } catch (IOException ex) {
                    //Logger.getLogger(processHandler.class.getName()).log(Level.SEVERE, null, ex);
                }
                print("Sent: "+o);
            }
        }
    }
    /**
     * A broadcast method to send a 
     * message to all the cohorts
     * at once.
     */
    private void sendAll(Object o){
        for(Integer i : pList.values()){
            sendTo(i, o);
        }
    }
    /**
     * A method to initiate the final commit.
     * In our case, the coordinator commits a 
     * value, so ideally everyone should commit
     * the same value.
    */
    public void commitValue(){
        print("Please enter a value to commit.");
        Object val = sc.nextInt();
        Commit commitMsg = new Commit(pid);
        commitMsg.setval(val);
        sendAll(commitMsg);
        stateInfo = "c";
        if(con.coordinatorFailState.equalsIgnoreCase(stateInfo)){
            fail(stateInfo);
        }
        sc.close();
        save_state(transaction_id, val);
        System.exit(0);
    }
    
    /**
     * Failure transition steps.
     */
    public void fail(String state){
        print("Failure at="+state);
        failureTransition();
        System.exit(1);
    }
    /**
     * Recovery mechanism.
     */
    public void recoverFromLog(){
        Abort abortMsg;
        try {
            File f = new File(recoveryFileName);
            if(f.exists() && !f.isDirectory()){
                BufferedReader bf = new BufferedReader(new FileReader(f));
                String line;
                line = bf.readLine();
                String failState;
                failState = line.split("=")[1];
                bf.close();
                f.delete();
                switch(failState){
                    case "q":
                        stateInfo = "q";
                        print("Recovery from="+failState);
                        abortMsg = new Abort(pid);
                        sendAll(abortMsg);
                        stateInfo = "a";
                        System.exit(0);
                        break;
                    case "w":
                        stateInfo = "w";
                        print("Recovery from="+failState);
                        abortMsg = new Abort(pid);
                        sendAll(abortMsg);
                        stateInfo = "a";
                        System.exit(0);
                        break;
                    case "p":
                        stateInfo = "p";
                        print("Recovery from="+failState);
                        commitValue();
                        break;
                    default : 
                        print("No recovery state found.");
                        break;
                }
            }
            
            
        } catch (FileNotFoundException ex) {
            //Logger.getLogger(processHandler.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            //Logger.getLogger(processHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    /**
     * Initiates the runtime shutdown hook thread
     * to capture the interrupt signal SIGINT 
     * which is the user interrupt to cause this 
     * program to fail. With this, its possible
     * to store the failure state and use to it
     * to do the required failure transition.
     */
    public void failureTransition(){
        PrintWriter pw = null;
        try {
            File f = new File(recoveryFileName);
            pw = new PrintWriter(new FileWriter(f));
            pw.println("fail_state="+stateInfo);
            pw.flush();
        } catch (IOException ex) {
            //Logger.getLogger(processHandler.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            pw.close();
        }
        
    }
    
    @Override
    public void run(){
        
        try {
            t5 = System.currentTimeMillis();
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
                            recoverFromLog();
                            
                            Request commitRequest = new Request(pid);
                            t6 = System.currentTimeMillis();
                            sendAll(commitRequest);
                            print("diff="+Math.abs(t6-t5));
                            stateInfo = "q";
                            if(Math.abs(t6-t5) >= con.tq1){
                                print("Coordinator timed out.");
                                Abort abortMsg = new Abort(pid);
                                sendAll(abortMsg);
                                print("Aborting...");
                                Thread.sleep(6000);
                                System.exit(0);
                            }
                            if(con.coordinatorFailState.equalsIgnoreCase(stateInfo)){
                                fail(stateInfo);
                            }
                            t1 = System.currentTimeMillis();
                        }
                    }
                    
                    if(recvMsg instanceof Ack){
                        print("Received: "+recvMsg);
                        if(((Ack)recvMsg).gettext().equalsIgnoreCase("Agree")){
                            agreedCount++;
                            if(agreedCount == (con.N-1)){
                                stateInfo = "w";
                                if(con.coordinatorFailState.equalsIgnoreCase(stateInfo)){
                                    fail(stateInfo);
                                }
                                t2 = System.currentTimeMillis();
                                if(Math.abs(t1-t2) >= con.tw1){
                                    print("Coordinator timed out.");
                                    Coordinator.coordinatorSocket.close();
                                    Abort abortMsg = new Abort(pid);
                                    sendAll(abortMsg);
                                    print("Aborting now...");
                                    System.exit(0);
                                }
                                Prepare prepareCommit = new Prepare(pid);
                                sendAll(prepareCommit);
                                stateInfo = "p";
                                if(con.coordinatorFailState.equalsIgnoreCase(stateInfo)){
                                    fail(stateInfo);
                                }
                                t3 = System.currentTimeMillis();
                            }
                        }
                        
                        if(((Ack)recvMsg).gettext().equalsIgnoreCase("Ready to commit")){
                            readyCount++;
                            
                            if(readyCount == (con.N-1)){
                                t4 = System.currentTimeMillis();
                                if(Math.abs(t4-t3)>=con.tp1){
                                    print("Coordinator timed out.");
                                    Coordinator.coordinatorSocket.close();
                                    Abort abortMsg = new Abort(pid);
                                    sendAll(abortMsg);
                                    print("Aborting now...");
                                    System.exit(0);
                                }
                                commitValue();
                            }
                        }
                    }
                    
                    if(recvMsg instanceof Abort){
                        stateInfo = "w";
                        print("Received: "+recvMsg);
                        Abort abortMsg = new Abort(pid);
                        sendAll(abortMsg);
                        stateInfo = "a";
                        System.exit(0);
                    }
            }
        } catch (IOException ex) {
            //Logger.getLogger(processHandler.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ClassNotFoundException ex) {
            //Logger.getLogger(processHandler.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InterruptedException ex) {
            //Logger.getLogger(processHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}