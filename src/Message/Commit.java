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
public class Commit implements Serializable{
    String text;
    Integer pid;
    Object val;
    
    public Commit(Integer pid){
        this.pid = pid;
    }
    
    public void settext(String text){
        this.text = text;
    }
    
    public Integer getpid(){
        return pid;
    }
    
    public void setval(Object val){
        this.val = val;
    }
    
    public Object getval() {
        return val;
    }
    
    @Override
    public String toString(){
        return "Text="+text+", pid="+pid;
    }
}
