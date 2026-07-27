package com.workspace.api.notes;

import com.workspace.api.note.domain.Note;
import com.workspace.api.note.repository.NoteRepository;
import lombok.AllArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@AllArgsConstructor
public class GetTest implements CommandLineRunner {
    private final NoteRepository noteRepository;

    @Override
    public void run(String @NonNull ... strings) {
        noteRepository.deleteAll();

        Note note1 = new Note(null, "Teste Nota 1", "Teste Nota fodona1", null, null, null, null);
        Note note2 = new Note(null, "Teste Nota 2", "Teste Nota fodona2", null, null, null, null);
        Note note3 = new Note(null, "Teste Nota 3", "Teste Nota fodona3", null, null, null, null);

        noteRepository.saveAll(List.of(note1, note2, note3));
    }
}
