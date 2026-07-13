package com.procurementsaas.identity.web;

import com.procurementsaas.identity.dto.Dtos.FeatureDto;
import com.procurementsaas.identity.repo.FeatureRepository;
import com.procurementsaas.identity.service.IdentityMapper;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/features")
public class FeatureController {

    private final FeatureRepository featureRepository;

    public FeatureController(FeatureRepository featureRepository) {
        this.featureRepository = featureRepository;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('FEATURE_FEATURE_VIEW')")
    public List<FeatureDto> list() {
        return featureRepository.findAll().stream().map(IdentityMapper::toDto).toList();
    }
}
