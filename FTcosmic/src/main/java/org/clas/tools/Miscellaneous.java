/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.clas.tools;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 *
 * @author fanchini
 */
public class Miscellaneous {
    public Miscellaneous(){};
       
    //Miscellaneous.evioInOutput evioclas = new Miscellaneous.evioInOutput();
   
            
    public String datetime(){
        
        Date date = new Date();// in ms from 1970 //
        // New Foramt Year(2016) Month (01-12) day(01-31) 24h minute(00-60) seconds //
        SimpleDateFormat ft = new SimpleDateFormat ("yyyyMMddHHmmss");
        String sdate = ft.format(date);
         //System.out.println("Current Date: " + sdate);
        return sdate;
   }
     
    private String extractstring(String name){
        // Name types like yyyyyyyyyyy.yyy.yy.yyy.type  and remove (.type) ///
         String extraction="";
         if(!name.isEmpty())extraction= name.substring(0,name.lastIndexOf("."));
         return extraction;
     }
    
     
     public String extractFileName(String input, String prj, String type){
         String final_filename =null;
         String str = null;
         String date = datetime();
         
         if(input.isEmpty() && type.isEmpty())System.out.println("ERROR Filename and type not specified!!!");
         else if(input.isEmpty() && !type.isEmpty())final_filename = "./"+date + prj + type;
         else{
             str = input;
             if(!type.isEmpty()){
                 str = extractstring(input);
             }
             final_filename = str + prj + type;
             //System.out.println("Output file: "+final_filename);
         }
         return final_filename;
     }
     
    public String extractRunN(String name){
        // Name types like yyyyyyyyyyy.yyy.yy.yyy.type  and remove (.type) ///
         String extraction="";
         if(!name.isEmpty()){
             extraction= name.substring(0,name.lastIndexOf("."));
             extraction= name.substring(extraction.lastIndexOf("/")+1, extraction.length());
         }
         return extraction;
     }
        
}
