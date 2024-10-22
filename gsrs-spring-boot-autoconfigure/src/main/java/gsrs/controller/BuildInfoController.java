package gsrs.controller;

import gsrs.buildInfo.BuildInfo;
import gsrs.buildInfo.BuildInfoFetcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BuildInfoController {

    @Autowired
    private BuildInfoFetcher buildInfoFetcher;

    @GetMapping("/api/v1/buildInfo")
    public BuildInfo getBuildInfo(){
        return buildInfoFetcher.getBuildInfo();
    }
}