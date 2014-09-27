package net.freifunk.api.dm4;

import de.deepamehta.core.RelatedTopic;
import de.deepamehta.core.Topic;
import de.deepamehta.core.model.CompositeValueModel;
import de.deepamehta.core.model.SimpleValue;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.osgi.PluginActivator;
import de.deepamehta.core.service.ResultList;
import de.deepamehta.core.storage.spi.DeepaMehtaTransaction;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

/**
 * Freifunk API Summarized Directory Client
 * 
 * @author @0x6d54, @glycoknob and @mukil
 * @version 0.1-SNAPSHOT
 */

@Path("/freifunk-api")
public class ImportPlugin extends PluginActivator {

    private Logger log = Logger.getLogger(getClass().getName());
    
    private final String FF_API_CHARSET = "UTF-8";
    
    // --- DeepaMehta 4 Topic Types
    
    private static final String DM4_WEBBROWSER_URL = "dm4.webbrowser.url";
    
    // --- Freifunk Topic Types
    
    private static final String FFN_COMMUNITY_TYPE = "net.freifunk.community";
    private static final String FFN_COMMUNITY_NAME_TYPE = "net.freifunk.community.name";
    private static final String FFN_COMMUNITY_MTIME_TYPE = "net.freifunk.community.api_mtime";
    private static final String FFN_COMMUNITY_APIVERSION_TYPE = "net.freifunk.community.api_version";
    private static final String FFN_COMMUNITY_VPN_TYPE = "net.freifunk.community.vpn";
    // ### Values for the following types not yet processed
    private static final String FFN_COMMUNITY_NETWORK_TYPE = "net.freifunk.community.network";
    private static final String FFN_COMMUNITY_LEGALS_TYPE = "net.freifunk.community.legals";
    private static final String FFN_COMMUNITY_ROUTING_TYPE = "net.freifunk.community.routing";
    private static final String FFN_COMMUNITY_ROUTING_VALUE_TYPE = "net.freifunk.community.routing_value";

    @Override
    public void init() {
        try {
            deleteAllImportedDataNodes();
            log.info("Freifunk API Data Plugin is initializing");
            // 1) Fetch API Directory
            URL apiDirectoryEndpoint = new URL("http://freifunk.net/map/ffSummarizedDir.json");
            HttpURLConnection connection = (HttpURLConnection) apiDirectoryEndpoint.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", "DeepaMehta 4 - Freifunk API Directory Client");
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
    // --- Plugin Service Methods (Freifunk API Directory API)
    // -- 
    
    @GET
    @Path("/routing/{protocol-name}")
    @Produces("application/json")
    public List<Topic> getFreifunkCommunitiesByRoutingValue(@PathParam("protocol-name") String value) {
        ArrayList<Topic> communities = new ArrayList<Topic>();
        // 1) clean up user input
        String query = value.toLowerCase().trim();
        // 2) fetch the one routing-value topic
        Topic routingValueTopic = dms.getTopic(FFN_COMMUNITY_ROUTING_VALUE_TYPE, new SimpleValue(query));
        // 3) navigate on the graph to collect all communities related to this topic
        if (routingValueTopic != null) {
            // 3.1) navigate one level up in our model, to our (many) intermediary topics
            ResultList<RelatedTopic> parents = routingValueTopic.getRelatedTopics("dm4.core.aggregation", 
                "dm4.core.child", "dm4.core.parent", FFN_COMMUNITY_ROUTING_TYPE, 0);
            for (Topic parent : parents) {
                // 3.2) one level further up we find one community topic per intermediary (routing) topic
                RelatedTopic community = parent.getRelatedTopic("dm4.core.composition", 
                    "dm4.core.child", "dm4.core.parent", FFN_COMMUNITY_TYPE);
                if (community != null) {
                    community.loadChildTopics();
                    communities.add(community);
                } else {
                    log.severe("Could not fetch related \"Freifunk Community\" topic for " 
                        + parent.getSimpleValue() + " ("+parent.getId()+")");
                }
            }
        } else {
            log.info("No routing name-values found for \"" + value + "\"");
        }
        return communities;
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
                    // 5.1) Put Topics (Composition Definition)
                    if (freifunkCommunity.has("name")) {
                        String name = freifunkCommunity.getString("name");
                        log.info("Hello Freifunk Community: " + name);
                        communityModel.put(FFN_COMMUNITY_NAME_TYPE, name);
                    }
                    if (freifunkCommunity.has("url")) {
                        String url = freifunkCommunity.getString("url");
                        communityModel.put(DM4_WEBBROWSER_URL, url);
                    }
                    if (freifunkCommunity.has("mtime")) {
                        String mtime = freifunkCommunity.getString("mtime");
                        communityModel.put(FFN_COMMUNITY_MTIME_TYPE, mtime);
                    }
                    // 5.1) Put or Reference Topics (Aggregation Definition)
                    if (freifunkCommunity.has("techDetails")) {
                        JSONObject techDetails = freifunkCommunity.getJSONObject("techDetails");
                        if (techDetails.has("vpn")) {
                            String vpn = techDetails.getString("vpn");
                            // implementing simple getOrCreateTopic-Logic for re-using aggregated topics
                            enrichAboutVPNTopic(communityModel, vpn);
                        } else if (techDetails.has("routing")) {
                            // Note: given routing protocol values MAY be many
                            // 5.1.2) Gather/parse all routing protocol names for this community in a string-array
                            String names[] = null;
                            try {
                                JSONArray protocols = techDetails.getJSONArray("routing");
                                names = new String[protocols.length()];
                                for (int k = 0; k < protocols.length(); k++) {
                                    String protocol_name_value = protocols.getString(k);
                                    names[k] = protocol_name_value;
                                }
                            } catch (JSONException je) {
                                // but there also maybe just single-valued (stored as a simple string value)
                                String protocol_name_value = techDetails.getString("routing");
                                // ... and as #### here in this string, we find: arrays (!) ..
                                if (protocol_name_value.contains(",")) { // split up many values
                                    int n = 0;
                                    String[] parsed_names = protocol_name_value.split(",");
                                    names = new String[parsed_names.length];
                                    for (String name : parsed_names) {
                                        names[n] = name;
                                        n++;
                                    }
                                } else { // as well as a sinmple string, to process the routing name-value
                                    names = new String[1];
                                    names[0] = protocol_name_value;
                                }
                            }
                            // 5.1.3) Write all protocol values for this community
                            enrichAboutRoutingProtocolNameTopic(communityModel, names);
                        }
                        // ### ..
                    }
                    if (freifunkCommunity.has("api")) {
                        String api = freifunkCommunity.getString("api");
                        // implementing simple getOrCreateTopic-Logic for re-using aggregated topics
                        enrichAboutApiVersionTopic(communityModel, api);
                    }
                    DeepaMehtaTransaction tx = dms.beginTx();
                    try {
                        dms.createTopic(new TopicModel(FFN_COMMUNITY_TYPE, communityModel));
                        tx.success();
                    } catch (Exception e) {
                        log.severe("Error creating \"Freifunk Community\" topic");
                        throw new RuntimeException(e);
                    } finally {
                        tx.finish();
                    }
                }
            }
            log.info("### Imported " + community_keys.length() + " Freifunk Communities from API Directory");
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException(ex);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        } catch (JSONException ex) {
            throw new RuntimeException(ex);
        }
    }
    
    private void deleteAllImportedDataNodes () {
        for (Topic node : dms.getTopics(FFN_COMMUNITY_TYPE, 0)) {
            DeepaMehtaTransaction tx = dms.beginTx();
            try {
                dms.deleteTopic(node.getId());
                tx.success();
            } catch (Exception e) {
                log.severe("Error deleting imported \"Freifunk Community\" topic");
                throw new RuntimeException(e);
            } finally {
                tx.finish();
            }
        }
    }

    private void enrichAboutApiVersionTopic(CompositeValueModel communityModel, String api) {
        // Note: IndexMode.KEY needs to be set on queried TopicType to succeed with the following type of query in DM4
        Topic existingApiTopic = dms.getTopic(FFN_COMMUNITY_APIVERSION_TYPE, new SimpleValue(api));
        if (existingApiTopic != null) { // Reference existing API Version Topic
            communityModel.putRef(FFN_COMMUNITY_APIVERSION_TYPE, existingApiTopic.getId());
        } else { // Create new API Version Topic
            communityModel.put(FFN_COMMUNITY_APIVERSION_TYPE, api);
        }
    }

    private void enrichAboutVPNTopic(CompositeValueModel communityModel, String vpn) {
        // Note: IndexMode.KEY needs to be set on queried TopicType to succeed with the following type of query in DM4
        String alteredVPNValue = vpn.toLowerCase().trim();
        Topic existingVPNTopic = dms.getTopic(FFN_COMMUNITY_VPN_TYPE, new SimpleValue(alteredVPNValue));
        if (existingVPNTopic != null) { // Reference existing VPN Value Topic
            communityModel.putRef(FFN_COMMUNITY_VPN_TYPE, existingVPNTopic.getId());
        } else { // Create new VPN Value Topic
            communityModel.put(FFN_COMMUNITY_VPN_TYPE, alteredVPNValue);
        }
    }

    private void enrichAboutRoutingProtocolNameTopic(CompositeValueModel communityModel, String[] nameValues) {
        CompositeValueModel routingProtocolTopicModel = new CompositeValueModel();
        for (String protocolName : nameValues) {
            // 1) clean up given name value
            if (protocolName != null) {
                String routingProtocolName = protocolName.replaceAll("\"", ""); // quotation marks
                routingProtocolName = routingProtocolName.replaceAll("[\\[\\]]", "");
                if (routingProtocolName.contains("[")) routingProtocolName.substring(1);
                if (routingProtocolName.contains("]")) routingProtocolName.substring(routingProtocolName.length()-1);
                routingProtocolName = routingProtocolName.toLowerCase().trim();
                if (routingProtocolName.isEmpty()) return; // sanity check
                // 2) Fetch existing routing value topic (by name-value)
                // Note: IndexMode.KEY needs to be set on queried TopicType to succeed with the following type of query in DM4
                Topic routingProtocolValueTopic = dms.getTopic(FFN_COMMUNITY_ROUTING_VALUE_TYPE, 
                    new SimpleValue(routingProtocolName));
                // 3) Build up intermediary routing topics with (aggregated) many routing value topics
                if (routingProtocolValueTopic != null) { // 3.1) Reference existing Routing Value Topic
                    routingProtocolTopicModel.addRef(FFN_COMMUNITY_ROUTING_VALUE_TYPE, routingProtocolValueTopic.getId());
                } else { // 3.2) Create new Routing Value Topic
                    routingProtocolTopicModel.add(FFN_COMMUNITY_ROUTING_VALUE_TYPE, 
                        new TopicModel(FFN_COMMUNITY_ROUTING_VALUE_TYPE, new SimpleValue(routingProtocolName)));
                }    
            } else {
                log.severe("Given protocol-name is NULL!");
            }
        }
        // 4) Create and attach new routing topic to \"Freifunk Community\"-Topic
        communityModel.put(FFN_COMMUNITY_ROUTING_TYPE, routingProtocolTopicModel);
    }
}
