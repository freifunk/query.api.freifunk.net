package net.freifunk.api.dm4;

import de.deepamehta.core.Topic;
import de.deepamehta.core.model.CompositeValueModel;
import de.deepamehta.core.model.SimpleValue;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.model.TopicTypeModel;
import de.deepamehta.core.osgi.PluginActivator;
import de.deepamehta.core.storage.spi.DeepaMehtaTransaction;

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

	@Override
	public void init() {
		try {
			deleteAllImportedDataNodes();
			log.info("Freifunk API Data Plugin is initializing");
			// 1) Fetch API Directory
			URL apiDirectoryEndpoint = new URL(
					"http://freifunk.net/map/ffSummarizedDir.json");
			HttpURLConnection connection = (HttpURLConnection) apiDirectoryEndpoint
					.openConnection();
			connection.setRequestMethod("GET");
			connection.setRequestProperty("User-Agent",
					"DeepaMehta 4 - Freifunk API Data");
			// 2) Check Response
			int httpStatusCode = connection.getResponseCode();
			if (httpStatusCode != HttpURLConnection.HTTP_OK) {
				throw new RuntimeException("HTTP connection error, Status: "
						+ httpStatusCode);
			}
			// 3) Start importing/processing the directory
			log.info("OK -  Freifunk API Data Plugin fetched summarized API Directory");
			processSummarizedAPIDirectory(connection.getInputStream());
		} catch (MalformedURLException ex) {
			Logger.getLogger(ImportPlugin.class.getName()).log(Level.SEVERE,
					null, ex);
		} catch (IOException ex) {
			Logger.getLogger(ImportPlugin.class.getName()).log(Level.SEVERE,
					null, ex);
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
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					summarizedDirectory, FF_API_CHARSET));
			for (String input; (input = reader.readLine()) != null;) {
				data.append(input);
			}

			reader.close();
			// 2) String to JSONObject
			if (data.toString().isEmpty())
				throw new RuntimeException(
						"Somebody handed us an EMPTY InputStream");
			// 3) Go through all communities
			freifunkApiSummaryObject = new JSONObject(data.toString());
			JSONArray community_keys = freifunkApiSummaryObject.names();
			if (community_keys.length() > 1) {
				log.info("Identified " + community_keys.length()
						+ " Freifunk Communities in API Directory");

				for (int i = 0; i < community_keys.length(); i++) {
					// 4) Grab single community
					JSONObject freifunkCommunity = freifunkApiSummaryObject
							.getJSONObject(community_keys.getString(i));
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
						JSONObject techDetails = freifunkCommunity
								.getJSONObject("techDetails");
						if (techDetails.has("vpn")) {
							String vpn = techDetails.getString("vpn");
							enrichAboutVPNTopic(communityModel, vpn); // implementing 
																	  // simple
																	  // getOrCreateTopic-Logic
						}
						// ### ..
					}
					if (freifunkCommunity.has("api")) {
						String api = freifunkCommunity.getString("api");
						enrichAboutApiVersionTopic(communityModel, api); // implementing
																			// simple
						// getOrCreateTopic-Logic
						DeepaMehtaTransaction tx = dms.beginTx();
						try {

							dms.createTopic(new TopicModel(FFN_COMMUNITY_TYPE,
									communityModel));
							tx.success();
						} catch (Exception e) {
							log.log(Level.SEVERE,
									"### Error creating Tropic: ", e);
							tx.failure();
							throw new RuntimeException(e);
						} finally {
							tx.finish();
						}

						log.info("### Imported " + community_keys.length()
								+ " Freifunk Communities from API Directory");
					}
				}
			}
		} catch (UnsupportedEncodingException ex) {
			throw new RuntimeException(ex);
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		} catch (JSONException ex) {
			throw new RuntimeException(ex);
		}
	}

	private void deleteAllImportedDataNodes() {
		for (Topic node : dms.getTopics(FFN_COMMUNITY_TYPE, false, 0)) {
			dms.deleteTopic(node.getId());
		}
	}

	private void enrichAboutApiVersionTopic(CompositeValueModel communityModel,
			String api) {
		// Note: IndexMode.KEY needs to be set on queried TopicType to succeed
		// with the following type of query in DM4
		Topic existingApiTopic = dms.getTopic(FFN_COMMUNITY_APIVERSION_TYPE,
				new SimpleValue(api), false);
		if (existingApiTopic != null) { // Reference existing API Version Topic
			communityModel.putRef(FFN_COMMUNITY_APIVERSION_TYPE,
					existingApiTopic.getId());
		} else { // Create new API Version Topic
			communityModel.put(FFN_COMMUNITY_APIVERSION_TYPE, api);
		}
	}

	private void enrichAboutVPNTopic(CompositeValueModel communityModel,
			String vpn) {
		// Note: IndexMode.KEY needs to be set on queried TopicType to succeed
		// with the following type of query in DM4
		String alteredVPNValue = vpn.toLowerCase().trim();
		Topic existingVPNTopic = dms.getTopic(FFN_COMMUNITY_VPN_TYPE,
				new SimpleValue(alteredVPNValue), false);
		if (existingVPNTopic != null) { // Reference existing VPN Value Topic
			communityModel.putRef(FFN_COMMUNITY_VPN_TYPE,
					existingVPNTopic.getId());
		} else { // Create new VPN Value Topic
			communityModel.put(FFN_COMMUNITY_VPN_TYPE, alteredVPNValue);
		}
	}

}
