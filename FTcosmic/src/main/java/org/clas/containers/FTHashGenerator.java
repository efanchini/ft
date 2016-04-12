/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.clas.containers;

/**
 *
 * @author gavalian
 */
public class FTHashGenerator {
    
    
    static int[] byteShits = new int[]{48,32,16,0};
    
    public static long hashCode(int... indecies){
        long result = (long) 0;
        
        for(int loop = 0; loop < indecies.length; loop++){
            long patern = (((long) indecies[loop])&0x000000000000FFFF)<<FTHashGenerator.byteShits[loop]; 
            result = (result | patern);
        }
        return result;
    }
    
    public static int getIndex(long hashcode, int order){
        int result = (int) (hashcode>>FTHashGenerator.byteShits[order])&0x000000000000FFFF;
        return result;
    }
    
    public static String  getString(long hashcode){
        StringBuilder str = new StringBuilder();
        for(int loop = 0; loop <4; loop++){
            str.append(String.format("%5d", FTHashGenerator.getIndex(hashcode, loop)));
        }
        return str.toString();
    }
    
    public static void main(String[] args){
        
        long code = FTHashGenerator.hashCode(1,1,1,10);
        
        System.out.println(code);
        System.out.println(FTHashGenerator.getString(code));
        System.out.println(FTHashGenerator.getIndex(code, 3));
    }
    
}
