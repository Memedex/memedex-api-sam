package net.ddns.memedex.handler;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import net.ddns.memedex.model.Item;
import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class GetByIdTest {

    GetById mockedGetById;

    @Before
    public void setup() {
        mockedGetById = mock(GetById.class);
    }

    @Test
    public void okResponseOnGetRequestTest() {
        mockedGetById.tableName = "TEST_DB";
        Item fakeItem = Item.builder().id("123").name("test").build();

        Map<String, String> pathParams = new HashMap<>();
        pathParams.put("id", "123");

        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent().withHttpMethod("GET").withPathParameters(pathParams);

        when(mockedGetById.getById(any(), any())).thenReturn(fakeItem);
        when(mockedGetById.handleRequest(any(), any())).thenCallRealMethod();

        APIGatewayProxyResponseEvent result = mockedGetById.handleRequest(event,null);

        assertEquals(200, result.getStatusCode().intValue());

        verify(mockedGetById, times(1)).getById(any(), any());
    }

    @Test
    public void serverErrorResponseOnDynamoDbErrorTest() {
        Map<String, String> pathParams = new HashMap<>();
        pathParams.put("id", "123");

        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent().withHttpMethod("GET").withPathParameters(pathParams);

        when(mockedGetById.getById(any(), any())).thenThrow(DynamoDbException.class);
        when(mockedGetById.handleRequest(any(), any())).thenCallRealMethod();

        APIGatewayProxyResponseEvent result = mockedGetById.handleRequest(event,null);

        assertEquals(400, result.getStatusCode().intValue());

        verify(mockedGetById, times(1)).getById(any(), any());
    }

    @Test(expected = RuntimeException.class)
    public void throwExceptionOnIncorrectMethodRequestTest() {
        Map<String, String> pathParams = new HashMap<>();
        pathParams.put("id", "123");

        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent().withHttpMethod("POST").withPathParameters(pathParams);

        when(mockedGetById.handleRequest(any(), any())).thenCallRealMethod();

        mockedGetById.handleRequest(event,null);
    }

    @Test(expected = NullPointerException.class)
    public void throwExceptionOnIncorrectPathParamRequestTest() {
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent().withHttpMethod("GET");

        when(mockedGetById.handleRequest(any(), any())).thenCallRealMethod();

        mockedGetById.handleRequest(event,null);
    }
}
