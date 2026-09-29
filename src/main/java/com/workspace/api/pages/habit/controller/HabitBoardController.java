package com.workspace.api.pages.habit.controller;
import com.workspace.api.pages.habit.dto.HabitDraftDTO;
import com.workspace.api.pages.habit.dto.board.HabitBoardDTO;
import com.workspace.api.pages.habit.dto.board.HabitBoardViewDTO;
import com.workspace.api.pages.habit.service.HabitBoardService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;

import java.util.List;

@RestController
@RequestMapping("/habits")
@RequiredArgsConstructor
public class HabitBoardController {
    private final HabitBoardService habits;

    @GetMapping("/board") public HabitBoardDTO board() {
        return habits.board();
    }

    @GetMapping public List<HabitBoardViewDTO> list() {
        return habits.board().habits();
    }

    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public HabitBoardViewDTO create(@Valid @RequestBody HabitDraftDTO draft) {
        return habits.create(draft);
    }

    @PutMapping("/{id}")
    public HabitBoardViewDTO update(@PathVariable Integer id,@Valid @RequestBody HabitDraftDTO draft) {
        return habits.update(id,draft);
    }
    public record StatusRequest(@NotNull @Pattern(regexp="active|paused") String status) {}

    @PutMapping("/{id}/status")
    public HabitBoardViewDTO status(@PathVariable Integer id,@Valid @RequestBody StatusRequest request) {
        return habits.status(id,request.status());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Integer id) {
        habits.delete(id);
    }
}
