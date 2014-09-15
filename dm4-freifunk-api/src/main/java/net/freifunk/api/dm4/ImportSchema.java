package net.freifunk.api.dm4;

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import de.deepamehta.core.Topic;
import de.deepamehta.core.TopicType;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.model.TopicTypeModel;

public class ImportSchema {

	public JsonFactory jfactory = new JsonFactory();
	public ObjectMapper m = new ObjectMapper();
	public Integer count = 0;
	public JsonNode jn;
	public Map<String, List<String>> types = new HashMap<String, List<String>>();
	private final JsonNodeFactory factory = JsonNodeFactory.instance;

	ImportSchema() throws JsonParseException, IOException {
		//jn = m.readTree(new File("/home/riso3860/spec.json"));
		jn = m.readTree(new File("/home/riso3860/ffSummarizedDir.json"));
	}
	



	Stack<String> type = new Stack<String>();

	// public String parseSchema(TreeNode node, String key) throws IOException {
	// if (node.isObject()) {
	// type.push(key);
	// Iterator<String> fields = node.fieldNames();
	// List<String> s = new ArrayList<String>();
	// while (fields.hasNext()) {
	// String field = fields.next();
	// if (node.at("/type").toString().contains("object")) {
	// // System.out.println("true");
	// key = parseSchema(node.get(field), field);
	//
	// } else {
	// // System.out.println("false");
	// // System.out.println(type.toString()+":"+field);
	// // s.add(node.a);
	// key = parseSchema(node.get(field), field);
	// }
	// }
	// types.put(type.toString(), s);
	// type.pop();
	// }
	//
	// return key;
	// }

	public String printAll(JsonNode node, String f) throws JsonProcessingException, IOException, JSONException {
		System.out.println("---");
		ObjectNode json = factory.objectNode();
		
		if (!f.equals("properties"))
			type.push(f);
		
		Iterator<String> fieldNames = node.fieldNames();
		
		Enumeration<String> e = type.elements();
		while (e.hasMoreElements())
			e.nextElement();
		
		String uri = type.toString().replaceAll(", ", ".")
				.replaceFirst(".schema.", ".community.")
				.replaceAll("[\\[\\]]", "");
		json.put("uri", uri);
		ArrayNode index_mode = factory.arrayNode();
		index_mode.add("dm4.core.fulltext");
		index_mode.add("dm4.core.fulltext_key");
		json.set("index_mode_uris", index_mode);
		json.put("data_type_uri", "dm.core.text");
		ArrayNode array = factory.arrayNode();
		while (fieldNames.hasNext()) {
			String fieldName = fieldNames.next();
			JsonNode fieldValue = node.get(fieldName);
			if (node.findParent("description").get("description") != null)
				json.set("value", node.findParent("description").get("description"));
			
			if (fieldValue.isObject()) {
				ObjectNode assoc = factory.objectNode();
				assoc.put("child_type_uri", uri+"."+printAll(fieldValue, fieldName).replaceAll("\n",""));
				assoc.put("child_cardinality_uri", "dm4.core.one");
				assoc.put("parent_cardinality_uri", "dm4.core.one");
				assoc.put("assoc_type_uri", "dm4.core.composition_def");
				//json.append(("composite: "+uri+"."+printAll(fieldValue, fieldName)).replaceAll("\n", "")+"\n");
				array.add(assoc);
				json.put("data_type_uri", "dm4.core.composite");
				json.set("assoc_defs", array);
			} else {
				//String value = fieldValue.toString();
				if (fieldName.equals("description")) {
					json.set("value", fieldValue);
					//json.append("\"value\"" + " : " + value+",\n");
				}
				if (fieldName.equals("enum")) {
					//json.append("\"options\"" + " : " + value+",\n");
					json.set("enum", fieldValue);
				}
			}
		}
		//json.append("}");
		System.out.println("xxx");
		System.out.println(m.writerWithDefaultPrettyPrinter().writeValueAsString(json));
		TopicTypeModel t = new TopicTypeModel(new JSONObject(json.toString()));
		System.err.println(t.getUri());
		System.err.println(t.getAssocDefs());
		if (!f.equals("properties")) {
				return type.pop();
		} else return "";
	}

	public String parseData(JsonNode node, String f) throws JsonProcessingException, IOException, JSONException {
		System.out.println("---");
		ObjectNode json = factory.objectNode();

		type.push(f);
		
		Iterator<String> fieldNames = node.fieldNames();
			
		String uri = type.toString().replaceAll(", ", ".")
				.replaceAll("[\\[\\]]", "");
		json.put("type_uri", uri);
		ObjectNode object = factory.objectNode();
		while (fieldNames.hasNext()) {
			String fieldName = fieldNames.next();
			JsonNode fieldValue = node.get(fieldName);
			//if (node.findParent("description").get("description") != null)
			//json.set("value", node.findParent("description").get("description"));
			
			if (fieldValue.isObject()) {
				ObjectNode assoc = factory.objectNode();
				assoc.set(uri+"."+parseData(fieldValue, fieldName).replaceAll("\n",""),fieldValue);
				json.set("composite", assoc);
			} else {
				json.set(fieldName, fieldValue);
			}
		}
		//json.append("}");
		System.out.println("xxx");
		System.out.println(m.writerWithDefaultPrettyPrinter().writeValueAsString(json));
		TopicModel t = new TopicModel(new JSONObject(json.toString()));
		System.err.println(t.getUri());
		System.err.println(t.getCompositeValueModel());
		
				return type.pop();
	}
	
	public static void main(String[] args) throws JsonProcessingException,
			IOException, JSONException {

		ImportSchema test = new ImportSchema();

		// test.parseSchema(test.jn, "start");
		


			test.parseData(test.jn.get("ansbach"), "freifunk");
	

		// for (Entry<String, ?> e : test.types.entrySet()) {
		// String uri = e.getKey().replaceAll(", ",
		// ".").replaceFirst("start.schema.",
		// "freifunk.community.").replaceAll("properties.",
		// "").replaceAll("[\\[\\]]", "");
		// System.out.println(uri+":"+e.getValue());
		//
		// }
		//
		// String type =
		// "{\"value\": \"Freifunk Community Name\",\"uri\": \"net.freifunk.community.name\", \"data_type_uri\": \"dm4.core.text\", \"index_mode_uris\": [\"dm4.core.fulltext\", \"dm4.core.fulltext_key\"], \"view_config_topics\": [{\"type_uri\": \"dm4.webclient.view_config\",\"composite\": {\"dm4.webclient.show_in_create_menu\": false}}]}";
		// TopicTypeModel t = null;
		// try {
		// t = new TopicTypeModel(new JSONObject(type));
		// } catch (JSONException e1) {
		// // TODO Auto-generated catch block
		// e1.printStackTrace();
		// }
		//
		// System.err.println(t.getUri());


	}
}
