/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Application;

import Node.Coordinator;
import Node.Process;
import Utils.Constants;

/**
 *
 * @author Ashwin
 */
public class Main {
    public static void main(String[] args){
            
        Constants con = new Constants();
        String option="";
        if(args.length == 1){
            option = args[0];
            if(option.equalsIgnoreCase("-c")){
                new Coordinator(4464,1).start();
            }
        }else{
            new Process(con.coordinator,4464).start();
        }    
    }
}
