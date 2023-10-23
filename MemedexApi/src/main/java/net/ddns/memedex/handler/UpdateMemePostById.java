package net.ddns.memedex.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.extern.slf4j.Slf4j;
import net.ddns.memedex.model.Entity;
import net.ddns.memedex.model.MemePost;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.enhanced.dynamodb.*;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.UpdateItemEnhancedRequest;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClientBuilder;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class UpdateMemePostById implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    protected String tableName = System.getenv("TABLE_NAME");
    protected String AWS_ENV = System.getenv("AWS_ENV");

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context) {
        if (!event.getHttpMethod().equals("PUT")) {
            throw new RuntimeException("PutMemePostById only accept PUT method, you tried: " + event.getHttpMethod());
        }

        Map<String, String> headers = new HashMap<>();
        headers.put("Access-Control-Allow-Headers", "*");
        headers.put("Access-Control-Allow-Origin", "*");
        headers.put("Access-Control-Allow-Methods", "*");

        log.debug("received: {}", event);

        String id = event.getPathParameters().get("id");

        Gson gson = new GsonBuilder().create();
        MemePost memePost = gson.fromJson(event.getBody(), MemePost.class);

        DefaultCredentialsProvider credentialsProvider = DefaultCredentialsProvider.create();

        DynamoDbClientBuilder ddbBuilder = DynamoDbClient.builder()
                .region(Region.US_EAST_2)
                .credentialsProvider(credentialsProvider);

        if ("AWS_SAM_LOCAL".equals(AWS_ENV)) {
            ddbBuilder.endpointOverride(URI.create("http://dynamodb-local:8000"));
        }

        DynamoDbEnhancedClient enhancedClient = DynamoDbEnhancedClient.builder()
                .dynamoDbClient(ddbBuilder.build())
                .build();

        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
        response.setHeaders(headers);

        try {
            MemePost result = this.update(enhancedClient, id, memePost);

            String memePostJson = gson.toJson(result);
            response.withStatusCode(200).withBody(memePostJson);
        } catch (DynamoDbException e) {
            log.error("DynamoDb exception occurred!", e);
            response.withStatusCode(400).withBody(e.getMessage());
        }

        log.debug("response from: {}, statusCode: {}, body: {}", event.getPath(), response.getStatusCode(), response.getBody());
        return response;
    }

    protected MemePost update(DynamoDbEnhancedClient enhancedClient, String id, MemePost memePost) throws DynamoDbException {
        DynamoDbTable<MemePost> table = enhancedClient.table(tableName, TableSchema.fromBean(MemePost.class));
        DynamoDbIndex<MemePost> indexedTable = table.index("invertedIndex");

        QueryConditional keyEqualTo = QueryConditional.keyEqualTo(b -> b.partitionValue(Entity.MEME_POST + "#" + id));

        QueryEnhancedRequest tableQuery = QueryEnhancedRequest.builder()
                .queryConditional(keyEqualTo)
                .attributesToProject("PK", "SK")
                .build();

        MemePost memePostToUpdate = indexedTable.query(tableQuery).stream().findFirst().get().items().get(0);

        memePost.setUser(memePostToUpdate.getUser());
        memePost.setId(memePostToUpdate.getId());

        Expression memePostExistsExpression = Expression.builder()
                .expression("attribute_exists(PK)")
                .build();

        UpdateItemEnhancedRequest<MemePost> updateItemEnhancedRequest = UpdateItemEnhancedRequest.builder(MemePost.class)
                .item(memePost)
                .conditionExpression(memePostExistsExpression)
                .ignoreNulls(true)
                .build();

        return table.updateItem(updateItemEnhancedRequest);
    }
}
