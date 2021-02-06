# infinispan-boot
This is a `spring-boot` demo of how to set up an embedded Infinispan cache that uses `DNS_PING` instead of `KUBE_PING`.


## Run it locally

This is a `spring-boot` application, so just run it with the following.

```bash
mvn spring-boot:run -Djgroups.dns.query=localhost -Dspring-boot.run.profiles=local
```

- `-Djgroups.dns.query=localhost` is needed for the Infinispan clustering configurations.  When working locally, this won't ever connect to a form cluster.
- `-Dspring-boot.run.profiles=local` will set up logback to use the non-folding log formats, which is easier to read when testing locally.


## Deploying to Kubernetes
When deploying this on OpenShift, the one gotcha is the default network stack is IPv6.  Even though the documentation says that IPv6 is allowed, pods were unable to find each other and form a cluster.

To resolve that issue, override the Java Options with the `JAVA_OPTS` environment variable, and add `-Djava.net.preferIPv4Stack=true` to it.

`DNS_PING` also requires that you have a DNS service that can return all the IPs for a given DNS Query.  Even though this is built with OpenShift in mind, in theory you could use any DNS service.

Create a `headless Service` that selects your application pods.

```yaml
kind: Service
apiVersion: v1
metadata:
  name: headless-cache
spec:
  ports:
    - name: dnsping
      protocol: TCP
      port: 7800
      targetPort: 7800
  selector:
    app: infinispan-boot-git
  clusterIP: None
  type: ClusterIP
  sessionAffinity: None
status:
  loadBalancer: {}
```

Update your `JAVA_OPTS` again with another system property for `-Djgroups.dns.query` that's set for your service local address, i.e. `headless-cache.<namespace>.svc.cluster.local`