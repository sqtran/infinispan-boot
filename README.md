# infinispan-boot
This is a `spring-boot` demo of how to set up an embedded Infinispan cache that uses `DNS_PING` instead of `KUBE_PING`.


## Run it locally

This is a `spring-boot` application, so just run it with the following.

```bash
mvn spring-boot:run -Djgroups.dns.query=localhost -Dspring-boot.run.profiles=local
```

- `-Djgroups.dns.query=localhost` is needed for the Infinispan clustering configurations.  When working locally, this won't ever connect to a form cluster.
- `-Dspring-boot.run.profiles=local` will set up logback to use the non-folding log formats, which is easier to read when testing locally.


## Testing

There are a few endpoints that you can use to verify that the cache is working. 

```bash
# PUT something into the cache
curl -X PUT localhost:8080/cache/<key>/<value>

# GET everything in the cache
curl localhost:8080/all

# GET an individual entry
curl localhost:8080/cache/<key>
```

## Deploying to Kubernetes
When deploying this on OpenShift, the one gotcha is the default network stack is IPv6.  Even though the documentation says that IPv6 is allowed, pods were unable to find each other and form a cluster.

To resolve that issue, override the Java Options with the `JAVA_OPTS_APPEND` environment variable, and add `-Djava.net.preferIPv4Stack=true` to it.

`DNS_PING` also requires that you have a DNS service that can return all the IPs for a given DNS Query.  Even though this is built with OpenShift in mind, in theory you could use any DNS service.

Create a `headless service` that selects your application pods.

```yaml
kind: Service
apiVersion: v1
metadata:
  name: headless-cache
spec:
  selector:
    app: infinispan-boot-git
  clusterIP: None
  type: ClusterIP
  sessionAffinity: None
status:
  loadBalancer: {}
```

⚠️ The `headless service` above intentionally exludes a port configuration. ⚠️

If this application is deployed in an Istio Service Mesh, `Pilot` will warn about conflicting ports and clustering will not work.  The configuration above works with and without Istio.


Lastly, update your `JAVA_OPTS_APPEND` again so it includes the `Djgroups.dns.query` parameter, like the following: `-Djgroups.dns.query=headless-cache.<namespace>.svc.cluster.local -Djava.net.preferIPv4Stack=true`
