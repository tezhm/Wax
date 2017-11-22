package com.tezhm.wax.internal;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.processing.Filer;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

/**
 *
 */
public class XmlWriter
{
    private Map<String, List<InjectionField>> classMap = new HashMap<>();

    public void appendClass(String className)
    {
        if (!classMap.containsKey(className))
        {
            classMap.put(className, new ArrayList<InjectionField>());
        }
    }

    public void appendField(String className, String fieldName, String fieldType, String factoryName)
    {
        appendClass(className);
        classMap.get(className).add(new InjectionField(fieldName, fieldType, factoryName));
    }

    public void flush(Filer filer) throws Exception
    {
        DocumentBuilderFactory icFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder icBuilder;

        icBuilder = icFactory.newDocumentBuilder();
        Document doc = icBuilder.newDocument();

        Element mainRootElement = doc.createElement("InjectionMap");
        doc.appendChild(mainRootElement);

        for (Map.Entry<String, List<InjectionField>> entry : classMap.entrySet())
        {
            Element classElement = doc.createElement("Class");
            classElement.setAttribute("name", entry.getKey());
            mainRootElement.appendChild(classElement);

            // Throws in a bunch of possibly unordered name + type + factory elements. They need to
            // be in a shared element rather than scattered at the same level
            //
            // Might even overwrite each other since they aren't getting unique element names
            for (InjectionField injectionField : entry.getValue())
            {
                Element field = doc.createElement("Field");
                field.setAttribute("name", injectionField.name);
                field.setAttribute("type", injectionField.type);
                field.setAttribute("factory", injectionField.factory);
                classElement.appendChild(field);
            }
        }

        writeToFiler(filer, doc);
    }

    private void writeToFiler(Filer filer, Document doc) throws Exception
    {
        writeXml(filer, doc);
//        writeJson(filer);
    }

    private void writeXml(Filer filer, Document doc) throws Exception
    {
        FileObject filerResourceFile = filer.createResource(
                StandardLocation.SOURCE_OUTPUT,
                "com.tezhm.wax.generated.res",
                "injectionmap.xml",
                (javax.lang.model.element.Element)null
        );

        try
        {
            Writer writer = filerResourceFile.openWriter();
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            DOMSource source = new DOMSource(doc);

            StreamResult console = new StreamResult(writer);
            transformer.transform(source, console);
            writer.flush();
            writer.close();
        }
        catch (Exception e)
        {
            cleanup(filerResourceFile);
            throw e;
        }
    }

    private void writeJson(Filer filer) throws Exception
    {
        JSONObject root = new JSONObject();
        JSONArray injectionMap = new JSONArray();

        for (Map.Entry<String, List<InjectionField>> entry : classMap.entrySet())
        {
            JSONArray fields = new JSONArray();

            for (InjectionField injectionField : entry.getValue())
            {
                JSONObject field = new JSONObject();
                field.put("name", injectionField.name);
                field.put("type", injectionField.type);
                field.put("factory", injectionField.factory);
                fields.add(field);
            }

            JSONObject injection = new JSONObject();
            injection.put("class", entry.getKey());
            injection.put("fields", fields);
            injectionMap.add(injection);
        }

        root.put("InjectionMap", injectionMap);

        FileObject filerResourceFile = filer.createResource(
                StandardLocation.SOURCE_OUTPUT,
                "com.tezhm.wax.generated.res",
                "injectionmap.json",
                (javax.lang.model.element.Element)null
        );

        try
        {
            Writer writer = filerResourceFile.openWriter();
            writer.write(root.toJSONString());
            writer.flush();
            writer.close();
        }
        catch (Exception e)
        {
            cleanup(filerResourceFile);
            throw e;
        }
    }

    private void cleanup(FileObject filerResourceFile)
    {
        try
        {
            filerResourceFile.delete();
        }
        catch (Exception ignored) { }
    }
}
