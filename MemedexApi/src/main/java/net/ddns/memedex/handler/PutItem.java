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
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClientBuilder;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;

import java.net.URI;

@Slf4j
public class PutItem implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    protected String tableName = System.getenv("SAMPLE_TABLE");
    protected String AWS_ENV = System.getenv("AWS_ENV");

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context) {
        if (!event.getHttpMethod().equals("POST")) {
            throw new RuntimeException("PutItem only accept POST method, you tried: " + event.getHttpMethod());
        }

        log.debug("received: {}", event);

        Gson gson = new GsonBuilder().create();
        Item item = gson.fromJson(event.getBody(), Item.class);

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
            this.putItem(enhancedClient, item);

            response.withStatusCode(200);
        } catch (DynamoDbException e) {
            log.error("DynamoDb exception occurred!", e);
            response.withStatusCode(400).withBody(e.getMessage());
        }

        log.debug("response from: {} statusCode: {} body: {}", event.getPath(), response.getStatusCode(), response.getBody());
        return response;
    }

    protected void putItem(DynamoDbEnhancedClient enhancedClient, Item item) throws DynamoDbException {
        DynamoDbTable<Item> sampleTable = enhancedClient.table(tableName, TableSchema.fromBean(Item.class));

        // Put the item into an Amazon DynamoDB table.
        sampleTable.putItem(item);
    }
}
