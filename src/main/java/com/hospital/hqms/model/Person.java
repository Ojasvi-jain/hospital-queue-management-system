package com.hospital.hqms.model;

/**
 * Abstract base class shared by every person in the system (patients and doctors).
 * Demonstrates abstraction, encapsulation and inheritance.
 */
public abstract class Person {

    private int id;
    private String name;
    private String phone;

    protected Person() {
    }

    protected Person(int id, String name, String phone) {
        this.id = id;
        this.name = name;
        this.phone = phone;
    }

    /** Human-readable identifier such as "P0007" or "D0002". */
    public abstract String getDisplayId();

    /** Role of this person in the hospital, e.g. "Patient" or "Doctor". */
    public abstract String getRole();

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    @Override
    public String toString() {
        return getRole() + " " + getDisplayId() + " - " + name;
    }
}
