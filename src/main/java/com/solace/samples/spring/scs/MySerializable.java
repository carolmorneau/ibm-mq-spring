package com.solace.samples.spring.scs;

import java.io.Serializable;

public class MySerializable implements Serializable {
    private String name;

    public MySerializable(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
