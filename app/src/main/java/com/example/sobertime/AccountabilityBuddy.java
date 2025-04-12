package main.java.com.example.sobertime;

public class AccountabilityBuddy {
    private long id;
    private String name;
    private String phone;
    private boolean enabled;
    private boolean notifyOnCheckin;
    private boolean notifyOnRelapse;
    private boolean notifyOnMilestone;

    // Getters and setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    
    public boolean isNotifyOnCheckin() { return notifyOnCheckin; }
    public void setNotifyOnCheckin(boolean notify) { this.notifyOnCheckin = notify; }
    
    public boolean isNotifyOnRelapse() { return notifyOnRelapse; }
    public void setNotifyOnRelapse(boolean notify) { this.notifyOnRelapse = notify; }
    
    public boolean isNotifyOnMilestone() { return notifyOnMilestone; }
    public void setNotifyOnMilestone(boolean notify) { this.notifyOnMilestone = notify; }
}