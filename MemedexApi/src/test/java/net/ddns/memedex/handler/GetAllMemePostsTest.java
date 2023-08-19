package net.ddns.memedex.handler;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import net.ddns.memedex.model.MemePost;
import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class GetAllMemePostsTest {

    GetAllMemePosts mockedGetAllMemePosts;

    @Before
    public void setup() {
        mockedGetAllMemePosts = mock(GetAllMemePosts.class);
    }

    @Test
    public void okResponseOnGetRequestTest() {
        MemePost fakeMemePost = MemePost.builder().user("test").id("123").build();

        List<MemePost> fakeMemePosts = new ArrayList<>();
        fakeMemePosts.add(fakeMemePost);

        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent().withHttpMethod("GET");

        when(mockedGetAllMemePosts.getAll(any())).thenReturn(fakeMemePosts);
        when(mockedGetAllMemePosts.handleRequest(any(), any())).thenCallRealMethod();

        APIGatewayProxyResponseEvent result = mockedGetAllMemePosts.handleRequest(event,null);

        assertEquals(200, result.getStatusCode().intValue());

        verify(mockedGetAllMemePosts, times(1)).getAll(any());
    }

    @Test
    public void serverErrorResponseOnDynamoDbErrorTest() {
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent().withHttpMethod("GET");

        when(mockedGetAllMemePosts.getAll(any())).thenThrow(DynamoDbException.class);
        when(mockedGetAllMemePosts.handleRequest(any(), any())).thenCallRealMethod();

        APIGatewayProxyResponseEvent result = mockedGetAllMemePosts.handleRequest(event,null);

        assertEquals(400, result.getStatusCode().intValue());

        verify(mockedGetAllMemePosts, times(1)).getAll(any());
    }

    @Test(expected = RuntimeException.class)
    public void throwExceptionOnIncorrectRequestTest() {
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent().withHttpMethod("POST");

        when(mockedGetAllMemePosts.handleRequest(any(), any())).thenCallRealMethod();

        mockedGetAllMemePosts.handleRequest(event,null);
    }
}
