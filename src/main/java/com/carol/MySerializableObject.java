package com.carol;

import java.io.Serializable;

public class MySerializableObject implements Serializable {

    private String myField;

    public MySerializableObject(String myField) {

        this.myField = myField;
    }

    public String getMyField() {
        return myField;
    }

    public void setMyField(String myField) {
        this.myField = myField;
    }

    @Override
    public String toString() {
        return myField;
    }
}
