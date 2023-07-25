package net.ddns.memedex.handler;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import net.ddns.memedex.model.Item;
import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class GetAllItemsTest {

    GetAllItems mockedGetAllItems;

    @Before
    public void setup() {
        mockedGetAllItems = mock(GetAllItems.class);
    }

    @Test
    public void okResponseOnGetRequestTest() {
        Item fakeItem = Item.builder().id("123").name("test").build();

        List<Item> fakeItems = new ArrayList<>();
        fakeItems.add(fakeItem);

        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent().withHttpMethod("GET");

        when(mockedGetAllItems.getAllItems(any())).thenReturn(fakeItems);
        when(mockedGetAllItems.handleRequest(any(), any())).thenCallRealMethod();

        APIGatewayProxyResponseEvent result = mockedGetAllItems.handleRequest(event,null);

        assertEquals(200, result.getStatusCode().intValue());

        verify(mockedGetAllItems, times(1)).getAllItems(any());
    }

    @Test
    public void serverErrorResponseOnDynamoDbErrorTest() {
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent().withHttpMethod("GET");

        when(mockedGetAllItems.getAllItems(any())).thenThrow(DynamoDbException.class);
        when(mockedGetAllItems.handleRequest(any(), any())).thenCallRealMethod();

        APIGatewayProxyResponseEvent result = mockedGetAllItems.handleRequest(event,null);

        assertEquals(400, result.getStatusCode().intValue());

        verify(mockedGetAllItems, times(1)).getAllItems(any());
    }

    @Test(expected = RuntimeException.class)
    public void throwExceptionOnIncorrectRequestTest() {
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent().withHttpMethod("POST");

        when(mockedGetAllItems.handleRequest(any(), any())).thenCallRealMethod();

        mockedGetAllItems.handleRequest(event,null);
    }
}
