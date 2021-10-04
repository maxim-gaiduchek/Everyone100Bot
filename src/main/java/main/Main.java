package main;

import datasourse.DatasourceConfig;
import datasourse.Service;
import entities.BotChat;
import entities.ChatUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.MessageEntity;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import utils.Formatter;
import utils.SimpleSender;

import java.util.*;
import java.util.stream.Stream;

public class Main extends TelegramLongPollingBot {

    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

    private static final String BOT_USERNAME = System.getenv("EVERYONE_100BOT_TELEGRAM_USERNAME");
    private static final String BOT_TOKEN = System.getenv("EVERYONE_100BOT_TELEGRAM_TOKEN");
    /*private static final String BOT_USERNAME = System.getenv("TEST_BOT_TELEGRAM_USERNAME");
    private static final String BOT_TOKEN = System.getenv("TEST_BOT_TELEGRAM_TOKEN");*/
    private static final long DEV_CHAT_ID = 505457346L;
    private static final long WAIT_TO_DELETE_MILLIS = 5000;
    private static final int USERS_TO_MENTION = 20;

    private final SimpleSender sender = new SimpleSender(BOT_TOKEN);
    private static final ApplicationContext CONTEXT = new AnnotationConfigApplicationContext(DatasourceConfig.class);
    public static final Service SERVICE = (Service) CONTEXT.getBean("service");

    private final Map<Long, Integer> chatsByChatIds = new HashMap<>();

    // setup

    private Main() {
        setChatsByChatIds();
    }

    private void setChatsByChatIds() {
        LOGGER.debug("Chats:");

        for (BotChat chat : SERVICE.getBotChats()) {
            chatsByChatIds.put(chat.getChatId(), chat.getId());

            LOGGER.debug("\t" + chat);
        }
    }

    // parsing

    @Override
    public void onUpdateReceived(Update update) {
        new Thread(() -> {
            LOGGER.debug(update.toString());

            if (update.hasMessage()) {
                parseMessage(update.getMessage());
            }
        }).start();
    }

    // message parsing

    private void parseMessage(Message message) {
        //LOGGER.debug(message.getNewChatMembers().toString());

        if (message.isCommand()) {
            parseCommand(message);
        } else if (message.isUserMessage()) {
            sendUserMessage(message.getChatId());
        }
        if (message.isGroupMessage() || message.isSuperGroupMessage()) {
            parseGroupMessage(message);
        } else if (!message.isUserMessage()) {
            sender.leaveChat(message.getChatId());
        }
    }

    private void parseCommand(Message message) {
        Long chatId = message.getChatId();
        boolean isUserMessage = message.isUserMessage();

        switch (message.getText()) {
            case "/everyone", "/everyone@Everyone100Bot" -> {
                if (isUserMessage) sendCommandCannotBeUsed(chatId);
            }
            case "/switchmute", "/switchmute@Everyone100Bot" -> {
                if (isUserMessage) {
                    sendCommandCannotBeUsed(chatId);
                } else {
                    switchMuteCommand(chatId, message.getFrom().getId(), message.getMessageId());
                }
            }
            case "/help", "/help@Everyone100Bot" -> helpCommand(chatId, isUserMessage);
            case "/donate", "/donate@Everyone100Bot" -> donateCommand(chatId);
            case "/sendstats" -> sendStatistics(chatId, isUserMessage);
            default -> {
                if (isUserMessage) sendUserMessage(chatId);
            }
        } // TODO change "/help@Everyone100Bot" to "/help@" + BOT_USERNAME
    }

    private void sendUserMessage(Long chatId) {
        String msg = """
                *Привет! Я - бот для упоминания всех пользователей в чате* (практически всех). Сначала добавь меня в твой чат. Что я буду в нем делать: напиши @everyone, @all, @here, /everyone, /everyone@Everyone100Bot или @Everyone100Bot, и я упомяну всех в чате, чтоб они обратили внимание на твое сообщение

                *Примечание:* из-за того, что Телеграм не дает ботам информацию про всех пользователей чата, я обхожу это ограничение по-другому. Я сохраняю тех юзеров, которые написали хоть раз пока я был в чате, потом их упоминаю. *Так что я не всех смогу упомянуть!*
                                
                *Команды*
                /everyone - Упомянуть всех
                /switchmute - Выкл./вкл. упоминание себя
                /help - Как пользоваться ботом
                /donate - Помочь творителю :З""";

        sender.sendStringAndInlineKeyboard(chatId, msg, getDonationKeyboard());
    }

    private void sendCommandCannotBeUsed(Long chatId) {
        String msg = """
                Добавь меня в чат, чтоб использовать эту команду :)""";

        sender.sendString(chatId, msg);
    }

    private void parseGroupMessage(Message message) {
        Long chatId = message.getChat().getId();
        Integer messageId = message.getMessageId();

        if (!chatsByChatIds.containsKey(chatId)) {
            sendFirstGroupMessage(chatId);
        }

        BotChat chat = getChat(chatId);

        addUser(chat, message.getFrom());
        if (message.isReply()) {
            addUser(chat, message.getReplyToMessage().getFrom());
        }
        if (!message.getNewChatMembers().isEmpty()) {
            addUsers(chat, message.getNewChatMembers());
        }
        try {
            if (message.getLeftChatMember() != null && !getMe().equals(message.getLeftChatMember())) {
                chat.deleteUser(message.getLeftChatMember().getId());
            } // TODO case if this bot is kicked
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }

        if (isBotCalled(message.getEntities(), message.getText())) {
            sendReply(chat, chatId, messageId);
        }

        SERVICE.saveBotChat(chat);
    }

    private void sendFirstGroupMessage(Long chatId) {
        String msg = """
                *Привет! Я - бот для упоминания всех пользователей в чате* (практически всех). Что я буду делать в чате: напиши @everyone, @all, @here, /everyone, /everyone@Everyone100Bot или @Everyone100Bot, и я упомяну всех в чате, чтоб они обратили внимание на твое сообщение

                *Примечание:* из-за того, что Телеграм не дает ботам информацию про всех пользователей чата, я обхожу это ограничение по-другому. Я сохраняю тех юзеров, которые написали хоть раз пока я был в чате, потом их упоминаю. *Так что я не всех смогу упомянуть!*
                                
                *Команды*
                /everyone - Упомянуть всех
                /switchmute - Выкл./вкл. упоминание себя
                /help - Как пользоваться ботом
                /donate - Помочь творителю :З""";

        sender.sendStringAndInlineKeyboard(chatId, msg, getDonationKeyboard());
    }

    // bot actions

    private BotChat getChat(Long chatId) {
        BotChat chat;

        if (chatsByChatIds.containsKey(chatId)) {
            chat = SERVICE.getBotChat(chatsByChatIds.get(chatId));
        } else {
            chat = new BotChat(chatId);
            SERVICE.saveBotChat(chat);
            chatsByChatIds.put(chatId, chat.getId());
        }

        return chat;
    }

    private void addUser(BotChat chat, User user) {
        if (user != null && !user.getIsBot()) chat.addUser(new ChatUser(user));
    }

    private void addUsers(BotChat chat, List<User> users) {
        users.forEach(user -> addUser(chat, user));
    }

    private boolean isBotCalled(List<MessageEntity> entities, String msg) {
        if (entities != null) {
            for (MessageEntity entity : entities) {
                if (entity.getText().equals("/everyone@" + BOT_USERNAME) ||
                    entity.getText().equals("@" + BOT_USERNAME) ||
                    entity.getText().equals("/everyone") ||
                    entity.getText().equals("@everyone")) return true;
            }
        }

        if (msg != null) {
            for (String word : msg.split(" ")) {
                if (word.equals("@all") || word.equals("@here")) return true;
            }
        }

        return false;
    }

    private void sendReply(BotChat chat, Long chatId, Integer messageId) {
        StringBuilder sb = new StringBuilder();
        List<ChatUser> muted = new ArrayList<>();
        List<ChatUser> users = new ArrayList<>();

        for (ChatUser user : chat.getUsers()) {
            if (chat.isMuted(user.getUserId())) {
                muted.add(user);
            } else {
                users.add(user);
            }
        }

        for (int i = 0; i < users.size(); i++) {
            ChatUser user = users.get(i);

            sb.append("[").append(user.getName()).append("](tg://user?id=").append(user.getUserId()).append(") ");

            if ((i + 1) % USERS_TO_MENTION == 0) {
                sender.sendString(chatId, sb.toString(), messageId);
                sb = new StringBuilder();
            }
        }

        if (!sb.isEmpty()) {
            sender.sendString(chatId, sb.toString(), messageId);
            sb = new StringBuilder();
        }

        for (ChatUser user : muted) {
            sb.append(Formatter.formatTelegramText(user.getName())).append(" ");
        }

        sb.append("_").append(users.size() - muted.size()).append(" упомянуто");
        if (muted.size() > 0) sb.append(", ").append(muted.size()).append(" не упомянуто");
        sb.append("_");

        sender.sendString(chatId, sb.toString(), messageId);
        chat.incrementCallCounter();
    }

    // commands

    private void switchMuteCommand(Long chatId, Integer userId, Integer messageId) {
        BotChat chat = getChat(chatId);
        boolean isMuted = chat.switchMute(userId);
        String msg = "Теперь я " + (isMuted ? "не " : "") + "буду вас упоминать";

        SERVICE.saveBotChat(chat);
        sender.sendString(chatId, msg);

        /*try {
            Message message = sender.sendString(chatId, msg);
            sender.deleteMessage(chatId, messageId);
            Thread.sleep(WAIT_TO_DELETE_MILLIS);
            sender.deleteMessage(chatId, message.getMessageId());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }*/

    } // TODO receive admin rights

    private void helpCommand(Long chatId, boolean isUserMessage) {
        String msg;

        if (isUserMessage) {
            msg = """
                    *Я - бот для упоминания всех пользователей в чате* (практически всех). Сначала добавь меня в твой чат. Что я буду в нем делать: напиши @everyone, @all, @here, /everyone, /everyone@Everyone100Bot или @Everyone100Bot, и я упомяну всех в чате, чтоб они обратили внимание на твое сообщение

                    *Примечание:* из-за того, что Телеграм не дает ботам информацию про всех пользователей чата, я обхожу это ограничение по-другому. Я сохраняю тех юзеров, которые написали хоть раз пока я был в чате, потом их упоминаю. *Так что я не всех смогу упомянуть!*
                                    
                    *Команды*
                    /everyone - Упомянуть всех
                    /switchmute - Выкл./вкл. упоминание себя
                    /help - Как пользоваться ботом
                    /donate - Помочь творителю :З""";
        } else {
            msg = """
                    *Я - бот для упоминания всех пользователей в чате* (практически всех). Что я буду делать в чате: напиши @everyone, @all, @here, /everyone, /everyone@Everyone100Bot или @Everyone100Bot, и я упомяну всех в чате, чтоб они обратили внимание на твое сообщение

                    *Примечание:* из-за того, что Телеграм не дает ботам информацию про всех пользователей чата, я обхожу это ограничение по-другому. Я сохраняю тех юзеров, которые написали хоть раз пока я был в чате, потом их упоминаю. *Так что я не всех смогу упомянуть!*
                                    
                    *Команды*
                    /everyone - Упомянуть всех
                    /switchmute - Выкл./вкл. упоминание себя
                    /help - Как пользоваться ботом
                    /donate - Помочь творителю :З""";
        }

        sender.sendString(chatId, msg);
    }

    private void donateCommand(Long chatId) {
        String msg = "Творитель будет рад любой мелочи <3";

        sender.sendStringAndInlineKeyboard(chatId, msg, getDonationKeyboard());
    }

    // admin commands

    private void sendStatistics(Long chatId, boolean isUserMessage) {
        if (chatId.equals(DEV_CHAT_ID)) {
            String msg = "Бота добавлено в *" + chatsByChatIds.size() + " чата(-ов)*!\n" +
                         "Ботом воспользовались *" + SERVICE.getSumOfCallCounters() + " раз(-а)*!";

            sender.sendString(DEV_CHAT_ID, msg);
        } else {
            if (isUserMessage) sendUserMessage(chatId);
        }
    }

    // keyboards

    private List<List<InlineKeyboardButton>> getDonationKeyboard() {
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        List<InlineKeyboardButton> row = new ArrayList<>();

        row.add(InlineKeyboardButton.builder().text("Monobank \uD83D\uDCB3").url("https://send.monobank.ua/6vpkHdBVeR").build());
        row.add(InlineKeyboardButton.builder().text("DonationsAlert \uD83C\uDF81").url("https://www.donationalerts.com/r/maxim_gaiduchek").build());

        keyboard.add(row);

        return keyboard;
    }

    // main

    @Override
    public String getBotUsername() {
        return BOT_USERNAME;
    }

    @Override
    public String getBotToken() {
        return BOT_TOKEN;
    }

    public static void main(String[] args) {
        try {
            TelegramBotsApi telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);

            telegramBotsApi.registerBot(new Main());
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}
