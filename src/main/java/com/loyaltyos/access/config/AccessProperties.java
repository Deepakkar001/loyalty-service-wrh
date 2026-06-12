package com.loyaltyos.access.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "loyaltyos.access")
public class AccessProperties {

    private DynamicNav dynamicNav = new DynamicNav();
    private Migration migration = new Migration();
    private Permissions permissions = new Permissions();

    public DynamicNav getDynamicNav() { return dynamicNav; }
    public void setDynamicNav(DynamicNav dynamicNav) { this.dynamicNav = dynamicNav; }
    public Migration getMigration() { return migration; }
    public void setMigration(Migration migration) { this.migration = migration; }
    public Permissions getPermissions() { return permissions; }
    public void setPermissions(Permissions permissions) { this.permissions = permissions; }

    public static class DynamicNav {
        private boolean enabled = false;
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
    }

    public static class Migration {
        private boolean runOnStartup = true;
        public boolean isRunOnStartup() { return runOnStartup; }
        public void setRunOnStartup(boolean runOnStartup) { this.runOnStartup = runOnStartup; }
    }

    public static class Permissions {
        private boolean enforce = false;
        public boolean isEnforce() { return enforce; }
        public void setEnforce(boolean enforce) { this.enforce = enforce; }
    }
}
