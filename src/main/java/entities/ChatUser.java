package entities;

import org.telegram.telegrambots.meta.api.objects.User;

public class ChatUser {

    private String name;
    private final Integer userId;
    // TODO isMuted

    public ChatUser(User user) {
        name = user.getFirstName();
        userId = user.getId();
    }

    public ChatUser(String[] dbInfo) {
        this.name = dbInfo[0];
        this.userId = Integer.parseInt(dbInfo[1]);
    }

    // getters

    public String getName() {
        return name;
    }

    public Integer getUserId() {
        return userId;
    }

    public String toDBString(String regex) {
        return name + regex + userId;
    }

    // setter

    public void setName(String name) {
        this.name = name;
    }

    // core

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ChatUser chatUser)) return false;

        return userId.equals(chatUser.userId);
    }

    @Override
    public int hashCode() {
        return userId.hashCode();
    }

    @Override
    public String toString() {
        return "ChatUser{" +
                "name='" + name + '\'' +
                ", userId=" + userId +
                '}';
    }
}
