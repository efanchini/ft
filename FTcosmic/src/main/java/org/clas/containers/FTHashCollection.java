/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.clas.containers;

import java.util.Map;
import java.util.TreeMap;

/**
 *
 * @author gavalian
 */
public class FTHashCollection<T> {
    private Map<Long,T>  collection = new TreeMap<Long,T>();
    private int          indexCount = 3;
    
    public FTHashCollection(){
        
    }
    
    public FTHashCollection(int nc){
        this.indexCount = nc;
    }
    
    public Map<Long,T>  getMap(){
        return this.collection;
    }
    
    public void add(T item, int... index){
        if(index.length!=this.indexCount){
            System.out.println("HashCollection:: error can not add item, inconsistency of index count.");
            return;
        }
        long code = FTHashGenerator.hashCode(index);
        this.collection.put(code, item);
    }
    
    public int getIndexCount(){ return this.indexCount;}
    
    public boolean hasItem(int... index){
        if(index.length!=this.indexCount) return false;
        long code = FTHashGenerator.hashCode(index);
        return this.collection.containsKey(code);
    }
    
    public T getItem(int... index){
        if(index.length!=this.indexCount) return null;
        long code = FTHashGenerator.hashCode(index);
        return this.collection.get(code);
    }
    
    public void show(){        
        for(Map.Entry<Long,T>  entry : this.collection.entrySet()){
            System.out.println(String.format("[%s] : ", FTHashGenerator.getString(entry.getKey())) + entry.getValue());
        }
    }
    
//    public static void main(String[] args){
//        HashCollection<Double> coll = new HashCollection<Double>();
//        coll.add(25.0, 1,11,1);
//        coll.add(35.0, 2,12,2);
//        coll.add(45.0, 3,13,3);
//        coll.add(55.0, 4,14,4);
//        coll.show();
//    }
}
