package datasourse;

import entities.BotChat;

import java.util.List;

public interface Service {

    BotChat getBotChat(int id);

    List<BotChat> getBotChats();

    void saveBotChat(BotChat chat);

    void updateBotChatUserName(BotChat chat);

    int getSumOfCallCounters();
}
