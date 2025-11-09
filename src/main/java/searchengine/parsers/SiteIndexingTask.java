package searchengine.parsers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import searchengine.config.SitesList;
import searchengine.dto.statistics.StatisticsIndex;
import searchengine.dto.statistics.StatisticsLemma;
import searchengine.dto.statistics.StatisticsPage;
import searchengine.model.IndexSearch;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.repository.IndexSearchRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;

import java.util.Date;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ForkJoinPool;

@RequiredArgsConstructor
@Slf4j
public class SiteIndexingTask implements Runnable {

    private final PageRepository pageRepository;
    private final SiteRepository siteRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexSearchRepository indexSearchRepository;
    private final LemmaParser lemmaParser;
    private final IndexParser indexParser;
    private final String url;
    private final SitesList sitesList;

    private final int coreAmount = Runtime.getRuntime().availableProcessors();

    private List<StatisticsPage> getPageDtoList() throws InterruptedException {
        try {
            if (!Thread.interrupted()) {
                String urlFormat = url + "/";
                List<StatisticsPage> statisticsPages = new CopyOnWriteArrayList<>();
                List<String> urls = new CopyOnWriteArrayList<>();
                ForkJoinPool forkJoinPool = new ForkJoinPool(coreAmount);
                List<StatisticsPage> result = forkJoinPool.invoke(new UrlParser(urlFormat, statisticsPages, urls));
                return result;
            } else {
                throw new InterruptedException();
            }
        } catch (InterruptedException e) {
            log.error("Interrupted during page parsing", e);
            throw e;
        }
    }

    @Override
    public void run() {
        try {
            if (siteRepository.existsByUrl(url)) {
                log.info("Starting deletion of old site data - {}", url);
                deleteDataFromSite();
            }

            log.info("Indexing started for {} at thread {}", url, Thread.currentThread().getName());
            saveDateSite();

            // Получаем список страниц
            List<StatisticsPage> statisticsPageList = getPageDtoList();
            saveToBase(statisticsPageList);

            // Парсим леммы и добавляем их в базу
            getLemmasPage();

            // Индексируем найденные слова
            indexingWords();

            log.info("Indexing completed for {}", url);
        } catch (InterruptedException ex) {
            log.error("Indexing interrupted for {}", url);
            errorSite();
        }
    }

    private void getLemmasPage() {
        if (!Thread.interrupted()) {
            Site sitePage = siteRepository.findByUrl(url);
            sitePage.setStatusTime(new Date());
            lemmaParser.run(sitePage);
            List<StatisticsLemma> statisticsLemmaList = lemmaParser.getLemmaDtoList();
            List<Lemma> lemmaList = new CopyOnWriteArrayList<>();
            for (StatisticsLemma stats : statisticsLemmaList) {
                lemmaList.add(new Lemma(stats.getLemma(), stats.getFrequency(), sitePage));
            }
            lemmaRepository.flush();
            lemmaRepository.saveAll(lemmaList);
        } else {
            throw new RuntimeException("Parsing interrupted!");
        }
    }

    private void saveToBase(List<StatisticsPage> pages) throws InterruptedException {
        if (!Thread.interrupted()) {
            List<Page> pageList = new CopyOnWriteArrayList<>();
            Site site = siteRepository.findByUrl(url);
            for (StatisticsPage statPage : pages) {
                int first = statPage.getUrl().indexOf(url) + url.length();
                String formattedPath = statPage.getUrl().substring(first);
                pageList.add(new Page(site, formattedPath, statPage.getCode(), statPage.getContent()));
            }
            pageRepository.flush();
            pageRepository.saveAll(pageList);
        } else {
            throw new InterruptedException();
        }
    }

    private void deleteDataFromSite() {
        Site sitePage = siteRepository.findByUrl(url);
        sitePage.setStatus(Site.Status.INDEXING);
        sitePage.setName(getName());
        sitePage.setStatusTime(new Date());
        siteRepository.save(sitePage);
        siteRepository.flush();
        siteRepository.delete(sitePage);
    }

    private void indexingWords() throws InterruptedException {
        if (!Thread.interrupted()) {
            Site sitePage = siteRepository.findByUrl(url);
            indexParser.run(sitePage);
            List<StatisticsIndex> statisticsIndexList = new CopyOnWriteArrayList<>(indexParser.getIndexList());
            List<IndexSearch> indexList = new CopyOnWriteArrayList<>();
            sitePage.setStatusTime(new Date());
            for (StatisticsIndex stats : statisticsIndexList) {
                Page page = pageRepository.getReferenceById(stats.getPageID());
                Lemma lemma = lemmaRepository.getReferenceById(stats.getLemmaID());
                indexList.add(new IndexSearch(page, lemma, stats.getRank()));
            }
            indexSearchRepository.flush();
            indexSearchRepository.saveAll(indexList);
            log.info("Indexing words complete for {}", url);
            sitePage.setStatus(Site.Status.INDEXED);
            siteRepository.save(sitePage);
        } else {
            throw new InterruptedException();
        }
    }

    private void saveDateSite() {
        Site sitePage = new Site();
        sitePage.setUrl(url);
        sitePage.setName(getName());
        sitePage.setStatus(Site.Status.INDEXING);
        sitePage.setStatusTime(new Date());
        siteRepository.flush();
        siteRepository.save(sitePage);
    }

    private String getName() {
        List<searchengine.config.Site> sites = sitesList.getSites();
        for (searchengine.config.Site s : sites) {
            if (s.getUrl().equals(url)) {
                return s.getName();
            }
        }
        return "";
    }

    private void errorSite() {
        Site sitePage = new Site();
        sitePage.setLastError("Indexing was stopped");
        sitePage.setStatus(Site.Status.FAILED);
        sitePage.setStatusTime(new Date());
        siteRepository.save(sitePage);
    }
}