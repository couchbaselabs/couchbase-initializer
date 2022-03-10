package {{&package}};

import com.couchbase.client.core.env.SecurityConfig;
import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.Collection;
import com.couchbase.client.java.kv.GetResult;
import com.couchbase.client.java.query.QueryResult;

import java.time.Duration;

import static com.couchbase.client.java.ClusterOptions.clusterOptions;

public class HelloCouchbase {

  public static void main(String... args) {
    String connectionString = "{{connectionString}}";
    String username = "{{username}}";
    String password = "{{password}}";

    Cluster cluster = Cluster.connect(connectionString, username, password);

    try {
      // This example assumes the "travel-sample" sample bucket is present.

      Bucket bucket = cluster.bucket("travel-sample");
      bucket.waitUntilReady(Duration.ofSeconds(10));

      QueryResult queryResult = cluster.query(
          "select * from `travel-sample` where type = 'airline' limit 5"
      );
      queryResult.rowsAsObject().forEach(System.out::println);

      Collection collection = bucket.defaultCollection();
      GetResult getResult = collection.get("airline_10");
      System.out.println(getResult);

    } finally {
      cluster.disconnect();
    }
  }
}
