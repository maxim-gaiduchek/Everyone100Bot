package datasourse.repositories;

import entities.BotChat;
import entities.ChatUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import javax.transaction.Transactional;
import java.util.Map;

public interface BotChatRepository extends JpaRepository<BotChat, Integer> {

    @Modifying
    @Transactional
    @Query("UPDATE BotChat chat SET chat.users = ?2 WHERE chat.id = ?1")
    void updateUserName(int id, Map<Long, ChatUser> users);

    @Query("SELECT SUM(chat.callCounter) FROM BotChat chat")
    int getSumOfCallCounters();
}
