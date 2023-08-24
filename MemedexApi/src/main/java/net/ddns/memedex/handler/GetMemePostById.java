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
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClientBuilder;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;

import java.net.URI;

@Slf4j
public class GetMemePostById implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    protected String tableName = System.getenv("TABLE_NAME");
    protected String AWS_ENV = System.getenv("AWS_ENV");


    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context) {
        if (!event.getHttpMethod().equals("GET")) {
            throw new RuntimeException("GetMemePostById only accept GET method, you tried: " + event.getHttpMethod());
        }

        log.debug("received: {}", event);

        String id = event.getPathParameters().get("id");

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

        try {
            MemePost result = this.getById(enhancedClient, id);

            Gson gson = new GsonBuilder().create();
            String memePost = gson.toJson(result);
            response.withStatusCode(200).withBody(memePost);
        } catch (DynamoDbException e) {
            log.error("DynamoDb exception occurred!", e);
            response.withStatusCode(400).withBody(e.getMessage());
        }

        log.debug("response from: {}, statusCode: {}, body: {}", event.getPath(), response.getStatusCode(), response.getBody());
        return response;
    }

    protected MemePost getById(DynamoDbEnhancedClient enhancedClient, String id) throws DynamoDbException {
        DynamoDbIndex<MemePost> table = enhancedClient.table(tableName, TableSchema.fromBean(MemePost.class)).index("invertedIndex");

        QueryConditional keyEqualTo = QueryConditional.keyEqualTo(b -> b.partitionValue(Entity.MEME_POST + "#" + id));

        QueryEnhancedRequest tableQuery = QueryEnhancedRequest.builder()
                .queryConditional(keyEqualTo)
                .build();

        return table.query(tableQuery).stream().findFirst().get().items().get(0);
    }
}
