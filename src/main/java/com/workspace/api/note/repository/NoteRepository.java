package com.workspace.api.note.repository;

import java.util.Optional;
import java.util.List;
import com.workspace.api.note.domain.Note;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import jakarta.persistence.LockModeType;
import org.springframework.stereotype.Repository;

@Repository
public interface NoteRepository extends JpaRepository<Note, String> {
    List<Note> findAllByUserId(String userId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select item from Note item where item.id = :id and item.userId = :userId")
    Optional<Note> findForUpdateByIdAndUserId(String id, String userId);

    Optional<Note> findByIdAndUserId(String id, String userId);
}
