package com.tannerembry.metrics;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.util.Base64;
import com.google.api.client.util.store.FileDataStoreFactory;

import com.google.api.services.gmail.GmailScopes;
import com.google.api.services.gmail.model.*;
import com.google.api.services.gmail.Gmail;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class JsonExporter {
    /** Application name. */
    private static final String APPLICATION_NAME =
            "Gmail API Java Quickstart";

    /** Directory to store user credentials for this application. */
    private static final java.io.File DATA_STORE_DIR = new java.io.File(
            System.getProperty("user.home"), ".credentials/gmail-java-quickstart");

    /** Global instance of the {@link FileDataStoreFactory}. */
    private static FileDataStoreFactory DATA_STORE_FACTORY;

    /** Global instance of the JSON factory. */
    private static final JsonFactory JSON_FACTORY =
            JacksonFactory.getDefaultInstance();

    /** Global instance of the HTTP transport. */
    private static HttpTransport HTTP_TRANSPORT;

    /** Global instance of the scopes required by this quickstart.
     *
     * If modifying these scopes, delete your previously saved credentials
     * at ~/.credentials/gmail-java-quickstart
     */
    private static final List<String> SCOPES =
            Arrays.asList(GmailScopes.GMAIL_READONLY);

    static {
        try {
            HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
            DATA_STORE_FACTORY = new FileDataStoreFactory(DATA_STORE_DIR);
        } catch (Throwable t) {
            t.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Creates an authorized Credential object.
     * @return an authorized Credential object.
     * @throws IOException
     */
    public static Credential authorize() throws IOException {
        try {
            // Load client secrets.
            InputStream in =
                    JsonExporter.class.getResourceAsStream("/client_secret.json");
            GoogleClientSecrets clientSecrets =
                    GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

            // Build flow and trigger user authorization request.
            GoogleAuthorizationCodeFlow flow =
                    new GoogleAuthorizationCodeFlow.Builder(
                            HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                            .setDataStoreFactory(DATA_STORE_FACTORY)
                            .setAccessType("offline")
                            .build();
            Credential credential = new AuthorizationCodeInstalledApp(
                    flow, new LocalServerReceiver()).authorize("user");
            System.out.println(
                    "Credentials saved to " + DATA_STORE_DIR.getAbsolutePath());
            return credential;
        } catch(Exception e){
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Build and return an authorized Gmail client service.
     * @return an authorized Gmail client service
     * @throws IOException
     */
    public static Gmail getGmailService() throws IOException {
        Credential credential = authorize();
        return new Gmail.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    public static void main(String[] args) throws IOException {
        // Build a new authorized API client service.
        Gmail service = getGmailService();

        String query = "in:inbox from:(member@paypal.com) subject:(Notification of payment received)";

        List<PaypalPurchase> allPurchases = getAllPurchases(service, "me", query);

        exportToTimeseriesJson(allPurchases);
        exportToDonutTimeOfDay(allPurchases);
        exportToDayofWeekJson(allPurchases);
        exportToEmailTypes(allPurchases);
    }

    private static void exportToTimeseriesJson(List<PaypalPurchase> purchases) {
        try {
            File dir = new File(System.getProperty("user.dir"));
            File jsonFile = new File(dir, "purchases_dates.json");
            if (!jsonFile.exists())
                jsonFile.createNewFile();

            //format JSON as shown in this example: http://c3js.org/samples/timeseries.html

            JSONObject json = new JSONObject();
            json.put("x", "x");
            JSONArray columns = new JSONArray();

            JSONArray dateColumn = new JSONArray();
            JSONArray shopColumn = new JSONArray();
            shopColumn.add("Shop");
            JSONArray machinesColumn = new JSONArray();
            machinesColumn.add("Machines");
            JSONArray arathiColumn = new JSONArray();
            arathiColumn.add("Arathi Basin");

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
                    shopColumn.add(0);

                if(machinesDownloads.containsKey(date))
                    machinesColumn.add(machinesDownloads.get(date));
                else
                    machinesColumn.add(0);

                if(arathiDownloads.containsKey(date))
                    arathiColumn.add(arathiDownloads.get(date));
                else
                    arathiColumn.add(0);
            }

            dateColumn.add(0, "x");
            columns.add(dateColumn);
            columns.add(shopColumn);
            columns.add(machinesColumn);
            columns.add(arathiColumn);
            json.put("columns", columns);

            FileWriter file = new FileWriter(jsonFile);
            file.write(json.toJSONString());
            file.flush();
            file.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void exportToDonutTimeOfDay(List<PaypalPurchase> purchases) {
        try {
            File dir = new File(System.getProperty("user.dir"));
            File jsonFile = new File(dir, "purchases_times.json");
            if (!jsonFile.exists())
                jsonFile.createNewFile();

            //format JSON as shown in this example: http://c3js.org/samples/timeseries.html

            JSONObject json = new JSONObject();
            JSONArray columns = new JSONArray();

            JSONArray morningColumn = new JSONArray();
            morningColumn.add("Morning");
            JSONArray afternoonColumn = new JSONArray();
            afternoonColumn.add("Afternoon");
            JSONArray eveningColumn = new JSONArray();
            eveningColumn.add("Evening");
            JSONArray nightColumn = new JSONArray();
            nightColumn.add("Night");

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

            columns.add(morningColumn);
            columns.add(afternoonColumn);
            columns.add(eveningColumn);
            columns.add(nightColumn);
            json.put("columns", columns);

            FileWriter file = new FileWriter(jsonFile);
            file.write(json.toJSONString());
            file.flush();
            file.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void exportToDayofWeekJson(List<PaypalPurchase> purchases) {
        try {
            File dir = new File(System.getProperty("user.dir"));
            File jsonFile = new File(dir, "purchases_daysInWeek.json");
            if (!jsonFile.exists())
                jsonFile.createNewFile();

            //format JSON as shown in this example: http://c3js.org/samples/timeseries.html

            JSONObject json = new JSONObject();
            json.put("x", "x");
            JSONArray columns = new JSONArray();

            JSONArray dayColumn = new JSONArray();
            String[] days = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};
            dayColumn.addAll(new ArrayList<String>(Arrays.asList(days)));
            JSONArray shopColumn = new JSONArray();
            shopColumn.add("Shop");
            JSONArray machinesColumn = new JSONArray();
            machinesColumn.add("Machines");
            JSONArray arathiColumn = new JSONArray();
            arathiColumn.add("Arathi Basin");

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
                    shopColumn.add(0);

                if(machinesDownloads.containsKey(day))
                    machinesColumn.add(machinesDownloads.get(day));
                else
                    machinesColumn.add(0);

                if(arathiDownloads.containsKey(day))
                    arathiColumn.add(arathiDownloads.get(day));
                else
                    arathiColumn.add(0);
            }

            dayColumn.add(0, "x");
            columns.add(dayColumn);
            columns.add(shopColumn);
            columns.add(machinesColumn);
            columns.add(arathiColumn);
            json.put("columns", columns);

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

    private static void exportToEmailTypes(List<PaypalPurchase> purchases) {
        try {
            File dir = new File(System.getProperty("user.dir"));
            File jsonFile = new File(dir, "purchases_emails.json");
            if (!jsonFile.exists())
                jsonFile.createNewFile();

            //format JSON as shown in this example: http://c3js.org/samples/timeseries.html

            JSONObject json = new JSONObject();
            JSONArray columns = new JSONArray();

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
                    emailColumn.add(key);
                    emailColumn.add(emailDownloads.get(key));
                    columns.add(emailColumn);
                }
            }
            columns.add(otherColumn);
            json.put("columns", columns);

            FileWriter file = new FileWriter(jsonFile);
            file.write(json.toJSONString());
            file.flush();
            file.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * List all Messages of the user's mailbox matching the query.
     *
     * @param service Authorized Gmail API instance.
     * @param userId User's email address. The special value "me"
     * can be used to indicate the authenticated user.
     * @param query String used to filter the Messages listed.
     * @throws IOException
     */
    public static List<PaypalPurchase> getAllPurchases(Gmail service, String userId,
                                                          String query) throws IOException {
        List<PaypalPurchase> allPurchases = new ArrayList<PaypalPurchase>();
        ListMessagesResponse response = service.users().messages().list(userId).setQ(query).execute();

        List<Message> messages = new ArrayList<Message>();
        while (response.getMessages() != null) {
            messages.addAll(response.getMessages());
            if (response.getNextPageToken() != null) {
                String pageToken = response.getNextPageToken();
                response = service.users().messages().list(userId).setQ(query)
                        .setPageToken(pageToken).execute();
            } else {
                break;
            }
        }

        for(Message message : messages) {
            Message m = service.users().messages().get(userId, message.getId()).execute();

            //this message part is what contains all paypal information
            MessagePart part = m.getPayload().getParts().get(0);

            //need to decode the actual body of the message
            byte[] decoded = Base64.decodeBase64(part.getBody().toString());
            String actual = new String(decoded, "UTF8");

            //use the decoded string and parse out all necessary data
            PaypalPurchase purchase = PaypalPurchase.parse(actual);
            allPurchases.add(purchase);
        }

        return allPurchases;
    }

}
