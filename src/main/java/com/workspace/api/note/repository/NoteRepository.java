package com.workspace.api.note.repository;

import java.util.Optional;
import java.util.List;
import com.workspace.api.note.domain.Note;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NoteRepository extends JpaRepository<Note, String> {
    List<Note> findAllByUserId(String userId);
    Optional<Note> findByIdAndUserId(String id, String userId);
}
