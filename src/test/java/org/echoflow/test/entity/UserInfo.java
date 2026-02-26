package org.echoflow.test.entity;

public class UserInfo {
    private String name;
    private Integer age;
    private String occupation;
    private java.util.List<String> hobbies;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public String getOccupation() {
        return occupation;
    }

    public void setOccupation(String occupation) {
        this.occupation = occupation;
    }

    public java.util.List<String> getHobbies() {
        return hobbies;
    }

    public void setHobbies(java.util.List<String> hobbies) {
        this.hobbies = hobbies;
    }

    @Override
    public String toString() {
        return "UserInfo{name='" + name + "', age=" + age + ", occupation='" + occupation + "', hobbies=" + hobbies
                + "}";
    }
}
