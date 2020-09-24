package com.tannerembry.metrics;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * This class gets an initialized mail instance and then exports the different json files.
 */
public class JsonExporter {

    private static File exportDirectory;

    public static void main(String[] args) throws IOException {
        try {
            exportDirectory = new File(args[0]);
            if(!exportDirectory.exists())
                exportDirectory = new File(System.getProperty("user.dir"));
        } catch(Exception e){
            exportDirectory = new File(System.getProperty("user.dir"));
        }

        long startTime = System.currentTimeMillis();

        PaypalInitializer paypalInitializer = new PaypalInitializer();
        List<PaypalPurchase> allPurchases = paypalInitializer.getAllPurchases();

        //Since PayPal changed the way that purchase notifications were sent and formatted, this covers the new way that
        //String query2 = "in:inbox from:(service@paypal.com) subject:(payment received)";

        //List<PaypalPurchase> allPurchases2 = MailInitializer.getAllPurchases(service, "me", query2);

        //System.out.println(allPurchases.size());
//        int badCount = 0;
//        int goodCount = 0;
//        for(PaypalPurchase purchase : allPurchases){
//            if(purchase != null) {
//                //System.out.println(purchase.toString());
//                goodCount++;
//            }
//            else
//                badCount++;
//        }
        //System.out.println("Total null parses: "+badCount);
        //System.out.println("Total okay (but not necessarily correct) parses: "+goodCount);

        //allPurchases.addAll(allPurchases2);
        Collections.sort(allPurchases);

        long endTime = System.currentTimeMillis();
        long totalSeconds = (endTime - startTime) / 1000;
        double totalMinutes = totalSeconds / 60;
        System.out.println("Compiled all PayPal receipts (found "+ allPurchases.size() +" in total). Job finished in "+(int)totalSeconds+" seconds.");

        double totalMoney = 0;
        for(PaypalPurchase purchase : allPurchases){
            totalMoney += purchase.getAmount();
        }

        System.out.println("Cumulative income from all purchases: $"+totalMoney);

        exportMonthlyData(allPurchases);
        exportClockData(allPurchases);
        exportDayData(allPurchases);
        exportEmailData(allPurchases);
        //exportDaysByMonthData(allPurchases);
        exportScatterData(allPurchases);
    }

    private static void exportMonthlyData(List<PaypalPurchase> purchases) {
        try {
            File jsonFile = new File(exportDirectory, "purchases_monthly.json");
            if (!jsonFile.exists())
                jsonFile.createNewFile();

            //format JSON as shown in this example: http://c3js.org/samples/timeseries.html

            JSONObject json = new JSONObject();

            JSONArray dateColumn = new JSONArray();
            JSONArray shopColumn = new JSONArray();
            JSONArray machinesColumn = new JSONArray();
            JSONArray arathiColumn = new JSONArray();

            //aggregate all purchases by month and count total
            HashMap<String, Integer> shopDownloads = new HashMap<String, Integer>();
            HashMap<String, Integer> machinesDownloads = new HashMap<String, Integer>();
            HashMap<String, Integer> arathiDownloads = new HashMap<String, Integer>();

            for(PaypalPurchase purchase : purchases){
                DateFormat targetFormat = new SimpleDateFormat("MM-yyyy");
                String formattedDate = targetFormat.format(purchase.getDate());

                if(!dateColumn.contains(formattedDate))
                    dateColumn.add(formattedDate);

                switch (purchase.getResource()){
                    case SHOP:
                        if (shopDownloads.containsKey(formattedDate)) {
                            int downloads = shopDownloads.get(formattedDate);
                            shopDownloads.put(formattedDate, downloads+1);
                        } else {
                            shopDownloads.put(formattedDate, 1);
                        }
                        break;
                    case MACHINES:
                        if (machinesDownloads.containsKey(formattedDate)) {
                            int downloads = machinesDownloads.get(formattedDate);
                            machinesDownloads.put(formattedDate, downloads+1);
                        } else {
                            machinesDownloads.put(formattedDate, 1);
                        }
                        break;
                    case ARATHI_BASIN:
                        if (arathiDownloads.containsKey(formattedDate)) {
                            int downloads = arathiDownloads.get(formattedDate);
                            arathiDownloads.put(formattedDate, downloads+1);
                        } else {
                            arathiDownloads.put(formattedDate, 1);
                        }
                        break;
                }
            }

            ListIterator<String> dateIterator = dateColumn.listIterator();

            while(dateIterator.hasNext()){
                String date = dateIterator.next();

                if(shopDownloads.containsKey(date))
                    shopColumn.add(shopDownloads.get(date));
                else
                    shopColumn.add(null);

                if(machinesDownloads.containsKey(date))
                    machinesColumn.add(machinesDownloads.get(date));
                else
                    machinesColumn.add(null);

                if(arathiDownloads.containsKey(date))
                    arathiColumn.add(arathiDownloads.get(date));
                else
                    arathiColumn.add(null);
            }

            json.put("x", dateColumn);
            json.put("Shop", shopColumn);
            json.put("Machines", machinesColumn);
            json.put("Arathi Basin", arathiColumn);

            FileWriter file = new FileWriter(jsonFile);
            file.write(json.toJSONString());
            file.flush();
            file.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void exportClockData(List<PaypalPurchase> purchases) {
        try {
            File jsonFile = new File(exportDirectory, "purchases_clock.json");
            if (!jsonFile.exists())
                jsonFile.createNewFile();

            //format JSON as shown in this example: http://c3js.org/samples/timeseries.html

            JSONObject json = new JSONObject();

            JSONArray morningColumn = new JSONArray();
            JSONArray afternoonColumn = new JSONArray();
            JSONArray eveningColumn = new JSONArray();
            JSONArray nightColumn = new JSONArray();

            //aggregate all purchases by time of day purchased
            //Morning, Afternoon, Evening, Night
            HashMap<String, Integer> timeDownloads = new HashMap<String, Integer>();

            for(PaypalPurchase purchase : purchases){
                int hour = purchase.getDate().getHours();
                String time = "";
                if(hour < 6)
                    time = "Night";
                else if(hour < 12)
                    time = "Morning";
                else if(hour < 18)
                    time = "Afternoon";
                else
                    time = "Evening";

                if(timeDownloads.containsKey(time)){
                    int downloads = timeDownloads.get(time);
                    timeDownloads.put(time, downloads+1);
                }
                else{
                    timeDownloads.put(time, 1);
                }
            }

            for(String key : timeDownloads.keySet()){
                if(key.equals("Morning")){
                    morningColumn.add(timeDownloads.get(key));
                }
                else if(key.equals("Afternoon")){
                    afternoonColumn.add(timeDownloads.get(key));
                }
                else if(key.equals("Evening")){
                    eveningColumn.add(timeDownloads.get(key));
                }
                else{
                    nightColumn.add(timeDownloads.get(key));
                }
            }

            json.put("Morning", morningColumn);
            json.put("Afternoon", afternoonColumn);
            json.put("Evening", eveningColumn);
            json.put("Night", nightColumn);

            FileWriter file = new FileWriter(jsonFile);
            file.write(json.toJSONString());
            file.flush();
            file.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void exportDayData(List<PaypalPurchase> purchases) {
        try {
            File jsonFile = new File(exportDirectory, "purchases_days.json");
            if (!jsonFile.exists())
                jsonFile.createNewFile();

            //format JSON as shown in this example: http://c3js.org/samples/timeseries.html

            JSONObject json = new JSONObject();

            JSONArray dayColumn = new JSONArray();
            String[] days = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};
            dayColumn.addAll(new ArrayList<String>(Arrays.asList(days)));
            JSONArray shopColumn = new JSONArray();
            JSONArray machinesColumn = new JSONArray();
            JSONArray arathiColumn = new JSONArray();

            //aggregate all purchases by day and count total
            HashMap<String, Integer> shopDownloads = new HashMap<String, Integer>();
            HashMap<String, Integer> machinesDownloads = new HashMap<String, Integer>();
            HashMap<String, Integer> arathiDownloads = new HashMap<String, Integer>();

            for(PaypalPurchase purchase : purchases){
                String day = getDay(dayColumn, purchase.getDate().toString().substring(0, 3));

                switch (purchase.getResource()){
                    case SHOP:
                        if (shopDownloads.containsKey(day)) {
                            int downloads = shopDownloads.get(day);
                            shopDownloads.put(day, downloads+1);
                        } else {
                            shopDownloads.put(day, 1);
                        }
                        break;
                    case MACHINES:
                        if (machinesDownloads.containsKey(day)) {
                            int downloads = machinesDownloads.get(day);
                            machinesDownloads.put(day, downloads+1);
                        } else {
                            machinesDownloads.put(day, 1);
                        }
                        break;
                    case ARATHI_BASIN:
                        if (arathiDownloads.containsKey(day)) {
                            int downloads = arathiDownloads.get(day);
                            arathiDownloads.put(day, downloads+1);
                        } else {
                            arathiDownloads.put(day, 1);
                        }
                        break;
                }
            }

            ListIterator<String> dayIterator = dayColumn.listIterator();

            while(dayIterator.hasNext()){
                String day = dayIterator.next();

                if(shopDownloads.containsKey(day))
                    shopColumn.add(shopDownloads.get(day));
                else
                    shopColumn.add(null);

                if(machinesDownloads.containsKey(day))
                    machinesColumn.add(machinesDownloads.get(day));
                else
                    machinesColumn.add(null);

                if(arathiDownloads.containsKey(day))
                    arathiColumn.add(arathiDownloads.get(day));
                else
                    arathiColumn.add(null);
            }

            json.put("x", dayColumn);
            json.put("Shop", shopColumn);
            json.put("Machines", machinesColumn);
            json.put("Arathi Basin", arathiColumn);

            FileWriter file = new FileWriter(jsonFile);
            file.write(json.toJSONString());
            file.flush();
            file.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String getDay(JSONArray days, String dayPart){
        Iterator<String> daysIterator = days.iterator();
        while(daysIterator.hasNext()){
            String fullDay = daysIterator.next();
            if(fullDay.contains(dayPart))
                return fullDay;
        }
        return null;
    }

    private static void exportEmailData(List<PaypalPurchase> purchases) {
        try {
            File jsonFile = new File(exportDirectory, "purchases_emails.json");
            if (!jsonFile.exists())
                jsonFile.createNewFile();

            //format JSON as shown in this example: http://c3js.org/samples/timeseries.html

            JSONObject json = new JSONObject();

            //aggregate all purchases by email service purchased on
            HashMap<String, Integer> emailDownloads = new HashMap<String, Integer>();

            for(PaypalPurchase purchase : purchases){
                String email = "other";
                try {
                    int atIndex = purchase.getBuyerEmail().indexOf('@')+1;
                    int periodIndex = purchase.getBuyerEmail().indexOf('.', atIndex+1);
                    email = purchase.getBuyerEmail().substring(atIndex, periodIndex);
                } catch(Exception e) {}

                if(emailDownloads.containsKey(email)){
                    int downloads = emailDownloads.get(email);
                    emailDownloads.put(email, downloads+1);
                }
                else{
                    emailDownloads.put(email, 1);
                }
            }

            JSONArray otherColumn = new JSONArray();
            otherColumn.add("other");

            for(String key : emailDownloads.keySet()){
                if(key.equals("other") || emailDownloads.get(key) < 3)
                    otherColumn.add(emailDownloads.get(key));
                else {
                    JSONArray emailColumn = new JSONArray();
                    emailColumn.add(emailDownloads.get(key));
                    json.put(key, emailColumn);
                }
            }
            json.put("other", otherColumn);

            FileWriter file = new FileWriter(jsonFile);
            file.write(json.toJSONString());
            file.flush();
            file.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void exportDaysByMonthData(List<PaypalPurchase> purchases) {
        try {
            File jsonFile = new File(exportDirectory, "purchases_day_by_month.json");
            if (!jsonFile.exists())
                jsonFile.createNewFile();

            //format JSON as shown in this example: http://c3js.org/samples/timeseries.html

            JSONObject json = new JSONObject();

            JSONArray dateColumn = new JSONArray();
            JSONArray monColumn = new JSONArray();
            JSONArray tuesColumn = new JSONArray();
            JSONArray wedColumn = new JSONArray();
            JSONArray thursColumn = new JSONArray();
            JSONArray friColumn = new JSONArray();
            JSONArray satColumn = new JSONArray();
            JSONArray sunColumn = new JSONArray();

            //aggregate all purchases by month and count total
            HashMap<String, Integer> monDownloads = new HashMap<String, Integer>();
            HashMap<String, Integer> tuesDownloads = new HashMap<String, Integer>();
            HashMap<String, Integer> wedDownloads = new HashMap<String, Integer>();
            HashMap<String, Integer> thursDownloads = new HashMap<String, Integer>();
            HashMap<String, Integer> friDownloads = new HashMap<String, Integer>();
            HashMap<String, Integer> satDownloads = new HashMap<String, Integer>();
            HashMap<String, Integer> sunDownloads = new HashMap<String, Integer>();

            for(PaypalPurchase purchase : purchases){
                DateFormat targetFormat = new SimpleDateFormat("MM-yyyy");
                String formattedDate = targetFormat.format(purchase.getDate());

                if(!dateColumn.contains(formattedDate))
                    dateColumn.add(formattedDate);

                Calendar c = Calendar.getInstance();
                c.setTime(purchase.getDate());
                int dayOfWeek = c.get(Calendar.DAY_OF_WEEK);

                switch (dayOfWeek){
                    case 1:
                        incrementDownloads(sunDownloads, formattedDate);
                        break;
                    case 2:
                        incrementDownloads(monDownloads, formattedDate);
                        break;
                    case 3:
                        incrementDownloads(tuesDownloads, formattedDate);
                        break;
                    case 4:
                        incrementDownloads(wedDownloads, formattedDate);
                        break;
                    case 5:
                        incrementDownloads(thursDownloads, formattedDate);
                        break;
                    case 6:
                        incrementDownloads(friDownloads, formattedDate);
                        break;
                    case 7:
                        incrementDownloads(satDownloads, formattedDate);
                        break;
                }
            }

            ListIterator<String> dateIterator = dateColumn.listIterator();

            while(dateIterator.hasNext()){
                String date = dateIterator.next();

                if(monDownloads.containsKey(date))
                    monColumn.add(monDownloads.get(date));
                else
                    monColumn.add(null);

                if(tuesDownloads.containsKey(date))
                    tuesColumn.add(tuesDownloads.get(date));
                else
                    tuesColumn.add(null);

                if(wedDownloads.containsKey(date))
                    wedColumn.add(wedDownloads.get(date));
                else
                    wedColumn.add(null);

                if(thursDownloads.containsKey(date))
                    thursColumn.add(thursDownloads.get(date));
                else
                    thursColumn.add(null);

                if(friDownloads.containsKey(date))
                    friColumn.add(friDownloads.get(date));
                else
                    friColumn.add(null);

                if(satDownloads.containsKey(date))
                    satColumn.add(satDownloads.get(date));
                else
                    satColumn.add(null);

                if(sunDownloads.containsKey(date))
                    sunColumn.add(sunDownloads.get(date));
                else
                    sunColumn.add(null);
            }

            json.put("x", dateColumn);
            json.put("Monday", monColumn);
            json.put("Tuesday", tuesColumn);
            json.put("Wednesday", wedColumn);
            json.put("Thursday", thursColumn);
            json.put("Friday", friColumn);
            json.put("Saturday", satColumn);
            json.put("Sunday", sunColumn);

            FileWriter file = new FileWriter(jsonFile);
            file.write(json.toJSONString());
            file.flush();
            file.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void incrementDownloads(HashMap<String, Integer> dayDownloads, String formattedDate) {
        if (dayDownloads.containsKey(formattedDate)) {
            int downloads = dayDownloads.get(formattedDate);
            dayDownloads.put(formattedDate, downloads+1);
        } else {
            dayDownloads.put(formattedDate, 1);
        }
    }

    private static void exportScatterData(List<PaypalPurchase> purchases) {
        try {
            File jsonFile = new File(exportDirectory, "purchases_all.json");
            if (!jsonFile.exists())
                jsonFile.createNewFile();

            //format JSON as shown in this example: http://c3js.org/samples/timeseries.html

            JSONObject json = new JSONObject();

            JSONArray shopXColumn = new JSONArray();
            JSONArray shopColumn = new JSONArray();
            JSONArray machinesColumn = new JSONArray();
            JSONArray machinesXColumn = new JSONArray();
            JSONArray arathiColumn = new JSONArray();
            JSONArray arathiXColumn = new JSONArray();

            for(PaypalPurchase purchase : purchases){
                DateFormat targetFormat = new SimpleDateFormat("MM-dd-yyyy");
                String formattedDate = targetFormat.format(purchase.getDate());

                Calendar c = Calendar.getInstance();
                c.setTime(purchase.getDate());
                int hour = c.get(Calendar.HOUR_OF_DAY);
                int minute = c.get(Calendar.MINUTE);
                double plotMinute = minute / 60;
                double plotTime = hour + plotMinute;

                switch (purchase.getResource()){
                    case SHOP:
                        shopColumn.add(plotTime);
                        shopXColumn.add(formattedDate);
                        break;
                    case MACHINES:
                        machinesColumn.add(plotTime);
                        machinesXColumn.add(formattedDate);
                        break;
                    case ARATHI_BASIN:
                        arathiColumn.add(plotTime);
                        arathiXColumn.add(formattedDate);
                        break;
                }
            }

            json.put("shop", shopColumn);
            json.put("shop_x", shopXColumn);
            json.put("machines", machinesColumn);
            json.put("machines_x", machinesXColumn);
            json.put("arathi", arathiColumn);
            json.put("arathi_x", arathiXColumn);

            FileWriter file = new FileWriter(jsonFile);
            file.write(json.toJSONString());
            file.flush();
            file.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
