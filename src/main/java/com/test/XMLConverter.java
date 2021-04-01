package com.test;


import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.Map;

public class XMLConverter {
    public static void main(String[] args) throws  XMLException {
        if(args.length!=2){
            throw new XMLException("Missing arguments. Arguments must include input and output path");
        }
        JSONObject jsonObject= null;
        try {
            jsonObject = (JSONObject) new JSONParser().parse(new FileReader(args[0]));

        StringBuilder xmlOutput=new StringBuilder();
        xmlOutput.append("<object>");
        String output=convertToXML(jsonObject);
        xmlOutput.append(output);;
        xmlOutput.append("</object>");
        DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document doc = db.parse(new InputSource(new StringReader(xmlOutput.toString())));
        String formattedXML = prettyPrint(doc);
        Files.write(Paths.get(args[1]),formattedXML.getBytes());
        } catch (IOException e) {
            throw new XMLException("Error with file :"+e.getMessage());
        } catch (ParseException e) {
            throw new XMLException("Error while parsing input json file:"+e.getMessage());
        } catch (SAXException | ParserConfigurationException | TransformerException e) {
            throw new XMLException("Error while parsing xml :"+e.getMessage());
        }
    }

    public static String convertToXML(Object object) {
        return convertToXML(object, null);
    }

    public static String convertToXML(Object object, String tagName) {
        StringBuilder xmlContent = new StringBuilder();
        JSONArray jsonArray;
        Iterator itr;
        Object val;
        if (!(object instanceof JSONObject)) {
            if (object != null) {
                if (object.getClass().isArray()) {
                    object = (JSONArray)object;
                }

                if (object instanceof JSONArray) {
                    jsonArray = (JSONArray)object;
                    itr = jsonArray.iterator();

                    while(itr.hasNext()) {
                        val = itr.next();
                        xmlContent.append(convertToXML(val, tagName == null ? "array" : tagName));
                    }

                    return xmlContent.toString();
                }
            }

            String tagVal = null ==object?"":object.toString();
            return tagVal.length() == 0 ? "<" + getType(object) + "/>" : "<" + getType(object) + ">" + tagVal + "</" + getType(object) + ">";
        } else {
            JSONObject jo = (JSONObject)object;
            if (tagName != null) {
                if(!"array".equalsIgnoreCase(tagName)) {
                    xmlContent.append("<object name=\"");
                    xmlContent.append(tagName);
                    xmlContent.append("\">");
                }else {
                    xmlContent.append('<');
                    xmlContent.append(tagName);
                    xmlContent.append('>');
                    xmlContent.append('<');
                    xmlContent.append("object");
                    xmlContent.append('>');
                }
            }
            Iterator iter = jo.entrySet().iterator();

            while(true) {
                while(iter.hasNext()) {
                    Map.Entry entry=(Map.Entry)iter .next();
                    String key = (String) entry.getKey();
                    Object value = entry.getValue();
                    if (value == null) {
                        value = "";
                    } else if (value.getClass().isArray()) {
                        value = (JSONArray)value;
                    }

                    if (value instanceof JSONArray) {
                        xmlContent.append("<");
                        xmlContent.append(getType(value));
                        xmlContent.append(" name=\"");
                        xmlContent.append(key);
                        xmlContent.append("\">");
                        jsonArray = (JSONArray)value;
                        itr = jsonArray.iterator();

                        while(itr.hasNext()) {
                            val = itr.next();
                            if (val instanceof JSONArray) {
                                xmlContent.append(convertToXML(val));
                            } else {
                                xmlContent.append(convertToXML(val, key));
                            }
                        }
                        xmlContent.append("</array>");
                    } else if (value instanceof String){
                        if(!((String) value).trim().equals("")) {
                            buildContent(xmlContent,value,key);
                        }else {
                            xmlContent.append("<null name=\"");
                            xmlContent.append(key);
                            xmlContent.append("\"/>");
                        }

                    }else if (value instanceof Integer || value instanceof Long || value instanceof Float || value instanceof Double){
                        buildContent(xmlContent,value,key);
                    }else if (value instanceof Boolean){
                        buildContent(xmlContent,value,key);
                    }else {
                        xmlContent.append(convertToXML(value, key));
                    }
                }

                if (tagName != null) {
                    if(!"array".equalsIgnoreCase(tagName)) {
                        xmlContent.append("</");
                        xmlContent.append("object");
                        xmlContent.append('>');
                    }else {
                        xmlContent.append("</");
                        xmlContent.append("object");
                        xmlContent.append('>');
                        xmlContent.append("</");
                        xmlContent.append(tagName);
                        xmlContent.append('>');
                    }
                }

                return xmlContent.toString();
            }
        }
    }

    private static void buildContent(StringBuilder sb,Object value,String key){
        sb.append("<");
        sb.append(getType(value));
        sb.append(" name=\"");
        sb.append(key);
        sb.append("\">");
        sb.append(value);
        sb.append("</");
        sb.append(getType(value));
        sb.append(">");
    }

    private static String getType(Object object){
        if(object instanceof String){
            return XMLConstants.OBJ_TYPE_STRING;
        }else if(object instanceof Integer || object instanceof Long || object instanceof Float || object instanceof Double){
            return XMLConstants.OBJ_TYPE_NUMBER;
        }else if(object instanceof Boolean){
            return XMLConstants.OBJ_TYPE_BOOLEAN;
        }else if(object instanceof JSONArray){
            return XMLConstants.OBJ_TYPE_ARRAY;
        }
        return "";
    }

    private static String prettyPrint(Document doc) throws TransformerException {
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        transformer.setOutputProperty(OutputKeys.ENCODING, "us-ascii");
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
        transformer.setOutputProperty(OutputKeys.DOCTYPE_PUBLIC, "yes");

        DOMSource source = new DOMSource(doc);
        StringWriter writer = new StringWriter();
        StreamResult result = new StreamResult(writer);
        transformer.transform(source, result);
        return writer.getBuffer().toString();
    }
}
