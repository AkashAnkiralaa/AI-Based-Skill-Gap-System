package org.example.model;

public class Resource {
    private int id;
    private int skillId;
    private String title;
    private String resourceUrl;
    private String resourceType;

    public Resource() {}

    public Resource(int id, int skillId, String title, String resourceUrl, String resourceType) {
        this.id = id;
        this.skillId = skillId;
        this.title = title;
        this.resourceUrl = resourceUrl;
        this.resourceType = resourceType;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getSkillId() { return skillId; }
    public void setSkillId(int skillId) { this.skillId = skillId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getResourceUrl() { return resourceUrl; }
    public void setResourceUrl(String resourceUrl) { this.resourceUrl = resourceUrl; }

    public String getResourceType() { return resourceType; }
    public void setResourceType(String resourceType) { this.resourceType = resourceType; }
}

