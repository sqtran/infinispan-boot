package webapp;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.infinispan.Cache;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfiguration;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.manager.DefaultCacheManager;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@EnableAutoConfiguration
public class Example {

  private static final Logger logger = LogManager.getLogger(Example.class);
  private static Cache<String, String> cache;

  @RequestMapping(value = "/cache/{key}/{value}", method = RequestMethod.PUT)
  public ResponseEntity<String> add(@PathVariable String key, @PathVariable String value) {
    cache.put(key, value);
    return new ResponseEntity<String>("Added!", HttpStatus.OK);
  }

  @RequestMapping(value = "/cache/{key}", method = RequestMethod.GET)
  public ResponseEntity<String> get(@PathVariable String key) {
    return new ResponseEntity<String>(cache.get(key).toString(), HttpStatus.OK);
  }

  @RequestMapping(value = "/all", method = RequestMethod.GET)
  public ResponseEntity<String> getAll() {
    StringBuilder builder = new StringBuilder();

    builder.append("Host: ");
    try {
      builder.append(InetAddress.getLocalHost().getHostName());
    } catch (UnknownHostException e) {
      builder.append("Unable to determine host\n");
    }

    for(String s : cache.keySet()) {
      String output = String.format("[%s] = %s\n", s, cache.get(s).toString());
      System.out.print(output);
      builder.append(output);
    }
    return new ResponseEntity<String>(builder.toString(), HttpStatus.OK);
  }

  @RequestMapping(value = "/clear", method = RequestMethod.GET)
  public ResponseEntity<String> clear() {
    cache.clear();
    return new ResponseEntity<String>("Cache cleared", HttpStatus.OK);
  }

  @RequestMapping("/health")
  public String healthz() {
    logger.info("status.:.UP");
    return "status.:.UP";
  }

  public static void main(String[] args) throws Exception {

    // Configure Infinispan to use default transport and the default Kubernetes
    // JGroups configuration.
    GlobalConfiguration globalConfig = new GlobalConfigurationBuilder().transport().defaultTransport()
      // newer Infinispan versions come with a default jgroups for kubernetes
      //.addProperty("configurationFile", "default-configs/default-jgroups-kubernetes.xml").build();
      .addProperty("configurationFile", "myconfiguration.xml").build();

    // Use a distributed cache for the quickstart application.
    Configuration cacheConfiguration = new ConfigurationBuilder().clustering().cacheMode(CacheMode.REPL_SYNC).build();

    DefaultCacheManager cacheManager = new DefaultCacheManager(globalConfig, cacheConfiguration);
    cacheManager.defineConfiguration("default", cacheConfiguration);
    cache = cacheManager.getCache("default");

    // Each cluster member updates its own entry in the cache.
    /*
     * String hostname = Inet4Address.getLocalHost().getHostName();
     * ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
     * scheduler.scheduleAtFixedRate(() -> { String time = Instant.now().toString();
     * cache.put(hostname, time); System.out.println("[" + time + "][" + hostname +
     * "] Values from the cache: "); cache.entrySet().forEach(entry ->
     * System.out.printf("Host %s Time %s \n", entry.getKey(), entry.getValue()) );
     * }, 0, 2, TimeUnit.SECONDS);
     * 
     * try { //The container operates for one hour and then shuts down.
     * TimeUnit.HOURS.sleep(1); } catch (InterruptedException e) {
     * scheduler.shutdown(); cacheManager.stop(); }
     */
    SpringApplication.run(Example.class, args);
  }

}
