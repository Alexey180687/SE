package searchengine.parsers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import searchengine.morphology.Morphology;
import searchengine.repository.PageRepository;

@Component
@RequiredArgsConstructor
@Slf4j
public class LemmaCreationTask {
    private final PageRepository pageRepository;
    private final Morphology morphology;

}