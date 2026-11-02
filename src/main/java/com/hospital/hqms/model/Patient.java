package com.hospital.hqms.model;

public class Patient extends Person {

    public static final int SENIOR_AGE = 60;

    private int age;
    private String gender;
    private String email;
    private String bloodGroup;
    private String address;

    public Patient() {
    }

    public Patient(int id, String name, int age, String gender, String phone,
                   String email, String bloodGroup, String address) {
        super(id, name, phone);
        this.age = age;
        this.gender = gender;
        this.email = email;
        this.bloodGroup = bloodGroup;
        this.address = address;
    }

    @Override
    public String getDisplayId() {
        return String.format("P%04d", getId());
    }

    @Override
    public String getRole() {
        return "Patient";
    }

    /** Senior citizens automatically get a higher queue priority. */
    public boolean isSenior() {
        return age >= SENIOR_AGE;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getBloodGroup() {
        return bloodGroup;
    }

    public void setBloodGroup(String bloodGroup) {
        this.bloodGroup = bloodGroup;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}
