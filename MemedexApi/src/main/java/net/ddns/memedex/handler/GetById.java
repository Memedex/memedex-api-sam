package net.ddns.memedex.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.extern.slf4j.Slf4j;
import net.ddns.memedex.model.Item;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.GetItemEnhancedRequest;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClientBuilder;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;

import java.net.URI;

/**
 * A simple example includes a HTTP get method to get one item by id from a DynamoDB table.
 */
@Slf4j
public class GetById implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    protected String tableName = System.getenv("SAMPLE_TABLE");
    protected String AWS_ENV = System.getenv("AWS_ENV");


    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context) {
        if (!event.getHttpMethod().equals("GET")) {
            throw new RuntimeException("GetById only accept GET method, you tried: " + event.getHttpMethod());
        }

        log.debug("received: {}", event);

        String id = event.getPathParameters().get("id");

        DefaultCredentialsProvider credentialsProvider = DefaultCredentialsProvider.create();

        DynamoDbClientBuilder ddbBuilder = DynamoDbClient.builder()
                .region(Region.US_EAST_2)
                .credentialsProvider(credentialsProvider);

        if ("AWS_SAM_LOCAL".equals(AWS_ENV)) {
            ddbBuilder.endpointOverride(URI.create("http://host.docker.internal:8000"));
        }

        DynamoDbEnhancedClient enhancedClient = DynamoDbEnhancedClient.builder()
                .dynamoDbClient(ddbBuilder.build())
                .build();

        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();

        try {
            Item result = this.getById(enhancedClient, id);

            Gson gson = new GsonBuilder().create();
            String jsonItem = gson.toJson(result);
            response.withStatusCode(200).withBody(jsonItem);
        } catch (DynamoDbException e) {
            log.error("DynamoDb exception occurred!", e);
            response.withStatusCode(400).withBody(e.getMessage());
        }

        log.debug("response from: {} statusCode: {} body: {}", event.getPath(), response.getStatusCode(), response.getBody());
        return response;
    }

    protected Item getById(DynamoDbEnhancedClient enhancedClient, String id) throws DynamoDbException {
        DynamoDbTable<Item> sampleTable = enhancedClient.table(tableName, TableSchema.fromBean(Item.class));
        Key key = Key.builder()
                .partitionValue(id)
                .build();

        // Get the item by using the key.
        return sampleTable.getItem(
                (GetItemEnhancedRequest.Builder requestBuilder) -> requestBuilder.key(key));
    }
}
