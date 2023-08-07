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
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
public class GetAllItems implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    protected String tableName = System.getenv("SAMPLE_TABLE");
    protected String AWS_ENV = System.getenv("AWS_ENV");

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context) {
        if (!event.getHttpMethod().equals("GET")) {
            throw new RuntimeException("GetAllItems only accept GET method, you tried: " + event.getHttpMethod());
        }

        log.debug("received: {}", event);

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
            List<Item> results = this.getAllItems(enhancedClient);

            Gson gson = new GsonBuilder().create();
            String jsonItem = gson.toJson(results);
            response.withStatusCode(200).withBody(jsonItem);
        } catch (DynamoDbException e) {
            log.error("DynamoDb exception occurred!", e);
            response.withStatusCode(400).withBody(e.getMessage());
        }

        log.debug("response from: {} statusCode: {} body: {}", event.getPath(), response.getStatusCode(), response.getBody());
        return response;
    }

    protected List<Item> getAllItems(DynamoDbEnhancedClient enhancedClient) throws DynamoDbException {
        // get all items from the table (only first 1MB data, you can use `LastEvaluatedKey` to get the rest of data)
        // https://docs.aws.amazon.com/AWSJavaScriptSDK/latest/AWS/DynamoDB/DocumentClient.html#scan-property
        // https://docs.aws.amazon.com/amazondynamodb/latest/APIReference/API_Scan.html
        AtomicReference<DynamoDbTable<Item>> sampleTable = new AtomicReference<>(enhancedClient.table(tableName, TableSchema.fromBean(Item.class)));
        return sampleTable.get().scan().items().stream().toList();
    }
}
