package com.biometric.controller;

import com.biometric.dto.Result;
import com.biometric.entity.BiometricModel;
import com.biometric.service.ModelService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/models")
public class ModelController {

    @Autowired
    private ModelService modelService;

    @GetMapping
    public Result<List<BiometricModel>> list() {
        return Result.success(modelService.list());
    }

    @GetMapping("/{id}")
    public Result<BiometricModel> get(@PathVariable Long id) {
        return Result.success(modelService.getById(id));
    }

    @PostMapping
    public Result<Void> create(@RequestBody BiometricModel model) {
        modelService.create(model);
        return Result.success();
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody BiometricModel model) {
        model.setId(id);
        modelService.update(model);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        modelService.delete(id);
        return Result.success();
    }
}
