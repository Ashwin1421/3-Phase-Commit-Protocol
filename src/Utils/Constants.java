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
    }
    
}
