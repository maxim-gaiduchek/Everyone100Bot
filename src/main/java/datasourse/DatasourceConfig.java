package datasourse;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import datasourse.repositories.BotChatRepository;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import javax.sql.DataSource;

@Configuration
@EnableAutoConfiguration
@EnableJpaRepositories
@EntityScan("/entities")
public class DatasourceConfig {

    private static final String JDBC_URL = System.getenv("EVERYONE_100BOT_JDBC_URL");
    private static final String JDBC_USERNAME = System.getenv("EVERYONE_100BOT_JDBC_USERNAME");
    private static final String JDBC_PASSWORD = System.getenv("EVERYONE_100BOT_JDBC_PASSWORD");
    private static final int JDBC_MAX_CONNECTION_POOL = 5;

    @Bean
    public DataSource dataSource() {
        HikariConfig dataSourceConfig = new HikariConfig();

        dataSourceConfig.setJdbcUrl(JDBC_URL);
        dataSourceConfig.setDriverClassName("org.postgresql.Driver");
        dataSourceConfig.setUsername(JDBC_USERNAME);
        dataSourceConfig.setPassword(JDBC_PASSWORD);
        dataSourceConfig.setMaximumPoolSize(JDBC_MAX_CONNECTION_POOL);

        return new HikariDataSource(dataSourceConfig);
    }

    @Bean(name = "service")
    public Service service(BotChatRepository repository) {
        return new JpaRepositoriesService(repository);
    }
}
