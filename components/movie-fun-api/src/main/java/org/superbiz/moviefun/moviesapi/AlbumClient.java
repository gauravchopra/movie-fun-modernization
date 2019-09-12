package org.superbiz.moviefun.moviesapi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.RestOperations;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

import static org.springframework.http.HttpMethod.GET;

public class AlbumClient {

    private String albumUrl;
    private RestOperations restOperations;
    private Logger logger= LoggerFactory.getLogger(this.getClass());

    private static ParameterizedTypeReference<List<AlbumInfo>> albumListType = new ParameterizedTypeReference<List<AlbumInfo>>() {
    };

    public AlbumClient(String albumUrl, RestOperations restOperations) {
        this.albumUrl = albumUrl;
        this.restOperations = restOperations;
    }

    public void addAlbum(AlbumInfo album) {
        logger.info("Url to hit::"+albumUrl);
        restOperations.postForEntity(albumUrl, album, AlbumInfo.class);
    }

    public AlbumInfo find(Long albumId){
        return restOperations.getForObject(albumUrl+"/"+albumId, AlbumInfo.class);
    }

    public void deleteAlbumId(Long albumId) {
        restOperations.delete(albumUrl + "/" + albumId);
    }

    public int countAll() {
        return restOperations.getForObject(albumUrl + "/count", Integer.class);
    }


    public int count(String field, String key) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(albumUrl + "/count")
                .queryParam("field", field)
                .queryParam("key", key);

        return restOperations.getForObject(builder.toUriString(), Integer.class);
    }


    public List<AlbumInfo> findAll(int start, int pageSize) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(albumUrl)
                .queryParam("start", start)
                .queryParam("pageSize", pageSize);

        return restOperations.exchange(builder.toUriString(), GET, null, albumListType).getBody();
    }

    public List<AlbumInfo> findRange(String field, String key, int start, int pageSize) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(albumUrl)
                .queryParam("field", field)
                .queryParam("key", key)
                .queryParam("start", start)
                .queryParam("pageSize", pageSize);

        return restOperations.exchange(builder.toUriString(), GET, null, albumListType).getBody();
    }

    public List<AlbumInfo> getAlbums() {
        return restOperations.exchange(albumUrl, GET, null, albumListType).getBody();
    }
}