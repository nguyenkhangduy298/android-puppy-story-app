package com.example.admin.puppyapp;

public class DogModel {
    public String name, url, breed, description;

    public DogModel() {
    }

    public DogModel(String name, String breed, String description){
        this.name = name;
        this.breed = breed;
        this.description = description;
    }

    public DogModel(String name, String url, String breed, String description) {
        this.name = name;
        this.url = url;
        this.breed = breed;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBreed() {
        return breed;
    }

    public void setBreed(String breed) {
        this.breed = breed;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }


    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
