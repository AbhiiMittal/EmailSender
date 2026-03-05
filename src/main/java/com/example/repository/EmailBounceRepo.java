package com.example.repository;

import com.example.entity.EmailBounce;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmailBounceRepo extends JpaRepository<EmailBounce,Long> {

    @Query("SELECT u.emailId FROM EmailBounce u WHERE u.emailId IN :emails")
    List<String> findEmailAddressesIn(@Param("emails") List<String> emails);
}
