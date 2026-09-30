package com.project.optrabidz.notification.application.channel;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "optrabidz.notification.channels")
public class NotificationChannelProperties {
    /** Email notification channel settings. */
    private final ChannelSwitch email = new ChannelSwitch();

    /** Push notification channel settings. */
    private final ChannelSwitch push = new ChannelSwitch();

    public ChannelSwitch getEmail() {
        return email;
    }

    public ChannelSwitch getPush() {
        return push;
    }

    public static final class ChannelSwitch {
        /** Whether the notification channel is enabled. */
        private boolean enabled = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }
}
