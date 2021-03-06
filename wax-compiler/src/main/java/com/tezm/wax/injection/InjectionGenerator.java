package com.tezm.wax.injection;

import org.json.simple.JSONObject;

import java.io.Writer;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.Filer;
import javax.lang.model.element.Element;
import javax.lang.model.type.TypeMirror;
import javax.tools.FileObject;
import javax.tools.StandardLocation;

/**
 *
 */
public class InjectionGenerator
{
    private final String packagePrefix;
    private final InjectionMap injectionMap;

    /**
     *
     * @param packagePrefix
     */
    public InjectionGenerator(String packagePrefix)
    {
        this.packagePrefix = packagePrefix;
        this.injectionMap = new InjectionMap();
    }

    public void process(InjectionContainer injections)
    {
        for (Map.Entry<TypeMirror, Set<Element>> classFieldEntry : injections.fields.entrySet())
        {
            TypeMirror cls = classFieldEntry.getKey();
            String enclosingClassName = cls.toString().replace('.', '/');

            for (Element field : classFieldEntry.getValue())
            {
                String fieldName = field.toString();
                TypeMirror fieldType = field.asType();
                String fieldTypeName = fieldType.toString().replace('.', '/');
                String factoryName = fieldTypeName + "Factory";

                this.injectionMap.appendField(
                        enclosingClassName,
                        fieldName,
                        fieldTypeName,
                        this.packagePrefix + factoryName
                );
            }
        }
    }

    public void flush(Filer filer) throws Exception
    {
        JSONObject injectionMap = this.injectionMap.toJson();

        FileObject resource = filer.createResource(
                StandardLocation.SOURCE_OUTPUT,
                "com.tezm.wax.generated.res",
                "injection_map.json",
                (Element)null
        );

        try
        {
            Writer writer = resource.openWriter();
            writer.write(injectionMap.toJSONString());
            writer.flush();
            writer.close();
        }
        catch (Exception e)
        {
            cleanup(resource);
            throw e;
        }
    }

    private void cleanup(FileObject resource)
    {
        try
        {
            resource.delete();
        }
        catch (Exception ignored) { }
    }
}
