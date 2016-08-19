package com.tannerembry.metrics;

import java.text.SimpleDateFormat;
import java.util.Date;

public class PaypalPurchase {

    private String buyerName;
    private String buyerEmail;
    private ResourceType resource;
    private Date purchaseDate;
    private double amount;

    private PaypalPurchase(String buyerName, String buyerEmail, Date date, ResourceType resource, double amount){
        this.buyerName = buyerName;
        this.buyerEmail = buyerEmail;
        this.purchaseDate = date;
        this.resource = resource;
        this.amount = amount;
    }

    public String getBuyerName(){
        return buyerName;
    }

    public String getBuyerEmail(){
        return buyerEmail;
    }

    public ResourceType getResource(){
        return resource;
    }

    public double getAmount(){
        return amount;
    }

    public static PaypalPurchase parse(String purchase){
        String buyerName = null;
        String buyerEmail = null;
        ResourceType resource = null;
        Date date = null;
        double amount = 0;

        String[] split = purchase.split("\\r?\\n");
        for(int i=0; i<split.length; i++){

            if(split[i].equals("Buyer:")) {
                buyerName = split[i + 1];
                buyerEmail = split[i + 2];
            }
            else if(split[i].startsWith("Description:Purchase Resource: ")){
                resource = ResourceType.parse(split[i]);

                String s = split[i+1];
                if(s.startsWith("Unit price: ")){
                    String amt = s.substring(s.indexOf("$")+1, s.lastIndexOf("U")-1);
                    try{
                        amount = Double.parseDouble(amt);
                    } catch(Exception e) {}
                }
            }
            else if(split[i].contains("Transaction ID:")){
                SimpleDateFormat parserSDF = new SimpleDateFormat("MMM d, yyyy HH:mm:ss zzz");
                String s = split[i];
                String d = s.substring(s.indexOf("Z")+1, s.indexOf("|")-1);
                try {
                    date = parserSDF.parse(d);
                } catch(Exception e) {e.printStackTrace();}
            }
        }

        return new PaypalPurchase(buyerName, buyerEmail, date, resource, amount);
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
        return ("Name: "+buyerName+",\t Email: "+buyerEmail+",\t Date: "+purchaseDate.toString()+",\t Resource: "+resource.toString()+",\t Price: "+amount);
    }
}
