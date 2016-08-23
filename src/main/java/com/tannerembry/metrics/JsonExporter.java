package com.tannerembry.metrics;

import com.google.api.services.gmail.Gmail;
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
        // Build a new authorized API client service.
        Gmail service = MailInitializer.getGmailService();

        String query = "in:inbox from:(member@paypal.com) subject:(Notification of payment received)";

        List<PaypalPurchase> allPurchases = MailInitializer.getAllPurchases(service, "me", query);

        exportMonthlyData(allPurchases);
        exportClockData(allPurchases);
        exportDayData(allPurchases);
        exportEmailData(allPurchases);
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
                if(key.equals("other") || emailDownloads.get(key) < 2)
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
}