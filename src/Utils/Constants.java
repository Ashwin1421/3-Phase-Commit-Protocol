/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Ashwin
 */
public class Constants {
    Properties prop = new Properties();
    InputStream configFileInputStream = null;
    public String coordinator = null;
    public Integer N;
    public long tq1, tw1, tp1;
    public long tqi, twi, tpi;
    public String coordinatorFailState;
    public String cohortFailState;
    public int failingCohort;

    public Constants(){
        configFileInputStream = getClass().getClassLoader().getResourceAsStream("config.properties");
        if(configFileInputStream!=null){
            try {
                prop.load(configFileInputStream);
            } catch (IOException ex) {
                Logger.getLogger(Constants.class.getName()).log(Level.SEVERE, null, ex);
            }
        }else{
            System.out.println("Error loading in config file!");
        }
        
        this.coordinator = prop.getProperty("coordinator");
        this.N = Integer.parseInt(prop.getProperty("n"));
        this.tq1 = Long.parseLong(prop.getProperty("tq1"));
        this.tw1 = Long.parseLong(prop.getProperty("tw1"));
        this.tp1 = Long.parseLong(prop.getProperty("tp1"));
        this.tqi = Long.parseLong(prop.getProperty("tqi"));
        this.twi = Long.parseLong(prop.getProperty("twi"));
        this.tpi = Long.parseLong(prop.getProperty("tpi"));
        this.coordinatorFailState = prop.getProperty("f1");
        this.cohortFailState = prop.getProperty("fi");
        this.failingCohort = Integer.parseInt(prop.getProperty("i"));
    }
    
}
