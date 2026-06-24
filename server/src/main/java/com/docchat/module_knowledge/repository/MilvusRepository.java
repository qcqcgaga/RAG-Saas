package com.docchat.module_knowledge.repository;

import io.milvus.v2.client.MilvusClientV2;
import io.milvus.v2.common.DataType;
import io.milvus.v2.common.IndexParam;
import io.milvus.v2.service.collection.request.CreateCollectionReq;
import io.milvus.v2.service.collection.request.AddFieldReq;
import io.milvus.v2.service.collection.request.HasCollectionReq;
import io.milvus.v2.service.vector.request.DeleteReq;
import io.milvus.v2.service.vector.request.InsertReq;
import io.milvus.v2.service.vector.request.SearchReq;
import io.milvus.v2.service.vector.request.data.FloatVec;
import io.milvus.v2.service.vector.response.SearchResp;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Repository
@RequiredArgsConstructor
public class MilvusRepository {

    private final MilvusClientV2 milvusClient;

    private static final int EMBEDDING_DIM = 1536;
    private static final String CHUNK_ID_FIELD = "chunk_id";
    private static final String DOCUMENT_ID_FIELD = "document_id";
    private static final String DOCUMENT_NAME_FIELD = "document_name";
    private static final String CHUNK_INDEX_FIELD = "chunk_index";
    private static final String CONTENT_FIELD = "content";
    private static final String EMBEDDING_FIELD = "embedding";

    /** 获取租户 collection 名称 */
    public String getCollectionName(Long tenantId) {
        return "docchat_vectors_" + tenantId;
    }

    /** 创建租户 collection */
    public void createCollection(String collectionName) {
        if (collectionExists(collectionName)) {
            log.info("Collection {} 已存在", collectionName);
            return;
        }

        var schema = buildSchema();
        var indexParams = buildIndexParams();

        milvusClient.createCollection(CreateCollectionReq.builder()
                .collectionName(collectionName)
                .collectionSchema(schema)
                .indexParams(indexParams)
                .build());

        log.info("创建 Milvus collection: {}", collectionName);
    }

    /** 插入向量数据 */
    public void insert(String collectionName, List<JsonObject> data) {
        milvusClient.insert(InsertReq.builder()
                .collectionName(collectionName)
                .data(data)
                .build());
        log.info("插入 {} 条向量到 {}", data.size(), collectionName);
    }

    /** 构建单条向量数据的 JsonObject */
    public JsonObject buildRow(String chunkId, Long documentId,
                               String documentName, int chunkIndex,
                               String content, float[] embedding) {
        JsonObject row = new JsonObject();
        row.addProperty(CHUNK_ID_FIELD, chunkId);
        row.addProperty(DOCUMENT_ID_FIELD, documentId);
        row.addProperty(DOCUMENT_NAME_FIELD, documentName);
        row.addProperty(CHUNK_INDEX_FIELD, chunkIndex);
        row.addProperty(CONTENT_FIELD, content);
        JsonArray vec = new JsonArray(EMBEDDING_DIM);
        for (float v : embedding) {
            vec.add(v);
        }
        row.add(EMBEDDING_FIELD, vec);
        return row;
    }

    /** 向量搜索 */
    public List<SearchResp.SearchResult> search(String collectionName,
                                                 float[] queryVector, int topK) {
        var searchReq = SearchReq.builder()
                .collectionName(collectionName)
                .data(List.of(new FloatVec(queryVector)))
                .topK(topK)
                .outputFields(List.of(DOCUMENT_NAME_FIELD, CHUNK_INDEX_FIELD, CONTENT_FIELD))
                .searchParams(Map.of("metric_type", "COSINE",
                        "params", "{\"nprobe\": 8}"))
                .build();

        var searchResp = milvusClient.search(searchReq);
        return searchResp.getSearchResults().get(0);
    }

    /** 按 documentId 删除向量 */
    public void deleteByDocumentId(String collectionName, Long documentId) {
        var deleteReq = DeleteReq.builder()
                .collectionName(collectionName)
                .filter("document_id == " + documentId)
                .build();
        milvusClient.delete(deleteReq);
        log.info("删除文档 {} 的向量数据 from {}", documentId, collectionName);
    }

    private boolean collectionExists(String collectionName) {
        try {
            var req = HasCollectionReq.builder()
                    .collectionName(collectionName).build();
            return Boolean.TRUE.equals(milvusClient.hasCollection(req));
        } catch (Exception e) {
            return false;
        }
    }

    private CreateCollectionReq.CollectionSchema buildSchema() {
        var schema = CreateCollectionReq.CollectionSchema.builder().build();
        schema.addField(AddFieldReq.builder()
                .fieldName(CHUNK_ID_FIELD).dataType(DataType.VarChar)
                .maxLength(64).isPrimaryKey(true).build());
        schema.addField(AddFieldReq.builder()
                .fieldName(DOCUMENT_ID_FIELD).dataType(DataType.Int64).build());
        schema.addField(AddFieldReq.builder()
                .fieldName(DOCUMENT_NAME_FIELD).dataType(DataType.VarChar)
                .maxLength(255).build());
        schema.addField(AddFieldReq.builder()
                .fieldName(CHUNK_INDEX_FIELD).dataType(DataType.Int32).build());
        schema.addField(AddFieldReq.builder()
                .fieldName(CONTENT_FIELD).dataType(DataType.VarChar)
                .maxLength(60000).build());
        schema.addField(AddFieldReq.builder()
                .fieldName(EMBEDDING_FIELD).dataType(DataType.FloatVector)
                .dimension(EMBEDDING_DIM).build());
        return schema;
    }

    private List<IndexParam> buildIndexParams() {
        List<IndexParam> indexParams = new ArrayList<>();
        indexParams.add(IndexParam.builder()
                .fieldName(EMBEDDING_FIELD)
                .indexType(IndexParam.IndexType.IVF_FLAT)
                .metricType(IndexParam.MetricType.COSINE)
                .extraParams(Map.of("nlist", 1024))
                .build());
        return indexParams;
    }
}
