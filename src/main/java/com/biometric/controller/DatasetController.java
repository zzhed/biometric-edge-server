package com.biometric.controller;

import com.biometric.dto.Result;
import com.biometric.entity.DatasetInfo;
import com.biometric.service.DatasetService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/datasets")
public class DatasetController {

    @Autowired
    private DatasetService datasetService;

    @GetMapping
    public Result<List<DatasetInfo>> list() {
        return Result.success(datasetService.list());
    }

    @GetMapping("/{id}")
    public Result<DatasetInfo> get(@PathVariable Long id) {
        return Result.success(datasetService.getById(id));
    }

    @PostMapping
    public Result<Void> create(@RequestBody DatasetInfo dataset) {
        datasetService.create(dataset);
        return Result.success();
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody DatasetInfo dataset) {
        dataset.setId(id);
        datasetService.update(dataset);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        datasetService.delete(id);
        return Result.success();
    }
}
