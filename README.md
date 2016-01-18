# Platform
## Docker
### Build
To build the WAR file within Docker run the following:

```bash
docker build -f Dockerfile.build -t platform/build .
# If on Linux
docker run -d -v /path/to/my/target:/app/target platform/build
# Otherwise
docker run --name=builder platform/build
docker cp builder:/app/target .
```

### Run
To run the app after building run the following:

```bash
docker build -f Dockerfile.run -t augurworks/platform .
docker run -d --name=platform -p 8080:8080 augurworks/platform
```

To confirm the project is running got to [http://[docker-ip]:8080/hello](http://[docker-ip]:8080/hello) and confirm that the Hello World JSON appears.
