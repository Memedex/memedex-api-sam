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

public class DeleteMemePostByIdTest {

    DeleteMemePostById mockedDeleteMemePostById;

    @Before
    public void setup() {
        mockedDeleteMemePostById = mock(DeleteMemePostById.class);
    }

    @Test
    public void okResponseOnDeleteRequestTest() {
        mockedDeleteMemePostById.tableName = "TEST_DB";

        Map<String, String> pathParams = new HashMap<>();
        pathParams.put("id", "123");

        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent().withHttpMethod("DELETE").withPathParameters(pathParams);

        doNothing().when(mockedDeleteMemePostById).deleteById(any(), any());
        when(mockedDeleteMemePostById.handleRequest(any(), any())).thenCallRealMethod();

        APIGatewayProxyResponseEvent result = mockedDeleteMemePostById.handleRequest(event,null);

        assertEquals(204, result.getStatusCode().intValue());

        verify(mockedDeleteMemePostById, times(1)).deleteById(any(), any());
    }

    @Test
    public void serverErrorResponseOnDynamoDbErrorTest() {
        Map<String, String> pathParams = new HashMap<>();
        pathParams.put("id", "123");

        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent().withHttpMethod("DELETE").withPathParameters(pathParams);

        doThrow(DynamoDbException.class).when(mockedDeleteMemePostById).deleteById(any(), any());
        when(mockedDeleteMemePostById.handleRequest(any(), any())).thenCallRealMethod();

        APIGatewayProxyResponseEvent result = mockedDeleteMemePostById.handleRequest(event,null);

        assertEquals(400, result.getStatusCode().intValue());

        verify(mockedDeleteMemePostById, times(1)).deleteById(any(), any());
    }

    @Test(expected = RuntimeException.class)
    public void throwExceptionOnIncorrectMethodRequestTest() {
        Map<String, String> pathParams = new HashMap<>();
        pathParams.put("id", "123");

        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent().withHttpMethod("GET").withPathParameters(pathParams);

        when(mockedDeleteMemePostById.handleRequest(any(), any())).thenCallRealMethod();

        mockedDeleteMemePostById.handleRequest(event,null);
    }

    @Test(expected = NullPointerException.class)
    public void throwExceptionOnIncorrectPathParamRequestTest() {
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent().withHttpMethod("DELETE");

        when(mockedDeleteMemePostById.handleRequest(any(), any())).thenCallRealMethod();

        mockedDeleteMemePostById.handleRequest(event,null);
    }
}
