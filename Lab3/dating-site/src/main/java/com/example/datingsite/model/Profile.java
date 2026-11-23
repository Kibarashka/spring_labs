package com.example.datingsite.model;

import java.util.ArrayList;
import java.util.List;

public class Profile {

    private Long id;
    private String name;
    private int age;
    private String publicInfo;
    private String privateInfo;
    private List<String> keywords;

    public Profile(Long id, String name, int age,
                   String publicInfo, String privateInfo,
                   List<String> keywords) {
        this.id = id;
        this.name = name;
        this.age = age;
        this.publicInfo = publicInfo;
        this.privateInfo = privateInfo;
        this.keywords = keywords == null ? new ArrayList<>() : new ArrayList<>(keywords);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public int getAge() {
        return age;
    }

    public String getPublicInfo() {
        return publicInfo;
    }

    public String getPrivateInfo() {
        return privateInfo;
    }

    public List<String> getKeywords() {
        return keywords;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public void setPublicInfo(String publicInfo) {
        this.publicInfo = publicInfo;
    }

    public void setPrivateInfo(String privateInfo) {
        this.privateInfo = privateInfo;
    }

    public void setKeywords(List<String> keywords) {
        this.keywords = keywords == null ? new ArrayList<>() : new ArrayList<>(keywords);
    }
}
