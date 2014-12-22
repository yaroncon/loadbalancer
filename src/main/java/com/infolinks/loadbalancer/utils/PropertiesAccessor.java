package com.infolinks.loadbalancer.utils;

import java.util.Properties;

import static java.lang.String.format;

/**
 * Created by: yaron
 */
public class PropertiesAccessor {

    private final Properties properties;

    public PropertiesAccessor(Properties properties)
    {
        this.properties = properties;
    }

    public <T> T get(String key, Class<T> type)
    {
        Object val = this.properties.get(key);
        if (val == null)
            return null;
        else if (type.isInstance(val))
            return type.cast(val);
        else
            return null;
    }

    public <T> T get(String key, Class<T> type, T defaultValue)
    {
        T val = get(key, type);
        if (val == null)
            return defaultValue;
        return val;
    }

    public <T> T require(String key, Class<T> type)
    {
        T val = get(key, type);
        if (val == null)
            throw new IllegalArgumentException( format( "missing key: %s", key ) );
        return val;
    }

    public String[] keySet() {
        return this.properties.keySet().toArray(new String[this.properties.size()]);
    }
}
