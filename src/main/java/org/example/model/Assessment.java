package org.example.model;

import java.sql.Timestamp;

public class Assessment {
    private int id;
    private int userId;
    private int skillId;
    private String skillName;
    private String category;
    private int requiredLevel;
    private int actualLevel;
    private int gapLevel;
    private Timestamp assessedAt;

    public Assessment() {}

    public Assessment(int userId, String category, int requiredLevel, int actualLevel) {
        this.userId = userId;
        this.category = category;
        this.requiredLevel = requiredLevel;
        this.actualLevel = actualLevel;
    }

    public Assessment(int userId, int skillId, String skillName, String category,
                     int requiredLevel, int actualLevel) {
        this.userId = userId;
        this.skillId = skillId;
        this.skillName = skillName;
        this.category = category;
        this.requiredLevel = requiredLevel;
        this.actualLevel = actualLevel;
        this.gapLevel = requiredLevel - actualLevel;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    public int getSkillId() { return skillId; }
    public void setSkillId(int skillId) { this.skillId = skillId; }
    public String getSkillName() { return skillName; }
    public void setSkillName(String skillName) { this.skillName = skillName; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public int getRequiredLevel() { return requiredLevel; }
    public void setRequiredLevel(int requiredLevel) { this.requiredLevel = requiredLevel; }
    public int getActualLevel() { return actualLevel; }
    public void setActualLevel(int actualLevel) { this.actualLevel = actualLevel; }
    public int getGapLevel() { return gapLevel; }
    public void setGapLevel(int gapLevel) { this.gapLevel = gapLevel; }
    public Timestamp getAssessedAt() { return assessedAt; }
    public void setAssessedAt(Timestamp assessedAt) { this.assessedAt = assessedAt; }
}

