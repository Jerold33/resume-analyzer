package com.example.resumeanalyzer.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.resumeanalyzer.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
class UserRepositoryTest {

    @Autowired
    UserRepository userRepository;

    @Test
    void saveAndFindByEmail() {
        User u = new User("repo@example.com", "encoded");
        userRepository.save(u);

        assertThat(userRepository.findByEmail("repo@example.com")).isPresent();
        assertThat(userRepository.existsByEmail("repo@example.com")).isTrue();
    }
}
