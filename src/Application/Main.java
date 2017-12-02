/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Application;

import Node.Coordinator;
import Node.Process;
import Utils.Constants;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.SocketException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Ashwin
 */
public class Main {
    public static void main(String[] args){
        
        Constants con = new Constants();
        String option;
        if(args.length == 1){
            option = args[0];
            if(option.equalsIgnoreCase("-c")){
                Coordinator c = new Coordinator(4464,1);
                c.start();
            }
            
        }else{
            Process p = new Process(con.coordinator, 4464);
            int pollLimit = 10;
            while(pollLimit>0){
                try {
                    p.connect();
                } catch (SocketException ex){
                    p.reconnect();
                } catch (IOException ex) {
                    //Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
                }
                pollLimit--;
            }
        }    
    }
}
