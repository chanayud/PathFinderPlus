package com.example.pathfinderplus;

public class AddressRecord {
    private String chosenAddress;
    private boolean isSelected;

    public AddressRecord(String chosenAddress) {
        this.chosenAddress = chosenAddress;
        this.isSelected = false;
    }

    public String getChosenAddress() {
        return chosenAddress;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }
}
