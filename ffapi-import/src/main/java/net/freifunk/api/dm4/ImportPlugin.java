package net.freifunk.api.dm4;

import de.deepamehta.core.osgi.PluginActivator;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.logging.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

public class ImportPlugin extends PluginActivator {

    private Logger log = Logger.getLogger(getClass().getName());
    
    private final String FF_API_CHARSET = "UTF-8";

    @Override
    public void init() {
        log.info("Freifunk API Data Plugin is initializing");
        InputStream ffDirectory = getStaticResource("web/ffSummarizedDir.json");
        log.info("Freifunk API Data Plugin fetched summarized API Directory");
        processSummarizedAPIDirectory(ffDirectory);
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
                    // 5) Store JSONObject (with various schemas) as a Freifunk Community Topic
                    if (freifunkCommunity.has("name")) {
                        log.info("Hello Freifunk Community: " + freifunkCommunity.getString("name"));   
                    }
                }
            }
            // 3) 
            log.info("");
        } catch (UnsupportedEncodingException ex) {
            log.severe(ex.getMessage());
        } catch (IOException ex) {
            log.severe(ex.getMessage());
        } catch (JSONException ex) {
            log.severe(ex.getMessage());
        }
    }
    
}
