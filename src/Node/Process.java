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
import static Node.processHandler.fcount;
import Utils.Constants;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
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
    Constants con = new Constants();
    String file_name;
    File file;
    String recoveryFileName = "cohort.log";
    
    static long t1,t2,t3,t4,t5,t6;
    static String stateInfo;
    
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
        print("Attempting to reconnect to coordinator...");
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
            //Logger.getLogger(Process.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    /**
     * Pretty print all kinds of messages.
     */
    private void print(Object o){
        System.out.println("[process@"+processSocket.getLocalSocketAddress()+"]$:"+o.toString());
    }
    
    /**
     * Generic send method to send any kind of
     * message or object. 
     * Allows to send other values or 
     * data structures.
     */
    private void send(ObjectOutputStream out, Object o){
        try {
            out.writeObject(o);
            out.flush();
            print("Sent: "+o);
        } catch (IOException ex) {
            //Logger.getLogger(Process.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }
    /**
     * Method to initiate failure transition
     * depending the state at which it has
     * failed.
     */
    public void fail(String state){
        print("Failure at="+state);
        failureTransition();
        System.exit(1);
    }
    /**
     * Recovery mechanism to recover from 
     * a failed state and then perform
     * failure transition operation
     * as per the 3-Phase commit protocol.
     */
    public void recoverFromLog(ObjectOutputStream objOut){
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
                        print("Aborting...");
                        abortMsg = new Abort(pid);
                        send(objOut, abortMsg);
                        stateInfo = "a";
                        System.exit(0);
                        break;
                    case "w":
                        stateInfo = "w";
                        print("Recovery from="+failState);
                        print("Aborting...");
                        abortMsg = new Abort(pid);
                        send(objOut, abortMsg);
                        stateInfo = "a";
                        System.exit(0);
                        break;
                    case "p":
                        stateInfo = "p";
                        print("Recovery from="+failState);
                        //commitValue();
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
     * Actions performed during failure 
     * transition for cohort.
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
    /**
     * Method invoked to start the process 
     * program.
     */
    public void start(){
        try {
            print("Connection established with "+hostName+"@"+port+".");
            t1 = System.currentTimeMillis();
            ObjectOutputStream objout = new ObjectOutputStream(processSocket.getOutputStream());
            ObjectInputStream objin = new ObjectInputStream(processSocket.getInputStream());
            
            Register registerMsg = new Register("Register");
            send(objout, registerMsg);
            t2 = System.currentTimeMillis();
            print("diff="+Math.abs(t2-t1));
            /**
             * Timeout transition during state "q"
             * for cohort "i".
             */
            if(Math.abs(t2-t1) >= con.tqi){
                print("Cohort timed out.");
                Abort abortMsg = new Abort(pid);
                send(objout, abortMsg);
                print("Aborting...");
                System.exit(0);
            }
            recoverFromLog(objout);
            while(true){
                if(processSocket.isConnected()){
                    Object recvMsg = objin.readObject();
                    
                    if(recvMsg instanceof Register){
                        print("Received: "+recvMsg);
                        pid = ((Register) recvMsg).getpid();
                        this.file_name = "state_"+pid+".out";
                        stateInfo = "q";
                        /**
                         * Failure transition during state "q".
                         */
                        if(con.cohortFailState.equalsIgnoreCase(stateInfo) && (con.failingCohort == pid) ){
                            fail(stateInfo);
                        }
                    }
                    
                    if(recvMsg instanceof Request){
                        print("Received: "+recvMsg);
                        t3 = System.currentTimeMillis();
                        print("Do you wish to commit ? (y/n)");
                        String text = sc.nextLine();
                        Ack sendAck = new Ack(pid);
                        if(text.equalsIgnoreCase("y")){
                            sendAck.settext("Agree");
                            send(objout, sendAck);
                            t4 = System.currentTimeMillis();
                            print("diff="+Math.abs(t4-t3));
                            if(Math.abs(t4-t3) >= con.twi){
                                print("Cohort timed out.");
                                Abort abortMsg = new Abort(pid);
                                send(objout, abortMsg);
                                print("Aborting...");
                                System.exit(0);
                            }
                            stateInfo = "w";
                            /**
                            * Failure transition during state "w".
                            */
                            if(con.cohortFailState.equalsIgnoreCase(stateInfo) && (con.failingCohort == pid) ){
                                fail(stateInfo);
                            }
                        }else{
                            Abort abortMsg = new Abort(pid);
                            send(objout, abortMsg);
                            t4 = System.currentTimeMillis();
                            print("diff="+Math.abs(t4-t3));
                            stateInfo = "a";
                            if(Math.abs(t4-t3) >= con.twi){
                                print("Cohort timed out.");
                                //abortMsg = new Abort(pid);
                                //send(objout, abortMsg);
                                print("Aborting...");
                                System.exit(0);
                            }
                            /**
                            * Failure transition during state "a".
                            */
                            if(con.cohortFailState.equalsIgnoreCase(stateInfo) && (con.failingCohort == pid) ){
                                fail(stateInfo);
                            }
                            System.exit(0);
                        }
                    }
                    
                    if(recvMsg instanceof Prepare){
                        print("Received: "+recvMsg);
                        Ack sendAck = new Ack(pid);
                        sendAck.settext("Ready to commit");
                        send(objout, sendAck);
                        t5 = System.currentTimeMillis();
                        stateInfo = "p";
                        if(con.cohortFailState.equalsIgnoreCase(stateInfo) && (con.failingCohort == pid) ){
                            fail(stateInfo);
                        }
                    }
                    
                    if(recvMsg instanceof Commit){
                        t6 = System.currentTimeMillis();
                        print("Received: "+recvMsg);
                        print("diff="+Math.abs(t6-t5));
                        if(Math.abs(t6-t5) >= con.tpi){
                            print("Cohort timed out.");
                            print("received value="+((Commit) recvMsg).getval());
                            save_state(transaction_id, ((Commit) recvMsg).getval());
                            stateInfo = "c";
                            if(con.cohortFailState.equalsIgnoreCase(stateInfo) && (con.failingCohort == pid) ){
                                fail(stateInfo);
                            }
                            System.exit(0);
                        }
                        print("Received value="+((Commit) recvMsg).getval());
                        save_state(transaction_id, ((Commit) recvMsg).getval());
                        stateInfo = "c";
                        if(con.cohortFailState.equalsIgnoreCase(stateInfo) && (con.failingCohort == pid) ){
                            fail(stateInfo);
                        }
                        System.exit(0);
                    }
                    
                    if(recvMsg instanceof Abort){
                        print("Received: "+recvMsg);
                        print("Aborting...");
                        stateInfo = "a";
                        if(con.cohortFailState.equalsIgnoreCase(stateInfo) && (con.failingCohort == pid) ){
                            fail(stateInfo);
                        }
                        System.exit(0);
                    }
                }else{
                    print("Connection lost.");
                }
            }
        } catch (IOException ex) {
            //Logger.getLogger(Process.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ClassNotFoundException ex) {
            //Logger.getLogger(Process.class.getName()).log(Level.SEVERE, null, ex);
        } 
        
    }
}
