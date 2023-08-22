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

public class CreateMemePostTest {

    CreateMemePost mockedCreateMemePost;

    @Before
    public void setup() {
        mockedCreateMemePost = mock(CreateMemePost.class);
    }

    @Test
    public void okResponseOnGetRequestTest() {
        mockedCreateMemePost.tableName = "TEST_DB";
        MemePost fakeMemePost = MemePost.builder().user("test").id("123").build();

        Gson gson = new GsonBuilder().create();
        String json = gson.toJson(fakeMemePost);

        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent().withHttpMethod("POST").withBody(json);

        doNothing().when(mockedCreateMemePost).create(any(), any());
        when(mockedCreateMemePost.handleRequest(any(), any())).thenCallRealMethod();

        APIGatewayProxyResponseEvent result = mockedCreateMemePost.handleRequest(event,null);

        assertEquals(200, result.getStatusCode().intValue());

        verify(mockedCreateMemePost, times(1)).create(any(), any());
    }

    @Test
    public void serverErrorResponseOnDynamoDbErrorTest() {
        MemePost fakeMemePost = MemePost.builder().user("test").id("123").build();

        Gson gson = new GsonBuilder().create();
        String json = gson.toJson(fakeMemePost);

        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent().withHttpMethod("POST").withBody(json);

        doThrow(DynamoDbException.class).when(mockedCreateMemePost).create(any(), any());
        when(mockedCreateMemePost.handleRequest(any(), any())).thenCallRealMethod();

        APIGatewayProxyResponseEvent result = mockedCreateMemePost.handleRequest(event,null);

        assertEquals(400, result.getStatusCode().intValue());

        verify(mockedCreateMemePost, times(1)).create(any(), any());
    }

    @Test(expected = RuntimeException.class)
    public void throwExceptionOnIncorrectMethodRequestTest() {
        MemePost fakeMemePost = MemePost.builder().user("test").id("123").build();

        Gson gson = new GsonBuilder().create();
        String json = gson.toJson(fakeMemePost);

        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent().withHttpMethod("GET").withBody(json);

        when(mockedCreateMemePost.handleRequest(any(), any())).thenCallRealMethod();

        mockedCreateMemePost.handleRequest(event,null);
    }
}
