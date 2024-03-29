package org.superbiz.moviefun.albums;

import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.superbiz.moviefun.CsvUtils;
import org.superbiz.moviefun.blobstore.Blob;
import org.superbiz.moviefun.blobstore.BlobStore;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static com.fasterxml.jackson.dataformat.csv.CsvSchema.ColumnType;
import static com.fasterxml.jackson.dataformat.csv.CsvSchema.builder;

@Service
public class AlbumsUpdater {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final ObjectReader objectReader;
    private final BlobStore blobStore;
    private final AlbumsRepository albumsBean;

    public AlbumsUpdater(BlobStore blobStore, AlbumsRepository albumsBean) {
        this.blobStore = blobStore;
        this.albumsBean = albumsBean;

        CsvSchema schema = builder()
            .addColumn("artist")
            .addColumn("title")
            .addColumn("year", ColumnType.NUMBER)
            .addColumn("rating", ColumnType.NUMBER)
            .build();

        objectReader = new CsvMapper().readerFor(Album.class).with(schema);
    }

    public void update() throws IOException {
        Optional<Blob> maybeBlob = blobStore.get("albums.csv");

        if (!maybeBlob.isPresent()) {
            logger.info("No albums.csv found when running AlbumsUpdater!");
            return;
        }

        List<Album> albumsToHave = CsvUtils.readFromCsv(objectReader, maybeBlob.get().inputStream);
        List<Album> albumsWeHave = albumsBean.getAlbums();

        createNewAlbums(albumsToHave, albumsWeHave);
        deleteOldAlbums(albumsToHave, albumsWeHave);
        updateExistingAlbums(albumsToHave, albumsWeHave);
    }


    private void createNewAlbums(List<Album> albumsToHave, List<Album> albumsWeHave) {
        Stream<Album> albumsToCreate = albumsToHave
            .stream()
            .filter(album -> albumsWeHave.stream().noneMatch(album::isEquivalent));

        albumsToCreate.forEach(albumsBean::addAlbum);
    }

    private void deleteOldAlbums(List<Album> albumsToHave, List<Album> albumsWeHave) {
        Stream<Album> albumsToDelete = albumsWeHave
            .stream()
            .filter(album -> albumsToHave.stream().noneMatch(album::isEquivalent));

        albumsToDelete.forEach(albumsBean::deleteAlbum);
    }

    private void updateExistingAlbums(List<Album> albumsToHave, List<Album> albumsWeHave) {
        Stream<Album> albumsToUpdate = albumsToHave
            .stream()
            .map(album -> addIdToAlbumIfExists(albumsWeHave, album))
            .filter(Album::hasId);

        albumsToUpdate.forEach(albumsBean::updateAlbum);
    }

    private Album addIdToAlbumIfExists(List<Album> existingAlbums, Album album) {
        Optional<Album> maybeExisting = existingAlbums.stream().filter(album::isEquivalent).findFirst();
        maybeExisting.ifPresent(existing -> album.setId(existing.getId()));
        return album;
    }
}
