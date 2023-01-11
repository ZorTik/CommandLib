package me.zort.commandlib.util;

import com.google.common.primitives.Primitives;

public class PrimitiveParser {

    private final String value;
    private final Class<?> typeClass;

    private Object parsed;
    private Throwable error = null;
    private boolean invalidFormat = false;

    public PrimitiveParser(String value, Class<?> typeClass) {
        this.value = value;
        this.typeClass = typeClass;
        this.parsed = value;
        if(value != null)
            parse();
    }

    public boolean isParsed() {
        return error == null && !parsed.getClass().equals(String.class);
    }

    public boolean isInvalidFormat() {
        return invalidFormat;
    }

    public Throwable getError() {
        return error;
    }

    public Object getAsObject() {
        return parsed;
    }

    private void parse() {
        Class<?> typeClass = Primitives.wrap(this.typeClass);
        try {
            if(typeClass.equals(Integer.class)) {
                this.parsed = Integer.parseInt(value);
            } else if(typeClass.equals(Double.class)) {
                this.parsed = Double.parseDouble(value);
            } else if(typeClass.equals(Float.class)) {
                this.parsed = Float.parseFloat(value);
            } else if(typeClass.equals(Long.class)) {
                this.parsed = Long.parseLong(value);
            } else if(typeClass.equals(Short.class)) {
                this.parsed = Short.parseShort(value);
            } else if(typeClass.equals(Byte.class)) {
                this.parsed = Byte.parseByte(value);
            } else if(typeClass.equals(Boolean.class)) {
                this.parsed = Boolean.parseBoolean(value);
            } else if(typeClass.equals(Character.class)) {
                this.parsed = value.charAt(0);
            } else {
                this.invalidFormat = true;
            }
        } catch(Exception e) {
            this.error = e;
        }
    }
}
