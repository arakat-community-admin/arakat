package io.github.arakat.arakatcommunity.controller;

import io.github.arakat.arakatcommunity.exception.GraphNotFoundException;
import io.github.arakat.arakatcommunity.exception.GraphRunFailedException;
import io.github.arakat.arakatcommunity.model.BaseResponse;
import io.github.arakat.arakatcommunity.service.GraphService;
import io.github.arakat.arakatcommunity.utils.ApiResponseUtils;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
public class GraphController {

    private GraphService graphService;

    @Autowired
    public GraphController(GraphService graphService) {
        this.graphService = graphService;
    }

    @RequestMapping(value = "/run-graph", method = RequestMethod.POST, consumes = "application/json", produces = "application/json")
    public ResponseEntity<BaseResponse> runGraph(@RequestBody String graph) {
        try {
            JSONObject graphWithConfigs = graphService.addConfigToDagProperties(graph);

            System.out.println(graphWithConfigs);
            String responseFromCore = graphService.postGraphAndDagPropsToCore(graphWithConfigs.toString());
            graphService.checkRunResult(responseFromCore);
            graphService.saveGraph(graph);
            graphService.saveWrittenTablesToDatabase(responseFromCore);
            graphService.sendGeneratedCodeToAirflow(responseFromCore);

            return ApiResponseUtils.createResponseEntity(200,
                    "Spark and Airflow scripts are successfully generated, you can check the results from the ResultsView page.",
                    "Spark and Airflow scripts are successfully generated, you can check the results from the ResultsView page.",
                    null, HttpStatus.OK);

        } catch (GraphRunFailedException e) {
            return ApiResponseUtils.createResponseEntity(400,
                    e.getMessage(),
                    e.getMessage(),
                    null, HttpStatus.BAD_REQUEST);
        } catch (IOException e) {
            return ApiResponseUtils.createResponseEntity(500,
                    e.getMessage(),
                    e.getMessage(),
                    null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @RequestMapping(value = "/get-graph/{graphId}", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<BaseResponse> getGraphById(@PathVariable String graphId) {
        try {
            JSONObject returnedGraph = graphService.getGraphById(graphId);
            JSONObject graphWithConfigs = graphService.addConfigToDagProperties(returnedGraph.toString());

            return ApiResponseUtils.createResponseEntity(200,
                    "Get graph by id",
                    "Get graph by id",
                    graphWithConfigs.toString(), HttpStatus.OK);
        } catch (GraphNotFoundException e) {
            return ApiResponseUtils.createResponseEntity(404,
                    e.getMessage(),
                    e.getMessage(),
                    null, HttpStatus.NOT_FOUND);
        }
    }
}
