package com.example.repository;

import com.example.entity.UnsubscribedUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UnsubscribedUserRepo extends JpaRepository<UnsubscribedUser,Long> {
    boolean existsByEmailAddress(String emailAddress);

    @Query("SELECT u.emailAddress FROM UnsubscribedUser u WHERE u.emailAddress IN :emails")
    List<String> findEmailAddressesIn(@Param("emails") List<String> emails);
}
