package net.ddns.memedex.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import jakarta.validation.*;
import lombok.extern.slf4j.Slf4j;
import net.ddns.memedex.model.Entity;
import net.ddns.memedex.model.MemePost;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClientBuilder;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Slf4j
public class CreateMemePost implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    protected String tableName = System.getenv("TABLE_NAME");
    protected String AWS_ENV = System.getenv("AWS_ENV");

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context) {
        if (!event.getHttpMethod().equals("POST")) {
            throw new RuntimeException("CreateMemePost only accept POST method, you tried: " + event.getHttpMethod());
        }

        Map<String, String> headers = new HashMap<>();
        headers.put("Access-Control-Allow-Headers", "*");
        headers.put("Access-Control-Allow-Origin", "*");
        headers.put("Access-Control-Allow-Methods", "*");

        log.debug("received: {}", event);

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
            ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
            Validator validator = factory.getValidator();
            Set<ConstraintViolation<MemePost>> violations = validator.validate(memePost);

            if (!violations.isEmpty()) {
                throw new ValidationException(violations.stream().map(ConstraintViolation::getMessage).toList().toString());
            }

            this.create(enhancedClient, memePost);

            response.withStatusCode(200);
        } catch (DynamoDbException e) {
            log.error("DynamoDb exception occurred!", e);
            response.withStatusCode(400).withBody(e.getMessage());
        } catch (ValidationException e2) {
            log.error("Validation error has occurred!", e2);
            response.withStatusCode(400).withBody(e2.getMessage());
        }

        log.debug("response from: {}, statusCode: {}, body: {}", event.getPath(), response.getStatusCode(), response.getBody());
        return response;
    }

    protected void create(DynamoDbEnhancedClient enhancedClient, MemePost memePost) throws DynamoDbException {
        DynamoDbTable<MemePost> table = enhancedClient.table(tableName, TableSchema.fromBean(MemePost.class));

        long timestamp = System.currentTimeMillis();
        memePost.setId(Entity.MEME_POST + "#" + timestamp);

        String user = memePost.getUser();
        memePost.setUser(Entity.USER + "#" + user);

        // Put the memePost into an Amazon DynamoDB table.
        table.putItem(memePost);
    }
}
