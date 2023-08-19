package net.ddns.memedex.handler;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.ddns.memedex.model.MemePost;
import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.when;

public class PutMemePostTest {

    PutMemePost mockedPutMemePost;

    @Before
    public void setup() {
        mockedPutMemePost = mock(PutMemePost.class);
    }

    @Test
    public void okResponseOnGetRequestTest() {
        mockedPutMemePost.tableName = "TEST_DB";
        MemePost fakeMemePost = MemePost.builder().user("test").id("123").build();

        Gson gson = new GsonBuilder().create();
        String json = gson.toJson(fakeMemePost);

        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent().withHttpMethod("POST").withBody(json);

        doNothing().when(mockedPutMemePost).put(any(), any());
        when(mockedPutMemePost.handleRequest(any(), any())).thenCallRealMethod();

        APIGatewayProxyResponseEvent result = mockedPutMemePost.handleRequest(event,null);

        assertEquals(200, result.getStatusCode().intValue());

        verify(mockedPutMemePost, times(1)).put(any(), any());
    }

    @Test
    public void serverErrorResponseOnDynamoDbErrorTest() {
        MemePost fakeMemePost = MemePost.builder().user("test").id("123").build();

        Gson gson = new GsonBuilder().create();
        String json = gson.toJson(fakeMemePost);

        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent().withHttpMethod("POST").withBody(json);

        doThrow(DynamoDbException.class).when(mockedPutMemePost).put(any(), any());
        when(mockedPutMemePost.handleRequest(any(), any())).thenCallRealMethod();

        APIGatewayProxyResponseEvent result = mockedPutMemePost.handleRequest(event,null);

        assertEquals(400, result.getStatusCode().intValue());

        verify(mockedPutMemePost, times(1)).put(any(), any());
    }

    @Test(expected = RuntimeException.class)
    public void throwExceptionOnIncorrectMethodRequestTest() {
        MemePost fakeMemePost = MemePost.builder().user("test").id("123").build();

        Gson gson = new GsonBuilder().create();
        String json = gson.toJson(fakeMemePost);

        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent().withHttpMethod("GET").withBody(json);

        when(mockedPutMemePost.handleRequest(any(), any())).thenCallRealMethod();

        mockedPutMemePost.handleRequest(event,null);
    }
}
