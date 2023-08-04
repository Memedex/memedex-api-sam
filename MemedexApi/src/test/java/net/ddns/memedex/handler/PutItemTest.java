package net.ddns.memedex.handler;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.ddns.memedex.model.Item;
import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.when;

public class PutItemTest {

    PutItem mockedPutItem;

    @Before
    public void setup() {
        mockedPutItem = mock(PutItem.class);
    }

    @Test
    public void okResponseOnGetRequestTest() {
        mockedPutItem.tableName = "TEST_DB";
        Item fakeItem = Item.builder().id("123").name("test").build();

        Gson gson = new GsonBuilder().create();
        String json = gson.toJson(fakeItem);

        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent().withHttpMethod("POST").withBody(json);

        doNothing().when(mockedPutItem).putItem(any(), any());
        when(mockedPutItem.handleRequest(any(), any())).thenCallRealMethod();

        APIGatewayProxyResponseEvent result = mockedPutItem.handleRequest(event,null);

        assertEquals(200, result.getStatusCode().intValue());

        verify(mockedPutItem, times(1)).putItem(any(), any());
    }

    @Test
    public void serverErrorResponseOnDynamoDbErrorTest() {
        Item fakeItem = Item.builder().id("123").name("test").build();

        Gson gson = new GsonBuilder().create();
        String json = gson.toJson(fakeItem);

        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent().withHttpMethod("POST").withBody(json);

        doThrow(DynamoDbException.class).when(mockedPutItem).putItem(any(), any());
        when(mockedPutItem.handleRequest(any(), any())).thenCallRealMethod();

        APIGatewayProxyResponseEvent result = mockedPutItem.handleRequest(event,null);

        assertEquals(400, result.getStatusCode().intValue());

        verify(mockedPutItem, times(1)).putItem(any(), any());
    }

    @Test(expected = RuntimeException.class)
    public void throwExceptionOnIncorrectMethodRequestTest() {
        Item fakeItem = Item.builder().id("123").name("test").build();

        Gson gson = new GsonBuilder().create();
        String json = gson.toJson(fakeItem);

        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent().withHttpMethod("GET").withBody(json);

        when(mockedPutItem.handleRequest(any(), any())).thenCallRealMethod();

        mockedPutItem.handleRequest(event,null);
    }
}
