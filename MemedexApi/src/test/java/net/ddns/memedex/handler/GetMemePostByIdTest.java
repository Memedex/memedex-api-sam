package net.ddns.memedex.handler;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import net.ddns.memedex.model.MemePost;
import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class GetMemePostByIdTest {

    GetMemePostById mockedGetMemePostById;

    @Before
    public void setup() {
        mockedGetMemePostById = mock(GetMemePostById.class);
    }

    @Test
    public void okResponseOnGetRequestTest() {
        mockedGetMemePostById.tableName = "TEST_DB";
        MemePost fakeMemePost = MemePost.builder().user("test").id("123").build();

        Map<String, String> pathParams = new HashMap<>();
        pathParams.put("id", "123");

        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent().withHttpMethod("GET").withPathParameters(pathParams);

        when(mockedGetMemePostById.getById(any(), any())).thenReturn(fakeMemePost);
        when(mockedGetMemePostById.handleRequest(any(), any())).thenCallRealMethod();

        APIGatewayProxyResponseEvent result = mockedGetMemePostById.handleRequest(event,null);

        assertEquals(200, result.getStatusCode().intValue());

        verify(mockedGetMemePostById, times(1)).getById(any(), any());
    }

    @Test
    public void serverErrorResponseOnDynamoDbErrorTest() {
        Map<String, String> pathParams = new HashMap<>();
        pathParams.put("id", "123");

        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent().withHttpMethod("GET").withPathParameters(pathParams);

        when(mockedGetMemePostById.getById(any(), any())).thenThrow(DynamoDbException.class);
        when(mockedGetMemePostById.handleRequest(any(), any())).thenCallRealMethod();

        APIGatewayProxyResponseEvent result = mockedGetMemePostById.handleRequest(event,null);

        assertEquals(400, result.getStatusCode().intValue());

        verify(mockedGetMemePostById, times(1)).getById(any(), any());
    }

    @Test(expected = RuntimeException.class)
    public void throwExceptionOnIncorrectMethodRequestTest() {
        Map<String, String> pathParams = new HashMap<>();
        pathParams.put("id", "123");

        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent().withHttpMethod("POST").withPathParameters(pathParams);

        when(mockedGetMemePostById.handleRequest(any(), any())).thenCallRealMethod();

        mockedGetMemePostById.handleRequest(event,null);
    }

    @Test(expected = NullPointerException.class)
    public void throwExceptionOnIncorrectPathParamRequestTest() {
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent().withHttpMethod("GET");

        when(mockedGetMemePostById.handleRequest(any(), any())).thenCallRealMethod();

        mockedGetMemePostById.handleRequest(event,null);
    }
}
