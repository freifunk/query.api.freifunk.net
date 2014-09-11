package net.freifunk.api.dm4;

import de.deepamehta.core.Topic;
import de.deepamehta.core.model.CompositeValueModel;
import de.deepamehta.core.model.SimpleValue;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.osgi.PluginActivator;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

public class ImportPlugin extends PluginActivator {

    private Logger log = Logger.getLogger(getClass().getName());
    
    private final String FF_API_CHARSET = "UTF-8";

    @Override
    public void init() {
        try {
            deleteAllImportedDataNodes();
            log.info("Freifunk API Data Plugin is initializing");
            // 1) Fetch API Directory
            URL apiDirectoryEndpoint = new URL("http://freifunk.net/map/ffSummarizedDir.json");
            HttpURLConnection connection = (HttpURLConnection) apiDirectoryEndpoint.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", "DeepaMehta 4 - Freifunk API Data");
            // 2) Check Response
            int httpStatusCode = connection.getResponseCode();
            if (httpStatusCode != HttpURLConnection.HTTP_OK) {
                throw new RuntimeException("HTTP connection error, Status: " + httpStatusCode);
            }
            // 3) Start importing/processing the directory
            log.info("OK -  Freifunk API Data Plugin fetched summarized API Directory");
            processSummarizedAPIDirectory(connection.getInputStream());
        } catch (MalformedURLException ex) {
            Logger.getLogger(ImportPlugin.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(ImportPlugin.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    
    
    // --
    // --- Private Utility Methods
    // --
    
    private void processSummarizedAPIDirectory(InputStream summarizedDirectory) {
        log.info("Freifunk API Data Plugin starts processing directory");
        JSONObject freifunkApiSummaryObject = null;
        try {
            StringBuffer data = new StringBuffer();
            // 1) Convert InputStream to String
            BufferedReader reader = new BufferedReader(new InputStreamReader(summarizedDirectory, FF_API_CHARSET));
            for (String input; (input = reader.readLine()) != null;) {
                data.append(input);
            }
            reader.close();
            // 2) String to JSONObject
            if (data.toString().isEmpty()) throw new RuntimeException("Somebody handed us an EMPTY InputStream");
            // 3) Go through all communities
            freifunkApiSummaryObject = new JSONObject (data.toString());
            JSONArray community_keys = freifunkApiSummaryObject.names();
            if (community_keys.length() > 1) {
                log.info("Identified " + community_keys.length() + " Freifunk Communities in API Directory");
                for (int i = 0; i < community_keys.length(); i++) {
                    // 4) Grab single community
                    JSONObject freifunkCommunity = 
                        freifunkApiSummaryObject.getJSONObject(community_keys.getString(i));
                    // 5) Convert API JSONObject to a "Freifunk Community"
                    CompositeValueModel communityModel = new CompositeValueModel();
                    if (freifunkCommunity.has("name")) {
                        String name = freifunkCommunity.getString("name");
                        log.info("Hello Freifunk Community: " + name);
                        communityModel.put("net.freifunk.community.name", name);
                    }
                    if (freifunkCommunity.has("url")) {
                        String url = freifunkCommunity.getString("url");
                        communityModel.put("dm4.webbrowser.url", url);
                    }
                    if (freifunkCommunity.has("techDetails")) {
                        JSONObject techDetails = freifunkCommunity.getJSONObject("techDetails");
                        if (techDetails.has("vpn")) {
                            String vpn = techDetails.getString("vpn");
                            enrichAboutVPNTopic(communityModel, vpn);
                        }
                        // ### ..
                    }
                    if (freifunkCommunity.has("mtime")) {
                        String mtime = freifunkCommunity.getString("mtime");
                        communityModel.put("net.freifunk.community.api_mtime", mtime);
                    }
                    if (freifunkCommunity.has("api")) {
                        String api = freifunkCommunity.getString("api");
                        enrichAboutApiVersionTopic(communityModel, api);
                    }
                    dms.createTopic(new TopicModel("net.freifunk.community", communityModel), null);
                }
            }
            log.info("### Importer created" + community_keys.length() + " Freifunk Communities from API Directory");
        } catch (UnsupportedEncodingException ex) {
            log.severe(ex.getMessage());
        } catch (IOException ex) {
            log.severe(ex.getMessage());
        } catch (JSONException ex) {
            log.severe(ex.getMessage());
        }
    }
    
    private void deleteAllImportedDataNodes () {
        for (Topic node : dms.getTopics("net.freifunk.community", false, 0)) {
            dms.deleteTopic(node.getId());
        }
    }

    private void enrichAboutApiVersionTopic(CompositeValueModel communityModel, String api) {
        // Note: IndexMode.KEY needs to be set on queried TopicType to succeed with the following type of query in DM4
        Topic existingApiTopic = dms.getTopic("net.freifunk.community.api_version", new SimpleValue(api), false);
        if (existingApiTopic != null) { // Reference existing API Version Topic
            communityModel.putRef("net.freifunk.community.api_version", existingApiTopic.getId());
        } else { // Create new API Version Topic
            communityModel.put("net.freifunk.community.api_version", api);
        }
    }

    private void enrichAboutVPNTopic(CompositeValueModel communityModel, String vpn) {
        // Note: IndexMode.KEY needs to be set on queried TopicType to succeed with the following type of query in DM4
        String alteredVPNValue = vpn.toLowerCase().trim();
        Topic existingVPNTopic = dms.getTopic("net.freifunk.community.vpn", new SimpleValue(alteredVPNValue), false);
        if (existingVPNTopic != null) { // Reference existing VPN Value Topic
            communityModel.putRef("net.freifunk.community.vpn", existingVPNTopic.getId());
        } else { // Create new VPN Value Topic
            communityModel.put("net.freifunk.community.vpn", alteredVPNValue);
        }
    }
    
}
