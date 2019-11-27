package com.theheavenscode.facerec.modal;


public class FaceDetails{

private String name;

    @Override
    public String toString() {
        return "{" +
            " name:'" + getName() + "'" +
            ", centerX:'" + getCenterX() + "'" +
            ", centerY:'" + getCenterY() + "'" +
            ", topWidth:'" + getTopWidth() + "'" +
            "}";
    }
private String centerX;
private String centerY;
private String topWidth;


    public FaceDetails(String name, String centerX, String centerY, String topWidth) {
        this.name = name;
        this.centerX = centerX;
        this.centerY = centerY;
        this.topWidth = topWidth;
    }




    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCenterX() {
        return centerX;
    }

    public void setCenterX(String centerX) {
        this.centerX = centerX;
    }

    public String getCenterY() {
        return centerY;
    }

    public void setCenterY(String centerY) {
        this.centerY = centerY;
    }

    public String getTopWidth() {
        return topWidth;
    }

    public void setTopWidth(String topWidth) {
        this.topWidth = topWidth;
    }






}