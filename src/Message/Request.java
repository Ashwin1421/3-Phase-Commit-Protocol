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
public class Request implements Serializable{
    Integer pid;
    
    public Request(Integer pid){
        this.pid = pid;
    }
    
    public Integer getpid(){
        return pid;
    }
    
    @Override
    public String toString(){
        return "Commit Request, pid="+pid;
    }
}
