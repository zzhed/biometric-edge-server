package com.biometric.controller;

import com.biometric.dto.Result;
import com.biometric.entity.ProtectionMethod;
import com.biometric.service.ProtectionMethodService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/protection-methods")
public class ProtectionMethodController {

    @Autowired
    private ProtectionMethodService protectionMethodService;

    @GetMapping
    public Result<List<ProtectionMethod>> list() {
        return Result.success(protectionMethodService.list());
    }

    @GetMapping("/{id}")
    public Result<ProtectionMethod> get(@PathVariable Long id) {
        return Result.success(protectionMethodService.getById(id));
    }

    @PostMapping
    public Result<Void> create(@RequestBody ProtectionMethod method) {
        protectionMethodService.create(method);
        return Result.success();
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody ProtectionMethod method) {
        method.setId(id);
        protectionMethodService.update(method);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        protectionMethodService.delete(id);
        return Result.success();
    }
}
