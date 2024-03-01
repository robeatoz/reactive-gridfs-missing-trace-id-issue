package com.example.reactivegridfsissue.controller;

import com.example.reactivegridfsissue.model.Entity;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.ReactiveGridFsTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@Slf4j
@RestController
@RequiredArgsConstructor
public class EntityController {

    private static final String FILE_NAME = "filename.txt";

    private final ReactiveGridFsTemplate reactiveGridFsTemplate;

    private final DefaultDataBufferFactory dataBufferFactory;

    @GetMapping(path = "/workingTraceIdButAllElementsInMemory")
    private Mono<String> workingTraceIdButAllElementsInMemory() {

        log.info("Starting workingTraceIdButAllElementsInMemory");

        Mono<ObjectId> objectIdMono =
                fetchLotOfEntities()
                        .log()
                        .map(this::mapToDataBuffer)
                        .reduce(
                                (dataBuffer1, dataBuffer2) -> dataBufferFactory.join(List.of(dataBuffer1, dataBuffer2))
                        )
                        .flatMap(dataBuffer -> reactiveGridFsTemplate.store(Mono.just(dataBuffer), FILE_NAME));

        return objectIdMono
                .doOnNext(objectId -> log.info("GridFS File stored: {}", objectId))
                .flatMap(this::fetchContentOfFile);
    }

    @GetMapping(path = "/notWorkingTraceIdButOnlyFewElementsInMemory")
    private Mono<String> notWorkingTraceIdButOnlyFewElementsInMemory() {

        log.info("Starting notWorkingTraceIdButOnlyFewElementsInMemory");

        Flux<DataBuffer> dataBufferFlux =
                fetchLotOfEntities()
                        .log()
                        .map(this::mapToDataBuffer);

        Mono<ObjectId> objectIdMono = reactiveGridFsTemplate.store(dataBufferFlux, FILE_NAME);

        return objectIdMono
                .doOnNext(objectId -> log.info("GridFS File stored: {}", objectId))
                .flatMap(this::fetchContentOfFile);
    }

    @SneakyThrows
    public Mono<String> fetchContentOfFile(ObjectId objectId) {

        return reactiveGridFsTemplate.findOne(
                        Query.query(
                                Criteria
                                        .where("_id")
                                        .is(objectId)
                        )
                )
                .flatMap(reactiveGridFsTemplate::getResource)
                .flatMap(gridFsResource -> DataBufferUtils.join(gridFsResource.getContent()))
                .map(dataBuffer -> {
                    byte[] bytes = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(bytes);
                    DataBufferUtils.release(dataBuffer);
                    return new String(bytes, StandardCharsets.UTF_8);
                });
    }

    private DataBuffer mapToDataBuffer(Entity entity) {
        String id = entity.getId() + "\n";
        return dataBufferFactory.wrap(id.getBytes());
    }

    private Flux<Entity> fetchLotOfEntities() {
        Stream<Entity> entityStream =
                IntStream
                        .rangeClosed(1, 5)
                        .mapToObj(String::valueOf)
                        .map(Entity::new);

        return Flux.fromStream(entityStream);
    }
}
