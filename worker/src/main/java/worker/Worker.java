package worker;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.Protocol;
import redis.clients.jedis.exceptions.JedisConnectionException;
import java.sql.*;
import org.json.JSONObject;
import argo.jdom.JdomParser;
import argo.jdom.JsonNode;
import argo.jdom.JsonRootNode;

class Worker {
  public static void main(String[] args) {
    try {
      /*Removing to change to connecting services in PCF
       * Jedis redis = connectToRedis("redis");
       * Connection dbConn = connectToDB("db");*/
   
      String vcap_services = System.getenv("VCAP_SERVICES");
      JsonNode postgreSQLcredentials = null;
      JsonNode rediscredentials = null;
    		  
      if (vcap_services != null && vcap_services.length() > 0) {
    	  // parsing rediscloud credentials
    	  JsonRootNode root = new JdomParser().parse(vcap_services);
    	  
    	  //get redis credentials
          JsonNode rediscloudNode = root.getNode("rediscloud");
          rediscredentials = rediscloudNode.getNode(0).getNode("credentials");
          
          //get postgreSQL credentials
          JsonNode postgreSQLcloudNode = root.getNode("elephantsql");
          postgreSQLcredentials = postgreSQLcloudNode.getNode(0).getNode("credentials");
       
      }
      
      Jedis redis = connectToRedis(rediscredentials);
      Connection dbConn = connectToDB(postgreSQLcredentials);

      System.out.println("Watching vote queue to see what comes in");
      
      while (true) {
        String voteJSON = redis.blpop(0, "votes").get(1);
        JSONObject voteData = new JSONObject(voteJSON);
        String voterID = voteData.getString("voter_id");
        String vote = voteData.getString("vote");

        System.err.printf("Processing vote for '%s' by '%s'\n", vote, voterID);
        updateVote(dbConn, voterID, vote);
      }
    } catch (Exception e) {
      e.printStackTrace();
      System.exit(1);
    }
  }

  static void updateVote(Connection dbConn, String voterID, String vote) throws SQLException {
    PreparedStatement insert = dbConn.prepareStatement(
      "INSERT INTO votes (id, vote) VALUES (?, ?)");
    insert.setString(1, voterID);
    insert.setString(2, vote);

    try {
      insert.executeUpdate();
    } catch (SQLException e) {
      PreparedStatement update = dbConn.prepareStatement(
        "UPDATE votes SET vote = ? WHERE id = ?");
      update.setString(1, vote);
      update.setString(2, voterID);
      update.executeUpdate();
    }
  }

  static Jedis connectToRedis(JsonNode credentials) {
      JedisPool pool = new JedisPool(new JedisPoolConfig(),
              credentials.getStringValue("hostname"),
              Integer.parseInt(credentials.getStringValue("port")),
              Protocol.DEFAULT_TIMEOUT,
              credentials.getStringValue("password"));
    return pool.getResource();
  }

  static Connection connectToDB(JsonNode credentials) throws SQLException {
    Connection conn = null;
    
    try {
    	Class.forName("org.postgresql.Driver");
    } catch (ClassNotFoundException e) {
        System.out.println("Class not found " + e);
    }
    
    //parse out credentials from URI string
    String uri = credentials.getStringValue("uri");
    String host = uri.substring(uri.lastIndexOf("@") + 1 , uri.length());
    //username and password parse:
    String userName = uri.substring(uri.indexOf("://") + 3 , uri.length());
    String passWord = userName;
    userName = userName.substring(0, userName.indexOf(":"));
    passWord = passWord.substring(passWord.indexOf(":") +1 , passWord.indexOf("@"));
    
      while (conn == null) {
        try {
          conn = DriverManager.getConnection("jdbc:postgresql://" + host, userName, passWord);
          System.out.println("connected to db");
        	
        } catch (SQLException e) {
          System.err.println("Failed to connect to db - retrying " + e.toString());
          sleep(1000);
        }
      }

      PreparedStatement st = conn.prepareStatement(
        "CREATE TABLE IF NOT EXISTS votes (id VARCHAR(255) NOT NULL UNIQUE, vote VARCHAR(255) NOT NULL)");
      st.executeUpdate();

    return conn;
  }

  static void sleep(long duration) {
    try {
      Thread.sleep(duration);
    } catch (InterruptedException e) {
      System.exit(1);
    }
  }
}
