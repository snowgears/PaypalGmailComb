package com.tannerembry.metrics;

import com.google.api.client.util.Base64;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.MessagePart;

import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
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

    public static PaypalPurchase parse(Message message) {
        PaypalPurchase purchase = null;

        try {
            //this message part is what contains all paypal information
            message.getPayload().getParts().get(0);

            purchase = parseOldFormat(message);
        } catch (NullPointerException e){

            purchase = parseNewFormat(message, false);

            //TODO delete this after debugging
//            if(purchase == null){
//                //print the date of the mis-parsed message
//                SimpleDateFormat parserSDF = new SimpleDateFormat("MMM d, yyyy HH:mm:ss zzz");
//                Date date = new Date(message.getInternalDate());
//                String strDate = parserSDF.format(date);
//
//                System.out.println("\t"+strDate);
//
//                purchase = parseNewFormat(message, true);
//                System.exit(1);
//            }
        }
        return purchase;
    }

    private static PaypalPurchase parseOldFormat(Message message) {
        Date date = new Date(message.getInternalDate());
        String buyerName = null;
        String buyerEmail = null;
        String transactionID = null;
        String spigotName = null;
        ResourceType resource = null;
        double amount = 0;

        //this message part is what contains all paypal information
        MessagePart part = message.getPayload().getParts().get(0);

        //need to decode the actual body of the message
        byte[] decoded = Base64.decodeBase64(part.getBody().toString());
        String purchase = "";
        try {
            purchase = new String(decoded, "UTF8");
        } catch (UnsupportedEncodingException e){
            return null;
        }


        String[] split = purchase.split("\\r?\\n");
        for(int i=0; i<split.length; i++){
            //System.out.println(split[i]);

            if(split[i].equals("Buyer:")) {
                buyerName = split[i + 1];
                buyerEmail = split[i + 2];
            }
            else if(split[i].startsWith("Description:Purchase Resource: ")){
                resource = ResourceType.parse(split[i]);
                spigotName = split[i].substring(split[i].indexOf("(")+1, split[i].indexOf(")"));

                String s = split[i+1];
                if(s.startsWith("Unit price: ")){
                    String amt = s.substring(s.indexOf("$")+1, s.lastIndexOf("U")-1);
                    try{
                        amount = Double.parseDouble(amt);
                    } catch(Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            else if(split[i].contains("Transaction ID:")){

                int startTrans = split[i].indexOf("Transaction ID:")+16;
                transactionID = split[i].substring(startTrans);
            }
        }

        if(buyerName == null ||
                buyerEmail == null ||
                transactionID == null ||
                spigotName == null ||
                resource == null ||
                date == null ||
                amount == 0)
            return null;

        return new PaypalPurchase(buyerName, buyerEmail, transactionID, spigotName, date, resource, amount);
    }

    //this parser uses a decoded html string containing the entire body of the purchase email
    //after the old format of notification emails was changed, this is the only way I have figured out how to do it
    private static PaypalPurchase parseNewFormat(Message message, boolean debug){
        Date date = new Date(message.getInternalDate());
        String buyerName = null;
        String buyerEmail = null;
        String transactionID = null;
        String spigotName = null;
        ResourceType resource = null;
        double amount = 0;

        //parse html body directly
        byte[] decoded = message.getPayload().getBody().decodeData();

        if (decoded == null)
            return null;

        String purchase = "";
        try {
            purchase = new String(decoded, "UTF8");
        } catch (UnsupportedEncodingException e){
            if(debug)
                e.printStackTrace();
            return null;
        }

        int startIndex = purchase.indexOf("Transaction ID");
        if(startIndex == -1) {
            if(debug)
                System.out.println("NULL: start.");
            return null;
        }

        //cut purchase string to where the actual information starts
        purchase = purchase.substring(startIndex);

        //parse out transction ID
        int startTrans = purchase.indexOf("\n");
        int endTrans = purchase.indexOf("https")-2;
        if(startTrans < 0 || endTrans < 0) {
            if(debug)
                System.out.println("NULL: transaction.");
            return null;
        }

        transactionID = purchase.substring(startTrans+1, endTrans);

        //parse out buyer name
        int nameIndex = purchase.indexOf("<!-- EmailContentSellerBuyerDetails");

        int startName = purchase.indexOf("display:inline", nameIndex);
        int endName = purchase.indexOf("<br/>", startName);
        if(nameIndex < 0 || startName < 0 || endName < 0) {
            if(debug) {
                System.out.println("NULL: name.");

                if(nameIndex < 0)
                    System.out.println("nameIndex.");
                if(startName < 0)
                    System.out.println("startName.");
                if(endName < 0)
                    System.out.println("endName.");

                System.out.println();
                System.out.println(purchase);
            }
            return null;
        }

        buyerName = purchase.substring(startName+18, endName-1);

        //parse out buyer email
        int startEmail = purchase.indexOf("display:inline", endName);
        int endEmail = purchase.indexOf("<br/>", startEmail);
        if(startEmail < 0 || endEmail < 0) {
            if(debug)
                System.out.println("NULL: email.");
            return null;
        }

        buyerEmail = purchase.substring(startEmail+18, endEmail-1);

        //parse out resource type
        int detailsIndex = purchase.indexOf("<!-- EmailContentShippingDetails", endEmail);

        int startResource = purchase.indexOf("Purchase Resource:", detailsIndex);
        int endResource = -1;

        if(startResource < 0){
            startResource = purchase.indexOf("Purchase+Resource");
            //endResource = purchase.indexOf("<br/>", startResource);
            endResource = startResource + 200;
        }
        else {
            endResource = purchase.indexOf("</tr>", startResource);
        }

        if(detailsIndex < 0 || startResource < 0 || endResource < 0) {
            if(debug) {
                System.out.println("NULL: resource.");
                if(detailsIndex < 0)
                    System.out.println("detailsIndex.");
                if(startResource < 0)
                    System.out.println("startResource.");
                if(endResource < 0)
                    System.out.println("endResource.");

                System.out.println();
                System.out.println(purchase);
            }
            return null;
        }

        String purchaseTitle = purchase.substring(startResource, endResource-1);

        //System.out.println("indexes: "+startResource+", "+endResource);

        resource = ResourceType.parse(purchaseTitle);

        //parse out spigot name
        spigotName = purchaseTitle.substring(purchaseTitle.lastIndexOf("(")+1, purchaseTitle.lastIndexOf(")"));

        //parse out amount
        int startAmt = purchaseTitle.indexOf("$")+1;
        int endAmt = purchaseTitle.indexOf(" ", startAmt);

        if(startAmt < 0 || endAmt < 0) {
            if(debug) {
                System.out.println("NULL: amount.");

                System.out.println(purchaseTitle);

                if(startAmt < 0)
                    System.out.println("startAmt.");
                if(endAmt < 0)
                    System.out.println("endAmt.");
            }
            return null;
        }

        String amt = purchaseTitle.substring(startAmt, endAmt);
        amount = Double.parseDouble(amt);

        if(buyerName == null ||
                buyerEmail == null ||
                transactionID == null ||
                spigotName == null ||
                resource == null ||
                date == null ||
                amount == 0)
            return null;

        return new PaypalPurchase(buyerName, buyerEmail, transactionID, spigotName, date, resource, amount);
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
