package net.ddns.memedex.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.Setter;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.*;

@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@DynamoDbBean
public class MemePost {
    String user;
    String id;
    String memeUrl;

    @DynamoDbAttribute("PK")
    @DynamoDbPartitionKey
    @DynamoDbSecondarySortKey(indexNames = "invertedIndex")
    public String getUser() {
        return this.user;
    }

    @DynamoDbAttribute("SK")
    @DynamoDbSortKey
    @DynamoDbSecondaryPartitionKey(indexNames = "invertedIndex")
    public String getId() {
        return this.id;
    }

    public String getMemeUrl() {
        return this.memeUrl;
    }
}
