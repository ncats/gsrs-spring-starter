package gsrs.controller;

import gsrs.buildInfo.BuildInfo;
import gsrs.buildInfo.BuildInfoFetcher;
import ix.core.util.pojopointer.LambdaParseRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@RestController
public class BuildInfoController {
@Autowired

//    private LambdaParseRegistry lambdaParseRegistry;
//    @Autowired

    private BuildInfoFetcher buildInfoFetcher;

    @GetMapping("/api/v1/buildInfo")
    public BuildInfo getBuildInfo(){
//        lambdaParseRegistry.printData();
        return buildInfoFetcher.getBuildInfo();
    }



}
