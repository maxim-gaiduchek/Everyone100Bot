package datasourse;

import datasourse.repositories.BotChatRepository;
import entities.BotChat;

import java.util.List;

public class JpaRepositoriesService implements Service {

    private final BotChatRepository repository;

    JpaRepositoriesService(BotChatRepository repository) {
        this.repository = repository;
    }

    @Override
    public BotChat getBotChat(int id) {
        return repository.findById(id).orElse(null);
    }

    @Override
    public List<BotChat> getBotChats() {
        return repository.findAll();
    }

    @Override
    public void saveBotChat(BotChat chat) {
        System.out.println(repository.save(chat));
    }

    @Override
    public void updateBotChatUserName(BotChat chat) {
        repository.updateUserName(chat.getId(), chat.getUsersMap());
    }

    @Override
    public int getSumOfCallCounters() {
        return repository.getSumOfCallCounters();
    }
}
