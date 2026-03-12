package org.example.model;

public class Skill {
    private int id;
    private String skillName;
    private String category;
    private String description;

    public Skill() {}

    public Skill(int id, String skillName, String category, String description) {
        this.id = id;
        this.skillName = skillName;
        this.category = category;
        this.description = description;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getSkillName() { return skillName; }
    public void setSkillName(String skillName) { this.skillName = skillName; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}

