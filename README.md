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
docker build -f Dockerfile.run -t alfred .
```

### Run
To run the app after building run the following:

```bash
docker run -d --name=alfred -p 8080:8080 alfred
```

To confirm the project is running got to [http://[docker-ip]:8080/](http://[docker-ip]:8080/) and confirm that a JSON message appears.

## Tag and Push
**NOTE:** Info on configuring the AWS command line for ECR and the repo referred to here can be found [here](https://console.aws.amazon.com/ecs/home?region=us-east-1#/repositories/alfred#images)

After building tag the local build then push the current version and change the latest tag with the following:

```bash
docker tag alfred alfred:[TAG]
docker tag alfred alfred:latest
docker push 274685854631.dkr.ecr.us-east-1.amazonaws.com/alfred:[TAG]
docker push 274685854631.dkr.ecr.us-east-1.amazonaws.com/alfred:latest
```

To run the remote container run the following after logging into the AWS ECR:

```bash
docker run -d --name=alfred -p 8080:8080 274685854631.dkr.ecr.us-east-1.amazonaws.com/alfred
```
