/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.clas.containers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 *
 * @author gavalian
 */
public class FTTableRow {
    
    private List<Number> entries = new ArrayList<Number>();
    
    public FTTableRow(){
        
    }
    
    public void set(Number... items){
        this.entries.clear();
        this.entries.addAll(Arrays.asList(items));
    }
    
    public void set(double[] values){
        this.entries.clear();
        for(double v : values){
            this.entries.add(v);
        }
    }
    
    public void set(int[] values){
        this.entries.clear();
        for(int v : values){
            this.entries.add(v);
        }
    }
    
    public void setAt(int index, Number value){
        this.entries.set(index, value);
    }
    
    public void add(Number num){
        this.entries.add(num);
    }
    
    public void show(){
        for(Number num : this.entries){
            System.out.println(num + "  " + num.getClass().getName());
        }
    }
    
    public int  getSize(){
        return this.entries.size();
    }
    
    public Number get(int index){
        return this.entries.get(index);
    }
    
    @Override
    public String toString(){
        StringBuilder str = new StringBuilder();
        for(Number num : this.entries){
            str.append(" / ");
            str.append(num);
        }
        return str.toString();
    }
    
    public static void main(String[] args){
        FTTableRow row = new FTTableRow();
        
        row.set(45670,3.5,6.7,7.5);
        
        row.show();
    }
}
