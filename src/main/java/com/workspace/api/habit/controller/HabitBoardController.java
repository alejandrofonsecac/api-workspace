package com.workspace.api.habit.controller;
import com.workspace.api.habit.dto.HabitDraftDTO;
import com.workspace.api.habit.service.HabitBoardService;
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

    @GetMapping("/board") public HabitBoardService.Board board() {
        return habits.board();
    }

    @GetMapping public List<HabitBoardService.View> list() {
        return habits.board().habits();
    }

    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public HabitBoardService.View create(@Valid @RequestBody HabitDraftDTO draft) {
        return habits.create(draft);
    }

    @PutMapping("/{id}")
    public HabitBoardService.View update(@PathVariable Integer id,@Valid @RequestBody HabitDraftDTO draft) {
        return habits.update(id,draft);
    }
    public record StatusRequest(@NotNull @Pattern(regexp="active|paused") String status) {}

    @PutMapping("/{id}/status")
    public HabitBoardService.View status(@PathVariable Integer id,@Valid @RequestBody StatusRequest request) {
        return habits.status(id,request.status());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Integer id) {
        habits.delete(id);
    }
}

