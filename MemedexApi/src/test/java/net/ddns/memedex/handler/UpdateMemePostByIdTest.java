package net.ddns.memedex.handler;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.ddns.memedex.model.MemePost;
import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class UpdateMemePostByIdTest {

    UpdateMemePostById mockedUpdateMemePostById;

    @Before
    public void setup() {
        mockedUpdateMemePostById = mock(UpdateMemePostById.class);
    }

    @Test
    public void okResponseOnPutRequestTest() {
        mockedUpdateMemePostById.tableName = "TEST_DB";
        MemePost fakeMemePost = MemePost.builder().user("test").id("123").memeUrl("https://www.test.com").build();

        Gson gson = new GsonBuilder().create();
        String json = gson.toJson(fakeMemePost);

        Map<String, String> pathParams = new HashMap<>();
        pathParams.put("id", "123");

        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent().withHttpMethod("PUT").withBody(json).withPathParameters(pathParams);

        when(mockedUpdateMemePostById.update(any(), any(), any())).thenReturn(fakeMemePost);
        when(mockedUpdateMemePostById.handleRequest(any(), any())).thenCallRealMethod();

        APIGatewayProxyResponseEvent result = mockedUpdateMemePostById.handleRequest(event,null);

        assertEquals(200, result.getStatusCode().intValue());

        verify(mockedUpdateMemePostById, times(1)).update(any(), any(), any());
    }

    @Test
    public void serverErrorResponseOnDynamoDbErrorTest() {
        MemePost fakeMemePost = MemePost.builder().user("test").id("123").memeUrl("https://www.test.com").build();

        Gson gson = new GsonBuilder().create();
        String json = gson.toJson(fakeMemePost);

        Map<String, String> pathParams = new HashMap<>();
        pathParams.put("id", "123");

        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent().withHttpMethod("PUT").withBody(json).withPathParameters(pathParams);

        when(mockedUpdateMemePostById.update(any(), any(), any())).thenThrow(DynamoDbException.class);
        when(mockedUpdateMemePostById.handleRequest(any(), any())).thenCallRealMethod();

        APIGatewayProxyResponseEvent result = mockedUpdateMemePostById.handleRequest(event,null);

        assertEquals(400, result.getStatusCode().intValue());

        verify(mockedUpdateMemePostById, times(1)).update(any(), any(), any());
    }

    @Test(expected = RuntimeException.class)
    public void throwExceptionOnIncorrectMethodRequestTest() {
        MemePost fakeMemePost = MemePost.builder().user("test").id("123").memeUrl("https://www.test.com").build();

        Gson gson = new GsonBuilder().create();
        String json = gson.toJson(fakeMemePost);

        Map<String, String> pathParams = new HashMap<>();
        pathParams.put("id", "123");

        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent().withHttpMethod("POST").withBody(json).withPathParameters(pathParams);

        when(mockedUpdateMemePostById.handleRequest(any(), any())).thenCallRealMethod();

        mockedUpdateMemePostById.handleRequest(event,null);
    }

    @Test(expected = NullPointerException.class)
    public void throwExceptionOnIncorrectPathParamRequestTest() {
        MemePost fakeMemePost = MemePost.builder().user("test").id("123").memeUrl("https://www.test.com").build();

        Gson gson = new GsonBuilder().create();
        String json = gson.toJson(fakeMemePost);

        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent().withHttpMethod("PUT").withBody(json);

        when(mockedUpdateMemePostById.handleRequest(any(), any())).thenCallRealMethod();

        mockedUpdateMemePostById.handleRequest(event,null);
    }
}
