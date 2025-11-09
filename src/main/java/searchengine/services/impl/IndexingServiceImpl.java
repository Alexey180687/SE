package searchengine.services.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.model.Site;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;
import searchengine.services.IndexingService;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@RequiredArgsConstructor
public class IndexingServiceImpl implements IndexingService {

    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;

    private ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    @Override
    public boolean urlIndexing(String url) {
        Site site = siteRepository.findByUrl(url);
        if (site == null) {
            site = new Site();
            site.setUrl(url);
            site.setStatus(Site.Status.INDEXING);
            siteRepository.save(site);
        }

        // Стартуем задачу индексации асинхронно
        executorService.submit(() -> {
            // Здесь твоя логика индексации
        });

        return true;
    }

    @Override
    public boolean indexingAll() {
        // Логи для полной индексации всех сайтов
        return true;
    }

    @Override
    public boolean stopIndexing() {
        // Реализация метода остановки
        return true;
    }
}