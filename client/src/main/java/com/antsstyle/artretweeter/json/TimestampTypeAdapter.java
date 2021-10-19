/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.antsstyle.artretweeter.json;

import com.antsstyle.artretweeter.tools.FormatTools;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.Date;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author antss
 */
public class TimestampTypeAdapter extends TypeAdapter<Timestamp> {
    
    private static final Logger LOGGER = LogManager.getLogger(TimestampTypeAdapter.class);

    @Override
    public Timestamp read(JsonReader in) throws IOException {
        JsonToken token = in.peek();
        if (token.equals(JsonToken.NULL)) {
            in.nextNull();
            return null;
        }
        String string = in.nextString();
        try {
            Date date = FormatTools.DATETIME_FORMAT.parse(string);
            return new Timestamp(date.getTime());
        } catch (Exception e) {
            throw new IOException();
        }
    }

    @Override
    public void write(JsonWriter out, Timestamp timestamp) throws IOException {
        out.value(FormatTools.DATETIME_FORMAT.format(new Date(timestamp.getTime())));
    }
}
