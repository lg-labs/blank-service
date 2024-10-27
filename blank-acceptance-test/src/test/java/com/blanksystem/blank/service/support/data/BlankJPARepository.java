package com.blanksystem.blank.service.support.data;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface BlankJPARepository extends JpaRepository<BlankEntity, UUID> {

}
