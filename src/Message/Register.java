/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Message;

import java.io.Serializable;

/**
 *
 * @author Ashwin
 */
public class Register implements Serializable{
    String text;
    Integer pid;
    
    public Register(String text){
        this.text = text;
    }
    
    public void setpid(Integer pid){
        this.pid = pid;
    }
    
    public int getpid(){
        return pid;
    }
    
    @Override
    public String toString(){
        if( pid==null){
            return "Text="+text;
        }else{
            return "Text="+text+", pid="+pid;
        }
    }
}
