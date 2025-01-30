package searchengine.services;

import searchengine.dto.statistics.BadRequest;
import searchengine.dto.statistics.SearchResults;
import searchengine.dto.statistics.StatisticsSearch;
import searchengine.repository.SiteRepository;

import java.util.List;

public interface SearchService {

    public default Object executeSearch(String request, String site, int offset, int limit) {
        if (!site.isEmpty()) {
            SiteRepository siteRepository = null;
            if (siteRepository.findByUrl(site) == null) {
                return new BadRequest(false, "Required page not found");
            }
            List<StatisticsSearch> searchData = siteSearch(request, site, offset, limit);
            return new SearchResults(true, searchData.size(), searchData);
        } else {
            List<StatisticsSearch> searchData = allSiteSearch(request, offset, limit);
            return new SearchResults(true, searchData.size(), searchData);
        }
    }
    List<StatisticsSearch> allSiteSearch(String text, int offset, int limit);
    List<StatisticsSearch> siteSearch(String searchText, String url, int offset, int limit);
}