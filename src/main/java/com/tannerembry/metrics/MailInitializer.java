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

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class
MailInitializer {
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

    private static int CURRENT_YEAR = 0;
    private static int CURRENT_MONTH = 0;

    /**
     * Creates an authorized Credential object.
     * @return an authorized Credential object.
     * @throws IOException
     */
    public static Credential authorize() throws IOException {
        try {
            // Load client secrets.
            InputStream in =
                    MailInitializer.class.getResourceAsStream("/client_secret.json");
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

    /**
     * List all Messages of the user's mailbox matching the query.
     *
     * @param service Authorized Gmail API instance.
     * @param userId User's email address. The special value "me"
     * can be used to indicate the authenticated user.
     * @param query String used to filter the Messages listed.
     * @throws IOException
     */
    public static List<PaypalPurchase> getAllPurchases(Gmail service, String userId, String query) throws IOException {

        Calendar c = Calendar.getInstance();
        CURRENT_YEAR = c.get(Calendar.YEAR);
        CURRENT_MONTH = c.get(Calendar.MONTH);

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

        PaypalPurchase purchase;
        SimpleDateFormat parserSDF = new SimpleDateFormat("MMM d, yyyy HH:mm:ss zzz");

        System.out.println("Number of messages: "+messages.size());

        //System.out.println("\n\nNULL PURCHASE MESSAGE DATES:");

        for(Message message : messages) {
            Message m = service.users().messages().get(userId, message.getId()).execute();

            //use the decoded string and parse out all necessary data
            purchase = PaypalPurchase.parse(m);


            Date date = new Date(m.getInternalDate());
            String strDate = parserSDF.format(date);

            //TODO there are still 11 total misparsed emails ranging from Feb 2017 - Oct 2016 due to cut off message body for seemingly no reason
//            if(purchase == null){
//                System.out.println("\t"+strDate);
//            }

            if(purchase != null && !purchaseIsCurrentMonth(purchase)) {
                allPurchases.add(purchase);
            }
        }
        System.out.println("");

        return allPurchases;
    }

    private static boolean purchaseIsCurrentMonth(PaypalPurchase purchase){
        if(purchase != null){
            SimpleDateFormat parserSDF = new SimpleDateFormat("MMM d, yyyy HH:mm:ss zzz");

            Calendar cal = Calendar.getInstance();
            cal.setTime(purchase.getDate());
            int month = cal.get(Calendar.MONTH);
            int year = cal.get(Calendar.YEAR);

            if(month == CURRENT_MONTH && year == CURRENT_YEAR) {
                String strDate = parserSDF.format(purchase.getDate());
                System.out.println("\tExcluding Purchase: "+strDate);
                return true;
            }
        }
        return false;
    }

}
