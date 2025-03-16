package edu.ismt.dipendra.mealmate.model;

import com.google.firebase.firestore.Exclude;

/**
 * Model class for a geotagged store
 */
public class GeotaggedStore {
    
    private String id;
    private String name;
    private double latitude;
    private double longitude;
    private String address;
    private String userId;
    
    // Empty constructor needed for Firestore
    public GeotaggedStore() {
    }
    
    public GeotaggedStore(String name, double latitude, double longitude, String address, String userId) {
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.address = address;
        this.userId = userId;
    }
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public double getLatitude() {
        return latitude;
    }
    
    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }
    
    public double getLongitude() {
        return longitude;
    }
    
    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }
    
    public String getAddress() {
        return address;
    }
    
    public void setAddress(String address) {
        this.address = address;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    @Exclude
    public String getLocationString() {
        return latitude + "," + longitude;
    }
    
    @Override
    public String toString() {
        return name;
    }
} 