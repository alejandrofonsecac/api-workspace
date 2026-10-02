package com.workspace.api.notes;

import com.workspace.api.pages.note.domain.Note;
import com.workspace.api.pages.note.repository.NoteRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class NoteDeletionTest extends com.workspace.api.support.AuthenticatedTest {
    @Autowired MockMvc mvc;
    @Autowired NoteRepository notes;
    @Autowired EntityManager entityManager;

    @Test
    void deletesOnlyTheRequestedNoteAndReturnsNoContent() throws Exception {
        Note selected = ownedNote("Delete me");
        Note retained = ownedNote("Keep me");

        mvc.perform(delete("/notes/{id}", selected.getId()))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        entityManager.flush();
        entityManager.clear();
        assertThat(notes.findById(selected.getId())).isEmpty();
        assertThat(notes.findById(retained.getId())).isPresent();

        mvc.perform(delete("/notes/{id}", selected.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    void returnsNotFoundForMissingNoteWithoutDeletingOtherNotes() throws Exception {
        long originalCount = notes.count();

        mvc.perform(delete("/notes/{id}", UUID.randomUUID().toString()))
                .andExpect(status().isNotFound());

        assertThat(notes.count()).isEqualTo(originalCount);
    }
    private Note ownedNote(String title) {
        Note note = new Note(null, title, "Description", null, false, false, null);
        note.setUserId(testUser.getId());
        return notes.save(note);
    }
}
