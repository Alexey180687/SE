package searchengine.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.dto.statistics.BadRequest;
import searchengine.dto.statistics.Response;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.services.IndexingService;
import searchengine.services.SearchService;
import searchengine.services.StatisticsService;

@RestController
@RequestMapping("/api")
@Slf4j
@Tag(name = "Search engine API controller", description = "Whole site indexing, certain site reindexing, "
        + "stop indexing, search, site statistics")
public class ApiController {

    private final StatisticsService statisticsService;
    private final IndexingService indexingService;
    private final SearchService searchService;

    public ApiController(StatisticsService statisticsService, IndexingService indexingService, SearchService searchService) {
        this.statisticsService = statisticsService;
        this.indexingService = indexingService;
        this.searchService = searchService;
    }

    @GetMapping("/statistics")
    @Operation(summary = "Get site statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }

    @GetMapping("/startIndexing")
    @Operation(summary = "Site indexing")
    public ResponseEntity<Object> startIndexing() {
        if (indexingService.indexingAll()) {
            return new ResponseEntity<>(new Response(true), HttpStatus.OK);
        } else {
            return new ResponseEntity<>(new BadRequest(false, "Indexing not started"), HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("/stopIndexing")
    @Operation(summary = "Stop indexing")
    public ResponseEntity<Object> stopIndexing() {
        if (indexingService.stopIndexing()) {
            return new ResponseEntity<>(new Response(true), HttpStatus.OK);
        } else {
            return new ResponseEntity<>(new BadRequest(false, "Indexing was not stopped because it was not started"), HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("/search")
    @Operation(summary = "Search")
    public ResponseEntity<Object> search(@RequestParam(name = "query", required = false, defaultValue = "") String request,
                                         @RequestParam(name = "site", required = false, defaultValue = "") String site,
                                         @RequestParam(name = "offset", required = false, defaultValue = "0") int offset,
                                         @RequestParam(name = "limit", required = false, defaultValue = "20") int limit) {
        if (request.isEmpty()) {
            return new ResponseEntity<>(new BadRequest(false, "Empty request"), HttpStatus.BAD_REQUEST);
        }
        Object searchResponse = searchService.executeSearch(request, site, offset, limit);
        return (searchResponse instanceof BadRequest) ?
                new ResponseEntity<>(searchResponse, HttpStatus.BAD_REQUEST) :
                new ResponseEntity<>(searchResponse, HttpStatus.OK);
    }

    @PostMapping("/indexPage")
    @Operation(summary = "Certain page indexing")
    public ResponseEntity<Object> indexPage(@RequestParam(name = "url") String url) {
        if (url.isEmpty()) {
            log.info("Page not specified");
            return new ResponseEntity<>(new BadRequest(false, "Page not specified"), HttpStatus.BAD_REQUEST);
        } else {
            if (indexingService.urlIndexing(url)) {
                log.info("Page - " + url + " - added for reindexing");
                return new ResponseEntity<>(new Response(true), HttpStatus.OK);
            } else {
                log.info("Required page out of configuration file");
                return new ResponseEntity<>(new BadRequest(false, "Required page out of configuration file"), HttpStatus.BAD_REQUEST);
            }
        }
    }
}