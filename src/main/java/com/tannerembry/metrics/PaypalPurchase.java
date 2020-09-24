package com.tannerembry.metrics;

import org.json.JSONObject;
import java.util.Date;

public class PaypalPurchase implements Comparable<PaypalPurchase>{

    private String buyerName;
    private String buyerEmail;
    private String transactionID;
    private String spigotName;
    private ResourceType resource;
    private Date purchaseDate;
    private double amount;

    private PaypalPurchase(String buyerName, String buyerEmail, String transactionID, String spigotName, Date date, ResourceType resource, double amount){
        this.buyerName = buyerName;
        this.buyerEmail = buyerEmail;
        this.transactionID = transactionID;
        this.spigotName = spigotName;
        this.purchaseDate = date;
        this.resource = resource;
        this.amount = amount;
    }

    public String getBuyerName(){
        return buyerName;
    }

    public String getSpigotName(){
        return spigotName;
    }

    public String getBuyerEmail(){
        return buyerEmail;
    }

    public String getTransactionID(){
        return transactionID;
    }

    public ResourceType getResource(){
        return resource;
    }

    public double getAmount(){
        return amount;
    }

    public static PaypalPurchase parse(JSONObject jsonPurchase) {
        PaypalPurchase purchase = null;

        //TODO pull variables from the jsonPurchase object and put into here

        return purchase;
    }

    public Date getDate(){
        return purchaseDate;
    }

    public enum ResourceType {
        SHOP, MACHINES, ARATHI_BASIN;

        public static ResourceType parse(String s){
            if(s.contains("Shop"))
                return SHOP;
            if(s.contains("Machines")){
                return MACHINES;
            }
            if(s.contains("Arathi")){
                return ARATHI_BASIN;
            }
            return null;
        }
    }

    @Override
    public String toString(){
        return ("PURCHASE: "+
                "\n\t Name: "+buyerName+
                "\n\t Spigot Name: "+spigotName+
                "\n\t Email: "+buyerEmail+
                "\n\t ID: "+transactionID+
                "\n\t Date: "+purchaseDate.toString()+
                "\n\t Date: "+purchaseDate.toString()+
                "\n\t Resource: "+resource.toString()+
                "\n\t Price: "+amount);
    }

    public int compareTo(PaypalPurchase purchase) {
        return this.getDate().compareTo(purchase.getDate());
    }
}
